package com.almis.awe.service;

import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.queries.DatabaseConnection;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.security.authorization.ScreenTargetAuthorizer;
import com.almis.awe.service.data.connector.maintain.MaintainLauncher;
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
import org.springframework.jdbc.datasource.DataSourceUtils;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests of the access by screen to the maintain targets launched by a client (issue #914)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaintainServiceScreenAccessTest {

  private static final String UNAUTHORIZED = "unauthorized";
  private static final String TARGET = "UsrUpd";

  @Mock private MaintainLauncher maintainLauncher;
  @Mock private QueryUtil queryUtil;
  @Mock private DatabaseConfigProperties databaseConfigProperties;
  @Mock private ApplicationContext applicationContext;
  @Mock private AweElements aweElements;
  @Mock private AweSession aweSession;
  @Mock private DataSource dataSource;
  @Mock private Connection connection;
  @Mock private DatabaseConnection databaseConnection;
  @Mock private ScreenTargetAuthorizer authorizer;
  @Mock private ObjectProvider<ScreenTargetAuthorizer> authorizerProvider;

  private MaintainService maintainService;

  @BeforeEach
  void setUp() {
    maintainService = new MaintainService(maintainLauncher, queryUtil, databaseConfigProperties);
    maintainService.setApplicationContext(applicationContext);

    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(applicationContext.getBeanProvider(ScreenTargetAuthorizer.class)).thenReturn(authorizerProvider);
    when(authorizerProvider.getIfAvailable()).thenReturn(authorizer);
    when(aweElements.getLocaleWithLanguage(anyString(), any())).thenReturn("msg");
    when(aweElements.getLocaleWithLanguage(anyString(), any(), any())).thenReturn("msg");
    when(aweElements.getLocaleWithLanguage(eq("ERROR_MESSAGE_LAUNCHING_UNAUTHORIZED_MAINTAIN"), any(), any()))
      .thenReturn(UNAUTHORIZED);
    when(aweSession.isAuthenticated()).thenReturn(true);
    when(authorizer.authorize(TARGET, ScreenTargetAuthorizer.Kind.MAINTAIN)).thenReturn(true);

    when(databaseConnection.getDataSource()).thenReturn(dataSource);
    when(databaseConnection.getConnection()).thenReturn(connection);
    when(databaseConnection.hasConnection()).thenReturn(true);
    when(databaseConnection.getDatabaseAlias()).thenReturn("default");
  }

  private void givenTarget(Boolean isPublic) throws AWException {
    Target target = new Target().setName(TARGET).setIsPublic(isPublic).setQueryList(Collections.emptyList());
    when(aweElements.getMaintain(TARGET)).thenReturn(target);
  }

  private ObjectNode params() {
    return JsonNodeFactory.instance.objectNode();
  }

  @Test
  @DisplayName("A client launch is checked against the screens and refused when the authorizer refuses it")
  void clientLaunchIsRefused() throws Exception {
    givenTarget(null);
    when(authorizer.authorize(TARGET, ScreenTargetAuthorizer.Kind.MAINTAIN)).thenReturn(false);

    ObjectNode parameters = params();

    assertThatThrownBy(() -> maintainService.launchMaintain(TARGET, parameters, databaseConnection, false))
      .isInstanceOf(AWException.class)
      .satisfies(exc -> assertThat(((AWException) exc).getMessage()).isEqualTo(UNAUTHORIZED));
    verifyNoInteractions(maintainLauncher);
  }

  @Test
  @DisplayName("Every client launch variant is checked")
  void everyClientVariantIsChecked() throws Exception {
    givenTarget(null);
    when(authorizer.authorize(TARGET, ScreenTargetAuthorizer.Kind.MAINTAIN)).thenReturn(false);

    ObjectNode parameters = params();

    assertThatThrownBy(() -> maintainService.launchMaintain(TARGET, parameters)).isInstanceOf(AWException.class);
    assertThatThrownBy(() -> maintainService.launchMaintain(TARGET, databaseConnection, false))
      .isInstanceOf(AWException.class);
  }

  @Test
  @DisplayName("A client launch the authorizer allows goes on")
  void allowedClientLaunchGoesOn() throws Exception {
    givenTarget(null);

    try (var ignored = mockStatic(DataSourceUtils.class)) {
      ServiceData result = maintainService.launchMaintain(TARGET, params(), databaseConnection, false);
      assertThat(result).isNotNull();
    }
    verify(authorizer).authorize(TARGET, ScreenTargetAuthorizer.Kind.MAINTAIN);
  }

  @Test
  @DisplayName("A server launch is not checked")
  void serverLaunchIsNotChecked() throws Exception {
    givenTarget(null);
    when(authorizer.authorize(TARGET, ScreenTargetAuthorizer.Kind.MAINTAIN)).thenReturn(false);

    try (var ignored = mockStatic(DataSourceUtils.class)) {
      ServiceData result = maintainService.launchPrivateMaintain(TARGET, params(), databaseConnection, false);
      assertThat(result).isNotNull();
    }
    verify(authorizer, never()).authorize(any(), any());
  }

  @Test
  @DisplayName("A public target is not checked against the screens")
  void publicTargetIsNotChecked() throws Exception {
    givenTarget(true);
    when(aweSession.isAuthenticated()).thenReturn(false);
    when(authorizer.authorize(TARGET, ScreenTargetAuthorizer.Kind.MAINTAIN)).thenReturn(false);

    try (var ignored = mockStatic(DataSourceUtils.class)) {
      ServiceData result = maintainService.launchMaintain(TARGET, params(), databaseConnection, false);
      assertThat(result).isNotNull();
    }
    verify(authorizer, never()).authorize(any(), any());
  }

  @Test
  @DisplayName("An unauthenticated client is refused before the screens are checked")
  void unauthenticatedIsRefusedFirst() throws Exception {
    givenTarget(null);
    when(aweSession.isAuthenticated()).thenReturn(false);

    ObjectNode parameters = params();

    assertThatThrownBy(() -> maintainService.launchMaintain(TARGET, parameters, databaseConnection, false))
      .isInstanceOf(AWException.class);
    verify(authorizer, never()).authorize(any(), any());
  }

  @Test
  @DisplayName("Without an authorizer bean the launch is not checked")
  void withoutAuthorizerBean() throws Exception {
    givenTarget(null);
    when(authorizerProvider.getIfAvailable()).thenReturn(null);

    try (var ignored = mockStatic(DataSourceUtils.class)) {
      ServiceData result = maintainService.launchMaintain(TARGET, params(), databaseConnection, false);
      assertThat(result).isNotNull();
    }
  }
}
