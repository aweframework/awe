package com.almis.awe.model.entities.maintain;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the maintain target {@code server-only} attribute (issue #886).
 */
class TargetServerOnlyTest {

  private static XStream xStreamMarshaller;

  @BeforeAll
  static void beforeAll() {
    xStreamMarshaller = new XStream();
    xStreamMarshaller.autodetectAnnotations(true);
    xStreamMarshaller.processAnnotations(Target.class);
    xStreamMarshaller.allowTypesByWildcard(new String[]{"com.almis.awe.model.entities.**"});
    xStreamMarshaller.aliasSystemAttribute(null, "class");
  }

  private Target parse(String attributes) {
    return (Target) xStreamMarshaller.fromXML("<target name=\"t\" " + attributes + "/>");
  }

  @Test
  @DisplayName("A target without the attribute is not server-only")
  void defaultIsNotServerOnly() {
    assertThat(new Target().isServerOnly()).isFalse();
    assertThat(parse("").isServerOnly()).isFalse();
  }

  @Test
  @DisplayName("server-only=\"true\" is parsed from the target element")
  void serverOnlyTrueIsParsed() {
    assertThat(parse("server-only=\"true\"").isServerOnly()).isTrue();
  }

  @Test
  @DisplayName("server-only=\"false\" is parsed from the target element")
  void serverOnlyFalseIsParsed() {
    assertThat(parse("server-only=\"false\"").isServerOnly()).isFalse();
  }

  @Test
  @DisplayName("server-only is independent of public")
  void serverOnlyIsIndependentOfPublic() {
    Target target = parse("public=\"true\" server-only=\"true\"");
    assertThat(target.isPublic()).isTrue();
    assertThat(target.isServerOnly()).isTrue();
  }

  @Test
  @DisplayName("copy keeps server-only")
  void copyKeepsServerOnly() {
    assertThat(parse("server-only=\"true\"").copy().isServerOnly()).isTrue();
  }
}
