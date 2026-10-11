package com.almis.awe.security.authentication;

import com.almis.awe.config.SecurityConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Local login authentication manager")
class LocalLoginAuthenticationManagerTest {

  @Mock
  private AuthenticationManager delegate;

  private final SecurityConfigProperties properties = new SecurityConfigProperties();
  private LocalLoginAuthenticationManager manager;

  @BeforeEach
  void setUp() {
    manager = new LocalLoginAuthenticationManager(delegate, new LocalLoginPolicy(properties));
  }

  private void switchOff() {
    properties.getSso().setEnabled(true);
    properties.getSso().setLocalLogin(false);
    properties.getSso().setBreakGlassUsers(List.of("admin"));
  }

  private Authentication authenticated(String name) {
    return UsernamePasswordAuthenticationToken.authenticated(name, null, List.of());
  }

  @Test
  @DisplayName("With SSO off the original authentication reaches the delegate untouched")
  void ssoOffIsUntouched() {
    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(" user ", "secret");
    Authentication result = authenticated("user");
    when(delegate.authenticate(token)).thenReturn(result);

    assertThat(manager.authenticate(token)).isSameAs(result);
    verify(delegate).authenticate(token);
  }

  @Test
  @DisplayName("With local login open the original authentication reaches the delegate untouched")
  void localLoginOpenIsUntouched() {
    properties.getSso().setEnabled(true);
    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(" user ", "secret");
    Authentication result = authenticated("user");
    when(delegate.authenticate(token)).thenReturn(result);

    assertThat(manager.authenticate(token)).isSameAs(result);
  }

  @Test
  @DisplayName("Tokens that are not a user name and password login are never touched")
  void otherTokensAreUntouched() {
    switchOff();
    UserDetails details = User.withUsername(" john ").password("x").authorities("ROLE_X").build();
    UsernamePasswordAuthenticationToken userDetailsPrincipal = new UsernamePasswordAuthenticationToken(details, "x");
    PreAuthenticatedAuthenticationToken preAuthenticated = new PreAuthenticatedAuthenticationToken("john", "n/a", AuthorityUtils.createAuthorityList("ROLE_X"));
    TestingAuthenticationToken testing = new TestingAuthenticationToken("john", "x");
    when(delegate.authenticate(any())).thenAnswer(invocation -> invocation.getArgument(0));

    assertThat(manager.authenticate(userDetailsPrincipal)).isSameAs(userDetailsPrincipal);
    assertThat(manager.authenticate(preAuthenticated)).isSameAs(preAuthenticated);
    assertThat(manager.authenticate(testing)).isSameAs(testing);
  }

  @Test
  @DisplayName("A break-glass login reaches the delegate with the checked name and keeps the details")
  void breakGlassUserIsAuthenticatedWithTheCheckedNameAndDetails() {
    switchOff();
    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(" admin ", "secret");
    WebAuthenticationDetails details = new WebAuthenticationDetails("127.0.0.1", "session");
    token.setDetails(details);
    Authentication result = authenticated("admin");
    when(delegate.authenticate(any())).thenReturn(result);

    assertThat(manager.authenticate(token)).isSameAs(result);

    ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
    verify(delegate).authenticate(captor.capture());
    assertThat(captor.getValue().getPrincipal()).isEqualTo("admin");
    assertThat(captor.getValue().getCredentials()).isEqualTo("secret");
    assertThat(captor.getValue().getDetails()).isSameAs(details);
  }

  @Test
  @DisplayName("A refused user still costs a real authentication, with a throw-away password, and never succeeds")
  void refusedUserStillRunsTheDelegateAndFailsWithBadCredentials() {
    switchOff();
    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("john", "right-password");
    when(delegate.authenticate(any())).thenThrow(new BadCredentialsException("anything"));

    assertThatThrownBy(() -> manager.authenticate(token))
      .isInstanceOf(LocalLoginDisabledException.class)
      .isInstanceOf(BadCredentialsException.class)
      .hasMessage(LocalLoginDisabledException.BAD_CREDENTIALS_MESSAGE);

    ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
    verify(delegate).authenticate(captor.capture());
    assertThat(captor.getValue().getPrincipal()).isEqualTo("john");
    assertThat(captor.getValue().getCredentials()).isNotEqualTo("right-password");
  }

  @Test
  @DisplayName("A refused user is refused even if the delegate would (wrongly) accept the throw-away password")
  void refusedUserNeverGetsAnAuthenticatedToken() {
    switchOff();
    when(delegate.authenticate(any())).thenReturn(authenticated("john"));

    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("john", "x");
    assertThatThrownBy(() -> manager.authenticate(token))
      .isInstanceOf(LocalLoginDisabledException.class);
  }

  @Test
  @DisplayName("A refused user that does not exist fails the same way")
  void refusedUnknownUser() {
    switchOff();
    when(delegate.authenticate(any())).thenThrow(new UsernameNotFoundException("john"));

    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("john", "x");
    assertThatThrownBy(() -> manager.authenticate(token))
      .isInstanceOf(LocalLoginDisabledException.class);
  }

  @Test
  @DisplayName("A locked or expired break-glass account is still refused by the real provider checks")
  void lockedOrExpiredBreakGlassAccountsAreRefused() {
    switchOff();
    InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
      User.withUsername("admin").password("secret").authorities("ROLE_X").accountLocked(true).build(),
      User.withUsername("root").password("secret").authorities("ROLE_X").accountExpired(true).build());
    properties.getSso().setBreakGlassUsers(List.of("admin", "root"));
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
    provider.setPasswordEncoder(NoOpPasswordEncoder.getInstance());
    LocalLoginAuthenticationManager real = new LocalLoginAuthenticationManager(new ProviderManager(provider), new LocalLoginPolicy(properties));

    UsernamePasswordAuthenticationToken locked = new UsernamePasswordAuthenticationToken("admin", "secret");
    UsernamePasswordAuthenticationToken expired = new UsernamePasswordAuthenticationToken("root", "secret");
    assertThatThrownBy(() -> real.authenticate(locked)).isInstanceOf(LockedException.class);
    assertThatThrownBy(() -> real.authenticate(expired)).isInstanceOf(AccountExpiredException.class);
  }
}
