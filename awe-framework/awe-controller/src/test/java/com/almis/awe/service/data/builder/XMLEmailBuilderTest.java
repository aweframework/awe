package com.almis.awe.service.data.builder;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.email.Email;
import com.almis.awe.model.entities.email.EmailItem;
import com.almis.awe.model.entities.email.EmailMessage;
import com.almis.awe.model.entities.queries.Variable;
import com.almis.awe.model.type.EmailMessageType;
import com.almis.awe.model.util.data.QueryUtil;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;

class XMLEmailBuilderTest {

  @InjectMocks
  private XMLEmailBuilder emailBuilder;

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private ApplicationContext context;

  @Mock
  private AweElements aweElements;

  /**
   * Initializes json mapper for tests
   */
  @BeforeEach
  public void initBeans() throws Exception {
    MockitoAnnotations.openMocks(this);
    emailBuilder.setApplicationContext(context);
    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getLanguage()).willReturn("ES");
    given(aweElements.getLocaleWithLanguage(anyString(), anyString())).willReturn("LOCALE");
    given(queryUtil.getParameters((String) eq(null))).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryUtil.getParameter(any(), any())).willReturn(JsonNodeFactory.instance.textNode("tutu"));
  }

  /**
   * Test context loaded
   */
  @Test
  void contextLoads() {
    // Check that controller are active
    assertNotNull(emailBuilder);
  }

  /**
   * Set null variables
   */
  @Test
  void setNullVariables() throws Exception {
    XMLEmailBuilder builder = emailBuilder.setVariables(null);

    // Assert builder is not null
    assertNotNull(builder);
  }

  /**
   * Parse attachments
   */
  @Test
  void parseAttachments() throws Exception {
    Email email = new Email();
    email.setFrom(new EmailItem());
    email.setSubjectList(Collections.singletonList(new EmailMessage()));
    email.setBodyList(Arrays.asList(new EmailMessage().setType("HTML"), new EmailMessage().setType("TEXT")));
    email.setAttachmentList(Collections.singletonList(new EmailItem()));
    emailBuilder.setEmail(email);
    XMLEmailBuilder builder = emailBuilder.parseEmail();

    // Assert builder is not null
    assertNotNull(builder);
  }

  /**
   * Parse attachments with values
   */
  @Test
  void parseAttachmentsWithValues() throws Exception {
    Email email = new Email();
    email.setFrom(new EmailItem());
    email.setToList(Collections.singletonList(new EmailItem()));
    email.setCcList(Collections.singletonList(new EmailItem()));
    email.setCcoList(Collections.singletonList(new EmailItem()));
    email.setSubjectList(Collections.singletonList(new EmailMessage().setValue("path").setLabel("name")));
    email.setBodyList(Arrays.asList(new EmailMessage().setType("HTML").setValue("path").setLabel("name"),
            new EmailMessage().setType("TEXT").setValue("path").setLabel("name")));
    email.setAttachmentList(Collections.singletonList((EmailItem) new EmailItem().setValue("path").setLabel("name")));
    email.setVariableList(Arrays.asList(new Variable().setId("path"), new Variable().setId("name")));
    emailBuilder.setEmail(email);
    emailBuilder.getParsedEmail().setMessageType(EmailMessageType.TEXT);
    XMLEmailBuilder builder = emailBuilder.parseEmail();

    // Assert builder is not null
    assertNotNull(builder);
  }
}