package com.almis.awe.scheduler.descriptor;

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
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A button that opens the screen which creates a record (new-*), navigating to it or as a dialog, launches no target,
 * so the screen element authorizer cannot derive it: it declares the permission of the maintain the create screen
 * saves with, so a user who cannot create does not see it.
 */
@DisplayName("Create button permissions")
class CreateButtonPermissionTest {

  private static final String MODULE = "application/awe-scheduler/";
  private static final String[] BUTTON_TAGS = {"button", "context-button"};
  private static final Set<String> OPEN_ACTIONS = Set.of("screen", "dialog");

  private static Document load(String path) throws Exception {
    try (InputStream stream = CreateButtonPermissionTest.class.getClassLoader().getResourceAsStream(MODULE + path)) {
      assertThat(stream).as(path).isNotNull();
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      return factory.newDocumentBuilder().parse(stream);
    }
  }

  private static List<Element> elements(Document document, String... tags) {
    List<Element> elements = new ArrayList<>();
    for (String tag : tags) {
      NodeList nodes = document.getElementsByTagName(tag);
      for (int i = 0; i < nodes.getLength(); i++) {
        elements.add((Element) nodes.item(i));
      }
    }
    return elements;
  }

  /**
   * Buttons ("screen#button") of the module screens that open a create screen, with that screen
   */
  static Map<String, String> createButtons() throws Exception {
    URL root = CreateButtonPermissionTest.class.getClassLoader().getResource(MODULE + "screen");
    assertThat(root).isNotNull();
    Map<String, String> buttons = new LinkedHashMap<>();
    try (Stream<Path> files = Files.list(Path.of(root.toURI()))) {
      for (Path file : files.filter(path -> path.toString().endsWith(".xml")).sorted().toList()) {
        String screen = file.getFileName().toString().replace(".xml", "");
        for (Element button : elements(load("screen/" + screen + ".xml"), BUTTON_TAGS)) {
          NodeList actions = button.getElementsByTagName("button-action");
          for (int i = 0; i < actions.getLength(); i++) {
            Element action = (Element) actions.item(i);
            if (OPEN_ACTIONS.contains(action.getAttribute("type")) && action.getAttribute("target").startsWith("new-")) {
              buttons.put(screen + "#" + button.getAttribute("id"), action.getAttribute("target"));
            }
          }
        }
      }
    }
    return buttons;
  }

  static Stream<String> createButtonKeys() throws Exception {
    return createButtons().keySet().stream();
  }

  private static Element button(String key) throws Exception {
    String[] parts = key.split("#");
    return elements(load("screen/" + parts[0] + ".xml"), BUTTON_TAGS).stream()
      .filter(button -> parts[1].equals(button.getAttribute("id")))
      .findFirst().orElseThrow();
  }

  /**
   * Permissions of the maintains the create screen saves with
   */
  private static List<String> createPermissions(String screen) throws Exception {
    Map<String, String> permissions = new LinkedHashMap<>();
    for (Element target : elements(load("global/Maintain.xml"), "target")) {
      permissions.put(target.getAttribute("name"), target.getAttribute("permission"));
    }
    return elements(load("screen/" + screen + ".xml"), "button-action").stream()
      .filter(action -> "server".equals(action.getAttribute("type")) && "maintain".equals(action.getAttribute("server-action")))
      .map(action -> permissions.get(action.getAttribute("target-action")))
      .distinct().toList();
  }

  @Test
  @DisplayName("The buttons that open a create screen are checked")
  void checksTheCreateButtons() throws Exception {
    assertThat(createButtons()).containsOnly(
      Map.entry("scheduler-calendars#ButNew", "new-scheduler-calendar"),
      Map.entry("scheduler-dashboard#ButNew", "new-scheduler-task"),
      Map.entry("scheduler-server#ButNew", "new-scheduler-server"),
      Map.entry("scheduler-tasks#ButNew", "new-scheduler-task"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("createButtonKeys")
  @DisplayName("A button that opens a create screen declares the permission of its create maintain")
  void createButtonDeclaresTheCreatePermission(String key) throws Exception {
    List<String> required = createPermissions(createButtons().get(key));
    assertThat(required).as("permission of the create maintains of %s", createButtons().get(key)).hasSize(1);
    assertThat(required.get(0)).isNotBlank();
    assertThat(button(key).getAttribute("permission")).as(key).isEqualTo(required.get(0));
  }
}
