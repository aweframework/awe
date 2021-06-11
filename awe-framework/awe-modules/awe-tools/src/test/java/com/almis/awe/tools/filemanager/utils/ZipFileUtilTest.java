package com.almis.awe.tools.filemanager.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Zip files util class
 *
 * @author pgarcia
 */
class ZipFileUtilTest {

  @TempDir
  File tempDir;

  /**
   * Zip file test
   *
   * @throws Exception Test error
   */
  @Test
  void testZipFiles() throws Exception {
    // Prepare
    zipFileTest(tempDir + "/testZip.zip");

    // Run
    File expectedFile = new File(tempDir + "/testZip.zip");

    // Assert
    assertTrue(expectedFile.exists());
  }


  /**
   * Zip file test
   *
   * @throws Exception Test error
   */
  @Test
  void testUnzipFile() throws Exception {
    // Prepare
    zipFileTest(tempDir + "/testZip.zip");

    // Run
    ZipFileUtil.unzip(tempDir.getPath() + "/testZip.zip", tempDir.getPath() + "/unzip");
    File expectedFile = new File(tempDir.getPath() + "/unzip");

    // Assert
    assertTrue(expectedFile.exists());
  }

  /**
   * Create a zip file
   *
   * @param path Zip path
   */
  private void zipFileTest(String path) throws IOException {
    List<String> files = new ArrayList<>();
    files.add(Files.createFile(Paths.get(tempDir.getPath(), "Dummy.txt")).toString());
    files.add(Files.createDirectory(Paths.get(tempDir.getPath(), "DummyFolder")).toString());
    ZipFileUtil.zip(path, files);
  }
}