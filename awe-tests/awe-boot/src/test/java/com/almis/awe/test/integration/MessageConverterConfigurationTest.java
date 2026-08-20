package com.almis.awe.test.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that a real AWE application serves JSON and never XML, whatever the classpath offers.
 *
 * <p>This is the wiring counterpart of {@code WebMvcConfigTest}: that one asserts the configurer's
 * behaviour in isolation, this one asserts the configurer is actually picked up by a booted
 * application. awe-boot carries {@code jackson-dataformat-xml} transitively through ADE's
 * JasperReports dependency, so without the configurer Spring MVC would register an XML converter
 * here and this test would fail (aweframework/awe#742).</p>
 *
 * <p>The absence check is expressed directly over the converters' supported media types rather than
 * through the shared removal predicate, so this guard stays independent of the code it guards.</p>
 */
@Tag("integration")
@DisplayName("HTTP message converter configuration tests")
class MessageConverterConfigurationTest extends AbstractSpringFixedEnvironmentIT {

  private static final String WILDCARD_SUBTYPE = "*";

  @Autowired
  private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

  @Test
  @DisplayName("The served converter list carries no XML converter")
  void servedConvertersHaveNoXml() {
    List<HttpMessageConverter<?>> converters = requestMappingHandlerAdapter.getMessageConverters();

    assertFalse(converters.isEmpty(), "No message converter was registered");
    assertTrue(converters.stream().noneMatch(MessageConverterConfigurationTest::servesXmlMediaType),
      "An XML message converter reached the served list: AWE's wire format would depend on the classpath");
  }

  /**
   * Check whether a converter advertises an XML media type, deciding it locally from the converter's
   * own supported media types so this guard never delegates to the code it guards.
   *
   * <p>Only format-less wildcards are skipped — {@code *&#47;*} and {@code application/*} carry no
   * format of their own, and {@link MediaType#isCompatibleWith} answers true for every wildcard.
   * The skip therefore tests the subtype for the bare {@code *} rather than calling
   * {@link MediaType#isWildcardSubtype()}, which is also true for a subtype such as {@code *+xml}
   * that does name XML. What is left is matched against {@code application/xml}, {@code text/xml}
   * and the {@code +xml} structured suffix, so both {@code application/hal+xml} and
   * {@code application/*+xml} are detected.</p>
   *
   * @param converter Served message converter to check
   * @return true when the converter advertises any XML media type
   */
  private static boolean servesXmlMediaType(HttpMessageConverter<?> converter) {
    return converter.getSupportedMediaTypes().stream()
      .filter(mediaType -> !mediaType.isWildcardType() && !WILDCARD_SUBTYPE.equals(mediaType.getSubtype()))
      .anyMatch(mediaType -> mediaType.isCompatibleWith(MediaType.APPLICATION_XML)
        || mediaType.isCompatibleWith(MediaType.TEXT_XML)
        || mediaType.getSubtype().endsWith("+xml"));
  }

  @Test
  @DisplayName("The served converter list still carries the JSON converter")
  void servedConvertersKeepJson() {
    assertTrue(requestMappingHandlerAdapter.getMessageConverters().stream()
        .anyMatch(MappingJackson2HttpMessageConverter.class::isInstance),
      "The JSON message converter is missing from the served list");
  }
}
