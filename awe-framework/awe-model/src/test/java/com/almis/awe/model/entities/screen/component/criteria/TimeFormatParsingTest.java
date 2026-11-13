package com.almis.awe.model.entities.screen.component.criteria;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parsing tests for the criteria {@code time-format} attribute (issue #190).
 */
class TimeFormatParsingTest {

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
  @DisplayName("time-format is parsed from the criteria element")
  void timeFormatIsParsed() {
    Criteria criteria = (Criteria) xStreamMarshaller.fromXML("<criteria component=\"time\" time-format=\"hh:mm a\"/>");

    assertThat(criteria.getTimeFormat()).isEqualTo("hh:mm a");
  }

  @Test
  @DisplayName("time-format is null when the attribute is missing")
  void timeFormatIsNullByDefault() {
    Criteria criteria = (Criteria) xStreamMarshaller.fromXML("<criteria component=\"time\"/>");

    assertThat(criteria.getTimeFormat()).isNull();
  }
}
