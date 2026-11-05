package com.almis.awe.security.authorization;

/**
 * Decides if a target launched by a client can be launched by the session user. The launches made by the server are
 * never checked.
 *
 * <p>Every bean of this type is a link of the {@link TargetLaunchAuthorizerChain}, which the services call: a launch
 * goes on only when every link allows it, so a new authorization layer is one more bean.</p>
 */
@FunctionalInterface
public interface TargetLaunchAuthorizer {

  /**
   * Check if a client can launch a target
   *
   * @param targetId Maintain or query identifier
   * @param kind     Kind of target
   * @return The launch must go on (false when it must be refused)
   */
  boolean authorize(String targetId, TargetKind kind);
}
