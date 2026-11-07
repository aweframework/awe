package com.almis.awe.screen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A button that opens the side panel of an administration screen as a new record (it sets PnlMod to new) only opens a
 * dialog, so the screen element authorizer cannot derive it from a target: it declares the permission of the maintain
 * its panel creates with, so a user who cannot create does not see it (instead of an empty panel in view mode).
 */
@DisplayName("Create button permissions")
class CreateButtonPermissionTest {

  private static final String AWE = "application/awe/";
  private static final String[] BUTTON_TAGS = {"button", "context-button"};

  private static Document load(String path) throws Exception {
    try (InputStream stream = CreateButtonPermissionTest.class.getClassLoader().getResourceAsStream(AWE + path)) {
      assertThat(stream).as(path).isNotNull();
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      return factory.newDocumentBuilder().parse(stream);
    }
  }

  /**
   * Screens (path under application/awe) that have a button which creates the record of their panel
   */
  static Stream<String> screensWithCreateButton() throws Exception {
    URL root = CreateButtonPermissionTest.class.getClassLoader().getResource(AWE + "screen");
    assertThat(root).isNotNull();
    Path screens = Path.of(root.toURI());
    List<String> paths = new ArrayList<>();
    try (Stream<Path> files = Files.walk(screens)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".xml")).sorted().toList()) {
        String path = "screen/" + screens.relativize(file).toString().replace('\\', '/');
        if (!createMaintains(load(path)).isEmpty()) {
          paths.add(path);
        }
      }
    }
    return paths.stream();
  }

  private static List<Element> buttons(Document screen) {
    List<Element> buttons = new ArrayList<>();
    for (String tag : BUTTON_TAGS) {
      NodeList elements = screen.getElementsByTagName(tag);
      for (int i = 0; i < elements.getLength(); i++) {
        buttons.add((Element) elements.item(i));
      }
    }
    return buttons;
  }

  private static boolean hasChild(Element button, String tag, Map<String, String> attributes) {
    NodeList children = button.getElementsByTagName(tag);
    for (int i = 0; i < children.getLength(); i++) {
      Element child = (Element) children.item(i);
      if (attributes.entrySet().stream().allMatch(entry -> entry.getValue().equals(child.getAttribute(entry.getKey())))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Maintains launched by the buttons shown only while the panel is a new record
   */
  private static List<String> createMaintains(Document screen) {
    List<String> maintains = new ArrayList<>();
    for (Element button : buttons(screen)) {
      if (hasChild(button, "dependency-element", Map.of("id", "PnlMod", "condition", "eq", "value", "new"))) {
        NodeList actions = button.getElementsByTagName("button-action");
        for (int i = 0; i < actions.getLength(); i++) {
          Element action = (Element) actions.item(i);
          if ("server".equals(action.getAttribute("type")) && action.getAttribute("server-action").startsWith("maintain")) {
            maintains.add(action.getAttribute("target-action"));
          }
        }
      }
    }
    return maintains;
  }

  private static Map<String, String> maintainPermissions() throws Exception {
    Map<String, String> permissions = new LinkedHashMap<>();
    NodeList targets = load("global/Maintain.xml").getDocumentElement().getElementsByTagName("target");
    for (int i = 0; i < targets.getLength(); i++) {
      Element target = (Element) targets.item(i);
      permissions.put(target.getAttribute("name"), target.getAttribute("permission"));
    }
    return permissions;
  }

  @Test
  @DisplayName("The administration screens with a create panel are checked")
  void checksTheAdministrationScreens() throws Exception {
    assertThat(screensWithCreateButton()).contains(
      "screen/configuration/application-parameter-admin.xml",
      "screen/configuration/sequence-admin.xml",
      "screen/databases/database-admin.xml",
      "screen/email-servers/email-server-admin.xml",
      "screen/modules/module-admin.xml",
      "screen/profiles/profile-admin.xml",
      "screen/queues/queue-admin.xml",
      "screen/sites/site-admin.xml",
      "screen/themes/theme-admin.xml",
      "screen/users/user-admin.xml");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("screensWithCreateButton")
  @DisplayName("A button that opens the panel as a new record declares the permission of its create maintain")
  void newButtonDeclaresTheCreatePermission(String path) throws Exception {
    Document screen = load(path);
    Map<String, String> permissions = maintainPermissions();
    List<String> required = createMaintains(screen).stream().map(permissions::get).distinct().toList();
    assertThat(required).as("permission of the create maintains %s", createMaintains(screen)).hasSize(1);
    assertThat(required.get(0)).isNotBlank();

    List<String> newButtons = new ArrayList<>();
    for (Element button : buttons(screen)) {
      if (hasChild(button, "button-action", Map.of("type", "value", "target", "PnlMod", "value", "new"))) {
        newButtons.add(button.getAttribute("id") + " permission=" + button.getAttribute("permission"));
      }
    }
    assertThat(newButtons).isNotEmpty()
      .allSatisfy(button -> assertThat(button).endsWith(" permission=" + required.get(0)));
  }
}
