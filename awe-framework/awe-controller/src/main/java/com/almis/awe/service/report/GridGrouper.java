package com.almis.awe.service.report;

import com.almis.ade.api.bean.input.DataBean;
import com.almis.ade.api.enumerate.ColumnType;
import com.almis.awe.model.constant.AweConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Groups the data rows of a grid as the clients show them (#833): for each group of a level, its group row (label,
 * count and aggregates), its content (sub-groups, or the data rows on the last level) and, when subtotals are on, the
 * subtotal row that closes it. ADE does not compute groups: the rows are marked with a style label (GROUP1..GROUPn,
 * SUBTOTAL) and the report styles them. Every group is printed expanded.
 */
final class GridGrouper {

  static final String GROUP_STYLE_PREFIX = "GROUP";
  static final String SUBTOTAL_STYLE = "SUBTOTAL";

  // Same aggregates as the clients: the other summary types are not aggregated
  private static final List<String> AGGREGATES = List.of("sum", "avg", "max", "min");
  private static final int LABEL_SCALE = 2;
  private static final String INDENT = "\u00A0\u00A0";

  /**
   * What the grouping needs
   *
   * @param levels         Fields that group the rows, one level each, in order
   * @param summaryTypes   Summary type by printed field (sum, avg, max, min)
   * @param labelField     Printed field where the group label goes (the first plain text column), null for none
   * @param subtotals      Close each group with a subtotal row
   * @param emptyLabel     Label of the group of an empty value
   * @param subtotalLabel  Text before the label of a subtotal
   * @param typeOf         Column type of a field, null when it is not known
   */
  record Spec(List<String> levels, Map<String, String> summaryTypes, String labelField, boolean subtotals,
              String emptyLabel, String subtotalLabel, Function<String, ColumnType> typeOf) {
  }

  private final Spec spec;
  private final List<String> fields;

  GridGrouper(Spec spec, List<String> fields) {
    this.spec = spec;
    this.fields = fields;
  }

  /**
   * Check whether an aggregate is supported
   *
   * @param summaryType Summary type of a column
   * @return Summary type is aggregated
   */
  static boolean isAggregate(String summaryType) {
    return summaryType != null && AGGREGATES.contains(summaryType.trim().toLowerCase(Locale.ROOT));
  }

  /**
   * Build the rows of the grouped grid
   *
   * @param rows Data rows (their cells are the beans of the report data)
   * @return Group rows, data rows (the same lists) and subtotal rows
   */
  List<List<Object>> group(List<List<Object>> rows) {
    List<List<Object>> grouped = new ArrayList<>();
    addGroups(rows, 0, grouped);
    return grouped;
  }

  private void addGroups(List<List<Object>> rows, int depth, List<List<Object>> out) {
    String field = spec.levels().get(depth);
    Map<String, List<List<Object>>> byValue = new LinkedHashMap<>();
    Map<String, String> labels = new LinkedHashMap<>();
    for (List<Object> row : rows) {
      String value = text(cell(row, field), true);
      byValue.computeIfAbsent(value, key -> new ArrayList<>()).add(row);
      labels.putIfAbsent(value, value.isEmpty() ? spec.emptyLabel() : labelOrValue(cell(row, field)));
    }
    List<String> values = new ArrayList<>(byValue.keySet());
    values.sort(GridGrouper::compareValues);
    for (String value : values) {
      List<List<Object>> groupRows = byValue.get(value);
      String label = labels.get(value);
      int level = depth + 1;

      List<Object> groupRow = blankRow(GROUP_STYLE_PREFIX + level);
      fillAggregates(groupRow, groupRows);
      setText(groupRow, spec.labelField(), indent(level) + label + " (" + groupRows.size() + ")");
      out.add(groupRow);

      if (depth < spec.levels().size() - 1) {
        addGroups(groupRows, depth + 1, out);
      } else {
        out.addAll(groupRows);
      }

      if (spec.subtotals()) {
        List<Object> subtotal = blankRow(SUBTOTAL_STYLE);
        fillAggregates(subtotal, groupRows);
        setText(subtotal, spec.labelField(), indent(level) + spec.subtotalLabel() + " " + label);
        out.add(subtotal);
      }
    }
  }

  private static String indent(int level) {
    return INDENT.repeat(level - 1);
  }

  /**
   * Row with an empty cell per field and the style label
   */
  private List<Object> blankRow(String style) {
    List<Object> row = new ArrayList<>();
    for (String field : fields) {
      row.add(AweConstants.DATALIST_STYLE_FIELD.equals(field)
        ? new DataBean().add(AweConstants.JSON_VALUE_PARAMETER, style).add(AweConstants.JSON_LABEL_PARAMETER, style)
        : new DataBean().add(AweConstants.JSON_VALUE_PARAMETER, null).add(AweConstants.JSON_LABEL_PARAMETER, ""));
    }
    return row;
  }

