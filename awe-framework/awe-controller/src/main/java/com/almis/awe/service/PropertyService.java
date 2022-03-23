package com.almis.awe.service;

import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Property service class. Used to retrieve database application parameters and add them to environment
 *
 * @author pgarcia on 12/06/2017.
 */
@Slf4j
public class PropertyService extends ServiceConfig {

  // Autowired services
  private final QueryService queryService;
  private final ConfigurableEnvironment environment;
  private final DatabaseConfigProperties databaseConfigProperties;

  /**
   * Autowired constructor
   *
   * @param queryService             Query service
   * @param configurableEnvironment  Configurable environment
   * @param databaseConfigProperties Database configuration properties
   */
  public PropertyService(QueryService queryService, ConfigurableEnvironment configurableEnvironment, DatabaseConfigProperties databaseConfigProperties) {
    this.queryService = queryService;
    this.environment = configurableEnvironment;
    this.databaseConfigProperties = databaseConfigProperties;
  }

  /**
   * Generate application properties
   *
   * @return Service data
   */
  public ServiceData refreshDatabaseProperties() {
    ServiceData serviceData = new ServiceData();

    // Retrieve properties from database if database is enabled
    if (databaseConfigProperties.isEnabled()) {
      try {
        log.info("===== Loading database properties =====");

        // Retrieve application properties
        DataList dataList = queryService.launchPrivateQuery(AweConstants.APPLICATION_PARAMETERS_QUERY, "1", "0").getDataList();

        // Store them into a properties object
        Map<String, Object> aweDatabaseProperties = dataList.getRows().stream().collect(Collectors.toMap(r -> r.get(AweConstants.PARAMETER_NAME).getStringValue(), r -> r.get(AweConstants.PARAMETER_VALUE).getStringValue()));

        // Update environment sources
        MutablePropertySources propertySources = environment.getPropertySources();
        MapPropertySource propertySource = new MapPropertySource(AweConstants.AWE_DATABASE_PROPERTIES, aweDatabaseProperties);

        // Store property source
        if (propertySources.contains(AweConstants.AWE_DATABASE_PROPERTIES)) {
          propertySources.replace(AweConstants.AWE_DATABASE_PROPERTIES, propertySource);
        } else {
          propertySources.addFirst(propertySource);
        }

        log.info("===== Database properties loaded ({} properties) =====", dataList.getRows().size());
      } catch (AWException exc) {
        log.error("Error loading database properties", exc);
      }
    }

    // Return service data for service calls
    return serviceData;
  }
}
