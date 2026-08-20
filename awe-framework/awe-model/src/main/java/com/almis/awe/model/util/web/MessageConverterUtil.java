package com.almis.awe.model.util.web;

import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * MessageConverterUtil Class
 * <p>
 * HTTP message converter utilities for AWE
 * </p>
 *
 * <p>Spring builds its converter lists from what it finds on the classpath, so an artifact such
 * as {@code jackson-dataformat-xml} — which can arrive transitively through any dependency,
 * without the application ever asking for it — is enough to add an XML converter to those lists.
 * That does not turn every response into XML: a request sending {@code Accept: *&#47;*} still
 * receives JSON. What it does change is any client whose {@code Accept} ranks an XML media type
 * above {@code *&#47;*} — a browser's default {@code Accept} does — plus the acceptance of XML
 * request bodies, and, on a {@code RestTemplate}, the serialisation of a body sent without an
 * explicit content type. AWE's own API contract is JSON, and that contract must not depend on the
 * dependency graph. These helpers express the rule once, over media types rather than over
 * converter classes, so no compile-time dependency on any XML provider is needed.</p>
 *
 * @see <a href="https://gitlab.com/aweframework/awe/-/issues/742">aweframework/awe#742</a>
 */
public final class MessageConverterUtil {

  private static final String XML_SUBTYPE = "xml";
  private static final String XML_SUFFIX = "+xml";

  private MessageConverterUtil() {
    // Utility class
  }

  /**
   * Check whether a converter serves an XML media type
   *
   * @param converter Message converter to check
   * @return true when the converter supports any XML media type
   */
  public static boolean servesXml(HttpMessageConverter<?> converter) {
    return converter.getSupportedMediaTypes().stream().anyMatch(MessageConverterUtil::isXml);
  }

  /**
   * Remove every XML converter from a converter list, keeping the rest in order
   *
   * @param converters Converter list to clean, modified in place
   */
  public static void removeXmlConverters(List<HttpMessageConverter<?>> converters) {
    converters.removeIf(MessageConverterUtil::servesXml);
  }

  /**
   * Remove every XML converter from a rest template, so a body sent without an explicit content
   * type is never serialised as XML
   *
   * @param restTemplate Rest template to pin
   * @return the same rest template, for chaining
   */
  public static RestTemplate withoutXmlConverters(RestTemplate restTemplate) {
    removeXmlConverters(restTemplate.getMessageConverters());
    return restTemplate;
  }

  /**
   * Check whether a media type is an XML media type
   *
   * @param mediaType Media type to check
   * @return true for {@code application/xml}, {@code text/xml} and any {@code +xml} suffix
   */
  private static boolean isXml(MediaType mediaType) {
    String subtype = mediaType.getSubtype();
    return XML_SUBTYPE.equals(subtype) || subtype.endsWith(XML_SUFFIX);
  }
}
