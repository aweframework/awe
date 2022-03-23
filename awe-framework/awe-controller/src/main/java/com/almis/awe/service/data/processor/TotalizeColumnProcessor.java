package com.almis.awe.service.data.processor;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.entities.queries.SqlField;
import com.almis.awe.model.entities.queries.Totalize;
import com.almis.awe.model.entities.queries.TotalizeBy;
import com.almis.awe.model.entities.queries.TotalizeField;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.NumericService;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TransformCellProcessor class
 */
public class TotalizeColumnProcessor implements ColumnProcessor {
  Map<String, CellData> totalizeValues = null;
  Map<String, String> totalizeKeys = null;
  List<SqlField> fieldList = null;
  private Totalize totalize;

  // Autowired services
  private final AweElements elements;
  private final NumericService numericService;
  private final EncodeService encodeService;

  /**
   * Totalize column processor constructor
   *
   * @param elements       AWE elements
   * @param numericService Numeric service
   * @param encodeService  Encode service
   */
  public TotalizeColumnProcessor(AweElements elements, NumericService numericService, EncodeService encodeService) {
    this.elements = elements;
    this.numericService = numericService;
    this.encodeService = encodeService;
  }

  /**
   * Set transform field
   *
   * @param totalize Totalize field
   * @return TotalizeColumnProcessor
   */
  public TotalizeColumnProcessor setTotalize(Totalize totalize) {
    this.totalize = totalize;
    return this;
  }

  /**
   * Set field list
   *
   * @param fieldList Field list
   * @return TotalizeColumnProcessor
   */
  public TotalizeColumnProcessor setFieldList(List<SqlField> fieldList) {
    this.fieldList = fieldList;
    return this;
  }

  /**
   * Check if add a new line
   *
   * @param row Row to check
   * @return Add a new line
   */
  public boolean checkNewLine(Map<String, CellData> row) {
    boolean addLine = row == null;
    // If totalizeKeys is null, generate it
    if (totalizeKeys == null) {
      totalizeKeys = new HashMap<>();
    }

    // Check totalizeBy fields
    List<TotalizeBy> totalizeByList = totalize.getTotalizeByList();

    // Check if one of totalize by fields is different
    if (totalizeByList != null && row != null) {
      // Check totalize by
      for (TotalizeBy totalizeBy : totalizeByList) {
        addLine = checkTotalizedLine(row, totalizeBy, addLine);
      }
    }
    return addLine;
  }

  /**
   * Check totalized line
   *
   * @param row        Row to check
   * @param totalizeBy Totalize by
   * @param addLine    Add line value
   * @return Add line or not
   */
  private boolean checkTotalizedLine(Map<String, CellData> row, TotalizeBy totalizeBy, boolean addLine) {
    String lastValue = row.get(totalizeBy.getField()) != null ? row.get(totalizeBy.getField()).getStringValue() : null;
    if (totalizeKeys.containsKey(totalizeBy.getField())) {
      String nextValue = totalizeKeys.get(totalizeBy.getField());
      if (nextValue != null && !nextValue.equals(lastValue)) {
        addLine = true;
        totalizeKeys.put(totalizeBy.getField(), lastValue);
      }
    } else if (lastValue != null) {
      totalizeKeys.put(totalizeBy.getField(), lastValue);
    }
    return addLine;
  }

  /**
   * Retrieve a new totalize line
   *
   * @return New totalize line
   */
  private Map<String, CellData> getNewLine() {
    // Create new row
    Map<String, CellData> totalizeRow = new HashMap<>();

    for (SqlField field : fieldList) {
      String columnIdentifier;
      String totalizeIdentifier = "-" + totalize.getFunction();
      CellData cell;

      TransformCellProcessor transformProcessor = new TransformCellProcessor(elements, field, numericService, encodeService);

      columnIdentifier = transformProcessor.getColumnIdentifier();
      totalizeIdentifier = columnIdentifier + totalizeIdentifier;

      // Get totalize values
      if (totalizeValues.containsKey(totalizeIdentifier)) {
        // Format output data
        cell = totalizeValues.get(totalizeIdentifier);

      } else {
        cell = new CellData();
      }

      totalizeRow.put(columnIdentifier, cell);
    }
    totalizeValues.clear();
    return totalizeRow;
  }

