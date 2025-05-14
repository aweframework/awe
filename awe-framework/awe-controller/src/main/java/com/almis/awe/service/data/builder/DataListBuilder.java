package com.almis.awe.service.data.builder;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.FilterColumn;
import com.almis.awe.model.dto.SortColumn;
import com.almis.awe.model.entities.Global;
import com.almis.awe.model.entities.queries.Field;
import com.almis.awe.model.entities.queries.SqlField;
import com.almis.awe.model.type.CellDataType;
import com.almis.awe.service.data.processor.*;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.QTuple;
import com.querydsl.core.types.dsl.SimpleOperation;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.almis.awe.model.constant.AweConstants.*;

/*
 * File Imports
 */

/**
 * DataList Builder
 * <p>
 * Builder class to generate DataLists
 * </p>
 * @author Pablo GARCIA - 20/MAR/2017
 */
public class DataListBuilder extends ServiceConfig {

  private DataList dataList = null;
  private List<DataList> dataListList = null;
  private Map<String, List<CellData>> columnList = null;
  private boolean sort = false;
  private boolean distinct = false;
  private boolean filter = false;
  private boolean compute = false;
  private boolean transform = false;
  private boolean translate = false;
  private boolean postTransform = false;
  private boolean postTranslate = false;
  private boolean compound = false;
  private boolean identifier = false;
  private boolean totalize = false;
  private boolean noPrint = false;
  private boolean paginate = false;
  private List<Tuple> queryResult = null;
  private List<Global> enumQueryResult = null;
  private List<String> serviceQueryResult = null;
  private List<SqlField> fieldList = null;
  private Expression<?> projection = null;
  private List<SortColumn> sortList = null;
  private List<SortColumn> distinctList = null;
  private List<FilterColumn> filterList = null;
  private List<TotalizeColumnProcessor> totalizeList = null;
  private List<ComputedColumnProcessor> computedList = null;
  private List<TransformCellProcessor> transformList = null;
  private List<TranslateCellProcessor> translateList = null;
  private List<TransformCellProcessor> postTransformList = null;
  private List<TranslateCellProcessor> postTranslateList = null;
  private List<CompoundColumnProcessor> compoundList = null;
  private List<String> noPrintList = null;
  private long max = -1;
  private long page = -1;
  private long records = -1;

  /**
   * Adds a EnumQueryResult
   *
   * @param enumQueryResult Enumerated query output
   * @return DataListBuilder
   */
  public DataListBuilder setEnumQueryResult(List<Global> enumQueryResult) {
    this.enumQueryResult = enumQueryResult;
    return this;
  }

  /**
   * Adds a ServiceQueryResult
   *
   * @param serviceQueryResult Service query output
   * @return DataListBuilder
   */
  public DataListBuilder setServiceQueryResult(final String[] serviceQueryResult) {
    this.serviceQueryResult = serviceQueryResult == null ? null : Arrays.asList(serviceQueryResult);
    return this;
  }

  /**
   * Adds the list of Field needed for the service query result
   *
   * @param fieldList Field list
   * @return DataListBuilder
   */
  public DataListBuilder setFieldList(List<SqlField> fieldList) {
    this.fieldList = fieldList;
    return this;
  }

  /**
   * Adds a QueryResult
   *
   * @param queryResult Query result
   * @return DataListBuilder
   */
  public DataListBuilder setQueryResult(List<Tuple> queryResult) {
    this.queryResult = queryResult;
    return this;
  }

  /**
   * Adds the query projection containing information about the columns returned
   *
   * @param projection Query projection
   * @return DataListBuilder
   */
  public DataListBuilder setQueryProjection(Expression<?> projection) {
    this.projection = projection;
    return this;
  }

  /**
   * Set the response as a datalist
   *
   * @param dataList DataList
   * @return DataListBuilder
   */
  public DataListBuilder setDataList(DataList dataList) {
    this.dataList = dataList;
    setPage(dataList.getPage());
    setRecords(dataList.getRecords());
    return this;
  }

  /**
   * Add a datalist
   *
   * @param dataList DataList
   * @return DataListBuilder
   */
  public DataListBuilder addDataList(DataList dataList) {
    if (dataListList == null) {
      dataListList = new ArrayList<>();
    }
    dataListList.add(dataList);
    return this;
  }

  /**
   * Add a computed processor
   *
   * @param computed Computed
   * @return DataListBuilder
   */
  public DataListBuilder addComputed(ComputedColumnProcessor computed) {
    if (computedList == null) {
      computedList = new ArrayList<>();
    }
    computedList.add(computed);
    this.compute = true;
    return this;
  }

