package com.almis.awe.service.data.connector.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class QueryLauncherTest {

  @InjectMocks
  private QueryLauncher queryLauncher;

  @Test
  void givenNullQuery_throwsNullPointerException() {
    assertThrows(NullPointerException.class, () -> queryLauncher.launchQuery(null, null));
  }
}