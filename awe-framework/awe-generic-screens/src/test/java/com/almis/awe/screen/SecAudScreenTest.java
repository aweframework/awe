package com.almis.awe.screen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.almis.awe.screen.AccessAdminDescriptor.AWE;
import static com.almis.awe.screen.AccessAdminDescriptor.actions;
import static com.almis.awe.screen.AccessAdminDescriptor.attributes;
import static com.almis.awe.screen.AccessAdminDescriptor.byId;
import static com.almis.awe.screen.AccessAdminDescriptor.elements;
import static com.almis.awe.screen.AccessAdminDescriptor.load;
import static com.almis.awe.screen.AccessAdminDescriptor.text;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The security audit screen (SecAud) holds the filters in the grid itself: a text search, an outcome quick filter and
 * the autofilter of every column, all of them on the server. The date range is the only criterion outside the grid, in
 * one row above the events. The page has an Events and a Statistics tab, the four summary cards fill from the range,
 * and the detail of an event opens in a side panel filled from the selected row. Every text is translated.
 */
@DisplayName("Security audit screen")
class SecAudScreenTest {

  private static final String SCREEN = "screen/security/SecAud.xml";
  private static final String GRID = "GrdSecAud";

  // The screen schema is validated by the xml-maven-plugin of the module build

  @Test
  @DisplayName("Two page tabs: Events with the cards and the grid, Statistics with the four charts")
  void twoPageTabs() throws Exception {
    Document screen = load(AWE + SCREEN);
    assertThat(attributes(elements(byId(screen, "TabSecAud"), "tabcontainer"), "id"))
      .containsExactly("TabSecAudEvt", "TabSecAudSta");

    // The events tab carries no criteria: the time range lives in the picker, above the tab
    assertThat(elements(byId(screen, "TabSecAudEvt"), "criteria")).isEmpty();
    assertThat(attributes(elements(byId(screen, "PopRng"), "criteria"), "id")).containsExactly("CrtFro", "CrtTo");

    // Four summary cards of the range, one per total query
    assertThat(attributes(elements(byId(screen, "TabSecAudEvt"), "card"), "target-action"))
      .containsExactly("SecAudTotEvt", "SecAudTotKo", "SecAudTotUsr", "SecAudTotLck");

    // The statistics tab holds four views of the range: activity per day, failures by cause, channels and hours
    assertThat(attributes(elements(byId(screen, "TabSecAudSta"), "chart"), "id"))
      .containsExactly("ChrSecAudAct", "ChrSecAudCau", "ChrSecAudChn", "ChrSecAudHou");
  }

  @Test
  @DisplayName("The grid holds the filters: a search, an outcome quick filter with counts and a funnel per column")
  void gridHoldsTheFilters() throws Exception {
    Document screen = load(AWE + SCREEN);
    Element grid = byId(screen, GRID);
    assertThat(grid.getAttribute("target-action")).isEqualTo("SecAudEvtLst");

    // Text search of user, address and detail, and quick filter of the outcome, counted with the rest of the filters
    assertThat(byId(screen, "CrtUsrTxt").getTagName()).isEqualTo("search");
    Element quickFilter = byId(screen, "CrtRes");
    assertThat(quickFilter.getTagName()).isEqualTo("quick-filter");
    assertThat(quickFilter.getAttribute("target-action")).isEqualTo("SecAudResCnt");

    // Every funnel filters on the server: values for the text columns, date for the range (through its day)
    List<Element> columns = elements(grid, "column");
    assertThat(columns.stream().filter(column -> column.hasAttribute("filter"))
      .map(column -> column.getAttribute("name") + ":" + column.getAttribute("filter")))
      .containsExactlyInAnyOrder("EvtDat:date", "TypTxt:values", "ResTxt:values", "UsrNam:values", "CauTxt:values",
        "ChnTxt:values", "IpAdrTxt:values");

    // The view button column of the grid opens the side panel
    assertThat(actions(byId(screen, "ViwBtn"))).containsExactly("dialog:PnlSecAud");
  }

  @Test
  @DisplayName("The side panel shows one read only criterion per detail field, read from the selected row")
  void sidePanelOfTheSelectedEvent() throws Exception {
    Document screen = load(AWE + SCREEN);
    Element panel = byId(screen, "PnlSecAud");
    assertThat(panel.getAttribute("header-grid")).isEqualTo(GRID);

    List<Element> criteria = elements(panel, "criteria");
    assertThat(attributes(criteria, "id")).containsExactly("ViwDat", "ViwUsr", "ViwCau", "ViwChn", "ViwTen", "ViwIde",
      "ViwDet", "ViwIp", "ViwCor", "ViwSes", "ViwAgt");
    for (Element criterion : criteria) {
      assertThat(criterion.getAttribute("component")).as(criterion.getAttribute("id")).isEqualTo("text-view");
      Element origin = elements(criterion, "dependency-element").getFirst();
      assertThat(origin.getAttribute("id")).as(criterion.getAttribute("id")).isEqualTo(GRID);
      assertThat(origin.getAttribute("attribute")).as(criterion.getAttribute("id")).isEqualTo("selectedRowValue");
    }
    // The optional values come from never-null columns (a dash when empty): a null would keep the value of the row
    // opened before
    assertThat(criteria.stream().map(criterion -> elements(criterion, "dependency-element").getFirst().getAttribute("column")))
      .containsExactly("EvtDat", "UsrVal", "CauVal", "ChnTxt", "TenVal", "IdeSecAud", "DetVal", "IpVal", "CorVal",
        "SesVal", "AgtVal");
  }

  @ParameterizedTest
  @ValueSource(strings = {"Locale-en-GB.xml", "Locale-es-ES.xml", "Locale-fr-FR.xml"})
  @DisplayName("Every text of the screen, its tabs and its queries is translated")
  void textsAreTranslated(String locale) throws Exception {
    Set<String> keys = new LinkedHashSet<>();
    Matcher matcher = Pattern.compile("\"([A-Z][A-Z0-9_]+)\"").matcher(text(AWE + SCREEN));
    while (matcher.find()) {
      keys.add(matcher.group(1));
    }
    // The tab labels come from the enumerated group of the tabs
    Element group = elements(load(AWE + "global/Enumerated.xml").getDocumentElement(), "group").stream()
      .filter(element -> "SecAudTabs".equals(element.getAttribute("id"))).findFirst().orElseThrow();
    assertThat(attributes(elements(group, "option"), "value")).containsExactly("TabSecAudEvt", "TabSecAudSta");
    keys.addAll(attributes(elements(group, "option"), "label"));

    Set<String> defined = new LinkedHashSet<>(attributes(
      elements(load(AWE + "locale/" + locale).getDocumentElement(), "locale"), "name"));

    assertThat(keys).hasSizeGreaterThan(40);
    assertThat(defined).as(locale).containsAll(keys);
  }
}
