package com.almis.awe.autoconfigure;

import com.almis.awe.config.*;
import com.almis.awe.security.accessbean.LoginAccessControl;
import com.almis.awe.service.AccessService;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.MenuService;
import com.almis.awe.service.TotpService;
import com.almis.awe.session.AweSessionDetails;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.spring.autoconfigure.TotpAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;

import java.util.Objects;

@Configuration
@Slf4j
@EnableConfigurationProperties({SecurityConfigProperties.class, TotpConfigProperties.class})
@Import(TotpAutoConfiguration.class) // Work around for https://github.com/samdjstevens/java-totp/issues/53
public class SecurityConfig extends ServiceConfig {

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
    log.info("Using authentication mode: " + mode);

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
    } else {
      auth.authenticationProvider(getBean(AuthenticationProvider.class));
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
                                     TotpConfigProperties totpConfigProperties) {
    return new AccessService(aweSessionDetails, menuService, encodeService, totpService, baseConfigProperties, securityConfigProperties, totpConfigProperties);
  }

  /**
   * Totp service
   *
   * @param secretGenerator Secret generator
   * @param qrDataFactory   QR data factory
   * @param qrGenerator     QR Generator
   * @param codeVerifier    TOTP Code verifier
   * @return TOTP Service
   */
  @Bean
  @ConditionalOnMissingBean
  public TotpService totpService(SecretGenerator secretGenerator, QrDataFactory qrDataFactory, QrGenerator qrGenerator, CodeVerifier codeVerifier) {
    return new TotpService(secretGenerator, qrDataFactory, qrGenerator, codeVerifier);
  }
}