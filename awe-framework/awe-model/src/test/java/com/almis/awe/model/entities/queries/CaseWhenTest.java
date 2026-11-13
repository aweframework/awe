package com.almis.awe.model.entities.queries;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaseWhenTest {

  private static final String CASE_WHEN_XML = """
    <case alias="label">
       <when left-field="Nam" condition="eq" right-variable="purple-hills">
         <then>
           <constant value="PURPLE-HILLS"/>
         </then>
       </when>
       <when>
         <and>
           <filter left-field="Nam" condition="eq" right-variable="sunny"/>
           <filter left-field="Nam" condition="is not null"/>
         </and>
         <then>
           <constant value="SUNNY"/>
         </then>
       </when>
       <else>
         <constant value="null" type="NULL"/>
       </else>
     </case>""";

  private static XStream xStreamMarshaller;

  @BeforeAll
  static void beforeAll() {
    // Setup marshaller
    xStreamMarshaller = new XStream();
    xStreamMarshaller.autodetectAnnotations(true);
    xStreamMarshaller.processAnnotations(Case.class);
    xStreamMarshaller.allowTypesByWildcard(new String[] {"com.almis.awe.model.entities.**"});
    xStreamMarshaller.aliasSystemAttribute(null, "class");
  }

  @Test
  void copyCaseWhenTest() {
    // Given
    CaseWhen caseWhenOriginal = ((Case) xStreamMarshaller.fromXML(CASE_WHEN_XML)).getCaseWhenList().get(0);
    // When
    CaseWhen caseWhenCopied = caseWhenOriginal.copy();
    // Then
    assertEquals(caseWhenOriginal.getLeftField(), caseWhenCopied.getLeftField());
    assertEquals(caseWhenOriginal.getCondition(), caseWhenCopied.getCondition());
    assertEquals(caseWhenOriginal.getRightVariable(), caseWhenCopied.getRightVariable());
  }

  @Test
  void testCaseWhenToString() {
    // Given
    final String expected = "[WHEN (Nam eq variable(purple-hills)) THEN \"PURPLE-HILLS\", WHEN ((Nam eq variable(sunny)) and (Nam is not null )) THEN \"SUNNY\"]";
    // When
    List<CaseWhen> caseWhenList = ((Case) xStreamMarshaller.fromXML(CASE_WHEN_XML)).getCaseWhenList();
    // Then
    assertEquals(expected, caseWhenList.toString());
  }
}
