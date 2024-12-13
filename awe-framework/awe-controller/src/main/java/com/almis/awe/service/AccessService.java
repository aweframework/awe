package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.config.TotpConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.user.AweUserDetailService;
import com.almis.awe.session.AweSessionDetails;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.almis.awe.model.constant.AweConstants.*;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;

/**
 * Manage application accesses
 */
@Service
public class AccessService extends ServiceConfig {

  // Autowired components
  private final AweSessionDetails sessionDetails;
  private final BaseConfigProperties baseConfigProperties;
  private final SecurityConfigProperties securityConfigProperties;
  private final EncodeService encodeService;
  private final MenuService menuService;
  private final TotpConfigProperties totpConfigProperties;
  private final TotpService totpService;
  private final AweUserDetailService userDetailsService;
  private final MaintainService maintainService;

  public static final String PROVISIONING_NEW_USER = "ProvisionNewUser";
  public static final String SCREEN = "screen";
  public static final String REDIRECT = "redirect";
  public static final String CHANGE_LANGUAGE = "change-language";
  public static final String CHANGE_THEME = "change-theme";
  private static final String EMAIL = "email";
  private static final String USERNAME = "userName";
  private static final String PROFILE = "profile";

  @Value("${jasypt.encryptor.algorithm:PBEWithMD5AndDES}")
  private String jasyptAlgorithm;
  @Value("${jasypt.encryptor.keyObtentionIterations:1000}")
  private Integer jasyptKeyObtentionIterations;
  @Value("${jasypt.encryptor.poolSize:1}")
  private Integer jasyptPoolSize;
  @Value("${jasypt.encryptor.providerName:SunJCE}")
  private String jasyptProviderName;
  @Value("${jasypt.encryptor.saltGeneratorClassname:org.jasypt.salt.RandomSaltGenerator}")
  private String jasyptSaltGeneratorClassname;
  @Value("${jasypt.encryptor.stringOutputType:base64}")
  private String jasyptStringOutputType;

  /**
   * AccessService constructor
   *
   * @param menuService              Menu service
   * @param sessionDetails           Session details
   * @param encodeService            Encode services
   * @param totpService              Totp service
   * @param baseConfigProperties     Base configuration properties
   * @param securityConfigProperties Security configuration properties
   * @param totpConfigProperties     Totp configuration properties
   * @param aweUserDetailService     Awe user details
   * @param maintainService          Maintain service
   */
  public AccessService(AweSessionDetails sessionDetails,
                       MenuService menuService,
                       EncodeService encodeService,
                       TotpService totpService,
                       BaseConfigProperties baseConfigProperties,
                       SecurityConfigProperties securityConfigProperties,
                       TotpConfigProperties totpConfigProperties,
                       AweUserDetailService aweUserDetailService,
                       MaintainService maintainService) {
    this.sessionDetails = sessionDetails;
    this.menuService = menuService;
    this.encodeService = encodeService;
    this.totpService = totpService;
    this.baseConfigProperties = baseConfigProperties;
    this.securityConfigProperties = securityConfigProperties;
    this.totpConfigProperties = totpConfigProperties;
    this.userDetailsService = aweUserDetailService;
    this.maintainService = maintainService;
  }

