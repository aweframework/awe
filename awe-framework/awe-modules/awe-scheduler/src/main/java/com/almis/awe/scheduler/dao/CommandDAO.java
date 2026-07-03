package com.almis.awe.scheduler.dao;

import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.executor.CommandExecutorResolver;
import lombok.extern.slf4j.Slf4j;

/**
 * DAO to manage schedule commands. Thin delegator: resolves the appropriate
 * {@link com.almis.awe.scheduler.executor.CommandExecutor} strategy (local or
 * SSH) for the task and runs it. Behaviour of each execution path lives in
 * the resolved executor, keeping this class -- and its callers, notably
 * {@code CommandJobService} -- unchanged.
 */
@Slf4j
public class CommandDAO {

  // Private services
  private final CommandExecutorResolver commandExecutorResolver;

  /**
   * Autowired constructor
   *
   * @param commandExecutorResolver Command executor resolver
   */
  public CommandDAO(CommandExecutorResolver commandExecutorResolver) {
    this.commandExecutorResolver = commandExecutorResolver;
  }

  /**
   * Run a command task
   *
   * @param commandTask Task
   * @param envp Enviroment variables
   * @param timeout Timeout
   * @return exit code
   */
  public Integer runCommand(Task commandTask, String[] envp, final long timeout) {
    return commandExecutorResolver.resolve(commandTask).execute(commandTask, envp, timeout);
  }
}
