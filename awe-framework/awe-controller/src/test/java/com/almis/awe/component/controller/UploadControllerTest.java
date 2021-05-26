package com.almis.awe.component.controller;

import com.almis.awe.controller.UploadController;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.util.log.LogUtil;
import com.almis.awe.service.BroadcastService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Rest controller /file test class
 */
@ExtendWith(MockitoExtension.class)
class UploadControllerTest {

  @InjectMocks
  UploadController uploadController;
  @Mock
  private ApplicationContext context;
  @Mock
  private BroadcastService broadcastService;
  @Mock
  private AweSession aweSession;
  @Mock
  private AweElements aweElements;
  @Mock
  private LogUtil logUtil;

  /**
   * Initializes json mapper for tests
   */
  @BeforeEach
  public void initBeans() {
    uploadController.setApplicationContext(context);
    when(context.getBean(AweSession.class)).thenReturn(aweSession);
    when(context.getBean(LogUtil.class)).thenReturn(logUtil);
  }

  /**
   * Test upload file ko
   */
  @Test
  void testUploadKOAWException() {

    uploadController.handleAWException(new AWException("Title", "Message"));
    verify(broadcastService, atLeastOnce()).broadcastMessageToUID(isNull(), any(ClientAction.class));
  }

  /**
   * Test upload file ko
   */
  @Test
  void testUploadKOMaxSize() {
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    uploadController.handleMaxSizeException(new MaxUploadSizeExceededException(22342342323L));
    verify(broadcastService, atLeastOnce()).broadcastMessageToUID(isNull(), any(ClientAction.class));
  }
}
