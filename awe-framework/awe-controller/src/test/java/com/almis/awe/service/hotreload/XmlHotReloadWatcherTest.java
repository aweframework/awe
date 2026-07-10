package com.almis.awe.service.hotreload;

import com.almis.awe.config.BaseConfigProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * XmlHotReloadWatcher tests (real file system + real watch service)
 */
@ExtendWith(MockitoExtension.class)
class XmlHotReloadWatcherTest {

  private static final long VERIFY_TIMEOUT = 15000;
  // Window over which a "nothing more happens" assertion must keep holding
  private static final Duration QUIET_PERIOD = Duration.ofSeconds(1);
  private static final Duration QUIET_TIMEOUT = QUIET_PERIOD.plusSeconds(2);

  @TempDir
  Path tempDir;

  @Mock
  private XmlHotReloadService hotReloadService;

  private BaseConfigProperties baseConfigProperties;
  private XmlHotReloadWatcher watcher;
  private Path moduleRoot;

  @BeforeEach
  void setUp() throws IOException {
    baseConfigProperties = new BaseConfigProperties();
    baseConfigProperties.setModuleList(new String[]{"module-one"});

    // Fake exploded classpath root for the module
    moduleRoot = tempDir.resolve("classes/application/module-one");
    Files.createDirectories(moduleRoot.resolve("screen/sub"));
    Files.createDirectories(moduleRoot.resolve("global"));

    watcher = new XmlHotReloadWatcher(hotReloadService, baseConfigProperties) {
      @Override
      protected Map<String, List<Path>> resolveClasspathModuleRoots() {
        return Map.of("module-one", List.of(moduleRoot));
      }

      @Override
      protected List<Path> resolveStaticRoots() {
        return List.of();
      }
    };
    watcher.setDebounceMillis(300);
  }

  @AfterEach
  void tearDown() {
    watcher.stop();
  }

  /**
   * Test that touching a screen file in a subdirectory triggers a reload for it
   */
  @Test
  void screenChangeTriggersReload() throws IOException {
    watcher.start();
    assertTrue(watcher.isRunning());

    Path screenFile = moduleRoot.resolve("screen/sub/foo.xml");
    Files.writeString(screenFile, "<screen/>");

    verify(hotReloadService, timeout(VERIFY_TIMEOUT)).reloadFor(moduleRoot, screenFile);
  }

  /**
   * Test that non-XML files are ignored
   */
  @Test
  void nonXmlChangesAreIgnored() throws IOException {
    watcher.start();

    Files.writeString(moduleRoot.resolve("screen/notes.txt"), "not an xml");

    await().during(QUIET_PERIOD).atMost(QUIET_TIMEOUT)
      .untilAsserted(() -> verifyNoInteractions(hotReloadService));
  }

  /**
   * Test that repeated events over the same file are coalesced into a single reload
   */
  @Test
  void debounceCoalescesRepeatedEvents() throws IOException {
    watcher.start();

    Path queriesFile = moduleRoot.resolve("global/Queries.xml");
    for (int write = 0; write < 3; write++) {
      Files.writeString(queriesFile, "<queries version=\"" + write + "\"/>");
    }

    verify(hotReloadService, timeout(VERIFY_TIMEOUT)).reloadFor(moduleRoot, queriesFile);

    // The repeated events must coalesce into a single reload
    await().during(QUIET_PERIOD).atMost(QUIET_TIMEOUT)
      .untilAsserted(() -> verify(hotReloadService, times(1)).reloadFor(moduleRoot, queriesFile));
  }

  /**
   * Test that re-saving a file with identical content (e.g. an editor Ctrl+S that only
   * touches the modification time) does not trigger a second reload, while a real content
   * change still does
   */
  @Test
  void savingWithoutContentChangeDoesNotReloadAgain() throws IOException {
    watcher.start();

    Path screenFile = moduleRoot.resolve("screen/sub/foo.xml");
    Files.writeString(screenFile, "<screen/>");
    verify(hotReloadService, timeout(VERIFY_TIMEOUT)).reloadFor(moduleRoot, screenFile);

    // Re-save the exact same content: no second reload must be triggered
    Files.writeString(screenFile, "<screen/>");
    await().during(QUIET_PERIOD).atMost(QUIET_TIMEOUT)
      .untilAsserted(() -> verify(hotReloadService, times(1)).reloadFor(moduleRoot, screenFile));

    // A real content change is reloaded again
    Files.writeString(screenFile, "<screen label=\"changed\"/>");
    verify(hotReloadService, timeout(VERIFY_TIMEOUT).times(2)).reloadFor(moduleRoot, screenFile);
  }

