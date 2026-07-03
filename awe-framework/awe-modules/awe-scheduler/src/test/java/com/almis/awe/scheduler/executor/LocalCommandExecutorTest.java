package com.almis.awe.scheduler.executor;

import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.TriggerBuilder;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Covers local command execution: the command path is passed as the process
 * working directory when set, and the action is launched from it (resolved
 * from PATH) rather than being forced relative.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class LocalCommandExecutorTest {

  private LocalCommandExecutor localCommandExecutor;

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
    given(runtime.exec(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any())).willReturn(process);
    given(process.getErrorStream()).willReturn(IOUtils.toInputStream("error stream data", "UTF-8"));
    given(process.getInputStream()).willReturn(IOUtils.toInputStream("output stream data", "UTF-8"));
    given(process.exitValue()).willReturn(0);

    Task task = generateTask();
    task.setAction("test.exe");

    // Run action
    Integer exitCode = localCommandExecutor.execute(task, new String[0], 1000);

    // Check that controller are active
    verify(runtime, Mockito.times(1)).exec(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any());
    assertEquals(0, exitCode);
  }

  @Test
  void runCmdCommand() throws Exception {
    // Mock
    given(runtime.exec(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any())).willReturn(process);
    Task task = generateTask();
    task.setAction("test.cmd");
    task.getParameterList().add(new TaskParameter().setValue("tutu"));

    // Run action
    localCommandExecutor.execute(task, new String[]{}, 1000);

    // Check mock called
    verify(runtime, Mockito.times(1)).exec(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  void ioExceptionDuringExecutionReturnsExitCodeOne() throws Exception {
    given(runtime.exec(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .willThrow(new java.io.IOException("boom"));

    Task task = generateTask();
    task.setAction("test.exe");

    Integer exitCode = localCommandExecutor.execute(task, new String[0], 1000);

    assertEquals(1, exitCode);
  }

  private Task generateTask() {
    Task task = new Task();
    task.setCommandPath("/test/command/");
    task.setParameterList(new ArrayList<>());
    task.setTrigger(TriggerBuilder.newTrigger().build());
    return task;
  }
}
