package com.almis.awe.service.screen;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.Component;
import com.almis.awe.model.type.AttributeRestrictionType;
import com.almis.awe.model.type.RowType;
import com.almis.awe.service.MenuService;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.PropertyAccessorFactory;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.almis.awe.model.constant.AweConstants.NO_KEY;

/**
 * Validates the screen configuration rules (AweScrCnf) and describes the screen to the rule editors
 */
@Slf4j
public class ScreenConfigurationService extends ServiceConfig {

  // Length of the AweScrCnf value column
  private static final int MAX_VALUE_LENGTH = 60;
  private static final String ATTRIBUTE = "attribute";
  private static final String VALUE = "value";
  private static final String SCREEN = "screen";
  private static final String COMPONENT = "component";
  private static final String USER_ID = "userId";
  private static final String PROFILE_ID = "profileId";
  private static final String SCOPE_TYPE = "scopeType";
  private static final String EVERYONE = "everyone";
  private static final String PROFILE = "profile";
  private static final String PROFILE_NAME = "profileName";
  private static final String MESSAGE_PREFIX = "ERROR_MESSAGE_SCREEN_CONFIGURATION_";

  private final MenuService menuService;

  /**
   * Autowired constructor
   *
   * @param menuService Menu service
   */
  public ScreenConfigurationService(MenuService menuService) {
    this.menuService = menuService;
  }

  /**
   * Validate the rows sent by the screen configuration grid (deleted rows are not validated)
   *
   * @param rowTypes   Row operations
   * @param screens    Screen identifiers
   * @param components Component identifiers
   * @param attributes Attribute names
   * @param values     Values
   * @return Empty service data
   * @throws AWException A rule is not valid or its screen cannot be opened by the user
   */
  public ServiceData validateRules(List<String> rowTypes, List<String> screens, List<String> components, List<String> attributes, List<String> values) throws AWException {
    for (int i = 0; i < rowTypes.size(); i++) {
      requireAccess(screens.get(i));
      if (!RowType.DELETE.name().equals(rowTypes.get(i))) {
        validateRule(screens.get(i), components.get(i), attributes.get(i), values.get(i));
      }
    }
    return new ServiceData();
  }

  /**
   * Validate a rule
   *
   * @param screenId    Screen identifier (empty for all the screens)
   * @param componentId Component identifier
   * @param attribute   Attribute name
   * @param value       Value
   * @throws AWException The rule is not valid
   */
  public void validateRule(String screenId, String componentId, String attribute, String value) throws AWException {
    AttributeRestrictionType restriction = findAttribute(attribute);
    if (screenId != null && !screenId.isEmpty()) {
      validateTarget(restriction, screenId, componentId, attribute);
    }
    validateValue(restriction, attribute, value == null ? "" : value);
  }

  private void validateTarget(AttributeRestrictionType restriction, String screenId, String componentId, String attribute) throws AWException {
    Component component = findComponent(menuService.getScreen(screenId), componentId);
    if (component == null) {
      throw invalid("UNKNOWN_COMPONENT", componentId, screenId);
    }
    if (!restriction.appliesTo(component.getClass())) {
      throw invalid("ATTRIBUTE_NOT_APPLICABLE", attribute, componentId);
    }
  }

  private void validateValue(AttributeRestrictionType restriction, String attribute, String text) throws AWException {
    if (text.length() > MAX_VALUE_LENGTH) {
      throw invalid("VALUE_TOO_LONG", attribute, String.valueOf(MAX_VALUE_LENGTH));
    }
    switch (restriction.getValueType()) {
      case BOOLEAN -> {
        if (!"true".equalsIgnoreCase(text) && !"false".equalsIgnoreCase(text)) {
          throw invalid("INVALID_BOOLEAN", attribute, text);
        }
      }
      case INTEGER -> {
        if (!text.matches("-?\\d{1,9}")) {
          throw invalid("INVALID_INTEGER", attribute, text);
        }
      }
      case ENUM -> {
        if (!restriction.getAllowedValues().contains(text)) {
          throw invalid("INVALID_ENUM", attribute, text, String.join(", ", restriction.getAllowedValues()));
        }
      }
      default -> {
        // Free text
      }
    }
  }

