package com.almis.awe.model.type;

import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.component.Component;
import com.almis.awe.model.entities.screen.component.button.AbstractButton;
import com.almis.awe.model.entities.screen.component.criteria.AbstractCriteria;
import com.almis.awe.model.entities.screen.component.grid.AbstractGrid;
import com.almis.awe.model.entities.screen.component.grid.Column;
import com.almis.awe.model.entities.screen.component.pivottable.PivotTable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * AttributeRestrictionType Enumerated
 *
 * Attribute list of components
 *
 *
 * @author Pablo VIDAL - 29/MAY/2013
 */
public enum AttributeRestrictionType {

  // ELEMENT ATTRIBUTES
  STYLE("style", Element.class),
  LABEL("label", Element.class),
  TITLE("title", Element.class),
  TYPE("type", Element.class),
  EXPAND("expand", Element.class),
  HELP("help", Element.class),
  HELP_IMAGE("helpImage", Element.class),

  // COMPONENT ATTRIBUTES
  COMPONENT("componentType", Component.class),
  INITIAL_LOAD("initialLoad", Component.class),
  SERVER_ACTION("serverAction", Component.class),
  TARGET_ACTION("targetAction", Component.class),
  MAX("max", Component.class),
  AUTOLOAD("autoload", Component.class),
  AUTOREFRESH("autorefresh", Component.class),

  // CRITERIA ATTRIBUTES
  REQUIRED("required", AbstractCriteria.class),
  VALUE("value", AbstractCriteria.class),
  SESSION("session", AbstractCriteria.class),
  VARIABLE("variable", AbstractCriteria.class),
  PROPERTY("property", AbstractCriteria.class),
  VALIDATION("validation", AbstractCriteria.class),
  GROUP("group", AbstractCriteria.class),
  NUMBER_FORMAT("numberFormat", AbstractCriteria.class),
  SHOW_SLIDER("showSlider", AbstractCriteria.class),
  DESTINATION("destination", AbstractCriteria.class),
  DEFAULT_VALUE("defaultValue", AbstractCriteria.class),
  CAPITALIZE("capitalize", AbstractCriteria.class),
  VISIBLE("visible", AbstractCriteria.class),
  READONLY("readonly", AbstractCriteria.class),
  CHECKED("checked", AbstractCriteria.class),
  STRICT("strict", AbstractCriteria.class),
  MESSAGE("message", AbstractCriteria.class),
  FORMULE("formule", AbstractCriteria.class),
  PLACEHOLDER("placeholder", AbstractCriteria.class),
  AREA_ROWS("areaRows", AbstractCriteria.class),
  OPTIONAL("optional", AbstractCriteria.class),
  PRINTABLE("printable", AbstractCriteria.class),
  UNIT("unit", AbstractCriteria.class),
  CHECK_EMPTY("checkEmpty", AbstractCriteria.class),
  CHECK_INITIAL("checkInitial", AbstractCriteria.class),
  CHECK_TARGET("checkTarget", AbstractCriteria.class),
  SPECIFIC("specific", AbstractCriteria.class),
  TIMEOUT("timeout", AbstractCriteria.class),
  RESTRICTED_VALUE_LIST("restrictedValueList", AbstractCriteria.class),
  SHOW_WEEKENDS("showWeekends", AbstractCriteria.class),
  SHOW_FUTURE_DATES("showFutureDates", AbstractCriteria.class),
  DATE_FORMAT("dateFormat", AbstractCriteria.class),
  SHOW_TODAY_BUTTON("showTodayButton", AbstractCriteria.class),
  DATE_VIEW_MODE("dateViewMode", AbstractCriteria.class, "days", "months", "years"),

  // COLUMN ATTRIBUTES
  FIELD("field", Column.class),
  WIDTH("width", Column.class),
  CHAR_LENGTH("charLength", Column.class),
  ALIGN("align", Column.class, "left", "center", "right"),
  SORTABLE("sortable", Column.class),
  MOVABLE("movable", Column.class),
  HIDDEN("hidden", Column.class),
  SENDABLE("sendable", Column.class),
  SUMMARY_TYPE("summaryType", Column.class),
  FORMATTER("formatter", Column.class),
  FORMAT_OPTIONS("formatOptions", Column.class),
  FROZEN("frozen", Column.class),
  POSITION("position", Column.class),

  // BUTTON ATTRIBUTES
  BROWSER_ACTION("browserAction", AbstractButton.class),
  BUTTON_TYPE("buttonType", AbstractButton.class),
  CANCEL("cancel", AbstractButton.class),

