package com.almis.awe.service.data.connector.query;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWEQueryException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ColumnFilter;
import com.almis.awe.model.dto.ColumnValues;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.queries.*;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.NumericService;
import com.almis.awe.service.data.builder.DataListBuilder;
import com.almis.awe.service.data.builder.QueryBuilder;
import com.almis.awe.service.data.processor.*;
import com.almis.awe.service.data.processor.ColumnValuesProcessor.ColumnValueGroup;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AbstractQueryConnector Class
 * <p>
 * Abstract class that all database-related query launcher should extend
 * </p>
 *
 * @author Jorge BELLON 24-02-2017
 */
public abstract class AbstractQueryConnector extends ServiceConfig implements QueryConnector {

  // Groups read at most to search the labels of a column whose label is not the value: beyond them the response
  // states that more values exist
  protected static final int COLUMN_VALUES_SEARCH_LIMIT = 10000;
  private static final String ERROR_TITLE_GENERATING_FILTER = "ERROR_TITLE_GENERATING_FILTER";
  private static final String ERROR_MESSAGE_GENERATING_FILTER = "ERROR_MESSAGE_GENERATING_FILTER";

  // Autowired services
  private final QueryUtil queryUtil;
  private final BaseConfigProperties baseConfigProperties;
  private final AweElements elements;
  private final NumericService numericService;
  private final EncodeService encodeService;
  private final ObjectMapper mapper;

  /**
   * Autowired constructor
   *
   * @param queryUtil            Query utilities
   * @param baseConfigProperties Base config properties
   * @param elements             AWE elements
   * @param numericService       Numeric service
   * @param encodeService        Encode service
   * @param mapper               Object mapper
   */
  protected AbstractQueryConnector(QueryUtil queryUtil, BaseConfigProperties baseConfigProperties, AweElements elements,
                                   NumericService numericService, EncodeService encodeService, ObjectMapper mapper) {
    this.queryUtil = queryUtil;
    this.baseConfigProperties = baseConfigProperties;
    this.elements = elements;
    this.numericService = numericService;
    this.encodeService = encodeService;
    this.mapper = mapper;
  }

  /**
   * Build results
   *
   * @param builder Builder
   * @param query   Query launched
   * @return Final output
   * @throws AWException error generating results
   */
  protected ServiceData buildResults(QueryBuilder builder, Query query) throws AWException {
    ServiceData result;
    try {
      // Launch query
      result = (ServiceData) builder.setQuery(query).build();
    } catch (AWException exc) {
      throw exc;
    } catch (Exception exc) {
      throw new AWEQueryException(getLocale("ERROR_TITLE_LAUNCHING_SQL_QUERY"), exc.getMessage(), query.getId(), exc);
    }
    return result;
  }

  /**
   * Generate datalist result
   *
   * @param result       Output
   * @param query        Query launched
   * @param parameterMap Parameters
   * @return Final output
   * @throws AWException error generating results
   */
  protected ServiceData generateResults(ServiceData result, Query query, Map<String, QueryParameter> parameterMap) throws AWException {
    DataList datalist;
    try {
      // Generate datalist
      datalist = fillDataList(result, query, parameterMap);
    } catch (AWException exc) {
      throw exc;
    } catch (Exception exc) {
      throw new AWException(getLocale("ERROR_TITLE_ERROR_EXECUTING_SERVICE_QUERY"),
        getLocale("ERROR_MESSAGE_EXECUTING_SERVICE_QUERY", query.getId()), exc);
    }
    // fillDataList already tolerates a null result by returning an empty DataList (a subscription may
    // deliver no data at all), so honour that contract here instead of dereferencing a null result
    ServiceData output = result == null ? new ServiceData() : result;
    output.setDataList(datalist);
    return output;
  }

  /**
   * Retrieves complete data list with totals
   *
   * @param serviceData  ServiceData
   * @param query        Query
   * @param parameterMap Parameters
   * @return Complete data list with totals
   * @throws AWException Complete list generation error
   */
  protected DataList fillDataList(ServiceData serviceData, Query query, Map<String, QueryParameter> parameterMap) throws AWException {

    // If there's no result, return an empty DataList
    if (serviceData == null) {
      return new DataList();
    }

    // Recover builder's instance
    DataListBuilder builder = getDataListBuilder(serviceData, query, parameterMap);

    // Add transformations & translations
    builder = processDataList(builder, query, parameterMap);

    // Filter datalist by the grid column filters
    builder = filterDataList(builder, query, parameterMap);

    // Sort datalist
    builder = sortDataList(builder, parameterMap);

    // Return complete result
    return builder.build();
  }

