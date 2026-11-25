package com.almis.awe.scheduler.autoconfigure;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.web.MessageConverterUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Client of the maintains of the application (remote scheduler): it reads their answer with the AWE mapper whatever rest
 * template bean the application ends up with (another starter can declare its own restTemplate bean)
 */
class SchedulerConfigRestTemplateTest {

  private final RestTemplate shared = MessageConverterUtil.withoutXmlConverters(new RestTemplate());
  private final RestTemplate client = SchedulerConfig.maintainClient(shared, DataListUtil.getMapper());

  @Test
  void readsTheAnswerOfAnApplicationMaintainWithItsEnumNames() throws IOException {
    MockHttpInputMessage answer = new MockHttpInputMessage("{\"type\":\"OK\"}".getBytes(StandardCharsets.UTF_8));
    answer.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    ServiceData serviceData = (ServiceData) jsonConverter(client).read(ServiceData.class, answer);

    assertThat(serviceData.getType()).isEqualTo(AnswerType.OK);
  }

  @Test
  void keepsTheRequestFactoryOfTheRestTemplateBeanWithoutChangingItsConverters() {
    List<HttpMessageConverter<?>> sharedConverters = List.copyOf(shared.getMessageConverters());

    assertThat(client).isNotSameAs(shared);
    assertThat(client.getRequestFactory()).isSameAs(shared.getRequestFactory());
    assertThat(shared.getMessageConverters()).containsExactlyElementsOf(sharedConverters);
  }

  @Test
  void keepsWithoutXmlConverters() {
    assertThat(client.getMessageConverters())
      .noneMatch(converter -> converter.getSupportedMediaTypes().stream()
        .anyMatch(type -> type.getSubtype().endsWith("xml")));
  }

  @SuppressWarnings("unchecked")
  private static HttpMessageConverter<Object> jsonConverter(RestTemplate restTemplate) {
    return (HttpMessageConverter<Object>) restTemplate.getMessageConverters().stream()
      .filter(JacksonJsonHttpMessageConverter.class::isInstance)
      .findFirst()
      .orElseThrow();
  }
}
