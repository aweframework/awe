package com.almis.awe.scheduler.executor;

import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Runs a scheduler command task on the local host via the JVM process API.
 * The command path acts as the working directory when set (the process is
 * launched there); the action is resolved from the PATH rather than being
 * forced relative, so a system command works directly and a script located
 * in the command path is invoked with an explicit {@code ./<script>}.
 */
@Slf4j
public class LocalCommandExecutor implements CommandExecutor {

  // Private services
  private final Runtime runtime;
  private final CommandStreamLogger commandStreamLogger;

  /**
   * Autowired constructor
   *
   * @param runtime             Runtime service
   * @param commandStreamLogger Command output/error stream logger
   */
  public LocalCommandExecutor(Runtime runtime, CommandStreamLogger commandStreamLogger) {
    this.runtime = runtime;
    this.commandStreamLogger = commandStreamLogger;
  }

  @Override
  public Integer execute(Task commandTask, String[] envp, long timeout) {
    int exit;
    Process proc = null;

    String finalCommand = constructCommand(commandTask);
    String rawPath = commandTask.getCommandPath();
    boolean hasWorkingDir = rawPath != null && !rawPath.isBlank();
    log.info("[Batch] Batch {} launch started on path {}", finalCommand, rawPath);

    try {
      if (hasWorkingDir) {
        proc = runtime.exec(finalCommand, envp, Paths.get(rawPath).toFile());
      } else {
        proc = runtime.exec(finalCommand, envp);
      }

      // Wait for process
      proc.waitFor(timeout, TimeUnit.SECONDS);

      // Log output and error messages
      commandStreamLogger.log(commandTask, proc.getErrorStream(), "ERROR");
      commandStreamLogger.log(commandTask, proc.getInputStream(), "OUTPUT");

      // Retrieve exit value
      exit = proc.exitValue();
    } catch (IOException exc) {
      log.error("[{}] Error executing command {}", commandTask.getTrigger().getKey(), commandTask.getAction(), exc);
      exit = 1;
    } catch (InterruptedException exc) {
      Thread.currentThread().interrupt();
      log.error("[{}] Error, command interrupted {}", commandTask.getTrigger().getKey().toString(), commandTask.getAction(), exc);
      exit = 1;
    } finally {
      if (proc != null) {
        proc.destroy();
      }
    }

    return exit;
  }

  /**
   * Construct the batch to execute
   *
   * @param commandTask Task
   * @return String with command
   */
  private String constructCommand(Task commandTask) {
    String finalCommand = commandTask.getAction() + generateParameterList(commandTask.getParameterList());

    if (SystemUtils.IS_OS_WINDOWS && !commandTask.getAction().matches("(.*).exe")) {
      if (commandTask.getAction().matches("(.*).bat")) {
        finalCommand = "start " + finalCommand;
      }
      finalCommand = "cmd /c " + finalCommand;
    }
    return finalCommand;
  }

  /**
   * Generate parameter list
   *
   * @param parameters Task parameters
   * @return String with list of parameters
   */
  private String generateParameterList(List<TaskParameter> parameters) {
    String parameterList = parameters.stream().map(TaskParameter::getValue).collect(Collectors.joining(" "));
    return parameterList.isEmpty() ? "" : " " + parameterList;
  }
}
