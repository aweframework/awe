package com.almis.awe.scheduler.executor;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.dao.ServerDAO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.TriggerBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;

/**
 * Class used for testing CommandExecutorResolver: it resolves the CommandExecutor
 * strategy (local or remote) for a given task, from Task.serverId + Server.typeOfConnection.
 */
@ExtendWith(MockitoExtension.class)
class CommandExecutorResolverTest {

  @InjectMocks
  private CommandExecutorResolver commandExecutorResolver;

  @Mock
  private LocalCommandExecutor localCommandExecutor;

  @Mock
  private SshCommandExecutor sshCommandExecutor;

  @Mock
  private ServerDAO serverDAO;

  @Test
  void resolvesToLocalExecutorWhenTaskHasNoServer() {
    Task task = generateTask(null);

    CommandExecutor resolved = commandExecutorResolver.resolve(task);

    assertSame(localCommandExecutor, resolved);
  }

  @Test
  void resolvesToSshExecutorWhenServerTypeIsSsh() throws AWException {
    Task task = generateTask(1);
    given(serverDAO.findServer(1)).willReturn(activeServer("ssh"));

    CommandExecutor resolved = commandExecutorResolver.resolve(task);

    assertSame(sshCommandExecutor, resolved);
  }

  @Test
  void resolvesToFailFastGuardWhenServerTypeIsUnsupported() throws AWException {
    Task task = generateTask(1);
    given(serverDAO.findServer(1)).willReturn(activeServer("ftp"));

    CommandExecutor resolved = commandExecutorResolver.resolve(task);

    assertGuardFailsFast(resolved, task);
  }

  @Test
  void resolvesToFailFastGuardWhenServerNotFound() throws AWException {
    Task task = generateTask(1);
    given(serverDAO.findServer(1)).willReturn(null);

    CommandExecutor resolved = commandExecutorResolver.resolve(task);

    assertGuardFailsFast(resolved, task);
  }

  @Test
  void resolvesToFailFastGuardWhenServerIsInactive() throws AWException {
    Task task = generateTask(1);
    Server server = activeServer("ssh");
    server.setActive(false);
    given(serverDAO.findServer(1)).willReturn(server);

    CommandExecutor resolved = commandExecutorResolver.resolve(task);

    assertGuardFailsFast(resolved, task);
  }

  @Test
  void resolvesToFailFastGuardWhenServerLookupThrows() throws AWException {
    Task task = generateTask(1);
    given(serverDAO.findServer(1)).willThrow(new AWException("boom"));

    CommandExecutor resolved = commandExecutorResolver.resolve(task);

    assertGuardFailsFast(resolved, task);
  }

  private void assertGuardFailsFast(CommandExecutor resolved, Task task) {
    Integer exitCode = resolved.execute(task, new String[0], 10);
    assertEquals(1, exitCode);
  }

  private Server activeServer(String typeOfConnection) {
    Server server = new Server();
    server.setTypeOfConnection(typeOfConnection);
    server.setActive(true);
    return server;
  }

  private Task generateTask(Integer serverId) {
    Task task = new Task();
    task.setServerId(serverId);
    task.setTrigger(TriggerBuilder.newTrigger().build());
    return task;
  }
}
