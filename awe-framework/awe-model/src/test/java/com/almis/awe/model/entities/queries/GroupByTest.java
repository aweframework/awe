package com.almis.awe.model.entities.queries;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupByTest {

  private static final String GROUP_BY_WITH_CASE = """
    <query id="testGroupByWithCase">
        <table id="ope" alias="o"/>
        <field id="l1_nom" table="o" alias="Nam"/>
        <case alias="theme">
          <when left-field="IdeThm" condition="eq" right-variable="1">
            <then>
              <constant value="SUNSET"/>
            </then>
          </when>
          <else>
            <constant value="OTHER" />
          </else>
        </case>
        <order-by field="l1_nom" table="o" type="ASC" nulls="FIRST"/>
        <group-by field="l1_nom"/>
        <group-by>
          <case alias="theme">
            <when left-field="IdeThm" condition="eq" right-variable="1">
              <then>
                <constant value="SUNSET"/>
              </then>
            </when>
            <else>
              <constant value="OTHER" />
            </else>
          </case>
        </group-by>
        <variable id="1" type="INTEGER" value="1"/>
      </query>""";

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
