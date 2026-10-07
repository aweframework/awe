package com.almis.awe.testing.model.types;

/**
 * When a piece of evidence of the Playwright tool (trace, video) is kept
 */
public enum EvidenceMode {
  /**
   * Never recorded
   */
  OFF,
  /**
   * Kept only when the test (trace) or a test of the class (video) failed
   */
  ON_FAILURE,
  /**
   * Always kept
   */
  ALWAYS
}
