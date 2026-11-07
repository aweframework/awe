package com.almis.awe.security.authorization;

import com.almis.awe.config.AuthorizationMode;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.permissions.PermissionCatalog;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.services.Service;
import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.security.audit.SecurityAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The permission check of the targets launched by a client: a query, a maintain target or a service that declares a
 * permission runs only when the session user has all of them as PERM_ authorities
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionTargetAuthorizerTest {

  @Mock private ApplicationContext applicationContext;
  @Mock private AweElements aweElements;
  @Mock private SecurityAuditService securityAuditService;
  @Mock private ObjectProvider<SecurityAuditService> auditProvider;

  private SecurityConfigProperties properties;
  private PermissionTargetAuthorizer authorizer;

  @BeforeEach
  void setUp() throws Exception {
    properties = new SecurityConfigProperties();
    authorizer = new PermissionTargetAuthorizer(properties);
    authorizer.setApplicationContext(applicationContext);
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(applicationContext.getBeanProvider(SecurityAuditService.class)).thenReturn(auditProvider);
    when(auditProvider.getIfAvailable()).thenReturn(securityAuditService);
    when(securityAuditService.isEnabled()).thenReturn(true);
    when(aweElements.getQuery("UsrLst")).thenReturn(new Query().setId("UsrLst").setPermission("users.read"));
    when(aweElements.getQuery("Free")).thenReturn(new Query().setId("Free"));
    when(aweElements.getMaintain("UsrDel"))
      .thenReturn(new Target().setName("UsrDel").setPermission("users.read  users.delete"));
    when(aweElements.getService("UsrExp"))
      .thenReturn(Service.builder().id("UsrExp").permission("users.read").build());
    when(aweElements.getQuery("OwnData")).thenReturn(new Query().setId("OwnData").setPermission("self.read"));
    when(aweElements.getQuery("OwnAndUsers"))
      .thenReturn(new Query().setId("OwnAndUsers").setPermission("self.read users.read"));
    PermissionCatalog catalog = mock(PermissionCatalog.class);
    when(catalog.getGrantedAlways()).thenReturn(Set.of("self.read", "self.update"));
    when(aweElements.getPermissionCatalog()).thenReturn(catalog);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private static void signedInWith(String... authorities) {
    SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("jane", null, authorities));
  }

  @Test
  void aTargetWithoutPermissionIsAllowed() {
    signedInWith("ROLE_operator");

    assertThat(authorizer.authorize("Free", TargetKind.QUERY)).isTrue();
    verify(securityAuditService, never()).failure(any(), any(), any(), any());
  }

  @Test
  void aUserWithThePermissionIsAllowed() {
    signedInWith("ROLE_operator", "PERM_users.read");

    assertThat(authorizer.authorize("UsrLst", TargetKind.QUERY)).isTrue();
    verify(securityAuditService, never()).failure(any(), any(), any(), any());
  }

  @Test
  void aPermissionGrantedAlwaysNeedsNoAuthority() {
    signedInWith("ROLE_operator");

    assertThat(authorizer.authorize("OwnData", TargetKind.QUERY)).isTrue();
    assertThat(authorizer.authorize("OwnAndUsers", TargetKind.QUERY)).isFalse();
  }

  @Test
  void aPermissionGrantedAlwaysIsNotHeldWithoutAnAuthenticatedUser() {
    assertThat(authorizer.authorize("OwnData", TargetKind.QUERY)).isFalse();

    SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "anonymousUser",
      AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    assertThat(authorizer.authorize("OwnData", TargetKind.QUERY)).isFalse();
  }

  @Test
  void everyPermissionOfTheAttributeIsRequired() {
    signedInWith("PERM_users.read");

    assertThat(authorizer.authorize("UsrDel", TargetKind.MAINTAIN)).isFalse();

    signedInWith("PERM_users.read", "PERM_users.delete");
    assertThat(authorizer.authorize("UsrDel", TargetKind.MAINTAIN)).isTrue();
  }

  @Test
  void enforceRefusesAndAuditsWithoutTheUserPermissions() {
    signedInWith("PERM_users.read", "PERM_profiles.read");

    assertThat(authorizer.authorize("UsrDel", TargetKind.MAINTAIN)).isFalse();
    verify(securityAuditService).failure(eq(SecurityAuditEventType.ACCESS_DENIED), any(),
      eq(SecurityAuditCause.PERMISSION), eq("maintain: UsrDel"));
  }

  @Test
  void aRefusedQueryIsAuditedAsAQuery() {
    signedInWith("ROLE_operator");

    assertThat(authorizer.authorize("UsrLst", TargetKind.QUERY)).isFalse();
    verify(securityAuditService).failure(eq(SecurityAuditEventType.ACCESS_DENIED), any(),
      eq(SecurityAuditCause.PERMISSION), argThat("query: UsrLst"::equals));
  }

  @Test
  void warnAuditsThatTheLaunchWasAllowed() {
    properties.getAuthorization().setMode(AuthorizationMode.WARN);
    signedInWith("ROLE_operator");

    assertThat(authorizer.authorize("UsrLst", TargetKind.QUERY)).isTrue();
    verify(securityAuditService).failure(eq(SecurityAuditEventType.ACCESS_DENIED), any(),
      eq(SecurityAuditCause.PERMISSION), eq("query: UsrLst (allowed: warn)"));
  }

  @Test
  void offDoesNotCheck() {
    properties.getAuthorization().setMode(AuthorizationMode.OFF);
    signedInWith("ROLE_operator");

    assertThat(authorizer.authorize("UsrLst", TargetKind.QUERY)).isTrue();
    verify(securityAuditService, never()).failure(any(), any(), any(), any());
  }

  @Test
  void withoutAuthenticationATargetWithPermissionIsRefused() {
    assertThat(authorizer.authorize("UsrLst", TargetKind.QUERY)).isFalse();
  }

  @Test
  void anUnknownTargetIsLeftToTheServices() throws Exception {
    when(aweElements.getMaintain("Missing")).thenReturn(null);

    assertThat(authorizer.authorize("Missing", TargetKind.MAINTAIN)).isTrue();
  }

  @Test
  void aRefusalIsNotLostWhenTheAuditFails() {
    when(securityAuditService.isEnabled()).thenThrow(new IllegalStateException("audit down"));
    signedInWith("ROLE_operator");

    assertThat(authorizer.authorize("UsrLst", TargetKind.QUERY)).isFalse();
  }

  @Test
  void theChainChecksTheAccessByScreenFirst() {
    Map<String, Object> beans = new LinkedHashMap<>();
    beans.put("permissions", authorizer);
    beans.put("screens", new ScreenTargetAuthorizer(properties, new com.almis.awe.config.BaseConfigProperties(), null, null));
    beans.put("custom", (TargetLaunchAuthorizer) (targetId, kind) -> true);

    assertThat(new StaticListableBeanFactory(beans).getBeanProvider(TargetLaunchAuthorizer.class).orderedStream())
      .extracting(link -> link.getClass().getSimpleName())
      .startsWith("ScreenTargetAuthorizer", "PermissionTargetAuthorizer");
  }

  @Test
  void aServiceIsRefusedAndAuditedWithoutItsPermissions() {
    signedInWith("ROLE_operator");

    assertThat(authorizer.authorize("UsrExp", TargetKind.SERVICE)).isFalse();
    verify(securityAuditService).failure(eq(SecurityAuditEventType.ACCESS_DENIED), any(),
      eq(SecurityAuditCause.PERMISSION), eq("service: UsrExp"));
  }

  @Test
  void aServiceIsAllowedWithItsPermissions() {
    signedInWith("PERM_users.read");

    assertThat(authorizer.authorize("UsrExp", TargetKind.SERVICE)).isTrue();
    verify(securityAuditService, never()).failure(any(), any(), any(), any());
  }

  @Test
  void aServiceWithoutPermissionOrUnknownIsAllowed() throws Exception {
    when(aweElements.getService("Free")).thenReturn(Service.builder().id("Free").build());
    when(aweElements.getService("Missing")).thenReturn(null);
    signedInWith("ROLE_operator");

    assertThat(authorizer.authorize("Free", TargetKind.SERVICE)).isTrue();
    assertThat(authorizer.authorize("Missing", TargetKind.SERVICE)).isTrue();
  }

  @Test
  void canLaunchDecidesWithoutLoggingNorAuditingInAnyMode() throws Exception {
    when(aweElements.getMaintain("Missing")).thenReturn(null);
    properties.getAuthorization().setMode(AuthorizationMode.OFF);
    signedInWith("PERM_users.read");

    assertThat(authorizer.canLaunch("UsrLst", TargetKind.QUERY)).isTrue();
    assertThat(authorizer.canLaunch("UsrDel", TargetKind.MAINTAIN)).isFalse();
    assertThat(authorizer.canLaunch("Free", TargetKind.QUERY)).isTrue();
    assertThat(authorizer.canLaunch("OwnData", TargetKind.QUERY)).isTrue();
    assertThat(authorizer.canLaunch("Missing", TargetKind.MAINTAIN)).isTrue();
    verify(securityAuditService, never()).failure(any(), any(), any(), any());
  }

  /**
   * Targets without permission, of every kind, used by the require-undeclared tests
   */
  private void undeclaredTargets() throws Exception {
    when(aweElements.getMaintain("FreeUpd")).thenReturn(new Target().setName("FreeUpd"));
    when(aweElements.getService("FreeSvc")).thenReturn(Service.builder().id("FreeSvc").build());
  }

  @Test
  void withoutRequireUndeclaredATargetWithoutPermissionNeedsNoDefaultPermission() throws Exception {
    undeclaredTargets();
    signedInWith("ROLE_operator");

    assertThat(authorizer.authorize("Free", TargetKind.QUERY)).isTrue();
    assertThat(authorizer.authorize("FreeUpd", TargetKind.MAINTAIN)).isTrue();
    assertThat(authorizer.authorize("FreeSvc", TargetKind.SERVICE)).isTrue();
    verify(securityAuditService, never()).failure(any(), any(), any(), any());
  }

  @Test
  void requireUndeclaredRefusesAndAuditsAQueryWithoutPermissionUnlessTheUserHasDefaultRead() {
    properties.getPermissions().setRequireUndeclared(true);
    signedInWith("ROLE_operator", "PERM_default.write");

    assertThat(authorizer.authorize("Free", TargetKind.QUERY)).isFalse();
    verify(securityAuditService).failure(eq(SecurityAuditEventType.ACCESS_DENIED), any(),
      eq(SecurityAuditCause.PERMISSION), eq("query: Free"));

    signedInWith("ROLE_operator", "PERM_default.read");
    assertThat(authorizer.authorize("Free", TargetKind.QUERY)).isTrue();
  }

  @Test
  void requireUndeclaredRefusesAndAuditsAMaintainOrAServiceWithoutPermissionUnlessTheUserHasDefaultWrite()
    throws Exception {
    undeclaredTargets();
    properties.getPermissions().setRequireUndeclared(true);
    signedInWith("ROLE_operator", "PERM_default.read");

    assertThat(authorizer.authorize("FreeUpd", TargetKind.MAINTAIN)).isFalse();
    assertThat(authorizer.authorize("FreeSvc", TargetKind.SERVICE)).isFalse();
    verify(securityAuditService).failure(eq(SecurityAuditEventType.ACCESS_DENIED), any(),
      eq(SecurityAuditCause.PERMISSION), eq("maintain: FreeUpd"));
    verify(securityAuditService).failure(eq(SecurityAuditEventType.ACCESS_DENIED), any(),
      eq(SecurityAuditCause.PERMISSION), eq("service: FreeSvc"));

    signedInWith("ROLE_operator", "PERM_default.write");
    assertThat(authorizer.authorize("FreeUpd", TargetKind.MAINTAIN)).isTrue();
    assertThat(authorizer.authorize("FreeSvc", TargetKind.SERVICE)).isTrue();
  }

  @Test
  void requireUndeclaredIsFollowedByCanLaunch() throws Exception {
    undeclaredTargets();
    properties.getPermissions().setRequireUndeclared(true);
    signedInWith("ROLE_operator", "PERM_default.read");

    assertThat(authorizer.canLaunch("Free", TargetKind.QUERY)).isTrue();
    assertThat(authorizer.canLaunch("FreeUpd", TargetKind.MAINTAIN)).isFalse();
    verify(securityAuditService, never()).failure(any(), any(), any(), any());
  }

  @Test
  void requireUndeclaredKeepsTheDeclaredPermissionAsTheOnlyRequirement() {
    properties.getPermissions().setRequireUndeclared(true);
    signedInWith("PERM_users.read");

    assertThat(authorizer.authorize("UsrLst", TargetKind.QUERY)).isTrue();
    assertThat(authorizer.authorize("UsrExp", TargetKind.SERVICE)).isTrue();
  }

  @Test
  void requireUndeclaredLeavesPublicServerOnlyDispatcherAndUnknownTargetsAsTheyAre() throws Exception {
    when(aweElements.getQuery("PubLst")).thenReturn(Query.builder().id("PubLst").isPublic(true).build());
    when(aweElements.getQuery("SrvLst")).thenReturn(Query.builder().id("SrvLst").serverOnly(true).build());
    when(aweElements.getMaintain("PubUpd")).thenReturn(Target.builder().name("PubUpd").isPublic(true).build());
    when(aweElements.getMaintain("SrvUpd")).thenReturn(Target.builder().name("SrvUpd").serverOnly(true).build());
    when(aweElements.getService("launchMaintain"))
      .thenReturn(Service.builder().id("launchMaintain").authorization("target").build());
    when(aweElements.getMaintain("Missing")).thenReturn(null);
    properties.getPermissions().setRequireUndeclared(true);
    signedInWith("ROLE_operator");

    assertThat(authorizer.authorize("PubLst", TargetKind.QUERY)).isTrue();
    assertThat(authorizer.authorize("SrvLst", TargetKind.QUERY)).isTrue();
    assertThat(authorizer.authorize("PubUpd", TargetKind.MAINTAIN)).isTrue();
    assertThat(authorizer.authorize("SrvUpd", TargetKind.MAINTAIN)).isTrue();
    assertThat(authorizer.authorize("launchMaintain", TargetKind.SERVICE)).isTrue();
    assertThat(authorizer.authorize("Missing", TargetKind.MAINTAIN)).isTrue();
    verify(securityAuditService, never()).failure(any(), any(), any(), any());
  }

  @Test
  void hasPermissionsRequiresEveryPermissionOfTheAttribute() {
    signedInWith("PERM_users.read");

    assertThat(authorizer.hasPermissions("users.read self.update")).isTrue();
    assertThat(authorizer.hasPermissions("users.read users.delete")).isFalse();
    verify(securityAuditService, never()).failure(any(), any(), any(), any());
  }
}
