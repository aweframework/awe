package com.almis.awe.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.exception.AWEUnauthorizedException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.queries.DatabaseConnection;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.security.authorization.TargetKind;
import com.almis.awe.security.authorization.TargetLaunchAuthorizer;
import com.almis.awe.service.data.connector.maintain.MaintainLauncher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
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
  private static final String ACCESS_DENIED = "access denied";
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
  @Mock private TargetLaunchAuthorizer authorizer;

  private MaintainService maintainService;

  @BeforeEach
  void setUp() {
    maintainService = new MaintainService(maintainLauncher, queryUtil, databaseConfigProperties, authorizer);
    maintainService.setApplicationContext(applicationContext);

    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(aweElements.getLocaleWithLanguage(anyString(), any())).thenReturn("msg");
    when(aweElements.getLocaleWithLanguage(anyString(), any(), any())).thenReturn("msg");
    when(aweElements.getLocaleWithLanguage(eq("ERROR_MESSAGE_ACTION_NOT_PERMITTED"), any())).thenReturn(UNAUTHORIZED);
    when(aweElements.getLocaleWithLanguage(eq("ERROR_TITLE_ACCESS_DENIED"), any())).thenReturn(ACCESS_DENIED);
    when(aweSession.isAuthenticated()).thenReturn(true);
    when(authorizer.authorize(TARGET, TargetKind.MAINTAIN)).thenReturn(true);

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
    when(authorizer.authorize(TARGET, TargetKind.MAINTAIN)).thenReturn(false);

    ObjectNode parameters = params();

    assertThatThrownBy(() -> maintainService.launchMaintain(TARGET, parameters, databaseConnection, false))
      .isInstanceOf(AWEUnauthorizedException.class)
      .hasMessage(UNAUTHORIZED)
      .satisfies(exc -> assertThat(((AWException) exc).getTitle()).isEqualTo(ACCESS_DENIED));
    verifyNoInteractions(maintainLauncher);
    verify(aweElements, never()).getLocaleWithLanguage(anyString(), any(), eq(TARGET));
  }

  @Test
  @DisplayName("Every client launch variant is checked")
  void everyClientVariantIsChecked() throws Exception {
    givenTarget(null);
    when(authorizer.authorize(TARGET, TargetKind.MAINTAIN)).thenReturn(false);

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
    verify(authorizer).authorize(TARGET, TargetKind.MAINTAIN);
  }

  @Test
  @DisplayName("A server launch is not checked")
  void serverLaunchIsNotChecked() throws Exception {
    givenTarget(null);
    when(authorizer.authorize(TARGET, TargetKind.MAINTAIN)).thenReturn(false);

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
    when(authorizer.authorize(TARGET, TargetKind.MAINTAIN)).thenReturn(false);

    try (var ignored = mockStatic(DataSourceUtils.class)) {
      ServiceData result = maintainService.launchMaintain(TARGET, params(), databaseConnection, false);
      assertThat(result).isNotNull();
    }
    verify(authorizer, never()).authorize(any(), any());
  }

  @Test
  @DisplayName("An unauthenticated client is refused before the screens are checked, and the log names the target")
  void unauthenticatedIsRefusedFirst() throws Exception {
    givenTarget(null);
    when(aweSession.isAuthenticated()).thenReturn(false);
    Logger logger = (Logger) LoggerFactory.getLogger(MaintainService.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    ObjectNode parameters = params();

    try {
      assertThatThrownBy(() -> maintainService.launchMaintain(TARGET, parameters, databaseConnection, false))
        .isInstanceOf(AWEUnauthorizedException.class)
        .hasMessage(UNAUTHORIZED)
        .satisfies(exc -> assertThat(((AWException) exc).getTitle()).isEqualTo(ACCESS_DENIED));
    } finally {
      logger.detachAppender(appender);
    }
    verify(authorizer, never()).authorize(any(), any());
    assertThat(appender.list)
      .anySatisfy(event -> {
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("[" + TARGET + "]");
      });
  }

  @Test
  @DisplayName("The maintain service cannot be built without an authorizer")
  void authorizerIsRequired() {
    assertThatThrownBy(() -> new MaintainService(maintainLauncher, queryUtil, databaseConfigProperties, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("targetLaunchAuthorizer is required: pass the TargetLaunchAuthorizerChain bean");
  }
}
