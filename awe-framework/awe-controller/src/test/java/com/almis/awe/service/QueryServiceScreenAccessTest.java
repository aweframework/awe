package com.almis.awe.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.security.authorization.ScreenTargetAuthorizer;
import com.almis.awe.service.data.connector.query.QueryLauncher;
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
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests of the access by screen to the queries launched by a client (issue #914)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QueryServiceScreenAccessTest {

  private static final String QUERY = "UserList";

  @Mock private QueryLauncher queryLauncher;
  @Mock private QueryUtil queryUtil;
  @Mock private ApplicationContext applicationContext;
  @Mock private AweElements aweElements;
  @Mock private AweRequest aweRequest;
  @Mock private AweSession aweSession;
  @Mock private ScreenTargetAuthorizer authorizer;
  @Mock private ObjectProvider<ScreenTargetAuthorizer> authorizerProvider;

  private QueryService queryService;

  @BeforeEach
  void setUp() throws Exception {
    queryService = new QueryService(queryLauncher, queryUtil);
    queryService.setApplicationContext(applicationContext);
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(applicationContext.getBeanProvider(ScreenTargetAuthorizer.class)).thenReturn(authorizerProvider);
    when(authorizerProvider.getIfAvailable()).thenReturn(authorizer);
    when(aweElements.getLocaleWithLanguage(anyString(), any())).thenReturn("msg");
    when(aweElements.getLocaleWithLanguage(anyString(), any(), any())).thenReturn("msg");
    when(aweSession.isAuthenticated()).thenReturn(true);
    when(aweRequest.getTargetAction()).thenReturn(QUERY);
    when(aweRequest.getToken()).thenReturn("token");
    when(aweRequest.getParameter(anyString())).thenReturn(JsonNodeFactory.instance.stringNode("{}"));
    when(queryUtil.getParameters(any(), any(), any())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryUtil.getParameters(any(ObjectNode.class))).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryUtil.getParameters()).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryLauncher.launchQuery(any(Query.class), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(new DataList()));
    when(aweElements.getQuery(QUERY)).thenReturn(new Query());
    when(authorizer.authorize(QUERY, ScreenTargetAuthorizer.Kind.QUERY)).thenReturn(true);
  }

  private void refuse() {
    when(authorizer.authorize(QUERY, ScreenTargetAuthorizer.Kind.QUERY)).thenReturn(false);
  }

  @Test
  @DisplayName("A client query launch the authorizer allows goes on")
  void allowedLaunchGoesOn() throws Exception {
    assertThat(queryService.launchQuery(QUERY)).isNotNull();
    verify(authorizer).authorize(QUERY, ScreenTargetAuthorizer.Kind.QUERY);
  }

  @Test
  @DisplayName("A client query launch the authorizer refuses is not launched")
  void refusedLaunchIsNotLaunched() throws Exception {
    refuse();

    assertThatThrownBy(() -> queryService.launchQuery(QUERY)).isInstanceOf(AWException.class);
    assertThatThrownBy(() -> queryService.launchQuery(QUERY, "alias")).isInstanceOf(AWException.class);
    assertThatThrownBy(() -> queryService.launchQuery(QUERY, "1", "0")).isInstanceOf(AWException.class);
    assertThatThrownBy(() -> queryService.launchQuery(QUERY, JsonNodeFactory.instance.objectNode()))
      .isInstanceOf(AWException.class);
    verify(queryLauncher, never()).launchQuery(any(Query.class), any(ObjectNode.class));
  }

  @Test
  @DisplayName("Every action that launches the target-action query is checked")
  void everyQueryActionIsChecked() {
    refuse();

    assertThatThrownBy(() -> queryService.launchQueryAction()).isInstanceOf(AWException.class);
    assertThatThrownBy(() -> queryService.updateModelAction()).isInstanceOf(AWException.class);
    assertThatThrownBy(() -> queryService.checkUniqueAction()).isInstanceOf(AWException.class);
    assertThatThrownBy(() -> queryService.checkEmptyAction()).isInstanceOf(AWException.class);
    assertThatThrownBy(() -> queryService.subscribe(QUERY, new ComponentAddress())).isInstanceOf(AWException.class);
  }

  @Test
  @DisplayName("A server query launch is not checked")
  void serverLaunchIsNotChecked() throws Exception {
    refuse();

    assertThat(queryService.launchPrivateQuery(QUERY)).isNotNull();
    assertThat(queryService.launchPrivateQuery(QUERY, "alias")).isNotNull();
    assertThat(queryService.launchPrivateQuery(QUERY, "1", "0")).isNotNull();
    assertThat(queryService.launchPrivateQuery(QUERY, JsonNodeFactory.instance.objectNode())).isNotNull();
    verify(authorizer, never()).authorize(any(), any());
  }

  @Test
  @DisplayName("A public query is not checked against the screens")
  void publicQueryIsNotChecked() throws Exception {
    refuse();
    when(aweSession.isAuthenticated()).thenReturn(false);
    when(aweElements.getQuery(QUERY)).thenReturn(new Query().setIsPublic(true));

    assertThat(queryService.launchQuery(QUERY)).isNotNull();
    verify(authorizer, never()).authorize(any(), any());
  }

  @Test
  @DisplayName("Without an authorizer bean the query launch is not checked")
  void withoutAuthorizerBean() throws Exception {
    when(authorizerProvider.getIfAvailable()).thenReturn(null);

    assertThat(queryService.launchQuery(QUERY)).isNotNull();
  }
}
