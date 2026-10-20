package com.almis.awe.service.user;

import com.almis.awe.config.PasswordEncoderType;
import com.almis.awe.dao.UserDAO;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.dto.User;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.security.authentication.encoder.AwePasswordEncoder;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.MaintainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import tools.jackson.databind.node.ObjectNode;

import static com.almis.awe.service.user.CurrentPasswordChangeService.OWN_UPDATE_MAINTAIN;
import static com.almis.awe.service.user.CurrentPasswordChangeService.PUBLIC_UPDATE_MAINTAIN;
import static com.almis.awe.service.user.CurrentPasswordChangeService.USER_SESSION_PARAMETER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Password changes that require the current password: it is checked in Java against the stored hash, and the update
 * targets read the user from a session parameter that only this service sets
 */
@ExtendWith(MockitoExtension.class)
class CurrentPasswordChangeServiceTest {

  private static final String USER = "jorgito";
  private static final String CURRENT_PASSWORD = "current-1";
  private static final String NEW_PASSWORD = "new-pass-2";

  private final AwePasswordEncoder encoder = new AwePasswordEncoder(PasswordEncoderType.BCRYPT);

  @Mock
  private ApplicationContext context;
  @Mock
  private UserDAO userDAO;
  @Mock
  private MaintainService maintainService;
  @Mock
  private AweSession aweSession;

  private CurrentPasswordChangeService service;

  @BeforeEach
  void setUp() {
    service = new CurrentPasswordChangeService(userDAO, encoder);
    service.setApplicationContext(context);
    lenient().when(context.getBean(MaintainService.class)).thenReturn(maintainService);
    lenient().when(context.getBean(AweSession.class)).thenReturn(aweSession);
  }

  @Test
  void publicChangeWithTheCurrentPasswordUpdatesTheStoredUser() throws AWException {
    // The request may name the user with another case or padding: the stored name is the one bound to the update
    when(userDAO.findByUserName(" JORGITO ")).thenReturn(user(encoder.encode(CURRENT_PASSWORD), true));

    ServiceData result = service.changePassword(" JORGITO ", CURRENT_PASSWORD, NEW_PASSWORD);

    assertUpdated(PUBLIC_UPDATE_MAINTAIN);
    assertSilentOk(result);
  }

  @Test
  void publicChangeAcceptsALegacyCurrentPasswordHash() throws AWException {
    when(userDAO.findByUserName(USER)).thenReturn(user(EncodeService.encodeRipEmd160(CURRENT_PASSWORD) + "   ", true));

    service.changePassword(USER, CURRENT_PASSWORD, NEW_PASSWORD);

    assertUpdated(PUBLIC_UPDATE_MAINTAIN);
  }

  @Test
  void publicChangeWithAWrongCurrentPasswordUpdatesNothing() throws AWException {
    when(userDAO.findByUserName(USER)).thenReturn(user(encoder.encode(CURRENT_PASSWORD), true));

    ServiceData result = service.changePassword(USER, "wrong-password", NEW_PASSWORD);

    assertNothingUpdated();
    assertSilentOk(result);
  }

  @Test
  void publicChangeWithoutTheCurrentPasswordUpdatesNothing() throws AWException {
    when(userDAO.findByUserName(USER)).thenReturn(user(encoder.encode(CURRENT_PASSWORD), true));

    ServiceData result = service.changePassword(USER, null, NEW_PASSWORD);

    assertNothingUpdated();
    assertSilentOk(result);
  }

  @Test
  void publicChangeOfAnInactiveUserUpdatesNothing() throws AWException {
    when(userDAO.findByUserName(USER)).thenReturn(user(encoder.encode(CURRENT_PASSWORD), false));

    ServiceData result = service.changePassword(USER, CURRENT_PASSWORD, NEW_PASSWORD);

    assertNothingUpdated();
    assertSilentOk(result);
  }

