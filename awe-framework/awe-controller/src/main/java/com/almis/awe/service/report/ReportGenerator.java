package com.almis.awe.service.report;

import com.almis.ade.api.ADE;
import com.almis.ade.api.bean.input.PrintBean;
import com.almis.ade.api.fluid.engine.generic.TemplateExporterBuilder;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.type.OutputFormatType;
import com.almis.awe.model.util.data.DateUtil;
import com.almis.awe.model.util.data.StringUtil;
import com.almis.awe.model.util.file.FileUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JREmptyDataSource;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Generate the component controllers of the screen
 */
@Slf4j
public class ReportGenerator extends ServiceConfig {

  // Autowired services
  private final ReportDesigner designer;
  private final ADE adeAPI;
  private final BaseConfigProperties baseConfigProperties;

  /**
   * Autowired constructor
   *
   * @param reportDesigner       Report designer
   * @param adeAPI               ADE API
   * @param baseConfigProperties Base config properties
   */
  public ReportGenerator(ReportDesigner reportDesigner, ADE adeAPI, BaseConfigProperties baseConfigProperties) {
    this.designer = reportDesigner;
    this.adeAPI = adeAPI;
    this.baseConfigProperties = baseConfigProperties;
  }

  /**
   * Generate a report and return client actions to download it
   *
   * @param screen Screen to generate
   * @return List of file data
   * @throws AWException Error generating report
   */
  public List<FileData> generateScreenReportFiles(Screen screen) throws AWException {
    // Get screen parameters
    ObjectNode parameters = getRequest().getParametersSafe();

    // Retrieve print formats
    List<String> printFormats = StringUtil.asList(parameters.get(AweConstants.PRINT_FORMATS));

    // With screen parameters, generate the print bean
    PrintBean printBean = designReport(screen, parameters);

    // Get currentDate
    String currentDate = DateUtil.dat2WebTimestamp(new Date());

    // Generate file name
    String screenTitle = getLocale(screen.getLabel());
    String fileName = StringUtil.fixFileName( screenTitle + "_" + currentDate);

    // Set screen title parameter
    getRequest().setParameter("ScrTit", screenTitle);
    getRequest().setParameter("ScrTitFil", fileName);

    // Llamar a ADE con el bean creado
    TemplateExporterBuilder builderService = buildReport(printBean, fileName);


    // Generar los formatos que haya definido el usuario
    String basePath = StringUtil.getAbsolutePath(baseConfigProperties.getPaths().getReports(), baseConfigProperties.getPaths().getBase());
    return printFormats.stream().map(format -> generateReportFormat(builderService, format, fileName, basePath)).toList();
  }

  /**
   * Generate a report and return client actions to download it
   *
   * @param reportFiles Report files to download
   * @return Service data with the actions to download the generated reports
   * @throws AWException Error generating report
   */
  public ServiceData downloadScreenReportFiles(List<FileData> reportFiles) throws AWException {
    ServiceData serviceData = new ServiceData();
    for (FileData reportFile : reportFiles) {
      serviceData.addClientAction(new ClientAction("get-file").addParameter("filename", FileUtil.fileDataToString(reportFile)));
    }

    return serviceData;
  }

  /**
   * Design the report
   *
   * @param screen     Screen to design
   * @param parameters Screen parameters
   * @return Print bean designed
   * @throws AWException Error designing report
   */
  private PrintBean designReport(Screen screen, ObjectNode parameters) throws AWException {
    // Generate report structure
    List<Element> reportStructure = screen.getReportStructure(new ArrayList<>(), null, parameters, baseConfigProperties.getComponent().getDataSuffix());

    // Generate print bean
    return designer.getPrintDesign(reportStructure, parameters);
  }

