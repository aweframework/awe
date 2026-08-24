package com.almis.awe.scheduler.executor;

import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Runs a scheduler command task on the local host via the JVM process API.
 * The command path acts as the working directory when set (the process is
 * launched there), and the action is resolved through the PATH first, falling
 * back to an executable file inside the command path. A system command keeps
 * resolving from the PATH and is never shadowed by a file of the same name in
 * the command path, while a script living there runs without an explicit
 * {@code ./<script>} -- which alone could never work, since execvp searches
 * the PATH and never the working directory.
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

    String[] finalCommand = constructCommand(commandTask);
    String rawPath = commandTask.getCommandPath();
    boolean hasWorkingDir = rawPath != null && !rawPath.isBlank();
    log.info("[Batch] Batch {} launch started on path {}", String.join(" ", finalCommand), rawPath);

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
   * @return Command and its arguments, one array element each
   */
  private String[] constructCommand(Task commandTask) {
    String action = resolveAction(commandTask.getAction(), commandTask.getCommandPath());
    List<String> command = new ArrayList<>();

    if (SystemUtils.IS_OS_WINDOWS && !action.matches("(.*).exe")) {
      command.add("cmd");
      command.add("/c");
      if (action.matches("(.*).bat")) {
        command.add("start");
      }
    }

    command.add(action);
    if (commandTask.getParameterList() != null) {
      commandTask.getParameterList().stream()
        .map(parameter -> Objects.toString(parameter.getValue(), ""))
        .forEach(command::add);
    }

    return command.toArray(new String[0]);
  }

  /**
   * Resolve the action to launch: the PATH takes precedence, and the command
   * path is only a fallback. An action already carrying a path separator is
   * left untouched, so the explicit {@code ./<script>} and absolute-path forms
   * keep working. Giving the PATH precedence keeps every task that resolves
   * today resolving the same way, and stops a file dropped into the command
   * path -- often a data directory -- from taking over a system command name.
   *
   * @param action      Configured action
   * @param commandPath Configured command path
   * @return Action to hand over to the process API
   */
  private String resolveAction(String action, String commandPath) {
    if (action == null || action.isBlank()
      || commandPath == null || commandPath.isBlank()
      || action.indexOf('/') >= 0 || action.indexOf('\\') >= 0
      || isOnPath(action)) {
      return action;
    }

    Path candidate = toPath(commandPath, action);
    return candidate != null && isExecutableFile(candidate) ? candidate.toString() : action;
  }

  /**
   * Check whether the action resolves to an executable file through the PATH.
   *
   * @param action Configured action
   * @return true when the PATH already provides the action
   */
  private boolean isOnPath(String action) {
    String systemPath = System.getenv("PATH");
    if (systemPath == null || systemPath.isBlank()) {
      return false;
    }

    return Arrays.stream(systemPath.split(File.pathSeparator))
      .filter(directory -> !directory.isBlank())
      .map(directory -> toPath(directory, action))
      .filter(Objects::nonNull)
      .anyMatch(this::isExecutableFile);
  }

  /**
   * Resolve a name inside a directory, tolerating entries that are not valid
   * paths on this platform (a quoted PATH entry on Windows, for instance).
   *
   * @param directory Directory to resolve into
   * @param name      Name to resolve
   * @return Absolute normalized path, or null when the input is not a valid path
   */
  private Path toPath(String directory, String name) {
    try {
      return Paths.get(directory).resolve(name).toAbsolutePath().normalize();
    } catch (InvalidPathException exc) {
      log.debug("[Batch] Ignoring invalid path entry {} while resolving {}", directory, name, exc);
      return null;
    }
  }

  /**
   * Check that a candidate is a runnable command: a regular file with execute permission.
   *
   * @param candidate Candidate path
   * @return true when the candidate can be executed
   */
  private boolean isExecutableFile(Path candidate) {
    return Files.isRegularFile(candidate) && Files.isExecutable(candidate);
  }
}