  /**
   * Add a compound processor
   *
   * @param compound Compound processor
   * @return DataListBuilder
   */
  public DataListBuilder addCompound(CompoundColumnProcessor compound) {
    if (compoundList == null) {
      compoundList = new ArrayList<>();
    }
    compoundList.add(compound);
    this.compound = true;
    return this;
  }

  /**
   * Add a transform processor
   *
   * @param transform Transform processor
   * @return DataListBuilder
   */
  public DataListBuilder addTransform(TransformCellProcessor transform) {
    if (transformList == null) {
      transformList = new ArrayList<>();
    }
    transformList.add(transform);
    this.transform = true;
    return this;
  }

  /**
   * Add a translate processor
   *
   * @param translate Translate processor
   * @return DataListBuilder
   */
  public DataListBuilder addTranslate(TranslateCellProcessor translate) {
    if (translateList == null) {
      translateList = new ArrayList<>();
    }
    translateList.add(translate);
    this.translate = true;
    return this;
  }

  /**
   * Add a transform processor
   *
   * @param transform Transform processor
   * @return DataListBuilder
   */
  public DataListBuilder addPostTransform(TransformCellProcessor transform) {
    if (postTransformList == null) {
      postTransformList = new ArrayList<>();
    }
    postTransformList.add(transform);
    this.postTransform = true;
    return this;
  }

  /**
   * Add a translate processor
   *
   * @param translate Translate processor
   * @return DataListBuilder
   */
  public DataListBuilder addPostTranslate(TranslateCellProcessor translate) {
    if (postTranslateList == null) {
      postTranslateList = new ArrayList<>();
    }
    postTranslateList.add(translate);
    this.postTranslate = true;
    return this;
  }

  /**
   * Add a no print field
   *
   * @param noPrint No print field alias
   * @return DataListBuilder
   */
  public DataListBuilder addNoPrint(String noPrint) {
    if (noPrintList == null) {
      noPrintList = new ArrayList<>();
    }
    noPrintList.add(noPrint);
    this.noPrint = true;
    return this;
  }

  /**
   * Add a totalize processor
   *
   * @param totalize Totalize processor
   * @return DataListBuilder
   */
  public DataListBuilder addTotalize(TotalizeColumnProcessor totalize) {
    if (totalizeList == null) {
      totalizeList = new ArrayList<>();
    }
    totalizeList.add(totalize);
    this.totalize = true;
    return this;
  }

  /**
   * Add a column to datalist
   *
   * @param columnId Column id
   * @param data     Column data
   * @param type     Column data type
   * @return DataListBuilder
   */
  public DataListBuilder addColumn(String columnId, List<?> data, String type) {
    if (columnList == null) {
      columnList = new HashMap<>();
    }

    // Generate a cell data list
    List<CellData> columnData = new ArrayList<>();
    for (Object value : data) {
      CellData cellData = new CellData(value).setType(CellDataType.valueOf(type));
      columnData.add(cellData);
    }
    columnList.put(columnId, columnData);
    return this;
  }

  /**
   * Manage pagination or not
   *
   * @param paginate Paginate
   * @return DataListBuilder
   */
  public DataListBuilder paginate(boolean paginate) {
    this.paginate = paginate;
    return this;
  }

  /**
   * Set datalist page
   *
   * @param page Page number
   * @return DataListBuilder
   */
  public DataListBuilder setPage(Long page) {
    this.page = page;
    return this;
  }

  /**
   * Set datalist max records per page
   *
   * @param max Max elements per page
   * @return DataListBuilder
   */
  public DataListBuilder setMax(Long max) {
    this.max = max;
    return this;
  }

  /**
   * Set datalist records
   *
   * @param records Total records
   * @return DataListBuilder
   */
  public DataListBuilder setRecords(Long records) {
    this.records = records;
    return this;
  }

  /**
   * Get datalist page
   *
   * @return records
   */
  private long getPage() {
    return this.page < 1 ? 1 : this.page;
  }

  /**
   * Get datalist records
   *
   * @return records
   */
  private long getRecords() {
    return this.records < 0 ? dataList.getRows().size() : this.records;
  }

  /**
   * Get datalist records
   *
   * @return records
   */
  private long getTotalPages() {
    return Math.max(max <= 0 ? 1 : (long) Math.ceil((double) getRecords() / max), 1);
  }

