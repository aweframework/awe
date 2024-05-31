package com.almis.awe.factory;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.EmailServer;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class MailSenderFactory extends ServiceConfig {

  private final JavaMailSender defaultSender;
  private final QueryService queryService;
  private Map<String, JavaMailSender> mailSenderMap;

  public MailSenderFactory(JavaMailSender defaultSender, QueryService queryService) {
    this.defaultSender = defaultSender;
    this.queryService = queryService;
  }

  /**
   * Initialize Mail Sender Factory
   */
  public MailSenderFactory init() {
    try {
      // Retrieve server list
      DataList dataList = queryService.launchPrivateQuery("EmlSrvLst").getDataList();

      mailSenderMap = DataListUtil.asBeanList(dataList, EmailServer.class).stream()
        .collect(Collectors.toConcurrentMap(EmailServer::getName, this::generateMailSender));
    } catch (AWException exc) {
      log.error("Error retrieving email server list", exc);
    }

    return this;
  }

  /**
   * Retrieve mail server
   * @return Mail server
   */
  public JavaMailSender getMailSender() {
    return getMailSender(null);
  }

  /**
   * Retrieve mail server
   * @param mailServer Mail server
   * @return Mail server
   */
  public JavaMailSender getMailSender(String mailServer) {
    if (mailSenderMap == null) {
      this.init();
    }

    if (mailServer != null && mailSenderMap != null) {
      return Optional.ofNullable(mailSenderMap.get(mailServer)).orElse(defaultSender);
    }

    return defaultSender;
  }

  /**
   * Generate each mail server
   * @param emailServer Mail server
   * @return Mail sender from mail server
   */
  private JavaMailSender generateMailSender(EmailServer emailServer) {
    JavaMailSenderImpl javaMailSender;

    // Create JavaMailSender
    javaMailSender = new JavaMailSenderImpl();
    javaMailSender.setHost(emailServer.getHost());

    // Add authentication
    if (emailServer.isAuthenticated()) {
      javaMailSender.setUsername(emailServer.getUser());
      javaMailSender.setPassword(emailServer.getPass());
    }

    return javaMailSender;
  }
}
