package com.almis.awe.builder.screen;

import com.almis.awe.builder.enumerates.AdvancedMode;
import com.almis.awe.builder.enumerates.ButtonType;
import com.almis.awe.builder.screen.button.ButtonBuilder;
import com.almis.awe.builder.screen.criteria.SelectCriteriaBuilder;
import com.almis.awe.builder.screen.criteria.TextCriteriaBuilder;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.component.Advanced;
import com.almis.awe.model.entities.screen.component.Window;
import com.almis.awe.model.entities.screen.component.button.Button;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Builders can add an advanced block to a window and choose how the window shows it (#790)
 */
class AdvancedSearchBuilderTest {

  @Test
  void buildsAWindowWithEssentialCriteriaAnAdvancedBlockAndButtons() throws Exception {
    Window window = new WindowBuilder()
      .setAdvancedMode(AdvancedMode.SIDE_PANEL)
      .setId("WinCrt")
      .addTag(new TagBuilder().setType("div").setStyle("panel-body")
        .addCriteria(new TextCriteriaBuilder().setId("CrtUsr"))
        .addAdvanced(new AdvancedBuilder().setId("AdvCrt")
          .addTag(new TagBuilder().setType("div").addCriteria(new SelectCriteriaBuilder().setId("CrtThm")))))
      .addButton(new ButtonBuilder().setButtonType(ButtonType.SUBMIT).setId("ButSch"))
      .build();

    assertEquals("side-panel", window.getAdvancedMode());
    List<Element> body = window.getElementList().get(0).getElementList();
    Advanced advanced = assertInstanceOf(Advanced.class, body.get(1));
    assertEquals("AdvCrt", advanced.getId());
    assertInstanceOf(Button.class, window.getElementList().get(1));
    assertEquals(List.of("CrtThm"), window.getAdvancedCriteria());
    assertEquals(List.of("CrtUsr", "CrtThm"), window.getSearchCriteria());
  }

  @Test
  void aWindowCanHoldTheAdvancedBlockDirectly() throws Exception {
    Window window = new WindowBuilder()
      .addAdvanced(new AdvancedBuilder().addCriteria(new TextCriteriaBuilder().setId("CrtThm")))
      .build();

    assertInstanceOf(Advanced.class, window.getElementList().get(0));
    assertEquals("inline", window.getAdvancedModeConverter());
  }

  @Test
  void aWindowHasNoAdvancedModeByDefault() {
    Window window = new WindowBuilder().build();

    assertNull(window.getAdvancedMode());
    assertNull(window.getAdvancedModeConverter());
  }
}
