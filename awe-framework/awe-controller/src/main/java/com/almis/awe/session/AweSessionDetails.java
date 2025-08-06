package com.almis.awe.session;

import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.config.SessionConfigProperties;
import com.almis.awe.exception.AWERuntimeException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.tracker.AweClientTracker;
import com.almis.awe.model.tracker.AweConnectionTracker;
import com.almis.awe.service.BroadcastService;
import com.almis.awe.service.MenuService;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static com.almis.awe.model.constant.AweConstants.*;

/**
 * Session details
 */
@Slf4j
public class AweSessionDetails extends ServiceConfig {

  // Autowired services
  private final AweClientTracker clientTracker;
  private final QueryService queryService;
  private final SessionService sessionService;
  private final AweConnectionTracker connectionTracker;
  private final BroadcastService broadcastService;
  private final SessionConfigProperties sessionConfigProperties;
  private final SecurityConfigProperties securityConfigProperties;
  private final MenuService menuService;

  /**
   * Autowired constructor
   *
   * @param aweClientTracker         awe client tracker
   * @param queryService             query service
   * @param sessionService           session service
   * @param connectionTracker        connection tracker
   * @param broadcastService         Broadcasting service
   * @param menuService              Menu service
   * @param sessionConfigProperties  Session properties
   * @param securityConfigProperties Security properties
   */
  @Autowired
  public AweSessionDetails(AweClientTracker aweClientTracker, QueryService queryService, SessionService sessionService,
													 AweConnectionTracker connectionTracker, BroadcastService broadcastService,  MenuService menuService, SessionConfigProperties sessionConfigProperties, SecurityConfigProperties securityConfigProperties) {
    this.clientTracker = aweClientTracker;
    this.queryService = queryService;
    this.sessionService = sessionService;
    this.connectionTracker = connectionTracker;
    this.broadcastService = broadcastService;
    this.menuService = menuService;
    this.sessionConfigProperties = sessionConfigProperties;
		this.securityConfigProperties = securityConfigProperties;
	}

  /**
   * Manage login success
   */
  public void onLoginSuccess(AweUserDetails userDetails) {

    // Set user as fully authenticated
    if (userDetails.isEnabled2fa()) {
      userDetails.setFullyAuthenticated(true);
    }

    // Store user in session
    sessionService.setSessionParameter(SESSION_USER, userDetails.getUsername());

    // Store user details
    storeUserDetails(userDetails);

    // Initialize session variables
    initializeSessionVariables();
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
            .forEach(c -> broadcastService.broadcastMessageToUID(c, createLogoutRedirectAction())
            );

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
          sessionService.setSessionParameter(paramName, row.get(AweConstants.JSON_VALUE_PARAMETER).getStringValue());
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
  private void storeUserDetails(AweUserDetails userDetails) {
    AweSession session = getSession();
    Assert.notNull(userDetails, "User details must not be null");

    // Store user data in session
    session.setParameter(SESSION_LAST_LOGIN, new Date());
    session.setParameter(SESSION_FULLNAME, userDetails.getName());
    session.setParameter(SESSION_LANGUAGE, userDetails.getLanguage());
    session.setParameter(SESSION_THEME, userDetails.getTheme());
    session.setParameter(SESSION_PROFILE, userDetails.getProfile());
    session.setParameter(SESSION_RESTRICTION, userDetails.getRestrictions());
    session.setParameter(SESSION_INITIAL_SCREEN, userDetails.getInitialScreen());
    session.setParameter(SESSION_TOKEN, getRequest().getToken());
  }

  /**
   * Creates and returns a {@link ClientAction} object configured to handle logout redirection.
   * The configuration varies based on the system's SSO (Single Sign-On) settings.
   * <p>
   * If SSO is enabled and auto-launch is configured, the action will redirect to a specific
   * logout SSO target and set the appropriate screen context from the public menu.
   * If SSO is not enabled or auto-launch is not configured, the action will redirect to the root URL.
   *
   * @return a configured {@link ClientAction} object for logout redirection
   */
  public ClientAction createLogoutRedirectAction() {

    try {
      ClientAction screenActionBuilder = new ClientAction(SCREEN)
          .addParameter(SESSION_CONNECTION_TOKEN, UUID.randomUUID());

      if (securityConfigProperties.getSso().isEnabled() && securityConfigProperties.getSso().isAutoLaunch()) {
        Menu menu = menuService.getMenu(PUBLIC_MENU);
        screenActionBuilder.setContext(menu.getScreenContext());
        screenActionBuilder.setTarget("logout-sso");
      } else {
        screenActionBuilder.setTarget("/");
      }
      return screenActionBuilder;

    } catch (AWException exc) {
      throw new AWERuntimeException(exc);
    }
  }
}
