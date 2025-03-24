package com.almis.awe.listener;

import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.tracker.AweConnectionTracker;
import com.almis.awe.model.type.LaunchPhaseType;
import com.almis.awe.model.util.security.mapper.mapper.AuthenticateUserMapper;
import com.almis.awe.service.InitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Event when websocket is connected
 */
@Slf4j
public class WebSocketEventListener {

  // Autowired services
  private final InitService initService;
  private final AweConnectionTracker connectionTracker;

  /**
   * Autowired constructor
   *
   * @param initService Init service
   */
  public WebSocketEventListener(InitService initService, AweConnectionTracker connectionTracker) {
    this.initService = initService;
    this.connectionTracker = connectionTracker;
  }

  /**
   * On connect event
   *
   * @param event Session connect event
   */
  @EventListener
  public void onConnectEvent(SessionConnectEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String user = AuthenticateUserMapper.getUserFromAuthentication((Authentication) event.getUser());
    log.debug("[WebSocket Connect Event] User: {}", user);
    if (user != null) {
      String token = Objects.requireNonNull(accessor.getNativeHeader(AweConstants.SESSION_CONNECTION_HEADER)).get(0);
      connectionTracker.initializeUserConnections(user, token, getSessionId(accessor.getSessionAttributes()));
    }
  }

  private String getSessionId(Map<String, Object> sessionAttributes) {
    return (String) Optional.ofNullable(sessionAttributes.get("SPRING.SESSION.ID"))
            .orElse(sessionAttributes.get("HTTP.SESSION.ID"));
  }

  /**
   * On connected event
   *
   * @param event websocket event
   */
  @EventListener
  public void onConnectedEvent(SessionConnectedEvent event) {
    log.debug("[WebSocket Connected Event]");
    StompHeaderAccessor.wrap(event.getMessage());
  }

  /**
   * On disconnect event
   *
   * @param event websocket disconnect event
   */
  @EventListener
  public void onDisconnectEvent(SessionDisconnectEvent event) {
    String user = AuthenticateUserMapper.getUserFromAuthentication((Authentication) event.getUser());
    log.debug("[WebSocket Disconnect Event] User: {}", user);
    StompHeaderAccessor.wrap(event.getMessage());

    // Launch client end services
    initService.launchPhaseServices(LaunchPhaseType.CLIENT_END);
  }
}
