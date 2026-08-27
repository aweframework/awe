package com.almis.awe.scheduler.executor;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.dao.ServerDAO;
import com.almis.awe.scheduler.enums.SshHostKeyPolicy;
import com.almis.awe.scheduler.ssh.SshSupport;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
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
    this.sshClient.setServerKeyVerifier(SshSupport.buildServerKeyVerifier(hostKeyPolicy, knownHostsPath));
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
      SshSupport.registerIdentities(session, server, commandTask.getTrigger().getKey().toString());
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
   * Construct the remote command line to execute, mirroring the local
   * executor: the command runs directly when commandPath is blank, and
   * otherwise commandPath acts as the working directory. A bare action is
   * wrapped in a {@code command -v} lookup so the remote PATH takes
   * precedence and commandPath is only the fallback, which lets a script
   * living there run without an explicit {@code ./<script>} while a system
   * command is never shadowed by a file of the same name. Only the action is
   * resolved this way -- the remote PATH itself is left untouched, so nothing
   * else the script invokes can be picked up from commandPath. An action
   * already carrying a path separator is run as given.
   *
   * @param commandTask Task
   * @return String with remote command
   */
  private String constructCommand(Task commandTask) {
    String action = commandTask.getAction();
    String actionWithParams = action + generateParameterList(commandTask.getParameterList());
    String commandPath = commandTask.getCommandPath();

    if (commandPath == null || commandPath.isBlank()) {
      return actionWithParams;
    }

    String changeDirectory = "cd " + shellQuote(commandPath) + " && ";
    if (action == null || action.indexOf('/') >= 0) {
      return changeDirectory + actionWithParams;
    }

    return changeDirectory + "if command -v " + action + " > /dev/null 2>&1; then "
      + actionWithParams + "; else ./" + actionWithParams + "; fi";
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

    return parameters.stream()
      .map(parameter -> " " + shellQuote(Objects.toString(parameter.getValue(), "")))
      .collect(Collectors.joining());
  }

  /**
   * Wrap a value in POSIX single quotes so the remote shell takes it as one literal argument.
   *
   * @param value Value to quote
   * @return Single quoted value
   */
  private String shellQuote(String value) {
    return "'" + value.replace("'", "'\\''") + "'";
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
