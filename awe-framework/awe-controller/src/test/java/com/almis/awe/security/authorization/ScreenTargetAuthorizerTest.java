package com.almis.awe.security.authorization;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.ScreenAccessMode;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.ScreenRestriction;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.security.audit.SecurityAuditService;
import com.almis.awe.service.menu.MenuOptionLookup;
import com.almis.awe.service.menu.MenuProvider;
import com.almis.awe.service.screen.ScreenRestrictionGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests of the authorizer of the targets launched by a client (issue #914)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScreenTargetAuthorizerTest {

  private static final String TARGET = "UsrUpd";

  @Mock private ApplicationContext applicationContext;
  @Mock private AweSession aweSession;
  @Mock private ScreenTargetIndex index;
  @Mock private MenuOptionLookup menuOptionLookup;
  @Mock private SecurityAuditService securityAuditService;
  @Mock private ObjectProvider<SecurityAuditService> auditProvider;

  private SecurityConfigProperties properties;
  private ScreenTargetAuthorizer authorizer;

  @BeforeEach
  void setUp() {
    properties = new SecurityConfigProperties();
    authorizer = new ScreenTargetAuthorizer(properties, index, menuOptionLookup);
    authorizer.setApplicationContext(applicationContext);
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(applicationContext.getBeanProvider(SecurityAuditService.class)).thenReturn(auditProvider);
    when(auditProvider.getIfAvailable()).thenReturn(securityAuditService);
    when(securityAuditService.isEnabled()).thenReturn(true);
    when(aweSession.isAuthenticated()).thenReturn(true);
  }

  private void givenIndexed(String... options) {
    when(index.getOptionsUsing(TARGET)).thenReturn(Set.of(options));
  }

  private void givenAvailable(String option, String menu) throws Exception {
    when(menuOptionLookup.isAvailableOption(option, menu)).thenReturn(true);
  }

  @Test
  @DisplayName("Mode off does not check anything")
  void offDoesNotCheck() {
    properties.setScreenAccess(ScreenAccessMode.OFF);
    givenIndexed("users");

    assertThat(authorizer.authorize(TARGET, ScreenTargetAuthorizer.Kind.MAINTAIN)).isTrue();
    verify(index, never()).getOptionsUsing(anyString());
  }

  @Test
  @DisplayName("A target that no screen uses is allowed")
  void unindexedTargetIsAllowed() {
    when(index.getOptionsUsing(TARGET)).thenReturn(Set.of());

    assertThat(authorizer.authorize(TARGET, ScreenTargetAuthorizer.Kind.QUERY)).isTrue();
    verify(securityAuditService, never()).failure(any(), any(), any(), any());
  }

  @Test
  @DisplayName("A target is allowed when the user can open an option that uses it")
  void availableOptionAllows() throws Exception {
    givenIndexed("users", "profiles");
    givenAvailable("profiles", "private");

    assertThat(authorizer.authorize(TARGET, ScreenTargetAuthorizer.Kind.MAINTAIN)).isTrue();
    verify(securityAuditService, never()).failure(any(), any(), any(), any());
  }

  @Test
  @DisplayName("A target is refused in enforce mode when no option that uses it is available, and it is audited")
  void restrictedIsRefusedAndAudited() {
    givenIndexed("users", "profiles");

    assertThat(authorizer.authorize(TARGET, ScreenTargetAuthorizer.Kind.MAINTAIN)).isFalse();
    verify(securityAuditService).failure(eq(SecurityAuditEventType.ACCESS_DENIED), any(),
      eq(SecurityAuditCause.RESTRICTED), eq("maintain: " + TARGET));
  }

  @Test
  @DisplayName("A refused query is audited as a query")
  void refusedQueryIsAuditedAsQuery() {
    givenIndexed("users");

    assertThat(authorizer.authorize(TARGET, ScreenTargetAuthorizer.Kind.QUERY)).isFalse();
    verify(securityAuditService).failure(eq(SecurityAuditEventType.ACCESS_DENIED), any(),
      eq(SecurityAuditCause.RESTRICTED), eq("query: " + TARGET));
  }

  @Test
  @DisplayName("Warn mode audits the launch without access but allows it")
  void warnAuditsAndAllows() {
    properties.setScreenAccess(ScreenAccessMode.WARN);
    givenIndexed("users");

    assertThat(authorizer.authorize(TARGET, ScreenTargetAuthorizer.Kind.MAINTAIN)).isTrue();
    verify(securityAuditService).failure(eq(SecurityAuditEventType.ACCESS_DENIED), any(),
      eq(SecurityAuditCause.RESTRICTED), eq("maintain: " + TARGET));
  }

  @Test
  @DisplayName("A refusal is not lost when the audit is disabled or fails")
  void refusalSurvivesAuditProblems() {
    givenIndexed("users");
    when(securityAuditService.isEnabled()).thenReturn(false);
    assertThat(authorizer.authorize(TARGET, ScreenTargetAuthorizer.Kind.MAINTAIN)).isFalse();

    when(securityAuditService.isEnabled()).thenReturn(true);
    org.mockito.Mockito.doThrow(new IllegalStateException("sink down")).when(securityAuditService)
      .failure(any(), any(), any(), any());
    assertThat(authorizer.authorize(TARGET, ScreenTargetAuthorizer.Kind.MAINTAIN)).isFalse();
  }

  @Test
  @DisplayName("An anonymous caller is allowed by an option of the public menu")
  void anonymousAllowedByPublicOption() throws Exception {
    when(aweSession.isAuthenticated()).thenReturn(false);
    givenIndexed("login");
    givenAvailable("login", "public");

    assertThat(authorizer.authorize(TARGET, ScreenTargetAuthorizer.Kind.QUERY)).isTrue();
    verify(menuOptionLookup, never()).isAvailableOption(anyString(), eq("private"));
  }

  @Test
  @DisplayName("An anonymous caller is refused when only private options use the target")
  void anonymousRefusedByPrivateOption() throws Exception {
    when(aweSession.isAuthenticated()).thenReturn(false);
    givenIndexed("users");
    givenAvailable("users", "private");

    assertThat(authorizer.authorize(TARGET, ScreenTargetAuthorizer.Kind.QUERY)).isFalse();
  }

  @Test
  @DisplayName("An authenticated user is allowed by an option of the public menu")
  void authenticatedAllowedByPublicOption() throws Exception {
    givenIndexed("help");
    givenAvailable("help", "public");

    assertThat(authorizer.authorize(TARGET, ScreenTargetAuthorizer.Kind.QUERY)).isTrue();
  }

  @Test
  @DisplayName("The options under a restricted parent option are restricted")
  void optionsUnderRestrictedParentAreRestricted() throws Exception {
    Option parent = new Option();
    parent.setName("tools");
    Option child = new Option();
    child.setName("users");
    parent.addElement(child);
    Menu menu = new Menu();
    menu.addElement(parent);
    ScreenRestriction restriction = new ScreenRestriction();
    restriction.setOption("tools");
    restriction.setRestricted(true);
    restriction.setProfile("operator");
    new ScreenRestrictionGenerator().applyScreenRestriction(List.of(restriction), menu);

    MenuProvider menuProvider = org.mockito.Mockito.mock(MenuProvider.class);
    when(menuProvider.getMenuWithRestrictions("private")).thenReturn(menu);
    MenuOptionLookup realLookup = new MenuOptionLookup(menuProvider, new BaseConfigProperties());

    assertThat(realLookup.isAvailableOption("users", "private")).isFalse();
    assertThat(realLookup.isAvailableOption("tools", "private")).isFalse();
  }
}