  /**
   * Performs the login action
   *
   * @return serviceData the result of the login
   * @throws AWException Error generating login
   */
  public ServiceData login() throws AWException {

    // Get user details
    AweUserDetails userDetails = ((AweUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal());

    // Go to corresponding screen depending on second authentication factor
    switch (totpConfigProperties.getEnabled()) {
      case OPTIONAL:
        if (userDetails.isEnabled2fa()) {
          return goTo2faScreen(userDetails);
        } else {
          return goToHomeScreen(userDetails);
        }
      case FORCE:
        if (StringUtils.isNotBlank(userDetails.getSecret2fa())) {
          return goTo2faScreen(userDetails);
        } else {
          return goToActivate2faScreen(userDetails);
        }
      case DISABLED:
      default:
        return goToHomeScreen(userDetails);
    }
  }

  /**
   * Azure EntraID redirection
   * @return do send redirect to Azure oauth portal
   */
  public ServiceData loginWithAzureEntraID() {
    return new ServiceData().addClientAction(new ClientAction(REDIRECT).setTarget(AZURE_OAUTH2_AUTHORIZATION_URL));
  }

  /**
   * Manage Oauth success authentication
   * @param oauth2User Oauth2 user info
   * @return initial screen url for redirection
   * @throws AWException AWE exception
   */
  public String onAuthenticationSuccess(DefaultOAuth2User oauth2User) throws AWException {

    // Get user details
    AweUserDetails userDetails = getUserDetails(oauth2User);

    // Store session details
    sessionDetails.onLoginSuccess(userDetails);

    // Generate initial URL
    Menu menu = menuService.getMenu();
    String initialUrl = "/" + menu.getScreenContext() + "/" + userDetails.getInitialScreen();

    // Store initial url in session
    getSession().setParameter(SESSION_INITIAL_URL, initialUrl);

    return initialUrl;
  }

  private AweUserDetails getUserDetails(DefaultOAuth2User oauth2User) throws AWException {
    AweUserDetails userDetails;
    String userEmail = oauth2User.getAttribute(PREFERRED_USERNAME);
    // Get user info from email
    userDetails = userDetailsService.loadUserByEmail(userEmail);

    if (userDetails == null) {
      // User not exist, retrieve generic user info from role
      userDetails = userDetailsService.loadUserByRole(oauth2User);
      // If is enabled, provision new user
      autoProvisionUser(userDetails);
    }
    return userDetails;
  }

  private void autoProvisionUser(AweUserDetails userDetails) throws AWException {
    if (securityConfigProperties.isAutoProvisionUser()) {
      // Provision new user
      ObjectNode parameters = JsonNodeFactory.instance.objectNode();
      parameters.put(EMAIL, userDetails.getEmail());
      parameters.put(USERNAME, userDetails.getUsername());
      parameters.put(SESSION_FULLNAME, userDetails.getName());
      parameters.put(PROFILE, userDetails.getProfile());
      parameters.put(SESSION_LANGUAGE, baseConfigProperties.getLanguageDefault());
      maintainService.launchPrivateMaintain(PROVISIONING_NEW_USER, parameters);
    }
  }

  /**
     * Go home screen
     *
     * @param userDetails User details
     * @return Service data
     * @throws AWException AWE exception
     */
  private ServiceData goToHomeScreen(AweUserDetails userDetails) throws AWException {
    // Store session details
    sessionDetails.onLoginSuccess(userDetails);

    // Generate initial URL
    Menu menu = menuService.getMenu();
    String initialUrl = "/" + menu.getScreenContext() + "/" + userDetails.getInitialScreen();

    // Store initial url in session
    getSession().setParameter(SESSION_INITIAL_URL, initialUrl);

    // Go home screen
    return new ServiceData()
      .addClientAction(new ClientAction(SCREEN)
        .addParameter(SESSION_CONNECTION_TOKEN, UUID.randomUUID())
        .addParameter(JSON_SCREEN, initialUrl))
      .addClientAction(new ClientAction(CHANGE_LANGUAGE)
        .addParameter(SESSION_LANGUAGE, userDetails.getLanguage()))
      .addClientAction(new ClientAction(CHANGE_THEME)
        .addParameter(SESSION_THEME, userDetails.getTheme()));
  }

  /**
   * Go to second factor authentication screen
   *
   * @param userDetails User details
   * @return Service data
   */
  private ServiceData goToActivate2faScreen(AweUserDetails userDetails) {
    return new ServiceData()
      .addClientAction(new ClientAction(SCREEN)
        .setContext(SCREEN)
        .addParameter(JSON_SCREEN, totpConfigProperties.getActivateScreen()))
      .addClientAction(new ClientAction(CHANGE_LANGUAGE)
        .addParameter(SESSION_LANGUAGE, userDetails.getLanguage()))
      .addClientAction(new ClientAction(CHANGE_THEME)
        .addParameter(SESSION_THEME, userDetails.getTheme()));
  }

  /**
   * Go to second factor authentication screen
   *
   * @param userDetails User details
   * @return Service data
   */
  private ServiceData goTo2faScreen(AweUserDetails userDetails) {
    return new ServiceData()
      .addClientAction(new ClientAction(SCREEN)
        .setContext(SCREEN)
        .addParameter(JSON_SCREEN, totpConfigProperties.getInitialScreen()))
      .addClientAction(new ClientAction(CHANGE_LANGUAGE)
        .addParameter(SESSION_LANGUAGE, userDetails.getLanguage()))
      .addClientAction(new ClientAction(CHANGE_THEME)
        .addParameter(SESSION_THEME, userDetails.getTheme()));
  }

  /**
   * Retrieve QR Code in PNG format as String
   *
   * @return QR Code as string
   */
  public ServiceData verify2faCode(String code) throws AWException {
    AweUserDetails userDetails = ((AweUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    if (totpService.verify2faCode(code)) {
      return goToHomeScreen(userDetails);
    } else {
      throw new AWException(getLocale("ERROR_TITLE_INVALID_2FA_CODE"), getLocale("ERROR_MESSAGE_INVALID_2FA_CODE"), AnswerType.WARNING);
    }
  }

  /**
   * Performs the logout action
   *
   * @return serviceData the result of the login
   */
  public ServiceData logout() {
    // Return to home screen
    return new ServiceData()
      .addClientAction(new ClientAction(SCREEN)
        .addParameter(SESSION_CONNECTION_TOKEN, UUID.randomUUID())
        .addParameter(JSON_SCREEN, "/"))
      .addClientAction(new ClientAction(CHANGE_LANGUAGE)
        .addParameter(SESSION_LANGUAGE, baseConfigProperties.getLanguageDefault()))
      .addClientAction(new ClientAction(CHANGE_THEME)
        .addParameter(SESSION_THEME, baseConfigProperties.getTheme()));
  }

  /**
   * Check if user is authenticated
   *
   * @return User is authenticated
   * @throws AWException Error checking authentication
   */
  public boolean isAuthenticated() throws AWException {
    return getSession().isAuthenticated();
  }

  /**
   * Get profile restriction list
   *
   * @return Profile restriction file list
   */
  public ServiceData getProfileNameFileList() {
    // Get profiles file list
    ServiceData serviceData = new ServiceData();
    DataList dataList = new DataList();

    Set<String> profileList = getElements().getProfileList();
    for (String profile : profileList) {
      Map<String, CellData> row = new HashMap<>();
      // Set screen name
      row.put(AweConstants.JSON_VALUE_PARAMETER, new CellData(profile));

      // Store screen label
      row.put(AweConstants.JSON_LABEL_PARAMETER, new CellData(profile));

      // Store row
      dataList.addRow(row);
    }

    // Set records
    dataList.setRecords(dataList.getRows().size());

    // Sort results
    DataListUtil.sort(dataList, AweConstants.JSON_LABEL_PARAMETER, "asc");

    // Set datalist to service
    serviceData.setDataList(dataList);
    return serviceData;
  }

  /**
   * Encrypts a text parameter with algorithm RipEmd160
   *
   * @param textToEncrypt text to encrypt
   * @param phraseKey     phrase key
   * @return Service Data with text encrypted
   * @throws AWException Error
   */
  public ServiceData encryptText(String textToEncrypt, String phraseKey) throws AWException {

    // Encode the text
    String textEncrypted = encodeService.encryptRipEmd160WithPhraseKey(textToEncrypt, phraseKey);
    DataList dataList = new DataList();
    DataListUtil.addColumnWithOneRow(dataList, "encoded", textEncrypted);
    return new ServiceData()
      .setDataList(dataList);
  }

  /**
   * Encrypts a text parameter with algorithm RipEmd160
   *
   * @param textToEncrypt text to encrypt
   * @param phraseKey     phrase key
   * @return Service Data with text encrypted
   */
  public ServiceData encryptProperty(String textToEncrypt, String phraseKey) {

    String key = Optional.ofNullable(phraseKey).filter(StringUtils::isNotBlank).orElse(securityConfigProperties.getMasterKey());

    // Get encode bean
    SimpleStringPBEConfig config = new SimpleStringPBEConfig();
    config.setPassword(key);
    config.setAlgorithm(jasyptAlgorithm);
    config.setKeyObtentionIterations(jasyptKeyObtentionIterations);
    config.setPoolSize(jasyptPoolSize);
    config.setProviderName(jasyptProviderName);
    config.setSaltGeneratorClassName(jasyptSaltGeneratorClassname);
    config.setStringOutputType(jasyptStringOutputType);
    PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
    encryptor.setConfig(config);

    // Encode the text
    String textEncrypted = "ENC(" + encryptor.encrypt(textToEncrypt) + ")";
    DataList dataList = new DataList();
    DataListUtil.addColumnWithOneRow(dataList, "encoded", textEncrypted);
    return new ServiceData()
      .setDataList(dataList);
  }
}
