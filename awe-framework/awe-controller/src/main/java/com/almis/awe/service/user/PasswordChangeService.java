package com.almis.awe.service.user;

import com.almis.awe.config.AuthType;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.service.MaintainService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.util.WebUtils;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.almis.awe.model.constant.AweConstants.JSON_SCREEN;
import static com.almis.awe.model.constant.AweConstants.SCREEN;
import static com.almis.awe.model.constant.AweConstants.SESSION_CONNECTION_TOKEN;

/**
 * Password change of a user whose password has expired.
 * <p>
 * A user with an expired password cannot get an authenticated session. When the login verifies the password and finds
 * it expired, the login handler calls {@link #allowPasswordChange(HttpSession, String)}, which keeps a short-lived
 * request in the server side HTTP session. That request is bound to the user who proved to know the password and it
 * allows only {@link #changeExpiredPassword(String)}: it grants no authentication. Once the password is changed, the
 * request is discarded (single use) and the user has to sign in with the new password through the regular login.
 */
@Slf4j
public class PasswordChangeService extends ServiceConfig {

  /**
   * HTTP session attribute that keeps the pending password change
   */
  public static final String SESSION_ATTRIBUTE = "AWE_EXPIRED_PASSWORD_CHANGE";

  /**
   * Maintain that stores the new password of the user (not public: it is only launched from this service)
   */
  public static final String UPDATE_MAINTAIN = "ChdPwdExpiredUpdate";

  /**
   * Screen where the expired password is changed
   */
  public static final String PASSWORD_CHANGE_SCREEN = "change-expired-password";

  /**
   * Time a password change request stays valid
   */
  public static final Duration VALIDITY = Duration.ofMinutes(5);

  /** Session parameter with the user of the update (set by this service only, read by the update maintain) */
  public static final String UPDATE_USER_SESSION_PARAMETER = "expiredPasswordUser";
  private static final String PASSWORD_PARAMETER = "Pas";
  private static final String ERROR_TITLE_PASSWORD_CHANGE = "ERROR_TITLE_PASSWORD_CHANGE";

  private final SecurityConfigProperties securityConfigProperties;
  private final AweUserDetailService userDetailService;
  private final MaintainService maintainService;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  /**
   * Autowired constructor
   *
   * @param securityConfigProperties Security configuration properties
   * @param userDetailService        User detail service
   * @param maintainService          Maintain service
   * @param passwordEncoder          Encoder used to check the passwords of the local users
   */
  public PasswordChangeService(SecurityConfigProperties securityConfigProperties, AweUserDetailService userDetailService,
                               MaintainService maintainService, PasswordEncoder passwordEncoder) {
    this(securityConfigProperties, userDetailService, maintainService, passwordEncoder, Clock.systemUTC());
  }

