package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.email.Email;
import com.almis.awe.model.entities.email.EmailItem;
import com.almis.awe.model.entities.email.EmailMessage;
import com.almis.awe.model.entities.email.ParsedEmail;
import com.almis.awe.model.entities.queries.Variable;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.data.builder.XMLEmailBuilder;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.File;
import java.util.Arrays;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Email service tests
 *
 * @author pgarcia
 */
class EmailServiceTest {

  @InjectMocks
  private EmailService emailService;

  @InjectMocks
  private XMLEmailBuilder emailBuilder;

  @Mock
  private JavaMailSender mailSender;

  @Mock
  private AweElements aweElements;

  @Mock
  private BaseConfigProperties baseConfigProperties;

  @Mock
  private ApplicationContext context;

  @Mock
  private MimeMessage mimeMessage;

  @Mock
  private QueryUtil queryUtil;

  /**
   * Initializes json mapper for tests
   */
  @BeforeEach
  public void initBeans() throws Exception {
    MockitoAnnotations.openMocks(this);
    doReturn(aweElements).when(context).getBean(AweElements.class);
    emailService.setApplicationContext(context);
    emailBuilder.setApplicationContext(context);
    when(aweElements.getLocaleWithLanguage(anyString(), any())).thenReturn("");
  }

  /**
   * Test of check public addresses
   *
   * @throws Exception Test error
   */
  @Test
  void sendMail() throws Exception {
    emailService.setApplicationContext(context);
    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(mailSender.createMimeMessage()).willReturn(mimeMessage);
    given(aweElements.getLanguage()).willReturn("ES");
    given(aweElements.getLocaleWithLanguage(anyString(), anyString())).willReturn("LOCALE");
    given(baseConfigProperties.getEncoding()).willReturn("UTF-8");
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
    verify(mailSender).send(mimeMessage);
  }

  /**
   * Test of check public addresses
   *
   * @throws Exception Test error
   */
  @Test
  void sendXMLMail() throws Exception {
    // Generate XML email
    Variable valueVariable = new Variable().setId("value").setName("value").setType("STRING");
    Variable valuesVariable = new Variable().setId("value2").setName("value2").setType("STRING");
    Variable labelVariable = new Variable().setId("label").setName("label").setType("STRING");
    Variable testVariable = new Variable().setId("test").setName("test").setType("STRING");
    Email email = new Email();
    email
      .setId("TestEmail")
      .setFrom((EmailItem) new EmailItem().setValue("value").setLabel("label"))
      .setToList(Arrays.asList((EmailItem) new EmailItem().setValue("value").setLabel("label"), (EmailItem) new EmailItem().setValue("value").setLabel("label")))
      .setCcList(singletonList((EmailItem) new EmailItem().setValue("value2").setLabel("value2")))
      .setCcoList(singletonList((EmailItem) new EmailItem().setValue("value").setLabel("label")))
      .setSubjectList(singletonList(new EmailMessage().setValue("test")))
      .setBodyList(Arrays.asList(new EmailMessage().setType("HTML").setValue("test"),
        new EmailMessage().setType("Text").setValue("test")))
      .setVariableList(Arrays.asList(valueVariable, valuesVariable, labelVariable));

    ArrayNode values = JsonNodeFactory.instance.arrayNode();
    values.add("tutu@test.com");
    values.add("lala@test.com");

    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.set("value", values);
    parameters.set("label", values);

    given(aweElements.getEmail(anyString())).willReturn(email);
    given(queryUtil.getParameter(eq(valueVariable), any(ObjectNode.class))).willReturn(JsonNodeFactory.instance.textNode("tutu@test.com"));
    given(queryUtil.getParameter(eq(valuesVariable), any(ObjectNode.class))).willReturn(values);
    given(queryUtil.getParameter(eq(labelVariable), any(ObjectNode.class))).willReturn(JsonNodeFactory.instance.textNode("tutu@test.com"));
    given(queryUtil.getParameter(eq(testVariable), any(ObjectNode.class))).willReturn(JsonNodeFactory.instance.textNode("test of subject and body"));

    // Build message
    ParsedEmail parsedEmail = emailBuilder
      .setEmail(email)
      .setParameters(parameters)
      .parseEmail()
      .build();

    assertEquals(2, parsedEmail.getTo().size());
    assertEquals(2, parsedEmail.getCc().size());
    assertEquals(1, parsedEmail.getCco().size());
  }
}