package com.almis.awe.testing.extensions;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.driver.BrowserDriver;
import com.almis.awe.testing.driver.BrowserDriverFactory;
import com.almis.awe.testing.driver.BrowserSession;
import com.almis.awe.testing.driver.SeleniumBrowserDriverFactory;
import com.almis.awe.testing.model.SeleniumModel;
import com.automation.remarks.video.exception.RecordingException;
import com.automation.remarks.video.recorder.IVideoRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class SeleniumExtensionTest {

  @TempDir
  Path tempDir;

  private SeleniumExtension extension;
  private SeleniumModel model;
  private Path shots;

  @BeforeEach
  void setUp() throws IOException {
    WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class));
    when(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)).thenReturn(new byte[]{1, 2, 3});

    shots = tempDir.resolve("shots");
    AweTestConfigProperties properties = new AweTestConfigProperties();
    properties.setScreenshotPath(shots.toString());
    properties.setAllowedRecording(false);

    model = new SeleniumModel().setDriver(driver).setProperties(properties).setCurrentOption("option");

    extension = new SeleniumExtension();
    ReflectionTestUtils.setField(extension, "seleniumModel", model);
    // An open session makes beforeEach skip the opening of the browser
    ReflectionTestUtils.setField(extension, "session", mock(BrowserSession.class));
  }

  @Test
  void afterTestExecutionStoresExactlyOneErrorScreenshotOnFailure() throws Exception {
    extension.beforeEach(context("t010_login", new AssertionError("boom")));

    extension.afterTestExecution(context("t010_login", new AssertionError("boom")));

    assertThat(shots()).hasSize(1);
    assertThat(shots().get(0)).startsWith("LoginIT-").contains("-[ERROR]-option-t010_login").endsWith(".png");
  }

  @Test
  void afterTestExecutionStoresNothingOnPass() throws Exception {
    extension.beforeEach(context("t010_login", null));

    extension.afterTestExecution(context("t010_login", null));

    assertThat(shots()).isEmpty();
  }

  @Test
  void afterEachDoesNotTakeTheScreenshotAnymore() throws Exception {
    extension.beforeEach(context("t010_login", new AssertionError("boom")));

    extension.afterEach(context("t010_login", new AssertionError("boom")));

    assertThat(shots()).isEmpty();
  }

  @Test
  void beforeEachResetsTheScreenshotTakenFlag() throws Exception {
    model.setScreenshotTaken(true);

    extension.beforeEach(context("t020_next", new AssertionError("boom")));
    assertThat(model.isScreenshotTaken()).isFalse();

    extension.afterTestExecution(context("t020_next", new AssertionError("boom")));
    assertThat(shots()).hasSize(1);
  }


  @Test
  void afterEachNeverFailsWhenTheRecorderCannotStopTheRecording() throws Exception {
    IVideoRecorder recorder = mock(IVideoRecorder.class);
    when(recorder.stopAndSave(anyString())).thenThrow(new RecordingException("Recording Condition was not fulfilled within 20 seconds"));
    SeleniumExtension recording = recordingExtension(recorder);

    recording.beforeEach(context("t010_login", null));

    assertThatCode(() -> recording.afterEach(context("t010_login", null))).doesNotThrowAnyException();
  }

  @Test
  void afterEachNeverFailsWhenTheRecorderThrowsAnyRuntimeException() throws Exception {
    IVideoRecorder recorder = mock(IVideoRecorder.class);
    when(recorder.stopAndSave(anyString())).thenThrow(new IllegalStateException("boom"));
    SeleniumExtension recording = recordingExtension(recorder);

    recording.beforeEach(context("t010_login", null));

    assertThatCode(() -> recording.afterEach(context("t010_login", null))).doesNotThrowAnyException();
  }

  @Test
  void aFailedRecordingStartDoesNotFailTheTestNorTheStop() throws Exception {
    IVideoRecorder recorder = mock(IVideoRecorder.class);
    doThrow(new RecordingException("start failed")).when(recorder).start();
    SeleniumExtension recording = recordingExtension(recorder);

    assertThatCode(() -> recording.beforeEach(context("t010_login", null))).doesNotThrowAnyException();
    assertThatCode(() -> recording.afterEach(context("t010_login", null))).doesNotThrowAnyException();
    verify(recorder, never()).stopAndSave(anyString());
  }

  @Test
  void aFailedRecordingCreationDoesNotFailTheTest() throws Exception {
    SeleniumExtension recording = new SeleniumExtension() {
      @Override
      protected IVideoRecorder createRecorder() {
        throw new IllegalStateException("no recorder");
      }
    };
    prepare(recording);

    assertThatCode(() -> recording.beforeEach(context("t010_login", null))).doesNotThrowAnyException();
    assertThatCode(() -> recording.afterEach(context("t010_login", null))).doesNotThrowAnyException();
  }

  @Test
  void aNullVideoFromTheRecorderIsTolerated() throws Exception {
    IVideoRecorder recorder = mock(IVideoRecorder.class);
    when(recorder.stopAndSave(anyString())).thenReturn(null);
    SeleniumExtension recording = recordingExtension(recorder);

    recording.beforeEach(context("t010_login", null));

    assertThatCode(() -> recording.afterEach(context("t010_login", new AssertionError("boom")))).doesNotThrowAnyException();
    verify(recorder).stopAndSave(anyString());
  }

  @Test
  void theBrowserIsOpenedOnceThroughTheFactoryOfTheTool() throws Exception {
    RecordingFactory factory = new RecordingFactory();
    SeleniumExtension opening = factoryExtension(factory);

    opening.beforeEach(context("t010_login", null));
    opening.beforeEach(context("t020_next", null));

    assertThat(factory.created).containsExactly("t010_login");
    assertThat(factory.model).isSameAs(model);
  }

  @Test
  void theBrowserIsClosedAfterTheTestClass() throws Exception {
    RecordingFactory factory = new RecordingFactory();
    SeleniumExtension opening = factoryExtension(factory);
    opening.beforeEach(context("t010_login", null));

    opening.afterAll(context("t010_login", null));
    opening.afterAll(context("t010_login", null));

    assertThat(factory.closed).isEqualTo(1);
  }

  @Test
  void anotherToolIsOpenedWithoutTouchingTheSeleniumDriver() throws Exception {
    BrowserDriver browser = mock(BrowserDriver.class);
    SeleniumExtension opening = factoryExtension(new RecordingFactory() {
      @Override
      public BrowserSession create(SeleniumModel target, String testName) {
        target.setBrowser(browser);
        return super.create(target, testName);
      }
    });
    model.setDriver(null);

    opening.beforeEach(context("t010_login", null));

    assertThat(model.getBrowser()).isSameAs(browser);
  }

  @Test
  void theDefaultFactoryIsTheOneOfTheConfiguredTool() {
    assertThat(new SeleniumExtension().createDriverFactory(new AweTestConfigProperties()))
      .isInstanceOf(SeleniumBrowserDriverFactory.class);
  }

  private SeleniumExtension factoryExtension(BrowserDriverFactory factory) {
    SeleniumExtension opening = new SeleniumExtension() {
      @Override
      protected BrowserDriverFactory createDriverFactory(AweTestConfigProperties properties) {
        return factory;
      }
    };
    ReflectionTestUtils.setField(opening, "seleniumModel", model);
    return opening;
  }

  /**
   * Factory double: records what it was asked for and hands out a session that counts its closes
   */
  private static class RecordingFactory implements BrowserDriverFactory {
    private final List<String> created = new java.util.ArrayList<>();
    private int closed;
    private SeleniumModel model;

    @Override
    public BrowserSession create(SeleniumModel target, String testName) {
      model = target;
      created.add(testName);
      return new BrowserSession() {
        @Override
        public BrowserDriver browser() {
          return mock(BrowserDriver.class);
        }

        @Override
        public void close() {
          closed++;
        }
      };
    }
  }

  private SeleniumExtension recordingExtension(IVideoRecorder recorder) {
    SeleniumExtension recording = new SeleniumExtension() {
      @Override
      protected IVideoRecorder createRecorder() {
        return recorder;
      }
    };
    prepare(recording);
    return recording;
  }

  private void prepare(SeleniumExtension target) {
    model.getProperties().setAllowedRecording(true);
    ReflectionTestUtils.setField(target, "seleniumModel", model);
    ReflectionTestUtils.setField(target, "session", mock(BrowserSession.class));
  }

  private ExtensionContext context(String testName, Throwable failure) {
    ExtensionContext parent = mock(ExtensionContext.class);
    when(parent.getDisplayName()).thenReturn("LoginIT");
    ExtensionContext context = mock(ExtensionContext.class);
    when(context.getDisplayName()).thenReturn(testName);
    when(context.getParent()).thenReturn(Optional.of(parent));
    when(context.getExecutionException()).thenReturn(Optional.ofNullable(failure));
    return context;
  }

  private List<String> shots() throws IOException {
    if (!Files.exists(shots)) {
      return java.util.Collections.emptyList();
    }
    try (Stream<Path> files = Files.list(shots)) {
      return files.map(path -> path.getFileName().toString()).collect(Collectors.toList());
    }
  }
}
