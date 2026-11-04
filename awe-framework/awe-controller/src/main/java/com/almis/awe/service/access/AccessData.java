package com.almis.awe.service.access;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.almis.awe.model.constant.AweConstants.COMPONENT_MAX;
import static com.almis.awe.model.constant.AweConstants.COMPONENT_PAGE;
import static com.almis.awe.model.constant.AweConstants.COMPONENT_SORT;
import static com.almis.awe.model.constant.AweConstants.JSON_VALUE_PARAMETER;

/**
 * Names and cell helpers shared by the services of the access window (access-admin)
 */
final class AccessData {

  static final String SUBJECT_USER = "user";
  static final String SUBJECT_PROFILE = "profile";
  static final String FILTER_ALLOWED = "allowed";
  static final String FILTER_DENIED = "denied";
  static final String CHANGE_ALLOW = "allow";
  static final String CHANGE_DENY = "deny";
  static final String CHANGE_REMOVE = "remove";
  // Request and query parameters, and query columns
  static final String USER = "user";
  static final String PROFILE = "profile";
  static final String OPTION = "option";
  static final String RESTRICTION = "restriction";
  static final String USER_ID = "userId";
  static final String PROFILE_ID = "profileId";
  static final String FULL_NAME = "fullName";
  static final String USER_RESTRICTION = "userRestriction";
  // Rules: allow and deny ("R", restricted)
  static final String ALLOW = "A";
  static final String DENY = "R";
  static final String TRUE = "true";
  static final String FALSE = "false";
  // Locale prefix of the window
  static final String PREFIX = "SCREEN_TEXT_ACCESS_";
  // Tones of the tags and cards
  static final String SUCCESS = "success";
  static final String DANGER = "danger";
  static final String INFO = "info";
  static final String WARNING = "warning";
  // Columns of a card
  static final String VISIBLE = "visible";
  static final String TITLE = "title";
  static final String DESCRIPTION = "description";
  static final String ICON = "icon";
  static final String ICON_TONE = "iconTone";
  static final String WARNING_ICON = "lucide:triangle-alert";
  static final String SEPARATOR = " · ";
  // Layer of an access inherited from an ancestor
  static final String LAYER_INHERITED = "inherited";

  private AccessData() {
  }

  /**
   * Parameters of a query launched by a service of the window: every row, in the order of the query. A paging or sort
   * parameter missing from them is read from the request (the max of the grid or card that called the service, else
   * gridRowsPerPage, and its page and sort), which would hide or reorder the rules and users the cascade needs
   *
   * @return Parameters without page nor sort
   */
  static ObjectNode allRows() {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.put(COMPONENT_MAX, 0);
    parameters.put(COMPONENT_PAGE, 1);
    parameters.putArray(COMPONENT_SORT);
    return parameters;
  }

  static ServiceData asServiceData(List<Map<String, CellData>> rows) {
    return new ServiceData().setDataList(new DataList().setRows(new ArrayList<>(rows)).setRecords(rows.size()));
  }

  /**
   * One row with a value, as the label of a button
   */
  static ServiceData valueData(String value) {
    return asServiceData(List.of(new HashMap<>(Map.of(JSON_VALUE_PARAMETER, new CellData(value)))));
  }

  static String stringOf(CellData cell) {
    return cell == null || cell.getValue() == null ? null : cell.getStringValue();
  }

  /**
   * Integer value of a cell: the databases return the identifiers as integer, long or decimal numbers
   */
  static Integer integerOf(CellData cell) {
    if (cell == null || cell.getValue() == null) {
      return null;
    }
    Integer value = cell.getIntegerValue();
    if (value == null && StringUtils.isNotBlank(cell.getStringValue())) {
      value = Integer.valueOf(cell.getStringValue());
    }
    return value;
  }

  /**
   * Option names without blanks or repetitions, in their order
   */
  static List<String> distinct(List<String> options) {
    return new ArrayList<>(Optional.ofNullable(options).orElse(Collections.emptyList()).stream()
      .filter(StringUtils::isNotBlank)
      .collect(LinkedHashSet<String>::new, Set::add, Set::addAll));
  }
}
