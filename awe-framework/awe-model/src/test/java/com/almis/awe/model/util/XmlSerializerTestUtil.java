package com.almis.awe.model.util;

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
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.springframework.oxm.xstream.XStreamMarshaller;

/**
 * Test utility to build an XStream serializer configured like SerializerConfig does at runtime
 */
public final class XmlSerializerTestUtil {

  /**
   * Hide utility class constructor
   */
  private XmlSerializerTestUtil() {
  }

  /**
   * Build an XStream serializer with the same driver, annotations and security settings
   * used by the framework autoconfiguration
   *
   * @return XStream serializer
   */
  public static XStreamSerializer buildSerializer() {
    XStreamMarshaller marshaller = new XStreamMarshaller();
    marshaller.setStreamDriver(new DomDriver(null, new NoNameCoder()));
    marshaller.getXStream().processAnnotations(new Class[]{
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
    marshaller.getXStream().allowTypesByWildcard(new String[]{"java.*", "com.almis.awe.model.entities.**"});
    return new XStreamSerializer(marshaller);
  }
}
