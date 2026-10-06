package com.almis.awe.testing.extensions;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.driver.BrowserDriverFactory;
import com.almis.awe.testing.driver.BrowserSession;
import com.almis.awe.testing.driver.SeleniumBrowserDriverFactory;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.recorder.SeleniumRecorderFactory;
import com.automation.remarks.video.recorder.IVideoRecorder;
import com.automation.remarks.video.recorder.VideoRecorder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.*;

import java.io.File;
import java.io.IOException;

/**
 * Utilities suite for browser testing: opens the browser of the configured tool ({@code awe.test.tool}) for the test class,
 * and collects the evidence of the tests
 */
@Slf4j
public class SeleniumExtension implements AfterAllCallback, BeforeEachCallback, AfterTestExecutionCallback,
  AfterEachCallback, TestInstancePostProcessor {

  public static final String VIDEO_SCREEN_SIZE = "video.screen.size";
  public static final String WD_HUB = SeleniumBrowserDriverFactory.WD_HUB;
  private final SeleniumModel seleniumModel = new SeleniumModel();
  private BrowserSession session;
  private IVideoRecorder recorder;
  private final FailureEvidence failureEvidence = new FailureEvidence();

  /**
   * Close the browser after a test suite
   */
  @Override
  public void afterAll(ExtensionContext extensionContext) {
    if (session != null) {
      BrowserSession closing = session;
      session = null;
      closing.close();
    }
  }

  /**
   * Open the browser of the test run with the tool that is configured
   *
   * @param extensionContext Extension context
   * @throws IOException If the address of a remote browser is not valid
   */
  private void initializeBrowser(ExtensionContext extensionContext) throws IOException {
    AweTestConfigProperties properties = seleniumModel.getProperties();

    // Set video folder property
    System.setProperty("video.folder", properties.getScreenshotPath());
    System.setProperty("video.file.extension", properties.getVideoFormat().getVideoExtension());
    System.setProperty(VIDEO_SCREEN_SIZE, String.format("%dx%d", properties.getBrowserWidth(), properties.getBrowserHeight()));

    session = createDriverFactory(properties).create(seleniumModel, extensionContext.getDisplayName());
  }

  /**
   * Get the factory of the tool that runs the tests ({@code awe.test.tool})
   *
   * @param properties Test properties
   * @return Factory that opens the browser
   */
  protected BrowserDriverFactory createDriverFactory(AweTestConfigProperties properties) {
    return BrowserDriverFactory.forTool(properties.getTool());
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {

    // If the browser has been opened, return
    if (session == null) {
      initializeBrowser(extensionContext);
    }

    // Set test title
    seleniumModel.setTestTitle(extensionContext.getDisplayName());
    seleniumModel.setScreenshotTaken(false);

    // Check recording
    this.recorder = null;
    if (seleniumModel.getProperties().isAllowedRecording()) {
      startRecording();
    }
  }

  /**
   * Start the video recording. Recording is evidence, never a test condition: a failure is logged and the test goes on
   * without video (no recorder is kept, so the stop is skipped).
   */
  private void startRecording() {
    try {
      IVideoRecorder videoRecorder = createRecorder();
      videoRecorder.start();
      this.recorder = videoRecorder;
    } catch (Exception exc) {
      log.warn("Video recording could not be started. The test goes on without video", exc);
    }
  }

  /**
   * Create the video recorder of the test
   *
   * @return Video recorder
   */
  protected IVideoRecorder createRecorder() {
    return SeleniumRecorderFactory.getRecorder(VideoRecorder.conf().recorderType());
  }

  /**
   * Take the failure screenshot right after the test method, before the test class own @AfterEach methods
   * (which run before {@link #afterEach}) can navigate away from the failing page.
   */
  @Override
  public void afterTestExecution(ExtensionContext extensionContext) {
    boolean testFailed = extensionContext.getExecutionException().isPresent();
    String testClass = extensionContext.getParent().orElse(extensionContext).getDisplayName();

    // Every failure leaves a screenshot, even when no assertWithScreenshot was involved
    failureEvidence.captureScreenshotOnFailure(seleniumModel, testClass, testFailed);
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    boolean testFailed = extensionContext.getExecutionException().isPresent();
    String testClass = extensionContext.getParent().orElse(extensionContext).getDisplayName();

    IVideoRecorder videoRecorder = this.recorder;
    this.recorder = null;
    if (videoRecorder != null && seleniumModel.getProperties().isAllowedRecording()) {
      try {
        log.debug("Storing video recording...");
        String fileName = failureEvidence.buildName(testClass, seleniumModel.getCurrentOption(),
          seleniumModel.getTestTitle(), testFailed);
        File result = videoRecorder.stopAndSave(fileName);
        boolean keepAll = "ALL".equalsIgnoreCase(seleniumModel.getProperties().getVideoSave().toString());
        failureEvidence.resolveVideo(result, testFailed, keepAll);
      } catch (Exception exc) {
        // Recording is evidence, never a test condition: it must not change the test result
        log.warn("Video recording could not be stored. The test result is not affected", exc);
      }
    }
  }

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext extensionContext) throws Exception {
    testInstance.getClass()
      .getMethod("setSeleniumModel", SeleniumModel.class)
      .invoke(testInstance, seleniumModel);
  }
}
