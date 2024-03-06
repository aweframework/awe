package com.almis.awe.service.data.connector.query;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queues.Queue;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.log.LogUtil;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.NumericService;
import com.almis.awe.service.data.builder.QueueBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * QueueQueryConnector Class
 * Connection class between QueryLauncher and EnumBuilder
 *
 * @author Pablo GARCIA 25-07-2017
 */
@Slf4j
public class QueueQueryConnector extends AbstractQueryConnector {

  /**
   * Autowired constructor
   *
   * @param queryUtil            QueryUtil
   * @param baseConfigProperties Base configuration properties
   * @param elements             AWE element
   * @param numericService       Numeric service
   * @param encodeService        Encode service
   * @param mapper               Object mapper
   */
  public QueueQueryConnector(QueryUtil queryUtil, BaseConfigProperties baseConfigProperties, AweElements elements,
                             NumericService numericService, EncodeService encodeService, ObjectMapper mapper) {
    super(queryUtil, baseConfigProperties, elements, numericService, encodeService, mapper);
  }

  @Override
  public ServiceData launch(Query query, ObjectNode parameters) throws AWException {

    // Log start query prepare time
    List<Long> timeLapse = LogUtil.prepareTimeLapse();

    // Get query builder
    QueueBuilder builder = getBean(QueueBuilder.class);
    Queue queue = getElements().getQueue(query.getQueue()).copy();

    // Get query preparation time
    LogUtil.checkpoint(timeLapse);

    // Query launch
    builder.setQueue(queue);
    ServiceData result = buildResults(builder, query);

    // Get elapsed query time
    LogUtil.checkpoint(timeLapse);

    // Generate results
    Map<String, QueryParameter> variableMap = getQueryUtil().getVariableMap(query, parameters);
    result = generateResults(result, query, variableMap);

    // // Get elapsed datalist time
    LogUtil.checkpoint(timeLapse);

    // Log query
    log.info("[{}] =>  {} records. Prepare queue time: {}s - Queue time: {}s - Datalist time: {}s - {}",
      query.getId(),
      result.getDataList().getRecords(),
      LogUtil.getElapsed(timeLapse, AweConstants.PREPARATION_TIME),
      LogUtil.getElapsed(timeLapse, AweConstants.EXECUTION_TIME),
      LogUtil.getElapsed(timeLapse, AweConstants.RESULTS_TIME),
      LogUtil.getTotalTime(timeLapse));
    return result;
  }

  @Override
  public ServiceData subscribe(Query query, ComponentAddress address, ObjectNode parameters) throws AWException {

    // Log start query prepare time
    List<Long> timeLapse = LogUtil.prepareTimeLapse();

    // Get query builder
    QueueBuilder builder = getBean(QueueBuilder.class);
    Queue queue = getElements().getQueue(query.getQueue()).copy();

    // Get query preparation time
    LogUtil.checkpoint(timeLapse);

    // Query launch
    Map<String, QueryParameter> variableMap = getQueryUtil().getVariableMap(query, parameters);
    ServiceData result = builder.setQuery(query)
      .setQueue(queue)
      .setAddress(address)
      .setParameters(parameters)
      .setVariables(variableMap)
      .subscribe();

    // // Get elapsed datalist time
    LogUtil.checkpoint(timeLapse);

    // Log query
    log.info("[{}] =>  Prepare queue time: {}s - Queue time: {}s - {}",
      query.getId(),
      LogUtil.getElapsed(timeLapse, AweConstants.PREPARATION_TIME),
      LogUtil.getElapsed(timeLapse, AweConstants.EXECUTION_TIME),
      LogUtil.getTotalTime(timeLapse));
    return result;
  }

  /**
   * Manage subscription data
   *
   * @param query            Query
   * @param subscriptionData Subscription data
   * @param parameterMap     Parameter map
   * @return Service data with client actions
   * @throws AWException Error managing subscription data
   */
  public ServiceData onSubscriptionData(Query query, ServiceData subscriptionData, Map<String, QueryParameter> parameterMap) throws AWException {

    // Log start query prepare time
    List<Long> timeLapse = LogUtil.prepareTimeLapse();

    // Generate results
    ServiceData result = generateResults(subscriptionData, query, parameterMap);

    // // Get elapsed datalist time
    LogUtil.checkpoint(timeLapse);

    // Log query
    log.info("[{}] => Subscription data retrieved - Datalist time: {}s - {}",
      query.getId(),
      LogUtil.getElapsed(timeLapse, AweConstants.PREPARATION_TIME),
      LogUtil.getTotalTime(timeLapse));
    return result;
  }
}