  @Test
  void publicChangeOfAnUnknownUserUpdatesNothing() throws AWException {
    when(userDAO.findByUserName(USER)).thenThrow(new UsernameNotFoundException(USER));

    ServiceData result = service.changePassword(USER, CURRENT_PASSWORD, NEW_PASSWORD);

    assertNothingUpdated();
    assertSilentOk(result);
  }

  @Test
  void publicChangeWithoutUserUpdatesNothing() throws AWException {
    ServiceData result = service.changePassword(null, CURRENT_PASSWORD, NEW_PASSWORD);

    assertNothingUpdated();
    verifyNoInteractions(userDAO);
    assertSilentOk(result);
  }

  @Test
  void theSessionParameterIsRemovedWhenTheUpdateFails() throws AWException {
    when(userDAO.findByUserName(USER)).thenReturn(user(encoder.encode(CURRENT_PASSWORD), true));
    AWException failure = new AWException("title", "message");
    when(maintainService.launchPrivateMaintain(eq(PUBLIC_UPDATE_MAINTAIN), any(ObjectNode.class))).thenThrow(failure);

    assertThatThrownBy(() -> service.changePassword(USER, CURRENT_PASSWORD, NEW_PASSWORD)).isSameAs(failure);

    verify(aweSession).removeParameter(USER_SESSION_PARAMETER);
  }

  @Test
  void ownChangeWithTheCurrentPasswordUpdatesTheSessionUser() throws AWException {
    when(aweSession.getUser()).thenReturn(USER);
    when(userDAO.findByUserName(USER)).thenReturn(user(encoder.encode(CURRENT_PASSWORD), true));

    ServiceData result = service.changeOwnPassword(CURRENT_PASSWORD, NEW_PASSWORD);

    assertUpdated(OWN_UPDATE_MAINTAIN);
    assertSilentOk(result);
  }

  @Test
  void ownChangeWithAWrongCurrentPasswordUpdatesNothing() throws AWException {
    when(aweSession.getUser()).thenReturn(USER);
    when(userDAO.findByUserName(USER)).thenReturn(user(encoder.encode(CURRENT_PASSWORD), true));

    ServiceData result = service.changeOwnPassword("wrong-password", NEW_PASSWORD);

    assertNothingUpdated();
    assertSilentOk(result);
  }

  @Test
  void ownChangeWithoutSessionUserUpdatesNothing() throws AWException {
    when(aweSession.getUser()).thenReturn(null);

    ServiceData result = service.changeOwnPassword(CURRENT_PASSWORD, NEW_PASSWORD);

    assertNothingUpdated();
    verifyNoInteractions(userDAO);
    assertSilentOk(result);
  }

  private void assertUpdated(String target) throws AWException {
    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    InOrder order = inOrder(aweSession, maintainService);
    order.verify(aweSession).setParameter(USER_SESSION_PARAMETER, USER);
    order.verify(maintainService).launchPrivateMaintain(eq(target), parameters.capture());
    order.verify(aweSession).removeParameter(USER_SESSION_PARAMETER);
    assertThat(parameters.getValue().get("Pas").asString()).isEqualTo(NEW_PASSWORD);
    assertThat(parameters.getValue().properties()).hasSize(1);
  }

  private void assertNothingUpdated() throws AWException {
    verify(maintainService, never()).launchPrivateMaintain(anyString(), any(ObjectNode.class));
    verify(aweSession, never()).setParameter(eq(USER_SESSION_PARAMETER), any());
  }

  /**
   * Both outcomes answer the same: the maintain fills its default title and message
   */
  private static void assertSilentOk(ServiceData result) {
    assertThat(result.getType()).isEqualTo(AnswerType.OK);
    assertThat(result.getTitle()).isEmpty();
    assertThat(result.getMessage()).isEmpty();
    assertThat(result.getResultDetails()).isEmpty();
    assertThat(result.getClientActionList()).isEmpty();
  }

  private static User user(String password, boolean enabled) {
    return new User().setUsername(USER).setPassword(password).setEnabled(enabled);
  }
}
