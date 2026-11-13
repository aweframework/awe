package com.almis.awe.screen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

import static com.almis.awe.screen.AccessAdminDescriptor.AWE;
import static com.almis.awe.screen.AccessAdminDescriptor.GRID;
import static com.almis.awe.screen.AccessAdminDescriptor.SCREEN;
import static com.almis.awe.screen.AccessAdminDescriptor.actions;
import static com.almis.awe.screen.AccessAdminDescriptor.attributes;
import static com.almis.awe.screen.AccessAdminDescriptor.byId;
import static com.almis.awe.screen.AccessAdminDescriptor.elements;
import static com.almis.awe.screen.AccessAdminDescriptor.load;
import static com.almis.awe.screen.AccessAdminDescriptor.showWhen;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Permissions view of the access window (access-admin, design/user-permissions): a tab next to the menu options
 * with the effective permissions of the same subject grouped by resource, their state, origins and warnings, its
 * quick filters, its summary, the comparison and a side panel that explains a permission profile by profile.
 */
@DisplayName("Access admin screen: permissions view")
class AccessAdminPermissionsScreenTest {

  private static final String PERMISSIONS = "GrdAccPer";
  private static final String PANEL = "PnlAccPerWhy";

  @Test
  @DisplayName("Two views of the same subject: the menu options and the permissions, the second loaded when it is opened")
  void twoViewsOfTheSameSubject() throws Exception {
    Document screen = load(AWE + SCREEN);
    Element tab = byId(screen, "access-view");

    assertThat(tab.getTagName()).isEqualTo("tab");
    assertThat(tab.getAttribute("target-action")).isEqualTo("access-views");
    Element group = elements(load(AWE + "global/Enumerated.xml").getDocumentElement(), "group").stream()
      .filter(element -> "access-views".equals(element.getAttribute("id"))).findFirst().orElseThrow();
    assertThat(attributes(elements(group, "option"), "value")).containsExactly("menu", "permissions");
    assertThat(attributes(elements(tab, "tabcontainer"), "id")).containsExactly("menu", "permissions");
    assertThat(byId(screen, GRID).getParentNode().getParentNode()).isSameAs(byId(screen, "menu"));
    assertThat(elements(byId(screen, "permissions"), "grid")).extracting(grid -> grid.getAttribute("id"))
      .containsExactly(PERMISSIONS);
    // A user that cannot read the permissions does not load them with the screen
    assertThat(byId(screen, "permissions").getAttribute("deferred")).isEqualTo("true");
    // The subject is shared: above the views, not inside one of them
    assertThat(elements(tab, "criteria")).extracting(criterion -> criterion.getAttribute("id"))
      .doesNotContain("subject-type", "IdeOpe", "IdePro", "compare-with");
    // The module, the review and the preview are of the menu options
    for (String id : List.of("access-module", "ButAccRev", "ButAccPrv")) {
      assertThat(showWhen(byId(screen, id), "show", "access-view")).as(id).isEqualTo("ne permissions");
    }
  }

  @Test
  @DisplayName("The permissions grid: grouped by resource, state, origins as chips and warning, with its search and quick filters")
  void permissionsGrid() throws Exception {
    Document screen = load(AWE + SCREEN);
    Element grid = byId(screen, PERMISSIONS);

    assertThat(grid.getAttribute("target-action")).isEqualTo("access-permissions");
    assertThat(grid.getAttribute("load-all")).isEqualTo("true");
    assertThat(grid.getAttribute("group-by")).isEqualTo("permission-resource");
    assertThat(byId(screen, "access-permission-search").getTagName()).isEqualTo("search");
    Element filter = byId(screen, "access-permission-filter");
    assertThat(filter.getTagName()).isEqualTo("quick-filter");
    assertThat(filter.getAttribute("target-action")).isEqualTo("access-permission-counts");
    List<Element> columns = elements(grid, "column");
    assertThat(columns.stream().filter(column -> !"true".equals(column.getAttribute("hidden")))
      .map(column -> column.getAttribute("name")))
      .containsExactly("permission-name", "permission-state-text", "compare-permission-text", "compare-permission-reason",
        "permission-origins", "permission-warning", "PerWhyBtn");
    assertThat(byId(screen, "permission-name").getAttribute("secondary-field")).isEqualTo("permission-key");
    assertThat(byId(screen, "permission-state-text").getAttribute("status-field")).isEqualTo("permission-state-tone");
    Element origins = byId(screen, "permission-origins");
    assertThat(origins.getAttribute("chips")).isEqualTo("true");
    assertThat(origins.getAttribute("chip-separator")).isEqualTo("|");
    assertThat(byId(screen, "permission-warning").getAttribute("icon-field")).isEqualTo("permission-warning-icon");
    // Nothing is changed from this view: the grid sends nothing
    assertThat(columns).allSatisfy(column -> assertThat(column.getAttribute("sendable")).as(column.getAttribute("name"))
      .isEqualTo("false"));
    // While comparing, the state of the other subject and why it changes take the room of the origins and the warning
    for (String column : List.of("compare-permission-text", "compare-permission-reason", "permission-origins",
      "permission-warning")) {
      String targetType = column.startsWith("compare-") ? "show-column" : "hide-column";
      assertThat(showWhen(byId(screen, column), targetType, "compare-with")).as(column).isEqualTo("ne ");
    }
  }

