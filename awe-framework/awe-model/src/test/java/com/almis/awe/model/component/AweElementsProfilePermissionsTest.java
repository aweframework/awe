package com.almis.awe.model.component;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWERuntimeException;
import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.model.entities.access.Profile;
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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * The permissions and exclusions of the profile files: they are parsed, and checked against the permission catalog
 * when they are loaded. At startup an unknown permission fails the load, on a reload the previous files are kept
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AweElementsProfilePermissionsTest {

  private static final String CONTRIBUTOR = """
    <profile>
      <restriction option="users" />
      <permission id="*" />
      <exclude id="users.*" />
    </profile>
    """;

  @Mock
  private WebApplicationContext context;

  @Mock
  private Environment environment;

  private final XStreamSerializer serializer = XmlSerializerTestUtil.buildSerializer();
  private AweElementsDao elementsDao;
  private AweElements aweElements;

  @BeforeEach
  void setUp() {
    when(context.getEnvironment()).thenReturn(environment);
    BaseConfigProperties baseConfigProperties = new BaseConfigProperties();
    baseConfigProperties.setModuleList(new String[]{"module-one"});
    baseConfigProperties.setLanguageList(List.of("en-GB"));
    elementsDao = spy(new AweElementsDao(serializer, baseConfigProperties));
    aweElements = new AweElements(context, baseConfigProperties, elementsDao);
    catalog("users");
  }

  private void catalog(String resourceId) {
    Permissions file = new Permissions()
      .setVerbList(List.of(new PermissionVerb().setId("read").setLabel("READ")))
      .setResourceList(List.of(new PermissionResource().setId(resourceId).setLabel("LABEL").setVerbs("read"),
        new PermissionResource().setId("reports").setLabel("LABEL").setVerbs("read")));
    doReturn(CompletableFuture.completedFuture(List.of(file)))
      .when(elementsDao).readXmlFileListAsync(Permissions.class, "/global/Permissions.xml");
  }

  private void profiles(Map<String, String> files) {
    doAnswer(invocation -> {
      Map<String, Profile> storage = new ConcurrentHashMap<>();
      files.forEach((name, xml) -> storage.put(name, serializer.getObjectFromTemplate(Profile.class, xml)));
      return CompletableFuture.completedFuture(storage);
    }).when(elementsDao).readModuleFolderXmlFile(eq(Profile.class), anyString());
  }

  private static String profile(String permission) {
    return "<profile><permission id=\"" + permission + "\" /></profile>";
  }

  @Test
  void aProfileFileDeclaresItsPermissionsAndExclusions() throws Exception {
    profiles(Map.of("contributor", CONTRIBUTOR));

    aweElements.init();

    Profile contributor = aweElements.getProfile("contributor");
    assertThat(contributor.getRestrictions()).singleElement().extracting("option").isEqualTo("users");
    assertThat(contributor.getPermissionIds()).containsExactly("*");
    assertThat(contributor.getExclusionIds()).containsExactly("users.*");
    assertThat(contributor.copy().getExclusionIds()).containsExactly("users.*");
  }

  @Test
  void anUnknownPermissionOrExclusionFailsTheStartup() {
    profiles(Map.of("reader", profile("*.reed"),
      "contributor", "<profile><permission id=\"*\" /><exclude id=\"invoices.*\" /></profile>"));

    assertThatThrownBy(() -> aweElements.init())
      .isInstanceOf(AWERuntimeException.class)
      .hasMessageContaining("profile 'reader' (permission '*.reed'): unknown permission '*.reed'")
      .hasMessageContaining("profile 'contributor' (exclude 'invoices.*'): unknown permission 'invoices.*'");
  }

  @Test
  void reloadingProfilesWithAnUnknownPermissionKeepsThePreviousProfiles() throws Exception {
    profiles(Map.of("reader", profile("*.read")));
    aweElements.init();

    profiles(Map.of("reader", profile("users.reed"), "other", "<profile />"));
    aweElements.reloadProfiles();

    assertThat(aweElements.getProfile("reader").getPermissionIds()).containsExactly("*.read");
    assertThat(aweElements.getProfile("other")).isNull();
  }

  @Test
  void aFullReloadWithAnUnknownPermissionKeepsThePreviousProfiles() throws Exception {
    profiles(Map.of("reader", profile("*.read")));
    aweElements.init();

    profiles(Map.of("reader", profile("users.reed")));
    aweElements.reloadAll();

    assertThat(aweElements.getProfile("reader").getPermissionIds()).containsExactly("*.read");
  }

  @Test
  void reloadingTheCatalogWithoutAPermissionOfAProfileKeepsThePreviousCatalog() {
    profiles(Map.of("reader", profile("users.*")));
    aweElements.init();

    catalog("invoices");
    aweElements.reloadPermissions();

    assertThat(aweElements.getPermissionCatalog().getPermissionIds()).containsExactly("users.read", "reports.read");
  }
}
