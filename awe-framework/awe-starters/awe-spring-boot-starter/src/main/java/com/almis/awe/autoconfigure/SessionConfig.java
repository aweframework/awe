package com.almis.awe.autoconfigure;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.SessionConfigProperties;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.tracker.AweClientTracker;
import com.almis.awe.model.tracker.AweConnectionTracker;
import com.almis.awe.service.BroadcastService;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.SessionService;
import com.almis.awe.session.AweSessionDetails;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.SessionScope;

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
   * Session details
   *
   * @param aweClientTracker         Awe Client tracker
   * @param queryService             Query service
   * @param connectionTracker        connection tracker
   * @param baseConfigProperties     Base properties
   * @param securityConfigProperties Security properties
   * @param sessionConfigProperties  Session properties
   * @return Session details bean
   */
  @Bean
  @ConditionalOnMissingBean
  public AweSessionDetails aweSessionDetails(AweClientTracker aweClientTracker, QueryService queryService,
                                             AweConnectionTracker connectionTracker, BroadcastService broadcastService, BaseConfigProperties baseConfigProperties, SecurityConfigProperties securityConfigProperties, SessionConfigProperties sessionConfigProperties) {
    return new AweSessionDetails(aweClientTracker, queryService, connectionTracker, broadcastService, baseConfigProperties, securityConfigProperties, sessionConfigProperties);
  }
}