  /**
   * Set datalist max records per page
   *
   * @param sortList Sort field list
   * @return DataListBuilder
   */
  public DataListBuilder sort(List<SortColumn> sortList) {
    this.sortList = sortList;
    this.sort = sortList != null && !sortList.isEmpty();
    return this;
  }

  /**
   * Filter datalist
   *
   * @param column Column name
   * @param value  filter value
   * @return DataListBuilder
   */
  public DataListBuilder filter(String column, String value) {
    if (this.filterList == null) {
      this.filterList = new ArrayList<>();
    }
    this.filterList.add(new FilterColumn(column, value));
    this.filter = true;
    return this;
  }

  /**
   * Filter datalist
   *
   * @param filterList Filter list
   * @return DataListBuilder
   */
  public DataListBuilder filter(List<FilterColumn> filterList) {
    this.filterList = filterList;
    this.filter = filterList != null && !filterList.isEmpty();
    return this;
  }

  /**
   * Set datalist max records per page
   *
   * @param distinctList Sort field list
   * @return DataListBuilder
   */
  public DataListBuilder distinct(List<SortColumn> distinctList) {
    this.distinctList = distinctList;
    this.distinct = distinctList != null && !distinctList.isEmpty();
    return this;
  }

  /**
   * Generate identifiers
   *
   * @return DataListBuilder
   */
  public DataListBuilder generateIdentifiers() {
    this.identifier = true;
    return this;
  }

  /**
   * Totalize the datalist
   *
   * @param totalizeList Totalize list
   * @return DataListBuilder
   */
  public DataListBuilder totalize(List<TotalizeColumnProcessor> totalizeList) {
    this.totalizeList = totalizeList;
    this.totalize = totalizeList != null && !totalizeList.isEmpty();
    return this;
  }

  /**
   * Build datalist
   *
   * @return DataListBuilder
   * @throws AWException Error building datalist
   */
  public DataList build() throws AWException {

    // Get build data
    extractData();

    // Transform data
    transformData();

    // Set page, records and total
    dataList.setPage(getPage());
    dataList.setRecords(getRecords());
    dataList.setTotal(getTotalPages());

    return dataList;
  }

  /**
   * Extract data
   */
  private void extractData() {
    dataList = dataList == null ? new DataList() : dataList;

    // Manage results from SQL
    if (queryResult != null) {
      generateFromQueryResult();
    }

    // Manage results from enumerated
    if (enumQueryResult != null) {
      generateFromEnumQueryResult();
    }

    // Manage results from services
    if (serviceQueryResult != null) {
      generateFromServiceQueryResult();
    }

    if (columnList != null) {
      generateFromColumnListResult();
    }

    // Generate the list
    if (dataListList != null) {
      doMerge();
    }
  }

  /**
   * Transform data
   *
   * @throws AWException Error building datalist
   */
  private void transformData() throws AWException {

    // Filter the list
    if (filter) {
      doFilter();
    }

    // Get distinct values
    if (distinct) {
      doDistinct();
    }

    // Sort the final list
    if (sort) {
      doSort();
    }

    // Totalize
    if (totalize) {
      // Pre process fields before totalize
      if (translate || transform || compute || compound) {
        doPreProcess();
      }

      // Totalize
      doTotalize();

      // Update records (new records on totalize)
      setRecords((long) dataList.getRows().size());
    }

    // Paginate if rows are more than max
    if (paginate) {
      doPaginate();
    }

    // Calculate transform, translate, identifiers and noPrints
    if (translate || transform || compute || compound || identifier || noPrint) {
      doPostProcess();
    }
  }

  /**
   * Generate datalist data from query result
   */
  private void generateFromQueryResult() {
    if (this.projection == null) {
      throw new NullPointerException(getLocale("ERROR_TITLE_NOT_DEFINED", "projection"));
    }

    // Retrieve column names from projection
    List<String> columnNames = new ArrayList<>();
    List<Expression<?>> columns = ((QTuple) projection).getArgs();
    for (Expression<?> columnOperation : columns) {
      if (columnOperation instanceof SimpleOperation) {
        List<Expression<?>> columnData = ((SimpleOperation<?>) columnOperation).getArgs();
        columnNames.add(columnData.get(columnData.size() - 1).toString());
      } else {
        columnNames.add(columnOperation.toString());
      }
    }

    // For each row, create a map and add it to the datalist
    for (Tuple tuple : queryResult) {
      Map<String, CellData> row = new HashMap<>();
      for (int i = 0; i < columnNames.size(); i++) {
        row.put(columnNames.get(i), new CellData(tuple.get(i, Object.class)));
      }
      dataList.addRow(row);
    }
  }

