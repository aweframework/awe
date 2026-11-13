package com.almis.awe.service.screen;

import com.almis.awe.exception.AWENotFoundException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.criteria.Criteria;
import com.almis.awe.model.entities.screen.component.grid.Column;
import com.almis.awe.service.MenuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScreenConfigurationServiceTest {

  private MenuService menuService;
  private ScreenConfigurationService service;

  @BeforeEach
  void setUp() throws AWException {
    // Locales answer their own key, so a test can tell which rule rejected the value
    AweElements elements = mock(AweElements.class, invocation ->
      "getLocaleWithLanguage".equals(invocation.getMethod().getName()) ? invocation.getArgument(0) : null);
    ApplicationContext context = mock(ApplicationContext.class);
    when(context.getBean(AweElements.class)).thenReturn(elements);

    Criteria criteria = new Criteria();
    criteria.setId("Crt");
    criteria.setLabel("LABEL_CRT");
    criteria.setPlaceholder("Type here");
    Column column = new Column();
    column.setName("Col");
    column.setAlign("left");
    criteria.addElement(column);
    Screen screen = new Screen();
    screen.addElement(criteria);

    menuService = mock(MenuService.class);
    when(menuService.getScreen("Scr")).thenReturn(screen);
    when(menuService.isAvailableScreen("Scr")).thenReturn(true);
    service = new ScreenConfigurationService(menuService);
    service.setApplicationContext(context);
  }

  private void assertRejected(String screen, String component, String attribute, String value, String localeKey) {
    assertThatThrownBy(() -> service.validateRule(screen, component, attribute, value))
      .isInstanceOf(AWException.class)
      .hasMessage(localeKey);
  }

  @Test
  void validRulesAreAccepted() throws AWException {
    service.validateRule("Scr", "Crt", "placeholder", "Any text");
    service.validateRule("Scr", "Col", "align", "right");
    // All screens: only the attribute and its value are checked
    service.validateRule("", "Anything", "capitalize", "true");
  }

  @Test
  void unknownScreenIsRejected() throws AWException {
    when(menuService.getScreen("Nope")).thenThrow(new AWENotFoundException("title", "not defined"));

    assertThatThrownBy(() -> service.validateRule("Nope", "Crt", "placeholder", "x")).isInstanceOf(AWENotFoundException.class);
  }

  @Test
  void componentOutsideTheScreenIsRejected() {
    assertRejected("Scr", "Other", "placeholder", "x", "ERROR_MESSAGE_SCREEN_CONFIGURATION_UNKNOWN_COMPONENT");
  }

  @Test
  void unknownAttributeIsRejected() {
    assertRejected("Scr", "Crt", "nope", "x", "ERROR_MESSAGE_SCREEN_CONFIGURATION_UNKNOWN_ATTRIBUTE");
  }

  @Test
  void attributeOfAnotherKindOfComponentIsRejected() {
    assertRejected("Scr", "Crt", "totalize", "x", "ERROR_MESSAGE_SCREEN_CONFIGURATION_ATTRIBUTE_NOT_APPLICABLE");
  }

  @Test
  void valueMustMatchTheTypeOfTheAttribute() {
    assertRejected("Scr", "Crt", "capitalize", "yes", "ERROR_MESSAGE_SCREEN_CONFIGURATION_INVALID_BOOLEAN");
    assertRejected("Scr", "Crt", "timeout", "1.5", "ERROR_MESSAGE_SCREEN_CONFIGURATION_INVALID_INTEGER");
    assertRejected("Scr", "Col", "align", "middle", "ERROR_MESSAGE_SCREEN_CONFIGURATION_INVALID_ENUM");
  }

  @Test
  void valueLongerThanTheStoredColumnIsRejected() {
    assertRejected("Scr", "Crt", "placeholder", "x".repeat(61), "ERROR_MESSAGE_SCREEN_CONFIGURATION_VALUE_TOO_LONG");
  }

  @Test
  void deletedRowsOfAGridAreNotValidated() throws AWException {
    service.validateRules(List.of("DELETE"), List.of("Scr"), List.of("Nope"), List.of("placeholder"), List.of("x"));

    assertThatThrownBy(() -> service.validateRules(List.of("DELETE", "INSERT"), List.of("Scr", "Scr"), List.of("Nope", "Nope"), List.of("placeholder", "placeholder"), List.of("x", "x")))
      .isInstanceOf(AWException.class).hasMessage("ERROR_MESSAGE_SCREEN_CONFIGURATION_UNKNOWN_COMPONENT");
  }

  @Test
  void aScreenTheUserCannotOpenCannotBePersonalized() {
    String refused = "ERROR_MESSAGE_SCREEN_CONFIGURATION_NO_ACCESS";
    assertThatThrownBy(() -> service.validateRules(List.of("INSERT"), List.of("Hidden"), List.of("Crt"),
      List.of("placeholder"), List.of("x"))).isInstanceOf(AWException.class).hasMessage(refused);
    // Deleting the rules of a screen is a personalization too
    assertThatThrownBy(() -> service.validateRules(List.of("DELETE"), List.of("Hidden"), List.of("Crt"),
      List.of("placeholder"), List.of("x"))).isInstanceOf(AWException.class).hasMessage(refused);
    assertThatThrownBy(() -> service.getScreenStructure("Hidden")).hasMessage(refused);
    assertThatThrownBy(() -> service.getScreenElementAttributes("Hidden", "Crt")).hasMessage(refused);
    assertThatThrownBy(() -> service.getScreenConfigurationOverview("Hidden")).hasMessage(refused);
    assertThatThrownBy(() -> service.getScreenConfigurationHistory("Hidden")).hasMessage(refused);
    assertThatThrownBy(() -> service.getScreenConfigurationExport("Hidden")).hasMessage(refused);
  }

  @Test
  void rulesOfAllTheScreensNeedNoScreenAccess() throws AWException {
    service.validateRules(List.of("INSERT", "DELETE", "INSERT"), List.of("", "", "Scr"), List.of("A", "A", "Crt"),
      List.of("capitalize", "capitalize", "placeholder"), List.of("true", "true", "x"));
  }

  @Test
  void screenStructureIsAFlatTreeOfElementsWithTheirParent() throws AWException {
    ServiceData data = service.getScreenStructure("Scr");

    List<Map<String, CellData>> rows = data.getDataList().getRows();
    assertThat(rows).hasSize(2);
    assertThat(rows.get(0)).containsEntry("path", new CellData("0")).containsEntry("parent", new CellData(""))
      .containsEntry("id", new CellData("Crt")).containsEntry("elementType", new CellData("Criteria"))
      .containsEntry("label", new CellData("LABEL_CRT"));
    assertThat(rows.get(1)).containsEntry("path", new CellData("0.0")).containsEntry("parent", new CellData("0"))
      .containsEntry("id", new CellData("Col"));
  }

  @Test
  void elementAttributesCarryTheirTypeAllowedValuesAndCurrentValue() throws AWException {
    List<Map<String, CellData>> criteriaRows = service.getScreenElementAttributes("Scr", "Crt").getDataList().getRows();
    List<Map<String, CellData>> columnRows = service.getScreenElementAttributes("Scr", "Col").getDataList().getRows();

    assertThat(criteriaRows).anySatisfy(row -> assertThat(row)
      .containsEntry("attribute", new CellData("placeholder")).containsEntry("valueType", new CellData("TEXT"))
      .containsEntry("value", new CellData("Type here")));
    assertThat(criteriaRows).noneSatisfy(row -> assertThat(row).containsEntry("attribute", new CellData("align")));
    assertThat(columnRows).anySatisfy(row -> assertThat(row)
      .containsEntry("attribute", new CellData("align")).containsEntry("valueType", new CellData("ENUM"))
      .containsEntry("allowedValues", new CellData("left,center,right")).containsEntry("value", new CellData("left")));
  }

  @Test
  void screenOptionIsTheMenuOptionThatOpensTheScreen() throws AWException {
    Option parent = new Option();
    parent.setName("tools");
    Option option = new Option();
    option.setName("users");
    option.setScreen("Scr");
    parent.addElement(option);
    Menu menu = new Menu();
    menu.addElement(parent);
    when(menuService.getMenuWithRestrictions()).thenReturn(menu);

    assertThat(service.getScreenOption("Scr").getDataList().getRows()).singleElement()
      .satisfies(row -> assertThat(row).containsEntry("option", new CellData("users")));
    // A screen that no option opens has no row
    assertThat(service.getScreenOption("Other").getDataList().getRows()).isEmpty();
  }

  private static Map<String, CellData> storedRule(int id, String component, String attribute, String profileId, String profileName) {
    return new java.util.HashMap<>(Map.of("id", new CellData(id), "component", new CellData(component), "attribute", new CellData(attribute),
      "userId", new CellData(""), "user", new CellData(""), "profileId", new CellData(profileId), "profileName", new CellData(profileName),
      "value", new CellData("x")));
  }

  @Test
  void overviewFlagsTheRulesThatNoLongerWorkAgainstTheRealScreen() throws AWException {
    DataList rules = new DataList();
    rules.addRow(storedRule(1, "Crt", "placeholder", "", ""));
    rules.addRow(storedRule(2, "Other", "placeholder", "", ""));
    rules.addRow(storedRule(3, "Crt", "align", "", ""));
    rules.addRow(storedRule(4, "Crt", "label", "7", ""));

    List<Map<String, CellData>> rows = service.overview(menuService.getScreen("Scr"), rules).getDataList().getRows();

    assertThat(rows).extracting(row -> row.get("problem").getStringValue())
      .containsExactly("", "missingElement", "invalidAttribute", "missingSubject");
    assertThat(rows.get(0)).containsEntry("scopeType", new CellData("everyone")).containsEntry("ruleId", new CellData(1));
    assertThat(rows.get(3)).containsEntry("scopeType", new CellData("profile")).containsEntry("profileId", new CellData("7"));
  }

  private static Map<String, CellData> auditRow(String action, int id, String value) {
    return new java.util.HashMap<>(Map.of("date", new CellData("09/11/2026 10:30:00"), "operator", new CellData("ana "), "action", new CellData(action),
      "id", new CellData(id), "component", new CellData("Crt"), "attribute", new CellData("label"), "screen", new CellData("Scr"),
      "userId", new CellData(""), "user", new CellData("")));
  }

  @Test
  void historyDescribesTheAuditRowsNewestFirst() {
    DataList audit = new DataList();
    audit.addRow(withProfile(auditRow("I", 1, "a"), "a"));
    audit.addRow(withProfile(auditRow("U", 1, "b"), "b"));

    List<Map<String, CellData>> rows = service.history(audit).getDataList().getRows();

    assertThat(rows).extracting(row -> row.get("action").getStringValue()).containsExactly("changed", "created");
    assertThat(rows.get(0)).containsEntry("valueBefore", new CellData("a")).containsEntry("valueAfter", new CellData("b"))
      .containsEntry("undoable", new CellData(true)).containsEntry("operator", new CellData("ana")).containsEntry("scopeType", new CellData("profile"))
      .containsEntry("profileName", new CellData("Admin"));
    assertThat(rows.get(1)).containsEntry("undoable", new CellData(false));
  }

  private static Map<String, CellData> withProfile(Map<String, CellData> row, String value) {
    row.put("profileId", new CellData("7"));
    row.put("profileName", new CellData("Admin"));
    row.put("value", new CellData(value));
    return row;
  }

  @Test
  void exportKeepsTheRulesOfTheScreenWithTheAcronymAndLoginOfTheirSubject() {
    DataList rules = new DataList();
    rules.addRow(exportedRule(1, "Scr", "", "", ""));
    rules.addRow(exportedRule(2, "Scr", "7", "ADM", ""));
    rules.addRow(exportedRule(3, "Scr", "", "", "ana "));
    // Of every screen, of a profile that no longer exists
    rules.addRow(exportedRule(4, null, "", "", ""));
    rules.addRow(exportedRule(5, "Scr", "9", "", ""));

    List<Map<String, CellData>> rows = service.export("Scr", rules).getDataList().getRows();

    assertThat(rows).hasSize(3);
    assertThat(rows.get(0)).containsEntry("scopeType", new CellData("everyone")).containsEntry("component", new CellData("Crt"));
    assertThat(rows.get(1)).containsEntry("scopeType", new CellData("profile")).containsEntry("profile", new CellData("ADM"));
    assertThat(rows.get(2)).containsEntry("scopeType", new CellData("user")).containsEntry("user", new CellData("ana"));
  }

  private static Map<String, CellData> exportedRule(int id, String screen, String profileId, String profile, String user) {
    return new java.util.HashMap<>(Map.of("id", new CellData(id), "screen", new CellData(screen == null ? "" : screen), "component", new CellData("Crt"),
      "attribute", new CellData("label"), "value", new CellData("x"), "profileId", new CellData(profileId), "profile", new CellData(profile),
      "userId", new CellData(user.isEmpty() ? "" : "3"), "user", new CellData(user)));
  }
}
