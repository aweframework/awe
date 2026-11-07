package com.almis.awe.security.authorization;

/**
 * Kind of target that a client launches
 */
public enum TargetKind {
  /**
   * Maintain target
   */
  MAINTAIN,
  /**
   * Query
   */
  QUERY,
  /**
   * Service called by an action
   */
  SERVICE;

  /**
   * Retrieve the name of the kind for logs and audit details
   *
   * @return Lower case name
   */
  public String label() {
    return name().toLowerCase();
  }
}
