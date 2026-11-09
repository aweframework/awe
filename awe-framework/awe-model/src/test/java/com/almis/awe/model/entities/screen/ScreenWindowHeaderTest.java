package com.almis.awe.model.entities.screen;

import com.almis.awe.model.component.XStreamSerializer;
import com.almis.awe.model.type.WindowHeaderType;
import com.almis.awe.model.util.XmlSerializerTestUtil;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The window-header attribute of a screen is read from the descriptor, and parsed with a fallback (#925)
 */
class ScreenWindowHeaderTest {

  private final XStreamSerializer serializer = XmlSerializerTestUtil.buildSerializer();

  private Screen screen(String attributes) {
    String xml = "<screen template=\"window\" label=\"TITLE\" " + attributes + "/>";
    return serializer.getObjectFromXml(Screen.class, new StringReader(xml));
  }

  @Test
  void windowHeaderAttributeIsReadFromTheDescriptor() {
    assertThat(screen("window-header=\"none\"").getWindowHeader()).isEqualTo("none");
  }

  @Test
  void windowHeaderAttributeIsOptional() {
    assertThat(screen("").getWindowHeader()).isNull();
  }

  @Test
  void unknownHeaderFallsBackToTheGivenOne() {
    assertThat(WindowHeaderType.parse(" None ", WindowHeaderType.BREADCRUMB)).isEqualTo(WindowHeaderType.NONE);
    assertThat(WindowHeaderType.parse("other", WindowHeaderType.NONE)).isEqualTo(WindowHeaderType.NONE);
    assertThat(WindowHeaderType.parse(null, WindowHeaderType.BREADCRUMB)).isEqualTo(WindowHeaderType.BREADCRUMB);
  }
}