  /**
   * Generate datalist data from enum query result
   */
  private void generateFromEnumQueryResult() {
    Integer rowNumber = 1;
    for (Global option : enumQueryResult) {

      Map<String, CellData> row = new HashMap<>();
      row.put(DATALIST_IDENTIFIER, new CellData(rowNumber));

      if (option.getLabel() != null) {
        row.put(JSON_LABEL_PARAMETER, new CellData(option.getLabel()));
      }

      if (option.getValue() != null) {
        row.put(JSON_VALUE_PARAMETER, new CellData(option.getValue()));
      }

      dataList.addRow(row);
      rowNumber++;
    }
    dataList.setRecords(enumQueryResult.size());
  }

  /**
   * Generate datalist data from a column list
   */
  private void generateFromColumnListResult() {
    List<Map<String, CellData>> rowList = new ArrayList<>();
    for (Map.Entry<String, List<CellData>> columnListEntry : columnList.entrySet()) {
      int rowIndex = 0;
      for (CellData cellData : columnListEntry.getValue()) {
        Map<String, CellData> row;

        // Add row if it doesn't exist, else retrieve it
        if (rowList.size() <= rowIndex) {
          row = new HashMap<>();
          rowList.add(row);
        } else {
          row = rowList.get(rowIndex);
        }

        // Add cell to row
        row.put(columnListEntry.getKey(), cellData);
        rowIndex++;
      }
    }
    dataList.setRows(rowList);
    dataList.setRecords(rowList.size());
  }

  /**
   * Generate datalist data from service query result
   */
  private void generateFromServiceQueryResult() {
    int totalRows = 0;
    int row;
    if (serviceQueryResult != null) {
      // Generate virtual field if is null (value)
      if (fieldList == null) {
        fieldList = new ArrayList<>();
        Field field = new Field();
        field.setId("value");
        fieldList.add(field);
      }

      // Calculate total records
      if (this.records < 0) {
        setRecords((long) (serviceQueryResult.size() / fieldList.size()));
      }

      // Calculate total rows
      totalRows = (int) (records < 0 ? records : serviceQueryResult.size() / fieldList.size());
    }

    // Fill partial list
    for (row = 1; row <= totalRows; row++) {
      // Add row
      dataList.addRow(generateFieldValues(row));
    }
    dataList.setRecords(getRecords());
  }

  /**
   * Generates the field values
   *
   * @param rowIndex Row number
   * @return Row data
   */
  private HashMap<String, CellData> generateFieldValues(Integer rowIndex) {
    // Variable definition */
    HashMap<String, CellData> row = new HashMap<>();
    int columnIndex;
    int totalColumns = fieldList.size();

    // Generate row ID
    row.put(DATALIST_IDENTIFIER, new CellData(rowIndex));

    // Generate field values
    for (columnIndex = 0; columnIndex < totalColumns; columnIndex++) {

      // Store field value
      SqlField field = fieldList.get(columnIndex);
      String nom = field.getIdentifier();
      String value = serviceQueryResult.get(((rowIndex - 1) * totalColumns) + columnIndex);

      // Format output data
      CellData cell = new CellData(value);

      // Add cell
      row.put(nom, cell);
    }

    return row;
  }

  /**
   * Merge datalists
   */
  private void doMerge() {
    this.records = 0L;
    for (DataList list : dataListList) {
      // Get all rows from all lists
      dataList.getRows().addAll(list.getRows());
      this.records += list.getRecords();
    }
  }

  /**
   * Sort datalist
   */
  private void doSort() {
    // Set rows and records
    dataList.setRows(new SortRowProcessor().setSortList(sortList).process(dataList.getRows()));
  }

  /**
   * Remove the rows whose column value is distinct to the value
   */
  private void doFilter() {
    dataList.setRows(new FilterRowProcessor().setFilterList(filterList).process(dataList.getRows()));
  }

  /**
   * Keeps only distinct values of given fields
   */
  private void doDistinct() {
    // Set rows and records
    dataList.setRows(new DistinctRowProcessor().setDistinctList(distinctList).process(dataList.getRows()));
  }

  /**
   * Keeps only distinct values of given fields
   */
  private void doTotalize() throws AWException {
    // Set rows and records
    dataList.setRows(new TotalizeRowProcessor().setTotalizeList(totalizeList).process(dataList.getRows()));
  }

