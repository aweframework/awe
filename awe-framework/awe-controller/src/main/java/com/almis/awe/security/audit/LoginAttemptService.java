package com.almis.awe.security.audit;

import com.almis.awe.config.LockoutConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * Automatic account lockout: counts the consecutive failed local logins of a user in {@code ope.NumLog} and locks the
 * account ({@code ope.PwdLck}) when {@code awe.security.lockout.max-attempts} is reached. A successful login resets
 * the count. Does nothing while the lockout is switched off (max attempts 0).
 */
public class LoginAttemptService {

  /** Query reading the failed attempts of a user */
  public static final String FAILED_ATTEMPTS_QUERY = "UsrGetNumLog";
  /** Maintain storing the failed attempts of a user */
  public static final String UPDATE_ATTEMPTS_MAINTAIN = "UpdCntLog";
  /** Maintain locking or unlocking a user */
  public static final String UPDATE_LOCK_MAINTAIN = "UpdPwdLck";

  private static final String USER = "Usr";

  private final LockoutConfigProperties properties;
  private final QueryService queryService;
  private final MaintainService maintainService;

  /**
   * Constructor
   *
   * @param properties      Lockout properties
   * @param queryService    Query service
   * @param maintainService Maintain service
   */
  public LoginAttemptService(LockoutConfigProperties properties, QueryService queryService, MaintainService maintainService) {
    this.properties = properties;
    this.queryService = queryService;
    this.maintainService = maintainService;
  }

  /**
   * Whether the automatic lockout is on
   *
   * @return Lockout enabled
   */
  public boolean isEnabled() {
    return properties.getMaxAttempts() > 0;
  }

  /**
   * Count a failed login (wrong password) and lock the account when the limit is reached
   *
   * @param username User name
   * @return true when this failure locked the account
   * @throws AWException Error reading or updating the user
   */
  public boolean registerFailure(String username) throws AWException {
    if (!isEnabled() || username == null) {
      return false;
    }
    Integer current = failedAttempts(username);
    if (current == null) {
      // Unknown user: nothing to count
      return false;
    }
    int attempts = current + 1;
    maintainService.launchPrivateMaintain(UPDATE_ATTEMPTS_MAINTAIN, parameters(username).put(UPDATE_ATTEMPTS_MAINTAIN, attempts));
    if (attempts >= properties.getMaxAttempts()) {
      maintainService.launchPrivateMaintain(UPDATE_LOCK_MAINTAIN, parameters(username).put("PwdLck", 1));
      return true;
    }
    return false;
  }

  /**
   * Reset the failed logins after a successful one
   *
   * @param username User name
   * @throws AWException Error reading or updating the user
   */
  public void registerSuccess(String username) throws AWException {
    if (!isEnabled() || username == null) {
      return;
    }
    Integer current = failedAttempts(username);
    if (current != null && current > 0) {
      resetFailures(username);
    }
  }

  /**
   * Reset the failed logins (an administrator unlocked the account)
   *
   * @param username User name
   * @throws AWException Error updating the user
   */
  public void resetFailures(String username) throws AWException {
    maintainService.launchPrivateMaintain(UPDATE_ATTEMPTS_MAINTAIN, parameters(username).put(UPDATE_ATTEMPTS_MAINTAIN, 0));
  }

  private Integer failedAttempts(String username) throws AWException {
    DataList dataList = queryService.launchPrivateQuery(FAILED_ATTEMPTS_QUERY, parameters(username)).getDataList();
    if (dataList == null || dataList.getRows().isEmpty()) {
      return null;
    }
    Map<String, CellData> row = dataList.getRows().getFirst();
    return toInteger(row.get("value"));
  }

  private static int toInteger(CellData value) {
    if (value == null || value.getValue() == null) {
      return 0;
    }
    Integer number = value.getIntegerValue();
    if (number != null) {
      return number;
    }
    try {
      return Integer.parseInt(value.getStringValue().trim());
    } catch (NumberFormatException exc) {
      return 0;
    }
  }

  private static ObjectNode parameters(String username) {
    return JsonNodeFactory.instance.objectNode().put(USER, username);
  }
}
