package com.almis.awe.builder.enumerates;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps the builder {@link Action} enum inside the screen contract: every action the builder emits must be a value
 * of the buttonActionEnum of general.xsd, which is what both client engines handle (#823).
 */
class ActionContractTest {

  // general.xsd from the module folder (Maven basedir) or from the repository root (IDE runners)
  private static final String SCHEMA = "awe-generic-screens/src/main/resources/schemas/awe/general.xsd";
  private static final Path GENERAL_XSD = Stream.of(
      Path.of(System.getProperty("basedir", ".")).resolve("../../" + SCHEMA),
      Path.of("awe-framework/" + SCHEMA))
    .filter(Files::isRegularFile).findFirst().orElse(Path.of(SCHEMA));

  private static final Set<String> contract = new HashSet<>();
  private static final Set<String> deprecatedInContract = new HashSet<>();

  @BeforeAll
  static void loadContract() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    Element root = factory.newDocumentBuilder().parse(GENERAL_XSD.toFile()).getDocumentElement();
    NodeList types = root.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "simpleType");
    for (int i = 0; i < types.getLength(); i++) {
      Element type = (Element) types.item(i);
      if ("buttonActionEnum".equals(type.getAttribute("name"))) {
        NodeList values = type.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "enumeration");
        for (int j = 0; j < values.getLength(); j++) {
          Element value = (Element) values.item(j);
          contract.add(value.getAttribute("value"));
          if (value.getTextContent().contains("Deprecated")) {
            deprecatedInContract.add(value.getAttribute("value"));
          }
        }
      }
    }
    assertTrue(contract.size() > 50, "buttonActionEnum not found in " + GENERAL_XSD);
  }

  private static boolean isDeprecated(Action action) throws NoSuchFieldException {
    return Action.class.getField(action.name()).isAnnotationPresent(Deprecated.class);
  }

  @Test
  void everyActionIsAContractValue() throws NoSuchFieldException {
    List<String> outside = new ArrayList<>();
    for (Action action : Action.values()) {
      if (!contract.contains(action.toString()) && !isDeprecated(action)) {
        outside.add(action.name() + "=" + action);
      }
    }
    assertEquals(List.of(), outside, "builder actions that no client engine handles");
  }

  @Test
  void deprecatedContractValuesAreDeprecatedInTheBuilder() throws NoSuchFieldException {
    for (Action action : Action.values()) {
      if (deprecatedInContract.contains(action.toString())) {
        assertTrue(isDeprecated(action), action.name() + " emits a deprecated contract value");
      }
    }
  }

  @Test
  void gridSelectionActionsUseTheContractNames() {
    assertEquals("check-records-generated", Action.CHECK_RECORDS_GENERATED.toString());
    assertEquals("select-first-row", Action.SELECT_FIRST_ROW.toString());
    assertEquals("select-last-row", Action.SELECT_LAST_ROW.toString());
  }
}
