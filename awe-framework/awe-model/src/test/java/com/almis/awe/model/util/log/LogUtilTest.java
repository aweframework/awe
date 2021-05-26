package com.almis.awe.model.util.log;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class LogUtilTest {

  @Mock
  private LogUtil logUtil;

  @Test
  void logWithDatabase() {
    Logger spyLogger = spy(LogManager.getLogger(LogUtilTest.class));
    logUtil.logWithDatabase(LogUtilTest.class, Level.DEBUG, "testDatabase", "test log message");
    verify(logUtil, times(1)).logWithDatabase(any(), eq(Level.DEBUG), anyString(), anyString());
    verify(spyLogger, times(0)).log(eq(Level.DEBUG), anyString());
  }
}