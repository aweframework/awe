package com.almis.awe.dao;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDAOImplTest {

  private static final String USER = "jorgito";
  private static final String HASH = "{bcrypt}$2a$10$abcdefghijklmnopqrstuv";

  @Mock
  private QueryUtil queryUtil;
  @Mock
  private QueryService queryService;
  @Mock
  private ApplicationContext context;
  @Mock
  private AweSession aweSession;
  @Mock
  private MaintainService maintainService;

  private UserDAOImpl userDAO;

  @BeforeEach
  void setUp() {
    userDAO = new UserDAOImpl(queryUtil, queryService);
    userDAO.setApplicationContext(context);
  }

  @Test
  void updatePassword_launchesTheRehashTargetWithTheValuesInTheSession() throws Exception {
    when(context.getBean(AweSession.class)).thenReturn(aweSession);
    when(context.getBean(MaintainService.class)).thenReturn(maintainService);

    assertThat(userDAO.updatePassword(USER, HASH)).isTrue();

    InOrder order = inOrder(aweSession, maintainService);
    order.verify(aweSession).setParameter(UserDAOImpl.REHASH_USER_SESSION_PARAMETER, USER);
    order.verify(aweSession).setParameter(UserDAOImpl.REHASH_VALUE_SESSION_PARAMETER, HASH);
    order.verify(maintainService).launchPrivateMaintain(eq(UserDAOImpl.REHASH_MAINTAIN), any(ObjectNode.class));
    order.verify(aweSession).removeParameter(UserDAOImpl.REHASH_USER_SESSION_PARAMETER);
    order.verify(aweSession).removeParameter(UserDAOImpl.REHASH_VALUE_SESSION_PARAMETER);
  }

  @Test
  void updatePassword_doesNotPassTheValuesAsRequestParameters() throws Exception {
    when(context.getBean(AweSession.class)).thenReturn(aweSession);
    when(context.getBean(MaintainService.class)).thenReturn(maintainService);

    userDAO.updatePassword(USER, HASH);

    verify(maintainService).launchPrivateMaintain(eq("UsrRehashPwd"), org.mockito.ArgumentMatchers.<ObjectNode>argThat(ObjectNode::isEmpty));
  }

  @Test
  void updatePassword_whenTheMaintainFails_clearsTheSessionAndReturnsFalse() throws Exception {
    when(context.getBean(AweSession.class)).thenReturn(aweSession);
    when(context.getBean(MaintainService.class)).thenReturn(maintainService);
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenThrow(new AWException("error", "value too long"));

    assertThat(userDAO.updatePassword(USER, HASH)).isFalse();

    verify(aweSession).removeParameter(UserDAOImpl.REHASH_USER_SESSION_PARAMETER);
    verify(aweSession).removeParameter(UserDAOImpl.REHASH_VALUE_SESSION_PARAMETER);
  }

  @Test
  void updatePassword_withoutSession_returnsFalseAndLaunchesNothing() throws Exception {
    when(context.getBean(AweSession.class)).thenThrow(new IllegalStateException("no session"));

    assertThat(userDAO.updatePassword(USER, HASH)).isFalse();

    verify(context, never()).getBean(MaintainService.class);
    verify(maintainService, never()).launchPrivateMaintain(anyString(), any(ObjectNode.class));
  }
}
