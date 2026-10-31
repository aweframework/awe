package com.almis.awe.schemas;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The screen schema lets a window hold an advanced block and choose how it is shown (#790)
 */
class AdvancedSearchSchemaTest {

  private static File schemaFile;

  @BeforeAll
  static void loadSchema() throws Exception {
    schemaFile = Paths.get(AdvancedSearchSchemaTest.class.getResource("/schemas/awe/screen.xsd").toURI()).toFile();
  }

  private static void validate(String body) throws Exception {
    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schemaFile).newValidator()
      .validate(new StreamSource(new StringReader(
        "<screen template=\"window\" label=\"LABEL\"><tag source=\"center\">" + body + "</tag></screen>")));
  }

  @Test
  void acceptsAnAdvancedBlockAfterTheEssentialCriteriaOfAnInlineWindow() {
    assertThatCode(() -> validate("<window id=\"WinCrt\" label=\"LABEL\" advanced-mode=\"inline\">" +
      "<tag type=\"div\" style=\"panel-body\"><criteria id=\"CrtUsr\" component=\"text\"/>" +
      "<advanced><tag type=\"div\"><criteria id=\"CrtThm\" component=\"select\"/></tag>" +
      "<criteria id=\"CrtBlk\" component=\"checkbox\"/></advanced></tag>" +
      "<tag type=\"div\" style=\"panel-footer\"><button id=\"ButSch\" button-type=\"submit\" label=\"BUTTON_SEARCH\"/></tag>" +
      "</window>"))
      .doesNotThrowAnyException();
  }

  @Test
  void acceptsAnAdvancedBlockDirectlyInASidePanelWindow() {
    assertThatCode(() -> validate("<window label=\"LABEL\" advanced-mode=\"side-panel\">" +
      "<criteria id=\"CrtUsr\" component=\"text\"/><advanced id=\"AdvCrt\" style=\"row\">" +
      "<criteria id=\"CrtBlk\" component=\"checkbox\"/></advanced></window>"))
      .doesNotThrowAnyException();
  }

  @Test
  void acceptsTheSavedSearchesAttributeOfAWindow() {
    assertThatCode(() -> validate("<window label=\"LABEL\" saved-searches=\"true\">" +
      "<criteria id=\"CrtUsr\" component=\"text\"/><advanced><criteria id=\"CrtBlk\" component=\"checkbox\"/></advanced></window>"))
      .doesNotThrowAnyException();
  }

  @Test
  void rejectsASavedSearchesValueThatIsNotABoolean() {
    assertThatThrownBy(() -> validate("<window label=\"LABEL\" saved-searches=\"always\">" +
      "<advanced><criteria id=\"CrtBlk\" component=\"checkbox\"/></advanced></window>"))
      .hasMessageContaining("always");
  }

  @Test
  void rejectsAnUnknownAdvancedMode() {
    assertThatThrownBy(() -> validate("<window label=\"LABEL\" advanced-mode=\"drawer\">" +
      "<advanced><criteria id=\"CrtBlk\" component=\"checkbox\"/></advanced></window>"))
      .hasMessageContaining("drawer");
  }

  @Test
  void rejectsTheAdvancedAttributeOnACriterion() {
    assertThatThrownBy(() -> validate("<window label=\"LABEL\"><criteria id=\"CrtBlk\" component=\"checkbox\" advanced=\"true\"/></window>"))
      .hasMessageContaining("advanced");
  }

  @Test
  void rejectsASearchWindow() {
    assertThatThrownBy(() -> validate("<search-window label=\"LABEL\"><criteria id=\"CrtUsr\" component=\"text\"/></search-window>"))
      .hasMessageContaining("search-window");
  }
}
