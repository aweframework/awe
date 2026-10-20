package com.almis.awe.autoconfigure;

import com.almis.awe.config.PasswordEncoderType;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.security.authentication.encoder.Ripemd160PasswordEncoder;
import com.almis.awe.service.user.AweUserDetailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Database authentication provider password rehash tests")
class DaoAuthenticationProviderRehashTest {

  private static final String USER = "jorgito";
  private static final String PASSWORD = "secret";

  private final AuthenticationConfig config = new AuthenticationConfig();

  private PasswordEncoder encoder(PasswordEncoderType type) {
    SecurityConfigProperties properties = new SecurityConfigProperties();
    properties.setPasswordEncoder(type);
    return config.passwordEncoder(properties);
  }

  private AweUserDetailService aweUserDetailService(String storedHash) {
    AweUserDetailService userDetailService = mock(AweUserDetailService.class);
    when(userDetailService.loadUserByUsername(USER))
      .thenReturn(new AweUserDetails().setUsername(USER).setPassword(storedHash));
    when(userDetailService.updatePassword(any(), anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    return userDetailService;
  }

  private Authentication login(DaoAuthenticationProvider provider) {
    return provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(USER, PASSWORD));
  }

  @Test
  @DisplayName("A legacy RIPEMD-160 hash signs in and is rehashed with BCrypt")
  void legacyHash_isRehashedOnLogin() {
    AweUserDetailService userDetailService = aweUserDetailService(new Ripemd160PasswordEncoder().encode(PASSWORD));
    DaoAuthenticationProvider provider = config.daoAuthenticationProvider(userDetailService, encoder(PasswordEncoderType.BCRYPT));

    assertThat(login(provider).isAuthenticated()).isTrue();
    verify(userDetailService).updatePassword(any(), startsWith("{bcrypt}"));
  }

  @Test
  @DisplayName("A BCrypt hash signs in without being rehashed")
  void bcryptHash_isNotRehashed() {
    PasswordEncoder encoder = encoder(PasswordEncoderType.BCRYPT);
    AweUserDetailService userDetailService = aweUserDetailService(encoder.encode(PASSWORD));
    DaoAuthenticationProvider provider = config.daoAuthenticationProvider(userDetailService, encoder);

    assertThat(login(provider).isAuthenticated()).isTrue();
    verify(userDetailService, never()).updatePassword(any(), anyString());
  }

  @Test
  @DisplayName("With the RIPEMD-160 opt-out legacy hashes are not rehashed")
  void ripemd160OptOut_doesNotRehash() {
    AweUserDetailService userDetailService = aweUserDetailService(new Ripemd160PasswordEncoder().encode(PASSWORD));
    DaoAuthenticationProvider provider = config.daoAuthenticationProvider(userDetailService, encoder(PasswordEncoderType.RIPEMD160));

    assertThat(login(provider).isAuthenticated()).isTrue();
    verify(userDetailService, never()).updatePassword(any(), anyString());
  }

  @Test
  @DisplayName("A custom user details service keeps signing in with legacy hashes")
  void customUserDetailsService_signsInWithLegacyHash() {
    String legacyHash = new Ripemd160PasswordEncoder().encode(PASSWORD);
    UserDetailsService custom = username -> new AweUserDetails().setUsername(username).setPassword(legacyHash);
    DaoAuthenticationProvider provider = config.daoAuthenticationProvider(custom, encoder(PasswordEncoderType.BCRYPT));

    assertThat(login(provider).isAuthenticated()).isTrue();
  }
}