  PasswordChangeService(SecurityConfigProperties securityConfigProperties, AweUserDetailService userDetailService,
                        MaintainService maintainService, PasswordEncoder passwordEncoder, Clock clock) {
    this.securityConfigProperties = securityConfigProperties;
    this.userDetailService = userDetailService;
    this.maintainService = maintainService;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  /**
   * Allow the change of the expired password of a user in a session. Call it only after the password has been
   * verified by the authentication manager.
   *
   * @param session  HTTP session of the login attempt
   * @param username User whose password has been verified and is expired
   * @return true if the user can change the password now (only users with a local password can)
   */
  public boolean allowPasswordChange(HttpSession session, String username) {
    if (securityConfigProperties.getAuthMode() != AuthType.BBDD) {
      return false;
    }
    // A new failed login replaces whatever request was pending in the session
    synchronized (WebUtils.getSessionMutex(session)) {
      session.setAttribute(SESSION_ATTRIBUTE, new PasswordChangeRequest(username, clock.instant().plus(VALIDITY)));
    }
    return true;
  }

  /**
   * Allow the change of the expired password of a user, binding the request to a new session id so that a session id known
   * before the login cannot be used to change the password (session fixation)
   *
   * @param request  HTTP request of the login attempt
   * @param username User whose password has been verified and is expired
   * @return true if the user can change the password now (only users with a local password can)
   */
  public boolean allowPasswordChange(HttpServletRequest request, String username) {
    if (securityConfigProperties.getAuthMode() != AuthType.BBDD) {
      return false;
    }
    HttpSession session = request.getSession(true);
    request.changeSessionId();
    return allowPasswordChange(session, username);
  }

  /**
   * Discard the pending password change of a session. Call it on every authentication attempt that does not
   * create a new pending change, so that a request never survives into another login
   *
   * @param session HTTP session (can be null)
   */
  public void clearPasswordChange(HttpSession session) {
    if (session != null) {
      synchronized (WebUtils.getSessionMutex(session)) {
        session.removeAttribute(SESSION_ATTRIBUTE);
      }
    }
  }

  /**
   * Client action that takes the user to the screen where the expired password is changed
   *
   * @return Screen client action
   */
  public ClientAction getPasswordChangeScreenAction() {
    return new ClientAction(SCREEN)
      .setContext(SCREEN)
      .addParameter(JSON_SCREEN, PASSWORD_CHANGE_SCREEN);
  }

  /**
   * Service: change the expired password of the user bound to the current session
   *
   * @param password New password
   * @return Service data with the result and the redirection to the sign in screen
   * @throws AWException The request is not valid or the password does not meet the policy
   */
  public ServiceData changeExpiredPassword(String password) throws AWException {
    return changePassword(getRequest() == null ? null : getRequest().getHttpRequest().getSession(false), password);
  }

  /**
   * Change the expired password of the user bound to a session
   *
   * @param session  HTTP session
   * @param password New password
   * @return Service data with the result and the redirection to the sign in screen
   * @throws AWException The request is not valid or the password does not meet the policy
   */
  public ServiceData changePassword(HttpSession session, String password) throws AWException {
    // Take the request in one step: it is single use, so two concurrent calls cannot both change the password
    PasswordChangeRequest request = takePendingRequest(session);
    AweUserDetails user = getPendingUser(request);

    // Strictly single use: any error from here on, including the policy ones, spends the request and the user signs in again
    checkPasswordPolicy(password);
    if (passwordEncoder.matches(password, user.getPassword())) {
      throw new AWException(getLocale(ERROR_TITLE_PASSWORD_CHANGE), getLocale("ERROR_MESSAGE_PASSWORD_REPEATED"), AnswerType.WARNING);
    }

    // From now on the request is spent, whatever the outcome is
    // The maintain is reachable through /action/maintain by any signed in user, so it never reads the user from the request:
    // it reads it from this session attribute, which only the server sets and which lives only during the update
    AweSession aweSession = getSession();
    if (aweSession == null) {
      throw notAllowed();
    }
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.put(PASSWORD_PARAMETER, password);
    aweSession.setParameter(UPDATE_USER_SESSION_PARAMETER, user.getUsername());
    try {
      maintainService.launchPrivateMaintain(UPDATE_MAINTAIN, parameters);
    } finally {
      aweSession.removeParameter(UPDATE_USER_SESSION_PARAMETER);
    }
    log.info("[PASSWORD CHANGE] The expired password of user {} has been changed", user.getUsername());

    return new ServiceData()
      .setType(AnswerType.OK)
      .setTitle(getLocale("OK_TITLE_EXPIRED_PASSWORD_CHANGED"))
      .setMessage(getLocale("OK_MESSAGE_EXPIRED_PASSWORD_CHANGED"))
      .addClientAction(new ClientAction(SCREEN)
        .addParameter(SESSION_CONNECTION_TOKEN, UUID.randomUUID())
        .setTarget("/"));
  }

  /**
   * Remove the pending request of the session and return it (atomically, per session)
   */
  private PasswordChangeRequest takePendingRequest(HttpSession session) throws AWException {
    if (session == null) {
      throw notAllowed();
    }
    Object attribute;
    synchronized (WebUtils.getSessionMutex(session)) {
      attribute = session.getAttribute(SESSION_ATTRIBUTE);
      session.removeAttribute(SESSION_ATTRIBUTE);
    }
    if (!(attribute instanceof PasswordChangeRequest request)) {
      throw notAllowed();
    }
    return request;
  }

  /**
   * Retrieve the user of a taken request, which stays discarded when it is not valid anymore
   */
  private AweUserDetails getPendingUser(PasswordChangeRequest request) throws AWException {
    // Only the local authentication keeps the passwords in the AWE user table
    if (request.expiresAt().isBefore(clock.instant()) || securityConfigProperties.getAuthMode() != AuthType.BBDD) {
      throw notAllowed();
    }

    AweUserDetails user = userDetailService.loadUserByUsername(request.username());
    if (user == null || !user.isEnabled() || !user.isAccountNonLocked()) {
      throw notAllowed();
    }
    return user;
  }

  private void checkPasswordPolicy(String password) throws AWException {
    boolean valid = StringUtils.isNotBlank(password) && password.length() >= securityConfigProperties.getPasswordMinLength();
    if (valid) {
      try {
        // Same semantics as the pattern validation of the clients (the pattern has to be found in the password)
        valid = Pattern.compile(securityConfigProperties.getPasswordPattern()).matcher(password).find();
      } catch (PatternSyntaxException exc) {
        log.error("[PASSWORD CHANGE] The password pattern is not a valid regular expression", exc);
        valid = false;
      }
    }
    if (!valid) {
      throw new AWException(getLocale(ERROR_TITLE_PASSWORD_CHANGE), getLocale("ERROR_MESSAGE_PASSWORD_POLICY"), AnswerType.WARNING);
    }
  }

  private AWException notAllowed() {
    return new AWException(getLocale(ERROR_TITLE_PASSWORD_CHANGE), getLocale("ERROR_MESSAGE_PASSWORD_CHANGE_NOT_ALLOWED"), AnswerType.WARNING);
  }

  /**
   * Pending password change: the user who verified the expired password and the moment the request expires
   */
  record PasswordChangeRequest(String username, Instant expiresAt) implements Serializable {
  }
}
