package com.almis.awe.session;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.dto.User;
import com.almis.awe.model.entities.Global;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.tracker.AweClientTracker;
import com.almis.awe.model.tracker.AweConnectionTracker;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.BroadcastService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.almis.awe.model.constant.AweConstants.SESSION_FAILURE;
import static com.almis.awe.model.constant.AweConstants.SESSION_USER_DETAILS;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AweSessionDetailsTest {

  @InjectMocks
  private AweSessionDetails aweSessionDetails;

  @Mock
  private AuthenticationException authenticationException;

  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private AweSession aweSession;

  @Mock
  private AweRequest aweRequest;

  @Mock
  private AweElements aweElements;

  @Mock
  private AweClientTracker clientTracker;

  @Mock
  private AweConnectionTracker connectionTracker;

  @Mock
  private User userDetails;

  @Mock
  private QueryService queryService;

  @Mock
  private BroadcastService broadcastService;

  @BeforeEach
  public void setUp() {
    aweSessionDetails.setApplicationContext(applicationContext);
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    ReflectionTestUtils.setField(aweSessionDetails, "sessionParameters", Arrays.asList("module", null, "site", "", "database"));
  }

  @Test
  void onLoginSuccess() throws AWException {
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweSession.getUser()).thenReturn("user");
    when(aweSession.getParameter(User.class, SESSION_USER_DETAILS)).thenReturn(userDetails);
    when(userDetails.getLanguage()).thenReturn("ES");
    when(userDetails.getUserTheme()).thenReturn("sunset");
    when(userDetails.getProfileTheme()).thenReturn("sky");
    when(queryService.launchQuery(ArgumentMatchers.eq(null), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(new ServiceData().setDataList(DataListUtil.fromBeanList(Collections.singletonList(new Global()))));
    aweSessionDetails.onLoginSuccess();
    verify(aweSession, times(12)).setParameter(ArgumentMatchers.anyString(), ArgumentMatchers.any());
  }

  @Test
  void onLoginSuccessErrorSessionParameters() {
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweSession.getUser()).thenReturn("user");
    when(aweSession.getParameter(User.class, SESSION_USER_DETAILS)).thenReturn(userDetails);
    when(userDetails.getLanguage()).thenReturn("ES");
    when(userDetails.getUserTheme()).thenReturn("sunset");
    when(userDetails.getProfileTheme()).thenReturn("sky");
    aweSessionDetails.onLoginSuccess();
    verify(aweSession, times(3)).setParameter(ArgumentMatchers.eq(SESSION_FAILURE), ArgumentMatchers.any());
  }

  @Test
  void onLoginSuccessNullDataList() throws AWException {
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweSession.getUser()).thenReturn("user");
    when(aweSession.getParameter(User.class, SESSION_USER_DETAILS)).thenReturn(userDetails);
    when(userDetails.getLanguage()).thenReturn("ES");
    when(userDetails.getUserTheme()).thenReturn("sunset");
    when(userDetails.getProfileTheme()).thenReturn("sky");
    when(queryService.launchQuery(ArgumentMatchers.eq(null), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(new ServiceData());
    aweSessionDetails.onLoginSuccess();
    verify(aweSession, times(9)).setParameter(ArgumentMatchers.anyString(), ArgumentMatchers.any());
  }

  @Test
  void onLoginFailure() {
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    aweSessionDetails.onLoginFailure(authenticationException);
    verify(aweSession, times(1)).setParameter(ArgumentMatchers.eq(SESSION_FAILURE), ArgumentMatchers.any());
  }

  @Test
  void onLoginFailureUsernameNotFound() {
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    aweSessionDetails.onLoginFailure(Mockito.mock(UsernameNotFoundException.class));
    verify(aweSession, times(1)).setParameter(ArgumentMatchers.eq(SESSION_FAILURE), ArgumentMatchers.any());
  }

  @Test
  void onLoginFailureBadCredentials() {
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    aweSessionDetails.onLoginFailure(Mockito.mock(BadCredentialsException.class));
    verify(aweSession, times(1)).setParameter(ArgumentMatchers.eq(SESSION_FAILURE), ArgumentMatchers.any());
  }

  @Test
  void onLogoutSuccess() {
    when(aweSession.getUser()).thenReturn("user");
    when(aweSession.getSessionId()).thenReturn("session-id");
    when(connectionTracker.getUserConnectionsFromSession(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(Collections.emptySet());
    aweSessionDetails.onLogoutSuccess();
    verify(aweSession, times(10)).removeParameter(ArgumentMatchers.anyString());
  }

  @Test
  void onLogoutSuccessCloseSessions() {
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    when(aweSession.getUser()).thenReturn("user");
    when(aweSession.getSessionId()).thenReturn("session-id");
    when(aweRequest.getToken()).thenReturn("3");
    when(connectionTracker.getUserConnectionsFromSession(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(Stream.of("1", "2", "3", null, "", "5").collect(Collectors.toSet()));
    aweSessionDetails.onLogoutSuccess();
    verify(clientTracker, times(1)).removeObservers();
    verify(aweSession, times(10)).removeParameter(ArgumentMatchers.anyString());
    verify(broadcastService, times(3)).broadcastMessageToUID(ArgumentMatchers.anyString(), ArgumentMatchers.any(ClientAction.class));
    verify(clientTracker, times(1)).removeObservers();
  }
}