package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.dao.UserDAOImpl;
import com.almis.awe.factory.MailSenderFactory;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.User;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

  @InjectMocks
  private EmailService emailService;

  @Mock
  private MailSenderFactory mailSenderFactory;

  @Mock
  private JavaMailSender mailSender;

  @Mock
  private AweElements aweElements;

  @Mock
  private AweSession aweSession;

  @Mock
  private AweRequest aweRequest;

  @Mock
  private BaseConfigProperties baseConfigProperties;

  @Mock
  private ApplicationContext context;

  @Mock
  private MimeMessage mimeMessage;

  @Mock
  private QueryService queryService;

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private UserDAOImpl userDAO;

  @BeforeEach
  public void setUp() {
    emailService.setApplicationContext(context);
  }

  /**
   * Test of check public addresses
   *
   * @throws Exception Test error
   */
  @Test
  void sendMail() throws Exception {
    when(context.getBean(AweSession.class)).thenReturn(aweSession);
    when(context.getBean(AweRequest.class)).thenReturn(aweRequest);
    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(mailSenderFactory.getMailSender()).willReturn(mailSender);
    given(mailSender.createMimeMessage()).willReturn(mimeMessage);
    given(aweElements.getLanguage()).willReturn("es-ES");
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
    when(context.getBean(AweSession.class)).thenReturn(null);
    when(context.getBean(AweRequest.class)).thenReturn(aweRequest);
    given(mailSenderFactory.getMailSender()).willReturn(mailSender);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), any())).thenReturn("LOCALE");

    // Generate XML email
    Variable valueVariable = new Variable().setId("value").setName("value").setType("STRING");
    Variable valuesVariable = new Variable().setId("value2").setName("value2").setType("STRING");
    Variable labelVariable = new Variable().setId("label").setName("label").setType("STRING");
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

    given(queryUtil.getParameter(eq(valueVariable), any(ObjectNode.class))).willReturn(JsonNodeFactory.instance.textNode("tutu@test.com"));
    given(queryUtil.getParameter(eq(valuesVariable), any(ObjectNode.class))).willReturn(values);
    given(queryUtil.getParameter(eq(labelVariable), any(ObjectNode.class))).willReturn(JsonNodeFactory.instance.textNode("tutu@test.com"));

    // Build message
    ParsedEmail parsedEmail = new XMLEmailBuilder(queryService, queryUtil, aweElements)
      .setEmail(email)
      .setParameters(parameters)
      .parseEmail()
      .build();

    // Build message
    emailService.sendEmail(parsedEmail);

    assertEquals(2, parsedEmail.getTo().size());
    assertEquals(2, parsedEmail.getCc().size());
    assertEquals(1, parsedEmail.getCco().size());
  }

  @Test
  void sendXMLMailTwice() throws Exception {
    when(context.getBean(AweSession.class)).thenReturn(aweSession);
    when(context.getBean(AweRequest.class)).thenReturn(aweRequest);
    when(aweSession.getUser()).thenReturn("user");
    when(userDAO.findByUserName(anyString())).thenReturn(new User().setEmailServer("emailServer"));
    given(mailSenderFactory.getMailSender(anyString())).willReturn(mailSender);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), any())).thenReturn("LOCALE");

    // Generate XML email
    Variable valueVariable = new Variable().setId("value").setName("value").setType("STRING");
    Variable valuesVariable = new Variable().setId("value2").setName("value2").setType("STRING");
    Variable labelVariable = new Variable().setId("label").setName("label").setType("STRING");
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

    given(queryUtil.getParameter(eq(valueVariable), any(ObjectNode.class))).willReturn(JsonNodeFactory.instance.textNode("tutu@test.com"));
    given(queryUtil.getParameter(eq(valuesVariable), any(ObjectNode.class))).willReturn(values);
    given(queryUtil.getParameter(eq(labelVariable), any(ObjectNode.class))).willReturn(JsonNodeFactory.instance.textNode("tutu@test.com"));

    // Build message
    ParsedEmail parsedEmail = new XMLEmailBuilder(queryService, queryUtil, aweElements)
      .setEmail(email)
      .setParameters(parameters)
      .parseEmail()
      .build();

    // Build message
    emailService.sendEmail(parsedEmail);

    assertEquals(2, parsedEmail.getTo().size());
    assertEquals(2, parsedEmail.getCc().size());
    assertEquals(1, parsedEmail.getCco().size());

    values = JsonNodeFactory.instance.arrayNode();
    values.add("tutu@test.com");
    values.add("lala@test.com");

    parameters = JsonNodeFactory.instance.objectNode();
    parameters.set("value", values);
    parameters.set("label", values);

    parsedEmail = new XMLEmailBuilder(queryService, queryUtil, aweElements)
      .setEmail(email)
      .setParameters(parameters)
      .parseEmail()
      .build();

    // Build message
    emailService.sendEmail(parsedEmail);

    assertEquals(2, parsedEmail.getTo().size());
    assertEquals(2, parsedEmail.getCc().size());
    assertEquals(1, parsedEmail.getCco().size());
  }
}