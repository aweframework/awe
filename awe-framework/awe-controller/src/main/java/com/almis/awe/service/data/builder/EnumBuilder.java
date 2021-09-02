package com.almis.awe.service.data.builder;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.entities.Global;
import com.almis.awe.model.entities.enumerated.EnumeratedGroup;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;

import static com.almis.awe.model.constant.AweConstants.*;

/**
 * Generate enumerated datalists
 */
public class EnumBuilder extends ServiceConfig {

  private String enumeratedId;
  private static final String ERROR_TITLE_RETRIEVING_ELEMENT = "ERROR_TITLE_RETRIEVING_ELEMENT";
  private static final String ERROR_MESSAGE_RETRIEVING_ELEMENT = "ERROR_MESSAGE_RETRIEVING_ELEMENT";

  /**
   * Assigns the query to be recovered
   * 
   * @param enumeratedId Enumerated identifier
   * @return this
   */
  public EnumBuilder setEnumerated(String enumeratedId) {
    this.enumeratedId = enumeratedId;

    return this;
  }

  /**
   * Launches a query (must be defined in APP or awe Queries.xml file) and generates the output Query comes defined in target-action variable
   *
   * @return Enumerated group
   * @throws AWException Error retrieving enumerated group
   */
  public EnumeratedGroup getEnumerated() throws AWException {
    return getElements().getEnumerated(enumeratedId).copy();
  }

  /**
   * Launches a query (must be defined in APP or awe Queries.xml file) and generates the output Query comes defined in target-action variable
   *
   * @return Enumerated
   * @throws AWException Error building enumerated
   */
  public List<Global> build() throws AWException {
    if (enumeratedId == null) {
      throw new AWException(getLocale(ERROR_TITLE_RETRIEVING_ELEMENT),
              getLocale(ERROR_MESSAGE_RETRIEVING_ELEMENT, enumeratedId));
    }
    EnumeratedGroup enumerated = getEnumerated();
    if (enumerated == null) {
      throw new AWException(getLocale(ERROR_TITLE_RETRIEVING_ELEMENT),
              getLocale(ERROR_MESSAGE_RETRIEVING_ELEMENT, enumeratedId));
    }

    return enumerated.getOptionList();
  }

  /**
   * Get enumerated element as Json
   *
   * @return Enumerated as Json
   * @throws AWException Error retrieving enumerated
   */
  public ArrayNode getEnumeratedAsJson() throws AWException {
    EnumeratedGroup enumerated = getEnumerated();
    if (enumerated == null) {
      throw new AWException(getLocale(ERROR_TITLE_RETRIEVING_ELEMENT),
              getLocale(ERROR_MESSAGE_RETRIEVING_ELEMENT, enumeratedId));
    }
    return getOptionListAsJson(enumerated);
  }

  /**
   * Get enumerated element as DataList
   *
   * @return Enumerated element as DataList
   * @throws AWException Error retrieving enumerated
   */
  public DataList getEnumeratedAsDataList () throws AWException {
    EnumeratedGroup enumerated = getEnumerated();
    if (enumerated == null) {
      throw new AWException(getLocale(ERROR_TITLE_RETRIEVING_ELEMENT),
              getLocale(ERROR_MESSAGE_RETRIEVING_ELEMENT, enumeratedId));
    }
    return getOptionListAsDataList(enumerated);
  }

  private DataList getOptionListAsDataList(EnumeratedGroup enumerated) {
    DataList output = new DataList();
    int rowIndex = 1;
    for (Global option : enumerated.getOptionList()) {
      output.addRow(getDataListRow(option, rowIndex));
      rowIndex++;
    }
    return output;
  }

  /**
   * Find a enumerated label
   *
   * @param value Label value
   * @return Label found
   * @throws AWException Enumerated not found
   */
  public String findLabel(String value) throws AWException {
    EnumeratedGroup enumerated = getEnumerated();
    if (enumerated == null) {
      throw new AWException(getLocale(ERROR_TITLE_RETRIEVING_ELEMENT),
              getLocale(ERROR_MESSAGE_RETRIEVING_ELEMENT, enumeratedId));
    }
    return enumerated.findLabel(value);
  }

  /**
   * Returns the group option list
   * 
   * @param enumerated enumerated
   * @return Group option list
   */
  public ArrayNode getOptionListAsJson(EnumeratedGroup enumerated) {
    ArrayNode output = JsonNodeFactory.instance.arrayNode();
    for (Global option : enumerated.getOptionList()) {
      output.add(getJsonRow(option));
    }
    return output;
  }

  /**
   * Retrieve the data as a json row
   *
   * @param option option
   * @return Json row
   */
  public ObjectNode getJsonRow(Global option) {
    ObjectNode data = JsonNodeFactory.instance.objectNode();

    if (option.getLabel() != null) {
      data.put(JSON_LABEL_PARAMETER, option.getLabel());
    }

    if (option.getValue() != null) {
      data.put(JSON_VALUE_PARAMETER, option.getValue());
    }

    return data;
  }


  /**
   * Retrieve the data as a datalist row
   *
   * @param option option
   * @param rowIndex row index
   * @return Datalist row
   */
  private HashMap<String, CellData> getDataListRow(Global option, int rowIndex) {
    HashMap<String, CellData> row = new HashMap<>();
    if (option != null) {
      // Add index
      row.put(JSON_ID_PARAMETER, new CellData(rowIndex));
      row.put(JSON_LABEL_PARAMETER, new CellData(option.getLabel()));
      row.put(JSON_VALUE_PARAMETER,  new CellData(option.getValue()));
    }
    return row;
  }
}