  // GRID ATTRIBUTES
  TOTALIZE("totalize", AbstractGrid.class),
  SHOW_TOTALS("showTotals", AbstractGrid.class),
  TREEGRID("treegrid", AbstractGrid.class),
  EDITABLE("editable", AbstractGrid.class),
  MULTISELECT("multiselect", AbstractGrid.class),
  EXPAND_COLUMN("expandColumn", AbstractGrid.class),
  LOAD_ALL("loadAll", AbstractGrid.class),
  SEND_ALL("sendAll", AbstractGrid.class),
  TREE_ID("treeId", AbstractGrid.class),
  TREE_PARENT("treeParent", AbstractGrid.class),
  TREE_LEAF("treeLeaf", AbstractGrid.class),
  PAGINATION_DISABLED("disablePagination", AbstractGrid.class),
  PAGER_VALUES("pagerValues", AbstractGrid.class),

  // PIVOT ATTRIBUTES
  TOTAL_COLUMN_PLACEMENT("totalColumnPlacement", PivotTable.class, "left", "right"),
  TOTAL_ROW_PLACEMENT("totalRowPlacement", PivotTable.class, "top", "bottom"),
  RENDERER("renderer", PivotTable.class),
  AGGREGATOR("aggregator", PivotTable.class),
  AGGREGATION_FIELD("aggregationField", PivotTable.class),
  SORT_METHOD("sortMethod", PivotTable.class),
  COLS("cols", PivotTable.class),
  ROWS("rows", PivotTable.class),
  DECIMAL_NUMBERS("decimalNumbers", PivotTable.class),
  THOUSAND_SEPARATOR("thousandSeparator", PivotTable.class),
  DECIMAL_SEPARATOR("decimalSeparator", PivotTable.class);

  // Parameter VALUE
  private final String value;
  private final Class<?> componentClass;
  private final List<String> allowedValues;

  // Constructor. Allowed values are set only for attributes with a closed set of values (see the XSD enumerations)
  AttributeRestrictionType(String value, Class<?> componentClass, String... allowedValues) {
    this.value = value;
    this.componentClass = componentClass;
    this.allowedValues = List.of(allowedValues);
  }

  /**
   * Retrieve the attributes that apply to a component class
   *
   * @param componentClass Component class
   * @return Applicable attributes
   */
  public static List<AttributeRestrictionType> applicableTo(Class<?> componentClass) {
    return Arrays.stream(values()).filter(attribute -> attribute.appliesTo(componentClass)).toList();
  }

  /**
   * Check if the attribute belongs to a component class
   *
   * @param componentClassToCheck Component class
   * @return The attribute applies to the class
   */
  public boolean appliesTo(Class<?> componentClassToCheck) {
    return componentClass.isAssignableFrom(componentClassToCheck);
  }

  /**
   * Retrieve the type of the attribute value, taken from the type of the model field with the attribute name.
   * Attributes with no field of that name (required) are texts, except the boolean ones set by hand
   *
   * @return Value type
   */
  public AttributeValueType getValueType() {
    if (!allowedValues.isEmpty()) {
      return AttributeValueType.ENUM;
    }
    Class<?> fieldType = this == REQUIRED ? Boolean.class : fieldType();
    if (Boolean.class.equals(fieldType) || boolean.class.equals(fieldType)) {
      return AttributeValueType.BOOLEAN;
    }
    if (Integer.class.equals(fieldType) || int.class.equals(fieldType)) {
      return AttributeValueType.INTEGER;
    }
    return AttributeValueType.TEXT;
  }

  /**
   * Retrieve the allowed values of the attribute
   *
   * @return Allowed values (empty if the attribute has no closed set of values)
   */
  public List<String> getAllowedValues() {
    return allowedValues;
  }

  private Class<?> fieldType() {
    for (Class<?> type = componentClass; type != null; type = type.getSuperclass()) {
      for (Field field : type.getDeclaredFields()) {
        if (field.getName().equals(value)) {
          return field.getType();
        }
      }
    }
    return String.class;
  }

  /**
   * Retrieve enumerated value
   *
   * @param value Enumerated value
   * @return Enumerated
   */
  public static AttributeRestrictionType getEnum(String value) {
    for (AttributeRestrictionType enumerated : values()) {
      if (enumerated.value.equalsIgnoreCase(value)) {
        return enumerated;
      }
    }
    throw new IllegalArgumentException();
  }

  /**
   * Retrieve enumerated component class
   * @return Component class
   */
  public Class<?> getComponentClass() {
    return componentClass;
  }

  /**
   * Retrieve enumerated value
   * @return Value
   */
  public String getValue() {
    return value;
  }

  /**
   * Override toString method to return PROPERTY VALUE
   *
   * @return Property VALUE (if defined)
   */
  @Override
  public String toString() {
    return getValue();
  }
}
