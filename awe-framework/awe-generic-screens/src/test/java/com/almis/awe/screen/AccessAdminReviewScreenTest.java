package com.almis.awe.screen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
 * Review (S7) and view as (S8) of the access window (access-admin): the header buttons with their counts, the review side
 * panel with the fix of each finding, and the side panel with the menu preview of the chosen subject.
 */
@DisplayName("Access admin screen: review and view as")
class AccessAdminReviewScreenTest {

  private static final String REVIEW = "GrdAccRev";
  // Reloads the review, its counts and the tree after a fix, and the labels with the counts (0 then 1)
  private static final List<String> RELOAD = List.of("server:refresh-menu", "filter:CrdAccRev", "filter:" + REVIEW,
    "value:access-review-reload", "value:access-review-reload", "filter:" + GRID);

  @Test
  @DisplayName("Review opens the side panel with every finding; its label has the number of warnings")
  void reviewButton() throws Exception {
    Document screen = load(AWE + SCREEN);
    Element button = byId(screen, "ButAccRev");

    assertThat(actions(button)).containsExactly("filter:CrdAccRev", "filter:" + REVIEW, "dialog:PnlAccRev");
    assertThat(labelQuery(button)).isEqualTo("access-review-label");
    assertThat(showWhen(button, "label", "access-review-reload")).isEqualTo("eq 1");
    assertThat(byId(screen, "access-review-reload").getAttribute("value")).isEqualTo("1");
    assertThat(byId(screen, "PnlAccRev").getTagName()).isEqualTo("side-panel");
    assertThat(byId(screen, "CrdAccRev").getAttribute("target-action")).isEqualTo("access-review-counts");
    assertThat(byId(screen, "CrdAccRevHnt").getAttribute("description")).isEqualTo("SCREEN_TEXT_ACCESS_REVIEW_HINT");
  }

  @Test
  @DisplayName("The findings are the last block of the review panel: nothing lies under the grid, which scrolls in its box")
  void reviewGridIsTheLastBlock() throws Exception {
    Element body = byId(load(AWE + SCREEN), "PnlAccRevBdy");
    List<Element> blocks = new ArrayList<>();
    for (Node child = body.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element block) {
        blocks.add(block);
      }
    }

