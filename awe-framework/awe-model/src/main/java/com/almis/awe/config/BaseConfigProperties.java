package com.almis.awe.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/**
 * Application Base configuration properties
 */
@ConfigurationProperties(prefix = "awe.application")
@Validated
@Data
public class BaseConfigProperties {
  /**
   * Base encoding for all files.
   * Default value UTF-8
   */
  private String encoding = "UTF-8";

  /**
   * Application name
   * Default value AWE (Almis Web Engine)
   */
  private String name = "AWE (Almis Web Engine)";

  /**
   * Application language in ISO 639-1 2 Letters format. (en = English)
   * Default value en
   */
  @Size(min = 2, max = 2)
  private String languageDefault = "en";

  /**
   * Application available languages in ISO 639-1 2 Letters format. (en = English)
   * Default value list with EN, ES, FR languages
   */
  private List<@Size(min = 2, max = 2) String> languageList = Arrays.asList("en", "es", "fr");

  /**
   * Application them.
   * Default value sky
   */
  private String theme = "sky";

  /**
   * Application acronym (in lowercase).
   * Default value awe
   */
  private String acronym = "awe";

  /**
   * List with application modules separate by commas. Used to load xml files for each module.
   */
  @NotNull(message = "There is no application module defined. Please check your settings")
  private String[] moduleList;

  /**
   * Application address parameter name.
   * Default value address
   */
  private String address = "address";

  /**
   * Enable preload screens.
   * Default value true
   */
  private boolean preloadScreens = true;

  /**
   * Reload current screen.
   * Default value false
   */
  private boolean reloadCurrentScreen = false;

  /**
   * XML file extension parameter name.
   * Default value .xml
   */
  private String extensionXml = ".xml";

  /**
   * Show all print options
   * Default value true
   */
  private boolean printAllOptionsEnable = true;

  /**
   * Define the parent path for application log files
   * Default value /application
   */
  private String logGroup = "/application";

  /**
   * Define the path where awe log manager search the log files
   * Default value ${logging.file.path:${java.io.tmpdir}}${awe.application.log.group:/application}
   */
  @Value("${logging.file.path:${java.io.tmpdir}}${awe.application.log.group:/application}")
  private String logManagerPath;

  /**
   * Flag to activate log file appender by user.
   * Default value true
   */
  private boolean logUserEnable = true;

  /**
   * Highcharts export server url
   * Default value https://export.highcharts.com
   */
  private String highchartsServerUrl = "https://export.highcharts.com";

  /**
   * Loading timeout in milliseconds.
   * Default value 10000ms
   */
  @DurationUnit(ChronoUnit.MILLIS)
  private Duration loadingTimeout = Duration.ofMillis(10000);

  /**
   * Application Xml files definitions.
   */
  @NestedConfigurationProperty
  private Files files = new Files();

  /**
   * Application paths configuration.
   */
  @NestedConfigurationProperty
  private Paths paths = new Paths();

  /**
   * Application parameters configuration.
   */
  @NestedConfigurationProperty
  private Parameter parameter = new Parameter();

  /**
   * Jms engine configuration properties.
   */
  @NestedConfigurationProperty
  private Jms jms = new Jms();

  /**
   * Screen names configuration.
   */
  @NestedConfigurationProperty
  private Screen screen = new Screen();

  /**
   * AWE components configuration parameters.
   */
  @NestedConfigurationProperty
  private Component component = new Component();

  /**
   * Application AWE file manager parameters configuration.
   */
  @NestedConfigurationProperty
  private Filemanager filemanager = new Filemanager();

