package com.almis.awe.service.hotreload;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.service.BroadcastService;
import com.almis.awe.service.hotreload.XmlSchemaValidator.ValidationError;
import com.almis.awe.service.hotreload.XmlSchemaValidator.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.almis.awe.service.hotreload.XmlHotReloadService.XmlArtifactType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * XmlHotReloadService tests
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class XmlHotReloadServiceTest {

  @Mock
  private AweElements aweElements;

  @Mock
  private CacheManager cacheManager;

  @Mock
  private Cache queryDataCache;

  @Mock
  private XmlSchemaValidator schemaValidator;

  @Mock
  private BroadcastService broadcastService;

  private BaseConfigProperties baseConfigProperties;
  private XmlHotReloadService hotReloadService;

  @BeforeEach
  void setUp() {
    baseConfigProperties = new BaseConfigProperties();
    baseConfigProperties.setModuleList(new String[]{"module-one"});
    when(cacheManager.getCache("queryData")).thenReturn(queryDataCache);
    // Default: validation cannot run (files under fake paths do not exist), so it fails open
    when(schemaValidator.validate(any())).thenReturn(ValidationResult.notValidated());
    hotReloadService = new XmlHotReloadService(aweElements, baseConfigProperties, cacheManager, schemaValidator, broadcastService);
  }

  /**
   * Test the changed file classification
   */
  @Test
  void classifiesChangedFiles() {
    assertEquals(SCREEN, hotReloadService.classify("/app/target/classes/application/module-one/screen/home.xml"));
    assertEquals(SCREEN, hotReloadService.classify("/app/target/classes/application/module-one/screen/sub/folder/home.xml"));
    assertEquals(MENU, hotReloadService.classify("/app/target/classes/application/module-one/menu/public.xml"));
    assertEquals(LOCALE, hotReloadService.classify("/app/target/classes/application/module-one/locale/Locale-en-GB.xml"));
    assertEquals(PROFILE, hotReloadService.classify("/app/target/classes/application/module-one/profile/general.xml"));
    assertEquals(QUERIES, hotReloadService.classify("/app/target/classes/application/module-one/global/Queries.xml"));
    assertEquals(MAINTAIN, hotReloadService.classify("/app/target/classes/application/module-one/global/Maintain.xml"));
    assertEquals(SERVICES, hotReloadService.classify("/app/target/classes/application/module-one/global/Services.xml"));
    assertEquals(ENUMERATED, hotReloadService.classify("/app/target/classes/application/module-one/global/Enumerated.xml"));
    assertEquals(QUEUES, hotReloadService.classify("/app/target/classes/application/module-one/global/Queues.xml"));
    assertEquals(EMAIL, hotReloadService.classify("/app/target/classes/application/module-one/global/Email.xml"));
    assertEquals(ACTIONS, hotReloadService.classify("/app/target/classes/application/module-one/global/Actions.xml"));
    // Windows-like separators are normalized
    assertEquals(SCREEN, hotReloadService.classify("C:\\app\\target\\classes\\application\\module-one\\screen\\home.xml"));
    // Unknown global file names are ignored
    assertEquals(NONE, hotReloadService.classify("/app/target/classes/application/module-one/global/Whatever.xml"));
    // Files outside the known folders are ignored
    assertEquals(NONE, hotReloadService.classify("/app/target/classes/images/logo.xml"));
    // Non-XML files are ignored
    assertEquals(NONE, hotReloadService.classify("/app/target/classes/application/module-one/screen/home.txt"));
  }

  /**
   * Test that the classification is decided by the first folder segment under the module
   * root, so module names matching category folders do not misclassify files
   */
  @Test
  void classifiesByFirstSegmentUnderModuleRoot() {
    // Module literally named 'screen'
    Path moduleRoot = Paths.get("/app/target/classes/application/screen");

    assertEquals(MENU, hotReloadService.classify(moduleRoot, moduleRoot.resolve("menu/public.xml")));
    assertEquals(SCREEN, hotReloadService.classify(moduleRoot, moduleRoot.resolve("screen/home.xml")));
    assertEquals(LOCALE, hotReloadService.classify(moduleRoot, moduleRoot.resolve("locale/Locale-en-GB.xml")));
    assertEquals(PROFILE, hotReloadService.classify(moduleRoot, moduleRoot.resolve("profile/general.xml")));
    assertEquals(QUERIES, hotReloadService.classify(moduleRoot, moduleRoot.resolve("global/Queries.xml")));
    // Subfolders under a known first segment do not change the classification
    assertEquals(SCREEN, hotReloadService.classify(moduleRoot, moduleRoot.resolve("screen/menu/nested.xml")));
    // Unknown first segments are ignored
    assertEquals(NONE, hotReloadService.classify(moduleRoot, moduleRoot.resolve("other/Queries.xml")));
    // Non-XML files are ignored
    assertEquals(NONE, hotReloadService.classify(moduleRoot, moduleRoot.resolve("screen/home.txt")));
    // Without a module root the path-based classification is used
    assertEquals(MENU, hotReloadService.classify(null, Paths.get("/app/target/classes/application/module-one/menu/public.xml")));
  }

  /**
   * Test that a full reload rebuilds every element map and evicts the query cache
   */
  @Test
  void reloadAllReloadsEverythingAndEvictsCache() {
    hotReloadService.reloadAll();

    verify(aweElements).reloadAll();
    verify(queryDataCache).clear();
    verifyNoMoreInteractions(aweElements);
  }

  /**
   * Test that a screen change triggers a screens reload
   */
  @Test
  void screenChangeReloadsScreens() {
    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/screen/sub/home.xml"));

    verify(aweElements).reloadScreens();
    verifyNoMoreInteractions(aweElements);
    verifyNoInteractions(queryDataCache);
  }

  /**
   * Test that a menu change triggers a menus reload
   */
  @Test
  void menuChangeReloadsMenus() {
    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/menu/private.xml"));

    verify(aweElements).reloadMenus();
    verifyNoMoreInteractions(aweElements);
  }

  /**
   * Test that a locale change triggers a locales reload
   */
  @Test
  void localeChangeReloadsLocales() {
    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/locale/Locale-en-GB.xml"));

    verify(aweElements).reloadLocales();
    verifyNoMoreInteractions(aweElements);
  }

  /**
   * Test that a profile change triggers a profiles reload
   */
  @Test
  void profileChangeReloadsProfiles() {
    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/profile/general.xml"));

    verify(aweElements).reloadProfiles();
    verifyNoMoreInteractions(aweElements);
  }

  /**
   * Test that a queries change triggers a queries reload and evicts the query cache
   */
  @Test
  void queriesChangeReloadsQueriesAndEvictsCache() {
    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/global/Queries.xml"));

    verify(aweElements).reloadQueries();
    verify(queryDataCache).clear();
    verifyNoMoreInteractions(aweElements);
  }

  /**
   * Test that a maintain change triggers a maintains reload and evicts the query cache
   */
  @Test
  void maintainChangeReloadsMaintainsAndEvictsCache() {
    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/global/Maintain.xml"));

    verify(aweElements).reloadMaintains();
    verify(queryDataCache).clear();
    verifyNoMoreInteractions(aweElements);
  }

  /**
   * Test the remaining global file changes trigger their reload without cache eviction
   */
  @Test
  void globalChangesReloadTheirTypeWithoutCacheEviction() {
    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/global/Services.xml"));
    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/global/Enumerated.xml"));
    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/global/Queues.xml"));
    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/global/Email.xml"));
    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/global/Actions.xml"));

    verify(aweElements).reloadServices();
    verify(aweElements).reloadEnumerated();
    verify(aweElements).reloadQueues();
    verify(aweElements).reloadEmails();
    verify(aweElements).reloadActions();
    verifyNoMoreInteractions(aweElements);
    verifyNoInteractions(queryDataCache);
  }

  /**
   * Test that unknown files are ignored
   */
  @Test
  void unknownFilesAreIgnored() {
    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/screen/home.txt"));
    hotReloadService.reloadFor(Paths.get("/app/target/classes/other/file.xml"));

    verifyNoInteractions(aweElements);
    verifyNoInteractions(cacheManager);
  }

  /**
   * Test that a missing cache manager does not break a queries reload
   */
  @Test
  void missingCacheManagerIsTolerated() {
    XmlHotReloadService serviceWithoutCache = new XmlHotReloadService(aweElements, baseConfigProperties, null, schemaValidator, broadcastService);

    serviceWithoutCache.reloadFor(Paths.get("/app/target/classes/application/module-one/global/Queries.xml"));

    verify(aweElements).reloadQueries();
  }

  /**
   * Test that a successful reload broadcasts a reload-page action so connected clients refresh
   */
  @Test
  void successfulReloadBroadcastsReloadPage() {
    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/screen/home.xml"));

    ArgumentCaptor<ClientAction> captor = ArgumentCaptor.forClass(ClientAction.class);
    verify(broadcastService).broadcastMessage(captor.capture());
    assertEquals("reload-page", captor.getValue().getType());
  }

  /**
   * Test that a full reload also broadcasts the reload-page action
   */
  @Test
  void reloadAllBroadcastsReloadPage() {
    hotReloadService.reloadAll();

    ArgumentCaptor<ClientAction> captor = ArgumentCaptor.forClass(ClientAction.class);
    verify(broadcastService).broadcastMessage(captor.capture());
    assertEquals("reload-page", captor.getValue().getType());
  }

  /**
   * Test that a reload skipped because the definition is invalid does not broadcast anything
   */
  @Test
  void skippedReloadDoesNotBroadcast() {
    when(schemaValidator.validate(any()))
      .thenReturn(ValidationResult.validated(List.of(new ValidationError(4, 12, "boom"))));

    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/screen/home.xml"));

    verifyNoInteractions(broadcastService);
  }

  /**
   * Test that an ignored (unknown) file does not broadcast anything
   */
  @Test
  void ignoredFileDoesNotBroadcast() {
    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/global/Whatever.xml"));

    verifyNoInteractions(broadcastService);
  }

  /**
   * Test that refreshing clients broadcasts a reload-page action without reloading any
   * server-side definition (used for frontend JS/CSS rebuilds)
   */
  @Test
  void refreshClientsBroadcastsReloadPageWithoutReloadingDefinitions() {
    hotReloadService.refreshClients();

    ArgumentCaptor<ClientAction> captor = ArgumentCaptor.forClass(ClientAction.class);
    verify(broadcastService).broadcastMessage(captor.capture());
    assertEquals("reload-page", captor.getValue().getType());
    verifyNoInteractions(aweElements);
  }

  /**
   * Test that a missing broadcast service does not break a reload (fail open)
   */
  @Test
  void missingBroadcastServiceIsTolerated() {
    XmlHotReloadService serviceWithoutBroadcast =
      new XmlHotReloadService(aweElements, baseConfigProperties, cacheManager, schemaValidator, null);

    serviceWithoutBroadcast.reloadFor(Paths.get("/app/target/classes/application/module-one/screen/home.xml"));

    verify(aweElements).reloadScreens();
  }

  /**
   * Test that a definition invalid against its schema is not reloaded: the previous version is
   * kept and the reload is skipped so a broken edit never reaches the running context
   */
  @Test
  void invalidDefinitionSkipsReload() {
    when(schemaValidator.validate(any()))
      .thenReturn(ValidationResult.validated(List.of(new ValidationError(4, 12, "boom"))));

    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/screen/home.xml"));

    verifyNoInteractions(aweElements);
    verifyNoInteractions(queryDataCache);
  }

  /**
   * Test that a definition valid against its schema is reloaded normally
   */
  @Test
  void validDefinitionReloads() {
    when(schemaValidator.validate(any())).thenReturn(ValidationResult.validated(List.of()));

    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/screen/home.xml"));

    verify(aweElements).reloadScreens();
  }

  /**
   * Test that when validation cannot run the reload proceeds (fail open)
   */
  @Test
  void unvalidatedDefinitionFailsOpenAndReloads() {
    when(schemaValidator.validate(any())).thenReturn(ValidationResult.notValidated());

    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/screen/home.xml"));

    verify(aweElements).reloadScreens();
  }

  /**
   * Test that unknown files are never validated (nothing to reload anyway)
   */
  @Test
  void unknownFilesAreNotValidated() {
    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/global/Whatever.xml"));

    verifyNoInteractions(schemaValidator);
    verifyNoInteractions(aweElements);
  }

  /**
   * Test that a cache manager without the query cache does not break a queries reload
   */
  @Test
  void missingQueryCacheIsTolerated() {
    when(cacheManager.getCache("queryData")).thenReturn(null);

    hotReloadService.reloadFor(Paths.get("/app/target/classes/application/module-one/global/Queries.xml"));

    verify(aweElements).reloadQueries();
  }
}
