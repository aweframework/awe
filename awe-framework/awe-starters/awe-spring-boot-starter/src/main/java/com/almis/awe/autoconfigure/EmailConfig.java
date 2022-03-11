package com.almis.awe.autoconfigure;

import com.almis.awe.autoconfigure.config.EmailConfigProperties;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EmailService;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.data.builder.XMLEmailBuilder;
import com.almis.awe.service.data.connector.maintain.EmailMaintainConnector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Email configuration
 * @author dfuentes
 * Created by dfuentes on 25/04/2017.
 */
@Configuration
@EnableConfigurationProperties(value = EmailConfigProperties.class)
@ConditionalOnProperty(name = "awe.mail.enabled", havingValue = "true")
public class EmailConfig {

  // Email config properties
  private final EmailConfigProperties emailConfigProperties;

  /**
   * EmailConfig constructor
   * @param emailConfigProperties email config properties
   */
  public EmailConfig (EmailConfigProperties emailConfigProperties) {
    this.emailConfigProperties = emailConfigProperties;
  }

  /**
   * Default JavaMail configuration
   *
   * @return Mail sender
   */
  @Bean
  @ConditionalOnMissingBean
  public JavaMailSender defaultMail() {
    JavaMailSenderImpl javaMailSender;

    // Create JavaMailSender
    javaMailSender = new JavaMailSenderImpl();
    javaMailSender.setHost(emailConfigProperties.getHost());
    javaMailSender.setPort(emailConfigProperties.getPort());

    // Add authentication
    if (emailConfigProperties.isAuth()) {
      javaMailSender.setUsername(emailConfigProperties.getUser());
      javaMailSender.setPassword(emailConfigProperties.getPass());
    }

    // Generate smtp properties
    Properties properties = new Properties();
    properties.put("mail.smtp.localhost", emailConfigProperties.getLocalhost());
    properties.put("mail.debug", emailConfigProperties.isDebug());
    properties.put("mail.smtp.starttls.enable", emailConfigProperties.isTls());
    properties.put("mail.smtp.ssl.enable", emailConfigProperties.isSsl());
    if (emailConfigProperties.isSsl()){
      properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
      properties.put("mail.smtp.ssl.checkserveridentity", true);
    }

    // Add properties
    javaMailSender.setJavaMailProperties(properties);
    return javaMailSender;
  }

  /////////////////////////////////////////////
  // SERVICES
  /////////////////////////////////////////////
  /**
   * Email service
   * @param mailSender Mail sender
   * @param emailBuilder Email builder
   * @param baseConfigProperties Base configuration properties
   * @return Email service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public EmailService emailService(JavaMailSender mailSender, XMLEmailBuilder emailBuilder, BaseConfigProperties baseConfigProperties) {
    return new EmailService(mailSender, emailBuilder, baseConfigProperties);
  }

  /////////////////////////////////////////////
  // CONNECTORS
  /////////////////////////////////////////////

  /**
   * Email Maintain connector
   * @param emailService Email service
   * @return Email Maintain connector bean
   */
  @Bean
  @ConditionalOnMissingBean
  public EmailMaintainConnector emailMaintainConnector(EmailService emailService) {
    return new EmailMaintainConnector(emailService);
  }


  /////////////////////////////////////////////
  // BUILDERS
  /////////////////////////////////////////////

  /**
   * XML Email builder
   * @return XML Email builder bean
   */
  @Bean
  @ConditionalOnMissingBean
  @Scope("prototype")
  public XMLEmailBuilder xmlEmailBuilder(QueryService queryService, QueryUtil queryUtil) {
    return new XMLEmailBuilder(queryService, queryUtil);
  }

}
