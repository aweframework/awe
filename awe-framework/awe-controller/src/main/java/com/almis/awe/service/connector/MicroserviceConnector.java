package com.almis.awe.service.connector;

import com.almis.awe.config.RestConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.services.ServiceInputParameter;
import com.almis.awe.model.entities.services.ServiceMicroservice;
import com.almis.awe.model.entities.services.ServiceType;
import com.almis.awe.model.rest.RestParameter;
import com.almis.awe.model.rest.ServiceAuth;
import com.almis.awe.model.rest.ServiceDetails;
import com.almis.awe.model.type.ParameterType;
import com.almis.awe.model.util.data.DateUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;

import java.util.*;

/**
 * Launches a Microservice
 */
@Slf4j
public class MicroserviceConnector extends AbstractRestConnector {


  private final QueryUtil queryUtil;
  private final RestConfigProperties restConfigProperties;

  /**
   * Autowired constructor
   * @param requestFactory Request factory
   * @param queryUtil Query Util
   * @param objectMapper Object mapper
   * @param restConfigProperties Microservice configuration properties
   */
  public MicroserviceConnector(ClientHttpRequestFactory requestFactory, QueryUtil queryUtil, ObjectMapper objectMapper, RestConfigProperties restConfigProperties) {
    super(requestFactory, objectMapper);
    this.queryUtil = queryUtil;
    this.restConfigProperties = restConfigProperties;
  }

  @Override
  public ServiceData launch(ServiceType service, Map<String, Object> paramsMapFromRequest) throws AWException {
    // Variable definition
    ServiceData outData;
    final Map<String, ServiceDetails> microservicesConfig = restConfigProperties.getServices();
    ServiceMicroservice microservice = (ServiceMicroservice) service;
    ServiceDetails serviceDetails = Optional.ofNullable(microservicesConfig.get(microservice.getName())).orElse(new ServiceDetails().setName(microservice.getName()));

    // Retrieve microservice url
    StringBuilder urlBuilder = new StringBuilder()
            .append(serviceDetails.getBaseUrl())
            .append(AweConstants.URL_SEPARATOR)
            .append(Optional.ofNullable(serviceDetails.getName()).orElse(microservice.getName()))
            .append(microservice.getEndpoint());

    // Retrieve microservice auth (if defined)
    ServiceAuth serviceAuth = serviceDetails.getAuthentication();
    if (serviceAuth != null) {
      microservice.setAuthentication(serviceAuth.getType());
      microservice.setUsername(serviceAuth.getUsername());
      microservice.setPassword(serviceAuth.getPassword());
    }

    // Fix date parameters
    fixDateParameterMap(Optional.ofNullable(microservice.getParameterList()).orElse(Collections.emptyList()), paramsMapFromRequest);

    // Add specific parameters to the microservice call
    addDefinedParameters(microservice, paramsMapFromRequest);

    // Create request to microservice
    try {
      outData = doRequest(urlBuilder.toString(), microservice, paramsMapFromRequest);
    } catch (RestClientException exc) {
      throw new AWException(getLocale("ERROR_TITLE_INVALID_CONNECTION"),
        getLocale("ERROR_MESSAGE_CONNECTION_MICROSERVICE", microservice.getName()), exc);
    }

    // Check service response
    checkServiceResponse(outData);

    return outData;
  }

  /**
   * Read defined parameters from properties and add them to the parameter map
   *
   * @param microservice         Microservice
   * @param paramsMapFromRequest Parameter map
   */
  private void addDefinedParameters(ServiceMicroservice microservice, Map<String, Object> paramsMapFromRequest) {
    // Read session parameters
    final Map<String, ServiceDetails> microservicesConfig = restConfigProperties.getServices();
    ServiceDetails serviceDetails = Optional.ofNullable(microservicesConfig.get(microservice.getName())).orElse(new ServiceDetails().setName(microservice.getName()));
    Optional.ofNullable(serviceDetails.getParameters()).orElse(Collections.emptyList())
      .forEach(parameter -> {
        paramsMapFromRequest.put(parameter.getName(), getParameter(parameter, paramsMapFromRequest));
        addParameterToService(microservice, parameter);
      });
  }

  /**
   * Retrieve parameter value
   *
   * @param restParameter Rest parameter
   * @return Parameter value
   */
  private Object getParameter(RestParameter restParameter, Map<String, Object> variableList) {

    switch (restParameter.getType()) {
      case SESSION:
        return getSession().getParameter(restParameter.getValue());
      case VARIABLE:
        return variableList.get(restParameter.getValue());
      case REQUEST:
        return queryUtil.getRequestParameter(restParameter.getValue());
      case VALUE:
      default:
        return restParameter.getValue();
    }
  }

  /**
   * Add parameter to service
   *  @param microservice  Microservice
   * @param restParameter Parameter to add
   */
  private void addParameterToService(ServiceMicroservice microservice, RestParameter restParameter) {
    ServiceInputParameter parameter = new ServiceInputParameter();
    parameter.setName(restParameter.getName());
    parameter.setValue(restParameter.getValue());
    parameter.setType(ParameterType.STRING.toString());
    List<ServiceInputParameter> parameterList = microservice.getParameterList();
    if (parameterList == null) {
      parameterList = Collections.synchronizedList(new ArrayList<>());
    }
    parameterList.add(parameter);
    microservice.setParameterList(parameterList);
  }

  /**
   * Fix date parameters when sending to microservices
   * @param parameters List of parameters
   *                   @param paramsMapFromRequest Parameter map
   */
  private void fixDateParameterMap(List<ServiceInputParameter> parameters, Map<String, Object> paramsMapFromRequest) {
    parameters.stream()
      .filter(p -> List.of(ParameterType.DATE.toString(), ParameterType.DATE_RDB.toString()).contains(p.getType()))
      .forEach(p -> paramsMapFromRequest.put(p.getName(), fixDateParameter(p, paramsMapFromRequest.get(p.getName()))));
  }

  private Object fixDateParameter(ServiceInputParameter parameter, Object value) {
    if (value instanceof Date dateValue) {
      if (ParameterType.DATE.toString().equals(parameter.getType())) {
        return DateUtil.dat2WebDate(dateValue);
      } else if (ParameterType.DATE_RDB.toString().equals(parameter.getType())) {
        return DateUtil.dat2RDBDate(dateValue);
      }
    } else if (value instanceof String stringValue && ParameterType.DATE_RDB.toString().equals(parameter.getType())) {
      return DateUtil.web2RdbDate(stringValue);
    }
    return value;
  }
}
