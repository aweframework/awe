package com.almis.awe.developer.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Manage the development paths
 *
 * @author agomez
 */
public class PathService extends ServiceConfig {

  private static final String ERROR_TITLE_UPDATE_WRK_DIR = "ERROR_TITLE_UPDATE_WRK_DIR";
  private static final String ERROR_MESSAGE_UPDATE_WRK_DIR = "ERROR_MESSAGE_UPDATE_WRK_DIR";

  private final String developerPath;
  private final String developerPathFile;
  private final String developerPathProperty;

  /**
   * Autowired constructor
   *
   * @param developerPath         Developer path
   * @param developerPathFile     Developer path file
   * @param developerPathProperty Developer path property
   */
  public PathService(String developerPath, String developerPathFile, String developerPathProperty) {
    this.developerPath = developerPath;
    this.developerPathFile = developerPathFile;
    this.developerPathProperty = developerPathProperty;
  }

  /**
   * Retrieves property's value for final path
   *
   * @return Path
   */
  private Path getFinalPath() {
    return Paths.get(developerPath,developerPathFile);
  }

  /**
   * Check if path file properties exists
   *
   * @return Service data
   * @throws AWException Error checking path
   */
  public ServiceData checkPath() throws AWException {
    ServiceData serviceData = new ServiceData();
    Properties properties = getPropertiesFile();
    String path = properties.getProperty(developerPathProperty);

    // If path is not valid, set it to blank
    if (path == null) {
      updatePath("");
    }

    DataList dataList = new DataList();
    DataListUtil.addColumnWithOneRow(dataList, "paths", path);
    dataList.setRecords(dataList.getRows().size());

    return serviceData.setDataList(dataList);
  }

  /**
   * Check if path file properties exists
   *
   * @return Path
   * @throws AWException Error retrieving path
   */
  public Properties getPropertiesFile() throws AWException {
    Properties properties;

    // Check if properties file exists
    checkIfFileExists();

    // Retrieve properties file
    try (FileInputStream in = new FileInputStream(getFinalPath().toFile())) {
      properties = new Properties();
      properties.load(in);
    } catch (IOException exc) {
      throw new AWException(getLocale(ERROR_TITLE_UPDATE_WRK_DIR),
        getLocale(ERROR_MESSAGE_UPDATE_WRK_DIR), exc);
    }
    return properties;
  }

  /**
   * Check if properties file exists, if not, create it
   */
  private void checkIfFileExists() throws AWException {
    File propertiesFile = getFinalPath().toFile();
    if (!propertiesFile.exists()) {
      try {
        Paths.get(developerPath).toFile().mkdirs();
        if (!propertiesFile.createNewFile()) {
          throw new AWException(getLocale(ERROR_TITLE_UPDATE_WRK_DIR),
            getLocale(ERROR_MESSAGE_UPDATE_WRK_DIR));
        }
      } catch (IOException exc) {
        throw new AWException(getLocale(ERROR_TITLE_UPDATE_WRK_DIR),
          getLocale(ERROR_MESSAGE_UPDATE_WRK_DIR));
      }
    }
  }

  /**
   * Update path in properties file
   *
   * @param path Path to update
   * @throws AWException Error updating path
   */
  private void updatePath(String path) throws AWException {
    Properties properties = getPropertiesFile();
    properties.setProperty(developerPathProperty, path);

    try (FileOutputStream out = new FileOutputStream(getFinalPath().toFile())) {
      properties.store(out, null);
    } catch (IOException exc) {
      throw new AWException(getLocale(ERROR_TITLE_UPDATE_WRK_DIR),
        getLocale("ERROR_MESSAGE_UPDATE_WRK_DIR2"), exc);
    }
  }

  /**
   * Check if path file properties exists
   *
   * @return Path
   * @throws AWException Error retrieving path
   */
  public String getPath() throws AWException {
    return getPropertiesFile().getProperty(developerPathProperty);
  }

  /**
   * Updates user working directory
   *
   * @param path Path
   * @return Service data
   * @throws AWException Error setting path
   */
  public ServiceData setPath(String path) throws AWException {
    ServiceData serviceData = new ServiceData();
    // Update path
    updatePath(path);
    serviceData.setTitle(getLocale("CONFIRM_TITLE_UPDATE_WRK_DIR"))
      .setMessage(getLocale("CONFIRM_MESSAGE_UPDATE_WRK_DIR"));
    return serviceData;
  }
}
