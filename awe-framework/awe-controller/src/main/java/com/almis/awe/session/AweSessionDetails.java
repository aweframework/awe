package com.almis.awe.session;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.config.SessionConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.dto.User;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.tracker.AweClientTracker;
import com.almis.awe.model.tracker.AweConnectionTracker;
import com.almis.awe.service.BroadcastService;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.almis.awe.model.constant.AweConstants.*;

/**
 * Session details
 */
@Slf4j
public class AweSessionDetails extends ServiceConfig {

  // Autowired services
  private final AweClientTracker clientTracker;
  private final QueryService queryService;
  private final AweConnectionTracker connectionTracker;
  private final BroadcastService broadcastService;
  private final BaseConfigProperties baseConfigProperties;
  private final SecurityConfigProperties securityConfigProperties;
  private final SessionConfigProperties sessionConfigProperties;

  /**
   * Autowired constructor
   *
   * @param aweClientTracker         awe client tracker
   * @param queryService             query service
   * @param connectionTracker        connection tracker
   * @param broadcastService         Broadcasting service
   * @param baseConfigProperties     Base properties
   * @param securityConfigProperties Security properties
   * @param sessionConfigProperties  Session properties
   */
  public AweSessionDetails(AweClientTracker aweClientTracker, QueryService queryService, AweConnectionTracker connectionTracker, BroadcastService broadcastService, BaseConfigProperties baseConfigProperties, SecurityConfigProperties securityConfigProperties, SessionConfigProperties sessionConfigProperties) {
    this.clientTracker = aweClientTracker;
    this.queryService = queryService;
    this.connectionTracker = connectionTracker;
    this.broadcastService = broadcastService;
    this.baseConfigProperties = baseConfigProperties;
    this.securityConfigProperties = securityConfigProperties;
    this.sessionConfigProperties = sessionConfigProperties;
  }

  /**
   * Manage login success
   */
  public void onLoginSuccess() {
    AweSession session = getSession();

    // Store user in session
    session.setParameter(SESSION_USER, session.getUser());

    // Store user details
    storeUserDetails();

    // Initialize session variables
    initializeSessionVariables();
  }

  /**
   * Manage login failure
   *
   * @param exc Authentication exception
   */
  public void onLoginFailure(AuthenticationException exc) {
    AweSession session = getSession();
    String username = getRequest().getParameterAsString(baseConfigProperties.getParameter().getUsername());
    if (exc instanceof UsernameNotFoundException) {
      session.setParameter(SESSION_FAILURE, new AWException(getLocale("ERROR_TITLE_INVALID_USER"), getLocale("ERROR_MESSAGE_INVALID_USER", username), exc));
    } else if (exc instanceof BadCredentialsException) {
      session.setParameter(SESSION_FAILURE, new AWException(getLocale("ERROR_TITLE_INVALID_CREDENTIALS"), getLocale("ERROR_MESSAGE_INVALID_CREDENTIALS", username), exc));
    } else {
      session.setParameter(SESSION_FAILURE, new AWException(getLocale("ERROR_TITLE_INVALID_CREDENTIALS"), exc.getMessage(), exc));
    }
  }

  /**
   * Manage logout
   */
  private void onBeforeLogout() {
    // Close client tracker
    clientTracker.removeObservers();

    // Get user
    String user = getSession().getUser();

    // Send logout broadcast to other connections
    connectionTracker.getUserConnectionsFromSession(user, getSession().getSessionId())
      .stream()
      .filter(StringUtils::isNotBlank)
      .filter(c -> !c.equalsIgnoreCase(getRequest().getToken()))
      .forEach(c -> broadcastService.broadcastMessageToUID(c, new ClientAction("logout")));

    // Remove cometUID from user session
    connectionTracker.removeAllConnectionsFromUserSession(user, getSession().getSessionId());
  }

  /**
   * Manage logout success
   */
  public void onLogoutSuccess() {
    AweSession session = getSession();

    // Call oBeforeLogout
    onBeforeLogout();

    // Remove from session
    session.removeParameter(SESSION_USER_DETAILS);
    session.removeParameter(SESSION_LAST_LOGIN);
    session.removeParameter(SESSION_FULLNAME);
    session.removeParameter(SESSION_LANGUAGE);
    session.removeParameter(SESSION_THEME);
    session.removeParameter(SESSION_PROFILE);
    session.removeParameter(SESSION_RESTRICTION);
    session.removeParameter(SESSION_INITIAL_SCREEN);
    session.removeParameter(SESSION_INITIAL_URL);
    session.removeParameter(SESSION_TOKEN);
  }

  /**
   * Store user details
   */
  private void initializeSessionVariables() {
    sessionConfigProperties.getParameter().forEach((paramName, queryName) -> {
      try {
        ServiceData queryOutput = queryService.launchQuery(queryName, "1", "0");
        DataList queryData = queryOutput.getDataList();
        if (queryData != null && !queryData.getRows().isEmpty()) {
          Map<String, CellData> row = queryData.getRows().get(0);
          getSession().setParameter(paramName, row.get(AweConstants.JSON_VALUE_PARAMETER).getStringValue());
        }
      } catch (Exception exc) {
        log.error("There has been an error trying to retrieve the session parameter '{}'", paramName, exc);
        getSession().setParameter(SESSION_FAILURE, exc);
    }
    });
  }

  /**
   * Store user details
   */
  private void storeUserDetails() {
    AweSession session = getSession();
    User userDetails = session.getParameter(User.class, SESSION_USER_DETAILS);

    Assert.notNull(userDetails, "User details must not be null. Check if the authentication provider saves user information in session");

    // Specific language
    String language = Optional.ofNullable(userDetails.getLanguage())
      .filter(StringUtils::isNotBlank)
      .orElse(baseConfigProperties.getLanguageDefault())
      .substring(0, 2).toLowerCase();

    // Specific restriction
    String theme = Stream.of(userDetails.getUserTheme(), userDetails.getProfileTheme())
      .filter(StringUtils::isNotBlank)
      .findFirst().orElse(baseConfigProperties.getTheme());

    // Specific restriction
    String restriction = Stream.of(userDetails.getUserFileRestriction(), userDetails.getProfileFileRestriction())
      .filter(StringUtils::isNotBlank)
      .findFirst().orElse(securityConfigProperties.getDefaultRestriction());

    // Specific initial screen
    String initialScreen = Stream.of(userDetails.getUserInitialScreen(), userDetails.getProfileInitialScreen())
      .filter(StringUtils::isNotBlank)
      .findFirst().orElse(baseConfigProperties.getScreen().getInitial());

    // Store user data in session
    session.setParameter(SESSION_LAST_LOGIN, new Date());
    session.setParameter(SESSION_FULLNAME, userDetails.getUserFullName());
    session.setParameter(SESSION_LANGUAGE, language);
    session.setParameter(SESSION_THEME, theme);
    session.setParameter(SESSION_PROFILE, userDetails.getProfile());
    session.setParameter(SESSION_RESTRICTION, restriction);
    session.setParameter(SESSION_INITIAL_SCREEN, initialScreen);
    session.setParameter(SESSION_TOKEN, getRequest().getToken());
  }
}
