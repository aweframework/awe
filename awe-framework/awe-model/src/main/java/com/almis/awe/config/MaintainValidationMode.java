package com.almis.awe.config;

/**
 * Validation mode for multiple maintain operations that lack a declared non-audit list variable.
 *
 * <p>Introduced to provide a migration path for the breaking change added in 4.12.1, where such
 * operations started throwing instead of silently executing zero iterations (pre-4.12.1 behavior).
 */
public enum MaintainValidationMode {

  /**
   * Fail fast: a multiple maintain without a valid non-audit list variable is a configuration error
   * and throws. Protects new development. This is the default.
   */
  STRICT,

  /**
   * Tolerant: log a warning with the operation id and skip the operation, preserving pre-4.12.1
   * no-op semantics while keeping the defective descriptor visible in the logs.
   */
  WARN
}
