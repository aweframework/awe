package com.almis.awe.service.data.builder;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.email.Email;
import com.almis.awe.model.entities.email.EmailItem;
import com.almis.awe.model.entities.email.EmailMessage;
import com.almis.awe.model.entities.queries.Variable;
import com.almis.awe.model.type.EmailMessageType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.QueryService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class XMLEmailBuilderTest {

  @Mock
  private QueryService queryService;

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private AweElements aweElements;

  /**
   * Set null variables
   */
  @Test
  void setNullVariables() throws Exception {
    XMLEmailBuilder builder = new XMLEmailBuilder(queryService, queryUtil, aweElements);

    // Assert builder is not null
    assertNotNull(builder);
  }

  /**
   * Parse attachments
   */
  @Test
  void parseAttachments() throws Exception {
    given(queryUtil.getParameter(any(), any())).willReturn(JsonNodeFactory.instance.textNode("tutu"));
    XMLEmailBuilder emailBuilder = new XMLEmailBuilder(queryService, queryUtil, aweElements);

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
    given(queryUtil.getParameter(any(), any())).willReturn(JsonNodeFactory.instance.textNode("tutu"));
    XMLEmailBuilder emailBuilder = new XMLEmailBuilder(queryService, queryUtil, aweElements);

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