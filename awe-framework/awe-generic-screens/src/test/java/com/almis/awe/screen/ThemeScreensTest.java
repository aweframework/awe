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
 * The AWE 4 *Customize theme* screen is retired in AWE 5: a theme is a primary, a surface and a mode, edited in the
 * new and update theme screens, so nothing may still lead to it.
 */
@DisplayName("Theme screens")
class ThemeScreensTest {

  private static final String AWE = "application/awe/";

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

  @Test
  @DisplayName("The themes screen has no button to customize a theme")
  void themesScreenHasNoCustomizeButton() throws Exception {
    Document themes = load("screen/themes/Thm.xml");

    assertThat(attributes(themes, "button", "id")).contains("ButNew", "ButUpd").doesNotContain("ButCus");
    assertThat(attributes(themes, "button-action", "target")).doesNotContain("customize-theme");
  }

  @Test
  @DisplayName("No menu option, maintain or query of the customize theme screen is left")
  void customizeThemeScreenIsNotReachable() throws Exception {
    assertThat(attributes(load("menu/private.xml"), "option", "screen")).contains("ThmUpd")
      .doesNotContain("customize-themes");
    assertThat(attributes(load("global/Maintain.xml"), "target", "name")).contains("selectDarkMode")
      .doesNotContain("customizeTheme");
    assertThat(attributes(load("global/Queries.xml"), "query", "id")).contains("getUserThemeMode")
      .doesNotContain("themeConfiguration", "nextIdForThemeColors");
  }
}
