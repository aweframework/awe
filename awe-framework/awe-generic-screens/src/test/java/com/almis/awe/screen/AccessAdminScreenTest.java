package com.almis.awe.screen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.almis.awe.screen.AccessAdminDescriptor.AWE;
import static com.almis.awe.screen.AccessAdminDescriptor.GRID;
import static com.almis.awe.screen.AccessAdminDescriptor.SCREEN;
import static com.almis.awe.screen.AccessAdminDescriptor.actions;
import static com.almis.awe.screen.AccessAdminDescriptor.attributes;
import static com.almis.awe.screen.AccessAdminDescriptor.byId;
import static com.almis.awe.screen.AccessAdminDescriptor.elements;
import static com.almis.awe.screen.AccessAdminDescriptor.load;
import static com.almis.awe.screen.AccessAdminDescriptor.parse;
import static com.almis.awe.screen.AccessAdminDescriptor.showWhen;
import static com.almis.awe.screen.AccessAdminDescriptor.text;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The access window (access-admin, #791): the effective access of a profile or a user to the private menu as a tree
 * with its reason, its search and quick filters, the row actions and the side panel that explains an option. The
 * screen-access option of every menu opens it, and the screens it replaces are kept, marked as deprecated.
 */
@DisplayName("Access admin screen")
class AccessAdminScreenTest {

  private static final String REACT = "application/awe-react/";
  private static final String PANEL = "PnlAccWhy";
  // Menus of the applications and archetypes of the repository, from the root of this module
  private static final List<String> MENUS = List.of(
    "src/main/resources/application/awe/menu/private.xml",
    "../../awe-tests/awe-boot/src/main/resources/application/awe-boot/menu/private.xml",
    "../../awe-tests/awe-boot-react/src/main/resources/application/app/menu/private.xml",
    "../../awe-tests/awe-scheduler-boot/src/main/resources/application/awe-scheduler-boot/menu/private.xml",
    "../awe-boot-angular-archetype/src/main/resources/archetype-resources/src/main/resources/application/__artifactId__/menu/private.xml",
    "../awe-boot-react-archetype/src/main/resources/archetype-resources/src/main/resources/application/__artifactId__/menu/private.xml",
    "../awe-boot-angular-archetype/src/test/resources/projects/it1/reference/src/main/resources/application/test/menu/private.xml",
    "../awe-boot-react-archetype/src/test/resources/projects/it1/reference/src/main/resources/application/test/menu/private.xml");

  @Test
  @DisplayName("The screen is valid against the screen schema")
  void screenIsValid() throws Exception {
    File schema = Paths.get(getClass().getResource("/schemas/awe/screen.xsd").toURI()).toFile();
    File screen = Paths.get(getClass().getResource("/" + AWE + SCREEN).toURI()).toFile();

    assertThatCode(() -> SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schema).newValidator()
      .validate(new StreamSource(screen))).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("The tree of options is fed by access-tree, with the search and the quick filter of access-tree-counts")
  void treeOfOptions() throws Exception {
    Document screen = load(AWE + SCREEN);
    Element grid = byId(screen, GRID);

    assertThat(grid.getAttribute("treegrid")).isEqualTo("true");
    assertThat(grid.getAttribute("tree-id")).isEqualTo("option-id");
    assertThat(grid.getAttribute("tree-parent")).isEqualTo("parent-option");
    assertThat(grid.getAttribute("tree-leaf")).isEqualTo("option-is-leaf");
    assertThat(grid.getAttribute("expand-column")).isEqualTo("option-name");
    assertThat(grid.getAttribute("load-all")).isEqualTo("true");
    assertThat(grid.getAttribute("target-action")).isEqualTo("access-tree");
    assertThat(byId(screen, "access-search").getTagName()).isEqualTo("search");
    Element filter = byId(screen, "access-filter");
    assertThat(filter.getTagName()).isEqualTo("quick-filter");
    assertThat(filter.getAttribute("target-action")).isEqualTo("access-tree-counts");
    // Visible columns: option, access (and the two compare columns, shown while comparing), reason and own rule, then the
    // row buttons
    List<Element> columns = elements(grid, "column");
    assertThat(columns.stream().filter(column -> !"true".equals(column.getAttribute("hidden"))).map(column -> column.getAttribute("name")))
      .containsExactly("option-name", "access-text", "compare-access-text", "compare-reason", "reason", "own-rule-text",
        "AlwBtn", "DnyBtn", "WhyBtn");
    assertThat(byId(screen, "option-name").getAttribute("secondary-field")).isEqualTo("option-key");
    assertThat(byId(screen, "access-text").getAttribute("status-field")).isEqualTo("access-tone");
    assertThat(byId(screen, "reason").getAttribute("secondary-field")).isEqualTo("warning");
    // The grid only sends the options of the selected rows, the parameter of the maintains
    assertThat(columns.stream().filter(column -> !"false".equals(column.getAttribute("sendable"))).map(column -> column.getAttribute("name")))
      .containsExactly("access-options");
    assertThat(grid.getAttribute("empty-title")).isEqualTo("SCREEN_TEXT_ACCESS_EMPTY_TITLE");
  }

  @Test
  @DisplayName("Allow and Deny change the rule of the row, refresh the menu and reload the tree; Why opens the side panel")
  void rowActions() throws Exception {
    Document screen = load(AWE + SCREEN);

    assertThat(actions(byId(screen, "AlwBtn"))).containsExactly("server:access-allow", "server:refresh-menu", "value:access-review-reload", "value:access-review-reload",
      "filter:" + GRID);
    assertThat(actions(byId(screen, "DnyBtn"))).containsExactly("server:access-deny", "server:refresh-menu", "value:access-review-reload", "value:access-review-reload",
      "filter:" + GRID);
    assertThat(actions(byId(screen, "WhyBtn"))).containsExactly("filter:CrdAccRes", "filter:GrdAccWhy", "filter:GrdAccRul",
      "dialog:" + PANEL);
  }

  @Test
  @DisplayName("The subject bar: Profile | User, the user or the profile and the module; a change reloads the tree")
  void subjectBar() throws Exception {
    Document screen = load(AWE + SCREEN);

    // Profile | User: one button group with its options, starting on User (a radio is selected only when checked)
    Element subjectType = byId(screen, "subject-type");
    assertThat(subjectType.getAttribute("component")).isEqualTo("button-radio");
    assertThat(subjectType.getAttribute("initial-load")).isEqualTo("enum");
    assertThat(subjectType.getAttribute("target-action")).isEqualTo("access-subject-types");
    assertThat(subjectType.getAttribute("value")).isEqualTo("user");
    assertThat(subjectType.getAttribute("checked")).isEqualTo("true");
    Element group = elements(load(AWE + "global/Enumerated.xml").getDocumentElement(), "group").stream()
      .filter(element -> "access-subject-types".equals(element.getAttribute("id"))).findFirst().orElseThrow();
    assertThat(attributes(elements(group, "option"), "value")).containsExactly("profile", "user");
    Element user = byId(screen, "IdeOpe");
    assertThat(user.getAttribute("component")).isEqualTo("suggest");
    assertThat(user.getAttribute("target-action")).isEqualTo("UsrSug");
    Element profile = byId(screen, "IdePro");
    assertThat(profile.getAttribute("component")).isEqualTo("suggest");
    assertThat(profile.getAttribute("target-action")).isEqualTo("ProSug");
    // Not "module": that request parameter changes the module of the session menu
    Element module = byId(screen, "access-module");
    assertThat(module.getAttribute("component")).isEqualTo("select");
    assertThat(module.getAttribute("target-action")).isEqualTo("ModSel");
    assertThat(module.getAttribute("optional")).isEqualTo("true");
    assertThat(attributes(elements(screen.getDocumentElement(), "criteria"), "id")).doesNotContain("module");
    // The user is shown for a user subject, the profile for a profile subject
    assertThat(subjectShownWhen(user)).isEqualTo("user");
    assertThat(subjectShownWhen(profile)).isEqualTo("profile");
    // Every criterion of the subject reloads the tree (and with it the counts)
    List<String> reloads = elements(byId(screen, GRID), "dependency").stream()
      .filter(dependency -> elements(dependency, "dependency-action").stream()
        .anyMatch(action -> "filter".equals(action.getAttribute("type")) && GRID.equals(action.getAttribute("target"))))
      .flatMap(dependency -> attributes(elements(dependency, "dependency-element"), "id").stream())
      .toList();
    assertThat(reloads).contains("subject-type", "IdeOpe", "IdePro", "access-module");
  }

  @Test
  @DisplayName("The subject criteria share one row above the views, without a window header, and the tree is in the window of the menu options")
  void subjectCriteriaAndTreeInOneCard() throws Exception {
    Document screen = load(AWE + SCREEN);
    List<String> criteria = List.of("subject-type", "IdeOpe", "IdePro", "access-module", "compare-with");
    List<Element> windows = elements(screen.getDocumentElement(), "window");
    Element subject = byId(screen, "AccSbj");

    Element card = (Element) subject.getParentNode();

    assertThat(subject.getTagName()).isEqualTo("tag");
    assertThat(card.getTagName()).isEqualTo("window");
    assertThat(card.getAttribute("label")).isEqualTo("SCREEN_TEXT_ACCESS_SUBJECT");
    assertThat(((Element) card.getParentNode()).getAttribute("source")).isEqualTo("center");
    assertThat(elements(subject, "*").stream()
      .filter(element -> !"dependency-element".equals(element.getTagName()))
      .map(element -> element.getAttribute("id"))
      .filter(criteria::contains))
      .containsExactly("subject-type", "IdeOpe", "IdePro", "access-module", "compare-with");
    assertThat(nextElement(card).getTagName()).isEqualTo("tab");
    assertThat(windows).filteredOn(window -> "SCREEN_TEXT_ACCESS_MENU".equals(window.getAttribute("label")))
      .singleElement()
      .satisfies(window -> assertThat(elements(window, "grid")).extracting(grid -> grid.getAttribute("id"))
        .containsExactly(GRID));
    // One row: the criteria (and the help of the comparison) are siblings in the same container
    assertThat(criteria.stream().map(id -> byId(screen, id).getParentNode()).distinct()).hasSize(1);
    assertThat(byId(screen, "AccCmpHlp").getParentNode()).isSameAs(byId(screen, "subject-type").getParentNode());
  }

  private static String subjectShownWhen(Element criterion) {
    return showWhen(criterion, "show", "subject-type");
  }

  @Test
  @DisplayName("Selecting options shows Allow, Deny and Remove rule in the selection bar; each one opens the preview of its change")
  void bulkChangeSelectionBar() throws Exception {
    Document screen = load(AWE + SCREEN);
    Element grid = byId(screen, GRID);

    assertThat(grid.getAttribute("multiselect")).isEqualTo("true");
    assertThat(byId(screen, "access-change").getAttribute("component")).isEqualTo("hidden");
    List<Element> buttons = elements(grid, "button");
    assertThat(buttons).extracting(button -> button.getAttribute("id"), button -> button.getAttribute("selection"),
        button -> button.getAttribute("label"))
      .containsExactly(
        org.assertj.core.groups.Tuple.tuple("ButAccAlw", "true", "SCREEN_TEXT_ACCESS_ALLOW"),
        org.assertj.core.groups.Tuple.tuple("ButAccDny", "true", "SCREEN_TEXT_ACCESS_DENY"),
        org.assertj.core.groups.Tuple.tuple("ButAccRmv", "true", "SCREEN_TEXT_ACCESS_REMOVE_RULE"));
    List<String> changes = List.of("allow", "deny", "remove");
    // Enabled with several rows (the preview tells which change), or with one row the change applies to: an allowed
    // option has nothing to allow, a denied one nothing to deny, and an option without its own rule nothing to remove
    List<String> enabledWhen = List.of("access ne allowed", "access ne denied", "own-rule in A,R");
    for (int index = 0; index < buttons.size(); index++) {
      Element button = buttons.get(index);
      // The change of the preview, then its card and grids, loaded before the dialog opens
      assertThat(actions(button)).as(button.getAttribute("id")).containsExactly("check-some-selected:" + GRID,
        "value:access-change", "filter:CrdAccImp", "filter:GrdAccImpOpt", "filter:GrdAccImpUsr", "dialog:DlgAccImp");
      assertThat(elements(button, "button-action").get(1).getAttribute("value")).isEqualTo(changes.get(index));
      String enabled = enabledWhen.get(index);
      assertThat(elements(button, "dependency")).as(button.getAttribute("id"))
        .filteredOn(dependency -> "enable".equals(dependency.getAttribute("target-type")))
        .singleElement()
        .satisfies(dependency -> {
          assertThat(dependency.getAttribute("type")).isEqualTo("or");
          assertThat(elements(dependency, "dependency-element"))
            .extracting(element -> (element.getAttribute("column").isEmpty() ? element.getAttribute("attribute")
              : element.getAttribute("column")) + " " + element.getAttribute("condition") + " " + element.getAttribute("value"))
            .containsExactly("selectedRows gt 1", enabled);
        });
    }
  }

  @Test
  @DisplayName("The preview dialog shows the summary, the options and the users of the change, and confirms it")
  void bulkChangePreviewDialog() throws Exception {
    Document screen = load(AWE + SCREEN);
    Element dialog = byId(screen, "DlgAccImp");

    assertThat(dialog.getTagName()).isEqualTo("dialog");
    assertThat(dialog.getAttribute("modal")).isEqualTo("true");
    assertThat(byId(screen, "CrdAccImp").getAttribute("target-action")).isEqualTo("access-impact");
    assertThat(byId(screen, "GrdAccImpOpt").getAttribute("target-action")).isEqualTo("access-impact-options");
    assertThat(byId(screen, "GrdAccImpUsr").getAttribute("target-action")).isEqualTo("access-impact-users");
    // The grids of the dialog send nothing: the maintains read the selected options of the tree
    assertThat(elements(dialog, "column")).isNotEmpty()
      .allSatisfy(column -> assertThat(column.getAttribute("sendable")).as(column.getAttribute("name")).isEqualTo("false"));
    // One confirm button per change, shown for its change; deny is the danger one
    List<String> changes = List.of("allow", "deny", "remove");
    List<String> buttons = List.of("ButImpAlw", "ButImpDny", "ButImpRmv");
    for (int index = 0; index < buttons.size(); index++) {
      Element button = byId(screen, buttons.get(index));
      assertThat(actions(button)).containsExactly("server:access-" + changes.get(index), "server:refresh-menu",
        "value:access-review-reload", "value:access-review-reload", "close:DlgAccImp", "unselect-all-rows:" + GRID, "filter:" + GRID);
      assertThat(showWhen(button, "show", "access-change")).isEqualTo("eq " + changes.get(index));
    }
    assertThat(byId(screen, "ButImpDny").getAttribute("style")).isEqualTo("btn-danger");
    assertThat(actions(byId(screen, "ButImpCnc"))).containsExactly("close:DlgAccImp");
  }

  @Test
  @DisplayName("Compare shows one suggest of profiles and users to compare with and two columns of the tree; Stop comparing clears it")
  void compare() throws Exception {
    Document screen = load(AWE + SCREEN);

    assertThat(byId(screen, "access-compare").getAttribute("component")).isEqualTo("hidden");
    assertThat(actions(byId(screen, "ButCmp"))).containsExactly("value:access-compare");
    assertThat(showWhen(byId(screen, "ButCmp"), "show", "access-compare")).isEqualTo("ne true");
    // A hidden criterion is not reset in AngularJS: Stop comparing resets the suggest and empties access-compare with a value
    assertThat(actions(byId(screen, "ButCmpStp"))).containsExactly("reset:compare-with", "value:access-compare");
    assertThat(elements(byId(screen, "ButCmpStp"), "button-action").get(1).getAttribute("value")).isEmpty();
    assertThat(showWhen(byId(screen, "ButCmpStp"), "show", "access-compare")).isEqualTo("eq true");
    // One suggest searches the profiles and the users at once (design/access-admin/05-buscador-mixto.html), sized as the
    // subject suggest, and shown only while comparing; no second Profile | User group
    Element compareWith = byId(screen, "compare-with");
    assertThat(compareWith.getAttribute("component")).isEqualTo("suggest");
    assertThat(compareWith.getAttribute("label")).isEqualTo("SCREEN_TEXT_ACCESS_COMPARE_WITH");
    assertThat(compareWith.getAttribute("icon")).isEqualTo("lucide:git-compare");
    assertThat(compareWith.getAttribute("placeholder")).isEqualTo("SCREEN_TEXT_ACCESS_COMPARE_SEARCH");
    assertThat(compareWith.getAttribute("server-action")).isEqualTo("data");
    assertThat(compareWith.getAttribute("target-action")).isEqualTo("AccessCompareSug");
    assertThat(compareWith.getAttribute("style")).isEqualTo(byId(screen, "IdeOpe").getAttribute("style"));
    assertThat(showWhen(compareWith, "show", "access-compare")).isEqualTo("eq true");
    assertThat(attributes(elements(screen.getDocumentElement(), "criteria"), "id"))
      .doesNotContain("compare-type", "compare-profile", "compare-user");
    // The suggest is a service query: it sends the typed text, or the selected value to resolve its label
    Document queries = load(AWE + "global/Queries.xml");
    assertThat(elements(queries.getDocumentElement(), "query"))
      .filteredOn(element -> "AccessCompareSug".equals(element.getAttribute("id")))
      .singleElement()
      .satisfies(element -> {
        assertThat(element.getAttribute("service")).isEqualTo("access-compare-suggest");
        assertThat(attributes(elements(element, "variable"), "name")).containsExactly("suggest");
      });
    // The two columns of the comparison are shown only with a profile or a user to compare with
    assertThat(byId(screen, "compare-access-text").getAttribute("status-field")).isEqualTo("compare-access-tone");
    assertThat(byId(screen, "compare-reason").getAttribute("icon-field")).isEqualTo("compare-icon");
    // While comparing, the reason and the own rule give their room to the comparison (the option column takes the rest
    // of the width, and would have none)
    for (String column : List.of("compare-access-text", "compare-reason", "reason", "own-rule-text")) {
      String targetType = column.startsWith("compare-") ? "show-column" : "hide-column";
      assertThat(elements(byId(screen, column), "dependency")).as(column)
        .filteredOn(dependency -> targetType.equals(dependency.getAttribute("target-type")))
        .singleElement()
        .satisfies(dependency -> assertThat(elements(dependency, "dependency-element"))
          .extracting(element -> element.getAttribute("id") + " " + element.getAttribute("condition") + " "
            + element.getAttribute("value"))
          .containsExactly("compare-with ne "));
    }
    // The profile or the user to compare with reloads the tree
    List<String> reloads = elements(byId(screen, GRID), "dependency").stream()
      .filter(dependency -> elements(dependency, "dependency-action").stream()
        .anyMatch(action -> "filter".equals(action.getAttribute("type")) && GRID.equals(action.getAttribute("target"))))
      .flatMap(dependency -> attributes(elements(dependency, "dependency-element"), "id").stream())
      .toList();
    assertThat(reloads).contains("compare-with").doesNotContain("compare-type", "compare-profile", "compare-user");
    // The tree and its counts send the profile or the user to compare with, and nothing else of the comparison
    for (String query : List.of("access-tree", "access-tree-counts")) {
      assertThat(elements(queries.getDocumentElement(), "query")).as(query)
        .filteredOn(element -> query.equals(element.getAttribute("id")))
        .singleElement()
        .satisfies(element -> assertThat(attributes(elements(element, "variable"), "name"))
          .contains("compare-with")
          .doesNotContain("compare-type", "compare-profile", "compare-user"));
    }
    // No banner: what a comparison does is a static help right after the suggest, shown with it. A tag has no
    // dependencies, so the suggest shows and hides it
    assertThat(attributes(elements(screen.getDocumentElement(), "card"), "target-action")).doesNotContain("access-compare");
    assertThat(attributes(elements(queries.getDocumentElement(), "query"), "id")).doesNotContain("access-compare");
    Element help = byId(screen, "AccCmpHlp");
    assertThat(help.getAttribute("label")).isEqualTo("SCREEN_TEXT_ACCESS_COMPARE_HELP");
    assertThat(nextElement(compareWith)).isSameAs(help);
    assertThat(elements(compareWith, "dependency").stream()
      .filter(dependency -> "true".equals(dependency.getAttribute("initial")))
      .flatMap(dependency -> elements(dependency, "dependency-action").stream()
        .filter(action -> "#AccCmpHlp".equals(action.getAttribute("target")))
        .map(action -> {
          Element source = elements(dependency, "dependency-element").getFirst();
          return source.getAttribute("id") + " " + source.getAttribute("condition") + " " + source.getAttribute("value")
            + ": " + action.getAttribute("type") + " " + action.getAttribute("target-action");
        })))
      .containsExactlyInAnyOrder("access-compare eq true: remove-class hidden", "access-compare ne true: add-class hidden");
  }

  private static Element nextElement(Element element) {
    org.w3c.dom.Node next = element.getNextSibling();
    while (next != null && !(next instanceof Element)) {
      next = next.getNextSibling();
    }
    return (Element) next;
  }

  @Test
  @DisplayName("The why side panel shows the result, the cascade and every rule of the selected option, which it can switch and remove")
  void whySidePanel() throws Exception {
    Document screen = load(AWE + SCREEN);
    Element panel = byId(screen, PANEL);

    assertThat(panel.getTagName()).isEqualTo("side-panel");
    assertThat(panel.getAttribute("header-grid")).isEqualTo(GRID);
    assertThat(panel.getAttribute("header-title-field")).isEqualTo("option-name");
    assertThat(attributes(elements(panel, "header-tag"), "field")).containsExactly("access-text");
    assertThat(byId(screen, "GrdAccWhy").getAttribute("target-action")).isEqualTo("access-why");
    Element rules = byId(screen, "GrdAccRul");
    assertThat(rules.getAttribute("target-action")).isEqualTo("access-option-rules");
    assertThat(elements(rules, "column").stream().filter(column -> !"false".equals(column.getAttribute("sendable")))
      .map(column -> column.getAttribute("name"))).containsExactly("access-rule");
    // The option of the panel is the one of the selected row of the tree
    Element option = byId(screen, "access-option");
    assertThat(option.getAttribute("component")).isEqualTo("hidden");
    assertThat(attributes(elements(option, "dependency-element"), "column")).contains("option-id");
    assertThat(actions(byId(screen, "RmvBtn"))).containsSubsequence("confirm:DelMsg", "server:access-remove-rule",
      "server:refresh-menu", "filter:GrdAccRul", "filter:GrdAccWhy", "filter:" + GRID);
    assertThat(actions(byId(screen, "ButPnlAlw"))).containsSubsequence("server:access-allow", "server:refresh-menu",
      "filter:CrdAccRes", "filter:GrdAccWhy", "filter:GrdAccRul", "filter:" + GRID);
    assertThat(byId(screen, "ButPnlAlw").getAttribute("style")).isEqualTo("btn-primary");
  }

  @Test
  @DisplayName("The why side panel has the result card above the cascade and the hint below; its grids are not paged")
  void whySidePanelResultHintAndRules() throws Exception {
    Document screen = load(AWE + SCREEN);
    List<String> order = elements(byId(screen, "PnlAccWhyBdy"), "*").stream()
      .map(element -> element.getAttribute("id")).filter(id -> List.of("CrdAccRes", "GrdAccWhy", "CrdAccHnt", "GrdAccRul").contains(id))
      .toList();

    assertThat(order).containsExactly("CrdAccRes", "GrdAccWhy", "CrdAccHnt", "GrdAccRul");
    Element result = byId(screen, "CrdAccRes");
    assertThat(result.getTagName()).isEqualTo("card");
    assertThat(result.getAttribute("initial-load")).isEqualTo("query");
    assertThat(result.getAttribute("target-action")).isEqualTo("access-why-result");
    Element hint = byId(screen, "CrdAccHnt");
    assertThat(hint.getTagName()).isEqualTo("card");
    assertThat(hint.getAttribute("description")).isEqualTo("SCREEN_TEXT_ACCESS_HINT");
    // Six steps: the panel scrolls, the cascade shows every step without paginator
    assertThat(byId(screen, "GrdAccWhy").getAttribute("pagination-disabled")).isEqualTo("true");
    // A rule switches between Allow and Deny for its own subject, and everything that shows it is reloaded
    assertThat(actions(byId(screen, "SwtBtn"))).containsExactly("server:access-switch-rule", "server:refresh-menu",
      "value:access-review-reload", "value:access-review-reload", "filter:GrdAccRul", "filter:GrdAccWhy", "filter:CrdAccRes", "filter:" + GRID);
    assertThat(actions(byId(screen, "RmvBtn"))).contains("filter:CrdAccRes");
  }

  @Test
  @DisplayName("The grids of the panels whose rows have no limit load every row: none is hidden by the page of the request")
  void unboundedPanelGridsLoadEveryRow() throws Exception {
    Document screen = load(AWE + SCREEN);

    assertThat(List.of("GrdAccRul", "GrdAccRev", "GrdAccPrvHid", "GrdAccImpOpt"))
      .allSatisfy(id -> assertThat(byId(screen, id).getAttribute("load-all")).as(id).isEqualTo("true"));
    // A flat grid pages them in the browser: AngularJS hides the rows past its max without paginator
    assertThat(List.of("GrdAccRul", "GrdAccPrvHid", "GrdAccImpOpt"))
      .allSatisfy(id -> assertThat(byId(screen, id).getAttribute("pagination-disabled")).as(id).isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"Locale-en-GB.xml", "Locale-es-ES.xml", "Locale-fr-FR.xml"})
  @DisplayName("Every text of the screen and of its services is translated")
  void textsAreTranslated(String locale) throws Exception {
    Set<String> keys = new LinkedHashSet<>();
    Matcher matcher = Pattern.compile("\"([A-Z][A-Z0-9_]+)\"").matcher(text(AWE + SCREEN));
    while (matcher.find()) {
      keys.add(matcher.group(1));
    }
    keys.add("SCREEN_TEXT_ACCESS_DECIDES");
    // Texts of the comparison and of the preview of a change, built by the service
    for (String key : List.of("COMPARE_SAME", "FILTER_DIFFERENCES", "SUBJECT_PROFILE", "SUBJECT_USER", "IMPACT_TITLE_ALLOW",
      "IMPACT_TITLE_DENY", "IMPACT_TITLE_REMOVE", "IMPACT_OPTIONS_ALLOW", "IMPACT_OPTIONS_DENY", "IMPACT_OPTIONS_REMOVE",
      "IMPACT_USERS_ALLOW", "IMPACT_USERS_DENY", "IMPACT_USERS_REMOVE", "IMPACT_KEEP", "IMPACT_KEEPS",
      "IMPACT_KEEPS_DENIED", "IMPACT_LOSES", "IMPACT_GAINS", "IMPACT_MORE_OPTIONS", "IMPACT_MORE_USERS")) {
      keys.add("SCREEN_TEXT_ACCESS_" + key);
    }
    Set<String> defined = new LinkedHashSet<>(attributes(elements(load(AWE + "locale/" + locale).getDocumentElement(), "locale"), "name"));

    assertThat(keys).hasSizeGreaterThan(20);
    assertThat(defined).as(locale).containsAll(keys);
    // The texts of the compare banner, of the subtitle and of the View as label, all gone (the subject window has a
    // title again, SCREEN_TEXT_ACCESS_SUBJECT, so its header is not empty)
    assertThat(defined).as(locale).doesNotContain("SCREEN_TEXT_ACCESS_COMPARE_USER_TITLE",
      "SCREEN_TEXT_ACCESS_COMPARE_USER_TEXT", "SCREEN_TEXT_ACCESS_COMPARE_PROFILE_TITLE", "SCREEN_TEXT_ACCESS_COMPARE_PROFILE_TEXT",
      "SCREEN_TEXT_ACCESS_ADMIN_SUBTITLE", "SCREEN_TEXT_ACCESS_VIEW_AS", "SCREEN_TEXT_ACCESS_VIEW_AS_NONE");
  }

  @ParameterizedTest
  @ValueSource(strings = {AWE + "screen/security/ScrAccRes.xml", AWE + "screen/security/screen-access.xml",
    AWE + "screen/security/menu-access.xml", REACT + "screen/security/menu-access.xml"})
  @DisplayName("The replaced screens are kept, marked as deprecated")
  void replacedScreensAreDeprecated(String path) throws Exception {
    String source = text(path);
    String header = source.substring(0, source.indexOf("-->"));

    assertThat(header).contains("DEPRECATED since AWE 5.0.0: replaced by the access-admin screen (screen/security/access-admin.xml)");
    assertThat(load(path).getDocumentElement().getTagName()).isEqualTo("screen");
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "1", "2", "3", "4", "5", "6", "7"})
  @DisplayName("The screen-access option of every menu opens access-admin, and no menu lists the replaced screens")
  void menusOpenTheAccessWindow(int index) throws Exception {
    Path menu = Paths.get(MENUS.get(index));
    Document document;
    try (InputStream stream = Files.newInputStream(menu)) {
      document = parse(stream);
    }
    List<Element> options = elements(document.getDocumentElement(), "option");

    assertThat(options.stream().filter(option -> "screen-access".equals(option.getAttribute("name"))))
      .as(menu.toString())
      .singleElement()
      .satisfies(option -> {
        assertThat(option.getAttribute("screen")).isEqualTo("access-admin");
        assertThat(option.getAttribute("label")).isEqualTo("MENU_TOOLS_SCREENS_ACCESS");
      });
    assertThat(attributes(options, "name")).as(menu.toString()).doesNotContain("screen-access-upgraded", "menu-access");
    assertThat(attributes(options, "screen")).as(menu.toString()).doesNotContain("ScrAccRes", "screen-access", "menu-access");
  }
}
