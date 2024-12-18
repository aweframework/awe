package com.almis.awe.service.screen;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.screen.Screen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ScreenConfigurationGeneratorTest {

  @InjectMocks
  private ScreenConfigurationGenerator screenConfigurationGenerator;

  @Mock
  ApplicationContext applicationContext;

  @Mock
  AweElements aweElements;

  @BeforeEach
  void setUp() {
    screenConfigurationGenerator.setApplicationContext(applicationContext);
  }

  @Test
  void givenInterruptedFuture_applyScreenConfiguration() {
    // Given
    CompletableFuture<ServiceData> configurationTask = CompletableFuture.failedFuture(new InterruptedException());
    Screen screen = new Screen();
    // When
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    // Then
    screenConfigurationGenerator.applyScreenConfiguration(configurationTask, screen);
    verify(aweElements, times(1)).getLocaleWithLanguage(anyString(), eq(null));
  }
}