  @Test
  @DisplayName("Only in the permissions view, a change of the subject or of the comparison reloads the grid and the summary")
  void reloadOnlyInThePermissionsView() throws Exception {
    Document screen = load(AWE + SCREEN);

    List<Element> reloads = elements(byId(screen, PERMISSIONS), "dependency").stream()
      .filter(dependency -> elements(dependency, "dependency-action").stream()
        .anyMatch(action -> PERMISSIONS.equals(action.getAttribute("target"))))
      .toList();
    assertThat(reloads).singleElement().satisfies(dependency -> {
      assertThat(dependency.getAttribute("type")).isIn("", "and");
      assertThat(dependency.getAttribute("initial")).isEqualTo("true");
      assertThat(elements(dependency, "dependency-element"))
        .extracting(element -> element.getAttribute("id") + " " + element.getAttribute("condition") + " "
          + element.getAttribute("value"))
        .startsWith("access-view eq permissions")
        .extracting(text -> text.split(" ")[0])
        .contains("subject-type", "IdeOpe", "IdePro", "compare-with");
      assertThat(attributes(elements(dependency, "dependency-action"), "target"))
        .containsExactly(PERMISSIONS, "CrdAccPer");
    });
    Element summary = byId(screen, "CrdAccPer");
    assertThat(summary.getTagName()).isEqualTo("card");
    assertThat(summary.getAttribute("target-action")).isEqualTo("access-permission-summary");
    // The next sign-in notice is the help of the window of the grid, not a panel of its own
    assertThat(((Element) byId(screen, PERMISSIONS).getParentNode()).getAttribute("help"))
      .isEqualTo("SCREEN_TEXT_ACCESS_PERMISSION_NEXT_LOGIN");
  }

  @Test
  @DisplayName("The summary is hidden until a user or a profile is selected")
  void summaryHiddenWithoutSubject() throws Exception {
    Element summary = byId(load(AWE + SCREEN), "CrdAccPer");

    List<Element> shows = elements(summary, "dependency").stream()
      .filter(dependency -> "show".equals(dependency.getAttribute("target-type")))
      .toList();
    assertThat(shows).singleElement().satisfies(dependency -> {
      assertThat(dependency.getAttribute("initial")).isEqualTo("true");
      assertThat(dependency.getAttribute("type")).isEqualTo("or");
      // The criterion of the other subject type is reset, so the selected one decides
      assertThat(elements(dependency, "dependency-element"))
        .extracting(element -> element.getAttribute("id") + " " + element.getAttribute("condition"))
        .containsExactly("IdeOpe is not empty", "IdePro is not empty");
    });
  }

  @Test
  @DisplayName("Why opens a side panel with the result and what each profile gives to the permission of the row")
  void whySidePanel() throws Exception {
    Document screen = load(AWE + SCREEN);
    Element panel = byId(screen, PANEL);

    assertThat(actions(byId(screen, "PerWhyBtn")))
      .containsExactly("filter:CrdAccPerRes", "filter:GrdAccPerWhy", "dialog:" + PANEL);
    assertThat(panel.getTagName()).isEqualTo("side-panel");
    assertThat(panel.getAttribute("deferred")).isEqualTo("true");
    assertThat(panel.getAttribute("header-grid")).isEqualTo(PERMISSIONS);
    assertThat(panel.getAttribute("header-title-field")).isEqualTo("permission-title");
    assertThat(attributes(elements(panel, "header-tag"), "field")).containsExactly("permission-state-text");
    assertThat(byId(screen, "CrdAccPerRes").getAttribute("target-action")).isEqualTo("access-permission-why-result");
    assertThat(byId(screen, "GrdAccPerWhy").getAttribute("target-action")).isEqualTo("access-permission-why");
    // The permission of the panel is the one of the selected row
    Element permission = byId(screen, "access-permission");
    assertThat(permission.getAttribute("component")).isEqualTo("hidden");
    assertThat(attributes(elements(permission, "dependency-element"), "column")).contains("permission-id");
  }
}
