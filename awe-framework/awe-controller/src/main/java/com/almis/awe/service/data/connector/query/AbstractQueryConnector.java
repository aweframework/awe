package com.almis.awe.service.data.connector.query;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWEQueryException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.constant.AweConstants;
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
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Map;
import java.util.Optional;

/**
 * AbstractQueryConnector Class
 * <p>
 * Abstract class that all database-related query launcher should extend
 *
 * @author Jorge BELLON 24-02-2017
 */
public abstract class AbstractQueryConnector extends ServiceConfig implements QueryConnector {

  // Autowired services
  private final QueryUtil queryUtil;
  private final BaseConfigProperties baseConfigProperties;
  private final AweElements elements;
  private final NumericService numericService;
  private final EncodeService encodeService;

  /**
   * Autowired constructor
   *
   * @param queryUtil            Query utilities
   * @param baseConfigProperties Base config properties
   * @param elements             AWE elements
   * @param numericService       Numeric service
   * @param encodeService        Encode service
   */
  protected AbstractQueryConnector(QueryUtil queryUtil, BaseConfigProperties baseConfigProperties, AweElements elements, NumericService numericService, EncodeService encodeService) {
    this.queryUtil = queryUtil;
    this.baseConfigProperties = baseConfigProperties;
    this.elements = elements;
    this.numericService = numericService;
    this.encodeService = encodeService;
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
    result.setDataList(datalist);
    return result;
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
          .setMax(parameterMap.get(AweConstants.QUERY_MAX).getValue().asLong())
          .setPage(parameterMap.get(AweConstants.QUERY_PAGE).getValue().asLong())
          .setRecords(serviceDataList.getRecords());
      } else {
        builder.setDataList(serviceDataList)
          .setMax(parameterMap.get(AweConstants.QUERY_MAX).getValue().asLong())
          .setPage(parameterMap.get(AweConstants.QUERY_PAGE).getValue().asLong())
          .setRecords(serviceDataList.getRecords());
      }
    } else {
      // As string array
      builder.setServiceQueryResult((String[]) serviceData.getData())
        .setFieldList(query.getSqlFieldList())
        .setMax(parameterMap.get(AweConstants.QUERY_MAX).getValue().asLong())
        .setPage(parameterMap.get(AweConstants.QUERY_PAGE).getValue().asLong());
    }

    // Add transformations & translations
    builder = processDataList(builder, query, parameterMap);

    // Sort datalist
    builder = sortDataList(builder, parameterMap);

    // Return complete result
    return builder.build();
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
        builder.addCompound(new CompoundColumnProcessor(elements, baseConfigProperties, compound, variables, numericService, encodeService));
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
   * @param field   Field
   * @param builder Builder
   * @param variables Variable map
   * @throws AWException AWE exception
   */
  private void addFieldTransformations(OutputField field, DataListBuilder builder, Map<String, QueryParameter> variables) throws AWException {
    // Check transformations
    if (field.isTransform()) {
      TransformCellProcessor transformProcessor = new TransformCellProcessor(elements, field, numericService, encodeService);
      builder.addTransform(transformProcessor);
    }

    // Check translations
    if (field.isTranslate()) {
      TranslateCellProcessor translateProcessor = new TranslateCellProcessor(elements, field, variables, elements.getEnumerated(field.getTranslate()));
      builder.addTranslate(translateProcessor);
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
  public QueryUtil getQueryUtil() {
    return queryUtil;
  }
}
