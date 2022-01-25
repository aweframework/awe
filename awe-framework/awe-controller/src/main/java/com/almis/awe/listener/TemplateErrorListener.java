package com.almis.awe.listener;

import lombok.extern.slf4j.Slf4j;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.misc.STMessage;

@Slf4j
public class TemplateErrorListener implements STErrorListener {

  @Override
  public void compileTimeError(STMessage stMessage) {
    log.error("Compile time error: " + stMessage.toString(), stMessage.cause);
  }

  @Override
  public void runTimeError(STMessage stMessage) {
    log.error("Run time error: " + stMessage.toString(), stMessage.cause);
  }

  @Override
  public void IOError(STMessage stMessage) {
    log.error("I/O error: " + stMessage.toString(), stMessage.cause);
  }

  @Override
  public void internalError(STMessage stMessage) {
    log.error("Internal error: " + stMessage.toString(), stMessage.cause);
  }
}
