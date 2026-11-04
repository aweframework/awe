package com.almis.awe.screen;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reading of the descriptors of the access window (access-admin) for its descriptor tests: the elements by id, the
 * actions of a button and the dependencies that show or enable an element.
 */
final class AccessAdminDescriptor {

  static final String AWE = "application/awe/";
  static final String SCREEN = "screen/security/access-admin.xml";
  static final String GRID = "GrdAcc";

  private AccessAdminDescriptor() {
  }

  static Document load(String path) throws Exception {
    try (InputStream stream = AccessAdminDescriptor.class.getClassLoader().getResourceAsStream(path)) {
      assertThat(stream).as(path).isNotNull();
      return parse(stream);
    }
  }

  static Document parse(InputStream stream) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory.newDocumentBuilder().parse(stream);
  }

  static String text(String path) throws Exception {
    try (InputStream stream = AccessAdminDescriptor.class.getClassLoader().getResourceAsStream(path)) {
      assertThat(stream).as(path).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  static List<Element> elements(Element parent, String tag) {
    NodeList nodes = parent.getElementsByTagName(tag);
    List<Element> list = new ArrayList<>();
    for (int i = 0; i < nodes.getLength(); i++) {
      list.add((Element) nodes.item(i));
    }
    return list;
  }

  static Element byId(Document document, String id) {
    // A dependency element names its source component by id too; a column is named by its name
    return elements(document.getDocumentElement(), "*").stream()
      .filter(element -> !"dependency-element".equals(element.getTagName()))
      .filter(element -> id.equals(element.getAttribute("id"))
        || ("column".equals(element.getTagName()) && id.equals(element.getAttribute("name"))))
      .findFirst()
      .orElseThrow(() -> new AssertionError("No element " + id));
  }

  static List<String> attributes(List<Element> elements, String attribute) {
    return elements.stream().map(element -> element.getAttribute(attribute)).toList();
  }

  /**
   * Actions of a button or a button column, in order, as type:target-action, type:server-action (a server action without
   * target action) or type:target
   */
  static List<String> actions(Element button) {
    return elements(button, "button-action").stream()
      .map(action -> action.getAttribute("type") + ":" + List.of(action.getAttribute("target-action"),
        action.getAttribute("server-action"), action.getAttribute("target")).stream()
        .filter(value -> !value.isEmpty()).findFirst().orElse(""))
      .toList();
  }

  /**
   * Condition and value of the dependency of an element that shows it, on a source element: "condition value" (only the
   * value for the subject type)
   */
  static String showWhen(Element element, String targetType, String source) {
    return elements(element, "dependency").stream()
      .filter(dependency -> targetType.equals(dependency.getAttribute("target-type")))
      .flatMap(dependency -> elements(dependency, "dependency-element").stream())
      .filter(dependencyElement -> source.equals(dependencyElement.getAttribute("id")))
      .map(dependencyElement -> "subject-type".equals(source) ? dependencyElement.getAttribute("value")
        : dependencyElement.getAttribute("condition") + " " + dependencyElement.getAttribute("value"))
      .findFirst().orElse("");
  }
}