  /**
   * Test that directories created after startup are watched too
   */
  @Test
  void newSubdirectoriesAreWatched() throws IOException {
    watcher.start();

    Path newFolder = moduleRoot.resolve("screen/created-later");
    Files.createDirectories(newFolder);
    Path screenFile = newFolder.resolve("bar.xml");
    Files.writeString(screenFile, "<screen/>");

    verify(hotReloadService, timeout(VERIFY_TIMEOUT)).reloadFor(moduleRoot, screenFile);
  }

  /**
   * Test that changes in a configured source directory are copied to the classpath
   * root and trigger a reload
   */
  @Test
  void sourceChangesAreCopiedAndReloaded() throws IOException {
    Path sourceDir = tempDir.resolve("src/main/resources");
    Files.createDirectories(sourceDir.resolve("application/module-one/global"));
    baseConfigProperties.setXmlHotReloadSources(List.of(sourceDir.toString()));
    watcher.start();

    Path sourceFile = sourceDir.resolve("application/module-one/global/Queries.xml");
    Files.writeString(sourceFile, "<queries/>");

    Path targetFile = moduleRoot.resolve("global/Queries.xml");
    verify(hotReloadService, timeout(VERIFY_TIMEOUT)).reloadFor(moduleRoot, targetFile);
    assertTrue(Files.exists(targetFile));
    assertEquals("<queries/>", Files.readString(targetFile));
  }

  /**
   * Test that the watcher does not start without watchable roots
   */
  @Test
  void watcherWithoutRootsDoesNotStart() {
    XmlHotReloadWatcher emptyWatcher = new XmlHotReloadWatcher(hotReloadService, baseConfigProperties) {
      @Override
      protected Map<String, List<Path>> resolveClasspathModuleRoots() {
        return Map.of();
      }

      @Override
      protected List<Path> resolveStaticRoots() {
        return List.of();
      }
    };

    emptyWatcher.start();

    assertFalse(emptyWatcher.isRunning());
    emptyWatcher.stop();
  }

  /**
   * Test that a rebuilt frontend bundle (JS/CSS under a watched static root) refreshes the
   * connected clients without triggering any definition reload
   */
  @Test
  void frontendBundleChangeRefreshesClients() throws IOException {
    Path staticRoot = tempDir.resolve("classes/static");
    Files.createDirectories(staticRoot.resolve("js"));
    XmlHotReloadWatcher assetWatcher = new XmlHotReloadWatcher(hotReloadService, baseConfigProperties) {
      @Override
      protected Map<String, List<Path>> resolveClasspathModuleRoots() {
        return Map.of("module-one", List.of(moduleRoot));
      }

      @Override
      protected List<Path> resolveStaticRoots() {
        return List.of(staticRoot);
      }
    };
    assetWatcher.setDebounceMillis(300);
    try {
      assetWatcher.start();

      Files.writeString(staticRoot.resolve("js/specific.js"), "console.log('v1')");

      verify(hotReloadService, timeout(VERIFY_TIMEOUT)).refreshClients();
      verify(hotReloadService, never()).reloadFor(any(Path.class), any(Path.class));
    } finally {
      assetWatcher.stop();
    }
  }

  /**
   * Test that a non-bundle static file (e.g. an image) does not refresh clients
   */
  @Test
  void nonBundleStaticFileDoesNotRefreshClients() throws IOException {
    Path staticRoot = tempDir.resolve("classes/static");
    Files.createDirectories(staticRoot.resolve("images"));
    XmlHotReloadWatcher assetWatcher = new XmlHotReloadWatcher(hotReloadService, baseConfigProperties) {
      @Override
      protected Map<String, List<Path>> resolveClasspathModuleRoots() {
        return Map.of("module-one", List.of(moduleRoot));
      }

      @Override
      protected List<Path> resolveStaticRoots() {
        return List.of(staticRoot);
      }
    };
    assetWatcher.setDebounceMillis(300);
    try {
      assetWatcher.start();

      Files.writeString(staticRoot.resolve("images/logo.txt"), "not a bundle");

      await().during(QUIET_PERIOD).atMost(QUIET_TIMEOUT)
        .untilAsserted(() -> verify(hotReloadService, never()).refreshClients());
    } finally {
      assetWatcher.stop();
    }
  }

