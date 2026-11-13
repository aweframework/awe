package com.almis.awe.model.component;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWERuntimeException;
import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.model.entities.maintain.Maintain;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.permissions.PermissionResource;
import com.almis.awe.model.entities.permissions.PermissionVerb;
import com.almis.awe.model.entities.permissions.Permissions;
import com.almis.awe.model.entities.queries.Queries;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.action.ButtonAction;
import com.almis.awe.model.entities.screen.component.button.Button;
import com.almis.awe.model.entities.screen.component.grid.Column;
import com.almis.awe.model.entities.services.Service;
import com.almis.awe.model.entities.services.Services;
import com.almis.awe.model.util.XmlSerializerTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Validation of the {@code permission} attribute of the queries, the maintain targets, the services and the screen
 * elements when they are loaded: at startup a problem fails the load, on a reload the previous definitions are kept
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AweElementsPermissionAttributeTest {

  @Mock
  private WebApplicationContext context;

  @Mock
  private Environment environment;

  private AweElementsDao elementsDao;
  private AweElements aweElements;

  @BeforeEach
  void setUp() {
    when(context.getEnvironment()).thenReturn(environment);
    BaseConfigProperties baseConfigProperties = new BaseConfigProperties();
    baseConfigProperties.setModuleList(new String[]{"module-one"});
    baseConfigProperties.setLanguageList(List.of("en-GB"));
    elementsDao = spy(new AweElementsDao(XmlSerializerTestUtil.buildSerializer(), baseConfigProperties));
    aweElements = new AweElements(context, baseConfigProperties, elementsDao);
    catalog("users");
  }

  private void catalog(String resourceId) {
    Permissions file = new Permissions()
      .setVerbList(List.of(new PermissionVerb().setId("read").setLabel("READ"),
        new PermissionVerb().setId("delete").setLabel("DELETE")))
      .setResourceList(List.of(new PermissionResource().setId(resourceId).setLabel("LABEL").setVerbs("read,delete")));
    doReturn(CompletableFuture.completedFuture(List.of(file)))
      .when(elementsDao).readXmlFileListAsync(Permissions.class, "/global/Permissions.xml");
  }

  private void queries(Query... queries) {
    doAnswer(invocation -> {
      Map<String, Query> storage = invocation.getArgument(1);
      for (Query query : queries) {
        storage.put(query.getId(), query);
      }
      return CompletableFuture.completedFuture("OK");
    }).when(elementsDao).readXmlFilesAsync(eq(Queries.class), anyMap(), anyString());
  }

  private void maintains(Target... targets) {
    doAnswer(invocation -> {
      Map<String, Target> storage = invocation.getArgument(1);
      for (Target target : targets) {
        storage.put(target.getName(), target);
      }
      return CompletableFuture.completedFuture("OK");
    }).when(elementsDao).readXmlFilesAsync(eq(Maintain.class), anyMap(), anyString());
  }

  private void services(Service... services) {
    doAnswer(invocation -> {
      Map<String, Service> storage = invocation.getArgument(1);
      for (Service service : services) {
        storage.put(service.getId(), service);
      }
      return CompletableFuture.completedFuture("OK");
    }).when(elementsDao).readXmlFilesAsync(eq(Services.class), anyMap(), anyString());
  }

  private void screens(Screen... screens) {
    doAnswer(invocation -> {
      Map<String, Screen> storage = new ConcurrentHashMap<>();
      for (Screen screen : screens) {
        storage.put(screen.getId(), screen);
      }
      return CompletableFuture.completedFuture(storage);
    }).when(elementsDao).readModuleFolderXmlFile(eq(Screen.class), anyString());
  }

  private static Screen screen(String id, String buttonPermission, String columnPermission, String editPermission) {
    Button button = new Button();
    button.setPermission(buttonPermission);
    button.setId("ButDel");
    Column column = new Column();
    column.setPermission(columnPermission);
    column.setName("DelBtn");
    column.addElement(ButtonAction.builder().type("dialog").target("PnlUsr").editPermission(editPermission).build());
    return (Screen) new Screen().setId(id).addElement(button).addElement(column);
  }

  private static Query query(String id, String permission) {
    return new Query().setId(id).setPermission(permission);
  }

  private static Target target(String name, String permission) {
    return new Target().setName(name).setPermission(permission);
  }

  private static Service service(String id, String permission) {
    return Service.builder().id(id).permission(permission).build();
  }

  @Test
  void validPermissionsAreLoaded() throws Exception {
    queries(query("UsrLst", "users.read"), query("Free", null));
    maintains(target("UsrDel", "users.read users.delete"));

    aweElements.init();

    assertThat(aweElements.getQuery("UsrLst").getPermission()).isEqualTo("users.read");
    assertThat(aweElements.getQuery("Free").getPermission()).isNull();
    assertThat(aweElements.getMaintain("UsrDel").getPermission()).isEqualTo("users.read users.delete");
  }

  @Test
  void anUnknownPermissionOnAQueryFailsTheStartup() {
    queries(query("UsrLst", "users.reed"));

    assertThatThrownBy(() -> aweElements.init())
      .isInstanceOf(AWERuntimeException.class)
      .hasMessageContaining("query 'UsrLst'")
      .hasMessageContaining("unknown permission 'users.reed'");
  }

  @Test
  void anUnknownPermissionOnATargetFailsTheStartup() {
    maintains(target("UsrDel", "users.read users.remove"));

    assertThatThrownBy(() -> aweElements.init())
      .isInstanceOf(AWERuntimeException.class)
      .hasMessageContaining("maintain 'UsrDel'")
      .hasMessageContaining("unknown permission 'users.remove'");
  }

  @Test
  void aPermissionOnAPublicTargetFailsTheStartup() {
    queries(query("PubLst", "users.read").setIsPublic(true));

    assertThatThrownBy(() -> aweElements.init())
      .isInstanceOf(AWERuntimeException.class)
      .hasMessageContaining("query 'PubLst'")
      .hasMessageContaining("a public target cannot declare a permission");
  }

  @Test
  void reloadingQueriesWithAnUnknownPermissionKeepsThePreviousQueries() throws Exception {
    queries(query("UsrLst", "users.read"));
    aweElements.init();

    queries(query("UsrLst", "users.reed"), query("Other", null));
    aweElements.reloadQueries();

    assertThat(aweElements.getQuery("UsrLst").getPermission()).isEqualTo("users.read");
    assertThat(aweElements.getQuery("Other")).isNull();
  }

  @Test
  void reloadingMaintainsWithAnUnknownPermissionKeepsThePreviousTargets() throws Exception {
    maintains(target("UsrDel", "users.delete"));
    aweElements.init();

    maintains(target("UsrDel", "users.remove"), target("Other", null));
    aweElements.reloadMaintains();

    assertThat(aweElements.getMaintain("UsrDel").getPermission()).isEqualTo("users.delete");
    assertThat(aweElements.getMaintain("Other")).isNull();
  }

  @Test
  void reloadingTheCatalogWithoutAPermissionInUseKeepsThePreviousCatalog() {
    queries(query("UsrLst", "users.read"));
    aweElements.init();

    catalog("invoices");
    aweElements.reloadPermissions();

    assertThat(aweElements.getPermissionCatalog().getPermissionIds()).containsExactly("users.read", "users.delete");
  }

  @Test
  void aFullReloadWithAnUnknownPermissionKeepsThePreviousDefinitions() throws Exception {
    queries(query("UsrLst", "users.read"));
    aweElements.init();

    queries(query("UsrLst", "users.reed"));
    aweElements.reloadAll();

    assertThat(aweElements.getQuery("UsrLst").getPermission()).isEqualTo("users.read");
  }

  @Test
  void aFullReloadThatKeepsThePreviousCatalogKeepsTheTargetsThatNeedTheNewOne() throws Exception {
    queries(query("UsrLst", "users.read"));
    maintains(target("UsrDel", "users.delete"));
    aweElements.init();

    catalog("invoices");
    queries(query("UsrLst", "users.reed"));
    maintains(target("InvDel", "invoices.delete"));
    aweElements.reloadAll();

    assertThat(aweElements.getPermissionCatalog().getPermissionIds()).containsExactly("users.read", "users.delete");
    assertThat(aweElements.getQuery("UsrLst").getPermission()).isEqualTo("users.read");
    assertThat(aweElements.getMaintain("UsrDel").getPermission()).isEqualTo("users.delete");
    assertThat(aweElements.getMaintain("InvDel")).isNull();
  }

  @Test
  void validServicePermissionsAreLoaded() throws Exception {
    services(service("UsrExp", "users.read users.delete"), service("Free", null),
      Service.builder().id("launchQuery").authorization("target").build());

    aweElements.init();

    assertThat(aweElements.getService("UsrExp").getPermission()).isEqualTo("users.read users.delete");
    assertThat(aweElements.getService("Free").getPermission()).isNull();
    assertThat(aweElements.getService("launchQuery").isAuthorizedByTarget()).isTrue();
  }

  @Test
  void anUnknownPermissionOnAServiceFailsTheStartup() {
    services(service("UsrExp", "users.export"));

    assertThatThrownBy(() -> aweElements.init())
      .isInstanceOf(AWERuntimeException.class)
      .hasMessageContaining("service 'UsrExp'")
      .hasMessageContaining("unknown permission 'users.export'");
  }

  @Test
  void aPermissionOnAServiceAuthorizedByItsTargetFailsTheStartup() {
    services(Service.builder().id("launchQuery").authorization("target").permission("users.read").build());

    assertThatThrownBy(() -> aweElements.init())
      .isInstanceOf(AWERuntimeException.class)
      .hasMessageContaining("service 'launchQuery'")
      .hasMessageContaining("a service authorized by its target cannot declare a permission");
  }

  @Test
  void reloadingServicesWithAnUnknownPermissionKeepsThePreviousServices() throws Exception {
    services(service("UsrExp", "users.read"));
    aweElements.init();

    services(service("UsrExp", "users.reed"), service("Other", null));
    aweElements.reloadServices();

    assertThat(aweElements.getService("UsrExp").getPermission()).isEqualTo("users.read");
    assertThat(aweElements.getService("Other")).isNull();
  }

  @Test
  void reloadingTheCatalogWithoutAPermissionOfAServiceKeepsThePreviousCatalog() {
    services(service("UsrExp", "users.read"));
    aweElements.init();

    catalog("invoices");
    aweElements.reloadPermissions();

    assertThat(aweElements.getPermissionCatalog().getPermissionIds()).containsExactly("users.read", "users.delete");
  }

  @Test
  void aFullReloadWithAnUnknownPermissionOnAServiceKeepsThePreviousServices() throws Exception {
    services(service("UsrExp", "users.read"));
    aweElements.init();

    services(service("UsrExp", "users.reed"));
    aweElements.reloadAll();

    assertThat(aweElements.getService("UsrExp").getPermission()).isEqualTo("users.read");
  }

  @Test
  void aFullReloadThatKeepsThePreviousCatalogKeepsTheServicesThatNeedTheNewOne() throws Exception {
    queries(query("UsrLst", "users.read"));
    services(service("UsrExp", "users.read"));
    aweElements.init();

    catalog("invoices");
    queries(query("UsrLst", "users.reed"));
    services(service("InvExp", "invoices.read"));
    aweElements.reloadAll();

    assertThat(aweElements.getPermissionCatalog().getPermissionIds()).containsExactly("users.read", "users.delete");
    assertThat(aweElements.getService("UsrExp").getPermission()).isEqualTo("users.read");
    assertThat(aweElements.getService("InvExp")).isNull();
  }

  @Test
  void validScreenElementPermissionsAreLoaded() throws Exception {
    screens(screen("UsrScr", "users.delete", "users.read users.delete", "users.read"));

    aweElements.init();

    assertThat(aweElements.getScreen("UsrScr").getElementsByType(Button.class).getFirst().getPermission())
      .isEqualTo("users.delete");
  }

  @Test
  void anUnknownPermissionOnAScreenElementFailsTheStartup() {
    screens(screen("UsrScr", "users.remove", null, null));

    assertThatThrownBy(() -> aweElements.init())
      .isInstanceOf(AWERuntimeException.class)
      .hasMessageContaining("screen 'UsrScr'")
      .hasMessageContaining("unknown permission 'users.remove'");
  }

  @Test
  void anUnknownPermissionOnAColumnOrADialogActionFailsTheStartup() {
    screens(screen("UsrScr", null, "users.erase", "users.edit"));

    assertThatThrownBy(() -> aweElements.init())
      .isInstanceOf(AWERuntimeException.class)
      .hasMessageContaining("unknown permission 'users.erase'")
      .hasMessageContaining("unknown permission 'users.edit'");
  }

  @Test
  void reloadingScreensWithAnUnknownPermissionKeepsThePreviousScreens() throws Exception {
    screens(screen("UsrScr", "users.delete", null, null));
    aweElements.init();

    screens(screen("UsrScr", "users.remove", null, null), screen("Other", null, null, null));
    aweElements.reloadScreens();

    assertThat(aweElements.getScreen("UsrScr").getElementsByType(Button.class).getFirst().getPermission())
      .isEqualTo("users.delete");
  }

  @Test
  void reloadingTheCatalogWithoutAPermissionOfAScreenKeepsThePreviousCatalog() {
    screens(screen("UsrScr", null, null, "users.read"));
    aweElements.init();

    catalog("invoices");
    aweElements.reloadPermissions();

    assertThat(aweElements.getPermissionCatalog().getPermissionIds()).containsExactly("users.read", "users.delete");
  }

  @Test
  void aFullReloadWithAnUnknownPermissionOnAScreenKeepsThePreviousScreens() throws Exception {
    screens(screen("UsrScr", "users.delete", null, null));
    aweElements.init();

    screens(screen("UsrScr", "users.remove", null, null));
    aweElements.reloadAll();

    assertThat(aweElements.getScreen("UsrScr").getElementsByType(Button.class).getFirst().getPermission())
      .isEqualTo("users.delete");
  }
}
