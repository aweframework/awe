package com.almis.awe.autoconfigure;

import com.almis.awe.autoconfigure.config.WebsocketStompConfigProperties;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.listener.WebSocketEventListener;
import com.almis.awe.model.tracker.AweClientTracker;
import com.almis.awe.model.tracker.AweConnectionTracker;
import com.almis.awe.service.BroadcastService;
import com.almis.awe.service.InitService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.session.Session;
import org.springframework.session.web.socket.config.annotation.AbstractSessionWebSocketMessageBrokerConfigurer;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

/**
 * Awe Web Socket configuration.
 *
 * @author mvelez
 */
@Configuration
@EnableConfigurationProperties({BaseConfigProperties.class, WebsocketStompConfigProperties.class})
@EnableWebSocketMessageBroker
@Slf4j
public class WebsocketConfig extends AbstractSessionWebSocketMessageBrokerConfigurer<Session> {

  // Autowired components
  private final BaseConfigProperties baseConfigProperties;
  private final SecurityConfigProperties securityConfigProperties;
  private final WebsocketStompConfigProperties websocketStompConfigProperties;

  /**
   * Websocket config constructor
   *
   * @param baseConfigProperties           Base configuration properties
   * @param securityConfigProperties       Security configuration properties
   * @param websocketStompConfigProperties Websocket STOMP configuration properties
   */
  public WebsocketConfig(BaseConfigProperties baseConfigProperties, SecurityConfigProperties securityConfigProperties, 
                         WebsocketStompConfigProperties websocketStompConfigProperties) {
    this.baseConfigProperties = baseConfigProperties;
    this.securityConfigProperties = securityConfigProperties;
    this.websocketStompConfigProperties = websocketStompConfigProperties;
  }

  /**
   * Configures the message broker.
   *
   * @param messageBrokerRegistry Message broker registry
   */
  @Override
  public void configureMessageBroker(@NotNull MessageBrokerRegistry messageBrokerRegistry) {
    if (websocketStompConfigProperties.isEnableStompBrokerRelay()) {
      messageBrokerRegistry.enableStompBrokerRelay(
              websocketStompConfigProperties.getDestinationPrefixes().toArray(new String[0]))
          .setRelayHost(websocketStompConfigProperties.getRelayHost())
          .setRelayPort(websocketStompConfigProperties.getRelayPort())
          .setClientLogin(websocketStompConfigProperties.getClientLogin())
          .setClientPasscode(websocketStompConfigProperties.getClientPasscode())
          .setSystemLogin(websocketStompConfigProperties.getSystemLogin())
          .setSystemPasscode(websocketStompConfigProperties.getSystemPasscode())
          .setTaskScheduler(heartBeatScheduler());
      log.info("✓ WebSocket configured with STOMP Broker Relay");
    } else {
      // Simple broker
      messageBrokerRegistry.enableSimpleBroker(
              websocketStompConfigProperties.getDestinationPrefixes().toArray(new String[0]))
          .setTaskScheduler(heartBeatScheduler());
      log.info("✓ WebSocket configured with Simple Broker");
    }
    messageBrokerRegistry.setApplicationDestinationPrefixes("/" + baseConfigProperties.getAcronym());
  }

  /**
   * Registers the end points.
   *
   * @param registry Stomp end point registry
   */
  @Override
  public void configureStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/websocket")
      .setAllowedOriginPatterns(securityConfigProperties.getAllowedOriginPatterns());
  }

  /**
   * Awe Client Tracker
   *
   * @return Awe client tracker
   */
  @Bean
  @ConditionalOnMissingBean
  @SessionScope
  public AweClientTracker aweClientTracker() {
    return new AweClientTracker();
  }

  /**
   * Retrieve connection tracker
   *
   * @return Connection tracker
   */
  @Bean
  public AweConnectionTracker aweConnectionTracker() {
    return new AweConnectionTracker();
  }

  @Bean
  public TaskScheduler heartBeatScheduler() {
    return new ThreadPoolTaskScheduler();
  }

  // SERVICES
  /**
   * Broadcast service
   *
   * @param brokerMessagingTemplate Messaging template
   * @param connectionTracker       Connection tracker
   * @return Broadcasting service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public BroadcastService broadcastService(SimpMessagingTemplate brokerMessagingTemplate, AweConnectionTracker connectionTracker) {
    return new BroadcastService(brokerMessagingTemplate, connectionTracker);
  }

  // EVENTS

  /**
   * Websocket events
   *
   * @return Websocket connected event bean
   */
  @Bean
  @ConditionalOnMissingBean
  public WebSocketEventListener webSocketEvent(InitService initService, AweConnectionTracker connectionTracker) {
    return new WebSocketEventListener(initService, connectionTracker);
  }
}
