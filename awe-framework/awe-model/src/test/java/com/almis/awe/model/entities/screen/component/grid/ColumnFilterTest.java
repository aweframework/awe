package com.almis.awe.model.entities.screen.component.grid;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Column autofilter attributes (issue #912): they are read from the XML and reach the clients, and a column without
 * them serializes as before.
 */
class ColumnFilterTest {

  private static XStream xStreamMarshaller;

  @BeforeAll
  static void beforeAll() {
    xStreamMarshaller = new XStream();
    xStreamMarshaller.autodetectAnnotations(true);
    xStreamMarshaller.processAnnotations(Column.class);
    xStreamMarshaller.allowTypesByWildcard(new String[]{"com.almis.awe.model.entities.**"});
    xStreamMarshaller.aliasSystemAttribute(null, "class");
  }

  private Column parse(String attributes) {
    return (Column) xStreamMarshaller.fromXML("<column name=\"user\" label=\"USER\" " + attributes + "/>");
  }

  private JsonNode toJson(Column column) {
    return JsonMapper.builder().build().valueToTree(column);
  }

  @Test
  @DisplayName("filter and filter-fields are parsed from the column and reach the clients as filter and filterFields")
  void columnWithFilter() {
    Column column = parse("filter=\"text\" filter-fields=\"Nam,Eml\"");

    assertThat(column.getFilter()).isEqualTo("text");
    assertThat(column.getFilterFields()).isEqualTo("Nam,Eml");
    JsonNode json = toJson(column);
    assertThat(json.get("filter").asString()).isEqualTo("text");
    assertThat(json.get("filterFields").asString()).isEqualTo("Nam,Eml");
  }

  @Test
  @DisplayName("A column without filter serializes exactly as before")
  void columnWithoutFilter() {
    Column column = parse("sort-field=\"UsrNam\"");

    assertThat(column.getFilter()).isNull();
    assertThat(column.getFilterFields()).isNull();
    JsonNode json = toJson(column);
    assertThat(json.get("sortField").asString()).isEqualTo("UsrNam");
    assertThat(json.has("filter")).isFalse();
    assertThat(json.has("filterFields")).isFalse();
  }
}
