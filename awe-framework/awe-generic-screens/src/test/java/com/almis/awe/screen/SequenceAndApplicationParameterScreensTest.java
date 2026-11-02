package com.almis.awe.screen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The sequences and the application parameters of the application are edited in one administration screen each
 * (sequence-admin and application-parameter-admin) since AWE 5.0.0, with the pattern of the rest of the administration
 * windows: a read-only grid with a search, quick filters and row actions, and a side panel that creates and edits the
 * record. The name of both records is read-only when editing, because it is the primary key of a sequence and the key
 * the application reads for a parameter, so no other test would catch a panel that lets the user rename it.
 */
@DisplayName("Sequence and application parameter screens")
class SequenceAndApplicationParameterScreensTest {

  private static final String AWE = "application/awe/";
  private static final String SEQUENCE_SCREEN = "screen/configuration/sequence-admin.xml";
  private static final String PARAMETER_SCREEN = "screen/configuration/application-parameter-admin.xml";

  private Document load(String path) throws Exception {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(AWE + path)) {
      assertThat(stream).as(path).isNotNull();
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      return factory.newDocumentBuilder().parse(stream);
    }
  }

  private List<String> attributes(Document document, String tag, String attribute) {
    NodeList nodes = document.getElementsByTagName(tag);
    List<String> values = new ArrayList<>();
    for (int i = 0; i < nodes.getLength(); i++) {
      values.add(((Element) nodes.item(i)).getAttribute(attribute));
    }
    return values;
  }

  private Element withAttribute(Document document, String tag, String attribute, String value) {
    NodeList nodes = document.getElementsByTagName(tag);
    for (int i = 0; i < nodes.getLength(); i++) {
      Element element = (Element) nodes.item(i);
      if (value.equals(element.getAttribute(attribute))) {
        return element;
      }
    }
    return null;
  }

  private List<Element> children(Element element, String tag) {
    List<Element> children = new ArrayList<>();
    NodeList nodes = element.getElementsByTagName(tag);
    for (int i = 0; i < nodes.getLength(); i++) {
      children.add((Element) nodes.item(i));
    }
    return children;
  }

  /**
   * Tell whether the criterion is read-only in the edit mode of its panel: a set-readonly dependency launched by PnlMod
   */
  private boolean isReadOnlyWhenEditing(Document screen, String criterionId) {
    Element criterion = withAttribute(screen, "criteria", "id", criterionId);
    assertThat(criterion).as("criterion " + criterionId).isNotNull();
    for (Element dependency : children(criterion, "dependency")) {
      if ("set-readonly".equals(dependency.getAttribute("target-type"))) {
        for (Element element : children(dependency, "dependency-element")) {
          if ("PnlMod".equals(element.getAttribute("id")) && "edit".equals(element.getAttribute("value"))) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Tell whether the panel keeps the criterion read-only with an empty value: the lock of the name must not depend on it
   */
  private boolean hasElementWithAttribute(Document screen, String tag, String attribute, String value) {
    return withAttribute(screen, tag, attribute, value) != null;
  }

  @Test
  @DisplayName("The menus of the application open both administration screens")
  void menusOpenTheAdministrationScreens() throws Exception {
    Document menu = load("menu/private.xml");
    NodeList options = menu.getElementsByTagName("option");
    List<String> screens = new ArrayList<>();
    List<String> names = new ArrayList<>();
    for (int i = 0; i < options.getLength(); i++) {
      Element option = (Element) options.item(i);
      names.add(option.getAttribute("name"));
      screens.add(option.getAttribute("screen"));
    }

    assertThat(screens).as("Screens of the options sequences and application-parameters")
      .contains("sequence-admin", "application-parameter-admin")
      .doesNotContain("AweKey", "AweAppPar");
    assertThat(names).contains("sequences", "application-parameters");
  }

  @Test
  @DisplayName("The name of a sequence is read-only when editing")
  void sequenceNameIsReadOnlyWhenEditing() throws Exception {
    Document sequence = load(SEQUENCE_SCREEN);

    assertThat(isReadOnlyWhenEditing(sequence, "SeqNam")).as("Read-only name of the sequence panel").isTrue();
    // The grid sends the name of the selected rows to the delete and to the update, so the panel names it SeqNam
    assertThat(withAttribute(sequence, "column", "name", "KeyNam")).as("Column with the name of the sequences").isNotNull();
    assertThat(attributes(sequence, "column", "name")).as("Columns of the grid").contains("StaTxt", "KeyVal");
  }

  @Test
  @DisplayName("The name of an application parameter is read-only when editing")
  void parameterNameIsReadOnlyWhenEditing() throws Exception {
    Document parameter = load(PARAMETER_SCREEN);

    assertThat(isReadOnlyWhenEditing(parameter, "ParNam")).as("Read-only name of the parameter panel").isTrue();
    assertThat(attributes(parameter, "column", "name")).as("Columns of the grid").contains("IdeAweAppPar", "CatTxt", "StaTxt");
  }

  @Test
  @DisplayName("The application parameters screen prints its list and shows the category as a column")
  void parameterScreenPrintsAndShowsTheCategoryAsAColumn() throws Exception {
    Document parameter = load(PARAMETER_SCREEN);

    assertThat(attributes(parameter, "button", "id")).as("Buttons of the screen").contains("ButPrn", "ButNew", "ButDel");
    assertThat(hasElementWithAttribute(parameter, "include", "target-screen", "PrnOpt")).as("Print options").isTrue();

    // The category is a column and the only quick filter of the grid is the state of the parameter
    assertThat(attributes(parameter, "column", "name")).as("Columns of the grid").contains("CatTxt");
    assertThat(attributes(parameter, "quick-filter", "id")).as("Quick filters of the grid")
      .containsExactly("CrtAppParSta");
  }

  @Test
  @DisplayName("Saving an application parameter reloads the properties of the application")
  void savingAParameterReloadsTheProperties() throws Exception {
    Document parameter = load(PARAMETER_SCREEN);

    for (String button : List.of("ButPnlNew", "ButPnlUpd")) {
      Element save = withAttribute(parameter, "button", "id", button);
      assertThat(save).as("Button " + button).isNotNull();
      List<String> actions = new ArrayList<>();
      for (Element action : children(save, "button-action")) {
        actions.add(action.getAttribute("server-action") + " " + action.getAttribute("target-action"));
      }
      String maintain = actions.stream().filter(action -> action.startsWith("maintain "))
        .findFirst().orElseThrow();
      String reload = "maintain-silent refreshDatabaseProperties";
      assertThat(actions).as("Actions of " + button).contains(maintain, reload);
      assertThat(actions.indexOf(reload)).as("The reload comes after the maintain of " + button)
        .isGreaterThan(actions.indexOf(maintain));
    }
  }

  @Test
  @DisplayName("Both screens work on one record with their own maintains")
  void bothScreensUseTheirOwnMaintains() throws Exception {
    Document sequence = load(SEQUENCE_SCREEN);
    Document parameter = load(PARAMETER_SCREEN);
    List<String> targets = attributes(load("global/Maintain.xml"), "target", "name");

    // One maintain per action, and the multiple maintain of the previous screen keeps its own name
    assertThat(targets).contains("AweKeyNew", "AweKeyUpd", "AweKeyDel", "AweKeyMul");
    assertThat(targets).contains("AweAppParNew", "AweAppParUpd", "AweAppParDel", "AweAppParMul");
    assertThat(attributes(sequence, "button-action", "target-action"))
      .contains("AweKeyNew", "AweKeyUpd", "AweKeyDel");
    assertThat(attributes(parameter, "button-action", "target-action"))
      .contains("AweAppParNew", "AweAppParUpd", "AweAppParDel");

    // The deprecated screens keep working with their multiple maintain
    assertThat(attributes(load("screen/configuration/AweKey.xml"), "button-action", "target-action")).contains("AweKeyMul");
    assertThat(attributes(load("screen/configuration/AweAppPar.xml"), "button-action", "target-action")).contains("AweAppParMul");
  }

  @Test
  @DisplayName("The state of both records comes from the queries of their screen")
  void queriesReturnTheStateAndFilterByTheSearch() throws Exception {
    Document queries = load("global/Queries.xml");

    assertThat(attributes(queries, "query", "id")).as("Queries of both screens").contains(
      "AweKeyLst", "KeyStaCnt", "KeyStaCntAct", "KeyStaCntIna", "KeyNamUni",
      "AweAppParLst", "AppParStaCnt", "AppParStaCntAct", "AppParStaCntIna", "AppParNamUni");
    assertThat(attributes(queries, "variable", "name")).as("Search and quick filter criteria of both grids")
      .contains("CrtKeyTxt", "CrtKeySta", "CrtAppParTxt", "CrtAppParSta");
  }
}
