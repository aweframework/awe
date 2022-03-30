package com.almis.awe.session;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.SessionConfigProperties;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.tracker.AweClientTracker;
import com.almis.awe.model.tracker.AweConnectionTracker;
import com.almis.awe.service.BroadcastService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.almis.awe.model.constant.AweConstants.SESSION_FAILURE;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AweSessionDetailsTest {

  @InjectMocks
  private AweSessionDetails aweSessionDetails;

  @Mock
  private BaseConfigProperties baseConfigProperties;

  @Mock
  private SessionConfigProperties sessionConfigProperties;

  @Mock
  private SecurityConfigProperties securityConfigProperties;

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
  private QueryService queryService;

  @Mock
  private BroadcastService broadcastService;

  @Mock
  private Authentication authentication;

  @Mock
  private SecurityContext securityContext;

  @BeforeEach
  public void setUp() {
    aweSessionDetails.setApplicationContext(applicationContext);
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
  }

  @Test
  void onLoginSuccess() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(new AweUserDetails());
    SecurityContextHolder.setContext(securityContext);
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    aweSessionDetails.onLoginSuccess();
    verify(aweSession, times(9)).setParameter(ArgumentMatchers.anyString(), ArgumentMatchers.any());
  }


  @Test
  void onLoginSuccessErrorSessionParameters() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(new AweUserDetails());
    SecurityContextHolder.setContext(securityContext);
    when(sessionConfigProperties.getParameter()).thenReturn(Stream.of(new String[][] {
            { "module", "MyModule" },
            { "site", "MySite" },
            { "database", "MyDB" },
    }).collect(Collectors.toMap(data -> data[0], data -> data[1])));
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    aweSessionDetails.onLoginSuccess();
    verify(aweSession, times(3)).setParameter(eq(SESSION_FAILURE), any());
  }

  @Test
  void onLoginSuccessNullDataList() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(new AweUserDetails());
    SecurityContextHolder.setContext(securityContext);
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    aweSessionDetails.onLoginSuccess();
    verify(aweSession, times(9)).setParameter(ArgumentMatchers.anyString(), ArgumentMatchers.any());
  }

  @Test
  void onLogoutSuccess() {
    when(aweSession.getUser()).thenReturn("user");
    when(aweSession.getSessionId()).thenReturn("session-id");
    when(connectionTracker.getUserConnectionsFromSession(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(Collections.emptySet());
    aweSessionDetails.onLogoutSuccess();
    verify(aweSession, times(9)).removeParameter(ArgumentMatchers.anyString());
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
    verify(aweSession, times(9)).removeParameter(ArgumentMatchers.anyString());
    verify(broadcastService, times(3)).broadcastMessageToUID(ArgumentMatchers.anyString(), ArgumentMatchers.any(ClientAction.class));
    verify(clientTracker, times(1)).removeObservers();
  }
}