  /**
   * Retrieve the builder of the data list of a service result, with its rows and pagination
   *
   * @param serviceData  ServiceData
   * @param query        Query
   * @param parameterMap Parameters
   * @return Data list builder
   */
  private DataListBuilder getDataListBuilder(ServiceData serviceData, Query query, Map<String, QueryParameter> parameterMap) {
    DataListBuilder builder = getBean(DataListBuilder.class);

    // Define pagination
    builder.paginate(!query.isPaginationManaged());

    // Check output type
    builder.generateIdentifiers();
    DataList serviceDataList = serviceData.getDataList();
    if (serviceDataList != null) {
      if (query.getSqlFieldList() != null) {
        // As new datalist
        DataList serviceFieldsDataList = new DataList();
        query.getFieldList().forEach(field -> DataListUtil.copyColumn(serviceFieldsDataList, Optional.ofNullable(field.getAlias()).orElse(field.getId()), serviceDataList, field.getId()));
        builder.setDataList(serviceFieldsDataList)
          .setFieldList(query.getSqlFieldList())
          .setMax(parameterMap.get(AweConstants.QUERY_MAX).getValue().asLong(0L))
          .setPage(parameterMap.get(AweConstants.QUERY_PAGE).getValue().asLong(0L))
          .setRecords(serviceDataList.getRecords());
      } else {
        builder.setDataList(serviceDataList)
          .setMax(parameterMap.get(AweConstants.QUERY_MAX).getValue().asLong(0L))
          .setPage(parameterMap.get(AweConstants.QUERY_PAGE).getValue().asLong(0L))
          .setRecords(serviceDataList.getRecords());
      }
    } else {
      // As string array
      builder.setServiceQueryResult((String[]) serviceData.getData())
        .setFieldList(query.getSqlFieldList())
        .setMax(parameterMap.get(AweConstants.QUERY_MAX).getValue().asLong(0L))
        .setPage(parameterMap.get(AweConstants.QUERY_PAGE).getValue().asLong(0L));
    }
    return builder;
  }

  @Override
  public ServiceData launchColumnValues(Query query, ObjectNode parameters, ColumnValues columnValues) throws AWException {
    // A service that manages its pagination does not return all its rows
    if (query.isPaginationManaged()) {
      throw new AWEQueryException(getLocale(ERROR_TITLE_GENERATING_FILTER), getLocale(ERROR_MESSAGE_GENERATING_FILTER,
        "the service of the query manages its pagination: its column values are not available"), query.getId(), null);
    }

    // Reject invalid grid column filters before the rows are read
    validateColumnFilters(query, parameters);

    // Group the rows that match the other column filters, before any processing
    Map<String, QueryParameter> variableMap = getQueryUtil().getVariableMap(query, parameters);
    List<ColumnValueGroup> groups = ColumnValuesProcessor.group(getColumnValuesRows(query, parameters, variableMap).getRows(), columnValues);
    return getColumnValuesResult(query, columnValues, groups, variableMap, false, false);
  }

  /**
   * Retrieve every row of the query that matches the column filters of the parameters, without processing, sort nor
   * pagination
   *
   * @param query      Query
   * @param parameters Parameters
   * @param variables  Query variables
   * @return Rows
   * @throws AWException The connector does not list column values, or the query failed
   */
  protected DataList getColumnValuesRows(Query query, ObjectNode parameters, Map<String, QueryParameter> variables) throws AWException {
    throw new AWEQueryException(getLocale(ERROR_TITLE_GENERATING_FILTER), getLocale(ERROR_MESSAGE_GENERATING_FILTER,
      "column values are not supported by the connector of the query"), query.getId(), null);
  }

