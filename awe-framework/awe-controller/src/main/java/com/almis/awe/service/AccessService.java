package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.config.TotpConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweUserDetails;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;

import static com.almis.awe.model.constant.AweConstants.*;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.EMAIL;

/**
 * Manage application accesses
 */
@Service
@Slf4j
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
  public static final String UPDATE_OAUTH_ROLE = "UpdateOauthUserProfile";

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

    // Go to the corresponding screen depending on the second authentication factor
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
    return new ServiceData().addClientAction(new ClientAction(REDIRECT).setTarget(getRequest().getHttpRequest().getContextPath() + AZURE_OAUTH2_AUTHORIZATION_URL));
  }

	/**
	 * SSO login redirection - Multi-tenant aware
	 * Dynamically resolves the correct OAuth2 provider based on the current tenant
	 *
	 * @return ServiceData with redirect action to the appropriate OAuth2 provider
	 */
  public ServiceData loginWithSSO() {

		OAuth2UrlService oAuth2UrlService = this.getApplicationContext().getBean(OAuth2UrlService.class);
		Assert.notNull(oAuth2UrlService, "OAuth2UrlService bean not found");
		// Get the dynamic OAuth2 authorization URL based on the current tenant
		String oauth2Url = oAuth2UrlService.getOAuth2AuthorizationUrl(getRequest().getHttpRequest());
		log.info("Redirecting to tenant-specific OAuth2 provider: {}", oauth2Url);

		return new ServiceData().addClientAction(new ClientAction(REDIRECT).setTarget(oauth2Url));
	}

  /**
   * Manage Oauth success authentication
   * @param oAuth2Token Oauth2 token info
   * @return initial screen url for redirection
   * @throws AWException AWE exception
   */
  public String onAuthenticationSuccess(OAuth2AuthenticationToken oAuth2Token) throws AWException {

    // Get user details
    AweUserDetails userDetails = getUserDetails(oAuth2Token);

    // Store session details
    sessionDetails.onLoginSuccess(userDetails);

    // Generate initial URL
    Menu menu = menuService.getMenu();
    String initialUrl = "/" + menu.getScreenContext() + "/" + userDetails.getInitialScreen();

    // Store initial url in session
    getSession().setParameter(SESSION_INITIAL_URL, initialUrl);

    return initialUrl;
  }

	/**
	 * Retrieves user details from an OAuth2 authentication token.
	 * <p>
	 * Parses the token's principal attributes and associated roles to construct
	 * an instance of {@code AweUserDetails} which encapsulates user information.
	 *
	 * @param oauth2Token the OAuth2 authentication token containing user attributes and authorities
	 * @return an instance of {@code AweUserDetails} populated with user-specific details and roles
	 * @throws AWException if the user details cannot be loaded or an error occurs during processing
	 */
	private AweUserDetails getUserDetails(OAuth2AuthenticationToken oauth2Token) throws AWException {
		Map<String, Object> attributes = oauth2Token.getPrincipal().getAttributes();
		log.debug("Available attributes in token: {}", getAllAttributes(oauth2Token));

		String userName = mapUsernameFromOauthToken(attributes);
		String roleFromOAuth = userDetailsService.mapGrantedAuthorityProfile(oauth2Token.getAuthorities());

		return loadUserDetailsWithRoleSync(userName, roleFromOAuth, oauth2Token);
	}

	/**
	 * Maps and retrieves the username from the provided OAuth token attributes.
	 *
	 * @param attributes a Map containing the OAuth token attributes where the username attribute is expected to be present
	 * @return the username extracted from the OAuth token attributes
	 * @throws AWException if the username attribute is not found or is empty in the provided attributes
	 */
	private String mapUsernameFromOauthToken(Map<String, Object> attributes) throws AWException {
		String ssoUserNameAttribute = securityConfigProperties.getSso().getUserNameAttribute();
		Object userNameAttribute = attributes.get(ssoUserNameAttribute);

		if (userNameAttribute == null || userNameAttribute.toString().isEmpty()) {
			throw new AWException(getLocale("ERROR_TITLE_OAUTH_NO_USERNAME_ATTR"),	getLocale("ERROR_MESSAGE_OAUTH_NO_USERNAME_ATTR", ssoUserNameAttribute));
		}

		return userNameAttribute.toString();
	}

	/**
	 * Loads user details for the given username, synchronizing with the provided role from OAuth.
	 * If the user is not found in the database, attempts to provision a new user using the OAuth token.
	 *
	 * @param userName the username of the user whose details are to be loaded
	 * @param roleFromOAuth the role information retrieved from OAuth to be checked/updated
	 * @param oauth2Token the OAuth2 authentication token used to provision a new user if needed
	 * @return an instance of AweUserDetails containing the user's details
	 * @throws AWException if there is an issue during the process
	 */
	private AweUserDetails loadUserDetailsWithRoleSync(String userName, String roleFromOAuth, OAuth2AuthenticationToken oauth2Token) throws AWException {
		try {
			AweUserDetails userDetails = userDetailsService.loadUserByUsername(userName);
			checkUpdateRoleInOAuth(userDetails, roleFromOAuth);
			return userDetails;
		} catch (UsernameNotFoundException ex) {
			// User not found in the database, provision a new one
			return loadAndProvisionNewUser(oauth2Token);
		}
	}

	private AweUserDetails loadAndProvisionNewUser(OAuth2AuthenticationToken oauth2Token) throws AWException {
		AweUserDetails userDetails = userDetailsService.loadUserByRole(oauth2Token);
		autoProvisionUser(userDetails);
		return userDetails;
	}

	/**
	 * Gets all available attributes from the OAuth2 token (for debugging)
	 *
	 * @param oauth2Token The OAuth2 token
	 * @return Map of all attributes
	 */
	private Map<String, Object> getAllAttributes(OAuth2AuthenticationToken oauth2Token) {
		if (oauth2Token != null && oauth2Token.getPrincipal() != null) {
			return oauth2Token.getPrincipal().getAttributes();
		}
		return Map.of();
	}
  private void checkUpdateRoleInOAuth(AweUserDetails userDetails, String roleOAuth) throws AWException {
    boolean changed = !userDetails.getProfileName().equalsIgnoreCase(roleOAuth);
    if (changed && userDetailsService.existRole(roleOAuth)) {
     // Update profile
      ObjectNode parameters = JsonNodeFactory.instance.objectNode();
      parameters.put(USERNAME, userDetails.getUsername());
      parameters.put(PROFILE, roleOAuth);
      maintainService.launchPrivateMaintain(UPDATE_OAUTH_ROLE, parameters);
    }
  }

  private void autoProvisionUser(AweUserDetails userDetails) throws AWException {
    if (securityConfigProperties.getSso().isAutoProvisionUser()) {
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
   * Go to the second factor authentication screen
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
   * Go to the second factor authentication screen
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
   * Logs out the current user and prepares the necessary client actions for logout behavior.
   * <p>
   * The method determines the redirect screen based on the SSO configuration and prepares
   * client actions such as screen redirection, language change, and theme change.
   *
   * @return a ServiceData object containing client actions to handle the logout process.
   */
  public ServiceData logout() {

    // Get the logout screen to redirect
    ClientAction screenActionBuilder = sessionDetails.createLogoutRedirectAction();

    return new ServiceData().addClientAction(screenActionBuilder);
  }

  /**
   * Check if the user is authenticated
   *
   * @return User is authenticated
   * @throws AWException Error checking authentication
   */
  public boolean isAuthenticated() throws AWException {
    return getSession().isAuthenticated();
  }

  /**
   * Get a profile restriction list
   *
   * @return Profile restriction file list
   */
  public ServiceData getProfileNameFileList() {
    // Get the profile file list
    ServiceData serviceData = new ServiceData();
    DataList dataList = new DataList();

    Set<String> profileList = getElements().getProfileList();
    for (String profile : profileList) {
      Map<String, CellData> row = new HashMap<>();
      // Set the screen name
      row.put(JSON_VALUE_PARAMETER, new CellData(profile));

      // Store screen label
      row.put(JSON_LABEL_PARAMETER, new CellData(profile));

      // Store row
      dataList.addRow(row);
    }

    // Set records
    dataList.setRecords(dataList.getRows().size());

    // Sort results
    DataListUtil.sort(dataList, JSON_LABEL_PARAMETER, "asc");

    // Set datalist to service
    serviceData.setDataList(dataList);
    return serviceData;
  }

  /**
   * Encrypts a text parameter with the algorithm RipEmd160
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
   * Encrypts a text parameter with the algorithm RipEmd160
   *
   * @param textToEncrypt text to encrypt
   * @param phraseKey     phrase key
   * @return Service Data with text encrypted
   */
  public ServiceData encryptProperty(String textToEncrypt, String phraseKey) {

    String key = Optional.ofNullable(phraseKey).filter(StringUtils::isNotBlank).orElse(securityConfigProperties.getMasterKey());

    // Get encode bean
    PooledPBEStringEncryptor encryptor = getPooledPBEStringEncryptor(key);

    // Encode the text
    String textEncrypted = "ENC(" + encryptor.encrypt(textToEncrypt) + ")";
    DataList dataList = new DataList();
    DataListUtil.addColumnWithOneRow(dataList, "encoded", textEncrypted);
    return new ServiceData()
      .setDataList(dataList);
  }

  @NotNull
  private PooledPBEStringEncryptor getPooledPBEStringEncryptor(String key) {
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
    return encryptor;
  }
}
