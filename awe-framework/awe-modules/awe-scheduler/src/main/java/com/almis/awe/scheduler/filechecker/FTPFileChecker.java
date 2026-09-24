package com.almis.awe.scheduler.filechecker;

import com.almis.awe.scheduler.bean.file.File;
import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.dao.FileDAO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author dfuentes
 */
@Slf4j
public class FTPFileChecker extends Connector {

  // Autowired services
  private final FTPClient ftpClient;

  /**
   * Autowired constructor
   *
   * @param fileDAO   File DAO
   * @param ftpClient FTP client
   */
  public FTPFileChecker(FileDAO fileDAO, FTPClient ftpClient) {
    super(fileDAO);
    this.ftpClient = ftpClient;
  }

  @Override
  protected List<FTPFile> connectAndGetFiles(Task task) {
    File file = task.getFile();
    Server server = file.getServer();
    try {
      // Connect to ftp server
      ftpClient.connect(server.getHost(), server.getPort());
      if (server.getUser() != null && !server.getUser().isEmpty()) {
        ftpClient.login(server.getUser(), server.getPassword());
      }

      // Notify correctly connected
      log.debug("[FTP Connection] Connected to the server by FTP: {}", server.getName() + "(" + server.getServerId() + ")");

      // lists files and directories in the current working directory
      return new ArrayList<>(Arrays.asList(ftpClient.listFiles(file.getFilePath())));
    } catch (IOException exc) {
      log.error("[FTP Connection] Error connecting to the server by FTP: {}", server.getName() + "(" + server.getServerId() + ")", exc);
    } finally {
      try {
        // Always logout and disconnect from the FTP server
        ftpClient.logout();
        ftpClient.disconnect();

        // Notify correctly logged out
        log.debug("[FTP Connection] Logged out from FTP server: {}", server.getName() + "(" + server.getServerId()
          + ")");
      } catch (IOException exc) {
        log.error("[FTP Connection] FTP Connection error: {}", server.getName() + "(" + server.getServerId() + ")", exc);
      }
    }
    return new ArrayList<>();
  }

  @Override
  public String checkForChanges(Task task) {
    String changedFile = null;
    File file = task.getFile();

    // Iterate on every file and check for changes
    List<FTPFile> serverFiles = connectAndGetFiles(task);
    for (FTPFile ftpFile : serverFiles) {
      if (checkFileModifications(task, ftpFile.getName(), resolveRemotePath(file.getFilePath(), ftpFile.getName()), ftpFile.getTimestamp().getTime())) {
        changedFile = ftpFile.getName();
      }
    }

    // return if there are changes
    return changedFile;
  }
}
