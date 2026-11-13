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
import com.almis.awe.service.observation.FileTransferObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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

  // Observer of the uploads and downloads (metrics...). Optional: without one nothing is recorded
  private FileTransferObserver fileTransferObserver = FileTransferObserver.NONE;

  // Log directories besides the log manager path the log viewer may read (scheduler execution logs...)
  private List<LogDirectoryProvider> logDirectoryProviders = List.of();

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
   * Set the observer of the uploads and downloads
   *
   * @param fileTransferObserver File transfer observer (optional, defined by the application when it needs one)
   */
  @Autowired(required = false)
  public void setFileTransferObserver(FileTransferObserver fileTransferObserver) {
    this.fileTransferObserver = fileTransferObserver == null ? FileTransferObserver.NONE : fileTransferObserver;
  }

  /**
   * Set the log directories the log viewer may read besides the log manager path
   *
   * @param logDirectoryProviders Log directory providers (optional, defined by the modules that write their own logs)
   */
  @Autowired(required = false)
  public void setLogDirectoryProviders(List<LogDirectoryProvider> logDirectoryProviders) {
    this.logDirectoryProviders = logDirectoryProviders == null ? List.of() : List.copyOf(logDirectoryProviders);
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
    checkWithinLogDirectories(path);
    String fileContent;

    try {
      fileContent = new String(Files.readAllBytes(Path.of(path)));
    } catch (Exception exc) {
      throw new AWException(getLocale(ERROR_TITLE_FILE_READING_ERROR), getLocale(ERROR_MESSAGE_FILE_READING_ERROR), exc);
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
    int offset = getRequest().getParameter("offset").asInt(0);

    // Only the files under the log directories can be read: the path comes from the client
    checkWithinLogDirectories(path);

    if (Path.of(path).toFile().exists()) {
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
        throw new AWException(getLocale(ERROR_TITLE_FILE_READING_ERROR), getLocale(ERROR_MESSAGE_FILE_READING_ERROR), exc);
      }
    }

    // Retrieve entity
    return serviceData;
  }

  /**
   * Reject a path that is not a file under one of the log directories. The error does not show the path
   *
   * @param path File path
   * @throws AWException The path is not under a log directory
   */
  private void checkWithinLogDirectories(String path) throws AWException {
    if (!isWithinLogDirectories(path)) {
      log.warn("File request rejected: the path is not under a log directory");
      throw new AWException(getLocale(ERROR_TITLE_FILE_READING_ERROR), getLocale(ERROR_MESSAGE_FILE_READING_ERROR));
    }
  }

  /**
   * Check whether a path is a file under one of the log directories: the log manager path and the directories of the
   * {@link LogDirectoryProvider} beans. The path is normalized ({@code ..} segments resolved) and, when the file
   * exists, its real path (symbolic links followed) has to stay under the real path of the directory too.
   *
   * @param path File path
   * @return Path is under a log directory
   */
  public boolean isWithinLogDirectories(String path) {
    if (path == null || path.isBlank()) {
      return false;
    }
    try {
      Path file = Path.of(path).toAbsolutePath().normalize();
      for (Path directory : getLogDirectories()) {
        if (file.startsWith(directory) && !file.equals(directory) && resolvesUnder(file, directory)) {
          return true;
        }
      }
    } catch (InvalidPathException | IOException exc) {
      log.debug("Log file path could not be resolved", exc);
    }
    return false;
  }

  /**
   * Retrieve the log directories, absolute and normalized. Blank paths and file system roots are left out
   *
   * @return Log directories
   */
  private List<Path> getLogDirectories() {
    return Stream.concat(
        Stream.of(baseConfigProperties.getLogManagerPath()).filter(path -> path != null && !path.isBlank()).map(Path::of),
        logDirectoryProviders.stream().map(LogDirectoryProvider::getLogDirectory))
      .filter(directory -> directory != null && !directory.toString().isBlank())
      .map(directory -> directory.toAbsolutePath().normalize())
      // A file system root would open every file: never a log directory
      .filter(directory -> directory.getParent() != null)
      .toList();
  }

  /**
   * Check that an existing file stays under the directory once the symbolic links are followed
   *
   * @param file      Normalized file path, lexically under the directory
   * @param directory Normalized directory path
   * @return File resolves under the directory (or does not exist, so there is nothing to read)
   * @throws IOException Error resolving the real paths
   */
  private boolean resolvesUnder(Path file, Path directory) throws IOException {
    if (!Files.exists(file)) {
      return true;
    }
    return file.toRealPath().startsWith(directory.toRealPath());
  }

  /**
   * Retrieve a file stream from a client path. Only the files under the log directories can be read
   *
   * @param path        File path
   * @param contentType Content type
   * @return File stream
   * @throws AWException The path is not under a log directory or the file can not be read
   */
  public ResponseEntity<FileSystemResource> getFileStream(String path, String contentType) throws AWException {
    checkWithinLogDirectories(path);
    return streamFile(path, contentType);
  }

  /**
   * Retrieve a file stream. The caller has resolved the path (uploaded or generated file)
   *
   * @param path        File path
   * @param contentType Content type
   * @return File stream
   * @throws AWException Error reading the file
   */
  private ResponseEntity<FileSystemResource> streamFile(String path, String contentType) throws AWException {
    HttpHeaders headers = new HttpHeaders();

    try {
      // Generate text file headers
      FileSystemResource resource = new FileSystemResource(path);
      headers.setContentType(MediaType.parseMediaType(contentType));
      headers.setContentLength(resource.contentLength());
      headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=\"" + resource.getFilename() + "\"");
      fileTransferObserver.transferred(FileTransferObserver.Direction.DOWNLOAD, resource.contentLength());

      // Retrieve entity
      return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    } catch (Exception exc) {
      throw new AWException(getLocale(ERROR_TITLE_FILE_READING_ERROR), getLocale(ERROR_MESSAGE_FILE_READING_ERROR), exc);
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
    return streamFile(filePath, fileData.getMimeType());
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
      fileTransferObserver.transferred(FileTransferObserver.Direction.DOWNLOAD, outputStream.size());

      // Return the file stream
      return new ResponseEntity<>(outputStream.toByteArray(), headers, HttpStatus.OK);
    } catch (Exception exc) {
      throw new AWException(getLocale(ERROR_TITLE_FILE_READING_ERROR), getLocale(ERROR_MESSAGE_FILE_READING_ERROR), exc);
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
        Path destinationFile = Path.of(fullPath, fileData.getFileName());

        // Log saving file
        log.debug("Saving file on {}", destinationFile);

        // Store file
        try (InputStream inputStream = file.getInputStream()) {
          Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }
        fileTransferObserver.transferred(FileTransferObserver.Direction.UPLOAD, fileData.getFileSize());
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
   * Check whether the given (untrusted) {@link FileData} refers to a file inside the configured
   * upload area. A legitimately staged file always has {@code basePath} set to the configured
   * {@code uploadFilePath} (see {@link #uploadFile(MultipartFile, String)}), a simple destination
   * {@code relativePath}, and a sanitized {@code fileName}. This guard therefore requires
   * {@code basePath} to equal the configured upload root exactly, and rejects any parent-directory
   * traversal ({@code ..}) or absolute override in {@code relativePath}/{@code fileName} — the only
   * two ways to escape a fixed base. The checks are pure string comparisons on purpose: canonical
   * path resolution ({@code File#getCanonicalPath}, {@code File.separator}) behaves differently on
   * Windows and Linux, which made an earlier canonical-containment implementation pass locally but
   * fail (or, worse, mis-accept traversal) on the Linux CI runners.
   *
   * @param fileData Decoded (untrusted) file data
   * @return {@code true} if the file is inside the configured upload area
   */
  public boolean isPathWithinUploadArea(FileData fileData) {
    String expectedBase = baseConfigProperties.getComponent().getUploadFilePath();
    if (fileData.getBasePath() == null || !fileData.getBasePath().equals(expectedBase)) {
      return false;
    }
    return isSafePathSegment(fileData.getRelativePath()) && isSafePathSegment(fileData.getFileName());
  }

  /**
   * True if a path segment is safe to append under a fixed base: no parent-directory traversal and
   * not an absolute path. An absent (null/empty) segment is safe.
   *
   * @param value Untrusted path segment
   * @return {@code true} if the segment cannot escape the base
   */
  private boolean isSafePathSegment(String value) {
    if (value == null || value.isEmpty()) {
      return true;
    }
    if (value.contains("..")) {
      return false;
    }
    return !value.startsWith("/") && !value.startsWith("\\") && !value.matches("^[A-Za-z]:.*");
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
