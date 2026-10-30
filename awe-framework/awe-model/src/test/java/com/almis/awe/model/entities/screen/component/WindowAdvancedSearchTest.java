package com.almis.awe.model.entities.screen.component;

import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.Tag;
import com.almis.awe.model.entities.screen.component.criteria.Criteria;
import com.almis.awe.model.entities.screen.data.ScreenComponent;
import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A window that holds an advanced block tells the clients how to present it (#790). Any other window and its criteria
 * send nothing new (S9)
 */
class WindowAdvancedSearchTest {

  private static Criteria criterion(String id, String component) {
    Criteria criterion = new Criteria();
    criterion.setComponentType(component);
    criterion.setId(id);
    return criterion;
  }

  private static <T extends Element> T withChildren(T element, Element... children) {
    element.setElementList(new ArrayList<>(List.of(children)));
    return element;
  }

  private static Window window(Element... children) {
    Window window = withChildren(new Window(), children);
    window.setId("WinCrt");
    return window;
  }

  private static JsonNode json(Object element) {
    return new JsonMapper().valueToTree(element);
  }

  private static Tag parse(String xml) {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Element.class);
    return (Tag) xstream.fromXML(xml);
  }

  @Test
  void aWindowWithAnAdvancedBlockListsItsCriteriaAndShowsThemInlineByDefault() {
    Tag body = withChildren(new Tag(), criterion("CrtUsr", "text"), criterion("Key", "hidden"),
      withChildren(new Advanced(), withChildren(new Tag(), criterion("CrtThm", "select"), criterion("CrtBlk", "checkbox")),
        criterion("CrtScr", "suggest"), criterion("Hid", "hidden")),
      criterion("CrtPro", "select"));

    JsonNode json = json(window(body));

    assertThat(json.get("advancedMode").asString()).isEqualTo("inline");
    assertThat(json.get("advancedCriteria").toString()).isEqualTo("[\"CrtThm\",\"CrtBlk\",\"CrtScr\"]");
    assertThat(json.get("searchCriteria").toString()).isEqualTo("[\"CrtUsr\",\"CrtThm\",\"CrtBlk\",\"CrtScr\",\"CrtPro\"]");
  }

  @Test
  void aWindowCanShowItsAdvancedBlockInASidePanel() {
    Window window = window(criterion("CrtUsr", "text"), withChildren(new Advanced(), criterion("CrtThm", "select")));
    window.setAdvancedMode("side-panel");

    assertThat(json(window).get("advancedMode").asString()).isEqualTo("side-panel");
  }

  @Test
  void aWindowWithoutAnAdvancedBlockSendsNothingNew() {
    Window window = window(criterion("CrtUsr", "text"), withChildren(new Tag(), criterion("CrtThm", "select")));
    window.setAdvancedMode("side-panel");

    JsonNode json = json(window);

    assertThat(json.get("elementType").asString()).isEqualTo("Window");
    assertThat(json.has("advancedMode")).isFalse();
    assertThat(json.has("advancedCriteria")).isFalse();
    assertThat(json.has("searchCriteria")).isFalse();
  }

  @Test
  void aCriterionHasNoAdvancedFlag() {
    assertThat(json(criterion("CrtThm", "select")).has("advanced")).isFalse();
  }

  @Test
  void theControllerKeepsTheListsWithoutTheChildren() {
    Window window = window(criterion("CrtUsr", "text"), withChildren(new Advanced(), criterion("CrtThm", "select")));

    JsonNode controller = json(new ScreenComponent().setId("WinCrt").setController(window.copy())).get("controller");

    assertThat(controller.has("elementList")).isFalse();
    assertThat(controller.get("advancedMode").asString()).isEqualTo("inline");
    assertThat(controller.get("advancedCriteria").toString()).isEqualTo("[\"CrtThm\"]");
    assertThat(controller.get("searchCriteria").toString()).isEqualTo("[\"CrtUsr\",\"CrtThm\"]");
  }

  @Test
  void theAdvancedBlockIsAContainerOfTheScreenStructure() {
    Advanced advanced = withChildren(new Advanced(), criterion("CrtThm", "select"));
    advanced.setId("AdvCrt");

    JsonNode json = json(advanced);

    assertThat(advanced.getComponentTag()).isEqualTo("advanced");
    assertThat(json.get("elementType").asString()).isEqualTo("Advanced");
    assertThat(json.get("id").asString()).isEqualTo("AdvCrt");
    assertThat(json.get("elementList").get(0).get("id").asString()).isEqualTo("CrtThm");
  }

  @Test
  void aCopyKeepsTheModeAndTheAdvancedBlock() {
    Window window = window(withChildren(new Advanced(), criterion("CrtThm", "select")));
    window.setAdvancedMode("side-panel");

    Window copy = window.copy();

    assertThat(copy.getAdvancedMode()).isEqualTo("side-panel");
    assertThat((Object) copy.getElementList().get(0)).isInstanceOf(Advanced.class);
    assertThat((Object) copy.getElementList().get(0).copy()).isInstanceOf(Advanced.class);
  }

  @Test
  void theAdvancedBlockIsReadFromTheXmlInsideAWindow() {
    Tag tag = parse("<tag source=\"center\"><window id=\"WinCrt\" label=\"LABEL\" advanced-mode=\"side-panel\">" +
      "<tag type=\"div\" style=\"panel-body\"><criteria id=\"CrtUsr\" component=\"text\"/>" +
      "<advanced><tag type=\"div\"><criteria id=\"CrtThm\" component=\"select\"/></tag></advanced></tag>" +
      "<button id=\"ButSch\" button-type=\"submit\" label=\"BUTTON_SEARCH\"/></window></tag>");

    Window window = (Window) tag.getElementList().get(0);
    assertThat(window.getAdvancedMode()).isEqualTo("side-panel");
    assertThat((Object) window.getElementList().get(0).getElementList().get(1)).isInstanceOf(Advanced.class);
    assertThat(window.getAdvancedCriteria()).containsExactly("CrtThm");
    assertThat(window.getSearchCriteria()).containsExactly("CrtUsr", "CrtThm");
  }
}
