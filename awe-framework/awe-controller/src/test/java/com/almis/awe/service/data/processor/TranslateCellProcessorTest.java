package com.almis.awe.service.data.processor;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.entities.Global;
import com.almis.awe.model.entities.enumerated.EnumeratedGroup;
import com.almis.awe.model.entities.queries.Field;
import com.almis.awe.model.entities.queries.SqlField;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslateCellProcessorTest {

  @Mock
  private AweElements aweElements;

  @Test
  void getField() {
    SqlField field = new Field().setId("fieldId");
    TranslateCellProcessor processor = new TranslateCellProcessor(aweElements, field, Collections.emptyMap(), new EnumeratedGroup());
    assertEquals("fieldId", processor.getField().getIdentifier());
  }

  @Test
  void getTranslateEnumerated() {
    SqlField field = new Field().setId("fieldId");
    TranslateCellProcessor processor = new TranslateCellProcessor(aweElements, field, Collections.emptyMap(), new EnumeratedGroup().setId("translateEnum"));
    assertEquals("translateEnum", processor.getTranslateEnumerated().getId());
  }

  @Test
  void getColumnIdentifier() {
    SqlField field = new Field().setId("fieldId");
    TranslateCellProcessor processor = new TranslateCellProcessor(aweElements, field, Collections.emptyMap(), new EnumeratedGroup());
    assertEquals("fieldId", processor.getColumnIdentifier());
  }

  @Test
  void processNoLanguage() throws Exception {
    SqlField field = new Field().setId("fieldId");
    EnumeratedGroup enumeratedGroup = new EnumeratedGroup()
      .setOptionList(Arrays.asList(
        new Global().setValue("test1").setLabel("prueba"),
        new Global().setValue("test2").setLabel("lala"),
        new Global().setValue("prueba").setLabel("lolo"),
        new Global().setValue("test4").setLabel("lerele")));
    when(aweElements.getLocaleWithLanguage("lolo", null)).thenReturn("result");
    TranslateCellProcessor processor = new TranslateCellProcessor(aweElements, field, Collections.emptyMap(), enumeratedGroup);
    CellData expected = new CellData("prueba");
    assertEquals("result", processor.process(expected).getStringValue());
  }

  @Test
  void processSessionLanguage() throws Exception {
    SqlField field = new Field().setId("fieldId");
    EnumeratedGroup enumeratedGroup = new EnumeratedGroup()
      .setOptionList(Arrays.asList(
        new Global().setValue("test1").setLabel("prueba"),
        new Global().setValue("test2").setLabel("lala"),
        new Global().setValue("prueba").setLabel("lolo"),
        new Global().setValue("test4").setLabel("lerele")));
    when(aweElements.getLanguage()).thenReturn("es");
    when(aweElements.getLocaleWithLanguage("lolo", "es")).thenReturn("result");
    TranslateCellProcessor processor = new TranslateCellProcessor(aweElements, field, Collections.emptyMap(), enumeratedGroup);
    CellData expected = new CellData("prueba");
    assertEquals("result", processor.process(expected).getStringValue());
  }

  @Test
  void processParameterLanguage() throws Exception {
    SqlField field = new Field().setId("fieldId");
    EnumeratedGroup enumeratedGroup = new EnumeratedGroup()
      .setOptionList(Arrays.asList(
        new Global().setValue("test1").setLabel("prueba"),
        new Global().setValue("test2").setLabel("lala"),
        new Global().setValue("prueba").setLabel("lolo"),
        new Global().setValue("test4").setLabel("lerele")));
    when(aweElements.getLanguage()).thenReturn("es");
    when(aweElements.getLocaleWithLanguage("lolo", "en")).thenReturn("result");
    Map<String, QueryParameter> variables = new HashMap<>();
    QueryParameter parameter = new QueryParameter(JsonNodeFactory.instance.objectNode());
    parameter.setValue(JsonNodeFactory.instance.textNode("en"));
    variables.put("lang", parameter);
    TranslateCellProcessor processor = new TranslateCellProcessor(aweElements, field, variables, enumeratedGroup);
    CellData expected = new CellData("prueba");
    assertEquals("result", processor.process(expected).getStringValue());
  }
}