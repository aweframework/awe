package com.almis.awe.model.component;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWERuntimeException;
import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.model.entities.permissions.PermissionResource;
import com.almis.awe.model.entities.permissions.PermissionVerb;
import com.almis.awe.model.entities.permissions.Permissions;
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
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Load of the permission catalog from the Permissions.xml file of each module
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AweElementsPermissionsTest {

  private static final String PERMISSIONS_FILE = "/global/Permissions.xml";

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
    baseConfigProperties.setModuleList(new String[]{"module-one", "module-two"});
    baseConfigProperties.setLanguageList(List.of("en-GB"));
    elementsDao = spy(new AweElementsDao(XmlSerializerTestUtil.buildSerializer(), baseConfigProperties));
    aweElements = new AweElements(context, baseConfigProperties, elementsDao);
  }

  private static Permissions file(String resourceId, String verbs) {
    return new Permissions()
      .setVerbList(List.of(new PermissionVerb().setId("read").setLabel("PERMISSION_VERB_READ")))
      .setResourceList(List.of(new PermissionResource().setId(resourceId).setLabel("LABEL").setVerbs(verbs)));
  }

  private void moduleFiles(Permissions... files) {
    doReturn(CompletableFuture.completedFuture(List.of(files)))
      .when(elementsDao).readXmlFileListAsync(Permissions.class, PERMISSIONS_FILE);
  }

  /**
   * The catalog combines the Permissions.xml of every module at startup
   */
  @Test
  void initLoadsTheCatalogOfEveryModule() {
    moduleFiles(file("users", "read"), file("invoices", "read"));

    aweElements.init();

    assertThat(aweElements.getPermissionCatalog().getPermissionIds()).containsExactly("users.read", "invoices.read");
  }

  /**
   * A module without Permissions.xml is fine: the catalog is empty
   */
  @Test
  void modulesWithoutPermissionsFileLoadAnEmptyCatalog() {
    aweElements.init();

    assertThat(aweElements.getPermissionCatalog().isEmpty()).isTrue();
  }

  /**
   * An invalid catalog fails the startup
   */
  @Test
  void invalidCatalogFailsTheStartup() {
    moduleFiles(file("users", "read,write"));

    assertThatThrownBy(() -> aweElements.init())
      .isInstanceOf(AWERuntimeException.class)
      .hasMessageContaining("the 'write' verb exists only for the 'default' resource");
  }

  /**
   * Reloading the permissions rebuilds the catalog; an invalid catalog keeps the previous one
   */
  @Test
  void reloadRebuildsTheCatalogAndKeepsThePreviousOneWhenInvalid() {
    moduleFiles(file("users", "read"));
    aweElements.init();

    moduleFiles(file("invoices", "read"));
    aweElements.reloadPermissions();
    assertThat(aweElements.getPermissionCatalog().getPermissionIds()).containsExactly("invoices.read");

    moduleFiles(file("invoices", "read,write"));
    aweElements.reloadPermissions();
    assertThat(aweElements.getPermissionCatalog().getPermissionIds()).containsExactly("invoices.read");
  }

  /**
   * A full reload rebuilds the catalog too
   */
  @Test
  void reloadAllRebuildsTheCatalog() {
    moduleFiles(file("users", "read"));
    aweElements.init();

    moduleFiles(file("invoices", "read"));
    aweElements.reloadAll();

    assertThat(aweElements.getPermissionCatalog().getPermissionIds()).containsExactly("invoices.read");
  }
}
