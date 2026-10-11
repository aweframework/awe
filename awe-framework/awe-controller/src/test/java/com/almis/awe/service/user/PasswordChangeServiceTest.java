package com.almis.awe.service.user;

import com.almis.awe.config.AuthType;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.security.authentication.encoder.Ripemd160PasswordEncoder;
import com.almis.awe.service.MaintainService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordChangeServiceTest {

  private static final String USER = "jorgito";
  private static final String OLD_PASSWORD = "oldpass";
  private final Ripemd160PasswordEncoder encoder = new Ripemd160PasswordEncoder();

  @Mock
  private ApplicationContext context;
  @Mock
  private AweElements elements;
  @Mock
  private AweUserDetailService userDetailService;
  @Mock
  private MaintainService maintainService;
  @Mock
  private AweSession aweSession;

  private SecurityConfigProperties securityConfigProperties;
  private PasswordChangeService service;
  private MockHttpSession session;
  private Instant now;

  @BeforeEach
  void setUp() {
    securityConfigProperties = new SecurityConfigProperties();
    securityConfigProperties.setPasswordMinLength(4);
    securityConfigProperties.setPasswordPattern("^(?=.*[0-9]).*$");
    now = Instant.parse("2026-10-10T10:00:00Z");
    service = new PasswordChangeService(securityConfigProperties, userDetailService, maintainService, encoder,
      Clock.fixed(now, ZoneOffset.UTC));
    service.setApplicationContext(context);
    session = new MockHttpSession();
    lenient().when(context.getBean(AweElements.class)).thenReturn(elements);
    lenient().when(context.getBean(AweSession.class)).thenReturn(aweSession);
    lenient().when(elements.getLanguage()).thenReturn("en-GB");
    lenient().when(elements.getLocaleWithLanguage(anyString(), eq("en-GB"))).thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(userDetailService.loadUserByUsername(USER)).thenReturn(user(true, true));
  }

  private AweUserDetails user(boolean enabled, boolean nonLocked) {
    return new AweUserDetails()
      .setUsername(USER)
      .setPassword(encoder.encode(OLD_PASSWORD))
      .setEnabled(enabled)
      .setAccountNonLocked(nonLocked);
  }

  @Test
  void changeWithoutPendingRequest_isRejectedAndNothingIsUpdated() {
    AWException exception = assertThrows(AWException.class, () -> service.changePassword(session, "newpass1"));

    assertEquals("ERROR_MESSAGE_PASSWORD_CHANGE_NOT_ALLOWED", exception.getMessage());
    assertEquals(AnswerType.WARNING, exception.getType());
    verifyNoInteractions(maintainService);
  }

  @Test
  void changeWithoutSession_isRejected() {
    assertThrows(AWException.class, () -> service.changePassword(null, "newpass1"));
    verifyNoInteractions(maintainService);
  }

  @Test
  void validChange_updatesThePasswordOfTheUserBoundToTheRequest() throws Exception {
    service.allowPasswordChange(session, USER);
    when(maintainService.launchPrivateMaintain(eq(PasswordChangeService.UPDATE_MAINTAIN), any(ObjectNode.class)))
      .thenReturn(new ServiceData());

    ServiceData result = service.changePassword(session, "newpass1");

    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq(PasswordChangeService.UPDATE_MAINTAIN), parameters.capture());
    // The user is not a parameter: it is a server-side session parameter that only lives during the update
    assertFalse(parameters.getValue().has("Usr"));
    assertEquals("newpass1", parameters.getValue().get("Pas").asText());
    InOrder order = inOrder(aweSession, maintainService);
    order.verify(aweSession).setParameter(PasswordChangeService.UPDATE_USER_SESSION_PARAMETER, USER);
    order.verify(maintainService).launchPrivateMaintain(eq(PasswordChangeService.UPDATE_MAINTAIN), any(ObjectNode.class));
    order.verify(aweSession).removeParameter(PasswordChangeService.UPDATE_USER_SESSION_PARAMETER);
    assertEquals(AnswerType.OK, result.getType());
    assertEquals("OK_MESSAGE_EXPIRED_PASSWORD_CHANGED", result.getMessage());
    assertEquals("screen", result.getClientActionList().get(0).getType());
  }

  @Test
  void failedUpdate_stillRemovesTheUpdateUser() throws Exception {
    service.allowPasswordChange(session, USER);
    when(maintainService.launchPrivateMaintain(eq(PasswordChangeService.UPDATE_MAINTAIN), any(ObjectNode.class)))
      .thenThrow(new AWException("title", "message"));

    assertThrows(AWException.class, () -> service.changePassword(session, "newpass1"));

    verify(aweSession).removeParameter(PasswordChangeService.UPDATE_USER_SESSION_PARAMETER);
  }

  @Test
  void withoutAweSession_nothingIsUpdated() throws Exception {
    service.allowPasswordChange(session, USER);
    when(context.getBean(AweSession.class)).thenThrow(new IllegalStateException("no session"));

    assertThrows(AWException.class, () -> service.changePassword(session, "newpass1"));

    verifyNoInteractions(maintainService);
  }

  @Test
  void requestStoredTwice_keepsOnlyTheLastOne() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session);
    lenient().when(userDetailService.loadUserByUsername("other")).thenReturn(new AweUserDetails()
      .setUsername("other").setPassword(encoder.encode(OLD_PASSWORD)).setEnabled(true).setAccountNonLocked(true));
    when(maintainService.launchPrivateMaintain(eq(PasswordChangeService.UPDATE_MAINTAIN), any(ObjectNode.class)))
      .thenReturn(new ServiceData());

    service.allowPasswordChange(request, USER);
    service.allowPasswordChange(request, "other");
    service.changePassword(session, "newpass1");

    verify(aweSession).setParameter(PasswordChangeService.UPDATE_USER_SESSION_PARAMETER, "other");
    verify(aweSession, never()).setParameter(PasswordChangeService.UPDATE_USER_SESSION_PARAMETER, USER);
  }

  @Test
  void storingTheRequest_rotatesTheSessionId() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session);
    String before = session.getId();

    assertTrue(service.allowPasswordChange(request, USER));

    assertNotEquals(before, request.getSession().getId());
    assertNotNull(session.getAttribute(PasswordChangeService.SESSION_ATTRIBUTE));
  }

  @Test
  void clearingTheRequest_discardsThePendingChange() {
    service.allowPasswordChange(session, USER);

    service.clearPasswordChange(session);
    service.clearPasswordChange(null);

    assertThrows(AWException.class, () -> service.changePassword(session, "newpass1"));
    verifyNoInteractions(maintainService);
  }

  @Test
  void concurrentChanges_changeThePasswordOnlyOnce() throws Exception {
    service.allowPasswordChange(session, USER);
    CountDownLatch inside = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    when(maintainService.launchPrivateMaintain(eq(PasswordChangeService.UPDATE_MAINTAIN), any(ObjectNode.class)))
      .thenAnswer(invocation -> {
        inside.countDown();
        release.await(5, TimeUnit.SECONDS);
        return new ServiceData();
      });

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<ServiceData> first = executor.submit(() -> service.changePassword(session, "newpass1"));
      assertTrue(inside.await(5, TimeUnit.SECONDS));
      // The first call is updating: the request is already taken, a second call cannot use it
      Future<ServiceData> second = executor.submit(() -> service.changePassword(session, "newpass2"));
      assertThrows(ExecutionException.class, () -> second.get(5, TimeUnit.SECONDS));
      release.countDown();
      assertEquals(AnswerType.OK, first.get(5, TimeUnit.SECONDS).getType());
    } finally {
      release.countDown();
      executor.shutdownNow();
    }

    verify(maintainService, times(1)).launchPrivateMaintain(anyString(), any(ObjectNode.class));
  }

  @Test
  void validChange_isSingleUse() throws Exception {
    service.allowPasswordChange(session, USER);
    when(maintainService.launchPrivateMaintain(eq(PasswordChangeService.UPDATE_MAINTAIN), any(ObjectNode.class)))
      .thenReturn(new ServiceData());
    service.changePassword(session, "newpass1");

    assertThrows(AWException.class, () -> service.changePassword(session, "newpass2"));

    verify(maintainService, times(1)).launchPrivateMaintain(anyString(), any(ObjectNode.class));
  }

  @Test
  void expiredRequest_isRejectedAndDiscarded() {
    service.allowPasswordChange(session, USER);
    PasswordChangeService later = new PasswordChangeService(securityConfigProperties, userDetailService,
      maintainService, encoder, Clock.fixed(now.plus(Duration.ofMinutes(6)), ZoneOffset.UTC));
    later.setApplicationContext(context);

    assertThrows(AWException.class, () -> later.changePassword(session, "newpass1"));
    // The request is gone even for a later try inside the validity window of the clock
    assertThrows(AWException.class, () -> service.changePassword(session, "newpass1"));

    verifyNoInteractions(maintainService);
  }

  @Test
  void passwordBelowTheMinimumLength_isRejectedAndTheRequestIsSpent() {
    service.allowPasswordChange(session, USER);

    AWException exception = assertThrows(AWException.class, () -> service.changePassword(session, "a1"));

    assertEquals("ERROR_MESSAGE_PASSWORD_POLICY", exception.getMessage());
    // Strictly single use: the user starts again from the login
    assertNull(session.getAttribute(PasswordChangeService.SESSION_ATTRIBUTE));
    assertThrows(AWException.class, () -> service.changePassword(session, "newpass1"));
    verifyNoInteractions(maintainService);
  }

  @Test
  void passwordNotMatchingThePattern_isRejected() {
    service.allowPasswordChange(session, USER);

    AWException exception = assertThrows(AWException.class, () -> service.changePassword(session, "newpassword"));

    assertEquals("ERROR_MESSAGE_PASSWORD_POLICY", exception.getMessage());
    verifyNoInteractions(maintainService);
  }

  @Test
  void blankPassword_isRejected() {
    service.allowPasswordChange(session, USER);

    assertThrows(AWException.class, () -> service.changePassword(session, " "));
    assertThrows(AWException.class, () -> service.changePassword(session, null));

    verifyNoInteractions(maintainService);
  }

  @Test
  void samePasswordAsTheExpiredOne_isRejected() {
    securityConfigProperties.setPasswordPattern(".*");
    service.allowPasswordChange(session, USER);

    AWException exception = assertThrows(AWException.class, () -> service.changePassword(session, OLD_PASSWORD));

    assertEquals("ERROR_MESSAGE_PASSWORD_REPEATED", exception.getMessage());
    assertNull(session.getAttribute(PasswordChangeService.SESSION_ATTRIBUTE));
    verifyNoInteractions(maintainService);
  }

  @Test
  void disabledOrLockedUser_isRejectedAndTheRequestIsDiscarded() {
    service.allowPasswordChange(session, USER);
    when(userDetailService.loadUserByUsername(USER)).thenReturn(user(true, false), user(true, true));

    assertThrows(AWException.class, () -> service.changePassword(session, "newpass1"));
    // The request has been discarded: it is not usable again even if the user is fine now
    assertThrows(AWException.class, () -> service.changePassword(session, "newpass1"));

    verifyNoInteractions(maintainService);
  }

  @Test
  void passwordChangeScreenAction_goesToThePublicChangeScreen() {
    ClientAction action = service.getPasswordChangeScreenAction();

    assertEquals("screen", action.getType());
    assertEquals("screen", action.getContext());
    assertEquals("change-expired-password", ((CellData) action.getParameterMap().get("screen")).getStringValue());
  }

  @Test
  void ldapAuthentication_doesNotAllowToChangeTheLocalPassword() {
    securityConfigProperties.setAuthMode(AuthType.LDAP);

    assertFalse(service.allowPasswordChange(session, USER));

    assertThrows(AWException.class, () -> service.changePassword(session, "newpass1"));

    verifyNoInteractions(maintainService);
  }
}
