package com.almis.awe.autoconfigure;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.SessionConfigProperties;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.tracker.AweClientTracker;
import com.almis.awe.model.tracker.AweConnectionTracker;
import com.almis.awe.service.BroadcastService;
import com.almis.awe.service.MenuService;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.SessionService;
import com.almis.awe.session.AweSessionDetails;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.session.MapSession;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.web.context.annotation.SessionScope;

import java.util.concurrent.ConcurrentHashMap;

/**
 * AWE Session configuration class
 */
@Configuration
@EnableConfigurationProperties({BaseConfigProperties.class, SessionConfigProperties.class})
public class SessionConfig {

  /**
   * Session strategy
   *
   * @return Session strategy
   */
  @Bean
  @ConditionalOnMissingBean
  @SessionScope
  AweSession aweSession() {
    return new AweSession();
  }

  /**
   * Session service
   *
   * @return Session service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public SessionService sessionService() {
    return new SessionService();
  }


  /**
   * Creates an {@link AweSessionDetails} bean if no other bean of the same type is present.
   * This configuration class method specifies all required dependencies for constructing
   * an instance of {@link AweSessionDetails}.
   *
   * @param aweClientTracker         Tracks client-specific session information.
   * @param queryService             Service for executing and managing queries.
   * @param sessionService           Service for session-specific operations.
   * @param connectionTracker        Tracks user session connections.
   * @param broadcastService         Service responsible for broadcasting messages to clients.
   * @param menuService              Handles menu-related operations.
   * @param sessionConfigProperties  Configuration properties specific to session management.
   * @param securityConfigProperties Configuration properties for security and SSO details.
   * @return A fully initialized {@link AweSessionDetails} instance.
   */
  @Bean
  @ConditionalOnMissingBean
  public AweSessionDetails aweSessionDetails(AweClientTracker aweClientTracker, QueryService queryService, SessionService sessionService,
                                             AweConnectionTracker connectionTracker, BroadcastService broadcastService, MenuService menuService, SessionConfigProperties sessionConfigProperties, SecurityConfigProperties securityConfigProperties) {
    return new AweSessionDetails(aweClientTracker, queryService, sessionService, connectionTracker, broadcastService, menuService, sessionConfigProperties, securityConfigProperties);
  }

  /**
   * Session repository
   *
   * @return Map session repository
   */
  @Bean
  @ConditionalOnMissingBean
  public SessionRepository<MapSession> sessionRepository() {
    return new MapSessionRepository(new ConcurrentHashMap<>());
  }

  /**
   * Http session event publisher
   * @return session event publisher
   */
  @Bean
  @ConditionalOnMissingBean
  public HttpSessionEventPublisher httpSessionEventPublisher() {
    return new HttpSessionEventPublisher();
  }
}
