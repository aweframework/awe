package com.almis.awe.model.entities.screen.component;

import com.almis.awe.model.constant.AweConstants;
import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Side panel container: a dialog anchored to the left or right edge of the screen (#843)
 */
class SidePanelTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private static SidePanel parse(String xml) {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(SidePanel.class);
    return (SidePanel) xstream.fromXML(xml);
  }

  @Test
  void isOpenedAndClosedAsADialog() {
    SidePanel sidePanel = new SidePanel();

    assertThat(sidePanel).isInstanceOf(Dialog.class);
    assertThat(sidePanel.getComponentTag()).isEqualTo("side-panel");
  }

  @Test
  void attributesAreReadFromTheXml() {
    SidePanel sidePanel = parse("<side-panel id=\"Pnl\" label=\"LABEL\" position=\"left\" max-width=\"30rem\" " +
      "deferred=\"true\" on-close=\"reject\"/>");

    assertThat(sidePanel.getId()).isEqualTo("Pnl");
    assertThat(sidePanel.getPosition()).isEqualTo("left");
    assertThat(sidePanel.getMaxWidth()).isEqualTo("30rem");
    assertThat(sidePanel.isDeferred()).isTrue();
    assertThat(sidePanel.acceptOnClose()).isFalse();
  }

  @Test
  void clientReceivesTheRightEdgeByDefault() {
    JsonNode json = mapper.valueToTree(new SidePanel());

    assertThat(json.get("elementType").asString()).isEqualTo("SidePanel");
    assertThat(json.get("position").asString()).isEqualTo("right");
    assertThat(json.has("maxWidth")).isFalse();
    assertThat(json.has("deferred")).isFalse();
    assertThat(json.get("acceptOnClose").asBoolean()).isTrue();
  }

  @Test
  void clientReceivesThePositionAndMaxWidth() {
    SidePanel sidePanel = new SidePanel();
    sidePanel.setPosition("left");
    sidePanel.setMaxWidth("480px");

    JsonNode json = mapper.valueToTree(sidePanel);

    assertThat(json.get("position").asString()).isEqualTo("left");
    assertThat(json.get("maxWidth").asString()).isEqualTo("480px");
  }

  @Test
  void attributesSurviveACopy() {
    SidePanel sidePanel = new SidePanel();
    sidePanel.setPosition("left");
    sidePanel.setMaxWidth("480px");
    sidePanel.setDeferred(true);

    SidePanel copy = sidePanel.copy();

    assertThat(copy.getPosition()).isEqualTo("left");
    assertThat(copy.getMaxWidth()).isEqualTo("480px");
    assertThat(copy.isDeferred()).isTrue();
  }

  @Test
  void identityHeaderIsReadFromTheXml() {
    SidePanel sidePanel = parse("<side-panel id=\"Pnl\" label=\"LABEL\" header-grid=\"Grd\" header-title-field=\"FulNam\" " +
      "header-subtitle-field=\"UsrPro\" header-avatar-field=\"Ini\" header-avatar-tone-field=\"IniTon\">" +
      "<header-tag field=\"StaTxt\" tone-field=\"StaTon\"/>" +
      "<header-tag field=\"2faHdr\" tone-field=\"2faTon\" icon-field=\"2faIcn\"/></side-panel>");

    assertThat(sidePanel.getHeaderGrid()).isEqualTo("Grd");
    assertThat(sidePanel.getHeaderTitleField()).isEqualTo("FulNam");
    assertThat(sidePanel.getHeaderSubtitleField()).isEqualTo("UsrPro");
    assertThat(sidePanel.getHeaderAvatarField()).isEqualTo("Ini");
    assertThat(sidePanel.getHeaderAvatarToneField()).isEqualTo("IniTon");
    List<SidePanelHeaderTag> tags = sidePanel.getHeaderTags();
    assertThat(tags).extracting(SidePanelHeaderTag::getField).containsExactly("StaTxt", "2faHdr");
    assertThat(tags).extracting(SidePanelHeaderTag::getToneField).containsExactly("StaTon", "2faTon");
    assertThat(tags).extracting(SidePanelHeaderTag::getIconField).containsExactly(null, "2faIcn");
  }

  @Test
  void clientReceivesTheIdentityHeaderWithItsTagsInOrder() {
    SidePanel sidePanel = parse("<side-panel id=\"Pnl\" header-grid=\"Grd\" header-title-field=\"FulNam\" " +
      "header-subtitle-field=\"UsrPro\" header-avatar-field=\"Ini\" header-avatar-tone-field=\"IniTon\">" +
      "<header-tag field=\"StaTxt\" tone-field=\"StaTon\"/>" +
      "<header-tag field=\"2faHdr\" tone-field=\"2faTon\" icon-field=\"2faIcn\"/></side-panel>");

    JsonNode json = mapper.valueToTree(sidePanel);

    assertThat(json.get("headerGrid").asString()).isEqualTo("Grd");
    assertThat(json.get("headerTitleField").asString()).isEqualTo("FulNam");
    assertThat(json.get("headerSubtitleField").asString()).isEqualTo("UsrPro");
    assertThat(json.get("headerAvatarField").asString()).isEqualTo("Ini");
    assertThat(json.get("headerAvatarToneField").asString()).isEqualTo("IniTon");
    JsonNode tags = json.get("headerTags");
    assertThat(tags.size()).isEqualTo(2);
    assertThat(tags.get(0).get("field").asString()).isEqualTo("StaTxt");
    assertThat(tags.get(0).get("toneField").asString()).isEqualTo("StaTon");
    assertThat(tags.get(0).has("iconField")).isFalse();
    assertThat(tags.get(1).get("field").asString()).isEqualTo("2faHdr");
    assertThat(tags.get(1).get("iconField").asString()).isEqualTo("2faIcn");
  }

  @Test
  void clientReceivesNoIdentityHeaderWithoutHeaderGrid() {
    JsonNode json = mapper.valueToTree(parse("<side-panel id=\"Pnl\" label=\"LABEL\"/>"));

    assertThat(json.has("headerGrid")).isFalse();
    assertThat(json.has("headerTitleField")).isFalse();
    assertThat(json.has("headerTags")).isFalse();
  }

  @Test
  void identityHeaderSurvivesACopy() {
    SidePanel sidePanel = parse("<side-panel id=\"Pnl\" header-grid=\"Grd\" header-title-field=\"FulNam\">" +
      "<header-tag field=\"StaTxt\" tone-field=\"StaTon\"/></side-panel>");

    SidePanel copy = sidePanel.copy();

    assertThat(copy.getHeaderGrid()).isEqualTo("Grd");
    assertThat(copy.getHeaderTitleField()).isEqualTo("FulNam");
    assertThat(copy.getHeaderTags()).extracting(SidePanelHeaderTag::getField).containsExactly("StaTxt");
  }

  @Test
  void headerTagHasNoMarkupWhereItIsDeclared() {
    assertThat(new SidePanelHeaderTag().getTemplate()).isEqualTo(AweConstants.TEMPLATE_EMPTY);
  }
}
