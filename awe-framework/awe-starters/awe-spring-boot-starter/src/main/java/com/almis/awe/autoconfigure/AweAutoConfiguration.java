package com.almis.awe.autoconfigure;

import com.almis.ade.api.ADE;
import com.almis.awe.component.AweLoggingFilter;
import com.almis.awe.component.AweMDCTaskDecorator;
import com.almis.awe.config.*;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.XStreamSerializer;
import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.model.service.DataListService;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.*;
import com.almis.awe.service.connector.JavaConnector;
import com.almis.awe.service.connector.MicroserviceConnector;
import com.almis.awe.service.connector.RestConnector;
import com.almis.awe.service.data.builder.DataListBuilder;
import com.almis.awe.service.data.builder.EnumBuilder;
import com.almis.awe.service.data.builder.ServiceBuilder;
import com.almis.awe.service.data.connector.maintain.MaintainLauncher;
import com.almis.awe.service.data.connector.maintain.ServiceMaintainConnector;
import com.almis.awe.service.data.connector.query.EnumQueryConnector;
import com.almis.awe.service.data.connector.query.QueryLauncher;
import com.almis.awe.service.data.connector.query.ServiceQueryConnector;
import com.almis.awe.service.report.ReportDesigner;
import com.almis.awe.service.report.ReportGenerator;
import com.almis.awe.service.screen.ScreenComponentGenerator;
import com.almis.awe.service.screen.ScreenConfigurationGenerator;
import com.almis.awe.service.screen.ScreenModelGenerator;
import com.almis.awe.service.screen.ScreenRestrictionGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.annotation.RequestScope;

