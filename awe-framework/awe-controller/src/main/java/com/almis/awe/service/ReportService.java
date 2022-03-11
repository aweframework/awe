package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.service.report.ReportGenerator;

/**
 * QueryService Class
 * <p>
 * AWE Data Engine
 * Provides generate function to get application data
 *
 * @author Pablo GARCIA
 */
public class ReportService extends ServiceConfig {

  // Autowired services
  private final QueryService queryService;
  private final MenuService menuService;
  private final ReportGenerator reportGenerator;
  private final BaseConfigProperties baseConfigProperties;

  /**
   * Autowired constructor
   *
   * @param queryService         query service
   * @param menuService          menu service
   * @param reportGenerator      report generator
   * @param baseConfigProperties base config properties
   */
  public ReportService(QueryService queryService, MenuService menuService, ReportGenerator reportGenerator, BaseConfigProperties baseConfigProperties) {
    this.queryService = queryService;
    this.menuService = menuService;
    this.reportGenerator = reportGenerator;
    this.baseConfigProperties = baseConfigProperties;
  }

  /**
   * Retrieve print actions
   *
   * @return Print actions as service data
   * @throws AWException Error retrieving print actions
   */
  public ServiceData getPrintActions() throws AWException {
    ServiceData serviceData;

    // Get print options (based on awe.application.print-all-options-enable)
    if (baseConfigProperties.isPrintAllOptionsEnable()) {
      serviceData = queryService.launchQuery("PrnActAll");
    } else {
      serviceData = queryService.launchQuery("PrnActNotPrn");
    }

    return serviceData;
  }

  /**
   * Print current screen
   *
   * @return Screen print status
   * @throws AWException Error generating reports
   */
  public ServiceData printScreen() throws AWException {
    // Retrieve current screen id
    String screenName = getRequest().getParameterAsString(AweConstants.PRINT_SCREEN);

    // Generate a screen report with the screen
    return reportGenerator.generateScreenReport(menuService.getScreen(screenName));
  }
}
