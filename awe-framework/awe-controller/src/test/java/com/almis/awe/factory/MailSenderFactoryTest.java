package com.almis.awe.factory;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.EmailServer;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailSenderFactoryTest {

  @InjectMocks
  private MailSenderFactory mailSenderFactory;

  @Mock
  private QueryService queryService;

  @Mock
  private JavaMailSender defaultSender;

  @Test
  void init() throws Exception {
    EmailServer emailServer = new EmailServer().setAuthenticated(true).setUser("user").setPass("pass").setHost("localhost").setName("EmailServer");
    DataList dataList = DataListUtil.fromBeanList(Collections.singletonList(emailServer));
    when(queryService.launchPrivateQuery(anyString())).thenReturn(new ServiceData().setDataList(dataList));

    mailSenderFactory.init();

    assertEquals(defaultSender, mailSenderFactory.getMailSender());
    assertNotEquals(defaultSender, mailSenderFactory.getMailSender("EmailServer"));
  }

  @Test
  void initException() throws Exception {
    when(queryService.launchPrivateQuery(anyString())).thenThrow(AWException.class);

    mailSenderFactory.init();

    assertEquals(defaultSender, mailSenderFactory.getMailSender());
    assertEquals(defaultSender, mailSenderFactory.getMailSender("EmailServer"));
  }
}