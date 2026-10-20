package com.almis.awe.service.user;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.dao.UserDAO;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.dto.User;
import com.almis.awe.service.MaintainService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Password changes that require the current password of the user (change password screens and user settings).
 * <p>
 * Password hashes are salted, so the current password cannot be compared in SQL: it is checked here against the stored
 * hash with {@link PasswordEncoder#matches}. Only then the update target is launched, and the target reads the user
 * from a session parameter that only this service sets while it runs: any signed in user can launch a target through
 * /action/maintain (and anybody a public one), so the update targets never take the user from the request.
 * <p>
 * A wrong current password updates nothing and answers like a right one (the maintain fills its default message), as
 * the targets did when they compared the password in SQL.
 */
@Slf4j
public class CurrentPasswordChangeService extends ServiceConfig {

  /** Maintain that stores the new password of an active user (change password screens, without session) */
  public static final String PUBLIC_UPDATE_MAINTAIN = "ChdPwdPubUpdate";
  /** Maintain that stores the new password of the user in session (user settings) */
  public static final String OWN_UPDATE_MAINTAIN = "UsrUpdPwdUpdate";
  /** Session parameter with the user whose current password has been verified (only set while the update runs) */
  public static final String USER_SESSION_PARAMETER = "verifiedPasswordUser";
  private static final String PASSWORD_PARAMETER = "Pas";

  private final UserDAO userDAO;
  private final PasswordEncoder passwordEncoder;

  /**
   * Autowired constructor
   *
   * @param userDAO         User DAO
   * @param passwordEncoder Password encoder
   */
  public CurrentPasswordChangeService(UserDAO userDAO, PasswordEncoder passwordEncoder) {
    this.userDAO = userDAO;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Service: change the password of an active user who knows the current one (no session required)
   *
   * @param user            User name
   * @param currentPassword Current password
   * @param password        New password
   * @return Empty service data, whatever the outcome
   * @throws AWException Error storing the new password
   */
  public ServiceData changePassword(String user, String currentPassword, String password) throws AWException {
    User storedUser = getVerifiedUser(user, currentPassword);
    if (storedUser != null && storedUser.isEnabled()) {
      updatePassword(PUBLIC_UPDATE_MAINTAIN, storedUser.getUsername(), password);
    }
    return new ServiceData();
  }

  /**
   * Service: change the password of the user in session, who knows the current one
   *
   * @param currentPassword Current password
   * @param password        New password
   * @return Empty service data, whatever the outcome
   * @throws AWException Error storing the new password
   */
  public ServiceData changeOwnPassword(String currentPassword, String password) throws AWException {
    AweSession aweSession = getSession();
    User storedUser = aweSession == null ? null : getVerifiedUser(aweSession.getUser(), currentPassword);
    if (storedUser != null) {
      updatePassword(OWN_UPDATE_MAINTAIN, storedUser.getUsername(), password);
    }
    return new ServiceData();
  }

  /**
   * Retrieve a user when the password is the current one
   *
   * @param user     User name
   * @param password Password
   * @return Stored user, or null when the user does not exist or the password is not the current one
   */
  private User getVerifiedUser(String user, String password) {
    if (StringUtils.isBlank(user)) {
      return null;
    }
    User storedUser;
    try {
      storedUser = userDAO.findByUserName(user);
    } catch (UsernameNotFoundException exc) {
      return null;
    }
    if (storedUser == null || !passwordEncoder.matches(password, storedUser.getPassword())) {
      log.debug("[PASSWORD CHANGE] The current password of user {} has not been verified", user);
      return null;
    }
    return storedUser;
  }

  /**
   * Launch the update target with the verified user bound to the session only while it runs
   *
   * @param target   Update target
   * @param user     Verified user
   * @param password New password
   * @throws AWException Error storing the new password
   */
  private void updatePassword(String target, String user, String password) throws AWException {
    AweSession aweSession = getSession();
    if (aweSession == null) {
      return;
    }
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.put(PASSWORD_PARAMETER, password);
    aweSession.setParameter(USER_SESSION_PARAMETER, user);
    try {
      getBean(MaintainService.class).launchPrivateMaintain(target, parameters);
    } finally {
      aweSession.removeParameter(USER_SESSION_PARAMETER);
    }
    log.info("[PASSWORD CHANGE] The password of user {} has been changed", user);
  }
}