/**
 * AWE Autoconfiguration
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(value = {BaseConfigProperties.class,
  NumericConfigProperties.class,
  DatabaseConfigProperties.class,
  RestConfigProperties.class})
public class AweAutoConfiguration {

  // Autowired beans
  private final WebApplicationContext context;

  /**
   * Autowired constructor
   *
   * @param context Context
   */
  @Autowired
  public AweAutoConfiguration(WebApplicationContext context) {
    this.context = context;
  }

  /**
   * Numeric service constructor
   *
   * @return Numeric service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public NumericService numericService(NumericConfigProperties numericConfigProperties) {
    return new NumericService(numericConfigProperties);
  }

  /**
   * Encode service bean
   *
   * @param baseConfigProperties     Base configuration properties
   * @param securityConfigProperties Security configuration properties
   * @return EncodeService bean
   */
  @Bean
  @ConditionalOnMissingBean
  public EncodeService encodeService(BaseConfigProperties baseConfigProperties, SecurityConfigProperties securityConfigProperties) {
    return new EncodeService(baseConfigProperties, securityConfigProperties);
  }

  /**
   * Awe Request
   *
   * @return Request beans
   */
  @Bean
  @ConditionalOnMissingBean
  @RequestScope
  public AweRequest aweRequest(HttpServletRequest request, HttpServletResponse response, ObjectMapper mapper) {
    return new AweRequest(request, response, mapper);
  }

  /**
   * Awe Elements bean
   *
   * @param elementsDao          Elements DAO
   * @param baseConfigProperties Base configuration properties
   * @return Awe Elements bean
   */
  @Bean
  @ConditionalOnMissingBean
  public AweElements aweElements(AweElementsDao elementsDao, BaseConfigProperties baseConfigProperties) {
    return new AweElements(context, baseConfigProperties, elementsDao);
  }

  /**
   * Object mapper
   *
   * @return ObjectMapper bean
   */
  @Bean
  @ConditionalOnMissingBean
  public ObjectMapper objectMapper() {
    return new Jackson2ObjectMapperBuilder()
      .failOnUnknownProperties(false)
      .defaultViewInclusion(false)
      .featuresToEnable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .modules(new JavaTimeModule())
      .build();
  }

  /////////////////////////////////////////////
  // DAO
  /////////////////////////////////////////////

  /**
   * Awe Elements DAO
   *
   * @param serializer           XStream serializer
   * @param baseConfigProperties Base config properties
   * @return Awe Elements bean
   */
  @Bean
  @ConditionalOnMissingBean
  public AweElementsDao aweElementsDao(XStreamSerializer serializer, BaseConfigProperties baseConfigProperties) {
    return new AweElementsDao(serializer, baseConfigProperties);
  }

  /**
   * Initial load DAO
   *
   * @param queryService Query service
   * @return Initial load DATA ACCESS OBJECT
   */
  @Bean
  @ConditionalOnMissingBean
  public InitialLoadDao initialLoadDao(QueryService queryService) {
    return new InitialLoadDao(queryService);
  }

  /////////////////////////////////////////////
  // UTILITIES
  /////////////////////////////////////////////

  /**
   * Query utilities
   *
   * @param baseConfigProperties     Base config properties
   * @param databaseConfigProperties Database config properties
   * @return Query utilities bean
   */
  @Bean
  @ConditionalOnMissingBean
  public QueryUtil queryUtil(BaseConfigProperties baseConfigProperties, DatabaseConfigProperties databaseConfigProperties) {
    return new QueryUtil(baseConfigProperties, databaseConfigProperties);
  }

  /**
   * DataList Service bean
   *
   * @param conversionService Conversion service
   * @return DataList Service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public DataListService dataListService(ConversionService conversionService) {
    return new DataListService(conversionService);
  }


  /////////////////////////////////////////////
  // SERVICES
  /////////////////////////////////////////////

  /**
   * Launcher service
   *
   * @return Launcher service
   */
  @Bean
  @ConditionalOnMissingBean
  public LauncherService launcherService() {
    return new LauncherService();
  }

  /**
   * Property service
   *
   * @param queryService             Query service
   * @param configurableEnvironment  Configurable environment
   * @param databaseConfigProperties Database configuration properties
   * @return Property service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public PropertyService propertyService(QueryService queryService, ConfigurableEnvironment configurableEnvironment, DatabaseConfigProperties databaseConfigProperties) {
    return new PropertyService(queryService, configurableEnvironment, databaseConfigProperties);
  }

  /**
   * Init service
   *
   * @param launcherService Launcher service
   * @return Init service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public InitService initService(LauncherService launcherService) {
    return new InitService(launcherService);
  }

  /**
   * Action service
   *
   * @param launcherService      Launcher service
   * @param baseConfigProperties Base configuration properties
   * @return Action service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public ActionService actionService(LauncherService launcherService, BaseConfigProperties baseConfigProperties) {
    return new ActionService(launcherService, baseConfigProperties);
  }

  /**
   * Query service
   *
   * @param queryLauncher Query launcher
   * @param queryUtil     Query utilities
   * @return Query service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public QueryService queryService(QueryLauncher queryLauncher, QueryUtil queryUtil) {
    return new QueryService(queryLauncher, queryUtil);
  }

  /**
   * Maintain service
   *
   * @param maintainLauncher         Maintain launcher
   * @param queryUtil                Query utilities
   * @param databaseConfigProperties Database configuration properties
   * @return Maintain service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public MaintainService maintainService(MaintainLauncher maintainLauncher, QueryUtil queryUtil, DatabaseConfigProperties databaseConfigProperties) {
    return new MaintainService(maintainLauncher, queryUtil, databaseConfigProperties);
  }

  /**
   * Menu service
   *
   * @param queryService               Query service
   * @param screenRestrictionGenerator Screen Restriction generator
   * @param screenComponentGenerator   Screen component generator
   * @param initialLoadDao             Initial load service
   * @param baseConfigProperties       Base configuration properties
   * @param securityConfigProperties   Security configuration properties
   * @return Menu service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public MenuService menuService(QueryService queryService, ScreenRestrictionGenerator screenRestrictionGenerator,
                                 ScreenComponentGenerator screenComponentGenerator, InitialLoadDao initialLoadDao, BaseConfigProperties baseConfigProperties,
                                 SecurityConfigProperties securityConfigProperties, FavouriteService favouriteService, LauncherService launcherService) {
    return new MenuService(queryService, screenRestrictionGenerator, screenComponentGenerator, initialLoadDao,
      baseConfigProperties, securityConfigProperties, favouriteService, launcherService);
  }

  /**
   * Favourite service
   *
   * @param queryService    Query service
   * @param queryUtil       Query utilities
   * @param maintainService Maintain service
   * @return Menu service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public UserService userService(QueryService queryService, QueryUtil queryUtil,
                                 MaintainService maintainService) {
    return new UserService(queryService, queryUtil, maintainService);
  }

  /**
   * Favourite service
   *
   * @param queryService    Query service
   * @param queryUtil       Query utilities
   * @param dataListService DataList service
   * @param maintainService Maintain service
   * @return Menu service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public FavouriteService favouriteService(QueryService queryService, QueryUtil queryUtil,
                                           DataListService dataListService, MaintainService maintainService) {
    return new FavouriteService(queryService, queryUtil, dataListService, maintainService);
  }

  /**
   * Screen service
   *
   * @param menuService              Menu service
   * @param maintainService          Maintain service
   * @param templateService          Template service
   * @param screenComponentGenerator Screen component generator
   * @return Screen service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public ScreenService screenService(MenuService menuService, MaintainService maintainService, TemplateService templateService,
                                     ScreenComponentGenerator screenComponentGenerator, ApplicationEventPublisher eventPublisher) {
    return new ScreenService(menuService, maintainService, templateService, screenComponentGenerator, eventPublisher);
  }

  /**
   * File service
   *
   * @param broadcastService     Broadcast service
   * @param request              Request
   * @param baseConfigProperties Base configuration properties
   * @param encodeService        Encode service
   * @return File service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public FileService fileService(BroadcastService broadcastService, AweRequest request, BaseConfigProperties baseConfigProperties, EncodeService encodeService) {
    return new FileService(broadcastService, request, baseConfigProperties, encodeService);
  }

  /**
   * Locale service
   *
   * @return Locale service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public LocaleService localeService() {
    return new LocaleService();
  }

  /**
   * Log service
   *
   * @param queryUtil            Query utilities
   * @param baseConfigProperties Base configuration properties
   * @return Log service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public LogService logService(QueryUtil queryUtil, BaseConfigProperties baseConfigProperties) {
    return new LogService(queryUtil, baseConfigProperties);
  }

  /**
   * Report service
   *
   * @param queryService         Query service
   * @param menuService          Menu service
   * @param reportGenerator      Report generator
   * @param baseConfigProperties Base config properties
   * @return Report service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public ReportService reportService(QueryService queryService, MenuService menuService, ReportGenerator reportGenerator, BaseConfigProperties baseConfigProperties) {
    return new ReportService(queryService, menuService, reportGenerator, baseConfigProperties);
  }

  /**
   * Printer service
   *
   * @return Printer service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public PrinterService printerService() {
    return new PrinterService();
  }

  /**
   * System service
   *
   * @return System service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public SystemService systemService() {
    return new SystemService();
  }

  /**
   * ChartService service
   *
   * @param objectMapper         Object mapper
   * @param baseConfigProperties Base config properties
   * @return Chart service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public ChartService chartService(ObjectMapper objectMapper, BaseConfigProperties baseConfigProperties) {
    return new ChartService(objectMapper, baseConfigProperties);
  }

  /////////////////////////////////////////////
  // LAUNCHERS
  /////////////////////////////////////////////

  /**
   * Query launcher
   *
   * @return Query launcher bean
   */
  @Bean
  @ConditionalOnMissingBean
  @Scope("prototype")
  public QueryLauncher queryLauncher() {
    return new QueryLauncher();
  }

  /**
   * Maintain launcher
   *
   * @return Maintain launcher bean
   */
  @Bean
  @ConditionalOnMissingBean
  @Scope("prototype")
  public MaintainLauncher maintainLauncher() {
    return new MaintainLauncher();
  }


  /////////////////////////////////////////////
  // GENERATORS
  /////////////////////////////////////////////

  /**
   * Screen restriction generator
   *
   * @return Screen restriction generator bean
   */
  @Bean
  @ConditionalOnMissingBean
  public ScreenRestrictionGenerator screenRestrictionGenerator() {
    return new ScreenRestrictionGenerator();
  }

  /**
   * Screen configuration generator
   *
   * @return Screen configuration generator bean
   */
  @Bean
  @ConditionalOnMissingBean
  public ScreenConfigurationGenerator screenConfigurationGenerator() {
    return new ScreenConfigurationGenerator();
  }

  /**
   * Screen model generator
   *
   * @param screenRestrictionGenerator Screen restriction generator
   * @param initialLoadDao             Initial load service
   * @param baseConfigProperties       Base config properties
   * @return Screen model generator bean
   */
  @Bean
  @ConditionalOnMissingBean
  public ScreenModelGenerator screenModelGenerator(ScreenRestrictionGenerator screenRestrictionGenerator,
                                                   InitialLoadDao initialLoadDao, BaseConfigProperties baseConfigProperties) {
    return new ScreenModelGenerator(screenRestrictionGenerator, initialLoadDao, baseConfigProperties);
  }

  /**
   * Screen component generator
   *
   * @param request                      Request
   * @param screenModelGenerator         Screen model
   * @param screenConfigurationGenerator Screen configuration
   * @param initialLoadDao               Initial load service
   * @param aweElementsDao               AWE Elements DAO
   * @param baseConfigProperties         Base config properties
   * @return Screen component generator bean
   */
  @Bean
  @ConditionalOnMissingBean
  public ScreenComponentGenerator screenComponentGenerator(AweRequest request, ScreenModelGenerator screenModelGenerator,
                                                           ScreenConfigurationGenerator screenConfigurationGenerator,
                                                           InitialLoadDao initialLoadDao, AweElementsDao aweElementsDao, BaseConfigProperties baseConfigProperties) {
    return new ScreenComponentGenerator(request, screenModelGenerator, screenConfigurationGenerator, initialLoadDao, aweElementsDao, baseConfigProperties);
  }

  /////////////////////////////////////////////
  // REPORTING
  /////////////////////////////////////////////

  /**
   * Report generator
   *
   * @param reportDesigner       reportDesigner Report designer
   * @param ade                  ADE Api
   * @param baseConfigProperties Base config properties
   * @return Report generator bean
   */
  @Bean
  @ConditionalOnMissingBean
  public ReportGenerator reportGenerator(ReportDesigner reportDesigner, ADE ade, BaseConfigProperties baseConfigProperties) {
    return new ReportGenerator(reportDesigner, ade, baseConfigProperties);
  }

  /**
   * Report designer constructor
   *
   * @param queryService         Query service
   * @param mapper               Object mapper
   * @param baseConfigProperties Base config properties
   * @return Report designer bean
   */
  @Bean
  @ConditionalOnMissingBean
  public ReportDesigner reportDesigner(QueryService queryService, ObjectMapper mapper, BaseConfigProperties baseConfigProperties) {
    return new ReportDesigner(queryService, mapper, baseConfigProperties);
  }

  /////////////////////////////////////////////
  // CONNECTORS
  /////////////////////////////////////////////

  /**
   * Java connector
   *
   * @return Java connector bean
   */
  @Bean
  @ConditionalOnMissingBean
  public JavaConnector javaConnector() {
    return new JavaConnector();
  }

  /**
   * Microservice connector
   *
   * @param requestFactory Request factory
   * @param queryUtil      Query utilities
   * @param objectMapper   Object mapper
   * @return Microservice connector bean
   */
  @Bean
  @ConditionalOnMissingBean
  public MicroserviceConnector microserviceConnector(ClientHttpRequestFactory requestFactory, QueryUtil queryUtil, ObjectMapper objectMapper, RestConfigProperties restConfigProperties) {
    return new MicroserviceConnector(requestFactory, queryUtil, objectMapper, restConfigProperties);
  }

  /**
   * REST connector
   *
   * @param requestFactory       Request factory
   * @param objectMapper         Object mapper
   * @param restConfigProperties Rest config properties
   * @return REST connector bean
   */
  @Bean
  @ConditionalOnMissingBean
  public RestConnector restConnector(ClientHttpRequestFactory requestFactory, ObjectMapper objectMapper, RestConfigProperties restConfigProperties) {
    return new RestConnector(requestFactory, objectMapper, restConfigProperties);
  }

  /**
   * EnumQuery connector
   *
   * @param queryUtil            Query utilities
   * @param baseConfigProperties Base configuration properties
   * @param elements             AWE elements
   * @param numericService       Numeric service
   * @param encodeService        Encode service
   * @return EnumQuery connector bean
   */
  @Bean
  @ConditionalOnMissingBean
  public EnumQueryConnector enumQueryConnector(QueryUtil queryUtil, BaseConfigProperties baseConfigProperties, AweElements elements, NumericService numericService, EncodeService encodeService) {
    return new EnumQueryConnector(queryUtil, baseConfigProperties, elements, numericService, encodeService);
  }

  /**
   * Service Query connector
   *
   * @param queryUtil            Query utilities
   * @param baseConfigProperties Base configuration properties
   * @param elements             AWE elements
   * @param numericService       Numeric service
   * @param encodeService        Encode service
   * @return Service Query connector bean
   */
  @Bean
  @ConditionalOnMissingBean
  public ServiceQueryConnector serviceQueryConnector(QueryUtil queryUtil, BaseConfigProperties baseConfigProperties, AweElements elements, NumericService numericService, EncodeService encodeService) {
    return new ServiceQueryConnector(queryUtil, baseConfigProperties, elements, numericService, encodeService);
  }

  /**
   * Service Maintain connector
   *
   * @return Service Maintain connector bean
   */
  @Bean
  @ConditionalOnMissingBean
  public ServiceMaintainConnector serviceMaintainConnector() {
    return new ServiceMaintainConnector();
  }

  /////////////////////////////////////////////
  // BUILDERS
  /////////////////////////////////////////////

  /**
   * Data list builder
   *
   * @return Data list builder bean
   */
  @Bean
  @Scope("prototype")
  public DataListBuilder dataListBuilder() {
    return new DataListBuilder();
  }

  /**
   * Enum builder
   *
   * @return Enum builder bean
   */
  @Bean
  @Scope("prototype")
  public EnumBuilder enumBuilder() {
    return new EnumBuilder();
  }

  /**
   * Service builder
   *
   * @param launcherService Launcher service
   * @param queryUtil       QueryUtil service
   * @return Service builder bean
   */
  @Bean
  @Scope("prototype")
  public ServiceBuilder serviceBuilder(LauncherService launcherService, QueryUtil queryUtil) {
    return new ServiceBuilder(launcherService, queryUtil);
  }

  /**
   * Awe logging filter
   *
   * @return servlet filter
   */
  @Bean
  public AweLoggingFilter aweLoggingFilter() {
    return new AweLoggingFilter();
  }

  /**
   * Awe MDC Task decorator
   *
   * @return awe MDC task decorator
   */
  @Bean
  public AweMDCTaskDecorator aweMDCTaskDecorator() {
    return new AweMDCTaskDecorator();
  }

}
