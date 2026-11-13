package com.almis.awe.service.report;

import com.almis.ade.api.bean.input.DataBean;
import com.almis.awe.model.constant.AweConstants;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

/**
 * Rewrites the cells of the data of a grid for the ADE column types that draw more than a text (chips, progress bars and
 * sparklines) and for the text of formatted columns. The cells arrive as the clients send them (value and label); the
 * rules are keyed by field, so the same rewriting serves the parameters and the query data.
 */
final class GridCellPrinter {

  private static final String DEFAULT_CHIP_SEPARATOR = ",";
  private static final double PERCENT = 100d;

  private final ObjectMapper mapper;
  private final Map<String, BiFunction<DataBean, Map<String, DataBean>, Object>> rules = new LinkedHashMap<>();
  private final Map<String, String> statusFields = new LinkedHashMap<>();

  GridCellPrinter(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * Fields the rules read from the rows besides the printed columns (the status fields)
   *
   * @return Extra fields
   */
  Set<String> extraFields() {
    return new LinkedHashSet<>(statusFields.values());
  }

  /**
   * Print the column as one chip: the label of the cell, with the tone of the status field of the row
   *
   * @param field       Column field
   * @param statusField Field with the tone
   */
  void addStatus(String field, String statusField) {
    statusFields.put(field, statusField);
    rules.put(field, (cell, row) -> {
      String label = label(cell);
      return label.isEmpty() ? new ArrayList<DataBean>() : chipBean(tone(row, statusField), label);
    });
  }

  /**
   * Print the column as the chips of the items of its text
   *
   * @param field       Column field
   * @param separator   Separator of the items (a comma by default)
   * @param statusField Field with the tone of the chips, null for the neutral one
   */
  void addChips(String field, String separator, String statusField) {
    if (statusField != null && !statusField.isBlank()) {
      statusFields.put(field, statusField);
    }
    String itemSeparator = separator == null || separator.isEmpty() ? DEFAULT_CHIP_SEPARATOR : separator;
    rules.put(field, (cell, row) -> {
      String tone = statusField == null || statusField.isBlank() ? "neutral" : tone(row, statusField);
      List<DataBean> chips = new ArrayList<>();
      for (String item : label(cell).split(Pattern.quote(itemSeparator))) {
        if (!item.isBlank()) {
          chips.add(chipBean(tone, item.trim()));
        }
      }
      return chips;
    });
  }

  /**
   * Print the column as a bar filled from the value in the scale of the column
   *
   * @param field Column field
   * @param min   Lower end of the scale
   * @param max   Upper end of the scale
   */
  void addProgress(String field, Double min, Double max) {
    double low = min == null ? 0d : min;
    double high = max == null ? PERCENT : max;
    // The same rule as the clients: a scale that does not go up is the default one
    double scaleLow = high > low ? low : 0d;
    double scaleHigh = high > low ? high : PERCENT;
    rules.put(field, (cell, row) -> {
      Double number = toNumber(cell.getSingleValue(AweConstants.JSON_VALUE_PARAMETER));
      // An empty cell (a group, subtotal or total row) draws no bar: ADE leaves a null cell blank
      if (number == null && label(cell).isEmpty()) {
        return null;
      }
      Double fill = number == null ? null
        : (Math.clamp(number, scaleLow, scaleHigh) - scaleLow) / (scaleHigh - scaleLow) * PERCENT;
      return new DataBean()
        .add(AweConstants.JSON_VALUE_PARAMETER, fill)
        .add(AweConstants.JSON_LABEL_PARAMETER, label(cell));
    });
  }

  /**
   * Print the column as a sparkline of the series of the cell (empty when the cell is not a series of numbers)
   *
   * @param field Column field
   */
  void addSparkline(String field) {
    rules.put(field, (cell, row) -> new DataBean()
      .add(AweConstants.JSON_VALUE_PARAMETER, series(cell.getSingleValue(AweConstants.JSON_VALUE_PARAMETER)))
      .add(AweConstants.JSON_LABEL_PARAMETER, label(cell)));
  }

  /**
   * Print the column as the plain text of its HTML
   *
   * @param field Column field
   */
  void addFormattedText(String field) {
    rules.put(field, (cell, row) -> {
      String text = PrintText.plainText(label(cell));
      return new DataBean()
        .add(AweConstants.JSON_VALUE_PARAMETER, text)
        .add(AweConstants.JSON_LABEL_PARAMETER, text);
    });
  }

  /**
   * Check whether the column is drawn by a rule (and so cannot hold the label of a group row)
   *
   * @param field Column field
   * @return The column has a rule
   */
  boolean handles(String field) {
    return rules.containsKey(field);
  }

  /**
   * Check whether there is any rule
   *
   * @return The printer has rules
   */
  boolean isEmpty() {
    return rules.isEmpty();
  }

  /**
   * Rewrite the cells of the rows
   *
   * @param fields Fields of the rows (the position of each cell)
   * @param rows   Rows of the data, rewritten in place
   */
  void apply(List<String> fields, List<List<Object>> rows) {
    for (List<Object> row : rows) {
      Map<String, DataBean> cells = new HashMap<>();
      for (int index = 0; index < fields.size() && index < row.size(); index++) {
        if (row.get(index) instanceof DataBean bean) {
          cells.put(fields.get(index), bean);
        }
      }
      rules.forEach((field, rule) -> {
        int index = fields.indexOf(field);
        if (index >= 0 && index < row.size() && row.get(index) instanceof DataBean cell) {
          row.set(index, rule.apply(cell, cells));
        }
      });
    }
  }

  private static String label(DataBean cell) {
    Object label = cell.getSingleValue(AweConstants.JSON_LABEL_PARAMETER);
    Object value = cell.getSingleValue(AweConstants.JSON_VALUE_PARAMETER);
    Object text = label == null || label.toString().isEmpty() ? value : label;
    return text == null ? "" : text.toString();
  }

  private static String tone(Map<String, DataBean> row, String statusField) {
    DataBean status = row.get(statusField);
    Object value = status == null ? null : status.getSingleValue(AweConstants.JSON_VALUE_PARAMETER);
    return PrintPalette.toneName(value == null ? null : value.toString());
  }

  private static DataBean chipBean(String tone, String label) {
    return new DataBean()
      .add(AweConstants.JSON_VALUE_PARAMETER, tone)
      .add(AweConstants.JSON_LABEL_PARAMETER, label);
  }

  private static Double toNumber(Object value) {
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
   * Number of an item of a series: a number, or a text that parses as one
   */
  private Double seriesNumber(JsonNode item) {
    if (item.isNumber()) {
      return item.doubleValue();
    }
    return item.isString() ? toNumber(item.asString()) : null;
  }

  /**
   * Series of a sparkline cell: a list or the JSON array of numbers of its text; an empty list when it is not one
   */
  private List<Double> series(Object value) {
    try {
      JsonNode node = value instanceof String text ? mapper.readTree(text) : mapper.valueToTree(value);
      List<Double> numbers = new ArrayList<>();
      if (node == null || !node.isArray() || node.isEmpty()) {
        return numbers;
      }
      for (JsonNode item : node) {
        Double number = seriesNumber(item);
        if (number == null || number.isNaN() || number.isInfinite()) {
          return new ArrayList<>();
        }
        numbers.add(number);
      }
      return numbers;
    } catch (RuntimeException exc) {
      return new ArrayList<>();
    }
  }
}