  /**
   * Retrieve the rows of a service result that match the column filters, without processing, sort nor pagination
   *
   * @param serviceData Service result
   * @param query       Query
   * @param variables   Query variables
   * @return Rows
   * @throws AWException Error filtering the rows
   */
  protected DataList getFilteredRows(ServiceData serviceData, Query query, Map<String, QueryParameter> variables) throws AWException {
    if (serviceData == null) {
      return new DataList();
    }
    return filterDataList(getDataListBuilder(serviceData, query, variables).paginate(false), query, variables).build();
  }

  /**
   * Build the column values response from the groups of the filtered field values: computes the label of each value
   * from its representative row with the processing of the column, then applies the search and the cap
   *
   * @param query        Query
   * @param columnValues Column values
   * @param groups       Groups, sorted
   * @param variables    Query variables
   * @param searched     The search was already applied to the groups
   * @param truncated    The groups are not all the values of the column
   * @return Service data with the values and whether more exist
   * @throws AWException Error computing the labels
   */
  protected ServiceData getColumnValuesResult(Query query, ColumnValues columnValues, List<ColumnValueGroup> groups,
                                              Map<String, QueryParameter> variables, boolean searched, boolean truncated) throws AWException {
    List<String> labels = new ArrayList<>();
    if (ColumnValuesProcessor.isLabelTheValue(query, columnValues)) {
      groups.forEach(group -> labels.add(group.value() == null ? null : new CellData(group.value()).getStringValue()));
    } else {
      labels.addAll(getLabels(ColumnValuesProcessor.getLabelQuery(query, columnValues.getId()), columnValues.getId(), groups, variables));
    }
    return ColumnValuesProcessor.getResult(groups, labels, searched ? null : columnValues.getSearch(), truncated);
  }

  /**
   * Compute the label of each group: the column of its representative row after the processing of the label query
   *
   * @param labelQuery Query with the fields and processing the column needs
   * @param column     Column
   * @param groups     Groups
   * @param variables  Query variables
   * @return Labels, null for the empty values
   * @throws AWException Error processing the rows
   */
  private List<String> getLabels(Query labelQuery, String column, List<ColumnValueGroup> groups, Map<String, QueryParameter> variables) throws AWException {
    DataList rows = new DataList();
    groups.forEach(group -> rows.addRow(new HashMap<>(group.row())));
    DataList labelled = processDataList(getBean(DataListBuilder.class).setDataList(rows), labelQuery, variables).build();
    List<String> labels = new ArrayList<>();
    for (int index = 0; index < groups.size(); index++) {
      CellData label = labelled.getRows().get(index).get(column);
      labels.add(groups.get(index).value() == null || label == null ? null : label.getStringValue());
    }
    return labels;
  }

  /**
   * Filter data list by the grid column filters, unless the service manages the pagination (then the request
   * parameter reaches the service unchanged)
   *
   * @param builder   DataListBuilder
   * @param query     Query
   * @param variables Query variables
   * @return Builder
   * @throws AWException The column filters are not valid for the query
   */
  protected DataListBuilder filterDataList(DataListBuilder builder, Query query, Map<String, QueryParameter> variables) throws AWException {
    if (query != null && !query.isPaginationManaged() && variables.containsKey(AweConstants.QUERY_COLUMN_FILTERS)) {
      builder.columnFilter(getColumnFilters(query, variables.get(AweConstants.QUERY_COLUMN_FILTERS).getValue()));
    }

    return builder;
  }

  /**
   * Check the grid column filters of the request before running the query, unless the service manages the pagination
   *
   * @param query      Query
   * @param parameters Request parameters
   * @throws AWException The column filters are not valid for the query
   */
  protected void validateColumnFilters(Query query, ObjectNode parameters) throws AWException {
    if (!query.isPaginationManaged()) {
      getColumnFilters(query, getQueryUtil().getColumnFiltersParameter(query, parameters));
    }
  }

  /**
   * Parse the grid column filters and check their fields against the query
   *
   * @param query         Query
   * @param columnFilters Column filters parameter
   * @return Column filters
   * @throws AWException The column filters are not valid for the query
   */
  private List<ColumnFilter> getColumnFilters(Query query, JsonNode columnFilters) throws AWException {
    List<ColumnFilter> columnFilterList = getQueryUtil().getColumnFilterList(columnFilters);
    for (ColumnFilter columnFilter : columnFilterList) {
      getQueryUtil().getColumnFilterFields(query, columnFilter);
    }
    return columnFilterList;
  }

