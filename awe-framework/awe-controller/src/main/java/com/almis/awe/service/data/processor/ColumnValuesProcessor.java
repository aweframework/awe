package com.almis.awe.service.data.processor;

import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ColumnValues;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.queries.Compound;
import com.almis.awe.model.entities.queries.Computed;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queries.SqlField;
import com.almis.awe.model.type.ColumnFilterType;
import com.almis.awe.model.util.data.date.DateFormats;
import com.almis.awe.model.util.data.date.LegacyDateConverter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Distinct values of a grid column for its autofilter (#912): the values of the filtered fields with their rows count,
 * the label the column shows for each value, the search on the labels and the cap of the response.
 */
public final class ColumnValuesProcessor {

  private ColumnValuesProcessor() {
  }

  /**
   * A distinct value of the filtered fields
   *
   * @param value Value (null for the empty values)
   * @param count Rows with the value
   * @param row   Representative row of the value, with the fields its label needs
   */
  public record ColumnValueGroup(Object value, long count, Map<String, CellData> row) {
  }

  /**
   * Whether the label of each value is the value itself: the column is its only filtered field and it has no translate
   * nor transform, or the column has several filtered fields (a value can come from any of them), or it is a date
   * column (grouped by day, the day is the label)
   *
   * @param query        Query
   * @param columnValues Column values
   * @return The label is the value
   */
  public static boolean isLabelTheValue(Query query, ColumnValues columnValues) {
    if (columnValues.getType() == ColumnFilterType.DATE || columnValues.getFields().size() > 1) {
      return true;
    }
    return columnValues.getFields().get(0).equals(columnValues.getId()) && getSqlFields(query).stream()
      .filter(field -> columnValues.getId().equals(field.getIdentifier()))
      .findFirst()
      .map(field -> !field.isTranslate() && !field.isTransform())
      .orElse(false);
  }

  /**
   * Retrieve the query that computes the label of a column: a copy of the query with only the fields, computed and
   * compound fields the column needs (with their translate and transform) and without totalize
   *
   * @param query  Query
   * @param column Column (field, computed or compound identifier)
   * @return Label query
   */
  public static Query getLabelQuery(Query query, String column) {
    Set<String> fields = new HashSet<>();
    Set<String> outputs = new HashSet<>();
    addLabelDependencies(query, column, fields, outputs);
    Query labelQuery = query.copy();
    labelQuery.setSqlFieldList(getSqlFields(query).stream().filter(field -> fields.contains(field.getIdentifier())).toList());
    labelQuery.setComputedList(Optional.ofNullable(query.getComputedList()).orElse(Collections.emptyList()).stream()
      .filter(computed -> outputs.contains(computed.getIdentifier())).toList());
    labelQuery.setCompoundList(Optional.ofNullable(query.getCompoundList()).orElse(Collections.emptyList()).stream()
      .filter(compound -> outputs.contains(compound.getIdentifier())).toList());
    labelQuery.setTotalizeList(null);
    return labelQuery;
  }

  /**
   * Add the fields and the computed or compound fields an output needs, following the references of the computed
   * formats
   *
   * @param query   Query
   * @param output  Output identifier
   * @param fields  Fields needed
   * @param outputs Computed and compound fields needed
   */
  private static void addLabelDependencies(Query query, String output, Set<String> fields, Set<String> outputs) {
    if (getSqlFields(query).stream().anyMatch(field -> output.equals(field.getIdentifier()))) {
      fields.add(output);
      return;
    }
    if (outputs.contains(output)) {
      return;
    }
    List<Computed> formats = new ArrayList<>();
    Optional.ofNullable(query.getComputedList()).orElse(Collections.emptyList()).stream()
      .filter(computed -> output.equals(computed.getIdentifier()))
      .forEach(formats::add);
    Optional.ofNullable(query.getCompoundList()).orElse(Collections.emptyList()).stream()
      .filter(compound -> output.equals(compound.getIdentifier()))
      .map(Compound::getComputedList)
      .filter(Objects::nonNull)
      .forEach(formats::addAll);
    if (!formats.isEmpty()) {
      outputs.add(output);
    }
    for (Computed computed : formats) {
      Matcher references = AweConstants.DATALIST_COMPUTED_WILDCARD.matcher(Optional.ofNullable(computed.getFormat()).orElse(""));
      while (references.find()) {
        addLabelDependencies(query, references.group(1), fields, outputs);
      }
    }
  }

  /**
   * Group rows by the values of the filtered fields: a row counts once for each distinct value of its fields. Null and
   * empty values form one group; dates are grouped by day. The groups are sorted by value, empty values last.
   *
   * @param rows         Rows, before any processing
   * @param columnValues Column values
   * @return Groups, sorted
   */
  public static List<ColumnValueGroup> group(List<Map<String, CellData>> rows, ColumnValues columnValues) {
    Map<Optional<Object>, long[]> counts = new LinkedHashMap<>();
    Map<Optional<Object>, Map<String, CellData>> representatives = new HashMap<>();
    for (Map<String, CellData> row : rows) {
      Set<Optional<Object>> rowValues = new HashSet<>();
      for (String field : columnValues.getFields()) {
        Optional<Object> value = Optional.ofNullable(getValue(row.get(field), columnValues.getType()));
        if (rowValues.add(value)) {
          counts.computeIfAbsent(value, key -> new long[1])[0]++;
          representatives.putIfAbsent(value, row);
        }
      }
    }
    List<ColumnValueGroup> groups = new ArrayList<>();
    counts.forEach((value, count) -> groups.add(new ColumnValueGroup(value.orElse(null), count[0], representatives.get(value))));
    groups.sort(Comparator.comparing(ColumnValueGroup::value, Comparator.nullsLast(getValueComparator(columnValues.getType()))));
    return groups;
  }

  /**
   * Retrieve the value of a cell as the column lists it: null for null or empty cells, the day (dd/MM/yyyy) of a date
   *
   * @param cell Cell
   * @param type Column filter type
   * @return Value
   */
  public static Object getValue(CellData cell, ColumnFilterType type) {
    if (cell == null || cell.getObjectValue() == null || cell.getStringValue().isEmpty()) {
      return null;
    }
    if (type == ColumnFilterType.DATE) {
      Date date = cell.getDateValue();
      return date == null ? cell.getStringValue() : DateFormats.WEB_DATE.format(LegacyDateConverter.asLocalDate(date));
    }
    return cell.getValue();
  }

  private static Comparator<Object> getValueComparator(ColumnFilterType type) {
    if (type == ColumnFilterType.DATE) {
      return Comparator.comparing(value -> getDay(value.toString()), Comparator.nullsLast(Comparator.naturalOrder()));
    }
    return (value1, value2) -> new CellData(value1).compareTo(new CellData(value2));
  }

  private static LocalDate getDay(String value) {
    try {
      return LocalDate.parse(value, DateFormats.WEB_DATE);
    } catch (RuntimeException exc) {
      return null;
    }
  }

  /**
   * Build the response: the values whose label contains the search (ignoring case), up to the cap, and whether more
   * values exist
   *
   * @param groups    Groups, sorted
   * @param labels    Label of each group
   * @param search    Text the labels contain, null to keep every value
   * @param truncated The groups are not all the values of the column
   * @return Service data with the values (value, label, count) and the variable {@link AweConstants#ACTION_MORE_VALUES}
   */
  public static ServiceData getResult(List<ColumnValueGroup> groups, List<String> labels, String search, boolean truncated) {
    String searchText = search == null ? null : search.toLowerCase(Locale.ROOT);
    DataList dataList = new DataList();
    boolean more = truncated;
    for (int index = 0; index < groups.size(); index++) {
      String label = labels.get(index);
      if (searchText != null && (label == null || !label.toLowerCase(Locale.ROOT).contains(searchText))) {
        continue;
      }
      if (dataList.getRows().size() == ColumnValues.MAX_VALUES) {
        more = true;
        break;
      }
      ColumnValueGroup group = groups.get(index);
      Map<String, CellData> row = new HashMap<>();
      row.put(AweConstants.JSON_VALUE_PARAMETER, new CellData(group.value()));
      row.put(AweConstants.JSON_LABEL_PARAMETER, new CellData(label));
      row.put("count", new CellData(group.count()));
      dataList.addRow(row);
    }
    dataList.setRecords(dataList.getRows().size());
    return new ServiceData().setDataList(dataList).addVariable(AweConstants.ACTION_MORE_VALUES, new CellData(more));
  }

  private static List<SqlField> getSqlFields(Query query) {
    return Optional.ofNullable(query.getSqlFieldList()).orElse(Collections.emptyList());
  }
}
