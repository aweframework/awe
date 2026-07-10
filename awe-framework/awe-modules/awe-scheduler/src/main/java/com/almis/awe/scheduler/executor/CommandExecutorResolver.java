package com.almis.awe.scheduler.executor;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.constant.ServerConstants;
import com.almis.awe.scheduler.dao.ServerDAO;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolves the CommandExecutor strategy (local or remote) to use for a given
 * task, from Task.serverId + Server.typeOfConnection.
 * <p>
 * Unresolved, inactive, or unsupported server configurations resolve to a
 * fail-fast guard executor so CommandDAO's contract (never throw, always
 * return an exit code) is preserved end to end.
 */
@Slf4j
public class CommandExecutorResolver {

  // Fail-fast guard: returned whenever a task cannot be run (unresolved/unsupported
  // server). Logging of the reason happens in resolve(), so the guard itself
  // just reports failure.
  private static final CommandExecutor FAIL_FAST_EXECUTOR = (task, envp, timeout) -> 1;

  // Autowired services
  private final LocalCommandExecutor localCommandExecutor;
  private final SshCommandExecutor sshCommandExecutor;
  private final ServerDAO serverDAO;

  /**
   * Autowired constructor
   *
   * @param localCommandExecutor Local command executor
   * @param sshCommandExecutor   SSH command executor
   * @param serverDAO            Server DAO
   */
  public CommandExecutorResolver(LocalCommandExecutor localCommandExecutor, SshCommandExecutor sshCommandExecutor, ServerDAO serverDAO) {
    this.localCommandExecutor = localCommandExecutor;
    this.sshCommandExecutor = sshCommandExecutor;
    this.serverDAO = serverDAO;
  }

  /**
   * Resolve the CommandExecutor strategy for a task
   *
   * @param task Task
   * @return CommandExecutor to run the task with
   */
  public CommandExecutor resolve(Task task) {
    Integer serverId = task.getServerId();
    if (serverId == null) {
      return localCommandExecutor;
    }

    Server server;
    try {
      server = serverDAO.findServer(serverId);
    } catch (AWException exc) {
      log.error("[{}] Failed to resolve configured server for task", task.getTrigger().getKey(), exc);
      return FAIL_FAST_EXECUTOR;
    }

    if (server == null) {
      log.error("[{}] Configured server not found for task", task.getTrigger().getKey());
      return FAIL_FAST_EXECUTOR;
    }

    if (!server.isActive()) {
      log.error("[{}] Configured server is inactive for task", task.getTrigger().getKey());
      return FAIL_FAST_EXECUTOR;
    }

    if (ServerConstants.SSH.equalsIgnoreCase(server.getTypeOfConnection())) {
      return sshCommandExecutor;
    }

    log.error("[{}] Unsupported connection type for command execution: {}", task.getTrigger().getKey(), server.getTypeOfConnection());
    return FAIL_FAST_EXECUTOR;
  }
}
