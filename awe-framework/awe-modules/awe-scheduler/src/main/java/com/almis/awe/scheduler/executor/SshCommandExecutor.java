package com.almis.awe.scheduler.executor;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.dao.ServerDAO;
import com.almis.awe.scheduler.enums.SshHostKeyPolicy;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier;
import org.apache.sshd.client.keyverifier.RejectAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.util.security.SecurityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.time.Duration;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runs a scheduler command task on a remote host over an SSH exec channel
 * (Apache MINA SSHD). Mirrors {@link LocalCommandExecutor}'s contract:
 * never throws, always returns a non-null exit code, and logs stdout/stderr
 * via the shared {@link CommandStreamLogger} so both executors emit output
 * in the exact same format.
 * <p>
 * Per-invocation state (session, channel, exit code) is strictly method-local
 * so concurrent invocations of {@link #execute(Task, String[], long)} on this
 * singleton bean do not interfere with each other.
 */
@Slf4j
public class SshCommandExecutor implements CommandExecutor {

  // Bounded wait for a drain thread to finish after the channel closes, so a stuck stream reader
  // can never hang the executor indefinitely.
  private static final long DRAIN_JOIN_TIMEOUT_SECONDS = 10;

  // Private services
  private final CommandStreamLogger commandStreamLogger;
  private final ServerDAO serverDAO;
  private final Duration connectTimeout;
  private final SshClient sshClient;

  /**
   * Autowired constructor
   *
   * @param commandStreamLogger Command output/error stream logger
   * @param serverDAO           Server DAO, used to resolve host/port/credentials per task
   * @param hostKeyPolicy       Host-key verification policy
   * @param knownHostsPath      Path to the known_hosts file used to persist/read trusted host keys
   * @param connectTimeout      Connect and authentication timeout
   */
  public SshCommandExecutor(CommandStreamLogger commandStreamLogger, ServerDAO serverDAO,
                             SshHostKeyPolicy hostKeyPolicy, Path knownHostsPath, Duration connectTimeout) {
    this.commandStreamLogger = commandStreamLogger;
    this.serverDAO = serverDAO;
    this.connectTimeout = connectTimeout;
    this.sshClient = SshClient.setUpDefaultClient();
    this.sshClient.setServerKeyVerifier(buildServerKeyVerifier(hostKeyPolicy, knownHostsPath));
    this.sshClient.start();
  }

  @Override
  public Integer execute(Task commandTask, String[] envp, long timeout) {
    Server server;
    try {
      server = serverDAO.findServer(commandTask.getServerId());
    } catch (AWException exc) {
      log.error("[{}] Failed to resolve configured server for SSH task execution", commandTask.getTrigger().getKey(), exc);
      return 1;
    }

    if (server == null) {
      log.error("[{}] Configured server not found for SSH task execution", commandTask.getTrigger().getKey());
      return 1;
    }

    String finalCommand = constructCommand(commandTask);
    ClientSession session = null;
    ChannelExec channel = null;

    try {
      session = sshClient.connect(server.getUser(), server.getHost(), server.getPort())
        .verify(connectTimeout)
        .getSession();
      registerIdentities(session, server, commandTask);
      session.auth().verify(connectTimeout);

      channel = session.createExecChannel(finalCommand);
      channel.open().verify(connectTimeout);

      // Drain stdout/stderr CONCURRENTLY, started BEFORE waitFor(CLOSED). A command that emits
      // more than the SSH channel window (~2MB) before exiting would otherwise back-pressure the
      // remote (nothing reads getInvertedOut/getInvertedErr), CLOSED never arrives, and a command
      // that ran fine reports a false timeout. Each thread captures only method-local references
      // (the channel and this invocation's task) so concurrent execute() calls stay isolated.
      final ChannelExec drainChannel = channel;
      Thread outputDrain = startDrain(commandTask, drainChannel.getInvertedOut(), "OUTPUT");
      Thread errorDrain = startDrain(commandTask, drainChannel.getInvertedErr(), "ERROR");

      Set<ClientChannelEvent> events = channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), Duration.ofSeconds(timeout));
      if (!events.contains(ClientChannelEvent.CLOSED)) {
        log.error("[{}] SSH command execution timed out after {}s", commandTask.getTrigger().getKey(), timeout);
        return 1;
      }

      // Let both drains finish reading any buffered tail, bounded so a stuck drain can never hang
      // the executor forever (channel close in finally unblocks them regardless).
      joinDrain(commandTask, outputDrain);
      joinDrain(commandTask, errorDrain);

      Integer exitStatus = channel.getExitStatus();
      return exitStatus == null ? 1 : exitStatus;
    } catch (Exception exc) {
      log.error("[{}] SSH command execution failed", commandTask.getTrigger().getKey(), exc);
      return 1;
    } finally {
      closeQuietly(channel);
      closeQuietly(session);
    }
  }

  /**
   * Start a daemon thread that drains a channel stream through the shared stream logger. Daemon so
   * a drain that is still blocked on a slow/stuck stream (e.g. after a timeout) can never keep the
   * JVM alive; the thread captures only method-local references so concurrent executions stay
   * isolated.
   *
   * @param commandTask Task owning the execution
   * @param stream      Channel stream to drain (inverted stdout or stderr)
   * @param type        Stream type label ("OUTPUT" or "ERROR")
   * @return the started drain thread
   */
  private Thread startDrain(Task commandTask, InputStream stream, String type) {
    Thread thread = new Thread(() -> commandStreamLogger.log(commandTask, stream, type),
      "ssh-drain-" + type.toLowerCase() + "-" + commandTask.getTrigger().getKey());
    thread.setDaemon(true);
    thread.start();
    return thread;
  }

  /**
   * Join a drain thread with a bounded timeout, preserving the never-throw contract: an interrupt
   * is logged and the flag restored rather than propagated.
   *
   * @param commandTask Task owning the execution
   * @param drain       Drain thread to join
   */
  private void joinDrain(Task commandTask, Thread drain) {
    try {
      drain.join(Duration.ofSeconds(DRAIN_JOIN_TIMEOUT_SECONDS).toMillis());
      if (drain.isAlive()) {
        log.warn("[{}] SSH output drain did not finish within {}s", commandTask.getTrigger().getKey(), DRAIN_JOIN_TIMEOUT_SECONDS);
      }
    } catch (InterruptedException exc) {
      Thread.currentThread().interrupt();
      log.warn("[{}] Interrupted while waiting for SSH output drain to finish", commandTask.getTrigger().getKey(), exc);
    }
  }

  /**
   * Release the shared SSH client's background resources on context shutdown.
   */
  @PreDestroy
  public void shutdown() {
    sshClient.stop();
  }

  /**
   * Register the authentication identities on the session from the server credentials.
   * When a private key is configured it is parsed (unlocked with its passphrase when set)
   * and added as a public-key identity; when a password is configured it is added as a
   * password identity. Both may be present, in which case the SSH client attempts them in
   * turn. A key that fails to parse is logged and skipped so a configured password can
   * still authenticate; secrets are never logged.
   *
   * @param session     Client session to authenticate
   * @param server      Server holding the decrypted credentials
   * @param commandTask Task, used for log correlation
   */
  private void registerIdentities(ClientSession session, Server server, Task commandTask) {
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
        log.error("[{}] Failed to load the configured SSH private key for the task server", commandTask.getTrigger().getKey(), exc);
      }
    }

    String password = server.getPassword();
    if (password != null && !password.isBlank()) {
      session.addPasswordIdentity(password);
    }
  }

  /**
   * Construct the remote command line to execute, mirroring the local
   * executor: the command runs directly when commandPath is blank, or a
   * {@code cd '<commandPath>' && <action> <params>} form when set, so
   * commandPath acts as the working directory and the action is resolved
   * from the remote PATH (use {@code ./<script>} explicitly to run a script
   * located in commandPath).
   *
   * @param commandTask Task
   * @return String with remote command
   */
  private String constructCommand(Task commandTask) {
    String actionWithParams = commandTask.getAction() + generateParameterList(commandTask.getParameterList());
    String commandPath = commandTask.getCommandPath();

    if (commandPath == null || commandPath.isBlank()) {
      return actionWithParams;
    }

    return "cd '" + commandPath + "' && " + actionWithParams;
  }

  /**
   * Generate parameter list
   *
   * @param parameters Task parameters
   * @return String with list of parameters
   */
  private String generateParameterList(List<TaskParameter> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return "";
    }
    String parameterList = parameters.stream().map(TaskParameter::getValue).collect(Collectors.joining(" "));
    return parameterList.isEmpty() ? "" : " " + parameterList;
  }

  /**
   * Build the MINA server key verifier matching the configured host-key policy.
   *
   * @param hostKeyPolicy  Host-key verification policy
   * @param knownHostsPath Path to the known_hosts file
   * @return ServerKeyVerifier
   */
  private ServerKeyVerifier buildServerKeyVerifier(SshHostKeyPolicy hostKeyPolicy, Path knownHostsPath) {
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
   * Ensure the parent directory of the known_hosts file exists so trusted
   * host keys can be persisted (TOFU) or read (STRICT). Best effort: logs a
   * loud warning on failure instead of throwing, since a missing directory
   * degrades host-key trust rather than breaking startup.
   *
   * @param knownHostsPath Path to the known_hosts file
   */
  private void ensureKnownHostsDirectory(Path knownHostsPath) {
    try {
      Path parent = knownHostsPath.getParent();
      if (parent != null && !Files.exists(parent)) {
        Files.createDirectories(parent);
      }
    } catch (IOException exc) {
      log.warn("[Ssh] Could not provision known_hosts directory for {} -- host-key trust may not persist", knownHostsPath, exc);
    }
  }

  private void closeQuietly(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException exc) {
        log.debug("[Ssh] Error closing SSH resource", exc);
      }
    }
  }
}
