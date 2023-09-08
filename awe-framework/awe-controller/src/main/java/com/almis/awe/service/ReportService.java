package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.service.report.ReportGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * QueryService Class
 * <p>
 * AWE Data Engine
 * Provides generate function to get application data
 * </p>
 * @author Pablo GARCIA
 */
@Slf4j
public class ReportService extends ServiceConfig {

  // Constants
  private static final String ERROR_TITLE_VIEWING_PDF_FILE = "ERROR_TITLE_VIEWING_PDF_FILE";

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
    return queryService.launchPrivateQuery(baseConfigProperties.isPrintAllOptionsEnable() ? "PrnActAll" : "PrnActNotPrn");
  }

  /**
   * Print current screen
   *
   * @return Screen print status
   * @throws AWException Error generating reports
   */
  public ServiceData printScreen(String screenName) throws AWException {
    // Generate a screen report with the screen
    return reportGenerator.generateScreenReport(menuService.getScreen(screenName));
  }

  /**
   * Retrieve application manual header
   * @param filePath absolute file path
   * @return Pdf file as FileData
   * @throws AWException File not found
   */
  public ServiceData viewPdfFile(String filePath) throws AWException {
    ServiceData serviceData = new ServiceData();
    Path path = Paths.get(filePath);
    Resource resource = new FileSystemResource(path);
    try {
      FileData fileData = null;
      if (resource.exists()) {
        File pdfFile = resource.getFile();
        fileData = new FileData(pdfFile.getName(),
                                resource.contentLength(),
                                "application/pdf")
                                .setBasePath(pdfFile.getParent());
      }
      serviceData.setData(fileData);
    } catch (IOException exc) {
      log.error(this.getLocale(ERROR_TITLE_VIEWING_PDF_FILE), exc);
      throw new AWException(this.getLocale(ERROR_TITLE_VIEWING_PDF_FILE), exc);
    }
    return serviceData;
  }
}
