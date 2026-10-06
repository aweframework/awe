package com.almis.awe.testing.config;

import com.almis.awe.testing.model.types.BrowserTool;
import org.springframework.core.convert.converter.Converter;

/**
 * Reads {@code awe.test.tool}. An unknown tool is rejected with a message that lists the supported ones, instead of the
 * generic "no enum constant" of the default conversion
 */
public class BrowserToolConverter implements Converter<String, BrowserTool> {

  @Override
  public BrowserTool convert(String source) {
    return BrowserTool.fromName(source);
  }
}