  /**
   * Paginate results
   */
  private void doPaginate() {
    List<Map<String, CellData>> rows = dataList.getRows();
    if (max > 0 && rows.size() > max) {
      // Avoid off bounds
      page = Math.min(page, getTotalPages());
      page = page < 1 ? 1 : page;

      // Calculate start and end rows
      int startRow = (int) ((page - 1) * max);
      int endRow = (int) Math.min(page * max, rows.size());

      // Retrieve sublist
      dataList.setRows(rows.stream().skip(startRow).limit((long) endRow - startRow).toList());
    }
  }

  /**
   * Translate, transform, compute and compound before totalization
   */
  private void doPreProcess() throws AWException {
    for (Map<String, CellData> row : dataList.getRows()) {
      translateTransformComputeCompound(row);
    }
  }

  /**
   * Apply translate, transform, compute and compounds
   * @param row
   * @throws AWException
   */
  private void translateTransformComputeCompound(Map<String, CellData> row) throws AWException {
    // Translate rows
    if (translate) {
      doTranslates(row, translateList);
    }

    // Transform rows
    if (transform) {
      doTransforms(row, transformList);
    }

    // Calculate computes for this row
    if (compute) {
      doComputes(row);
    }

    // Calculate compounds for this row
    if (compound) {
      doCompounds(row);
    }
  }

  /**
   * Translate, transform, noPrint and identifiers
   */
  private void doPostProcess() throws AWException {
    AtomicInteger rowIndex = new AtomicInteger(1);
    for (Map<String, CellData> row : dataList.getRows()) {
      boolean hasIdentifier = row.containsKey(DATALIST_IDENTIFIER);

      // Translate and transform on new added rows
      doEvaluate(row);

      // Remove no print fields
      if (noPrint) {
        doNoPrint(row);
      }

      // Generate identifier only if not generated previously
      if (identifier && !hasIdentifier) {
        row.put(DATALIST_IDENTIFIER, new CellData(rowIndex.getAndIncrement()));
      }
    }
  }

  /**
   * Evaluate depending on new added rows
   * @param row New added row
   * @throws AWException
   */
  private void doEvaluate(Map<String, CellData> row) throws AWException {
    boolean isNewAddedRow = row.containsKey(DATALIST_NEW_ADDED_ROW);

    if (isNewAddedRow) {
      // Translate data for this row
      if (translate) {
        doTranslates(row, translateList);
      }

      // Transform data for this row
      if (transform) {
        doTransforms(row, transformList);
      }

      // Remove new added flag
      row.remove(DATALIST_NEW_ADDED_ROW);
    } else if (!totalize) {
      // Translate rows
      translateTransformComputeCompound(row);
    }

    // Translate data for this row on computed and compounds
    if (postTranslate) {
      doTranslates(row, postTranslateList);
    }

    // Transform data for this row on computed and compounds
    if (postTransform) {
      doTransforms(row, postTransformList);
    }
  }

  /**
   * Generate compute columns
   *
   * @param row Row to process
   */
  private void doComputes(Map<String, CellData> row) throws AWException {
    for (ComputedColumnProcessor computed : computedList) {
      // Process computed
      CellData cell = computed.process(row);
      row.put(computed.getColumnIdentifier(), cell);
    }
  }

  /**
   * Generate transformations on columns
   *
   * @param row Row to process
   */
  private void doTransforms(Map<String, CellData> row, List<TransformCellProcessor> list) throws AWException {
    for (TransformCellProcessor processor : list) {
      // Process transform
      row.put(processor.getColumnIdentifier(), processor.process(row.get(processor.getColumnIdentifier())));
    }
  }

  /**
   * Generate translations on columns
   *
   * @param row Row to process
   */
  private void doTranslates(Map<String, CellData> row, List<TranslateCellProcessor> list) throws AWException {
    for (TranslateCellProcessor processor : list) {
      // Process translate
      if (processor.getTranslateEnumerated() == null) {
        throw new AWException(getLocale("ERROR_MESSAGE_ENUMERATED_NOT_DEFINED", processor.getField().getTranslate()));
      }
      row.put(processor.getColumnIdentifier(), processor.process(row.get(processor.getColumnIdentifier())));
    }
  }

  /**
   * Generate compound columns
   *
   * @param row Row to process
   */
  private void doCompounds(Map<String, CellData> row) throws AWException {
    for (CompoundColumnProcessor processor : compoundList) {
      // Process compound
      CellData cell = processor.process(row);
      row.put(processor.getColumnIdentifier(), cell);
    }
  }

  /**
   * Remove noPrint fields
   *
   * @param row Row to process
   */
  private void doNoPrint(Map<String, CellData> row) {
    for (String columnId : noPrintList) {
      // Process compound
      row.remove(columnId);
    }
  }
}