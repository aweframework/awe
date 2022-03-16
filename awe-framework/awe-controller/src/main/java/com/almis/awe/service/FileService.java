package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.util.data.StringUtil;
import com.almis.awe.model.util.file.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Manage application initialization
 */
@Slf4j
public class FileService extends ServiceConfig {

  // Autowired services
  private final BroadcastService broadcastService;
  private final AweRequest request;
  private final BaseConfigProperties baseConfigProperties;
  private final EncodeService encodeService;

  private static final String ERROR_TITLE_FILE_READING_ERROR = "ERROR_TITLE_FILE_READING_ERROR";
  private static final String ERROR_MESSAGE_FILE_READING_ERROR = "ERROR_MESSAGE_FILE_READING_ERROR";

  /**
   * Autowired constructor
   *
   * @param broadcastService     Broadcaster
   * @param request              Request
   * @param baseConfigProperties Base configuration properties
   * @param encodeService        Encode service
   */
  public FileService(BroadcastService broadcastService, AweRequest request, BaseConfigProperties baseConfigProperties, EncodeService encodeService) {
    this.broadcastService = broadcastService;
    this.request = request;
    this.baseConfigProperties = baseConfigProperties;
    this.encodeService = encodeService;
  }

  /**
   * Retrieve a text file content
   *
   * @param path        File path
   * @param contentType Content type
   * @return Text file
   * @throws AWException Error retrieving text file
   */
  public ResponseEntity<String> getTextFile(String path, String contentType) throws AWException {
    String fileContent;

    try {
      fileContent = new String(Files.readAllBytes(Paths.get(path)));
    } catch (Exception exc) {
      throw new AWException(getLocale(ERROR_TITLE_FILE_READING_ERROR), getLocale(ERROR_MESSAGE_FILE_READING_ERROR, path), exc);
    }

    // Generate text file headers
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.parseMediaType(contentType));

