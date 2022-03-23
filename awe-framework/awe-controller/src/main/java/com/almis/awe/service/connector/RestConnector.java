package com.almis.awe.service.connector;

import com.almis.awe.config.RestConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.services.ServiceRest;
import com.almis.awe.model.entities.services.ServiceType;
import com.almis.awe.model.rest.ServiceAuth;
import com.almis.awe.model.rest.ServiceDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;

/**
 * Launches a Rest service
 */
public class RestConnector extends AbstractRestConnector {

  private final RestConfigProperties restConfigProperties;
  /**
   * Autowired constructor
   *
   * @param requestFactory Request factory
   * @param restConfigProperties Rest config properties
   */
  public RestConnector(ClientHttpRequestFactory requestFactory, ObjectMapper objectMapper, RestConfigProperties restConfigProperties) {
    super(requestFactory, objectMapper);
    this.restConfigProperties = restConfigProperties;
  }

  @Override
  public ServiceData launch(ServiceType service, Map<String, Object> paramsMapFromRequest) throws AWException {
    // Variable definition
    ServiceData outData;
    StringBuilder urlBuilder = new StringBuilder();
    ServiceRest serviceRest = (ServiceRest) service;

    // Retrieve rest server (if defined)
    if (serviceRest.getServer() != null) {
      final ServiceDetails serviceDetails = Optional.ofNullable(restConfigProperties.getServices().get(serviceRest.getServer())).orElse(new ServiceDetails());
      urlBuilder.append(serviceDetails.getBaseUrl());
      // Retrieve microservice auth (if defined)
      ServiceAuth serviceAuth = serviceDetails.getAuthentication();
      if (serviceAuth != null) {
        serviceRest.setAuthentication(serviceAuth.getType());
        serviceRest.setUsername(serviceAuth.getUsername());
        serviceRest.setPassword(serviceAuth.getPassword());
      }
    }

    // Add endpoint to url
    urlBuilder.append(serviceRest.getEndpoint());

    // Create request to rest service
    try {
      outData = doRequest(urlBuilder.toString(), serviceRest, paramsMapFromRequest);
    } catch (RestClientException exc) {
      throw new AWException(getLocale("ERROR_TITLE_INVALID_CONNECTION"),
        getLocale("ERROR_MESSAGE_CONNECTION_REST", urlBuilder.toString()), exc);
    }

    // Check service response
    checkServiceResponse(outData);

    return outData;
  }
}
