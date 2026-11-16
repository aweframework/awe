package com.almis.awe.scheduler.util;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.web.MessageConverterUtil;
import com.almis.awe.rest.dto.AweRestResponse;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerDeferredTest {

  @Test
  void anAcceptedAnswerIsDeferredAndKeepsItsReferenceAndDescription() {
    ServiceData answer = SchedulerDeferred.accepted("job-77", "Sent to the engine");

    assertThat(SchedulerDeferred.isDeferred(answer)).isTrue();
    assertThat(SchedulerDeferred.externalReference(answer)).isEqualTo("job-77");
    assertThat(SchedulerDeferred.description(answer)).isEqualTo("Sent to the engine");
  }

  @Test
  void theMarkerDoesNotChangeTheKindOfAnswer() {
    ServiceData answer = SchedulerDeferred.accepted("job-77", "Sent");

    assertThat(answer.getType()).isEqualTo(AnswerType.OK);
    assertThat(answer.isValid()).isTrue();
  }

  @Test
  void aNormalAnswerIsNotDeferred() {
    assertThat(SchedulerDeferred.isDeferred(null)).isFalse();
    assertThat(SchedulerDeferred.isDeferred(new ServiceData())).isFalse();
    assertThat(SchedulerDeferred.isDeferred(new ServiceData().setType(AnswerType.WARNING).setMessage("x"))).isFalse();
    assertThat(SchedulerDeferred.externalReference(new ServiceData())).isNull();
    assertThat(SchedulerDeferred.description(new ServiceData())).isNull();
  }

  @Test
  void theReferenceIsOptional() {
    ServiceData answer = SchedulerDeferred.accepted(null, null);

    assertThat(SchedulerDeferred.isDeferred(answer)).isTrue();
    assertThat(SchedulerDeferred.externalReference(answer)).isNull();
    assertThat(SchedulerDeferred.description(answer)).isNull();
  }

  @Test
  void aReferenceLongerThan256CharactersIsTruncated() {
    ServiceData answer = SchedulerDeferred.accepted("r".repeat(300), "d".repeat(2500));

    assertThat(SchedulerDeferred.externalReference(answer)).hasSize(256);
    assertThat(SchedulerDeferred.description(answer)).hasSize(2000);
  }

  /**
   * The remote scheduler instance reads the answer of the remote maintain call: the remote AWE maps the ServiceData to
   * its REST response and serializes it, and the scheduler deserializes it as ServiceData with its RestTemplate.
   */
  @Test
  void theMarkerSurvivesTheRemoteMaintainCallback() throws IOException {
    AweRestResponse response = new ModelMapper().map(SchedulerDeferred.accepted("job-77", "Sent"),
      AweRestResponse.class);
    ServiceData received = roundTrip(response);

    assertThat(SchedulerDeferred.isDeferred(received)).isTrue();
    assertThat(SchedulerDeferred.externalReference(received)).isEqualTo("job-77");
    assertThat(SchedulerDeferred.description(received)).isEqualTo("Sent");
    assertThat(received.getType()).isEqualTo(AnswerType.OK);
  }

  @Test
  void aNormalAnswerStaysNormalThroughTheRemoteMaintainCallback() throws IOException {
    AweRestResponse response = new ModelMapper().map(new ServiceData().setMessage("done"), AweRestResponse.class);
    ServiceData received = roundTrip(response);

    assertThat(SchedulerDeferred.isDeferred(received)).isFalse();
  }

  /**
   * Write the response as the remote AWE does and read it as the scheduler reads the answer of a remote maintain:
   * both with the JSON converter of the RestTemplate
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private ServiceData roundTrip(AweRestResponse response) throws IOException {
    RestTemplate restTemplate = MessageConverterUtil.withoutXmlConverters(new RestTemplate());
    for (HttpMessageConverter converter : restTemplate.getMessageConverters()) {
      if (converter.canWrite(AweRestResponse.class, MediaType.APPLICATION_JSON)
        && converter.canRead(ServiceData.class, MediaType.APPLICATION_JSON)) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpHeaders headers = new HttpHeaders();
        converter.write(response, MediaType.APPLICATION_JSON, new HttpOutputMessage() {
          @Override
          public OutputStream getBody() {
            return out;
          }

          @Override
          public HttpHeaders getHeaders() {
            return headers;
          }
        });
        headers.setContentType(MediaType.APPLICATION_JSON);
        return (ServiceData) converter.read(ServiceData.class, new HttpInputMessage() {
          @Override
          public InputStream getBody() {
            return new ByteArrayInputStream(out.toByteArray());
          }

          @Override
          public HttpHeaders getHeaders() {
            return headers;
          }
        });
      }
    }
    throw new IllegalStateException("No JSON converter");
  }
}
