package com.almis.awe.test.model;

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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * File test class
 *
 * @author pgarcia
 */
@Service
public class File extends ServiceConfig {

  private static final String GRID_FILES = "download/";
  private static final String GRID_FILE_TYPE = "text/plain";
  private static final Pattern GRID_FILE_NAME = Pattern.compile("[\\w-]+\\.txt");

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

  /**
   * Download one of the files of the test grids (classpath folder "download"). Only a plain file name is accepted, so
   * the value of the cell cannot reach any other resource
   *
   * @param fileName File name
   * @return Service data
   * @throws AWException The file is not one of the grid files
   */
  public ServiceData downloadGridFile(String fileName) throws AWException {
    if (fileName == null || !GRID_FILE_NAME.matcher(fileName).matches()) {
      throw new AWException("Error reading file", "Not a file of the test grids: " + fileName);
    }
    ClassPathResource resource = new ClassPathResource(GRID_FILES + fileName);
    try {
      FileData fileData = new FileData(fileName, resource.contentLength(), GRID_FILE_TYPE);
      fileData.setFileStream(resource.getInputStream());
      return new ServiceData().setData(fileData);
    } catch (IOException exc) {
      throw new AWException("Error reading file", "Error reading the grid file " + fileName, exc);
    }
  }
}
