package com.almis.awe.model.dto;

import com.almis.awe.model.type.ColumnFilterType;
import com.almis.awe.model.type.UnionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Distinct values requested for the autofilter of a grid column, received in the {@code columnValues} request
 * parameter of the {@code column-values} action (issue #912): {@code {"id", "fields"?, "type", "search"?}}.
 * <p>
 * {@code id} is the column (its field gives the label of each value), {@code fields} the filtered fields (the
 * values), {@code [id]} when absent.
 */
@Data
@AllArgsConstructor
public class ColumnValues {

  // Values returned at most; the response states whether more exist
  public static final int MAX_VALUES = 1000;

  private String id;
  private List<String> fields;
  private ColumnFilterType type;
  private String search;

  /**
   * Retrieve the column values as a column filter without condition, to check its fields with the column filter rules
   *
   * @return Column filter of the same column, fields and type
   */
  public ColumnFilter asColumnFilter() {
    return new ColumnFilter(id, fields, type, null, false, UnionType.AND, new ArrayList<>());
  }

  /**
   * Parse the column values of the request
   *
   * @param node Column values (an object)
   * @return Column values
   * @throws IllegalArgumentException The column values are not valid, with the reason as message
   */
  public static ColumnValues fromJson(JsonNode node) {
    if (node == null || !node.isObject()) {
      throw new IllegalArgumentException("columnValues must be an object");
    }
    String id = ColumnFilter.getText(node, "id");
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("columnValues needs an id: " + node);
    }
    ColumnFilterType type = ColumnFilterType.fromCode(ColumnFilter.getText(node, "type"))
      .orElseThrow(() -> new IllegalArgumentException("unknown type in column values " + id));
    JsonNode search = node.get("search");
    if (search != null && !search.isNull() && !search.isString()) {
      throw new IllegalArgumentException("search must be a text in column values " + id);
    }
    String searchText = search == null || search.isNull() || search.stringValue().isBlank() ? null : search.stringValue();
    return new ColumnValues(id, ColumnFilter.parseFields(node.get("fields"), id), type, searchText);
  }
}
