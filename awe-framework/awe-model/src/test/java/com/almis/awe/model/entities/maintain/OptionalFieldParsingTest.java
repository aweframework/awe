package com.almis.awe.model.entities.maintain;

import com.almis.awe.model.entities.queries.SqlField;
import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parsing tests for the maintain {@code optional} field attribute (issue #344).
 * <p>
 * The builder tests cover what the attribute does; these cover that it actually arrives from
 * the XML, which construction-by-builder would never catch.
 */
class OptionalFieldParsingTest {

  private static final String UPDATE_WITH_OPTIONAL_FIELD = """
    <update audit="HISAweThm">
      <table id="AweThm"/>
      <field id="IdeThm" table="AweThm" variable="themeId"/>
      <field id="Nam" table="AweThm" variable="themeName" optional="true"/>
      <field id="Act" table="AweThm" variable="themeActive" optional="false"/>
      <variable id="themeId" type="INTEGER" name="themeId"/>
      <variable id="themeName" type="STRING" name="themeName"/>
      <variable id="themeActive" type="INTEGER" name="themeActive"/>
    </update>""";

  private static XStream xStreamMarshaller;

  @BeforeAll
  static void beforeAll() {
    xStreamMarshaller = new XStream();
    xStreamMarshaller.autodetectAnnotations(true);
    xStreamMarshaller.processAnnotations(Update.class);
    xStreamMarshaller.allowTypesByWildcard(new String[]{"com.almis.awe.model.entities.**"});
    xStreamMarshaller.aliasSystemAttribute(null, "class");
  }

  @Test
  @DisplayName("optional=\"true\" is parsed from the field element")
  void optionalTrueIsParsed() {
    assertTrue(field("Nam").isOptional());
  }

  @Test
  @DisplayName("optional=\"false\" is parsed as not optional")
  void optionalFalseIsParsed() {
    assertFalse(field("Act").isOptional());
  }

  @Test
  @DisplayName("A field without the attribute is not optional, so existing maintains are unchanged")
  void missingAttributeIsNotOptional() {
    SqlField field = field("IdeThm");

    assertNull(field.getOptional());
    assertFalse(field.isOptional());
  }

  @Test
  @DisplayName("copy() preserves the optional attribute")
  void copyPreservesOptional() {
    assertTrue(((SqlField) field("Nam").copy()).isOptional());
    assertFalse(((SqlField) field("Act").copy()).isOptional());
  }

  private SqlField field(String id) {
    Update update = (Update) xStreamMarshaller.fromXML(UPDATE_WITH_OPTIONAL_FIELD);
    List<SqlField> fields = update.getSqlFieldList();
    return fields.stream()
      .filter(parsed -> id.equals(parsed.getId()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("No field parsed with id " + id));
  }
}
