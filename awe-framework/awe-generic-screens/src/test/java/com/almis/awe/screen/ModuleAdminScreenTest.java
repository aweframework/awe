package com.almis.awe.screen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The modules screen (module-admin) is one window with the grid of modules and one 640px side panel with three tabs
 * (General, Access, Sites and databases) and no theme. Its texts exist in every language, and the screens it replaces
 * are kept, marked as deprecated.
 */
@DisplayName("Module admin screen")
class ModuleAdminScreenTest {

  private static final String AWE = "application/awe/";
  private static final String SCREEN = "screen/modules/module-admin.xml";

  private Document load(String path) throws Exception {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(AWE + path)) {
      assertThat(stream).as(path).isNotNull();
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      return factory.newDocumentBuilder().parse(stream);
    }
  }

  private String text(String path) throws Exception {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(AWE + path)) {
      assertThat(stream).as(path).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static List<Element> elements(Element parent, String tag) {
    NodeList nodes = parent.getElementsByTagName(tag);
    List<Element> list = new ArrayList<>();
    for (int i = 0; i < nodes.getLength(); i++) {
      list.add((Element) nodes.item(i));
    }
    return list;
  }

  private static Element byId(Document document, String id) {
    // A dependency element names its source component by id too
    return elements(document.getDocumentElement(), "*").stream()
      .filter(element -> !"dependency-element".equals(element.getTagName()) && id.equals(element.getAttribute("id")))
      .findFirst()
      .orElseThrow(() -> new AssertionError("No element " + id));
  }

  private static List<String> attributes(List<Element> elements, String attribute) {
    return elements.stream().map(element -> element.getAttribute(attribute)).toList();
  }

  @Test
  @DisplayName("The screen is valid against the screen schema")
  void screenIsValid() throws Exception {
    File schema = Paths.get(getClass().getResource("/schemas/awe/screen.xsd").toURI()).toFile();
    File screen = Paths.get(getClass().getResource("/" + AWE + SCREEN).toURI()).toFile();

    assertThatCode(() -> SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schema).newValidator()
      .validate(new StreamSource(screen))).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("The grid lists the modules with search, quick filters and row actions, and sends only IdeModSel")
  void gridOfModules() throws Exception {
    Document screen = load(SCREEN);
    Element grid = byId(screen, "GrdModAdm");

    assertThat(grid.getAttribute("target-action")).isEqualTo("ModLstAdm");
    assertThat(byId(screen, "CrtModTxt").getTagName()).isEqualTo("search");
    assertThat(byId(screen, "CrtModSta").getAttribute("target-action")).isEqualTo("ModStaCnt");
    List<Element> columns = elements(grid, "column");
    assertThat(attributes(columns, "name")).containsSubsequence("Nam", "ProCnt", "UsrCnt", "SitCnt", "Ord", "StaTxt", "EdtBtn",
      "MorBtn").doesNotContain("IdeThm", "ThmTxt");
    assertThat(columns.stream().filter(column -> !"false".equals(column.getAttribute("sendable"))).map(column -> column.getAttribute("name")))
      .containsExactly("IdeModSel");
    Element name = columns.getFirst();
    assertThat(name.getAttribute("avatar-field")).isEqualTo("Ini");
    assertThat(name.getAttribute("secondary-field")).isEqualTo("ScrIni");
    assertThat(attributes(elements(grid, "context-button"), "id")).containsExactly("CtxButUpd", "CtxButDup", "CtxButDel");
    Element delete = byId(screen, "ButDel");
    assertThat(delete.getAttribute("selection")).isEqualTo("true");
    assertThat(delete.getAttribute("style")).isEqualTo("btn-danger");
    assertThat(attributes(elements(delete, "button-action"), "target-action")).contains("ModDel");
    assertThat(byId(screen, "ButNew").getAttribute("style")).isEqualTo("btn-primary");
  }

  @Test
  @DisplayName("One 640px side panel with the General, Access and Sites and databases tabs, opened on General")
  void sidePanelWithTabs() throws Exception {
    Document screen = load(SCREEN);
    Element panel = byId(screen, "PnlModAdm");

    assertThat(panel.getAttribute("max-width")).isEqualTo("640px");
    assertThat(panel.getAttribute("header-grid")).isEqualTo("GrdModAdm");
    assertThat(elements(screen.getDocumentElement(), "side-panel")).hasSize(1);
    Element tab = byId(screen, "ModTab");
    assertThat(tab.getAttribute("target-action")).isEqualTo("module-admin-tabs");
    assertThat(attributes(elements(tab, "tabcontainer"), "id")).containsExactly("general", "access", "sites");
    assertThat(attributes(elements(byId(screen, "general"), "criteria"), "id")).containsExactly("Nam", "Scr", "Ord", "Act");
    // A module is one of the modules declared in the menu, as in the deprecated screens: a select of ModSel
    Element name = byId(screen, "Nam");
    assertThat(name.getAttribute("component")).isEqualTo("select");
    assertThat(name.getAttribute("target-action")).isEqualTo("ModSel");
    assertThat(attributes(elements(byId(screen, "access"), "grid"), "id")).containsExactly("MdlPrfLst", "MdlUsrLst");
    assertThat(attributes(elements(byId(screen, "sites"), "grid"), "id")).containsExactly("MdlSitDbsLst");
    // One visible column in each Access grid; no theme anywhere
    assertThat(visibleColumns(byId(screen, "MdlPrfLst"))).containsExactly("IdePro");
    assertThat(visibleColumns(byId(screen, "MdlUsrLst"))).containsExactly("IdeOpe");
    assertThat(visibleColumns(byId(screen, "MdlSitDbsLst"))).containsExactly("IdeSit", "IdeDbs", "SitModOrd");
    assertThat(attributes(elements(screen.getDocumentElement(), "criteria"), "id")).doesNotContain("Thm", "IdeThm");
    assertThat(attributes(elements(screen.getDocumentElement(), "column"), "name")).doesNotContain("IdeThm", "OpeThm");
    // Every action that opens the panel selects the General tab first
    for (String opener : List.of("ButNew", "EdtBtn", "CtxButUpd", "CtxButDup")) {
      Element element = elements(screen.getDocumentElement(), "*").stream()
        .filter(candidate -> opener.equals(candidate.getAttribute("id")) || opener.equals(candidate.getAttribute("name")))
        .findFirst().orElseThrow();
      List<Element> actions = elements(element, "button-action");
      List<String> targets = actions.stream().map(action -> action.getAttribute("type") + ":" + action.getAttribute("target")).toList();
      assertThat(targets).as(opener).containsSubsequence("value:ModTab", "dialog:PnlModAdm");
    }
    // The module of the panel is a hidden criterion; new and duplicate clear it
    assertThat(byId(screen, "IdeMod").getAttribute("component")).isEqualTo("hidden");
    assertThat(attributes(elements(byId(screen, "CtxButDup"), "button-action"), "target")).contains("IdeMod");
    assertThat(attributes(elements(byId(screen, "ButPnlUpd"), "button-action"), "target-action")).contains("ModUpd");
    assertThat(attributes(elements(byId(screen, "ButPnlNew"), "button-action"), "target-action")).contains("ModNamUni", "ModNew");
  }

  private static List<String> visibleColumns(Element grid) {
    return elements(grid, "column").stream().filter(column -> !"true".equals(column.getAttribute("hidden")))
      .map(column -> column.getAttribute("name")).toList();
  }

  @Test
  @DisplayName("The panel tabs are an enumerated group")
  void tabsAreEnumerated() throws Exception {
    Element group = elements(load("global/Enumerated.xml").getDocumentElement(), "group").stream()
      .filter(element -> "module-admin-tabs".equals(element.getAttribute("id"))).findFirst().orElseThrow();
    assertThat(attributes(elements(group, "option"), "value")).containsExactly("general", "access", "sites");
  }

  @ParameterizedTest
  @ValueSource(strings = {"Locale-en-GB.xml", "Locale-es-ES.xml", "Locale-fr-FR.xml"})
  @DisplayName("Every text of the screen, its tabs and its queries is translated")
  void textsAreTranslated(String locale) throws Exception {
    Set<String> keys = new LinkedHashSet<>();
    Matcher matcher = Pattern.compile("\"([A-Z][A-Z0-9_]+)\"").matcher(text(SCREEN));
    while (matcher.find()) {
      keys.add(matcher.group(1));
    }
    keys.addAll(List.of("SCREEN_TEXT_MODULE_TAB_GENERAL", "SCREEN_TEXT_MODULE_TAB_ACCESS", "SCREEN_TEXT_MODULE_TAB_SITES",
      "SCREEN_TEXT_MODULE_FILTER_ALL", "SCREEN_TEXT_MODULE_FILTER_ACTIVE", "SCREEN_TEXT_MODULE_FILTER_INACTIVE"));
    Set<String> defined = new LinkedHashSet<>(attributes(elements(load("locale/" + locale).getDocumentElement(), "locale"), "name"));

    assertThat(keys).hasSizeGreaterThan(20);
    assertThat(defined).as(locale).containsAll(keys);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Mod", "ModNew", "ModUpd", "ModViw"})
  @DisplayName("The replaced screens are kept, marked as deprecated")
  void replacedScreensAreDeprecated(String screen) throws Exception {
    String source = text("screen/modules/" + screen + ".xml");
    String header = source.substring(0, source.indexOf("-->"));

    assertThat(header).contains("DEPRECATED since AWE 5.0.0: replaced by the module-admin screen (screen/modules/module-admin.xml)");
    assertThat(load("screen/modules/" + screen + ".xml").getDocumentElement().getTagName()).isEqualTo("screen");
  }
}
