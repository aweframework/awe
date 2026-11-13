package com.almis.awe.service.data.processor;

import com.almis.awe.model.component.XStreamSerializer;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.queues.MessageStatus;
import com.almis.awe.model.entities.queues.ResponseMessage;
import com.almis.awe.model.type.AnswerType;
import jakarta.jms.MapMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueProcessorTest {

  @Mock
  private XStreamSerializer serializer;

  @Mock
  private MapMessage message;

  private ResponseMessage statusResponse() {
    ResponseMessage response = new ResponseMessage();
    response.setType("MAP");
    response.setStatus(new MessageStatus().setType("result").setTitle("heading").setDescription("detail"));
    return response;
  }

  @Test
  void statusWithTitleAndDescriptionSetsBoth() throws Exception {
    when(message.getString("result")).thenReturn("warning");
    when(message.getString("heading")).thenReturn("Check");
    when(message.getString("detail")).thenReturn("Stock is low");

    ServiceData serviceData = new QueueProcessor(serializer).parseResponseMessage(statusResponse(), message);

    assertThat(serviceData.getType()).isEqualTo(AnswerType.WARNING);
    assertThat(serviceData.getTitle()).isEqualTo("Check");
    assertThat(serviceData.getMessage()).isEqualTo("Stock is low");
  }

  @Test
  void statusDescriptionIsKeptWhenTheResponseHasNoTitle() throws Exception {
    when(message.getString("result")).thenReturn("error");
    when(message.getString("heading")).thenReturn(null);
    when(message.getString("detail")).thenReturn("Account is blocked");

    ServiceData serviceData = new QueueProcessor(serializer).parseResponseMessage(statusResponse(), message);

    assertThat(serviceData.getType()).isEqualTo(AnswerType.ERROR);
    assertThat(serviceData.getMessage()).isEqualTo("Account is blocked");
  }

  @Test
  void statusWithoutDescriptionKeepsTheDefaultMessage() throws Exception {
    when(message.getString("result")).thenReturn("ok");
    when(message.getString("heading")).thenReturn("Done");
    when(message.getString("detail")).thenReturn(null);
    String defaultMessage = new ServiceData().getMessage();

    ServiceData serviceData = new QueueProcessor(serializer).parseResponseMessage(statusResponse(), message);

    assertThat(serviceData.getType()).isEqualTo(AnswerType.OK);
    assertThat(serviceData.getTitle()).isEqualTo("Done");
    assertThat(serviceData.getMessage()).isEqualTo(defaultMessage);
  }
}
