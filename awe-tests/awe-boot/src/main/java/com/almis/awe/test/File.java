package com.almis.awe.test;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.file.FileUtil;
import com.almis.awe.service.FileService;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * File test class
 *
 * @author pgarcia
 */
@Service
public class File extends ServiceConfig {

  private final FileService fileService;

  /**
   * File constructor
   * @param fileService File service
   */
  public File(FileService fileService) {
    this.fileService = fileService;
  }

  /**
   * Given a file identifier, download a file
   *
   * @param fileStringEncoded File string encoded
   * @return Service data
   * @throws AWException error retrieving file
   */
  public ServiceData downloadFile(String fileStringEncoded) throws AWException {
    ServiceData serviceData = new ServiceData();
    String fullPath = null;
    FileData fileData = FileUtil.stringToFileData(fileStringEncoded);

    try {
      fullPath = fileService.getFullPath(fileData, false);
      FileInputStream file = new FileInputStream(fullPath + fileData.getFileName());
      fileData.setFileStream(file);
    } catch (FileNotFoundException exc) {
      throw new AWException(getLocale("ERROR_TITLE_READING_FILE"), getLocale("Error reading file {0} from {1}", fileData.getFileName(), fullPath), exc);
    }

    // Set variables
    serviceData.setData(fileData);
    return serviceData;
  }

  /**
   * Given a file identifier, retrieve file information
   *
   * @param fileIdEncoded File data
   * @return File information
   * @throws AWException Error generating file info
   */
  public ServiceData getFileInfo(String fileIdEncoded) throws AWException {
    ServiceData serviceData = new ServiceData();
    FileData fileData = FileUtil.stringToFileData(fileIdEncoded);

    // Set variables
    String[] out = {fileIdEncoded, fileData.getFileName()};

    // Set variables
    return serviceData.setData(out);
  }
}