    assertThat(blocks).extracting(block -> elements(block, "*").get(0).getAttribute("id"))
      .containsExactly("CrdAccRev", "CrdAccRevHnt", REVIEW);
  }

  @Test
  @DisplayName("The findings are grouped by warning and only send their rule")
  void reviewGrid() throws Exception {
    Document screen = load(AWE + SCREEN);
    Element grid = byId(screen, REVIEW);

    assertThat(grid.getAttribute("target-action")).isEqualTo("access-review");
    assertThat(grid.getAttribute("group-by")).isEqualTo("review-group");
    assertThat(grid.getAttribute("group-subtotals")).isEqualTo("false");
    assertThat(elements(grid, "column").stream().filter(column -> !"false".equals(column.getAttribute("sendable")))
      .map(column -> column.getAttribute("name"))).containsExactly("review-rule");
    assertThat(byId(screen, "review-option").getAttribute("secondary-field")).isEqualTo("review-message");
  }

  @Test
  @DisplayName("Each finding is removed or gets its parent allowed, and every rule without effect is removed at once")
  void reviewFixes() throws Exception {
    Document screen = load(AWE + SCREEN);

    assertThat(actions(byId(screen, "review-remove"))).containsExactlyElementsOf(fix("confirm:DelMsg", "access-review-remove"));
    assertThat(actions(byId(screen, "review-allow-parent")))
      .containsExactlyElementsOf(fix("confirm:AlwPrnMsg", "access-review-allow-parent"));
    Element clean = byId(screen, "ButRevCln");
    assertThat(actions(clean)).containsExactlyElementsOf(fix("confirm:ClnMsg", "access-review-clean"));
    assertThat(clean.getAttribute("style")).isEqualTo("btn-primary");
    assertThat(labelQuery(clean)).isEqualTo("access-review-clean-label");
    // Each fix sets the reload criterion to 0 and back to 1, which reloads the labels
    assertThat(elements(clean, "button-action").stream().filter(action -> "value".equals(action.getAttribute("type")))
      .map(action -> action.getAttribute("value"))).containsExactly("0", "1");
  }

  @Test
  @DisplayName("Every change made from the screen reloads the count of the review")
  void everyChangeReloadsTheReviewCount() throws Exception {
    Document screen = load(AWE + SCREEN);
    // Buttons and button columns with a maintain: the row actions, the selection bar dialog and both side panels
    List<Element> changes = elements(screen.getDocumentElement(), "*").stream()
      .filter(element -> elements(element, "button-action").stream()
        .anyMatch(action -> "maintain".equals(action.getAttribute("server-action"))))
      .filter(element -> List.of("button", "column").contains(element.getTagName()))
      .toList();

    assertThat(changes).extracting(element -> element.getAttribute("id").isEmpty() ? element.getAttribute("name")
        : element.getAttribute("id"))
      .contains("AlwBtn", "DnyBtn", "SwtBtn", "RmvBtn", "ButPnlAlw", "ButImpAlw", "ButImpDny", "ButImpRmv",
        "review-remove", "review-allow-parent", "ButRevCln");
    for (Element change : changes) {
      List<Element> actions = elements(change, "button-action");
      int maintain = actions.indexOf(actions.stream()
        .filter(action -> "maintain".equals(action.getAttribute("server-action"))).findFirst().orElseThrow());
      assertThat(actions.subList(maintain, actions.size()).stream()
        .filter(action -> "access-review-reload".equals(action.getAttribute("target")))
        .map(action -> action.getAttribute("type") + ":" + action.getAttribute("value")))
        .as(change.getAttribute("id") + change.getAttribute("name"))
        .containsExactly("value:0", "value:1");
    }
  }

  @Test
  @DisplayName("View as is enabled with a subject and opens the menu preview")
  void viewAsButton() throws Exception {
    Document screen = load(AWE + SCREEN);
    Element button = byId(screen, "ButAccPrv");

    assertThat(actions(button)).containsExactly("filter:CrdAccPrv", "filter:GrdAccPrv",
      "filter:CrdAccPrvCnt", "filter:GrdAccPrvHid", "dialog:PnlAccPrv");
    // A fixed label: the panel names the subject
    assertThat(button.getAttribute("label")).isEqualTo("SCREEN_TEXT_ACCESS_PREVIEW_BUTTON");
    assertThat(button.getAttribute("style")).isEqualTo("btn-tonal");
    assertThat(button.getAttribute("help")).isEqualTo("HELP_ACCESS_PREVIEW_BUTTON");
    assertThat(elements(button, "dependency")).noneMatch(dependency -> "label".equals(dependency.getAttribute("target-type")));
    assertThat(showWhen(button, "enable", "IdeOpe")).isEqualTo("ne ");
    assertThat(showWhen(button, "enable", "IdePro")).isEqualTo("ne ");
  }

  @Test
  @DisplayName("The preview shows the banner, the read-only menu tree, the count and the hidden options")
  void viewAsPanel() throws Exception {
    Document screen = load(AWE + SCREEN);

    assertThat(byId(screen, "PnlAccPrv").getTagName()).isEqualTo("side-panel");
    assertThat(byId(screen, "CrdAccPrv").getAttribute("target-action")).isEqualTo("access-menu-preview-banner");
    Element menu = byId(screen, "GrdAccPrv");
    assertThat(menu.getAttribute("treegrid")).isEqualTo("true");
    assertThat(menu.getAttribute("target-action")).isEqualTo("access-menu-preview");
    assertThat(menu.getAttribute("multiselect")).isEmpty();
    // Read-only: no row buttons and nothing sent
    assertThat(elements(menu, "column")).noneMatch(column -> "button".equals(column.getAttribute("component")))
      .allSatisfy(column -> assertThat(column.getAttribute("sendable")).isEqualTo("false"));
    assertThat(byId(screen, "CrdAccPrvCnt").getAttribute("target-action")).isEqualTo("access-menu-preview-counts");
    assertThat(byId(screen, "GrdAccPrvHid").getAttribute("target-action")).isEqualTo("access-menu-preview-hidden");
  }

  @Test
  @DisplayName("The preview menu starts expanded, as a menu: every level of the private menu is open")
  void viewAsMenuStartsExpanded() throws Exception {
    Element menu = byId(load(AWE + SCREEN), "GrdAccPrv");
    // Both engines open a loaded node while its level (roots are 0) plus one is below the initial level
    int depth = depth(load(AWE + "menu/private.xml").getDocumentElement());

    assertThat(menu.getAttribute("load-all")).isEqualTo("true");
    assertThat(menu.getAttribute("initial-level")).isNotEmpty();
    assertThat(Integer.parseInt(menu.getAttribute("initial-level"))).isGreaterThan(depth);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Locale-en-GB.xml", "Locale-es-ES.xml", "Locale-fr-FR.xml"})
  @DisplayName("Every text built by the review and the preview services is translated")
  void serviceTextsAreTranslated(String locale) throws Exception {
    Set<String> keys = new TreeSet<>();
    for (String key : List.of("REVIEW", "REVIEW_BUTTON", "REVIEW_CLEAN", "REVIEW_WARNINGS", "REVIEW_NO_WARNINGS",
      "REVIEW_SUMMARY", "REVIEW_GROUP_OBSOLETE", "REVIEW_GROUP_PARENT_DENIED", "REVIEW_GROUP_REDUNDANT",
      "REVIEW_OBSOLETE_OPTION", "REVIEW_OBSOLETE_USER", "REVIEW_OBSOLETE_PROFILE", "REVIEW_PARENT_DENIED",
      "REVIEW_REDUNDANT_ALLOWED", "REVIEW_REDUNDANT_DENIED", "REVIEW_ALLOW_PARENT", "REVIEW_REMOVED_USER",
      "REVIEW_REMOVED_PROFILE", "PREVIEW_TITLE", "PREVIEW_MODULE", "PREVIEW_ALL_MODULES", "PREVIEW_TEXT", "PREVIEW_COUNT",
      "PREVIEW_PARENT_HIDDEN", "PREVIEW_HIDDEN_TEXT", "PREVIEW_HIDDEN_BY_PARENT")) {
      keys.add("SCREEN_TEXT_ACCESS_" + key);
    }
    Set<String> defined = new TreeSet<>(attributes(elements(load(AWE + "locale/" + locale).getDocumentElement(), "locale"), "name"));

    assertThat(defined).as(locale).containsAll(keys);
  }

  /**
   * Actions of a fix: its confirmation, its maintain and the reload
   */
  private static List<String> fix(String confirm, String maintain) {
    List<String> actions = new ArrayList<>(List.of(confirm, "server:" + maintain));
    actions.addAll(RELOAD);
    return actions;
  }

  /**
   * Levels of options under an element of a menu
   */
  private static int depth(Element parent) {
    int depth = 0;
    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element option && "option".equals(option.getTagName())) {
        depth = Math.max(depth, 1 + depth(option));
      }
    }
    return depth;
  }

  private static Element dependency(Element element, String targetType) {
    return elements(element, "dependency").stream()
      .filter(dependency -> targetType.equals(dependency.getAttribute("target-type")))
      .findFirst().orElseThrow(() -> new AssertionError("No " + targetType + " dependency"));
  }

  /**
   * Query of the dependency that sets the label of a button
   */
  private static String labelQuery(Element button) {
    Element dependency = dependency(button, "label");
    assertThat(dependency.getAttribute("source-type")).isEqualTo("query");
    return dependency.getAttribute("target-action");
  }
}
