package com.almis.awe.config;

import com.almis.awe.model.type.MenuType;
import com.almis.awe.model.type.XStreamDriverType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
   * Application language in ISO 3166 and ISO 639 codes format. (en-GB = English from Great Britain)
   * Default value en
   */
  @Size(min = 5, max = 5)
  private String languageDefault = "en-GB";

  /**
   * Application available languages in ISO 3166 and ISO 639 codes format. (en-GB = English from Great Britain)
   * Default value list with en-GB, es-ES, fr-FR languages
   */
  private List<@Size(min = 5, max = 5) String> languageList = Arrays.asList("en-GB", "es-ES", "fr-FR");

  /**
   * Application theme.
   * Default value sky
   */
  private String theme = "sky";

  /**
   * Application theme primary colour: one of the AWE 5 palettes (emerald, green, teal, cyan, sky, blue, indigo, violet,
   * purple, fuchsia, pink, rose, red, orange, amber, yellow, lime) or a hex colour (#RGB or #RRGGBB). It overrides the
   * primary colour of the theme.
   * Default value empty (the primary colour of the theme, else emerald)
   */
  private String themePrimary;

  /**
   * Application theme surface: one of the AWE 5 neutral palettes (slate, gray, zinc, neutral, stone), auto or a hex
   * colour (#RGB or #RRGGBB). It overrides the surface of the theme.
   * Default value empty (the surface of the theme, else auto)
   */
  private String themeSurface;

  /**
   * Application theme mode: light, dark or system (follows the operating system). It overrides the mode of the theme;
   * the mode the user chooses overrides it.
   * Default value empty (the mode of the theme, else system)
   */
  private String themeMode;

  /**
   * Application density: comfortable or compact. The density the user chooses overrides it.
   * Default value comfortable
   */
  private String density = "comfortable";

  /**
   * Application acronym (in lowercase).
   * Default value awe
   */
  private String acronym = "awe";

  /**
   * Application default user role
   */
  private String defaultRole = "operator";

  /**
   * List with application modules separate by commas. Used to load xml files for each module.
   */
  @NotNull(message = "There is no application module defined. Please check your settings")
  private String[] moduleList;

  /**
   * Menu position layout type
   */
  @Valid
  private MenuType menuType = MenuType.HORIZONTAL;

  /**
   * Enable the menu option search box.
   * Default value true
   */
  private boolean menuSearchEnabled = true;

  /**
   * Application address parameter name.
   * Default value address
   */
  private String address = "address";

  /**
   * Reload current screen.
   * Default value false
   */
  private boolean reloadCurrentScreen = false;

  /**
   * Enable XML definition hot reload (development only).
   * Default value false
   */
  private boolean xmlHotReload = false;

  /**
   * Source directories to watch for XML hot reload (e.g. src/main/resources). Changed XML
   * files are copied to the matching classpath location before reloading.
   * Default value empty list
   */
  private List<String> xmlHotReloadSources = new ArrayList<>();

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
   * Url of the AWE chart server, which renders the charts of the server (ECharts model to SVG).
   * Default value http://localhost:7801
   */
  private String chartServerUrl = "http://localhost:7801";

  /**
   * XStream driver
   */
  @Valid
  private XStreamDriverType xstreamDriverType = XStreamDriverType.DOM;

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
  @Valid
  private Files files = new Files();

  /**
   * Application paths configuration.
   */
  @NestedConfigurationProperty
  @Valid
  private Paths paths = new Paths();

  /**
   * Application parameters configuration.
   */
  @NestedConfigurationProperty
  @Valid
  private Parameter parameter = new Parameter();

  /**
   * Jms engine configuration properties.
   */
  @NestedConfigurationProperty
  @Valid
  private Jms jms = new Jms();

  /**
   * Screen names configuration.
   */
  @NestedConfigurationProperty
  @Valid
  private Screen screen = new Screen();

  /**
   * AWE components configuration parameters.
   */
  @NestedConfigurationProperty
  @Valid
  private Component component = new Component();

  /**
   * Map component configuration parameters.
   */
  @NestedConfigurationProperty
  @Valid
  private MapSettings map = new MapSettings();

  /**
   * Sign-in page branding: layout, logos, background image and texts.
   */
  @NestedConfigurationProperty
  @Valid
  private Login login = new Login();

  /**
   * Application AWE file manager parameters configuration.
   */
  @NestedConfigurationProperty
  @Valid
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
     * Tag angular folder path.
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
     * Sign-in screen logo. Uses application.paths.images as parent path. Deprecated for the sign-in page: use
     * awe.application.login.logo instead (this value is its fallback); in AngularJS it still styles the change
     * password and two-factor screens.
     * Default value ../images/logo/logo-awe-nuevo.svg
     */
    private String imageStartupLogo = "../" + images + "logo/logo-awe-nuevo.svg";

    /**
     * Navigation bar logo. Uses application.paths.images as parent path.
     * Default value ../images/logo/awe-logo-mark.svg
     */
    private String imageNavbarLogo = "../" + images + "logo/awe-logo-mark.svg";

    /**
     * Sign-in screen background image. Uses application.paths.images as parent path. Deprecated for the sign-in page:
     * use awe.application.login.background instead (this value is its fallback); in AngularJS it still styles the
     * change password and two-factor screens.
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
   * Configuration properties of the sign-in page. Texts are a locale key or a literal; an empty text is not shown.
   * Images are browser-resolvable URLs: relative to the application root, absolute or data URIs.
   */
  @Data
  public static class Login {
    /**
     * Layout of the sign-in page: split (the form column next to the background panel), form-only (the form centred
     * alone) or card (the form in a card over the full-screen background). Any other value is split.
     * Default value split
     */
    private String layout = "split";

    /**
     * Logo of the sign-in page. When empty, awe.application.paths.image-startup-logo if changed, else the AWE logo.
     * Default value empty
     */
    private String logo;

    /**
     * Logo of the sign-in page in dark mode.
     * Default value empty (the logo)
     */
    private String logoDark;

    /**
     * Background image of the sign-in page: the panel of the split layout, the backdrop of the card layout. When
     * empty, awe.application.paths.image-startup-background if changed, else the AWE brand panel.
     * Default value empty
     */
    private String background;

    /**
     * Name of a canvas effect the application registers for the image panel of the sign-in page (the split panel and
     * the card backdrop; the form-only layout has no background). AWE ships no effect: the application registers the
     * effects it writes in its own bundle and selects one here. Empty means no effect, so the background image stays.
     * Default value empty
     */
    private String backgroundEffect;

    /**
     * Title of the sign-in form.
     * Default value SCREEN_TEXT_LOGIN_TITLE ("Welcome to AWE")
     */
    private String title = "SCREEN_TEXT_LOGIN_TITLE";

    /**
     * Subtitle of the sign-in form.
     * Default value SCREEN_TEXT_LOGIN_SUBTITLE ("Enter your credentials to continue.")
     */
    private String subtitle = "SCREEN_TEXT_LOGIN_SUBTITLE";

    /**
     * Title of the caption over the background image.
     * Default value SCREEN_TEXT_LOGIN_PANEL_TITLE ("AWE Framework")
     */
    private String panelTitle = "SCREEN_TEXT_LOGIN_PANEL_TITLE";

    /**
     * Text of the caption over the background image.
     * Default value SCREEN_TEXT_LOGIN_PANEL_SUBTITLE ("Low coding complete functional web applications")
     */
    private String panelSubtitle = "SCREEN_TEXT_LOGIN_PANEL_SUBTITLE";

    /**
     * Footer text of the sign-in page, for instance the copyright.
     * Default value empty
     */
    private String footerText;

    /**
     * Show "Powered by AWE" in the footer of the sign-in page.
     * Default value true
     */
    private boolean poweredBy = true;

    /**
     * Show the language selector in the footer of the sign-in page, with the languages of
     * awe.application.language-list.
     * Default value true
     */
    private boolean languageSelector = true;

    /**
     * Show the light, dark and system colour mode selector in the footer of the sign-in page.
     * Default value true
     */
    private boolean modeSelector = true;
  }

  /**
   * Configuration properties of the map component
   */
  @Data
  public static class MapSettings {
    /**
     * Url template of the map tiles, with the {z}, {x} and {y} placeholders. Mind the usage policy of the tile server:
     * the default OpenStreetMap server is meant for light use, so use your own tile server for heavy traffic.
     * Default value https://tile.openstreetmap.org/{z}/{x}/{y}.png
     */
    @NotNull
    private String tileUrl = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
  }

  /**
   * Configuration properties of awe components
   */
  @Data
  public static class Component {
    /**
     * Component size
     * Default value md (Medium)
     */
    @Valid
    private ComponentSize size = ComponentSize.MD;
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
     * Loading indicator of the grids that do not define icon-loading: skeleton (placeholder rows with the grid columns on
     * every load), skeleton-dim (placeholder rows on the first load, dimmed rows on a reload) or any other icon-loading
     * value (spinner, dots...)
     * Default value skeleton
     */
    private String gridIconLoading = "skeleton";
    /**
     * How the toolbar of the grids that do not define columns-panel shows their Columns: dialog (a modal dialog) or
     * side-panel (a panel anchored to the right edge of the screen)
     * Default value dialog
     */
    private String gridColumnsPanel = "dialog";
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
    @Valid
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
     * Limit of points series in chart component.
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
