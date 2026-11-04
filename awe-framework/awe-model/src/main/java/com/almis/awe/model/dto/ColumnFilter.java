package com.almis.awe.model.dto;

import com.almis.awe.model.type.ColumnFilterOperator;
import com.almis.awe.model.type.ColumnFilterType;
import com.almis.awe.model.type.UnionType;
import com.almis.awe.model.util.data.date.DateFormats;
import com.almis.awe.model.util.data.date.DateTimeParser;
import lombok.AllArgsConstructor;
import lombok.Data;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.exc.JsonNodeException;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Filter of a grid column, received in the {@code columnFilters} request parameter (issue #912):
 * {@code {"id", "fields"?, "type", "values"?, "empty"?, "union"?, "conditions"?}}.
 * <p>
 * A row matches when any of the fields matches (OR). In a field, the values (plus the empty values when
 * {@code empty} is true) and the conditions (at most two, joined by {@code union}) must all match.
 */
@Data
@AllArgsConstructor
public class ColumnFilter {

  // Digits of the integer and of the decimal part of a number value, as the widest database number (38)
  private static final int MAX_NUMBER_DIGITS = 38;

  private static final int MAX_CONDITIONS = 2;

  private static final String UNION_KEY = "union";

  private String id;
  private List<String> fields;
  private ColumnFilterType type;
  private List<JsonNode> values;
  private boolean empty;
  private UnionType union;
  private List<ColumnFilterCondition> conditions;

  /**
   * Whether the filter keeps every row: it has no values, no empty flag and no conditions
   *
   * @return The filter adds no condition
   */
  public boolean isNoop() {
    return values == null && !empty && conditions.isEmpty();
  }

  /**
   * Write the filter as an entry of the column filters of the request, in its compact form: the id, the fields and the
   * type, then only the values, the empty flag and the conditions (with their union) it has
   *
   * @return Column filter entry
   */
  public ObjectNode toJson() {
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("id", id);
    ArrayNode fieldList = node.putArray("fields");
    fields.forEach(fieldList::add);
    node.put("type", type.getCode());
    if (values != null) {
      ArrayNode valueList = node.putArray("values");
      values.forEach(valueList::add);
    }
    if (empty) {
      node.put("empty", true);
    }
    if (!conditions.isEmpty()) {
      node.put(UNION_KEY, union == UnionType.OR ? "or" : "and");
      ArrayNode conditionList = node.putArray("conditions");
      for (ColumnFilterCondition condition : conditions) {
        ObjectNode conditionNode = conditionList.addObject();
        conditionNode.put("operator", condition.getOperator().getCode());
        if (condition.getValue() != null) {
          conditionNode.put("value", condition.getValue());
        }
        if (condition.getTo() != null) {
          conditionNode.put("to", condition.getTo());
        }
      }
    }
    return node;
  }

  /**
   * Parse the column filters of the request
   *
   * @param node Column filters (an array)
   * @return Column filters
   * @throws IllegalArgumentException The column filters are not valid, with the reason as message
   */
  public static List<ColumnFilter> fromJson(JsonNode node) {
    if (node == null || node.isNull() || node.isString() && node.stringValue().isBlank()) {
      return new ArrayList<>();
    }
    if (!node.isArray()) {
      throw new IllegalArgumentException("columnFilters must be an array");
    }
    List<ColumnFilter> filters = new ArrayList<>();
    for (JsonNode entry : node) {
      filters.add(parseFilter(entry));
    }
    return filters;
  }

  private static ColumnFilter parseFilter(JsonNode entry) {
    if (!entry.isObject()) {
      throw new IllegalArgumentException("a column filter must be an object: " + entry);
    }
    String id = getText(entry, "id");
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("a column filter needs an id: " + entry);
    }
    ColumnFilterType type = ColumnFilterType.fromCode(getText(entry, "type"))
      .orElseThrow(() -> new IllegalArgumentException("unknown type in column filter " + id));
    String unionCode = entry.has(UNION_KEY) ? getText(entry, UNION_KEY) : "and";
    UnionType union = switch (unionCode == null ? "" : unionCode) {
      case "and" -> UnionType.AND;
      case "or" -> UnionType.OR;
      default -> throw new IllegalArgumentException("unknown union in column filter " + id);
    };
    JsonNode empty = entry.get("empty");
    if (empty != null && !empty.isNull() && !empty.isBoolean()) {
      throw new IllegalArgumentException("empty must be a boolean in column filter " + id);
    }
    return new ColumnFilter(id, parseFields(entry.get("fields"), id), type, parseValues(entry.get("values"), id),
      empty != null && empty.isBoolean() && empty.booleanValue(), union, parseConditions(entry.get("conditions"), type, id));
  }

  static List<String> parseFields(JsonNode node, String id) {
    if (node == null || node.isNull()) {
      return List.of(id);
    }
    if (!node.isArray() || node.isEmpty()) {
      throw new IllegalArgumentException("fields must be a list of field ids in column filter " + id);
    }
    List<String> fields = new ArrayList<>();
    for (JsonNode field : node) {
      if (!field.isString() || field.stringValue().isBlank()) {
        throw new IllegalArgumentException("fields must be a list of field ids in column filter " + id);
      }
      fields.add(field.stringValue());
    }
    return fields;
  }

  private static List<JsonNode> parseValues(JsonNode node, String id) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (!node.isArray()) {
      throw new IllegalArgumentException("values must be a list in column filter " + id);
    }
    List<JsonNode> values = new ArrayList<>();
    for (JsonNode value : node) {
      if (!value.isValueNode()) {
        throw new IllegalArgumentException("values must be plain values in column filter " + id);
      }
      values.add(value);
    }
    return values;
  }

  private static List<ColumnFilterCondition> parseConditions(JsonNode node, ColumnFilterType type, String id) {
    List<ColumnFilterCondition> conditions = new ArrayList<>();
    if (node == null || node.isNull()) {
      return conditions;
    }
    if (!node.isArray()) {
      throw new IllegalArgumentException("conditions must be a list in column filter " + id);
    }
    if (node.size() > MAX_CONDITIONS) {
      throw new IllegalArgumentException("more than " + MAX_CONDITIONS + " conditions in column filter " + id);
    }
    for (JsonNode condition : node) {
      conditions.add(parseCondition(condition, type, id));
    }
    return conditions;
  }

  private static ColumnFilterCondition parseCondition(JsonNode node, ColumnFilterType type, String id) {
    if (!node.isObject()) {
      throw new IllegalArgumentException("a condition must be an object in column filter " + id);
    }
    ColumnFilterOperator operator = ColumnFilterOperator.fromCode(getText(node, "operator"))
      .filter(candidate -> candidate.appliesTo(type))
      .orElseThrow(() -> new IllegalArgumentException("unknown operator for type " + type.getCode() + " in column filter " + id));
    if (!operator.isValueRequired()) {
      return new ColumnFilterCondition(operator, null, null);
    }
    String value = parseValue(node.get("value"), type, id);
    String to = operator == ColumnFilterOperator.BETWEEN ? parseValue(node.get("to"), type, id) : null;
    return new ColumnFilterCondition(operator, value, to);
  }

  private static String parseValue(JsonNode node, ColumnFilterType type, String id) {
    if (node == null || !(node.isString() || node.isNumber()) || node.asString("").isBlank()) {
      throw new IllegalArgumentException("a condition needs a value in column filter " + id);
    }
    String value = node.asString("");
    try {
      return switch (type) {
        case NUMBER -> getPlainNumber(node.isNumber() ? node.decimalValue() : new BigDecimal(value.trim()));
        case DATE -> DateFormats.WEB_DATE.format(DateTimeParser.parseWebDate(value.trim()));
        default -> value;
      };
    } catch (NumberFormatException | DateTimeParseException | JsonNodeException exc) {
      throw new IllegalArgumentException("the value is not a " + type.getCode().toLowerCase(Locale.ROOT) + " in column filter " + id, exc);
    }
  }

  /**
   * Retrieve a number as plain text, rejecting the numbers with more digits than a database number holds: an exponent
   * such as 1e2000000000 would otherwise expand into a huge text
   *
   * @param number Number
   * @return Plain text of the number
   */
  private static String getPlainNumber(BigDecimal number) {
    if (number.precision() - number.scale() > MAX_NUMBER_DIGITS || number.scale() > MAX_NUMBER_DIGITS) {
      throw new NumberFormatException("too many digits");
    }
    return number.toPlainString();
  }

  static String getText(JsonNode node, String name) {
    JsonNode value = node.get(name);
    return value != null && value.isString() ? value.stringValue() : null;
  }
}
