package com.almis.awe.model.util.file;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.util.data.CompressionUtil;
import com.almis.awe.model.util.data.StringUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

/**
 * FileUtil Class
 * File Utilities for AWE
 *
 * @author Pablo GARCIA - 19/JUL/2017
 */
public class FileUtil  {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * FileUtil constructor
   */
  private FileUtil() {
  }

  /**
   * Extract safely content type
   * @param file Multipart file
   * @return Sanitized filename
   */
  public static String extractContentType(MultipartFile file) {
    return MimeType.valueOf(Objects.requireNonNull(file.getContentType())).toString();
  }

  /**
   * Sanitize filename
   * @param filename Filename
   * @return Sanitized filename
   */
  public static String sanitizeFileName(String filename) {
    return filename == null ? "" : filename.replaceAll(".*([\\\\/])(.*)", "$2");
  }

  /**
   * Fix an untrusted path
   * @param paths Untrusted paths
   * @return Normalized path
   */
  public static String fixUntrustedPath(String... paths) {
    List<String> fixedPaths = new ArrayList<>();
    for (String path : paths) {
      fixedPaths.add(path
        .replaceAll("\\.\\.[\\\\/]", "")
        .replaceAll("[\\\\/]", Matcher.quoteReplacement(File.separator)));
    }
    return Paths.get(".", fixedPaths.toArray(new String[0])).normalize().toString();
  }

  /**
   * Transform fileData into a string
   * @param fileData File Data
   * @return Stringifies file data
   * @throws AWException AWE exception
   */
  public static String fileDataToString(FileData fileData) throws AWException {
    try {
      return Base64.getEncoder().encodeToString(CompressionUtil.compress(StringUtil.compressJson(objectMapper.writeValueAsString(fileData))));
    } catch (IOException exc) {
      throw new AWException("Error encoding file into string", "There was an error trying to encode file data into string:\n" + fileData.getFileName(),  exc);
    }
  }

  /**
   * Transform fileData into a string
   * @param fileStringEncoded File String encoded
   * @return FileData
   * @throws AWException AWE exception
   */
  public static FileData stringToFileData(String fileStringEncoded) throws AWException {
    try {
      String fileString = StringUtil.decompressJson(CompressionUtil.decompress(Base64.getDecoder().decode(fileStringEncoded)));
      return objectMapper.treeToValue(objectMapper.readTree(fileString), FileData.class);
    } catch (IOException exc) {
      throw new AWException("Error decoding file from string", "There was an error trying to decode file data from string:\n" + fileStringEncoded,  exc);
    }
  }
}
