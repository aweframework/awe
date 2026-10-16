package com.almis.awe.model.entities.screen.component;

import com.almis.awe.model.entities.screen.component.container.TabContainer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dialogs and tab pages can defer the data of their components until they are opened (#824)
 */
class DeferredContainerTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void containersAreNotDeferredByDefault() {
    assertThat(new Dialog().isDeferred()).isFalse();
    assertThat(new TabContainer().isDeferred()).isFalse();
  }

  @Test
  void deferredFlagIsPublishedToTheClientOnlyWhenSet() {
    JsonNode plainDialog = mapper.valueToTree(new Dialog());
    JsonNode deferredDialog = mapper.valueToTree(deferredDialog());
    JsonNode deferredTab = mapper.valueToTree(deferredTab());
    JsonNode plainTab = mapper.valueToTree(new TabContainer());

    assertThat(plainDialog.has("deferred")).isFalse();
    assertThat(plainTab.has("deferred")).isFalse();
    assertThat(deferredDialog.get("deferred").asBoolean()).isTrue();
    assertThat(deferredTab.get("deferred").asBoolean()).isTrue();
  }

  @Test
  void deferredFlagSurvivesACopy() {
    assertThat(deferredDialog().copy().isDeferred()).isTrue();
    assertThat(deferredTab().copy().isDeferred()).isTrue();
  }

  private static Dialog deferredDialog() {
    Dialog dialog = new Dialog();
    dialog.setDeferred(true);
    return dialog;
  }

  private static TabContainer deferredTab() {
    TabContainer tabContainer = new TabContainer();
    tabContainer.setDeferred(true);
    return tabContainer;
  }
}