    // Retrieve entity
    return new ResponseEntity<>(fileContent, responseHeaders, HttpStatus.CREATED);
  }

  /**
   * Retrieve a log file content
   *
   * @return Service data with log value
   * @throws AWException Error retrieving log file
   */
  public ServiceData getLogFile() throws AWException {
    ServiceData serviceData = new ServiceData();
    List<String> content = new ArrayList<>();
    // Get path and offset
    String path = encodeService.decodeSymmetric(getRequest().getParameterAsString("path"));
    int offset = getRequest().getParameter("offset").asInt();

    if (Paths.get(path).toFile().exists()) {
      try (FileInputStream file = new FileInputStream(path);
           InputStreamReader fileReader = new InputStreamReader(file, StandardCharsets.UTF_8);
           BufferedReader bufferedReader = new BufferedReader(fileReader)) {
        // Read file
        int line = 0;
        String lineString;

        while ((lineString = bufferedReader.readLine()) != null) {
          if (offset <= line) {
            content.add(lineString);
          }
          line++;
        }

        // Generate output
        serviceData.addVariable("LOG_CONTENT", new CellData(content));
      } catch (Exception exc) {
        throw new AWException(getLocale(ERROR_TITLE_FILE_READING_ERROR), getLocale(ERROR_MESSAGE_FILE_READING_ERROR, path), exc);
      }
    }

    // Retrieve entity
    return serviceData;
  }

  /**
   * Retrieve a text file content
   *
   * @param path        File path
   * @param contentType Content type
   * @return Text file
   * @throws AWException Error retrieving text file
   */
  public ResponseEntity<FileSystemResource> getFileStream(String path, String contentType) throws AWException {
    HttpHeaders headers = new HttpHeaders();

    try {
      // Generate text file headers
      FileSystemResource resource = new FileSystemResource(path);
      headers.setContentType(MediaType.parseMediaType(contentType));
      headers.setContentLength(resource.contentLength());
      headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=\"" + resource.getFilename() + "\"");

      // Retrieve entity
      return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    } catch (Exception exc) {
      throw new AWException(getLocale(ERROR_TITLE_FILE_READING_ERROR), getLocale(ERROR_MESSAGE_FILE_READING_ERROR, path), exc);
    }
  }

  /**
   * Retrieve a file content
   *
   * @param fileData File data
   * @return Text file
   * @throws AWException Error retrieving text file
   */
  public ResponseEntity<FileSystemResource> getFileStream(FileData fileData) throws AWException {
    String filePath = getFullPath(fileData, false) + fileData.getFileName();
    return getFileStream(filePath, fileData.getMimeType());
  }

  /**
   * Retrieve a text file content
   *
   * @param fileData           File data
   * @param downloadIdentifier Download identifier
   * @return File to download
   * @throws AWException Error retrieving text file
   */
  public ResponseEntity<byte[]> downloadFile(FileData fileData, Integer downloadIdentifier) throws AWException {
    // convert JSON to Employee
    HttpHeaders headers = new HttpHeaders();
    String filePath = getFullPath(fileData, false) + fileData.getFileName();

    try (InputStream fileStream = fileData.getFileStream() != null ? fileData.getFileStream() : new FileInputStream(filePath);
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      // Generate text file headers
      headers.setContentType(MediaType.parseMediaType(fileData.getMimeType()));
      headers.setContentLength(fileData.getFileSize());
      headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"" + fileData.getFileName() + "\"");
      headers.add("Content-Transfer-Encoding", "binary");
      headers.add("Filename", fileData.getFileName());

      // Publish file downloaded
      ClientAction fileDownloadedAction = new ClientAction("file-downloaded/" + downloadIdentifier);
      fileDownloadedAction.setAsync(true);
      broadcastService.broadcastMessageToUID(request.getToken(), fileDownloadedAction);

      // Generate the file stream
      IOUtils.copy(fileStream, outputStream);

      // Return the file stream
      return new ResponseEntity<>(outputStream.toByteArray(), headers, HttpStatus.OK);
    } catch (Exception exc) {
      throw new AWException(getLocale(ERROR_TITLE_FILE_READING_ERROR), getLocale(ERROR_MESSAGE_FILE_READING_ERROR, filePath), exc);
    }
  }

  /**
   * Retrieve a text file content
   *
   * @param file   Uploaded file
   * @param folder Destination folder
   * @return File path
   * @throws AWException Error retrieving text file
   */
  public FileData uploadFile(MultipartFile file, String folder) throws AWException {
    FileData fileData = null;
    try {
      if (!file.isEmpty()) {
        // Store file
        fileData = new FileData(FileUtil.sanitizeFileName(file.getOriginalFilename()), file.getSize(), FileUtil.extractContentType(file), folder);
        fileData.setBasePath(baseConfigProperties.getComponent().getUploadFilePath());

        // Generate file path
        String fullPath = getFullPath(fileData, true);

        // Save file on upload path
        Path destinationFile = Paths.get(fullPath, fileData.getFileName());

        // Log saving file
        log.debug("Saving file on {}", destinationFile);

        // Store file
        try (InputStream inputStream = file.getInputStream()) {
          Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    } catch (Exception exc) {
      throw new AWException(getLocale("ERROR_TITLE_SAVING_ITEM"),
        getLocale("ERROR_MESSAGE_SAVING_ITEM"), exc);
    }

    return fileData;
  }

  /**
   * Deletes an uploaded file
   *
   * @return Service data
   * @throws AWException Error deleting file
   */
  public ServiceData deleteFile() throws AWException {
    String fileName = getRequest().getParameterAsString("filename");
    FileData fileData = FileUtil.stringToFileData(fileName);
    return deleteFile(fileData);
  }

  /**
   * Deletes an uploaded file
   *
   * @param fileData File data
   * @return Service data
   * @throws AWException Error deleting file
   */
  public ServiceData deleteFile(FileData fileData) throws AWException {
    ServiceData serviceData = new ServiceData();
    try {
      // Get file data
      String fullPath = getFullPath(fileData, false);

      // Check if file exists
      File dest = new File(fullPath + fileData.getFileName());
      if (!dest.exists()) {
        throw new AWException(getLocale("ERROR_TITLE_NONEXISTENT_FILE"),
          getLocale("ERROR_MESSAGE_NONEXISTENT_FILE", fileData.getFileName()));
      }

      // Remove file from path
      Files.delete(dest.toPath());
    } catch (IOException exc) {
      throw new AWException(getLocale("ERROR_TITLE_FILE_DELETE"), getLocale("ERROR_MESSAGE_FILE_DELETE"), exc);
    }
    return serviceData;
  }

  /**
   * Given a file identifier, retrieve file information
   *
   * @return File information
   * @throws AWException Error generating file info
   */
  public ServiceData getFileInfo() throws AWException {
    ServiceData serviceData = new ServiceData();
    String fileName = getRequest().getParameterAsString("filename");
    FileData fileData = FileUtil.stringToFileData(fileName);

    // Set variables
    serviceData
      .addVariable(AweConstants.ACTION_FILE_NAME, fileData.getFileName())
      .addVariable(AweConstants.ACTION_FILE_SIZE, fileData.getFileSize().toString())
      .addVariable(AweConstants.ACTION_FILE_TYPE, fileData.getMimeType())
      .addVariable(AweConstants.ACTION_FILE_PATH, fileName);
    return serviceData;
  }

  /**
   * Retrieves a previously uploaded file from upload path
   * @param fileData File data
   * @param create Create path
   * @return Uploaded file path
   */
  public String getFullPath(FileData fileData, boolean create) {
    // Variable definition
    String relativePath = fileData.getRelativePath();
    Long size = fileData.getFileSize();

    // Calculate max elements per folder
    if (relativePath == null && create) {
      relativePath = "tmp" + (size % baseConfigProperties.getComponent().getUploadMaxFilesFolder());
    } else if (relativePath == null) {
      relativePath = "";
    }

    // Calculate upload path
    String absolutePath = StringUtil.getAbsolutePath(fileData.getBasePath() + relativePath + AweConstants.FILE_SEPARATOR, baseConfigProperties.getPaths().getBase());

    // Generate folder (if not null)
    if (create) {
      fileData.setRelativePath(relativePath);
      new File(absolutePath).mkdirs();
    }

    return absolutePath;
  }
}
