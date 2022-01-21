package com.almis.awe.scheduler.filechecker;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.scheduler.bean.file.File;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.enums.ServerConnectionType;
import lombok.extern.slf4j.Slf4j;

/**
 * @author dfuentes
 */
@Slf4j
public class FileChecker extends ServiceConfig {

  // Autowired services
  private final FTPFileChecker ftpFileChecker;
  private final FolderFileChecker folderFileChecker;

  /**
   * Autowired constructor
   *
   * @param ftpFileChecker    FTP File checker
   * @param folderFileChecker Folder file checker
   */
  public FileChecker(FTPFileChecker ftpFileChecker, FolderFileChecker folderFileChecker) {
    this.ftpFileChecker = ftpFileChecker;
    this.folderFileChecker = folderFileChecker;
  }

  /**
   * Checks a file if needed and returns if it has changed or not
   *
   * @param task
   * @return boolean
   */
  public String checkFile(Task task) {
    // true -> modifications | false -> no modifications
    File file = task.getFile();
    if (file == null || file.getFileServerId() == null) {
      return null;
    } else {
      String protocol = file.getServer().getTypeOfConnection();
      switch (ServerConnectionType.valueOf(protocol.toUpperCase())) {
        case FTP:
          return ftpFileChecker.checkForChanges(task);
        case FOLDER:
          return folderFileChecker.checkForChanges(task);
        case HTTP:
        case HTTPS:
        case SSH:
        default:
          return null;
      }
    }
  }
}
