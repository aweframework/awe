package com.almis.awe.service.data.processor;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ColumnFilter;
import com.almis.awe.model.dto.ColumnFilterCondition;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.FilterColumn;
import com.almis.awe.model.type.UnionType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.date.DateTimeParser;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * FilterRowProcessor class
 * <p>
 * Keeps the rows that contain the filter values and, with grid column filters, the rows that match every column
 * filter, with the semantics of the SQL ones: text ignores case and matches % and _ literally, numbers are compared
 * as numbers, dates as whole days, and a row without value only matches empty.
 */
public class FilterRowProcessor implements RowProcessor {
  private List<FilterColumn> filterList;
  private List<ColumnFilter> columnFilterList;

  /**
   * Set filter list
   *
   * @param filterList filter list
   * @return filter row processor
   */
  public FilterRowProcessor setFilterList(List<FilterColumn> filterList) {
    this.filterList = filterList;
    return this;
  }

  /**
   * Set grid column filter list (their fields are the columns of the rows)
   *
   * @param columnFilterList Column filter list
   * @return filter row processor
   */
  public FilterRowProcessor setColumnFilterList(List<ColumnFilter> columnFilterList) {
    this.columnFilterList = columnFilterList;
    return this;
  }

  /**
   * Process row list
   *
   * @param rowList row list
   * @return row list processed
   */
  public List<Map<String, CellData>> process(List<Map<String, CellData>> rowList) {
    List<Map<String, CellData>> rows = rowList;
    if (filterList != null && !filterList.isEmpty()) {
      DataList dataList = new DataList();
      dataList.setRows(rows);
      DataListUtil.filterContains(dataList, filterList.toArray(new FilterColumn[0]));
      rows = dataList.getRows();
    }

    if (columnFilterList != null && !columnFilterList.isEmpty()) {
      // A mutable list, as the one it receives: the rows are sorted and paginated in place afterwards
      rows = rows.stream()
        .filter(row -> columnFilterList.stream().allMatch(columnFilter -> matches(columnFilter, row)))
        .collect(Collectors.toCollection(ArrayList::new));
    }

    // Retrieve filtered rows
    return rows;
  }

  private static boolean matches(ColumnFilter columnFilter, Map<String, CellData> row) {
    return columnFilter.isNoop() || columnFilter.getFields().stream().anyMatch(field -> matches(columnFilter, row.get(field)));
  }

  private static boolean matches(ColumnFilter columnFilter, CellData cell) {
    if (!matchesValues(columnFilter, cell)) {
      return false;
    }
    List<ColumnFilterCondition> conditions = columnFilter.getConditions();
    if (conditions.isEmpty()) {
      return true;
    }
    return UnionType.OR.equals(columnFilter.getUnion())
      ? conditions.stream().anyMatch(condition -> matches(columnFilter, condition, cell))
      : conditions.stream().allMatch(condition -> matches(columnFilter, condition, cell));
  }

  private static boolean matchesValues(ColumnFilter columnFilter, CellData cell) {
    if (columnFilter.getValues() == null && !columnFilter.isEmpty()) {
      return true;
    }
    boolean empty = isEmpty(cell);
    if (empty && columnFilter.isEmpty()) {
      return true;
    }
    for (JsonNode value : columnFilter.getValues() == null ? List.<JsonNode>of() : columnFilter.getValues()) {
      if (value.isNull() ? empty : !empty && matchesValue(value, cell)) {
        return true;
      }
    }
    return false;
  }

  private static boolean matchesValue(JsonNode value, CellData cell) {
    if (value.isNumber()) {
      BigDecimal number = toNumber(cell);
      return number != null && number.compareTo(value.decimalValue()) == 0;
    }
    return cell.getStringValue().equals(value.asString("").trim());
  }

  private static boolean matches(ColumnFilter columnFilter, ColumnFilterCondition condition, CellData cell) {
    return switch (condition.getOperator()) {
      case EMPTY -> isEmpty(cell);
      case NOT_EMPTY -> !isEmpty(cell);
      default -> !isEmpty(cell) && switch (columnFilter.getType()) {
        case NUMBER -> matchesNumber(condition, toNumber(cell));
        case DATE -> matchesDate(condition, toDate(cell));
        default -> matchesText(condition, cell.getStringValue().toLowerCase(Locale.ROOT));
      };
    };
  }

  private static boolean matchesText(ColumnFilterCondition condition, String text) {
    String value = condition.getValue().toLowerCase(Locale.ROOT);
    return switch (condition.getOperator()) {
      case EQ -> text.equals(value);
      case NE -> !text.equals(value);
      case CONTAINS -> text.contains(value);
      case NOT_CONTAINS -> !text.contains(value);
      case STARTS_WITH -> text.startsWith(value);
      case NOT_STARTS_WITH -> !text.startsWith(value);
      case ENDS_WITH -> text.endsWith(value);
      case NOT_ENDS_WITH -> !text.endsWith(value);
      default -> false;
    };
  }

  private static boolean matchesNumber(ColumnFilterCondition condition, BigDecimal number) {
    if (number == null) {
      return false;
    }
    int comparison = number.compareTo(new BigDecimal(condition.getValue()));
    return switch (condition.getOperator()) {
      case EQ -> comparison == 0;
      case NE -> comparison != 0;
      case GT -> comparison > 0;
      case GE -> comparison >= 0;
      case LT -> comparison < 0;
      case LE -> comparison <= 0;
      case BETWEEN -> comparison >= 0 && number.compareTo(new BigDecimal(condition.getTo())) <= 0;
      default -> false;
    };
  }

  private static boolean matchesDate(ColumnFilterCondition condition, LocalDate date) {
    if (date == null) {
      return false;
    }
    int comparison = date.compareTo(DateTimeParser.parseWebDate(condition.getValue()));
    return switch (condition.getOperator()) {
      case EQ -> comparison == 0;
      case NE -> comparison != 0;
      case GT -> comparison > 0;
      case GE -> comparison >= 0;
      case LT -> comparison < 0;
      case LE -> comparison <= 0;
      case BETWEEN -> comparison >= 0 && !date.isAfter(DateTimeParser.parseWebDate(condition.getTo()));
      default -> false;
    };
  }

  private static boolean isEmpty(CellData cell) {
    return cell == null || cell.isEmpty();
  }

  private static BigDecimal toNumber(CellData cell) {
    try {
      return cell.getObjectValue() instanceof Number number ? new BigDecimal(number.toString()) : new BigDecimal(cell.getStringValue());
    } catch (NumberFormatException exc) {
      return null;
    }
  }

  private static LocalDate toDate(CellData cell) {
    Object value = cell.getObjectValue();
    if (value instanceof LocalDate localDate) {
      return localDate;
    } else if (value instanceof LocalDateTime localDateTime) {
      return localDateTime.toLocalDate();
    } else if (value instanceof java.sql.Date sqlDate) {
      return sqlDate.toLocalDate();
    }
    Date date = cell.getDateValue();
    return date == null ? null : Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
  }
}