  /**
   * Sort data list
   *
   * @param builder   DataListBuilder
   * @param variables Query variables
   * @return Builder
   * @throws AWException Processing failed
   */
  protected DataListBuilder sortDataList(DataListBuilder builder, Map<String, QueryParameter> variables) throws AWException {
    // Mount sort list if not null
    if (variables.containsKey(AweConstants.QUERY_SORT)) {
      ArrayNode sortList = (ArrayNode) variables.get(AweConstants.QUERY_SORT).getValue();

      // Sort builder
      builder.sort(getQueryUtil().getSortList(sortList));
    }

    return builder;
  }

  /**
   * Process dataList
   *
   * @param builder   DataListBuilder
   * @param query     Query
   * @param variables Query variables
   * @return Builder
   * @throws AWException Processing failed
   */
  protected DataListBuilder processDataList(DataListBuilder builder, Query query, Map<String, QueryParameter> variables) throws AWException {
    // Add transformations & translations
    if (query.getSqlFieldList() != null) {
      for (SqlField field : query.getSqlFieldList()) {
        addFieldTransformations(field, builder, variables);
      }
    }

    // Add computed fields
    if (query.getComputedList() != null) {
      for (Computed computed : query.getComputedList()) {
        addComputedTransformations(computed, builder, variables);
      }
    }

    // Add compound fields
    if (query.getCompoundList() != null) {
      for (Compound compound : query.getCompoundList()) {
        builder.addCompound(new CompoundColumnProcessor(elements, baseConfigProperties, compound, variables, numericService, encodeService, mapper));
      }
    }

    // Add totalizators
    if (query.getTotalizeList() != null) {
      for (Totalize totalize : query.getTotalizeList()) {
        TotalizeColumnProcessor totalizeProcessor = new TotalizeColumnProcessor(elements, numericService);
        totalizeProcessor.setFieldList(query.getSqlFieldList()).setTotalize(totalize);
        builder.addTotalize(totalizeProcessor);
      }
    }

    return builder;
  }

  /**
   * Add field transformations to the builder
   *
   * @param field     Field
   * @param builder   Builder
   * @param variables Variable map
   * @throws AWException AWE exception
   */
  private void addFieldTransformations(OutputField field, DataListBuilder builder, Map<String, QueryParameter> variables) throws AWException {
    boolean isComputedOrCompound = field instanceof Computed || field instanceof Compound;

    // Check transformations
    if (field.isTransform() && !isComputedOrCompound) {
      TransformCellProcessor transformProcessor = new TransformCellProcessor(elements, field, numericService, encodeService);
      builder.addTransform(transformProcessor);
    }

    // Check translations
    if (field.isTranslate() && !isComputedOrCompound) {
      TranslateCellProcessor translateProcessor = new TranslateCellProcessor(elements, field, variables, elements.getEnumerated(field.getTranslate()));
      builder.addTranslate(translateProcessor);
    }

    // Check transformations
    if (field.isTransform() && isComputedOrCompound) {
      TransformCellProcessor transformProcessor = new TransformCellProcessor(elements, field, numericService, encodeService);
      builder.addPostTransform(transformProcessor);
    }

    // Check translations
    if (field.isTranslate() && isComputedOrCompound) {
      TranslateCellProcessor translateProcessor = new TranslateCellProcessor(elements, field, variables, elements.getEnumerated(field.getTranslate()));
      builder.addPostTranslate(translateProcessor);
    }

    // Check no print
    if (field.isNoprint()) {
      builder.addNoPrint(field.getIdentifier());
    }
  }

  /**
   * Add computed transformations to the builder
   *
   * @param computed  Computed
   * @param builder   Builder
   * @param variables Variables
   * @throws AWException AWE exception
   */
  private void addComputedTransformations(Computed computed, DataListBuilder builder, Map<String, QueryParameter> variables) throws AWException {
    // Add computed
    builder.addComputed(new ComputedColumnProcessor(elements, baseConfigProperties, computed, variables, numericService, encodeService));

    // Add no print
    if (computed.isNoprint()) {
      builder.addNoPrint(computed.getIdentifier());
    }
  }

  /**
   * Get query util
   *
   * @return Query util
   */
  @Override
  public QueryUtil getQueryUtil() {
    return queryUtil;
  }
}
