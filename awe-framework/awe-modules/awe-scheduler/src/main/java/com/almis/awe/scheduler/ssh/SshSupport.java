package com.almis.awe.scheduler.ssh;

import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.scheduler.enums.SshHostKeyPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier;
import org.apache.sshd.client.keyverifier.RejectAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.util.security.SecurityUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Collection;

/**
 * Shared SSH client concerns for the scheduler: host-key verification and
 * credential registration. Both the SSH command executor and the SFTP file
 * checker connect to the same configured {@link Server} entries, so the
 * host-key policy and the private-key/password handling live here rather
 * than being duplicated per consumer.
 */
@Slf4j
public final class SshSupport {

  private SshSupport() {
    // Utility class
  }

  /**
   * Build the MINA server key verifier matching the configured host-key policy.
   *
   * @param hostKeyPolicy  Host-key verification policy
   * @param knownHostsPath Path to the known_hosts file
   * @return ServerKeyVerifier
   */
  public static ServerKeyVerifier buildServerKeyVerifier(SshHostKeyPolicy hostKeyPolicy, Path knownHostsPath) {
    switch (hostKeyPolicy) {
      case STRICT:
        ensureKnownHostsDirectory(knownHostsPath);
        if (!Files.exists(knownHostsPath)) {
          log.warn("[Ssh] STRICT host-key policy configured but known_hosts file {} does not exist -- all hosts will be rejected until pre-provisioned", knownHostsPath);
        }
        return new KnownHostsServerKeyVerifier(RejectAllServerKeyVerifier.INSTANCE, knownHostsPath);
      case ACCEPT_ALL:
        return AcceptAllServerKeyVerifier.INSTANCE;
      case ACCEPT_ON_FIRST_USE:
      default:
        ensureKnownHostsDirectory(knownHostsPath);
        return new KnownHostsServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE, knownHostsPath);
    }
  }

  /**
   * Register the authentication identities on the session from the server credentials.
   * When a private key is configured it is parsed (unlocked with its passphrase when set)
   * and added as a public-key identity; when a password is configured it is added as a
   * password identity. Both may be present, in which case the SSH client attempts them in
   * turn. A key that fails to parse is logged and skipped so a configured password can
   * still authenticate; secrets are never logged.
   *
   * @param session    Client session to authenticate
   * @param server     Server holding the decrypted credentials
   * @param logContext Context used for log correlation
   */
  public static void registerIdentities(ClientSession session, Server server, String logContext) {
    String key = server.getKey();
    if (key != null && !key.isBlank()) {
      String passphrase = server.getKeyPassphrase();
      FilePasswordProvider passwordProvider = (passphrase == null || passphrase.isBlank())
        ? FilePasswordProvider.EMPTY
        : FilePasswordProvider.of(passphrase);
      try {
        Collection<KeyPair> keyPairs = SecurityUtils.getKeyPairResourceParser()
          .loadKeyPairs(null, NamedResource.ofName("ssh-key-" + server.getServerId()), passwordProvider, key);
        for (KeyPair keyPair : keyPairs) {
          session.addPublicKeyIdentity(keyPair);
        }
      } catch (IOException | GeneralSecurityException exc) {
        log.error("[{}] Failed to load the configured SSH private key for the task server", logContext, exc);
      }
    }

    String password = server.getPassword();
    if (password != null && !password.isBlank()) {
      session.addPasswordIdentity(password);
    }
  }

  /**
   * Ensure the parent directory of the known_hosts file exists so trusted
   * host keys can be persisted (TOFU) or read (STRICT). Best effort: logs a
   * loud warning on failure instead of throwing, since a missing directory
   * degrades host-key trust rather than breaking startup.
   *
   * @param knownHostsPath Path to the known_hosts file
   */
  private static void ensureKnownHostsDirectory(Path knownHostsPath) {
    try {
      Path parent = knownHostsPath.getParent();
      if (parent != null && !Files.exists(parent)) {
        Files.createDirectories(parent);
      }
    } catch (IOException exc) {
      log.warn("[Ssh] Could not provision known_hosts directory for {} -- host-key trust may not persist", knownHostsPath, exc);
    }
  }
}