  /**
   * Build report
   *
   * @param printBean Print bean
   * @return Report exporter
   * @throws AWException Error building report
   */
  private TemplateExporterBuilder buildReport(PrintBean printBean, String fileName) throws AWException {
    try {
      // Generate file
      return adeAPI
              .printBean()
              .withJasper()
              .buildAndExport(printBean)
              .withName(fileName)
              .withPath(StringUtil.getAbsolutePath(baseConfigProperties.getPaths().getReports(), baseConfigProperties.getPaths().getBase()))
              .withDataSource(new JREmptyDataSource());
    } catch (Exception exc) {
      throw new AWException(getLocale("ERROR_TITLE_GENERATING_DOCUMENT_DATA"),
              getLocale("ERROR_MESSAGE_GENERATING_DOCUMENT_DATA"), exc);
    }
  }

  /**
   * Generate report format (Async)
   *
   * @param builderService template export builder
   * @param format         format
   * @param fileName       file name
   * @param basePath       base path
   * @return future with generate report action
   */
  public FileData generateReportFormat(TemplateExporterBuilder builderService, String format, String fileName, String basePath) {
    String mimeType = MediaType.APPLICATION_PDF_VALUE;
    String fullFileName = fileName;

    try {
      switch (OutputFormatType.valueOf(format.toUpperCase())) {
        case XLSX:
          builderService.toXlsx();
          mimeType = AweConstants.APPLICATION_EXCEL;
          fullFileName += ".xlsx";
          getRequest().setParameter("XlsNam", Paths.get(basePath, fullFileName).toString());
          break;
        case CSV:
          builderService.toCsv();
          mimeType = AweConstants.APPLICATION_EXCEL;
          fullFileName += ".csv";
          getRequest().setParameter("CsvNam", Paths.get(basePath, fullFileName).toString());
          break;
        case DOCX:
          builderService.toDocx();
          mimeType = AweConstants.APPLICATION_WORD;
          fullFileName += ".docx";
          getRequest().setParameter("DocNam", Paths.get(basePath, fullFileName).toString());
          break;
        case TEXT:
          builderService.toText();
          mimeType = MediaType.TEXT_PLAIN_VALUE;
          fullFileName += ".txt";
          getRequest().setParameter("TxtNam", Paths.get(basePath, fullFileName).toString());
          break;
        case PDF:
        default:
          builderService.toPDF();
          mimeType = MediaType.APPLICATION_PDF_VALUE;
          fullFileName += ".pdf";
          getRequest().setParameter("PdfNam", Paths.get(basePath, fullFileName).toString());
          break;
      }
    } catch (Exception exc) {
      log.error("Error generating report file ({}): {}{}", format, basePath, fullFileName, exc);
    }

    // Generate file data
    File reportFile = new File(basePath + fullFileName);
    FileData fileData = new FileData(fullFileName, reportFile.length(), mimeType);
    storeHistoricReport(reportFile);
    fileData.setBasePath(basePath);

    // Log report
    log.debug("Report file ({}) generated: {}{}", mimeType, basePath, fullFileName);

    // Generate client action with file data
    return fileData;
  }

  /**
   * Store historic report in historic report path
   *
   * @param reportFile Report to store
   */
  private void storeHistoricReport(File reportFile) {
    // Retrieve file date and database
    Date reportDate = new Date(reportFile.lastModified());
    String database = getSessionDatabase();
    String reportDateFormatted = DateUtil.dat2SqlDateString(reportDate);

    // Generate historic directory
    File historicPath = Paths.get(StringUtil.getAbsolutePath(baseConfigProperties.getPaths().getReportsHistoric(), baseConfigProperties.getPaths().getBase()), reportDateFormatted, Optional.ofNullable(database).orElse("")).toFile();
    try {
      Files.createDirectories(historicPath.toPath());
      Files.copy(reportFile.toPath(), Paths.get(historicPath.getAbsolutePath(), reportFile.getName()));
    } catch (IOException exc) {
      // Log report
      log.error("Historic report file ({}) NOT generated on {}", reportFile.getAbsolutePath(), historicPath.getAbsolutePath(), exc);
    }
  }

  /**
   * Retrieve session database (safely)
   *
   * @return Session database
   */
  private String getSessionDatabase() {
    try {
      return getSession().getParameter(String.class, AweConstants.SESSION_DATABASE);
    } catch (Exception exc) {
      return "";
    }
  }
}