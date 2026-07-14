package com.almis.awe.service.hotreload;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Extension point consulted by {@link XmlHotReloadService} when its built-in
 * {@link XmlHotReloadService#classify(Path, Path)} cannot recognize a changed XML file. Lets
 * downstream modules add hot-reload support for their own descriptor types without AWE
 * depending on them: a matching handler owns its own matching predicate, reload, and cache
 * eviction. Schema validation is framework-owned: when the handler declares a
 * {@link #schemaCatalog()}, the framework validates the changed file against it before calling
 * {@link #reload} — the same cross-cutting, hardened (XXE-safe, fail-open) invariant enforced
 * for AWE's own built-in types, applied uniformly instead of re-implemented per handler.
 *
 * @author pvidal
 */
public interface XmlReloadHandler {

  /**
   * Whether this handler recognizes and can reload the changed file
   *
   * @param moduleRoot  Module root the changed file belongs to (nullable)
   * @param changedFile Changed file path
   * @return Whether this handler matches the changed file
   */
  boolean supports(Path moduleRoot, Path changedFile);

  /**
   * Reload the definition backed by the changed file. Called only after {@link #supports}
   * returned true for it. Any validation this handler performs is its own responsibility;
   * a thrown exception is caught and logged by the caller (fail-open), never propagated
   *
   * @param moduleRoot  Module root the changed file belongs to (nullable)
   * @param changedFile Changed file path
   * @return Outcome of the reload attempt
   */
  ReloadResult reload(Path moduleRoot, Path changedFile);

  /**
   * Dispatch priority among registered handlers, ascending: when more than one handler
   * matches the same changed file, the one with the lowest order is invoked and the rest
   * are not consulted for that event
   *
   * @return Dispatch order (default: 0)
   */
  default int order() {
    return 0;
  }

  /**
   * Classpath location of the OASIS catalog the handler's own XML files validate against. When
   * present, the framework validates the changed file against this catalog before invoking
   * {@link #reload}: a validated-and-invalid file is never reloaded (previous definition kept)
   * and {@link #reload} is never called for it. When absent (the default), the framework runs
   * no validation for this handler and calls {@link #reload} unconditionally
   *
   * @return Classpath location of the schema catalog, or empty for no framework validation
   */
  default Optional<String> schemaCatalog() {
    return Optional.empty();
  }

  /**
   * Outcome of a {@link XmlReloadHandler#reload} attempt
   */
  enum ReloadResult {
    /** The handler reloaded the definition; the caller broadcasts the reload to clients */
    HANDLED,
    /** The changed file matched but was invalid; the caller must not broadcast */
    INVALID,
    /** The handler chose not to reload (e.g. a no-op edit); the caller must not broadcast */
    SKIPPED
  }
}
