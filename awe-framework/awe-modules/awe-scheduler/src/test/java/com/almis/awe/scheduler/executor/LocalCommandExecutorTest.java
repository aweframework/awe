package com.almis.awe.scheduler.executor;

import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.TriggerBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Covers local command execution: the command path is passed as the process
 * working directory when set, and the action is resolved through PATH first,
 * falling back to an executable file inside the command path.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class LocalCommandExecutorTest {

  private LocalCommandExecutor localCommandExecutor;

  @TempDir
  private Path commandPath;

  @Mock
  private Runtime runtime;

  @Mock
  private Process process;

  @BeforeEach
  void setUp() {
    localCommandExecutor = new LocalCommandExecutor(runtime, new CommandStreamLogger());
  }

  @Test
  void runExeCommand() throws Exception {
    // Mock
    given(runtime.exec(ArgumentMatchers.any(String[].class), ArgumentMatchers.any(), ArgumentMatchers.any())).willReturn(process);
    given(process.getErrorStream()).willReturn(IOUtils.toInputStream("error stream data", "UTF-8"));
    given(process.getInputStream()).willReturn(IOUtils.toInputStream("output stream data", "UTF-8"));
    given(process.exitValue()).willReturn(0);

    Task task = generateTask();
    task.setAction("test.exe");

    // Run action
    Integer exitCode = localCommandExecutor.execute(task, new String[0], 1000);

    // Check that controller are active
    verify(runtime, Mockito.times(1)).exec(ArgumentMatchers.any(String[].class), ArgumentMatchers.any(), ArgumentMatchers.any());
    assertEquals(0, exitCode);
  }

  @Test
  void runCmdCommand() throws Exception {
    // Mock
    given(runtime.exec(ArgumentMatchers.any(String[].class), ArgumentMatchers.any(), ArgumentMatchers.any())).willReturn(process);
    Task task = generateTask();
    task.setAction("test.cmd");
    task.getParameterList().add(new TaskParameter().setValue("tutu"));

    // Run action
    localCommandExecutor.execute(task, new String[]{}, 1000);

    // Check mock called
    verify(runtime, Mockito.times(1)).exec(ArgumentMatchers.any(String[].class), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  void ioExceptionDuringExecutionReturnsExitCodeOne() throws Exception {
    given(runtime.exec(ArgumentMatchers.any(String[].class), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .willThrow(new java.io.IOException("boom"));

    Task task = generateTask();
    task.setAction("test.exe");

    Integer exitCode = localCommandExecutor.execute(task, new String[0], 1000);

    assertEquals(1, exitCode);
  }

  /**
   * A parameter value containing spaces must reach the process as a single argument. Runtime.exec
   * tokenizes a command string on whitespace and ignores quotes, so the command is handed over as
   * an argument array instead.
   */
  @Test
  void parameterValueWithSpacesStaysASingleArgument() throws Exception {
    given(runtime.exec(ArgumentMatchers.any(String[].class), ArgumentMatchers.any(), ArgumentMatchers.any())).willReturn(process);

    Task task = generateTask();
    task.setAction("script.sh");
    task.getParameterList().add(new TaskParameter().setValue("two words"));
    task.getParameterList().add(new TaskParameter().setValue("plain"));

    localCommandExecutor.execute(task, new String[0], 1000);

    ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
    verify(runtime).exec(captor.capture(), ArgumentMatchers.any(), ArgumentMatchers.any());
    assertEquals(java.util.List.of("script.sh", "two words", "plain"), java.util.Arrays.asList(captor.getValue()));
  }

  /**
   * Shell metacharacters in a value are inert: they travel as literal argument text because no
   * shell parses the command.
   */
  @Test
  void shellMetacharactersInAValueAreNotInterpreted() throws Exception {
    given(runtime.exec(ArgumentMatchers.any(String[].class), ArgumentMatchers.any(), ArgumentMatchers.any())).willReturn(process);

    Task task = generateTask();
    task.setAction("script.sh");
    task.getParameterList().add(new TaskParameter().setValue("; rm -rf /tmp/x"));

    localCommandExecutor.execute(task, new String[0], 1000);

    ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
    verify(runtime).exec(captor.capture(), ArgumentMatchers.any(), ArgumentMatchers.any());
    assertEquals(java.util.List.of("script.sh", "; rm -rf /tmp/x"), java.util.Arrays.asList(captor.getValue()));
  }

  /**
   * A task without parameters is launched with the action alone.
   */
  @Test
  void taskWithoutParametersLaunchesTheActionAlone() throws Exception {
    given(runtime.exec(ArgumentMatchers.any(String[].class), ArgumentMatchers.any(), ArgumentMatchers.any())).willReturn(process);

    Task task = generateTask();
    task.setAction("script.sh");

    localCommandExecutor.execute(task, new String[0], 1000);

    ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
    verify(runtime).exec(captor.capture(), ArgumentMatchers.any(), ArgumentMatchers.any());
    assertEquals(java.util.List.of("script.sh"), java.util.Arrays.asList(captor.getValue()));
  }

  /**
   * A bare action naming an executable file inside the command path is resolved to that file:
   * execvp only searches PATH, never the working directory, so the absolute path is what makes
   * the script launchable at all.
   */
  @Test
  void bareActionIsResolvedInsideTheCommandPathWhenNotOnPath() throws Exception {
    given(runtime.exec(ArgumentMatchers.any(String[].class), ArgumentMatchers.any(), ArgumentMatchers.any())).willReturn(process);
    Path script = Files.createFile(commandPath.resolve("purge-duplicates.sh"));
    assertTrue(script.toFile().setExecutable(true));

    Task task = generateTask();
    task.setCommandPath(commandPath.toString());
    task.setAction("purge-duplicates.sh");

    localCommandExecutor.execute(task, new String[0], 1000);

    assertEquals(java.util.List.of(script.toAbsolutePath().toString()), capturedCommand());
  }

  /**
   * PATH wins over the command path: a file sitting in the command path never shadows a system
   * command of the same name, so every task relying on PATH keeps resolving as it does today.
   */
  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void commandOnPathIsNotShadowedByAFileInTheCommandPath() throws Exception {
    given(runtime.exec(ArgumentMatchers.any(String[].class), ArgumentMatchers.any(), ArgumentMatchers.any())).willReturn(process);
    Path impostor = Files.createFile(commandPath.resolve("sh"));
    assertTrue(impostor.toFile().setExecutable(true));

    Task task = generateTask();
    task.setCommandPath(commandPath.toString());
    task.setAction("sh");

    localCommandExecutor.execute(task, new String[0], 1000);

    assertEquals(java.util.List.of("sh"), capturedCommand());
  }

  /**
   * An action already carrying a path separator is handed over untouched, so the explicit
   * {@code ./script} and absolute-path forms keep behaving exactly as before.
   */
  @Test
  void actionWithAPathSeparatorIsLeftUntouched() throws Exception {
    given(runtime.exec(ArgumentMatchers.any(String[].class), ArgumentMatchers.any(), ArgumentMatchers.any())).willReturn(process);
    Path script = Files.createFile(commandPath.resolve("run.sh"));
    assertTrue(script.toFile().setExecutable(true));

    Task task = generateTask();
    task.setCommandPath(commandPath.toString());
    task.setAction("./run.sh");

    localCommandExecutor.execute(task, new String[0], 1000);

    assertEquals(java.util.List.of("./run.sh"), capturedCommand());
  }

  /**
   * A non-executable file is not a runnable command, so it is left to PATH resolution (and to
   * its failure) instead of being pointed at.
   */
  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void nonExecutableFileInTheCommandPathIsNotResolved() throws Exception {
    given(runtime.exec(ArgumentMatchers.any(String[].class), ArgumentMatchers.any(), ArgumentMatchers.any())).willReturn(process);
    Path notExecutable = Files.createFile(commandPath.resolve("data.sh"));
    assertTrue(notExecutable.toFile().setExecutable(false));

    Task task = generateTask();
    task.setCommandPath(commandPath.toString());
    task.setAction("data.sh");

    localCommandExecutor.execute(task, new String[0], 1000);

    assertEquals(java.util.List.of("data.sh"), capturedCommand());
  }

  /**
   * A blank command path leaves the action alone: there is nothing to fall back to.
   */
  @Test
  void blankCommandPathLeavesTheActionUnresolved() throws Exception {
    given(runtime.exec(ArgumentMatchers.any(String[].class), ArgumentMatchers.any())).willReturn(process);

    Task task = generateTask();
    task.setCommandPath("");
    task.setAction("purge-duplicates.sh");

    localCommandExecutor.execute(task, new String[0], 1000);

    ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
    verify(runtime).exec(captor.capture(), ArgumentMatchers.any());
    assertEquals(java.util.List.of("purge-duplicates.sh"), java.util.Arrays.asList(captor.getValue()));
  }

  private java.util.List<String> capturedCommand() throws Exception {
    ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
    verify(runtime).exec(captor.capture(), ArgumentMatchers.any(), ArgumentMatchers.any());
    return java.util.Arrays.asList(captor.getValue());
  }

  private Task generateTask() {
    Task task = new Task();
    task.setCommandPath("/test/command/");
    task.setParameterList(new ArrayList<>());
    task.setTrigger(TriggerBuilder.newTrigger().build());
    return task;
  }
}
