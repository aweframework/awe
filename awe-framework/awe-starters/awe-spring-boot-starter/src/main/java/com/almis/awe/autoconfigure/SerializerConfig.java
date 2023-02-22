package com.almis.awe.autoconfigure;

import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.model.component.XStreamSerializer;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.access.Profile;
import com.almis.awe.model.entities.actions.Actions;
import com.almis.awe.model.entities.email.Emails;
import com.almis.awe.model.entities.enumerated.Enumerated;
import com.almis.awe.model.entities.locale.Locales;
import com.almis.awe.model.entities.maintain.Maintain;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.queries.Queries;
import com.almis.awe.model.entities.queues.Queues;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.services.Services;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.graalvm.polyglot.Context;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.oxm.xstream.XStreamMarshaller;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Initialize serializer beans
 *
 * @author pgarcia
 */
@Configuration
public class SerializerConfig {

  // Autowired services
  private final SecurityConfigProperties securityConfigProperties;

  private ThreadLocal<Context> engineThread;

  /**
   * SerializeConfig constructor
   * @param securityConfigProperties Security configuration properties
   */
  public SerializerConfig(SecurityConfigProperties securityConfigProperties) {
    this.securityConfigProperties = securityConfigProperties;
  }

  /**
   * On construct initialize thread local
   */
  @PostConstruct
  public void onConstruct() {
    engineThread = ThreadLocal.withInitial(() -> Context.create("js"));
  }

  /**
   * On destroy remove thread local
   */
  @PreDestroy
  public void onDestroy() {
    engineThread.remove();
  }

  /**
   * XStream serializer
   *
   * @return Serializer
   */
  @Bean
  @ConditionalOnMissingBean
  public XStreamSerializer xStreamSerializer(XStreamMarshaller xStreamMarshaller) {
    // Configure xstream security
    XStream xstream = xStreamMarshaller.getXStream();
    // allow any type from the same package
    xstream.allowTypesByWildcard(securityConfigProperties.getXstreamAllowPaths());
    // Retrieve serializer
    return new XStreamSerializer(xStreamMarshaller);
  }

  /**
   * Get XML management Engine
   *
   * @return XStream Marshaller
   */
  @Bean
  @ConditionalOnMissingBean
  public XStreamMarshaller xStreamMarshaller() {
    XStreamMarshaller xstreamMarshaller = new XStreamMarshaller();
    xstreamMarshaller.setStreamDriver(new DomDriver(null, new NoNameCoder()));

    // Process annotations
    xstreamMarshaller.getXStream().processAnnotations(new Class[]{
      Element.class,
      Enumerated.class,
      Queries.class,
      Queues.class,
      Maintain.class,
      Emails.class,
      Services.class,
      Actions.class,
      Profile.class,
      Screen.class,
      Menu.class,
      Locales.class
    });

    return xstreamMarshaller;
  }

  /**
   * Get Javascript management Engine
   *
   * @return Javascript engine
   */
  @Bean
  @Scope("prototype")
  public Context javascriptEngine() {
    return engineThread.get();
  }
}
