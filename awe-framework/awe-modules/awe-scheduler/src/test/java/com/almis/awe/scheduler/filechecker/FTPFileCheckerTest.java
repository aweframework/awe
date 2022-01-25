package com.almis.awe.scheduler.filechecker;

import com.almis.awe.scheduler.bean.file.File;
import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.dao.FileDAO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.TriggerBuilder;

import javax.naming.NamingException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

/**
 * Class used for testing FTPFileChecker class
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class FTPFileCheckerTest {

  @InjectMocks
  private FTPFileChecker fileChecker;

  @Mock
  private FileDAO fileDAO;

  @Mock
  private FTPClient ftpClient;

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
   *
   * @throws NamingException Test error
   */
  @Test
  void checkForChangesNoFiles() throws Exception {
    // Mock
    given(ftpClient.listFiles(anyString())).willReturn(new FTPFile[0]);
    Task task = generateTask("HTTPS");

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
    FTPFile ftpFile = new FTPFile();
    ftpFile.setName("test.txt");
    ftpFile.setTimestamp(Calendar.getInstance());
    FTPFile[] ftpFiles = new FTPFile[1];
    ftpFiles[0] = ftpFile;
    given(ftpClient.listFiles(anyString())).willReturn(ftpFiles);
    Task task = generateTask("FTP");

    // Call
    String changedFile = fileChecker.checkForChanges(task);

    // Verify
    assertEquals("test.txt", changedFile);
  }

  /**
   * Check triggers contains calendars without calendar list
   *
   * @throws NamingException Test error
   */
  @Test
  void checkForChangesWithFilesNoMatch() throws Exception {
    // Mock
    FTPFile ftpFile = new FTPFile();
    ftpFile.setName("test.pom");
    ftpFile.setTimestamp(Calendar.getInstance());
    FTPFile[] ftpFiles = new FTPFile[1];
    ftpFiles[0] = ftpFile;
    given(ftpClient.listFiles(anyString())).willReturn(ftpFiles);
    Task task = generateTask("HTTP");

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
    FTPFile ftpFile = new FTPFile();
    ftpFile.setName("test.txt");
    ftpFile.setTimestamp(Calendar.getInstance());
    FTPFile[] ftpFiles = new FTPFile[1];
    ftpFiles[0] = ftpFile;
    given(ftpClient.listFiles(anyString())).willReturn(ftpFiles);
    Task task = generateTask("SSH");
    addFileModifications(task);

    // Call
    String changedFile = fileChecker.checkForChanges(task);

    // Verify
    assertEquals("test.txt", changedFile);
  }

  /**
   * Check triggers contains calendars without calendar list
   *
   * @throws NamingException Test error
   */
  @Test
  void checkConnectionWithFileServerUser() throws Exception {
    // Mock
    doNothing().when(fileDAO).addModification(any(Task.class), anyString(), any(Date.class), anyBoolean());
    FTPFile ftpFile = new FTPFile();
    ftpFile.setName("test.txt");
    ftpFile.setTimestamp(Calendar.getInstance());
    FTPFile[] ftpFiles = new FTPFile[1];
    ftpFiles[0] = ftpFile;
    given(ftpClient.listFiles(anyString())).willReturn(ftpFiles);
    Task task = generateTask("SSH");
    task.getFile().setFileServerUser("test");
    addFileModifications(task);

    // Call
    String changedFile = fileChecker.checkForChanges(task);

    // Verify
    assertEquals("test.txt", changedFile);
  }

  /**
   * Check triggers contains calendars without calendar list
   *
   * @throws NamingException Test error
   */
  @Test
  void checkConnectionWithEmptyFileServerUser() throws Exception {
    // Mock
    FTPFile ftpFile = new FTPFile();
    ftpFile.setName("test.txt");
    ftpFile.setTimestamp(Calendar.getInstance());
    FTPFile[] ftpFiles = new FTPFile[1];
    ftpFiles[0] = ftpFile;
    given(ftpClient.listFiles(anyString())).willReturn(ftpFiles);
    Task task = generateTask("SSH");
    task.getFile().setFileServerUser("");
    addFileModifications(task);

    // Call
    String changedFile = fileChecker.checkForChanges(task);

    // Verify
    assertEquals("test.txt", changedFile);
  }

  private Task generateTask(String type) {
    Server server = new Server();
    server.setName("tutu");
    server.setHost("127.0.0.1");
    server.setActive(true);
    server.setTypeOfConnection(type);

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
