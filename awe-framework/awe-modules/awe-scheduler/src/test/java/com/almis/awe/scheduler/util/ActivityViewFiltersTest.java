package com.almis.awe.scheduler.util;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Switch of the views of the activity screen: the filters of the toolbar of the view that is left are given to the
 * toolbar of the view that is shown. The «Group by flow» switch of the screen is the view: the service does not set it
 */
class ActivityViewFiltersTest {

  @Test
  void switchingToTheRunsViewGivesTheFiltersOfTheExecutionsToolbarToTheRunsToolbar() {
    Map<String, String> selected = selected(ActivityViewFilters.copy("run", "Concil", "error", "file", "24"));

    assertEquals(Map.of("CrtRunTsk", "Concil", "CrtRunQck", "error", "CrtRunOri", "file", "CrtRunWin", "24"), selected);
  }

  @Test
  void switchingBackGivesThemToTheExecutionsToolbarAndAnEmptyFilterIsCleared() {
    Map<String, String> selected = selected(ActivityViewFilters.copy("exe", null, "", null, null));

    assertEquals(Map.of("CrtExeTsk", "", "CrtExeQck", "", "CrtExeOri", "", "CrtExeWin", ""), selected);
  }

  @Test
  void anUnknownViewIsRefused() {
    assertThrows(IllegalArgumentException.class, () -> ActivityViewFilters.copy("api", "", "", "", ""));
  }

  private static Map<String, String> selected(ServiceData serviceData) {
    Map<String, String> selected = new LinkedHashMap<>();
    for (ClientAction action : serviceData.getClientActionList()) {
      assertEquals("select", action.getType());
      selected.put(action.getTarget(), String.valueOf(((Object[]) action.getParameters().get("values"))[0]));
    }
    return selected;
  }
}
