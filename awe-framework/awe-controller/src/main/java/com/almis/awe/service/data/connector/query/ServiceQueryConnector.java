package com.almis.awe.service.data.connector.query;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.log.LogUtil;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.NumericService;
import com.almis.awe.service.data.builder.ServiceBuilder;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * EnumQueryConnector Class
 * Connection class between QueryLauncher and EnumBuilder
 *
 * @author Jorge BELLON 27-03-2017
 */
@Slf4j
public class ServiceQueryConnector extends AbstractQueryConnector {

  /**
   * Autowired constructor
   *
   * @param queryUtil            Query utilities
   * @param baseConfigProperties Base configuration properties
   * @param elements             AWE elements
   * @param numericService       Numeric service
   * @param encodeService        Encode service
   */
  public ServiceQueryConnector(QueryUtil queryUtil, BaseConfigProperties baseConfigProperties, AweElements elements, NumericService numericService, EncodeService encodeService) {
    super(queryUtil, baseConfigProperties, elements, numericService, encodeService);
  }

  @Override
  public ServiceData launch(Query query, ObjectNode parameters) throws AWException {

    // Log start query prepare time
    List<Long> timeLapse = LogUtil.prepareTimeLapse();

    // Get query builder
    ServiceBuilder builder = getBean(ServiceBuilder.class);
    builder.setParameters(parameters);

    // Get query preparation time
    LogUtil.checkpoint(timeLapse);

    // Query launch
    ServiceData result = buildResults(builder, query);

    // Get elapsed query time
    LogUtil.checkpoint(timeLapse);

    // Process and generate results
    if (query.isPostProcessed()) {
      Map<String, QueryParameter> variableMap = getQueryUtil().getVariableMap(query, parameters);
      result = generateResults(result, query, variableMap);
    }

    // Get elapsed datalist time
    LogUtil.checkpoint(timeLapse);

    // Log query
    log.info("[{}] =>  {} records. Prepare service time: {}s - Service time: {}s - Datalist time: {}s - Total time: {}s",
      query.getService(),
      result.getDataList().getRecords(),
      LogUtil.getElapsed(timeLapse, AweConstants.PREPARATION_TIME),
      LogUtil.getElapsed(timeLapse, AweConstants.EXECUTION_TIME),
      LogUtil.getElapsed(timeLapse, AweConstants.RESULTS_TIME),
      LogUtil.getTotalTime(timeLapse));
    return result;
  }

  @Override
  public ServiceData subscribe(Query query, ComponentAddress address, ObjectNode parameters) throws AWException {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
