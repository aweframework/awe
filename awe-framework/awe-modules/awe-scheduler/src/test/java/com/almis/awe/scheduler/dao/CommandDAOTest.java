package com.almis.awe.scheduler.dao;

import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.TriggerBuilder;

import javax.naming.NamingException;
import java.util.ArrayList;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Class used for testing CommandDao class
 */
@Log4j2
@ExtendWith(MockitoExtension.class)
class CommandDAOTest {

  @InjectMocks
  private CommandDAO commandDAO;

  @Mock
  private Runtime runtime;

  @Mock
  private Process process;

  /**
   * Check triggers contains calendars without calendar list
   *
   * @throws NamingException Test error
   */
  @Test
  void runExeCommand() throws Exception {
    // Mock
    given(runtime.exec(ArgumentMatchers.anyString(), ArgumentMatchers.any())).willReturn(process);
    given(process.getErrorStream()).willReturn(IOUtils.toInputStream("error stream data", "UTF-8"));
    given(process.getInputStream()).willReturn(IOUtils.toInputStream("output stream data", "UTF-8"));

    Task task = generateTask();
    task.setAction("test.exe");

    // Run action
    commandDAO.runCommand(task, new String[0], 1000);

    // Check that controller are active
    verify(runtime, Mockito.times(1)).exec(ArgumentMatchers.anyString(), ArgumentMatchers.any());
  }

  /**
   * Check triggers contains calendars without calendar list
   *
   * @throws NamingException Test error
   */
  @Test
  void runCmdCommand() throws Exception {
    // Mock
    given(runtime.exec(ArgumentMatchers.anyString(), ArgumentMatchers.any())).willReturn(process);
    Task task = generateTask();
    task.setAction("test.cmd");
    task.getParameterList().add(new TaskParameter().setValue("tutu"));

    // Run action
    commandDAO.runCommand(task, new String[]{}, 1000);

    // Check mock called
    verify(runtime, Mockito.times(1)).exec(ArgumentMatchers.anyString(), ArgumentMatchers.any());
  }

  private Task generateTask() {
    Task task = new Task();
    task.setCommandPath("/test/command/");
    task.setParameterList(new ArrayList<>());
    task.setTrigger(TriggerBuilder.newTrigger().build());
    return task;
  }
}
