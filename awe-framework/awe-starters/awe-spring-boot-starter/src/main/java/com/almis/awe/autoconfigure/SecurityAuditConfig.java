package com.almis.awe.autoconfigure;

import com.almis.awe.component.AweDatabaseContextHolder;
import com.almis.awe.config.LockoutConfigProperties;
import com.almis.awe.config.SecurityAuditConfigProperties;
import com.almis.awe.security.audit.DatabaseSecurityAuditSink;
import com.almis.awe.security.audit.LogSecurityAuditSink;
import com.almis.awe.security.audit.LoginAttemptService;
import com.almis.awe.security.audit.LoginAuditor;
import com.almis.awe.security.audit.SecurityAuditPurgeScheduler;
import com.almis.awe.security.audit.SecurityAuditScreenService;
import com.almis.awe.security.audit.SecurityAuditService;
import com.almis.awe.security.audit.SecurityAuditSessionListener;
import com.almis.awe.security.audit.SecurityAuditSink;
import com.almis.awe.security.audit.SecurityAuditTenantResolver;
import com.almis.awe.security.audit.UserAccountAuditService;
import com.almis.awe.security.multitenant.TenantContext;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Clock;

/**
 * Security audit: event service, database and JSON log sinks and the purge of expired events.
 * <p>
 * {@code awe.security.audit.enabled=false} switches the whole feature off: the service stays available as a no-op so
 * the audited code needs no checks.
 */
@Configuration
@EnableConfigurationProperties({SecurityAuditConfigProperties.class, LockoutConfigProperties.class})
public class SecurityAuditConfig {

  private static final String PREFIX = "awe.security.audit";

  /**
   * Tenant stored on the events: the SSO tenant of the request, or else the database alias of the session
   *
   * @param contextHolder Database context holder
   * @return Tenant resolver
   */
  @Bean
  @ConditionalOnMissingBean
  public SecurityAuditTenantResolver securityAuditTenantResolver(ObjectProvider<AweDatabaseContextHolder> contextHolder) {
    return () -> {
      String tenant = TenantContext.getCurrentTenant();
      if (tenant != null) {
        return tenant;
      }
      AweDatabaseContextHolder holder = contextHolder.getIfAvailable();
      return holder == null ? null : holder.getCurrentDatabase();
    };
  }

  /**
   * Security audit service
   *
   * @param properties     Security audit properties
   * @param sinks          Event sinks
   * @param tenantResolver Tenant resolver
   * @return Security audit service
   */
  @Bean
  @ConditionalOnMissingBean
  public SecurityAuditService securityAuditService(SecurityAuditConfigProperties properties,
                                                   ObjectProvider<SecurityAuditSink> sinks,
                                                   SecurityAuditTenantResolver tenantResolver) {
    return new SecurityAuditService(properties, sinks.orderedStream().toList(), tenantResolver, Clock.systemDefaultZone());
  }

  /**
   * JSON log sink
   *
   * @return Log sink
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(prefix = PREFIX, name = {"enabled", "log.enabled"}, havingValue = "true", matchIfMissing = true)
  public LogSecurityAuditSink logSecurityAuditSink() {
    return new LogSecurityAuditSink();
  }

  /**
   * Database sink
   *
   * @param dataSource Application data source
   * @return Database sink
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(prefix = PREFIX, name = {"enabled", "database.enabled"}, havingValue = "true", matchIfMissing = true)
  public DatabaseSecurityAuditSink databaseSecurityAuditSink(DataSource dataSource) {
    return new DatabaseSecurityAuditSink(dataSource);
  }

  /**
   * Purge of the expired events of the database sink
   *
   * @param sink       Database sink
   * @param properties Security audit properties
   * @return Purge scheduler
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(prefix = PREFIX, name = {"enabled", "database.enabled"}, havingValue = "true", matchIfMissing = true)
  public SecurityAuditPurgeScheduler securityAuditPurgeScheduler(DatabaseSecurityAuditSink sink, SecurityAuditConfigProperties properties) {
    return new SecurityAuditPurgeScheduler(sink, properties, Clock.systemDefaultZone());
  }

  /**
   * Automatic lockout after consecutive failed logins ({@code awe.security.lockout.max-attempts})
   *
   * @param properties      Lockout properties
   * @param queryService    Query service
   * @param maintainService Maintain service
   * @return Login attempt service
   */
  @Bean
  @ConditionalOnMissingBean
  public LoginAttemptService loginAttemptService(LockoutConfigProperties properties, QueryService queryService,
                                                 MaintainService maintainService) {
    return new LoginAttemptService(properties, queryService, maintainService);
  }

  /**
   * Records the local logins and applies the automatic lockout
   *
   * @param securityAuditService Security audit service
   * @param loginAttemptService  Login attempt service
   * @return Login auditor
   */
  @Bean
  @ConditionalOnMissingBean
  public LoginAuditor loginAuditor(SecurityAuditService securityAuditService, LoginAttemptService loginAttemptService) {
    return new LoginAuditor(securityAuditService, loginAttemptService);
  }

  /**
   * Security audit of the user account changes made through the generic screens (called from the maintains)
   *
   * @param securityAuditService Security audit service
   * @param queryService         Query service
   * @param loginAttemptService  Login attempt service
   * @return User account audit service
   */
  @Bean
  @ConditionalOnMissingBean
  public UserAccountAuditService userAccountAuditService(SecurityAuditService securityAuditService, QueryService queryService,
                                                         LoginAttemptService loginAttemptService) {
    return new UserAccountAuditService(securityAuditService, queryService, loginAttemptService);
  }

  /**
   * Services of the security audit screen
   *
   * @return Security audit screen service
   */
  @Bean
  @ConditionalOnMissingBean
  public SecurityAuditScreenService securityAuditScreenService() {
    return new SecurityAuditScreenService(Clock.systemDefaultZone());
  }

  /**
   * Records the sessions ended by inactivity
   *
   * @param securityAuditService Security audit service
   * @return Session listener
   */
  @Bean
  @ConditionalOnMissingBean
  public SecurityAuditSessionListener securityAuditSessionListener(SecurityAuditService securityAuditService) {
    return new SecurityAuditSessionListener(securityAuditService, Clock.systemUTC());
  }
}
