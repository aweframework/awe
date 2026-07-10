package com.almis.awe.service.hotreload;

import com.almis.awe.config.BaseConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Watches the exploded classpath folders (and optionally the configured source folders)
 * of every application module and triggers an {@link XmlHotReloadService} reload when an
 * XML definition file changes. Development-only component, wired when
 * {@code awe.application.xml-hot-reload} is enabled.
 *
 * <p>Only {@code file:} classpath roots (exploded directories such as {@code target/classes})
 * are watched; jar entries are skipped, which makes the watcher production-safe by
 * construction.</p>
 *
 * @author pvidal
 */
@Slf4j
public class XmlHotReloadWatcher implements SmartLifecycle {

  private static final long DEFAULT_DEBOUNCE_MILLIS = 400;
  private static final long SELF_SYNC_IGNORE_MILLIS = 10000;
  private static final long POLL_MILLIS = 100;

  // Autowired services
  private final XmlHotReloadService hotReloadService;
  private final BaseConfigProperties baseConfigProperties;

  // Watcher state
  private long debounceMillis = DEFAULT_DEBOUNCE_MILLIS;
  private volatile boolean running = false;
  private WatchService watchService;
  private Thread watchThread;
  private final Map<WatchKey, Path> watchedDirectories = new ConcurrentHashMap<>();
  private final List<Path> classpathModuleRoots = new ArrayList<>();
  private final List<SourceMapping> sourceMappings = new ArrayList<>();
  private final Map<Path, Long> pendingChanges = new ConcurrentHashMap<>();
  private final Map<Path, Long> selfSynchronizedFiles = new ConcurrentHashMap<>();
  private final Map<Path, String> contentHashes = new ConcurrentHashMap<>();
  private final List<Path> staticRoots = new ArrayList<>();

  /**
   * Mapping between a watched source root and the classpath root it synchronizes to
   *
   * @param sourceRoot Source module root (e.g. src/main/resources/application/module)
   * @param targetRoot Classpath module root (e.g. target/classes/application/module)
   */
  private record SourceMapping(Path sourceRoot, Path targetRoot) {
  }

  /**
   * Autowired constructor
   *
   * @param hotReloadService     XML hot reload service
   * @param baseConfigProperties Base configuration properties
   */
  public XmlHotReloadWatcher(XmlHotReloadService hotReloadService, BaseConfigProperties baseConfigProperties) {
    this.hotReloadService = hotReloadService;
    this.baseConfigProperties = baseConfigProperties;
  }

  /**
   * Set the debounce interval used to coalesce repeated file events
   *
   * @param debounceMillis Debounce interval in milliseconds
   */
  void setDebounceMillis(long debounceMillis) {
    this.debounceMillis = debounceMillis;
  }

  @Override
  public void start() {
    if (running) {
      return;
    }

    // Clear the mutable state: the instance may be started again after a stop
    watchedDirectories.clear();
    classpathModuleRoots.clear();
    sourceMappings.clear();
    pendingChanges.clear();
    selfSynchronizedFiles.clear();
    contentHashes.clear();
    staticRoots.clear();

    try {
      Map<String, List<Path>> moduleRoots = resolveClasspathModuleRoots();
      List<Path> watchRoots = new ArrayList<>();
      moduleRoots.values().forEach(watchRoots::addAll);
      moduleRoots.values().forEach(classpathModuleRoots::addAll);
      resolveSourceMappings(moduleRoots);
      sourceMappings.forEach(mapping -> watchRoots.add(mapping.sourceRoot()));

      // Watch the webpack output too: a rebuilt JS/CSS bundle triggers a client refresh
      staticRoots.addAll(resolveStaticRoots());
      watchRoots.addAll(staticRoots);

      if (watchRoots.isEmpty()) {
        log.info("XML hot reload is enabled but no watchable module folders were found (only exploded 'file:' classpath roots can be watched)");
        return;
      }

      watchService = FileSystems.getDefault().newWatchService();
      for (Path root : watchRoots) {
        registerRecursively(root);
      }

      running = true;
      watchThread = new Thread(this::watchLoop, "awe-xml-hot-reload");
      watchThread.setDaemon(true);
      watchThread.start();
      logActivationBanner(watchRoots);
    } catch (IOException exc) {
      log.error("XML hot reload watcher could not be started", exc);
    }
  }