  /**
   * Describe the elements of a screen as a flat tree: one row per element with its path and the path of its parent
   *
   * @param screenId Screen identifier
   * @return Rows: path, parent, position, elementType, id, label, configurable
   * @throws AWException Screen not found
   */
  public ServiceData getScreenStructure(String screenId) throws AWException {
    requireAccess(screenId);
    DataList dataList = new DataList();
    addStructure(menuService.getScreen(screenId), "", dataList);
    dataList.setRecords(dataList.getRows().size());
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Describe the attributes that can be configured in a component
   *
   * @param screenId    Screen identifier
   * @param componentId Component identifier
   * @return Rows: attribute, valueType (BOOLEAN, INTEGER, TEXT, ENUM), allowedValues (comma separated) and value (set in the XML)
   * @throws AWException Screen not found
   */
  public ServiceData getScreenElementAttributes(String screenId, String componentId) throws AWException {
    requireAccess(screenId);
    DataList dataList = new DataList();
    Component component = findComponent(menuService.getScreen(screenId), componentId);
    if (component != null) {
      var accessor = PropertyAccessorFactory.forDirectFieldAccess(component);
      for (AttributeRestrictionType attribute : AttributeRestrictionType.applicableTo(component.getClass())) {
        Object current = accessor.isReadableProperty(attribute.getValue()) ? accessor.getPropertyValue(attribute.getValue()) : null;
        Map<String, CellData> row = new HashMap<>();
        row.put(ATTRIBUTE, new CellData(attribute.getValue()));
        row.put("valueType", new CellData(attribute.getValueType().name()));
        row.put("allowedValues", new CellData(String.join(",", attribute.getAllowedValues())));
        row.put(VALUE, new CellData(current == null ? "" : current.toString()));
        dataList.addRow(row);
      }
    }
    dataList.setRecords(dataList.getRows().size());
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Find the menu option that opens a screen (a screen is loaded by its option, not by its identifier)
   *
   * @param screenId Screen identifier
   * @return One row with the option name, or no row when no option opens the screen
   * @throws AWException Menu not found
   */
  public ServiceData getScreenOption(String screenId) throws AWException {
    DataList dataList = new DataList();
    Option option = menuService.getMenuWithRestrictions().getOptionByScreen(screenId);
    if (option != null) {
      dataList.addRow(new HashMap<>(Map.of("option", new CellData(option.getName()))));
    }
    dataList.setRecords(dataList.getRows().size());
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Describe the active rules of a screen for the personalization overview: who each rule is for and whether it still works
   *
   * @param screenId Screen identifier
   * @return Rows: ruleId, component, attribute, scopeType (everyone, profile, user), profileId, profileName, userId, userName,
   * value, problem (empty, missingElement, invalidAttribute, missingSubject or duplicate) and duplicateGroup
   * @throws AWException Screen not found or query error
   */
  public ServiceData getScreenConfigurationOverview(String screenId) throws AWException {
    requireAccess(screenId);
    Screen screen = menuService.getScreen(screenId);
    ObjectNode parameters = JsonNodeFactory.instance.objectNode().put(SCREEN, screenId);
    DataList rules = (DataList) getBean(QueryService.class).launchPrivateQuery(AweConstants.SCREEN_SUBJECT_CONFIGURATION_QUERY, parameters)
      .getVariableMap().get(AweConstants.ACTION_DATA).getObjectValue();
    return overview(screen, rules);
  }

  ServiceData overview(Screen screen, DataList rules) {
    List<ScreenConfigurationOverview.Rule> stored = rules.getRows().stream().map(row -> new ScreenConfigurationOverview.Rule(
      Integer.parseInt(row.get("id").getStringValue()), row.get(COMPONENT).getStringValue(), row.get(ATTRIBUTE).getStringValue(),
      row.get(USER_ID).getStringValue(), row.get("user").getStringValue(), row.get(PROFILE_ID).getStringValue(),
      row.get(PROFILE_NAME).getStringValue(), row.get(VALUE).getStringValue())).toList();
    DataList dataList = new DataList();
    for (ScreenConfigurationOverview.Row row : ScreenConfigurationOverview.classify(stored,
      component -> findComponent(screen, component) != null, (component, attribute) -> isConfigurable(screen, component, attribute))) {
      ScreenConfigurationOverview.Rule rule = row.rule();
      Map<String, CellData> cells = new HashMap<>();
      cells.put("ruleId", new CellData(rule.id()));
      cells.put(COMPONENT, new CellData(rule.component()));
      cells.put(ATTRIBUTE, new CellData(rule.attribute()));
      cells.put(SCOPE_TYPE, new CellData(scopeOf(rule.userId(), rule.profileId())));
      cells.put(PROFILE_ID, new CellData(rule.profileId()));
      cells.put(PROFILE_NAME, new CellData(rule.profileName()));
      cells.put(USER_ID, new CellData(rule.userId()));
      cells.put("userName", new CellData(rule.userName()));
      cells.put(VALUE, new CellData(rule.value()));
      cells.put("problem", new CellData(row.problem() == null ? "" : problemName(row.problem())));
      cells.put("duplicateGroup", new CellData(row.duplicateGroup() == null ? "" : row.duplicateGroup()));
      dataList.addRow(cells);
    }
    dataList.setRecords(dataList.getRows().size());
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Describe the changes made to the rules of a screen, newest first, with the value before and after and whether each can be undone
   *
   * @param screenId Screen identifier
   * @return Rows: date, operator, action (created, changed, deleted), ruleId, component, attribute, screen, scopeType (everyone, profile,
   * user), profileId, profileName, userId, userName, value, valueBefore, valueAfter and undoable
   * @throws AWException Screen not found or query error
   */
  public ServiceData getScreenConfigurationHistory(String screenId) throws AWException {
    requireAccess(screenId);
    menuService.getScreen(screenId);
    ObjectNode parameters = JsonNodeFactory.instance.objectNode().put(SCREEN, screenId);
    DataList audit = (DataList) getBean(QueryService.class).launchPrivateQuery(AweConstants.SCREEN_CONFIGURATION_HISTORY_QUERY, parameters)
      .getVariableMap().get(AweConstants.ACTION_DATA).getObjectValue();
    return history(audit);
  }

  /**
   * Rules of a screen to export them: who each is for is given by the acronym of the profile or the login of the user, so the
   * rules can be imported where the ids are other. Rules of every screen and rules of a profile or user that no longer exists are left out
   *
   * @param screenId Screen identifier
   * @return Rows: component, attribute, value, scopeType (everyone, profile, user), profile (acronym) and user (login)
   * @throws AWException Screen not found or query error
   */
  public ServiceData getScreenConfigurationExport(String screenId) throws AWException {
    requireAccess(screenId);
    menuService.getScreen(screenId);
    ObjectNode parameters = JsonNodeFactory.instance.objectNode().put(SCREEN, screenId);
    DataList rules = (DataList) getBean(QueryService.class).launchPrivateQuery(AweConstants.SCREEN_SUBJECT_CONFIGURATION_QUERY, parameters)
      .getVariableMap().get(AweConstants.ACTION_DATA).getObjectValue();
    return export(screenId, rules);
  }

  ServiceData export(String screenId, DataList rules) {
    DataList dataList = new DataList();
    for (Map<String, CellData> rule : rules.getRows()) {
      String scopeType = scopeOf(rule.get(USER_ID).getStringValue(), rule.get(PROFILE_ID).getStringValue());
      String profile = PROFILE.equals(scopeType) ? rule.get(PROFILE).getStringValue().trim() : "";
      String user = "user".equals(scopeType) ? rule.get("user").getStringValue().trim() : "";
      // A rule of a profile or user that no longer exists cannot be imported elsewhere
      boolean orphan = (PROFILE.equals(scopeType) && profile.isEmpty()) || ("user".equals(scopeType) && user.isEmpty());
      if (screenId.equals(rule.get(SCREEN).getStringValue()) && !orphan) {
        dataList.addRow(exportRow(rule, scopeType, profile, user));
      }
    }
    dataList.setRecords(dataList.getRows().size());
    return new ServiceData().setDataList(dataList);
  }

  ServiceData history(DataList audit) {
    List<ScreenConfigurationHistory.Entry> entries = audit.getRows().stream().map(row -> new ScreenConfigurationHistory.Entry(
      row.get("date").getStringValue(), row.get("operator").getStringValue().trim(), row.get("action").getStringValue().trim(),
      Integer.parseInt(row.get("id").getStringValue()), row.get(COMPONENT).getStringValue(), row.get(ATTRIBUTE).getStringValue(),
      row.get(SCREEN).getStringValue(), row.get(USER_ID).getStringValue(), row.get("user").getStringValue(),
      row.get(PROFILE_ID).getStringValue(), row.get(PROFILE_NAME).getStringValue(), row.get(VALUE).getStringValue())).toList();
    DataList dataList = new DataList();
    for (ScreenConfigurationHistory.Row row : ScreenConfigurationHistory.build(entries)) {
      ScreenConfigurationHistory.Entry entry = row.entry();
      Map<String, CellData> cells = new HashMap<>();
      cells.put("date", new CellData(entry.date()));
      cells.put("operator", new CellData(entry.operator()));
      cells.put("action", new CellData(row.action()));
      cells.put("ruleId", new CellData(entry.id()));
      cells.put(COMPONENT, new CellData(entry.component()));
      cells.put(ATTRIBUTE, new CellData(entry.attribute()));
      cells.put(SCREEN, new CellData(entry.screen()));
      cells.put(SCOPE_TYPE, new CellData(scopeOf(entry.userId(), entry.profileId())));
      cells.put(PROFILE_ID, new CellData(entry.profileId()));
      cells.put(PROFILE_NAME, new CellData(entry.profileName()));
      cells.put(USER_ID, new CellData(entry.userId()));
      cells.put("userName", new CellData(entry.userName()));
      cells.put(VALUE, new CellData(entry.value()));
      cells.put("valueBefore", new CellData(row.valueBefore()));
      cells.put("valueAfter", new CellData(row.valueAfter()));
      cells.put("undoable", new CellData(row.undoable()));
      dataList.addRow(cells);
    }
    dataList.setRecords(dataList.getRows().size());
    return new ServiceData().setDataList(dataList);
  }

  private static String problemName(ScreenConfigurationOverview.Problem problem) {
    return switch (problem) {
      case MISSING_ELEMENT -> "missingElement";
      case INVALID_ATTRIBUTE -> "invalidAttribute";
      case MISSING_SUBJECT -> "missingSubject";
      case DUPLICATE -> "duplicate";
    };
  }

  private boolean isConfigurable(Screen screen, String componentId, String attribute) {
    Component component = findComponent(screen, componentId);
    try {
      return component != null && AttributeRestrictionType.getEnum(attribute).appliesTo(component.getClass());
    } catch (IllegalArgumentException exc) {
      return false;
    }
  }

  private void addStructure(Element parent, String parentPath, DataList dataList) {
    List<Element> children = parent.getElementList();
    for (int i = 0; i < children.size(); i++) {
      Element element = children.get(i);
      String path = parentPath.isEmpty() ? String.valueOf(i) : parentPath + "." + i;
      dataList.addRow(structureRow(element, path, parentPath, i));
      addStructure(element, path, dataList);
    }
  }

  private Map<String, CellData> structureRow(Element element, String path, String parentPath, int position) {
    String key = element.getElementKey();
    boolean hasId = key != null && !NO_KEY.equals(key);
    Map<String, CellData> row = new HashMap<>();
    row.put("path", new CellData(path));
    row.put("parent", new CellData(parentPath));
    row.put("position", new CellData(position));
    row.put("elementType", new CellData(element.getElementType()));
    row.put("id", new CellData(hasId ? key : ""));
    row.put("label", new CellData(element.getLabel() == null ? "" : getLocale(element.getLabel())));
    row.put("configurable", new CellData(hasId && element instanceof Component));
    return row;
  }

  private static Map<String, CellData> exportRow(Map<String, CellData> rule, String scopeType, String profile, String user) {
    Map<String, CellData> cells = new HashMap<>();
    cells.put(COMPONENT, new CellData(rule.get(COMPONENT).getStringValue()));
    cells.put(ATTRIBUTE, new CellData(rule.get(ATTRIBUTE).getStringValue()));
    cells.put(VALUE, new CellData(rule.get(VALUE).getStringValue()));
    cells.put(SCOPE_TYPE, new CellData(scopeType));
    cells.put(PROFILE, new CellData(profile));
    cells.put("user", new CellData(user));
    return cells;
  }

  private static String scopeOf(String userId, String profileId) {
    return !userId.isEmpty() ? "user" : profileId.isEmpty() ? EVERYONE : PROFILE;
  }

  private Component findComponent(Screen screen, String componentId) {
    return screen.getElementsById(componentId).stream()
      .filter(Component.class::isInstance).map(Component.class::cast).findFirst().orElse(null);
  }

  private AttributeRestrictionType findAttribute(String attribute) throws AWException {
    try {
      return AttributeRestrictionType.getEnum(attribute);
    } catch (IllegalArgumentException exc) {
      throw invalid("UNKNOWN_ATTRIBUTE", attribute);
    }
  }

  /**
   * A screen can only be personalized by who can open it (an empty screen means every screen: the editor permission
   * of the targets is what guards it)
   */
  private void requireAccess(String screenId) throws AWException {
    if (screenId != null && !screenId.isEmpty() && !menuService.isAvailableScreen(screenId)) {
      throw invalid("NO_ACCESS", screenId);
    }
  }

  private AWException invalid(String reason, String... parameters) {
    return new AWException(getLocale("ERROR_TITLE_SCREEN_CONFIGURATION_INVALID"), getLocale(MESSAGE_PREFIX + reason, parameters));
  }
}
