package com.almis.awe.testing.extensions;

import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.utilities.TextUtilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;

/**
 * Builds unique, sortable names for test evidence (videos and screenshots) and applies the
 * "evidence only on failure" policy: videos are kept only when they are evidence, and a screenshot
 * is taken for every failed test.
 * <p>
 * Evidence collection never throws: it must not mask or replace the original test failure.
 */
@Slf4j
public class FailureEvidence {

  private static final String TIMESTAMP_PATTERN = "yyyy-MM-dd_HH-mm-ss-SSS";
  private static final String ERROR_MARKER = "[ERROR]-";

  private static final String PROJECT_DIR_VARIABLE = "CI_PROJECT_DIR";
  private static final String JOB_URL_VARIABLE = "CI_JOB_URL";

  private final Clock clock;
  private final Function<String, String> environment;
  private final PrintStream out;
  private final DateTimeFormatter timestampFormatter;

  /**
   * Evidence using the system clock
   */
  public FailureEvidence() {
    this(Clock.systemDefaultZone());
  }

  /**
   * Evidence using the given clock (injectable for testing)
   *
   * @param clock Clock used to timestamp the names
   */
  public FailureEvidence(Clock clock) {
    this(clock, System::getenv, null);
  }

  /**
   * Evidence using the given clock, environment lookup and output sink (injectable for testing)
   *
   * @param clock       Clock used to timestamp the names
   * @param environment Environment variable lookup, returns null when the variable is not set
   * @param out         Sink of the evidence lines; null means the standard output at the time of printing
   */
  public FailureEvidence(Clock clock, Function<String, String> environment, PrintStream out) {
    this.clock = clock;
    this.environment = environment;
    this.out = out;
    this.timestampFormatter = DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN).withZone(clock.getZone());
  }

  /**
   * Build the base name of a piece of evidence:
   * {@code <TestClass>-<yyyy-MM-dd_HH-mm-ss-SSS>-[ERROR]-<option>-<title>}. The {@code [ERROR]-} marker is
   * only present for failures.
   *
   * @param testClass Test class name
   * @param option    Current option
   * @param title     Test title or assertion message
   * @param failed    Whether the evidence belongs to a failure
   * @return Evidence base name, without extension
   */
  public String buildName(String testClass, String option, String title, boolean failed) {
    return String.format("%s-%s-%s%s-%s",
      testClass,
      timestampFormatter.format(clock.instant()),
      failed ? ERROR_MARKER : "",
      sanitize(option),
      sanitize(title));
  }

  /**
   * Keep the recorded video when it is evidence (test failed or every video must be saved),
   * delete it otherwise. Never throws.
   *
   * @param video   Video file returned by the recorder (may be null)
   * @param failed  Whether the test failed
   * @param keepAll Whether every video must be kept
   */
  public void resolveVideo(File video, boolean failed, boolean keepAll) {
    if (video == null) {
      log.debug("No video recording available for this test");
      return;
    }

    if (failed || keepAll) {
      log.info("{}Video recording stored at {}", failed ? "Test failed. " : "", video.getAbsolutePath());
      if (failed) {
        print("Failure video: " + describe(video.toPath()));
      }
      return;
    }

    try {
      Files.deleteIfExists(video.toPath());
      log.debug("Test passed. Video recording discarded: {}", video.getAbsolutePath());
    } catch (Exception exc) {
      log.warn("Could not delete video recording {}", video.getAbsolutePath(), exc);
    }
  }

  /**
   * Take a screenshot of a failed test when none was taken yet. Never throws.
   *
   * @param model     Selenium model of the running test
   * @param testClass Test class name
   * @param failed    Whether the test failed
   * @return Path of the stored screenshot, if any
   */
  public Optional<Path> captureScreenshotOnFailure(SeleniumModel model, String testClass, boolean failed) {
    if (!failed || model.isScreenshotTaken()) {
      return Optional.empty();
    }

    WebDriver driver = model.getDriver();
    if (!(driver instanceof TakesScreenshot)) {
      log.warn("Test failed but no screenshot could be taken: the driver does not support screenshots");
      return Optional.empty();
    }

    try {
      File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
      Path target = Paths.get(model.getProperties().getScreenshotPath(),
        buildName(testClass, model.getCurrentOption(), model.getTestTitle(), true) + ".png");
      storeScreenshot(model, source, target);
      log.error("Test failed. Screenshot stored at: {}", target);
      return Optional.of(target);
    } catch (Exception exc) {
      log.warn("Test failed but the failure screenshot could not be stored", exc);
      return Optional.empty();
    }
  }

  /**
   * Store a failure screenshot, flag it as taken in the model and announce it in the test output: an
   * {@code [[ATTACHMENT|path]]} marker (GitLab shows it in the pipeline test report) plus a line with the link
   * to the file. Both are only produced once the file is stored.
   *
   * @param model  Selenium model of the running test
   * @param source Screenshot taken by the driver
   * @param target Where the screenshot must be stored
   * @throws IOException When the screenshot cannot be stored
   */
  public void storeScreenshot(SeleniumModel model, File source, Path target) throws IOException {
    Files.createDirectories(target.getParent());
    FileUtils.copyFile(source, target.toFile());
    model.setScreenshotTaken(true);
    relativeToProjectDir(target).ifPresent(relative -> print("[[ATTACHMENT|" + relative + "]]"));
    print("Failure screenshot: " + describe(target));
  }

  /**
   * Store the page source (DOM) of a failed test next to its screenshot: same name, {@code .html} extension. It
   * announces the file in the test output with a link, like the screenshot. Never throws: it must not mask the
   * original test failure.
   *
   * @param screenshotTarget Path of the failure screenshot the page source belongs to
   * @param pageSource       Page source returned by the driver (may be null)
   * @return Path of the stored page source, if any
   */
  public Optional<Path> storePageSource(Path screenshotTarget, String pageSource) {
    if (pageSource == null) {
      log.warn("Test failed but there is no page source to store");
      return Optional.empty();
    }

    try {
      String fileName = screenshotTarget.getFileName().toString();
      String baseName = fileName.toLowerCase().endsWith(".png") ? fileName.substring(0, fileName.length() - 4) : fileName;
      Path target = screenshotTarget.resolveSibling(baseName + ".html");
      Files.createDirectories(target.getParent());
      Files.write(target, pageSource.getBytes(StandardCharsets.UTF_8));
      print("Failure page source: " + describe(target));
      return Optional.of(target);
    } catch (Exception exc) {
      log.warn("Test failed but the page source could not be stored", exc);
      return Optional.empty();
    }
  }

  /**
   * Describe where a stored file can be reached: an artifact URL of the CI job when the job URL and the project
   * directory are known and the file is inside the project, its absolute path otherwise.
   */
  private String describe(Path file) {
    String jobUrl = variable(JOB_URL_VARIABLE);
    return relativeToProjectDir(file)
      .filter(relative -> jobUrl != null)
      .map(relative -> jobUrl + "/artifacts/file/" + relative)
      .orElseGet(() -> file.toAbsolutePath().toString());
  }

  /**
   * Path of the file relative to the CI project directory, with forward slashes on every OS
   */
  private Optional<String> relativeToProjectDir(Path file) {
    String projectDir = variable(PROJECT_DIR_VARIABLE);
    if (projectDir == null) {
      return Optional.empty();
    }

    Path root = Paths.get(projectDir).toAbsolutePath().normalize();
    Path absolute = file.toAbsolutePath().normalize();
    if (!absolute.startsWith(root) || absolute.equals(root)) {
      return Optional.empty();
    }
    return Optional.of(root.relativize(absolute).toString().replace('\\', '/'));
  }

  private String variable(String name) {
    String value = environment.apply(name);
    return value == null || value.trim().isEmpty() ? null : value.trim();
  }

  private void print(String line) {
    (out == null ? System.out : out).println(line);
  }

  private String sanitize(String text) {
    return TextUtilities.sanitizeMessage(text == null ? "" : text);
  }
}
