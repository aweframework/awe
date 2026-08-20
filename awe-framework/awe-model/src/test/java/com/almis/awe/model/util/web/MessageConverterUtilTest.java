package com.almis.awe.model.util.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the media-type rule AWE uses to keep XML off the wire.
 *
 * <p>The rule is expressed over media types rather than converter classes on purpose: the
 * converters Spring registers depend on what is on the classpath, and the artifact that
 * introduces them (for instance {@code jackson-dataformat-xml}) can arrive transitively
 * through any dependency. See aweframework/awe#742.</p>
 */
class MessageConverterUtilTest {

  /**
   * Converter advertising a media type with the {@code +xml} structured suffix
   */
  private static class SuffixXmlConverter implements HttpMessageConverter<Object> {

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
      return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
      return false;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
      return List.of(MediaType.parseMediaType("application/hal+xml"));
    }

    @Override
    public Object read(Class<?> clazz, HttpInputMessage inputMessage) {
      return null;
    }

    @Override
    public void write(Object value, MediaType contentType, HttpOutputMessage outputMessage) {
      // Never written: this stub only carries a media type
    }
  }

  @Test
  @DisplayName("Reports application/xml and text/xml converters as XML")
  void servesXml_forXmlSubtype() {
    assertTrue(MessageConverterUtil.servesXml(new SourceHttpMessageConverter<>()));
  }

  @Test
  @DisplayName("Reports the +xml structured suffix as XML")
  void servesXml_forStructuredXmlSuffix() {
    assertTrue(MessageConverterUtil.servesXml(new SuffixXmlConverter()));
  }

  @Test
  @DisplayName("Does not report JSON, text or wildcard converters as XML")
  void servesXml_forNonXmlConverters() {
    assertFalse(MessageConverterUtil.servesXml(new MappingJackson2HttpMessageConverter()));
    assertFalse(MessageConverterUtil.servesXml(new StringHttpMessageConverter()));
    assertFalse(MessageConverterUtil.servesXml(new ByteArrayHttpMessageConverter()));
    assertFalse(MessageConverterUtil.servesXml(new ResourceHttpMessageConverter()));
  }

  @Test
  @DisplayName("Removes every XML converter from a list and keeps the rest in order")
  void removeXmlConverters_keepsNonXmlOrder() {
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    StringHttpMessageConverter string = new StringHttpMessageConverter();
    MappingJackson2HttpMessageConverter json = new MappingJackson2HttpMessageConverter();
    converters.add(string);
    converters.add(new SourceHttpMessageConverter<>());
    converters.add(new SuffixXmlConverter());
    converters.add(json);

    MessageConverterUtil.removeXmlConverters(converters);

    assertEquals(List.of(string, json), converters);
  }

  @Test
  @DisplayName("Leaves a list without XML converters untouched")
  void removeXmlConverters_withoutXml() {
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new StringHttpMessageConverter());
    converters.add(new MappingJackson2HttpMessageConverter());

    MessageConverterUtil.removeXmlConverters(converters);

    assertEquals(2, converters.size());
  }

  @Test
  @DisplayName("Strips XML converters from a rest template and returns the same instance")
  void withoutXmlConverters_pinsRestTemplate() {
    RestTemplate restTemplate = new RestTemplate();
    assertTrue(restTemplate.getMessageConverters().stream().anyMatch(MessageConverterUtil::servesXml));

    RestTemplate pinned = MessageConverterUtil.withoutXmlConverters(restTemplate);

    assertSame(restTemplate, pinned);
    assertFalse(pinned.getMessageConverters().isEmpty());
    assertFalse(pinned.getMessageConverters().stream().anyMatch(MessageConverterUtil::servesXml));
  }
}
