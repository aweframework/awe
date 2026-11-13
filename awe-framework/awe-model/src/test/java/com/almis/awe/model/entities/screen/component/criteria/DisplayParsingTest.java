package com.almis.awe.model.entities.screen.component.criteria;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parsing tests for the criteria {@code display} attribute (issue #922).
 */
class DisplayParsingTest {

  private static XStream xStreamMarshaller;

  @BeforeAll
  static void beforeAll() {
    xStreamMarshaller = new XStream();
    xStreamMarshaller.autodetectAnnotations(true);
    xStreamMarshaller.processAnnotations(Criteria.class);
    xStreamMarshaller.allowTypesByWildcard(new String[]{"com.almis.awe.model.entities.**"});
    xStreamMarshaller.aliasSystemAttribute(null, "class");
  }

  @Test
  @DisplayName("display is parsed from the criteria element")
  void displayIsParsed() {
    Criteria criteria = (Criteria) xStreamMarshaller.fromXML("<criteria component=\"select-multiple\" display=\"chip\"/>");

    assertThat(criteria.getDisplay()).isEqualTo("chip");
  }

  @Test
  @DisplayName("display is null when the attribute is missing")
  void displayIsNullByDefault() {
    Criteria criteria = (Criteria) xStreamMarshaller.fromXML("<criteria component=\"select-multiple\"/>");

    assertThat(criteria.getDisplay()).isNull();
  }
}