  /**
   * Application Xml files config properties
   */
  @Data
  public static class Files {
    /**
     * Xml file name of actions definition.
     * Default value Actions.
     */
    private String actions = "Actions";
    /**
     * Xml file name of services definition.
     * Default value Services.
     */
    private String services = "Services";
    /**
     * Xml file name of locales definition.
     * Default value Locale-.
     */
    private String locale = "Locale-";
    /**
     * Xml file name of enumerated definition.
     * Default value Enumerated.
     */
    private String enumerated = "Enumerated";
    /**
     * Xml file name of queries definition.
     * Default value Queries.
     */
    private String query = "Queries";
    /**
     * Xml file name of Queues definition.
     * Default value Queues.
     */
    private String queue = "Queues";
    /**
     * Xml file name of maintains definition.
     * Default value Maintain.
     */
    private String maintain = "Maintain";
    /**
     * Xml file name of components definition.
     * Default value Components.
     */
    private String components = "Components";
    /**
     * Xml file name of templates definition.
     * Default value Templates.
     */
    private String templates = "Templates";
    /**
     * Xml file name of Email definition.
     * Default value Email.
     */
    private String email = "Email";
    /**
     * Xml file name of public menu definition.
     * Default value public.
     */
    private String menuPublic = "public";
    /**
     * Xml file name of private menu definition.
     * Default value private.
     */
    private String menuPrivate = "private";
  }

  /**
   * Paths property configuration
   * (@ means relative path to app base path)
   */
  @Data
  public static class Paths {

    public static final String FAVICON_ICON = "icon/favicon-awe.ico";

    /**
     * Application base path.
     * Default value /
     */
    private String base = "/";

    /**
     * Application folder path.
     * Default value /application/
     */
    private String application = "/application/";

    /**
     * Global folder path.
     * Default value /global/
     */
    private String global = "/global/";

    /**
     * Locale folder path.
     * Default value /locale/
     */
    private String locale = "/locale/";

    /**
     * Screen folder path.
     * Default value /screen/
     */
    private String screen = "/screen/";

    /**
     * Profile folder path.
     * Default value /profile/
     */
    private String profile = "/profile/";

    /**
     * Menu folder path.
     * Default value /menu/
     */
    private String menu = "/menu/";

    /**
     * Templates folder path.
     * Default value templates/
     */
    private String templates = "templates/";

    /**
     * Angular templates folder path.
     * Default value angular/
     */
    private String templatesAngular = "angular";

    /**
     * Tag folder path.
     * Default value angular/
     */
    private String tag = "tags/";

    /**
     * Tag folder path.
     * Default value angular/
     */
    private String tagAngular = tag + "angular/";

    /**
     * Server path. Used for set initial URL
     * Default value empty ""
     */
    private String server = "";

    /**
     * Images folder path.
     * Default value images/
     */
    private String images = "images/";

    /**
     * Reports destination folder path.
     * Default value @reports/
     */
    private String reports = "@reports/";

    /**
     * Documents folder path.
     * Default value static/docs/
     */
    private String documents = "static/docs/";

    /**
     * Historic report's destination folder path.
     * Default value @historicReports/
     */
    private String reportsHistoric = "@historicReports/";

    /**
     * Jasper jrx folder path.
     * Default value /jrx/
     */
    private String jrx = "/jrx/";

    /**
     * Temp folder path.
     * Default value /tmp
     */
    @Value("${java.io.tmpdir:/tmp}")
    private String temp = "/tmp";

    /**
     * Favicon icon path. Uses application.paths.images as parent path.
     * Default value /images/icon/favicon-awe.ico
     */
    private String iconFavicon = images + FAVICON_ICON;

    /**
     * Apple icon 57x57 path. Uses application.paths.images as parent path.
     * Default value /images/icon/favicon-awe.ico
     */
    private String iconPhone = images + FAVICON_ICON;

    /**
     * Apple icon 114x114 path. Uses application.paths.images as parent path.
     * Default value /images/icon/favicon-awe.ico
     */
    private String iconTablet = images + FAVICON_ICON;

    /**
     * Home screen logo. Uses application.paths.images as parent path.
     * Default value ../images/logo/logo-awe-nuevo.svg
     */
    private String imageStartupLogo = "../" + images + "logo/logo-awe-nuevo.svg";

    /**
     * Navigation bar logo. Uses application.paths.images as parent path.
     * Default value ../images/logo/logo-awe-nuevo2.svg
     */
    private String imageNavbarLogo = "../" + images + "logo/logo-awe-nuevo2.svg";

    /**
     * Home screen background image. Uses application.paths.images as parent path.
     * Default value ../images/background/signin-bg-1.jpg
     */
    private String imageStartupBackground = "../" + images + "background/signin-bg-1.jpg";
  }

