package com.almis.awe.model.entities.queries;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupByTest {

  private static final String GROUP_BY_WITH_CASE = "" +
    "<query id=\"testGroupByWithCase\">\n" +
    "    <table id=\"ope\" alias=\"o\"/>\n" +
    "    <field id=\"l1_nom\" table=\"o\" alias=\"Nam\"/>\n" +
    "    <case alias=\"theme\">\n" +
    "      <when left-field=\"IdeThm\" condition=\"eq\" right-variable=\"1\">\n" +
    "        <then>\n" +
    "          <constant value=\"SUNSET\"/>\n" +
    "        </then>\n" +
    "      </when>\n" +
    "      <else>\n" +
    "        <constant value=\"OTHER\" />\n" +
    "      </else>\n" +
    "    </case>\n" +
    "    <order-by field=\"l1_nom\" table=\"o\" type=\"ASC\" nulls=\"FIRST\"/>\n" +
    "    <group-by field=\"l1_nom\"/>\n" +
    "    <group-by>\n" +
    "      <case alias=\"theme\">\n" +
    "        <when left-field=\"IdeThm\" condition=\"eq\" right-variable=\"1\">\n" +
    "          <then>\n" +
    "            <constant value=\"SUNSET\"/>\n" +
    "          </then>\n" +
    "        </when>\n" +
    "        <else>\n" +
    "          <constant value=\"OTHER\" />\n" +
    "        </else>\n" +
    "      </case>\n" +
    "    </group-by>\n" +
    "    <variable id=\"1\" type=\"INTEGER\" value=\"1\"/>\n" +
    "  </query>";

  private static XStream xStreamMarshaller;

  @BeforeAll
  static void beforeAll() {
    // Setup marshaller
    xStreamMarshaller = new XStream();
    xStreamMarshaller.autodetectAnnotations(true);
    xStreamMarshaller.processAnnotations(Query.class);
    xStreamMarshaller.allowTypesByWildcard(new String[]{"com.almis.awe.model.entities.**"});
    xStreamMarshaller.aliasSystemAttribute(null, "class");
  }

  @Test
  void testToStringGroupByWithCase() {
    // Given
    final String expected = "[l1_nom, CASE WHEN (IdeThm eq variable(1)) THEN \"SUNSET\" ELSE \"OTHER\" as theme]";
    // When
    List<GroupBy> groupBy = ((Query) xStreamMarshaller.fromXML(GROUP_BY_WITH_CASE)).getGroupByList();
    // Then
    assertEquals(expected, groupBy.toString());
  }
}