  private void setText(List<Object> row, String field, String text) {
    int index = fields.indexOf(field);
    if (index >= 0) {
      row.set(index, new DataBean().add(AweConstants.JSON_VALUE_PARAMETER, text).add(AweConstants.JSON_LABEL_PARAMETER, text));
    }
  }

  private void fillAggregates(List<Object> row, List<List<Object>> rows) {
    spec.summaryTypes().forEach((field, summaryType) -> {
      int index = fields.indexOf(field);
      if (index < 0 || field.equals(spec.labelField())) {
        return;
      }
      Double aggregate = aggregate(summaryType, rows, field);
      if (aggregate != null) {
        row.set(index, aggregateCell(aggregate, spec.typeOf().apply(field)));
      }
    });
  }

  /**
   * Aggregate of a column over some rows, as the clients calculate it: the average divides by the number of rows
   */
  private Double aggregate(String summaryType, List<List<Object>> rows, String field) {
    List<Double> numbers = rows.stream().map(row -> number(cell(row, field))).filter(java.util.Objects::nonNull).toList();
    switch (summaryType.trim().toLowerCase(Locale.ROOT)) {
      case "sum":
        return numbers.stream().mapToDouble(Double::doubleValue).sum();
      case "avg":
        return rows.isEmpty() ? null : numbers.stream().mapToDouble(Double::doubleValue).sum() / rows.size();
      case "max":
        return numbers.stream().max(Double::compare).orElse(null);
      case "min":
        return numbers.stream().min(Double::compare).orElse(null);
      default:
        return null;
    }
  }

  /**
   * Cell of an aggregate with the java type its column prints (ADE formats numbers by the type of the column)
   */
  private static DataBean aggregateCell(double aggregate, ColumnType type) {
    BigDecimal rounded = BigDecimal.valueOf(aggregate).setScale(LABEL_SCALE, RoundingMode.HALF_UP);
    Object value = switch (type == null ? ColumnType.DOUBLE : type) {
      case INTEGER -> (int) Math.round(aggregate);
      case LONG -> Math.round(aggregate);
      case BIGDECIMAL -> rounded;
      case BIGINTEGER -> rounded.toBigInteger();
      default -> aggregate;
    };
    return new DataBean()
      .add(AweConstants.JSON_VALUE_PARAMETER, value)
      .add(AweConstants.JSON_LABEL_PARAMETER, rounded.stripTrailingZeros().toPlainString());
  }

  private static Object cell(List<Object> row, int index) {
    return index >= 0 && index < row.size() ? row.get(index) : null;
  }

  private Object cell(List<Object> row, String field) {
    return cell(row, fields.indexOf(field));
  }

  private static Object part(Object cell, String key) {
    return cell instanceof DataBean bean ? bean.getSingleValue(key) : null;
  }

  private static String text(Object cell, boolean useValue) {
    Object value = part(cell, useValue ? AweConstants.JSON_VALUE_PARAMETER : AweConstants.JSON_LABEL_PARAMETER);
    return value == null ? "" : value.toString();
  }

  private static String labelOrValue(Object cell) {
    String label = text(cell, false);
    return label.isEmpty() ? text(cell, true) : label;
  }

  private static Double number(Object cell) {
    Object value = part(cell, AweConstants.JSON_VALUE_PARAMETER);
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    try {
      return value == null || value.toString().isBlank() ? null : Double.valueOf(value.toString().trim());
    } catch (NumberFormatException exc) {
      return null;
    }
  }

  /**
   * Order of the groups of a level: numbers first, as numbers, then texts without case, the empty group last. Numbers
   * and texts never compare with each other and ties fall back to the exact value, so the order is total and the same
   * whatever order the rows come in
   */
  private static int compareValues(String a, String b) {
    if (a.isEmpty() || b.isEmpty()) {
      return Boolean.compare(a.isEmpty(), b.isEmpty());
    }
    Double numberA = parse(a);
    Double numberB = parse(b);
    if (numberA != null && numberB != null) {
      int byNumber = Double.compare(numberA, numberB);
      return byNumber == 0 ? a.compareTo(b) : byNumber;
    }
    if (numberA != null || numberB != null) {
      return numberA == null ? 1 : -1;
    }
    int byText = a.toLowerCase(Locale.ROOT).compareTo(b.toLowerCase(Locale.ROOT));
    return byText == 0 ? a.compareTo(b) : byText;
  }

  private static Double parse(String text) {
    try {
      return Double.valueOf(text.trim());
    } catch (NumberFormatException exc) {
      return null;
    }
  }
}
