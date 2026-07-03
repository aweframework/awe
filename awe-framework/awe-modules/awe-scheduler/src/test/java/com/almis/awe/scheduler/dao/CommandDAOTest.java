package com.almis.awe.scheduler.dao;

import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.executor.CommandExecutor;
import com.almis.awe.scheduler.executor.CommandExecutorResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.TriggerBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Class used for testing CommandDAO: it is a thin delegator to
 * CommandExecutorResolver.resolve(task).execute(...). Execution-path
 * behaviour (local process, SSH) is covered by LocalCommandExecutorTest and
 * SshCommandExecutorTest.
 */
@ExtendWith(MockitoExtension.class)
class CommandDAOTest {

  @InjectMocks
  private CommandDAO commandDAO;

  @Mock
  private CommandExecutorResolver commandExecutorResolver;

  @Mock
  private CommandExecutor commandExecutor;

  @Test
  void delegatesExecutionToResolvedExecutor() {
    Task task = generateTask();
    String[] envp = new String[0];
    given(commandExecutorResolver.resolve(task)).willReturn(commandExecutor);
    given(commandExecutor.execute(task, envp, 1000)).willReturn(0);

    Integer exitCode = commandDAO.runCommand(task, envp, 1000);

    assertEquals(0, exitCode);
    verify(commandExecutorResolver).resolve(task);
    verify(commandExecutor).execute(same(task), eq(envp), eq(1000L));
  }

  @Test
  void propagatesNonZeroExitCodeFromResolvedExecutor() {
    Task task = generateTask();
    String[] envp = new String[0];
    given(commandExecutorResolver.resolve(task)).willReturn(commandExecutor);
    given(commandExecutor.execute(task, envp, 1000)).willReturn(1);

    Integer exitCode = commandDAO.runCommand(task, envp, 1000);

    assertEquals(1, exitCode);
  }

  private Task generateTask() {
    Task task = new Task();
    task.setCommandPath("/test/command/");
    task.setTrigger(TriggerBuilder.newTrigger().build());
    return task;
  }
}
