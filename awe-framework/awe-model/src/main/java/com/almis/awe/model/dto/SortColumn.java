/*
 * Package definition
 */
package com.almis.awe.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import tools.jackson.databind.node.ObjectNode;

/**
 * SortColumn Class
 *
 * Bean class with sort column definition
 *
 *
 * @author Pablo GARCIA - 18/Jan/2016
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SortColumn {

  private String columnId;
  private String direction;

  /**
   * Constructor
   *
   * @param field     Field to sort
   * @param direction Direction (ASC, DESC)
   */
  public SortColumn(String field, String direction) {
    this.columnId = field;
    this.direction = direction != null ? direction.toUpperCase() : null;
  }

  /**
   * Constructor
   *
   * @param sortColumn Column to be sorted
   */
  public SortColumn(ObjectNode sortColumn) {
    this.columnId = sortColumn.get("id").asString("");
    this.direction = sortColumn.get("direction").asString("").toUpperCase();
  }
}
