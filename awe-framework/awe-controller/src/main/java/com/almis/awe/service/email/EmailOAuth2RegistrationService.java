package com.almis.awe.service.email;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security client registrations available for email servers with OAuth2 authentication
 * (spring.security.oauth2.client.registration.[id] with authorization-grant-type=client_credentials)
 */
public class EmailOAuth2RegistrationService extends ServiceConfig {

  // Null when the application has no OAuth2 client registration
  private final ClientRegistrationRepository clientRegistrationRepository;

  /**
   * Constructor
   *
   * @param clientRegistrationRepository Client registration repository (nullable)
   */
  public EmailOAuth2RegistrationService(ClientRegistrationRepository clientRegistrationRepository) {
    this.clientRegistrationRepository = clientRegistrationRepository;
  }

  /**
   * Retrieve the client_credentials registrations as select values
   *
   * @return Service data with value and label columns
   */
  public ServiceData getClientCredentialsRegistrations() {
    List<String> registrationIds = getClientCredentialsRegistrationIds();
    DataList dataList = new DataList();
    DataListUtil.addColumn(dataList, "value", registrationIds);
    DataListUtil.addColumn(dataList, "label", registrationIds);
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Retrieve the identifiers of the client_credentials registrations
   *
   * @return Registration identifiers
   */
  public List<String> getClientCredentialsRegistrationIds() {
    List<String> registrationIds = new ArrayList<>();
    if (clientRegistrationRepository instanceof Iterable<?> registrations) {
      for (Object registration : registrations) {
        if (registration instanceof ClientRegistration clientRegistration && isClientCredentials(clientRegistration)) {
          registrationIds.add(clientRegistration.getRegistrationId());
        }
      }
    }
    return registrationIds;
  }

  /**
   * Retrieve the client_credentials registration of an email server
   *
   * @param serverName     Email server name
   * @param registrationId Registration identifier
   * @return Client registration
   * @throws AWException Registration unknown or not client_credentials
   */
  public ClientRegistration getClientCredentialsRegistration(String serverName, String registrationId) throws AWException {
    ClientRegistration registration = clientRegistrationRepository == null || registrationId == null ? null :
      clientRegistrationRepository.findByRegistrationId(registrationId);
    if (registration == null) {
      throw new AWException(String.format("Email server '%s' uses an unknown OAuth2 client registration '%s'", serverName, registrationId));
    }
    if (!isClientCredentials(registration)) {
      throw new AWException(String.format("Email server '%s' uses OAuth2 client registration '%s', which is not client_credentials", serverName, registrationId));
    }
    return registration;
  }

  private static boolean isClientCredentials(ClientRegistration registration) {
    return AuthorizationGrantType.CLIENT_CREDENTIALS.equals(registration.getAuthorizationGrantType());
  }
}
