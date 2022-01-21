package com.almis.awe.scheduler.filechecker;

import com.almis.awe.scheduler.bean.file.File;
import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.dao.FileDAO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.TriggerBuilder;

import javax.naming.NamingException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

/**
 * Class used for testing FolderFileChecker
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class FolderFileCheckerTest {

  @InjectMocks
  private FolderFileChecker fileChecker;

  @Mock
  private FileDAO fileDAO;

  @Mock
  private FileClient fileClient;

  /**
   * Test context loaded
   */
  @Test
  void contextLoads() {
    // Check that controller are active
    assertNotNull(fileChecker);
  }

  /**
   * Check triggers contains calendars without calendar list
   */
  @Test
  void checkForChangesNoFiles() {
    // Mock
    given(fileClient.listFiles(anyString(), anyString())).willReturn(new ArrayList<>());
    Task task = generateTask();

    // Call
    String changedFile = fileChecker.checkForChanges(task);

    // Verify
    assertNull(changedFile);
  }

  /**
   * Check triggers contains calendars without calendar list
   *
   * @throws NamingException Test error
   */
  @Test
  void checkForChangesWithFiles() throws Exception {
    // Mock
    doNothing().when(fileDAO).addModification(any(Task.class), anyString(), any(Date.class), anyBoolean());
    java.io.File file = java.io.File.createTempFile("test", ".txt");
    file.deleteOnExit();
    List<java.io.File> files = new ArrayList<>();
    files.add(file);
    given(fileClient.listFiles(anyString(), anyString())).willReturn(files);
    Task task = generateTask();

    // Call
    String changedFile = fileChecker.checkForChanges(task);

    // Verify
    assertEquals(file.getName(), changedFile);
  }

  /**
   * Check triggers contains calendars without calendar list
   *
   * @throws NamingException Test error
   */
  @Test
  void checkForChangesWithFilesNoMatch() throws Exception {
    // Mock
    java.io.File file = java.io.File.createTempFile("test", ".pom");
    file.deleteOnExit();
    List<java.io.File> files = new ArrayList<>();
    files.add(file);
    given(fileClient.listFiles(anyString(), anyString())).willReturn(files);
    Task task = generateTask();

    // Call
    String changedFile = fileChecker.checkForChanges(task);

    // Verify
    assertNull(changedFile);
  }

  /**
   * Check triggers contains calendars without calendar list
   *
   * @throws NamingException Test error
   */
  @Test
  void checkForChangesWithFilesAndFileModifications() throws Exception {
    // Mock
    java.io.File file = java.io.File.createTempFile("test", ".txt");
    file.deleteOnExit();
    List<java.io.File> files = new ArrayList<>();
    files.add(file);
    given(fileClient.listFiles(anyString(), anyString())).willReturn(files);
    Task task = generateTask();
    addFileModifications(task);

    // Call
    String changedFile = fileChecker.checkForChanges(task);

    // Verify
    assertEquals(file.getName(), changedFile);
  }

  private Task generateTask() {
    Server server = new Server();
    server.setName("tutu");
    server.setHost("127.0.0.1");
    server.setActive(true);
    server.setTypeOfConnection("FTP");

    Task task = new Task();
    task.setTrigger(TriggerBuilder.newTrigger().build());
    task.setFile(new File()
      .setFilePattern(".*\\.txt")
      .setFilePath("test/")
      .setServer(server));
    return task;
  }

  private void addFileModifications(Task task) {
    // Define old calendar modifications
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.HOUR, -1);

    Map<String, Date> fileModifications = new HashMap<>();
    fileModifications.put("test/test.txt", calendar.getTime());

    task.getFile().setFileModifications(fileModifications);
  }
}
