package com.almis.awe.scheduler.enums;

/**
 * Host-key verification policy for SSH remote command execution.
 * <p>
 * Maps to a MINA SSHD {@code ServerKeyVerifier} when the SSH command
 * executor is wired (see {@code SchedulerConfig} in the scheduler starter):
 * <ul>
 *   <li>{@link #ACCEPT_ON_FIRST_USE} — trust-on-first-use (TOFU); unknown
 *   hosts are accepted and persisted to known_hosts, a changed key for an
 *   already-known host is still rejected. Default.</li>
 *   <li>{@link #STRICT} — only pre-provisioned known_hosts entries are
 *   accepted; unknown or changed hosts are rejected.</li>
 *   <li>{@link #ACCEPT_ALL} — every host key is accepted; insecure, for
 *   development/testing only.</li>
 * </ul>
 *
 * @author awe
 */
public enum SshHostKeyPolicy {
  ACCEPT_ON_FIRST_USE,
  STRICT,
  ACCEPT_ALL;
}
