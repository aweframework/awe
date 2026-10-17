package com.almis.awe.security.audit;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditEvent;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.model.security.audit.SecurityAuditOutcome;
import com.almis.awe.service.QueryService;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Security audit of the user account changes made through the generic screens: lock, unlock and password reset by an
 * administrator, password changes and second factor resets. Called from the maintain descriptors with
 * {@code <serve>}; the events are published once the maintain transaction commits.
 */
public class UserAccountAuditService extends ServiceConfig {

  /** Query reading the user name and lock flag of users by identifier */
  public static final String USER_STATUS_QUERY = "UsrGetSecSta";
  /** Query checking that a user has a password */
  public static final String PASSWORD_CHECK_QUERY = "UsrChkPwd";

  private final SecurityAuditService securityAuditService;
  private final QueryService queryService;
  private final LoginAttemptService loginAttemptService;

  /**
   * Constructor
   *
   * @param securityAuditService Security audit service
   * @param queryService         Query service
   * @param loginAttemptService  Login attempt service
   */
  public UserAccountAuditService(SecurityAuditService securityAuditService, QueryService queryService,
                                 LoginAttemptService loginAttemptService) {
    this.securityAuditService = securityAuditService;
    this.queryService = queryService;
    this.loginAttemptService = loginAttemptService;
  }

  /**
   * An administrator updates a user: launched before the update, it compares the stored lock flag with the new one.
   * A password reset also unlocks the user. Unlocking clears the failed logins of the automatic lockout.
   *
   * @param userId         User identifier
   * @param locked         New lock flag (1 locked)
   * @param passwordUpdate 1 when the password is reset
   * @return Empty service data
   * @throws AWException Error reading the user
   */
  public ServiceData auditUserUpdate(Integer userId, Integer locked, Integer passwordUpdate) throws AWException {
    if (!securityAuditService.isEnabled() && !loginAttemptService.isEnabled() || userId == null) {
      return new ServiceData();
    }
    List<UserStatus> users = userStatus(List.of(userId));
    if (users.isEmpty()) {
      return new ServiceData();
    }
    UserStatus user = users.getFirst();
    boolean reset = Integer.valueOf(1).equals(passwordUpdate);
    boolean nowLocked = !reset && Integer.valueOf(1).equals(locked);
    if (user.locked() && !nowLocked) {
      loginAttemptService.resetFailures(user.name());
      administratorEvent(SecurityAuditEventType.ACCOUNT_UNLOCKED, user.name());
    } else if (!user.locked() && nowLocked) {
      administratorEvent(SecurityAuditEventType.ACCOUNT_LOCKED, user.name());
    }
    if (reset) {
      administratorEvent(SecurityAuditEventType.PASSWORD_RESET, user.name());
    }
    return new ServiceData();
  }

  /**
   * A user changed the password: launched after the update, it checks that the new password is stored (a wrong
   * current password updates nothing).
   *
   * @param user     User name
   * @param password New password
   * @return Empty service data
   * @throws AWException Error reading the user
   */
  public ServiceData auditPasswordChange(String user, String password) throws AWException {
    if (!securityAuditService.isEnabled() || user == null) {
      return new ServiceData();
    }
    ObjectNode parameters = JsonNodeFactory.instance.objectNode().put("Usr", user.trim()).put("Pas", password);
    DataList result = queryService.launchPrivateQuery(PASSWORD_CHECK_QUERY, parameters).getDataList();
    if (result != null && !result.getRows().isEmpty()) {
      securityAuditService.success(SecurityAuditEventType.PASSWORD_CHANGE, user.trim(), null);
    } else {
      securityAuditService.failure(SecurityAuditEventType.PASSWORD_CHANGE, user.trim(), SecurityAuditCause.BAD_CREDENTIALS, null);
    }
    return new ServiceData();
  }

  /**
   * An administrator reset the second factor of some users
   *
   * @param userIds User identifiers
   * @return Empty service data
   * @throws AWException Error reading the users
   */
  public ServiceData auditTotpReset(List<Integer> userIds) throws AWException {
    if (!securityAuditService.isEnabled() || userIds == null || userIds.isEmpty()) {
      return new ServiceData();
    }
    for (UserStatus user : userStatus(userIds)) {
      administratorEvent(SecurityAuditEventType.TOTP_RESET, user.name());
    }
    return new ServiceData();
  }

  private void administratorEvent(SecurityAuditEventType type, String username) {
    String administrator = getSession() == null ? null : getSession().getUser();
    securityAuditService.publish(SecurityAuditEvent.builder()
      .type(type)
      .outcome(SecurityAuditOutcome.SUCCESS)
      .cause(SecurityAuditCause.ADMINISTRATOR)
      .username(username)
      .detail(administrator == null ? null : "administrator: " + administrator)
      .build());
  }

  private List<UserStatus> userStatus(List<Integer> userIds) throws AWException {
    ArrayNode ids = JsonNodeFactory.instance.arrayNode();
    userIds.forEach(ids::add);
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.set("IdeOpe", ids);
    DataList result = queryService.launchPrivateQuery(USER_STATUS_QUERY, parameters).getDataList();
    List<UserStatus> users = new ArrayList<>();
    if (result != null) {
      for (Map<String, CellData> row : result.getRows()) {
        CellData name = row.get("user");
        CellData locked = row.get("locked");
        if (name != null && name.getStringValue() != null) {
          users.add(new UserStatus(name.getStringValue().trim(), locked != null && "1".equals(String.valueOf(locked.getStringValue()).trim())));
        }
      }
    }
    return users;
  }

  private record UserStatus(String name, boolean locked) {
  }
}
