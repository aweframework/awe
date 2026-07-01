package com.almis.awe.autoconfigure;

import com.almis.awe.config.*;
import com.almis.awe.security.accessbean.LoginAccessControl;
import com.almis.awe.service.*;
import com.almis.awe.service.totp.AweTotpOperations;
import com.almis.awe.service.totp.OtpJavaTotpOperations;
import com.almis.awe.service.totp.ZxingQrPngGenerator;
import com.almis.awe.service.user.AweUserDetailService;
import com.almis.awe.session.AweSessionDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Clock;
import java.util.Objects;

@Configuration
@Slf4j
@EnableConfigurationProperties({SecurityConfigProperties.class, TotpConfigProperties.class})
public class SecurityConfig extends ServiceConfig {

  public static final String TOTP_CLOCK_BEAN = "aweTotpClock";

  // Autowired services
  private final SecurityConfigProperties securityConfigProperties;

  /**
   * Autowired constructor
   *
   * @param securityConfigProperties Security configuration properties
   */
  @Autowired
  public SecurityConfig(SecurityConfigProperties securityConfigProperties) {
    this.securityConfigProperties = securityConfigProperties;
  }

  /**
   * Configure current users datasource
   *
   * @param auth Authentication manager
   */
  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) {

    AuthType mode = securityConfigProperties.getAuthMode();
    log.info("Using authentication mode: {}", mode);

    if (Objects.requireNonNull(mode) == AuthType.CUSTOM) {// Custom authentication bean
      for (String provider : securityConfigProperties.getAuthCustomProviders()) {
        try {
          Object beanObj = getBean(provider);
          if (beanObj instanceof AuthenticationProvider authProvider) {
            auth.authenticationProvider(authProvider);
          }
        } catch (Exception exc) {
          log.error("Couldn't load authentication provider bean with name [{}]", provider, exc);
        }
      }
    }
  }

  /**
   * Get access control bean to use in the configuration method
   *
   * @return Login access control
   */
  @Bean
  @ConditionalOnMissingBean
  public LoginAccessControl loginAccessControl() {
    return new LoginAccessControl();
  }

  /**
   * Access service bean
   *
   * @param menuService              Menu service
   * @param aweSessionDetails        Awe session details
   * @param encodeService            Encode service
   * @param totpService              Totp service
   * @param baseConfigProperties     Base config properties
   * @param securityConfigProperties Security config properties
   * @param totpConfigProperties     Totp config properties
   * @param userDetailsService       AWE user details
   * @param maintainService          Maintain service
   * @return AccessService bean
   */
  @Bean
  @ConditionalOnMissingBean
  public AccessService accessService(AweSessionDetails aweSessionDetails,
																		 MenuService menuService,
																		 EncodeService encodeService,
																		 TotpService totpService,
																		 BaseConfigProperties baseConfigProperties,
																		 SecurityConfigProperties securityConfigProperties,
																		 TotpConfigProperties totpConfigProperties,
																		 UserDetailsService userDetailsService,
																		 MaintainService maintainService) {
    return new AccessService(aweSessionDetails, menuService, encodeService, totpService, baseConfigProperties, securityConfigProperties, totpConfigProperties, (AweUserDetailService) userDetailsService, maintainService);
  }

  /**
   * TOTP clock bean.
   *
   * @return UTC system clock
   */
  @Bean(name = TOTP_CLOCK_BEAN)
  @ConditionalOnMissingBean(name = TOTP_CLOCK_BEAN)
  public Clock totpClock() {
    return Clock.systemUTC();
  }

  /**
   * QR PNG generator bean.
   *
   * @return ZXing QR PNG generator
   */
  @Bean
  @ConditionalOnMissingBean
  public ZxingQrPngGenerator zxingQrPngGenerator() {
    return new ZxingQrPngGenerator();
  }

  /**
   * TOTP operations bean.
   *
   * @param totpClock UTC clock for TOTP time-step calculations
   * @param zxingQrPngGenerator QR PNG generator
   * @return TOTP operations
   */
  @Bean
  @ConditionalOnMissingBean
  public AweTotpOperations aweTotpOperations(@Qualifier(TOTP_CLOCK_BEAN) Clock totpClock,
                                             ZxingQrPngGenerator zxingQrPngGenerator) {
    return new OtpJavaTotpOperations(totpClock, zxingQrPngGenerator);
  }

  @Bean
  @ConditionalOnMissingBean
  public TotpService totpService(AweTotpOperations aweTotpOperations) {
    return new TotpService(aweTotpOperations);
  }
}
