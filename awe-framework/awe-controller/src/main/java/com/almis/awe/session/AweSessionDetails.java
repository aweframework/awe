package com.almis.awe.session;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.config.SessionConfigProperties;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.tracker.AweClientTracker;
import com.almis.awe.model.tracker.AweConnectionTracker;
import com.almis.awe.service.BroadcastService;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.Map;

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
  private final SessionConfigProperties sessionConfigProperties;

  /**
   * Autowired constructor
   *  @param aweClientTracker         awe client tracker
   * @param queryService             query service
   * @param connectionTracker        connection tracker
   * @param broadcastService         Broadcasting service
   * @param sessionConfigProperties  Session properties
   */
  public AweSessionDetails(AweClientTracker aweClientTracker, QueryService queryService, AweConnectionTracker connectionTracker, BroadcastService broadcastService, SessionConfigProperties sessionConfigProperties) {
    this.clientTracker = aweClientTracker;
    this.queryService = queryService;
    this.connectionTracker = connectionTracker;
    this.broadcastService = broadcastService;
    this.sessionConfigProperties = sessionConfigProperties;
  }

  /**
   * Manage login success
   */
  public void onLoginSuccess() {
    AweUserDetails userDetails = (AweUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    // Set user as fully authenticated
    userDetails.setFullyAuthenticated(true);

    // Store user in session
    getSession().setParameter(SESSION_USER, userDetails.getUsername());

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
}
