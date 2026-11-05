package com.almis.awe.model.entities.queries;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the query {@code server-only} attribute
 */
class QueryServerOnlyTest {

  private static XStream xStreamMarshaller;

  @BeforeAll
  static void beforeAll() {
    xStreamMarshaller = new XStream();
    xStreamMarshaller.autodetectAnnotations(true);
    xStreamMarshaller.processAnnotations(Query.class);
    xStreamMarshaller.allowTypesByWildcard(new String[]{"com.almis.awe.model.entities.**"});
    xStreamMarshaller.aliasSystemAttribute(null, "class");
  }

  private Query parse(String attributes) {
    return (Query) xStreamMarshaller.fromXML("<query id=\"q\" " + attributes + "/>");
  }

  @Test
  @DisplayName("A query without the attribute is not server-only")
  void defaultIsNotServerOnly() {
    assertThat(new Query().isServerOnly()).isFalse();
    assertThat(parse("").isServerOnly()).isFalse();
  }

  @Test
  @DisplayName("server-only=\"true\" is parsed from the query element")
  void serverOnlyTrueIsParsed() {
    assertThat(parse("server-only=\"true\"").isServerOnly()).isTrue();
  }

  @Test
  @DisplayName("server-only=\"false\" is parsed from the query element")
  void serverOnlyFalseIsParsed() {
    assertThat(parse("server-only=\"false\"").isServerOnly()).isFalse();
  }

  @Test
  @DisplayName("server-only is independent of public")
  void serverOnlyIsIndependentOfPublic() {
    Query query = parse("public=\"true\" server-only=\"true\"");
    assertThat(query.isPublic()).isTrue();
    assertThat(query.isServerOnly()).isTrue();
  }

  @Test
  @DisplayName("copy keeps server-only")
  void copyKeepsServerOnly() {
    assertThat(parse("server-only=\"true\"").copy().isServerOnly()).isTrue();
  }
}
