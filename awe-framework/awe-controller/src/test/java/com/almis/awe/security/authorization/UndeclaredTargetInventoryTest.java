package com.almis.awe.security.authorization;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.config.AuthorizationMode;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.actions.Action;
import com.almis.awe.model.entities.actions.Call;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.action.ButtonAction;
import com.almis.awe.model.entities.services.Service;
import org.junit.jupiter.api.AfterEach;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

/**
 * Tests of the inventory of the targets used by private screens that declare no permission
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UndeclaredTargetInventoryTest {

  @Mock private ApplicationContext applicationContext;
  @Mock private AweElements aweElements;

  private final Logger logger = (Logger) LoggerFactory.getLogger(UndeclaredTargetInventory.class);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private final Map<String, Query> queries = new HashMap<>();
  private final Map<String, Target> maintains = new HashMap<>();
  private final SecurityConfigProperties securityConfigProperties = new SecurityConfigProperties();
  private UndeclaredTargetInventory inventory;
  private Menu privateMenu;
  private Menu publicMenu;

  @BeforeEach
  void setUp() throws Exception {
    BaseConfigProperties baseConfigProperties = new BaseConfigProperties();
    ScreenTargetIndex index = new ScreenTargetIndex(baseConfigProperties);
    index.setApplicationContext(applicationContext);
    inventory = new UndeclaredTargetInventory(securityConfigProperties, baseConfigProperties, index);
    inventory.setApplicationContext(applicationContext);
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getQueryMap()).thenReturn(queries);
    when(aweElements.getMaintainMap()).thenReturn(maintains);

    privateMenu = new Menu();
    publicMenu = new Menu();
    when(aweElements.getMenu("private")).thenReturn(privateMenu);
    when(aweElements.getMenu("public")).thenReturn(publicMenu);
    securityConfigProperties.getAuthorization().setMode(AuthorizationMode.WARN);

    appender.start();
    logger.addAppender(appender);
    logger.setLevel(Level.INFO);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(appender);
    logger.setLevel(null);
  }

  private void screen(String option, Menu menu, Element... elements) throws AWException {
    Screen screen = new Screen();
    screen.setId(option);
    for (Element element : elements) {
      screen.addElement(element);
    }
    when(aweElements.getScreen(option)).thenReturn(screen);
    Option menuOption = new Option();
    menuOption.setName(option);
    menuOption.setScreen(option);
    menu.addElement(menuOption);
  }

  private ButtonAction target(String targetAction) {
    ButtonAction button = new ButtonAction();
    button.setTargetAction(targetAction);
    return button;
  }

  private ButtonAction serverAction(String actionId, Service service) throws AWException {
    ButtonAction button = new ButtonAction();
    button.setServerAction(actionId);
    when(aweElements.getAction(actionId)).thenReturn(Action.builder().id(actionId)
      .call(Call.builder().service(service.getId()).build()).build());
    when(aweElements.getService(service.getId())).thenReturn(service);
    return button;
  }

  private void query(String id, String permission) {
    queries.put(id, Query.builder().id(id).permission(permission).build());
  }

  private void maintain(String id, String permission) {
    maintains.put(id, Target.builder().name(id).permission(permission).build());
  }

  /**
   * A private screen that uses a target of each kind with and without permission
   */
  private void privateScreenOfEveryKind() throws AWException {
    query("UsrLst", null);
    query("PrfLst", "access.read");
    query("AnyLst", "  ");
    maintain("UsrUpd", null);
    maintain("PrfUpd", "access.update");
    screen("users", privateMenu, target("UsrLst"), target("PrfLst"), target("AnyLst"), target("UsrUpd"),
      target("PrfUpd"), target("users"),
      serverAction("export", Service.builder().id("exportUsers").build()),
      serverAction("purge", Service.builder().id("purgeUsers").permission("access.update").build()));
  }

  @Test
  @DisplayName("Lists the queries, maintains and services of private screens that declare no permission")
  void listsUndeclaredTargetsByKind() throws Exception {
    privateScreenOfEveryKind();

    assertThat(inventory.getUndeclaredTargets()).containsExactly(
      Map.entry(TargetKind.QUERY, Set.of("AnyLst", "UsrLst")),
      Map.entry(TargetKind.MAINTAIN, Set.of("UsrUpd")),
      Map.entry(TargetKind.SERVICE, Set.of("exportUsers")));
  }

  @Test
  @DisplayName("Public, server-only, public screen and dispatcher targets are not listed")
  void excludesTargetsThatTakeNoPermission() throws Exception {
    queries.put("PubLst", Query.builder().id("PubLst").isPublic(true).build());
    queries.put("SrvLst", Query.builder().id("SrvLst").serverOnly(true).build());
    maintains.put("PubUpd", Target.builder().name("PubUpd").isPublic(true).build());
    query("LoginLst", null);
    maintain("Shared", null);
    screen("users", privateMenu, target("PubLst"), target("SrvLst"), target("PubUpd"), target("Shared"),
      serverAction("data", Service.builder().id("launchQuery").authorization("target").build()));
    screen("login", publicMenu, target("LoginLst"), target("Shared"),
      serverAction("signin", Service.builder().id("signinService").build()));

    assertThat(inventory.getUndeclaredTargets()).isEmpty();
  }

  @Test
  @DisplayName("Warn mode logs one warning with the count and one line per kind, sorted")
  void warnModeLogsTheInventory() throws Exception {
    privateScreenOfEveryKind();

    inventory.logUndeclaredTargets();

    assertThat(appender.list).singleElement().satisfies(entry -> {
      assertThat(entry.getLevel()).isEqualTo(Level.WARN);
      assertThat(entry.getFormattedMessage()).isEqualTo("""
        4 targets used by private screens declare no permission (authorization mode is warn):
          query: AnyLst, UsrLst
          maintain: UsrUpd
          service: exportUsers""");
    });
  }

  @Test
  @DisplayName("With require-undeclared the inventory is still logged, naming the default permissions they need")
  void requireUndeclaredLogsTheInventoryWithTheDefaultPermissions() throws Exception {
    privateScreenOfEveryKind();
    securityConfigProperties.getPermissions().setRequireUndeclared(true);

    inventory.logUndeclaredTargets();

    assertThat(appender.list).singleElement().satisfies(entry -> {
      assertThat(entry.getLevel()).isEqualTo(Level.WARN);
      assertThat(entry.getFormattedMessage()).isEqualTo("""
        4 targets used by private screens declare no permission (authorization mode is warn, they require \
        default.read or default.write):
          query: AnyLst, UsrLst
          maintain: UsrUpd
          service: exportUsers""");
    });
  }

  @Test
  @DisplayName("An empty inventory logs nothing")
  void emptyInventoryLogsNothing() throws Exception {
    query("PrfLst", "access.read");
    screen("profiles", privateMenu, target("PrfLst"));

    inventory.logUndeclaredTargets();

    assertThat(appender.list).isEmpty();
  }

  @Test
  @DisplayName("Enforce and off modes log nothing and read no definition")
  void otherModesLogNothing() throws Exception {
    privateScreenOfEveryKind();

    for (AuthorizationMode mode : new AuthorizationMode[]{AuthorizationMode.ENFORCE, AuthorizationMode.OFF}) {
      securityConfigProperties.getAuthorization().setMode(mode);
      inventory.logUndeclaredTargets();
    }

    assertThat(appender.list).isEmpty();
  }

  @Test
  @DisplayName("A failure computing the inventory is logged and never stops the startup")
  void failureIsLoggedAndSwallowed() {
    when(aweElements.getQueryMap()).thenThrow(new IllegalStateException("queries not loaded"));

    assertThatCode(inventory::logUndeclaredTargets).doesNotThrowAnyException();

    assertThat(appender.list).singleElement().satisfies(entry -> {
      assertThat(entry.getLevel()).isEqualTo(Level.WARN);
      assertThat(entry.getFormattedMessage()).isEqualTo(
        "The targets that declare no permission could not be listed: queries not loaded");
    });
  }
}
