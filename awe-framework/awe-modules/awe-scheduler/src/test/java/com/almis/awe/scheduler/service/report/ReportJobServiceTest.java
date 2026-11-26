package com.almis.awe.scheduler.service.report;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.DataListUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S12b: the remote report callback reads the answer of the application with the AWE mapper (enum names).
 */
class ReportJobServiceTest {

  @Test
  void readsTheAnswerOfTheApplicationWithItsEnumNames() {
    ClientResponse answer = ClientResponse
      .create(HttpStatus.OK, ReportJobService.exchangeStrategies(DataListUtil.getMapper()))
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .body("{\"type\":\"OK\"}")
      .build();

    ServiceData serviceData = answer.bodyToMono(ServiceData.class).block();

    assertThat(serviceData).isNotNull();
    assertThat(serviceData.getType()).isEqualTo(AnswerType.OK);
  }
}
