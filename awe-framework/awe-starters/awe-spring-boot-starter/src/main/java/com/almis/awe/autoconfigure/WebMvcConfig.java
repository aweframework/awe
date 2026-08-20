package com.almis.awe.autoconfigure;

import com.almis.awe.model.util.web.MessageConverterUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Awe Web MVC configuration.
 *
 * <p>Pins the media types AWE serves. Spring MVC builds its converter list from what it finds on
 * the classpath, so an artifact such as {@code jackson-dataformat-xml} — which can arrive
 * transitively through any dependency, without the application ever asking for it — is enough to
 * add an XML converter to AWE's endpoints. AWE's own API contract is JSON, and that contract must
 * not depend on the dependency graph.</p>
 *
 * <p>The measured exposure is content negotiation, not every request. With an XML converter
 * registered, a request sending {@code Accept: *&#47;*} still receives JSON, because Spring Boot
 * moves XML converters to the end of the list. What breaks is any client whose {@code Accept}
 * ranks an XML media type above {@code *&#47;*}: a browser's default
 * {@code text/html,application/xhtml+xml,application/xml;q=0.9,...,*&#47;*;q=0.8} receives
 * {@code application/xhtml+xml}, and an explicit {@code Accept: application/xml} receives
 * {@code application/xml}. On top of that, the endpoints start accepting XML request bodies.</p>
 *
 * <p>This configurer therefore drops every XML-advertising converter from the served list, using
 * the shared rule in {@link MessageConverterUtil}. That rule keys off the media types a converter
 * advertises and ignores {@code canRead} and {@code canWrite}, so read-capable XML converters are
 * removed too — which is exactly what closes the accept side named above, since a converter takes
 * part in content negotiation and in request-body acceptance through the media types it advertises.
 * With the pin in place, a browser-style {@code Accept} receives JSON and an explicit
 * {@code Accept: application/xml} receives {@code 406 Not Acceptable}, which is the intended
 * contract: AWE does not serve XML.</p>
 *
 * <p>The removal happens in the {@code extendMessageConverters} phase, because
 * {@code WebMvcConfigurationSupport} runs {@code configureMessageConverters} for every configurer
 * first and only then {@code extendMessageConverters} for every configurer, so ordering sorts
 * configurers within a phase only. This class deliberately does not implement
 * {@link org.springframework.core.Ordered}: the lowest precedence value is exactly the order
 * {@code OrderComparator} already assigns to a configurer that does not implement that interface,
 * so declaring it would advertise a guarantee it cannot give. The removal therefore cannot be
 * guaranteed to run after an application configurer that appends an XML converter in that same
 * extend phase, and an application that wants to serve XML must use the property below rather than
 * rely on configurer ordering.</p>
 *
 * <p>An application that genuinely wants to serve XML opts out with
 * {@code awe.application.json-only=false}: this configurer then backs off entirely and Spring MVC
 * keeps whatever converters the classpath provides. The condition declares no {@code havingValue},
 * so it fails safe — any other value, including an empty value or a typo such as {@code flase},
 * leaves the pin active.</p>
 *
 * @see <a href="https://gitlab.com/aweframework/awe/-/issues/742">aweframework/awe#742</a>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebMvcConfigurer.class)
@ConditionalOnProperty(prefix = "awe.application", name = "json-only", matchIfMissing = true)
public class WebMvcConfig implements WebMvcConfigurer {

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    MessageConverterUtil.removeXmlConverters(converters);
  }
}
