package com.almis.awe.service.data.connector.maintain;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWEQueryException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.maintain.MaintainQuery;
import com.almis.awe.model.entities.queries.DatabaseConnection;
import com.almis.awe.model.entities.queries.Variable;
import com.almis.awe.model.entities.services.ServiceInputParameter;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.log.LogUtil;
import com.almis.awe.service.data.builder.ServiceBuilder;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintain connector for services
 */
@Slf4j
public class ServiceMaintainConnector extends ServiceConfig implements MaintainConnector {

  @Override
  public <T extends MaintainQuery> ServiceData launch(T query, DatabaseConnection connection, ObjectNode parameters) throws AWException {

    // Log start query prepare time
    List<Long> timeLapse = LogUtil.prepareTimeLapse();

    // Get query builder
    ServiceBuilder builder = getBean(ServiceBuilder.class);

    // Prepare variables
    List<ServiceInputParameter> serviceParameters = getServiceParameters(query.getService());
    Map<String, QueryParameter> variableMap = remapVariablesByServiceContract(
        query,
        getBean(QueryUtil.class).getVariableMap(query, parameters, serviceParameters),
        serviceParameters);
    builder
      .setQuery(query)
      .setParameters(parameters)
      .setVariables(variableMap);

    // Get query preparation time
    LogUtil.checkpoint(timeLapse);

    ServiceData result;
    try {
      // Launch service
      result = builder.build();
      LogUtil.checkpoint(timeLapse);

      // Log query
      log.info("[{}] => Prepare service time: {}s - Service time: {}s - {}",
        query.getService(),
        LogUtil.getElapsed(timeLapse, AweConstants.PREPARATION_TIME),
        LogUtil.getElapsed(timeLapse, AweConstants.EXECUTION_TIME),
        LogUtil.getTotalTime(timeLapse));

    } catch (AWException exc) {
      throw exc;
    } catch (Exception exc) {
      throw new AWEQueryException(getLocale("ERROR_TITLE_LAUNCHING_MAINTAIN"), exc.getMessage(), query.getId(), exc);
    }
    return result;
  }

  private List<ServiceInputParameter> getServiceParameters(String serviceId) throws AWException {
    return getElements().getService(serviceId).getType().getParameterList() == null
        ? Collections.emptyList()
        : getElements().getService(serviceId).getType().getParameterList();
  }

  private Map<String, QueryParameter> remapVariablesByServiceContract(MaintainQuery query,
                                                                      Map<String, QueryParameter> variableMap,
                                                                      List<ServiceInputParameter> serviceParameters) throws AWException {
    Map<String, QueryParameter> remappedVariableMap = new LinkedHashMap<>();
    List<Variable> variables = query.getVariableDefinitionList() == null ? Collections.emptyList() : query.getVariableDefinitionList();

    // Bind each variable value under its service parameter name, matching strictly by name (see
    // QueryUtil#pairVariablesToServiceContract). Order is irrelevant and never guessed positionally;
    // a service parameter not informed by the maintain raises an AWException.
    getBean(QueryUtil.class).pairVariablesToServiceContract(variables, serviceParameters)
        .forEach((variableId, serviceParameter) -> {
          String serviceParameterName = serviceParameter.getName();
          if (StringUtils.isNotBlank(serviceParameterName) && variableMap.containsKey(variableId)) {
            remappedVariableMap.put(serviceParameterName, variableMap.get(variableId));
          }
        });

    // Preserve any variable values not bound to a service parameter (defaults, extra variables)
    variableMap.forEach(remappedVariableMap::putIfAbsent);

    return remappedVariableMap;
  }
}
