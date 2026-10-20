package com.almis.awe.dao;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.User;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * User data implementation
 */
@Slf4j
public class UserDAOImpl extends ServiceConfig implements UserDAO {

  /** Maintain target that stores a rehashed password */
  public static final String REHASH_MAINTAIN = "UsrRehashPwd";
  /** Session parameter with the user whose password is rehashed (only set while the target runs) */
  public static final String REHASH_USER_SESSION_PARAMETER = "passwordRehashUser";
  /** Session parameter with the new password hash (only set while the target runs) */
  public static final String REHASH_VALUE_SESSION_PARAMETER = "passwordRehashValue";

  // Query service
  private final QueryUtil queryUtil;
  private final QueryService queryService;

  /**
   * Autowired constructor
   *
   * @param queryUtil            QueryUtil service
   * @param queryService         Query service
   */
  public UserDAOImpl(QueryUtil queryUtil, QueryService queryService) {
    this.queryUtil = queryUtil;
    this.queryService = queryService;
  }

  @Override
  public User findByUserName(String userName) {
    try {
      // Get user details from database
      return DataListUtil.asBeanList(queryService.launchPrivateQuery(AweConstants.USER_DETAIL_QUERY, queryUtil.getParameters()
          .put("user", userName)).getDataList(), User.class)
        .stream()
        .findFirst()
        .orElseThrow(() -> new UsernameNotFoundException(userName));
    } catch (Exception exc) {
      log.error("Error retrieving user details", exc);
      throw new UsernameNotFoundException(userName, exc);
    }
  }

  public User findByEmail(String email) {
    User userInfo = null;
    try {
      // Get user details from database
      userInfo = DataListUtil.asBeanList(queryService.launchPrivateQuery(AweConstants.USER_DETAIL_EMAIL_QUERY, queryUtil.getParameters()
              .put("email", email)).getDataList(), User.class)
          .stream()
          .findFirst()
          .orElse(null);
    } catch (Exception exc) {
      log.error("It has not been possible to recover the user details from email {}", email, exc);
    }
    return userInfo;
  }

  @Override
  public User findByRole(String profile) {
    try {
      // Get profile details from database
      return DataListUtil.asBeanList(queryService.launchPrivateQuery(AweConstants.USER_DETAIL_PROFILE_QUERY, queryUtil.getParameters()
              .put("profile", profile)).getDataList(), User.class)
          .stream()
          .findFirst()
          .orElseThrow();
    } catch (Exception exc) {
      log.error("Error retrieving profile {} details. Check application configuration", profile);
      return new User();
    }
  }

  @Override
  public boolean existRole(String role) throws AWException {
    return queryService.launchPrivateQuery(AweConstants.CHECK_EXIST_PROFILE,
        queryUtil.getParameters().put("profile", role)).getDataList().getRecords() == 1;
  }

  @Override
  public boolean updatePassword(String userName, String encodedPassword) {
    // The target is reachable through /action/maintain by any signed in user, so it never reads the user or the hash
    // from the request: it reads them from these session parameters, which only the server sets and which live only
    // during the update
    AweSession aweSession = getSession();
    if (aweSession == null) {
      return false;
    }
    aweSession.setParameter(REHASH_USER_SESSION_PARAMETER, userName);
    aweSession.setParameter(REHASH_VALUE_SESSION_PARAMETER, encodedPassword);
    try {
      getBean(MaintainService.class).launchPrivateMaintain(REHASH_MAINTAIN, JsonNodeFactory.instance.objectNode());
      return true;
    } catch (Exception exc) {
      // The exception is not logged: its message may carry the new hash
      log.debug("Error storing the rehashed password of user {}: {}", userName, exc.getClass().getSimpleName());
      return false;
    } finally {
      aweSession.removeParameter(REHASH_USER_SESSION_PARAMETER);
      aweSession.removeParameter(REHASH_VALUE_SESSION_PARAMETER);
    }
  }
}
