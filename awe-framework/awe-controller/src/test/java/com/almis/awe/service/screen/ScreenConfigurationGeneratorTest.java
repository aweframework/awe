package com.almis.awe.service.screen;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.screen.Screen;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
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

  @AfterEach
  void clearInterruptFlag() {
    Thread.interrupted();
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

  @Test
  void givenFailedFuture_applyScreenConfigurationKeepsThreadUninterrupted() {
    CompletableFuture<ServiceData> configurationTask = CompletableFuture.failedFuture(new IllegalStateException("Connection refused"));
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);

    screenConfigurationGenerator.applyScreenConfiguration(configurationTask, new Screen());

    assertThat(Thread.currentThread().isInterrupted()).isFalse();
  }

  @Test
  void givenInterruptedWait_applyScreenConfigurationRestoresTheInterruptFlag() throws Exception {
    @SuppressWarnings("unchecked")
    Future<ServiceData> interrupted = mock(Future.class);
    when(interrupted.get()).thenThrow(new InterruptedException("interrupted"));
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);

    screenConfigurationGenerator.applyScreenConfiguration(interrupted, new Screen());

    assertThat(Thread.currentThread().isInterrupted()).isTrue();
  }
}
