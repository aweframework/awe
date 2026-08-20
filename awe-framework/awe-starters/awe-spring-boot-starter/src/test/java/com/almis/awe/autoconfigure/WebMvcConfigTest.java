package com.almis.awe.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that AWE pins the media types it serves instead of inheriting whatever the
 * classpath happens to offer.
 *
 * <p>Spring MVC registers an XML converter as soon as an XML provider such as
 * {@code jackson-dataformat-xml} is present, and that artifact can arrive transitively through any
 * dependency. Without this configurer, a client whose {@code Accept} ranks an XML media type above
 * {@code *&#47;*} — a browser's default {@code Accept} does — receives XML from endpoints whose
 * contract is JSON, which is the defect tracked in aweframework/awe#742.</p>
 *
 * <p>The XML cases below are built from converters this module already depends on
 * ({@link SourceHttpMessageConverter}) plus a local stub for the {@code +xml} structured suffix, so
 * the test classpath never relies on a transitive XML provider. The stub can neither read nor write,
 * which is deliberate: the removal rule keys off the media types a converter advertises, so
 * read-capable and write-incapable XML converters are removed alike.</p>
 */
class WebMvcConfigTest {

  private final WebMvcConfig config = new WebMvcConfig();

  /**
   * Converter advertising a media type with the {@code +xml} structured suffix, while being able to
   * neither read nor write it
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

  /**
   * Application configurer that seeds the converter list and then appends an XML converter in the
   * extend phase. It declares no order, which is the realistic default for an application
   * configurer
   */
  private static class XmlAddingConfigurer implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
      converters.add(new StringHttpMessageConverter());
      converters.add(new MappingJackson2HttpMessageConverter());
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
      converters.add(new SourceHttpMessageConverter<>());
    }
  }

  /**
   * Same configurer, but sorting strictly before an unordered one, so AWE's removal runs after it
   */
  private static class HigherPrecedenceXmlAddingConfigurer extends XmlAddingConfigurer implements Ordered {

    @Override
    public int getOrder() {
      return Ordered.LOWEST_PRECEDENCE - 1;
    }
  }

  /**
   * Exposes the converter list Spring MVC actually serves, built through the real two-phase
   * delegation over a composite of configurers
   */
  private static class DelegatingConfiguration extends DelegatingWebMvcConfiguration {

    List<HttpMessageConverter<?>> servedConverters() {
      return getMessageConverters();
    }
  }

  /**
   * Build a converter list shaped like Spring MVC's own defaults, XML converters included.
   *
   * @return mutable converter list
   */
  private List<HttpMessageConverter<?>> defaultConverters() {
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new ByteArrayHttpMessageConverter());
    converters.add(new StringHttpMessageConverter());
    converters.add(new ResourceHttpMessageConverter());
    converters.add(new SourceHttpMessageConverter<>());
    converters.add(new SuffixXmlConverter());
    converters.add(new MappingJackson2HttpMessageConverter());
    return converters;
  }

  /**
   * Compose AWE's configurer with an application one through the real Spring MVC delegation, in the
   * sorted order Spring MVC would apply.
   *
   * @param applicationConfigurer Application configurer to compose with AWE's
   * @return the served converter list produced by both phases
   */
  private List<HttpMessageConverter<?>> servedConverters(WebMvcConfigurer applicationConfigurer) {
    List<WebMvcConfigurer> configurers = new ArrayList<>();
    configurers.add(config);
    configurers.add(applicationConfigurer);
    AnnotationAwareOrderComparator.sort(configurers);

    DelegatingConfiguration configuration = new DelegatingConfiguration();
    configuration.setConfigurers(configurers);
    return configuration.servedConverters();
  }

  @Test
  @DisplayName("Drops every XML-advertising converter from the served list, read-capable or not")
  void extendMessageConverters_removesXmlConverters() {
    List<HttpMessageConverter<?>> converters = defaultConverters();

    config.extendMessageConverters(converters);

    assertThat(converters)
      .noneMatch(SourceHttpMessageConverter.class::isInstance)
      .noneMatch(SuffixXmlConverter.class::isInstance);
  }

  @Test
  @DisplayName("Keeps the JSON converter and every non-XML converter untouched")
  void extendMessageConverters_keepsJsonAndNonXmlConverters() {
    List<HttpMessageConverter<?>> converters = defaultConverters();

    config.extendMessageConverters(converters);

    assertThat(converters)
      .hasSize(4)
      .anyMatch(MappingJackson2HttpMessageConverter.class::isInstance)
      .anyMatch(ByteArrayHttpMessageConverter.class::isInstance)
      .anyMatch(StringHttpMessageConverter.class::isInstance)
      .anyMatch(ResourceHttpMessageConverter.class::isInstance);
  }

  @Test
  @DisplayName("Does nothing when no XML converter is present")
  void extendMessageConverters_withoutXmlConverters_leavesListUntouched() {
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(new StringHttpMessageConverter());
    converters.add(new MappingJackson2HttpMessageConverter());

    config.extendMessageConverters(converters);

    assertThat(converters).hasSize(2);
  }

  /**
   * Proves what the removal does over a composed converter list: when the configurer that appends
   * XML sorts before AWE's, AWE's extend-phase removal clears that converter from the list Spring
   * MVC serves.
   *
   * <p>This asserts the removal, not the ordering: AWE does not and cannot claim to sort last, so
   * the rival configurer here is given a strictly higher precedence on purpose.</p>
   */
  @Test
  @DisplayName("Clears the XML converter added by a configurer that sorts before AWE")
  void servedConverters_whenXmlConfigurerSortsFirst_carryNoXml() {
    assertThat(servedConverters(new HigherPrecedenceXmlAddingConfigurer()))
      .noneMatch(SourceHttpMessageConverter.class::isInstance)
      .anyMatch(MappingJackson2HttpMessageConverter.class::isInstance);
  }

  /**
   * Documents the boundary of the removal: an unordered application configurer ties with AWE's own
   * configurer, because not implementing {@link Ordered} and returning
   * {@link Ordered#LOWEST_PRECEDENCE} are the same order value, and the tie is broken by
   * registration order through a stable sort. When that configurer is registered after AWE's, its
   * extend-phase XML converter is appended once AWE has already run and therefore survives.
   *
   * <p>This is precisely why the supported way to serve XML is the
   * {@code awe.application.json-only=false} property and not configurer ordering.</p>
   */
  @Test
  @DisplayName("Cannot outrun an unordered configurer registered later, so the opt-out is the property")
  void servedConverters_whenUnorderedXmlConfigurerIsRegisteredLast_stillCarryXml() {
    assertThat(servedConverters(new XmlAddingConfigurer()))
      .anyMatch(SourceHttpMessageConverter.class::isInstance)
      .anyMatch(MappingJackson2HttpMessageConverter.class::isInstance);
  }

  @Test
  @DisplayName("Is a WebMvcConfigurer and is loadable as an auto-configuration")
  void isLoadableAsAutoConfiguration() {
    new WebApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(WebMvcConfig.class))
      .run(context -> assertThat(context)
        .hasSingleBean(WebMvcConfig.class)
        .getBean(WebMvcConfig.class)
        .isInstanceOf(WebMvcConfigurer.class));
  }
}
