package com.almis.awe.test.integration.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.dao.UserDAOImpl;
import com.almis.awe.factory.MailSenderFactory;
import com.almis.awe.model.entities.email.ParsedEmail;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EmailService;
import com.almis.awe.service.QueryService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.File;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;

@Tag("integration")
@DisplayName("Email service Tests")
@WithMockUser(username = "test", password = "test")
class EmailServiceTest extends AbstractSpringAppIntegrationTest {

  private EmailService emailService;

  @Mock
  private MailSenderFactory mailSenderFactory;

  @Autowired
  private QueryService queryService;

  @Autowired
  private QueryUtil queryUtil;

  @Autowired
  private BaseConfigProperties baseConfigProperties;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private UserDAOImpl userDAO;

  @Mock
  private JavaMailSender mailSender;

  @Mock
  private MimeMessage mimeMessage;

  @BeforeEach
  public void setUp() {
    emailService = new EmailService(mailSenderFactory, baseConfigProperties, queryService, queryUtil, userDAO);
    emailService.setApplicationContext(applicationContext);
  }

  /**
   * Test of check public addresses
   *
   * @throws Exception Test error
   */
  @Test
  void sendMail() throws Exception {
    doReturn(mimeMessage).when(mailSender).createMimeMessage();
    when(mailSenderFactory.getMailSender(any())).thenReturn(mailSender);

    ParsedEmail email = new ParsedEmail()
      .setFrom(new InternetAddress("test@almis.com"))
      .setTo(singletonList(new InternetAddress("test@gmail.com")))
      .setReplyTo(singletonList(new InternetAddress("test@almis.com")))
      .setCc(singletonList(new InternetAddress("test@gmail.com")))
      .setCco(singletonList(new InternetAddress("test@gmail.com")))
      .setSubject("Test message")
      .setBody("<div style='background-color:red;'>Test div message</div>")
      .addAttachment("FileName.test", new File("test.jpg"));

    emailService.sendEmail(email);

    verify(mailSender, times(1)).send(mimeMessage);
  }

  /**
   * Test of check public addresses
   *
   * @throws Exception Test error
   */
  @Test
  void sendXMLMail() throws Exception {
    doReturn(mimeMessage).when(mailSender).createMimeMessage();
    when(mailSenderFactory.getMailSender(any())).thenReturn(mailSender);

    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.put("title", "test");
    parameters.put("subscription", "subscription");
    parameters.put("description", "description");
    parameters.set("UsrPrn", JsonNodeFactory.instance.arrayNode().add("test"));

    // Build message
    emailService.sendEmail("notify-users", parameters);

    verify(mailSender, times(1)).send(mimeMessage);
  }
}