  /**
   * Test that a reload failure does not kill the watcher: the next change must still
   * be processed
   */
  @Test
  void watcherSurvivesReloadFailures() throws IOException {
    doThrow(new RuntimeException("Reload failure")).doNothing()
      .when(hotReloadService).reloadFor(any(Path.class), any(Path.class));
    watcher.start();

    // First change: the reload blows up inside the watch thread
    Path firstFile = moduleRoot.resolve("screen/first.xml");
    Files.writeString(firstFile, "<screen/>");
    verify(hotReloadService, timeout(VERIFY_TIMEOUT)).reloadFor(moduleRoot, firstFile);

    // Second change: the watcher must still be alive and process it
    Path secondFile = moduleRoot.resolve("screen/second.xml");
    Files.writeString(secondFile, "<screen/>");
    verify(hotReloadService, timeout(VERIFY_TIMEOUT)).reloadFor(moduleRoot, secondFile);
    assertTrue(watcher.isRunning());
  }

  /**
   * Test that deleting an XML file triggers a reload of its category
   */
  @Test
  void screenDeletionTriggersReload() throws IOException {
    Path screenFile = moduleRoot.resolve("screen/deleted.xml");
    Files.writeString(screenFile, "<screen/>");
    watcher.start();

    Files.delete(screenFile);

    verify(hotReloadService, timeout(VERIFY_TIMEOUT)).reloadFor(moduleRoot, screenFile);
  }

  /**
   * Test that deleting a source file removes its synchronized classpath copy and
   * triggers a reload
   */
  @Test
  void sourceDeletionRemovesSynchronizedCopyAndReloads() throws IOException {
    Path sourceDir = tempDir.resolve("src/main/resources");
    Files.createDirectories(sourceDir.resolve("application/module-one/global"));
    baseConfigProperties.setXmlHotReloadSources(List.of(sourceDir.toString()));
    Path sourceFile = sourceDir.resolve("application/module-one/global/Queries.xml");
    Files.writeString(sourceFile, "<queries/>");
    Path targetFile = moduleRoot.resolve("global/Queries.xml");
    Files.writeString(targetFile, "<queries/>");
    watcher.start();

    Files.delete(sourceFile);

    verify(hotReloadService, timeout(VERIFY_TIMEOUT)).reloadFor(moduleRoot, targetFile);
    assertFalse(Files.exists(targetFile));
  }

  /**
   * Test that symbolic links inside a watched source folder are never synchronized
   * to the classpath (arbitrary-file-read hardening)
   */
  @Test
  void symbolicLinksAreNotSynchronized() throws IOException {
    Path sourceDir = tempDir.resolve("src/main/resources");
    Files.createDirectories(sourceDir.resolve("application/module-one/global"));
    baseConfigProperties.setXmlHotReloadSources(List.of(sourceDir.toString()));
    Path secretFile = tempDir.resolve("secret.xml");
    Files.writeString(secretFile, "<secret/>");
    watcher.start();

    Files.createSymbolicLink(sourceDir.resolve("application/module-one/global/Queries.xml"), secretFile);

    await().during(QUIET_PERIOD).atMost(QUIET_TIMEOUT).untilAsserted(() -> {
      assertFalse(Files.exists(moduleRoot.resolve("global/Queries.xml")));
      verifyNoInteractions(hotReloadService);
    });
  }

  /**
   * Test that a stopped watcher instance can be started again
   */
  @Test
  void watcherCanBeRestarted() throws IOException {
    watcher.start();
    watcher.stop();
    assertFalse(watcher.isRunning());

    watcher.start();
    assertTrue(watcher.isRunning());

    Path screenFile = moduleRoot.resolve("screen/after-restart.xml");
    Files.writeString(screenFile, "<screen/>");
    verify(hotReloadService, timeout(VERIFY_TIMEOUT)).reloadFor(moduleRoot, screenFile);
  }
}
