package com.almis.awe.service;

import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.queries.DatabaseConnection;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.data.connector.maintain.MaintainLauncher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import static org.mockito.Mockito.when;

/**
 * Tests for the {@code server-only} maintain targets (issue #886): a client launch is always refused,
 * a server launch still works.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaintainServiceServerOnlyTest {

  private static final String UNAUTHORIZED = "unauthorized";
  private static final String SERVER_ONLY_TARGET = "serverOnlyTarget";

  @Mock private MaintainLauncher maintainLauncher;
  @Mock private QueryUtil queryUtil;
  @Mock private DatabaseConfigProperties databaseConfigProperties;
  @Mock private ApplicationContext applicationContext;
  @Mock private AweElements aweElements;
  @Mock private AweSession aweSession;
  @Mock private DataSource dataSource;
  @Mock private Connection connection;
  @Mock private DatabaseConnection databaseConnection;

  private MaintainService maintainService;

  @BeforeEach
  void setUp() {
    maintainService = new MaintainService(maintainLauncher, queryUtil, databaseConfigProperties);
    maintainService.setApplicationContext(applicationContext);

    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(aweElements.getLocaleWithLanguage(anyString(), any())).thenReturn("msg");
    when(aweElements.getLocaleWithLanguage(anyString(), any(), any())).thenReturn("msg");
    when(aweElements.getLocaleWithLanguage(eq("ERROR_MESSAGE_LAUNCHING_UNAUTHORIZED_MAINTAIN"), any(), any()))
      .thenReturn(UNAUTHORIZED);

    when(databaseConnection.getDataSource()).thenReturn(dataSource);
    when(databaseConnection.getConnection()).thenReturn(connection);
    when(databaseConnection.hasConnection()).thenReturn(true);
    when(databaseConnection.getDatabaseAlias()).thenReturn("default");
  }

  private void givenTarget(String name, Boolean isPublic, Boolean serverOnly) throws AWException {
    Target target = new Target().setName(name).setIsPublic(isPublic).setServerOnly(serverOnly)
      .setQueryList(Collections.emptyList());
    when(aweElements.getMaintain(name)).thenReturn(target);
  }

  private ObjectNode params() {
    return JsonNodeFactory.instance.objectNode();
  }

  @Test
  @DisplayName("An authenticated client cannot launch a server-only target")
  void authenticatedClientIsRefused() throws Exception {
    when(aweSession.isAuthenticated()).thenReturn(true);
    givenTarget(SERVER_ONLY_TARGET, null, true);

    ObjectNode parameters = params();

    assertThatThrownBy(() -> maintainService.launchMaintain(SERVER_ONLY_TARGET, parameters, databaseConnection, false))
      .isInstanceOf(AWException.class)
      .satisfies(exc -> assertThat(((AWException) exc).getMessage()).isEqualTo(UNAUTHORIZED));
  }

  @Test
  @DisplayName("Every client launch variant refuses a server-only target")
  void everyClientVariantIsRefused() throws Exception {
    when(aweSession.isAuthenticated()).thenReturn(true);
    givenTarget(SERVER_ONLY_TARGET, null, true);

    ObjectNode parameters = params();

    assertThatThrownBy(() -> maintainService.launchMaintain(SERVER_ONLY_TARGET, parameters))
      .isInstanceOf(AWException.class);
    assertThatThrownBy(() -> maintainService.launchMaintain(SERVER_ONLY_TARGET, databaseConnection, false))
      .isInstanceOf(AWException.class);
  }

  @Test
  @DisplayName("A public and server-only target is refused: server-only wins")
  void serverOnlyWinsOverPublic() throws Exception {
    when(aweSession.isAuthenticated()).thenReturn(false);
    givenTarget("bothTarget", true, true);

    ObjectNode parameters = params();

    assertThatThrownBy(() -> maintainService.launchMaintain("bothTarget", parameters, databaseConnection, false))
      .isInstanceOf(AWException.class)
      .satisfies(exc -> assertThat(((AWException) exc).getMessage()).isEqualTo(UNAUTHORIZED));
  }

  @Test
  @DisplayName("The server can launch a server-only target")
  void serverLaunchIsAllowed() throws Exception {
    when(aweSession.isAuthenticated()).thenReturn(false);
    givenTarget(SERVER_ONLY_TARGET, null, true);

    try (var ignored = mockStatic(DataSourceUtils.class)) {
      ServiceData result = maintainService.launchPrivateMaintain(SERVER_ONLY_TARGET, params(), databaseConnection, false);
      assertThat(result).isNotNull();
    }
  }

  @Test
  @DisplayName("An authenticated client can still launch an ordinary target")
  void ordinaryTargetStillWorks() throws Exception {
    when(aweSession.isAuthenticated()).thenReturn(true);
    givenTarget("ordinaryTarget", null, null);

    try (var ignored = mockStatic(DataSourceUtils.class)) {
      ServiceData result = maintainService.launchMaintain("ordinaryTarget", params(), databaseConnection, false);
      assertThat(result).isNotNull();
    }
  }
}
