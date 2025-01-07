package com.almis.awe.model.util.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilTest {

  @Test
  void sanitizeInputParameter() {
    assertEquals("Parame&lt;test&gt;ter1", StringUtil.sanitizeInputParameter("Parame<test>ter1"));
    assertEquals("Param \\\\n eter12", StringUtil.sanitizeInputParameter("Param \n eter12"));
    assertEquals("Parameter-1", StringUtil.sanitizeInputParameter("Parameter-1"));
  }
}