  @Override
  public void stop() {
    running = false;
    if (watchService != null) {
      try {
        watchService.close();
      } catch (IOException exc) {
        log.warn("Error closing the XML hot reload watch service", exc);
      }
    }
    if (watchThread != null) {
      try {
        watchThread.join(2000);
      } catch (InterruptedException exc) {
        Thread.currentThread().interrupt();
      }
      if (watchThread.isAlive()) {
        log.warn("XML hot reload: the watch thread did not stop in time. Interrupting it");
        watchThread.interrupt();
      }
    }
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  /**
   * Log a highly visible, coloured startup banner so a developer can tell at a glance that XML
   * hot reload is ON. Colour is emitted only when the console supports ANSI (Spring Boot's
   * {@link AnsiOutput} strips it for log files and CI), so it never pollutes non-terminal output
   *
   * @param watchRoots Folders being watched
   */
  private void logActivationBanner(List<Path> watchRoots) {
    String rule = "========================================================================";
    // Colour each line on its own. Line multiplexers such as `concurrently` (used by
    // `npm run start:hot-reload`) prepend a reset-terminated prefix to every line, which would
    // clear a colour set once at the start of a multi-line block. AnsiOutput emits nothing when
    // the console has no ANSI support, so log files and CI stay plain.
    String banner = System.lineSeparator()
      + colourLine(rule) + System.lineSeparator()
      + colourLine("  XML HOT RELOAD IS ACTIVE  -  development only, never in production") + System.lineSeparator()
      + colourLine(rule);
    log.warn("{}{}  Watching: {}", banner, System.lineSeparator(), watchRoots);
  }

  /**
   * Wrap a single line in a bright-yellow, bold ANSI span (reset at the end of the line)
   *
   * @param content Line content
   * @return Coloured line (plain when the console has no ANSI support)
   */
  private String colourLine(String content) {
    return AnsiOutput.toString(AnsiColor.BRIGHT_YELLOW, AnsiStyle.BOLD, content, AnsiStyle.NORMAL);
  }

  /**
   * Resolve the exploded classpath root of each application module. Only {@code file:}
   * resources are kept: jar entries cannot be watched (nor edited), so packaged deployments
   * resolve no roots and the watcher stays idle.
   *
   * @return Module name to exploded classpath roots
   */
  protected Map<String, List<Path>> resolveClasspathModuleRoots() {
    Map<String, List<Path>> moduleRoots = new LinkedHashMap<>();
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    String applicationPath = stripLeadingSlash(baseConfigProperties.getPaths().getApplication());
    for (String module : baseConfigProperties.getModuleList()) {
      try {
        Resource[] resources = resolver.getResources("classpath*:" + applicationPath + module + "/");
        for (Resource resource : resources) {
          URL url = resource.getURL();
          if ("file".equals(url.getProtocol())) {
            moduleRoots.computeIfAbsent(module, key -> new ArrayList<>()).add(Path.of(url.toURI()));
          }
        }
      } catch (Exception exc) {
        log.warn("Could not resolve classpath folders for module '{}'", module, exc);
      }
    }
    return moduleRoots;
  }

  /**
   * Resolve the exploded static resource roots (the webpack output, e.g.
   * {@code target/classes/static}). Only {@code file:} roots are kept, so packaged deployments
   * resolve nothing and never trigger a client refresh.
   *
   * @return Static resource roots to watch
   */
  protected List<Path> resolveStaticRoots() {
    List<Path> roots = new ArrayList<>();
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    try {
      for (Resource resource : resolver.getResources("classpath*:static/")) {
        URL url = resource.getURL();
        if ("file".equals(url.getProtocol())) {
          roots.add(Path.of(url.toURI()));
        }
      }
    } catch (Exception exc) {
      log.warn("Could not resolve static resource folders for hot reload", exc);
    }
    return roots;
  }

  /**
   * Resolve the configured source directories into source-to-classpath mappings
   *
   * @param moduleRoots Module name to exploded classpath roots
   */
  private void resolveSourceMappings(Map<String, List<Path>> moduleRoots) {
    String applicationPath = stripLeadingSlash(baseConfigProperties.getPaths().getApplication());
    for (String sourceDirectory : baseConfigProperties.getXmlHotReloadSources()) {
      for (String module : baseConfigProperties.getModuleList()) {
        Path sourceRoot = Path.of(sourceDirectory, applicationPath, module);
        if (!Files.isDirectory(sourceRoot)) {
          continue;
        }
        List<Path> targets = moduleRoots.getOrDefault(module, List.of());
        if (targets.isEmpty()) {
          log.warn("Source folder '{}' has no exploded classpath root to synchronize to. Ignoring it", sourceRoot);
        } else {
          sourceMappings.add(new SourceMapping(sourceRoot, targets.get(0)));
        }
      }
    }
  }

  /**
   * Register a directory tree in the watch service
   *
   * @param root Root directory
   * @throws IOException Error registering the directory tree
   */
  private void registerRecursively(Path root) throws IOException {
    try (Stream<Path> directories = Files.walk(root)) {
      for (Path directory : directories.filter(Files::isDirectory).toList()) {
        if (Files.isSymbolicLink(directory)) {
          log.debug("XML hot reload: skipping symbolic link folder '{}'", directory);
          continue;
        }
        WatchKey key = directory.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
        watchedDirectories.put(key, directory);
      }
    }
  }

  /**
   * Watch loop: drains file events into the pending change map and processes the changes
   * whose debounce interval has elapsed. Reload failures never kill the loop
   */
  private void watchLoop() {
    try {
      while (running) {
        try {
          WatchKey key = watchService.poll(POLL_MILLIS, TimeUnit.MILLISECONDS);
          if (key != null) {
            drainEvents(key);
          }
          processPendingChanges();
        } catch (InterruptedException exc) {
          Thread.currentThread().interrupt();
          return;
        } catch (ClosedWatchServiceException exc) {
          return;
        } catch (Exception exc) {
          log.error("XML hot reload: unexpected error in the watch loop. The watcher keeps running", exc);
        }
      }
    } finally {
      // Keep the lifecycle flag truthful whenever the loop exits
      running = false;
    }
  }

  /**
   * Drain the events of a signalled watch key into the pending change map
   *
   * @param key Signalled watch key
   */
  private void drainEvents(WatchKey key) {
    Path directory = watchedDirectories.get(key);
    for (WatchEvent<?> event : key.pollEvents()) {
      processWatchEvent(directory, event);
    }
    if (!key.reset()) {
      Path lostDirectory = watchedDirectories.remove(key);
      log.warn("XML hot reload: watched folder '{}' is no longer accessible (deleted or rebuilt). It is not watched anymore", lostDirectory);
    }
  }

  /**
   * Handle a single watch event: enqueue XML changes, watch newly created folders, or trigger
   * a full reload on overflow. Symbolic links are never enqueued
   *
   * @param directory Directory the event's watch key maps to (nullable)
   * @param event     Watch event to process
   */
  private void processWatchEvent(Path directory, WatchEvent<?> event) {
    if (OVERFLOW.equals(event.kind())) {
      // Events have been lost: the changed files are unknown, so reload everything
      log.warn("XML hot reload: watch events lost (overflow). Triggering a full reload");
      reloadAllSafely();
      return;
    }
    if (directory == null) {
      return;
    }
    Path changed = directory.resolve((Path) event.context());
    if (Files.isDirectory(changed)) {
      if (ENTRY_CREATE.equals(event.kind())) {
        watchNewDirectory(changed);
      }
      return;
    }
    if (isXmlFile(changed) || isRefreshableAsset(changed)) {
      if (Files.isSymbolicLink(changed)) {
        log.debug("XML hot reload: skipping symbolic link '{}'", changed);
        return;
      }
      // Deletions are enqueued too: the category re-scan drops the removed definitions
      pendingChanges.put(changed, System.currentTimeMillis());
    }
  }

  /**
   * Whether a changed file is a rebuilt frontend asset (a JS or CSS bundle under a watched
   * static root). Such a change triggers a client refresh, not a definition reload
   *
   * @param file Changed file
   * @return Whether the file is a refreshable frontend asset
   */
  private boolean isRefreshableAsset(Path file) {
    if (staticRoots.stream().noneMatch(file::startsWith)) {
      return false;
    }
    String name = file.getFileName().toString();
    return name.endsWith(".js") || name.endsWith(".css");
  }

  /**
   * Watch a directory created after startup and enqueue any XML file it already contains
   * (files may have been created before the directory registration)
   *
   * @param directory New directory
   */
  private void watchNewDirectory(Path directory) {
    if (Files.isSymbolicLink(directory)) {
      log.debug("XML hot reload: skipping symbolic link folder '{}'", directory);
      return;
    }
    try {
      registerRecursively(directory);
      try (Stream<Path> children = Files.walk(directory)) {
        children.filter(Files::isRegularFile)
          .filter(this::isXmlFile)
          .filter(file -> !Files.isSymbolicLink(file))
          .forEach(file -> pendingChanges.put(file, System.currentTimeMillis()));
      }
    } catch (IOException exc) {
      log.warn("Could not watch new folder '{}'", directory, exc);
    }
  }

  /**
   * Trigger a full reload without letting a reload failure kill the watch thread
   */
  private void reloadAllSafely() {
    try {
      hotReloadService.reloadAll();
    } catch (Exception exc) {
      log.error("XML hot reload: full reload failed. The watcher keeps running", exc);
    }
  }

  /**
   * Process the pending changes whose debounce interval has elapsed
   */
  private void processPendingChanges() {
    long now = System.currentTimeMillis();
    for (Map.Entry<Path, Long> pending : pendingChanges.entrySet()) {
      if (now - pending.getValue() >= debounceMillis && pendingChanges.remove(pending.getKey(), pending.getValue())) {
        handleChange(pending.getKey());
      }
    }
    // Purge stale self-synchronization marks
    selfSynchronizedFiles.entrySet().removeIf(mark -> now - mark.getValue() > SELF_SYNC_IGNORE_MILLIS);
  }

  /**
   * Handle a debounced file change: synchronize it to the classpath root when it comes
   * from a source folder, then trigger the reload. Reload failures are logged and never
   * propagate to the watch loop
   *
   * @param changedFile Changed file
   */
  private void handleChange(Path changedFile) {
    // Skip events generated by our own source-to-classpath copies
    if (selfSynchronizedFiles.remove(changedFile) != null) {
      log.debug("XML hot reload: skipping self-synchronized file '{}'", changedFile);
      return;
    }

    // Skip no-op saves: editors (e.g. VS Code Ctrl+S) rewrite the file and touch its
    // modification time even when the content is unchanged, which would otherwise reload
    if (isUnchangedContent(changedFile)) {
      log.debug("XML hot reload: '{}' saved with no content change; skipping reload", changedFile);
      return;
    }

    try {
      // A rebuilt frontend bundle only needs the browser to refresh, not a definition reload
      if (isRefreshableAsset(changedFile)) {
        log.info("XML hot reload: frontend asset '{}' changed, refreshing connected clients", changedFile);
        hotReloadService.refreshClients();
        return;
      }
      Optional<SourceMapping> mapping = sourceMappings.stream()
        .filter(candidate -> changedFile.startsWith(candidate.sourceRoot()))
        .findFirst();
      if (mapping.isPresent()) {
        synchronizeAndReload(changedFile, mapping.get());
      } else {
        hotReloadService.reloadFor(findModuleRoot(changedFile), changedFile);
      }
    } catch (Exception exc) {
      log.error("XML hot reload: reload failed for '{}'. The watcher keeps running", changedFile, exc);
    }
  }

  /**
   * Whether a changed file carries the same content as the last time it was processed. A
   * missing (deleted) or unreadable file is never reported as unchanged, so deletions and
   * genuine edits are always processed
   *
   * @param changedFile Changed file
   * @return Whether the content is identical to the last processed version
   */
  private boolean isUnchangedContent(Path changedFile) {
    if (!Files.isRegularFile(changedFile)) {
      contentHashes.remove(changedFile);
      return false;
    }
    String hash = computeContentHash(changedFile);
    if (hash == null) {
      return false;
    }
    return hash.equals(contentHashes.put(changedFile, hash));
  }

  /**
   * Compute a content hash for change detection
   *
   * @param file File to hash
   * @return Hex-encoded SHA-256 of the file content, or null when it cannot be read
   */
  private String computeContentHash(Path file) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)));
    } catch (IOException | NoSuchAlgorithmException exc) {
      log.debug("XML hot reload: could not hash '{}' for change detection", file, exc);
      return null;
    }
  }

  /**
   * Find the classpath module root a changed file belongs to
   *
   * @param changedFile Changed file
   * @return Module root or null when the file is outside every known root
   */
  private Path findModuleRoot(Path changedFile) {
    return classpathModuleRoots.stream()
      .filter(changedFile::startsWith)
      .findFirst()
      .orElse(null);
  }

  /**
   * Synchronize a changed source file to its classpath location (copy on change, delete on
   * source removal) and trigger the reload. Symbolic links are never synchronized
   *
   * @param sourceFile Changed source file
   * @param mapping    Source mapping
   */
  private void synchronizeAndReload(Path sourceFile, SourceMapping mapping) {
    Path targetFile = mapping.targetRoot().resolve(mapping.sourceRoot().relativize(sourceFile)).normalize();
    if (!targetFile.startsWith(mapping.targetRoot())) {
      log.warn("XML hot reload: refusing to synchronize '{}' outside the classpath root '{}'", sourceFile, mapping.targetRoot());
      return;
    }
    try {
      if (Files.notExists(sourceFile, LinkOption.NOFOLLOW_LINKS)) {
        // Source file deleted: remove the synchronized classpath copy
        if (Files.deleteIfExists(targetFile)) {
          selfSynchronizedFiles.put(targetFile, System.currentTimeMillis());
          log.info("XML hot reload: deleted synchronized copy '{}' after removal of '{}'", targetFile, sourceFile);
        }
      } else if (Files.isSymbolicLink(sourceFile)) {
        log.debug("XML hot reload: skipping symbolic link '{}'", sourceFile);
        return;
      } else {
        Files.createDirectories(targetFile.getParent());
        Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        selfSynchronizedFiles.put(targetFile, System.currentTimeMillis());
        log.info("XML hot reload: synchronized '{}' to '{}'", sourceFile, targetFile);
      }
    } catch (IOException exc) {
      log.error("XML hot reload: could not synchronize '{}' to '{}'", sourceFile, targetFile, exc);
      return;
    }
    hotReloadService.reloadFor(mapping.targetRoot(), targetFile);
  }

  /**
   * Check whether a file has the configured XML extension
   *
   * @param file File to check
   * @return Whether the file is an XML file
   */
  private boolean isXmlFile(Path file) {
    return file.getFileName().toString().endsWith(baseConfigProperties.getExtensionXml());
  }

  /**
   * Strip the leading slash of a classpath location (classpath scanning locations must be relative)
   *
   * @param path Path to strip
   * @return Path without leading slash
   */
  private static String stripLeadingSlash(String path) {
    return path.startsWith("/") ? path.substring(1) : path;
  }
}