  /**
   * Add a new line with values
   *
   * @param list Row list
   * @throws AWException Error adding a new line
   */
  public void addNewLine(List<Map<String, CellData>> list) throws AWException {
    // Create new row
    Map<String, CellData> newRow = getNewLine();

    // Add label
    newRow.put(totalize.getField(), new CellData(elements.getLocaleWithLanguage(totalize.getLabel(), elements.getLanguage())));

    // Add style value
    if (totalize.getStyle() != null) {
      newRow.put(AweConstants.DATALIST_STYLE_FIELD, new CellData(totalize.getStyle()));
    }

    // Add row ID
    newRow.put("id", new CellData("TOT-" + list.size()));

    // Add row list
    list.add(newRow);
  }

  /**
   * Retrieve column identifier
   *
   * @return Column identifier
   */
  public String getColumnIdentifier() {
    return totalize.getField();
  }

  /**
   * Process row
   *
   * @param row Row to process
   * @return Null (Interface requirements)
   * @throws AWException Error processing row
   */
  public CellData process(Map<String, CellData> row) throws AWException {

    if (totalize == null) {
      throw new NullPointerException("No totalize defined");
    }

    if (fieldList == null) {
      throw new NullPointerException("No field list defined");
    }

    // Calculate values
    if (totalize.getTotalizeFieldList() != null && row != null) {
      if (totalizeValues == null) {
        totalizeValues = new HashMap<>();
      }

      // For each field to totalize, calculate values
      for (TotalizeField totalizeField : totalize.getTotalizeFieldList()) {
        calculateTotalizedRow(row, totalizeField);
      }
    }

    // Return null
    return null;
  }

  /**
   * Calculate totalized row
   *
   * @param row           Row to be calculated
   * @param totalizeField Totalize field
   */
  private void calculateTotalizedRow(Map<String, CellData> row, TotalizeField totalizeField) {
    // Big decimal treatment. Choose number type and cast to BigDecimal
    CellData field = Optional.ofNullable(row.get(totalizeField.getField())).orElse(new CellData());
    double doubleValue = fixDoubleValue(field.getDoubleValue(), field.getStringValue());

    int cntVal = Optional.ofNullable(getTotalsField(totalizeField, "CNT").getIntegerValue()).orElse(0) + 1;
    double totVal = Optional.ofNullable(getTotalsField(totalizeField, "SUM").getDoubleValue()).orElse(0.0) + doubleValue;
    double maxVal = Math.max(Optional.ofNullable(getTotalsField(totalizeField, "MAX").getDoubleValue()).orElse(doubleValue), doubleValue);
    double minVal = Math.min(Optional.ofNullable(getTotalsField(totalizeField, "MIN").getDoubleValue()).orElse(doubleValue), doubleValue);

    // Put value on list
    totalizeValues.put(getTotalsFieldIdentifier(totalizeField, "SUM"), new CellData(totVal));
    totalizeValues.put(getTotalsFieldIdentifier(totalizeField, "AVG"), new CellData(Math.ceil(totVal / cntVal)));
    totalizeValues.put(getTotalsFieldIdentifier(totalizeField, "MAX"), new CellData(maxVal));
    totalizeValues.put(getTotalsFieldIdentifier(totalizeField, "MIN"), new CellData(minVal));
    totalizeValues.put(getTotalsFieldIdentifier(totalizeField, "CNT"), new CellData(cntVal));
  }

  /**
   * Get totals field
   *
   * @param field     Field
   * @param operation Operation
   * @return Totals field value
   */
  private CellData getTotalsField(TotalizeField field, String operation) {
    return Optional.ofNullable(totalizeValues.get(getTotalsFieldIdentifier(field, operation))).orElse(new CellData());
  }

  /**
   * Get totals field identifier
   *
   * @param field     Field
   * @param operation Operation
   * @return Field identifier
   */
  private String getTotalsFieldIdentifier(TotalizeField field, String operation) {
    return String.format("%s-%s", field.getField(), operation);
  }

  /**
   * Fix double value with string value
   *
   * @param doubleValue Double value
   * @param stringValue String value
   * @return Double value fixed
   */
  private Double fixDoubleValue(Double doubleValue, String stringValue) {
    return Optional.ofNullable(doubleValue).orElse(parseStringValue(stringValue));
  }

  /**
   * Parse string value as double
   *
   * @param value String value
   * @return Double value
   */
  private Double parseStringValue(String value) {
    try {
      return numericService.parseNumericString(value).doubleValue();
    } catch (ParseException exc) {
      return 0.0;
    }
  }
}
