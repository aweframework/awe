package com.almis.awe.service.email;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailOAuth2RegistrationServiceTest {

  private final EmailOAuth2RegistrationService service = new EmailOAuth2RegistrationService(new InMemoryClientRegistrationRepository(
    registration("sso", AuthorizationGrantType.AUTHORIZATION_CODE),
    registration("office365", AuthorizationGrantType.CLIENT_CREDENTIALS),
    registration("gmail", AuthorizationGrantType.CLIENT_CREDENTIALS)));

  @Test
  void listsOnlyClientCredentialsRegistrations() {
    ServiceData serviceData = service.getClientCredentialsRegistrations();

    assertThat(DataListUtil.getColumn(serviceData.getDataList(), "value")).extracting(cell -> cell.getStringValue())
      .containsExactlyInAnyOrder("office365", "gmail");
    assertThat(DataListUtil.getColumn(serviceData.getDataList(), "label")).extracting(cell -> cell.getStringValue())
      .containsExactlyInAnyOrder("office365", "gmail");
  }

  @Test
  void listsNoRegistrationWithoutRepository() {
    ServiceData serviceData = new EmailOAuth2RegistrationService(null).getClientCredentialsRegistrations();

    assertThat(serviceData.getDataList().getRows()).isEmpty();
  }

  @Test
  void rejectsRegistrationThatIsUnknownOrNotClientCredentials() throws AWException {
    assertThat(service.getClientCredentialsRegistration("Mail", "office365").getRegistrationId()).isEqualTo("office365");
    assertThatThrownBy(() -> service.getClientCredentialsRegistration("Mail", "unknown"))
      .isInstanceOf(AWException.class).hasMessageContainingAll("'Mail'", "'unknown'");
    assertThatThrownBy(() -> service.getClientCredentialsRegistration("Mail", "sso"))
      .isInstanceOf(AWException.class).hasMessageContainingAll("'Mail'", "'sso'", "client_credentials");
    assertThatThrownBy(() -> new EmailOAuth2RegistrationService(null).getClientCredentialsRegistration("Mail", "office365"))
      .isInstanceOf(AWException.class).hasMessageContainingAll("'Mail'", "'office365'");
  }

  private static ClientRegistration registration(String registrationId, AuthorizationGrantType grantType) {
    return ClientRegistration.withRegistrationId(registrationId)
      .clientId("client-id")
      .clientSecret("client-secret")
      .authorizationGrantType(grantType)
      .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
      .authorizationUri("https://provider.com/oauth/authorize")
      .tokenUri("https://provider.com/oauth/token")
      .build();
  }
}
