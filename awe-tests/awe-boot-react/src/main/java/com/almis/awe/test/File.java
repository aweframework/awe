package com.almis.awe.test;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.Global;
import com.almis.awe.model.service.DataListService;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.file.FileUtil;
import com.almis.awe.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collections;

/**
 * File test class
 *
 * @author pgarcia
 */
@Service
public class File extends ServiceConfig {

  @Autowired
  private FileService fileService;

  @Autowired
  private DataListService dataListService;

  /**
   * Given a file identifier, download a file
   * @param filedata File data
   * @return Service data
   * @throws AWException error retrieving file
   */
  public ServiceData downloadFile(String filedata) throws AWException {
    ServiceData serviceData = new ServiceData();
    String fullPath = null;
    FileData fileData = FileUtil.stringToFileData(filedata);

    try {
      fullPath = fileService.getFullPath(fileData, false);

      FileInputStream file = new FileInputStream(fullPath + fileData.getFileName());
      fileData.setFileStream(file);
    } catch (FileNotFoundException exc) {
      throw new AWException(getElements().getLocale("ERROR_TITLE_READING_FILE"),
              getElements().getLocale("Error reading file {0} from {1}", fileData.getFileName(), fullPath), exc);
    }

    // Set variables
    serviceData.setData(fileData);
    return serviceData;
  }

  /**
   * Given a file identifier, retrieve file information
   * @param filedata File data
   * @return File information
   * @throws AWException Error generating file info
   */
  public ServiceData getFileInfo(String filedata) throws AWException {
    ServiceData serviceData = new ServiceData();
    FileData fileData = FileUtil.stringToFileData(filedata);

    // Set variables
    return serviceData.setDataList(DataListUtil.fromBeanList(Collections.singletonList(new Global().setValue(filedata).setLabel(fileData.getFileName()))));
  }
}