  /**
   * Application parameters configuration
   */
  @Data
  public static class Parameter {

    /**
     * Screen username parameter value.
     * Default value cod_usr
     */
    private String username = "cod_usr";
    /**
     * Screen password parameter value.
     * Default value pwd_usr
     */
    private String password = "pwd_usr";
    /**
     * Token parameter name.
     * Default value frame
     */
    private String token = "t";
  }

  /**
   * Screen names configuration properties
   */
  @Data
  public static class Screen {
    /**
     * Home screen name.
     * Default value home
     */
    private String home = "home";
    /**
     * Change password screen name.
     * Default value change_password
     */
    private String changePassword = "change_password";
    /**
     * Default initial screen name.
     * Default value information
     */
    private String initial = "information";
  }

  /**
   * Configuration properties of awe components
   */
  @Data
  public static class Component {
    /**
     * Component size
     * Default value sm (Small)
     */
    private ComponentSize size = ComponentSize.SM;
    /**
     * Suffix of data parameter send from client
     * Default value .data
     */
    private String dataSuffix = ".data";
    /**
     * Rows per page in grid component (Set to 0 to avoid pagination)
     * Default value 30
     */
    private int gridRowsPerPage = 30;
    /**
     * Pixels per char in grids
     * Default value 8
     */
    private int gridPixelsPerCharacter = 8;
    /**
     * Rows per page in criteria component (Set to 0 to avoid pagination)
     * Default value 100
     */
    private int criteriaRowsPerPage = 100;
    /**
     * Empty computed values if one of the wildcards are empty
     * Default value true
     */
    private boolean computedEmptyIfNull = true;
    /**
     * Suggest timeout in milliseconds.
     * Default value 300ms
     */
    @DurationUnit(ChronoUnit.MILLIS)
    private Duration suggestTimeout = Duration.ofMillis(300);

    /**
     * Component help timeout in milliseconds.
     * Default value 1000ms
     */
    @DurationUnit(ChronoUnit.MILLIS)
    private Duration helpTimeout = Duration.ofMillis(1000);
    /**
     * File uploader component Id.
     * Default value u
     */
    private String uploadFileId = "u";
    /**
     * Path for file uploads (Relative to application.base.path starts with @)
     * Default value @upload/
     */
    private String uploadFilePath = "@upload/";
    /**
     * Max size for uploaded files (in MB)
     * Default value 100MB
     */
    @DataSizeUnit(DataUnit.MEGABYTES)
    private DataSize uploadMaxFileSize = DataSize.ofMegabytes(100);
    /**
     * Max elements per folder
     * Default value 100
     */
    private int uploadMaxFilesFolder = 100;
    /**
     * File downloader component Id.
     * Default value d
     */
    private String downloadFileId = "d";

    /**
     * Number of group element limit of pivot table component.
     * Default value 5000
     */
    private Integer pivotNumGroup = 5000;

    /**
     * Limit of points series in chart component
     */
    private Integer chartLimitPointSerie = 1000000;
  }

  /**
   * Jms (Java messages system) configuration properties
   */
  @Data
  public static class Jms {
    /**
     * Enable AWE JMS service connector.
     * Default value false
     */
    private boolean enabled = false;
    /**
     * Jms service timeout in milliseconds.
     * Default value 10000ms
     */
    @DurationUnit(ChronoUnit.MILLIS)
    private Duration serviceTimeout = Duration.ofMillis(10000);
    /**
     * Jms message time to live in milliseconds.
     * Default value 0ms (the message remains on the queue indefinitely until is processed)
     */
    @DurationUnit(ChronoUnit.MILLIS)
    private Duration messageTimeToLive = Duration.ofMillis(0);
  }

  /**
   * Filemanager config properties
   */
  @Data
  public static class Filemanager {
    /**
     * File manager base path
     * Default value root path (/)
     */
    private String basePath = "/";
    /**
     * File manager date format
     * Default value yyyy-MM-dd hh:mm:ss
     */
    private String dateFormat = "yyyy-MM-dd hh:mm:ss";
  }
}