package com.almis.awe.builder.screen.grid;

import com.almis.awe.builder.enumerates.InitialLoad;
import com.almis.awe.builder.enumerates.ServerAction;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.entities.screen.component.grid.Column;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Radio and button-radio column builders")
class RadioColumnBuilderTest {

  @Test
  @DisplayName("A radio column takes its options from a list, like a select column")
  void radioColumnKeepsItsOptionList() throws AWException {
    Column column = new RadioColumnBuilder()
      .setId("choice")
      .setLabel("CHOICE")
      .setInitialLoad(InitialLoad.ENUMERATED)
      .setTargetAction("Es1Es0")
      .setSendable(true)
      .build();

    assertEquals("radio", column.getComponentType());
    assertEquals("choice", column.getId());
    assertTrue(InitialLoad.ENUMERATED.equalsStr(column.getInitialLoad()));
    assertEquals("Es1Es0", column.getTargetAction());
    assertTrue(column.getSendable());
    assertNull(column.getValue());
  }

  @Test
  @DisplayName("A button-radio column takes its options from a query")
  void buttonRadioColumnKeepsItsOptionList() throws AWException {
    Column column = new ButtonRadioColumnBuilder()
      .setId("mode")
      .setInitialLoad(InitialLoad.QUERY)
      .setServerAction(ServerAction.DATA)
      .setTargetAction("ModeList")
      .setReadonly(true)
      .build();

    assertEquals("button-radio", column.getComponentType());
    assertTrue(InitialLoad.QUERY.equalsStr(column.getInitialLoad()));
    assertEquals("ModeList", column.getTargetAction());
    assertTrue(column.getReadonly());
  }

  @Test
  @DisplayName("The columns have no group: their options are the list of the column")
  void columnsHaveNoGroup() throws AWException {
    assertNull(new RadioColumnBuilder().setId("alone").build().getGroup());
    assertNull(new ButtonRadioColumnBuilder().setId("alone").build().getGroup());
    assertFalse(java.util.Arrays.stream(RadioColumnBuilder.class.getDeclaredMethods()).anyMatch(method -> method.getName().equals("setGroup")));
    assertFalse(java.util.Arrays.stream(ButtonRadioColumnBuilder.class.getDeclaredMethods()).anyMatch(method -> method.getName().equals("setGroup")));
  }
}
