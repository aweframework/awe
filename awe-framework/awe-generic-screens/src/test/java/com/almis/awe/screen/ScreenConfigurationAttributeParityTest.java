package com.almis.awe.screen;

import com.almis.awe.model.type.AttributeRestrictionType;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The attribute selector of the screen configuration (ScrCnfAtrTyp) and the attributes the server can apply
 * (AttributeRestrictionType) must offer the same values: one that is only in the selector fails when the screen is built
 */
class ScreenConfigurationAttributeParityTest {

  @Test
  void theAttributeSelectorOffersExactlyTheAttributesTheServerApplies() throws Exception {
    List<String> selector = selectorValues();
    List<String> attributes = Arrays.stream(AttributeRestrictionType.values()).map(AttributeRestrictionType::getValue).toList();

    assertThat(selector).containsExactlyInAnyOrderElementsOf(attributes);
  }

  @Test
  void everySelectorOptionHasALocale() throws Exception {
    for (String locale : List.of("en-GB", "es-ES", "fr-FR")) {
      String content = new String(resource("application/awe/locale/Locale-" + locale + ".xml").readAllBytes());
      for (String label : selectorLabels()) {
        assertThat(content).as(locale + " " + label).contains("name=\"" + label + "\"");
      }
    }
  }

  private List<String> selectorValues() throws Exception {
    return selectorOptions().stream().map(option -> option.getAttribute("value")).toList();
  }

  private List<String> selectorLabels() throws Exception {
    return selectorOptions().stream().map(option -> option.getAttribute("label")).toList();
  }

  private List<org.w3c.dom.Element> selectorOptions() throws Exception {
    org.w3c.dom.Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
      .parse(resource("application/awe/global/Enumerated.xml"));
    org.w3c.dom.NodeList groups = document.getElementsByTagName("group");
    for (int i = 0; i < groups.getLength(); i++) {
      org.w3c.dom.Element group = (org.w3c.dom.Element) groups.item(i);
      if ("ScrCnfAtrTyp".equals(group.getAttribute("id"))) {
        org.w3c.dom.NodeList options = group.getElementsByTagName("option");
        return IntStream.range(0, options.getLength()).mapToObj(index -> (org.w3c.dom.Element) options.item(index)).toList();
      }
    }
    throw new IllegalStateException("ScrCnfAtrTyp group not found");
  }

  private InputStream resource(String path) {
    return getClass().getClassLoader().getResourceAsStream(path);
  }
}
