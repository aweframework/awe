package com.almis.awe.model.util.file;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * File util tests
 */
class FileUtilTest {

  @Test
  void sanitizeFileName() {
    assertEquals("tutu.zip", FileUtil.sanitizeFileName("allala/../tutu.zip"));
    assertEquals("", FileUtil.sanitizeFileName(null));
  }

  @Test
  void fixUntrustedPath() {
    assertEquals(Paths.get("allala", "tutu.zip").toString(), FileUtil.fixUntrustedPath("allala/..\\tutu.zip"));
    assertEquals(Paths.get("allala", "tutu", "alalal", "asdaas", "epa.txt").toString(), FileUtil.fixUntrustedPath("allala/../tutu\\../../", "/alalal\\../asdaas/../epa.txt"));
    assertEquals("", FileUtil.fixUntrustedPath());
  }
}