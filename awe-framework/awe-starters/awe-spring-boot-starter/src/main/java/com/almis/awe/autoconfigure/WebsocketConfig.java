package com.almis.awe.autoconfigure;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.listener.WebSocketEventListener;
import com.almis.awe.model.tracker.AweClientTracker;
import com.almis.awe.model.tracker.AweConnectionTracker;
import com.almis.awe.service.BroadcastService;
import com.almis.awe.service.InitService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
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
@EnableConfigurationProperties(BaseConfigProperties.class)
@EnableWebSocketMessageBroker
public class WebsocketConfig extends AbstractSessionWebSocketMessageBrokerConfigurer<Session> {

  // Autowired components
  private final BaseConfigProperties baseConfigProperties;
  private final SecurityConfigProperties securityConfigProperties;

  /**
   * Websocket config constructor
   *
   * @param baseConfigProperties     Base configuration properties
   * @param securityConfigProperties Security configuration properties
   */
  public WebsocketConfig(BaseConfigProperties baseConfigProperties, SecurityConfigProperties securityConfigProperties) {
    this.baseConfigProperties = baseConfigProperties;
    this.securityConfigProperties = securityConfigProperties;
  }

  /**
   * Configures the message broker.
   *
   * @param config Message broker registry
   */
  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic", "/queue");
    config.setApplicationDestinationPrefixes("/" + baseConfigProperties.getAcronym());
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

  /////////////////////////////////////////////
  // SERVICES
  /////////////////////////////////////////////

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

  /////////////////////////////////////////////
  // EVENTS
  /////////////////////////////////////////////

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
