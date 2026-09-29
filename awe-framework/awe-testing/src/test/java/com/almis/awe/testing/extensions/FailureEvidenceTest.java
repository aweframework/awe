package com.almis.awe.testing.extensions;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.model.SeleniumModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class FailureEvidenceTest {

  private static final Instant INSTANT = Instant.parse("2026-09-29T15:04:05.123Z");

  @TempDir
  Path tempDir;

  private FailureEvidence evidence;
  private WebDriver driver;
  private SeleniumModel model;
  private Map<String, String> environment;
  private ByteArrayOutputStream console;

  @BeforeEach
  void setUp() throws IOException {
    environment = new HashMap<>();
    console = new ByteArrayOutputStream();
    evidence = new FailureEvidence(Clock.fixed(INSTANT, ZoneOffset.UTC), environment::get,
      new PrintStream(console, true, StandardCharsets.UTF_8));

    driver = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class));
    File png = Files.createTempFile(tempDir, "source", ".png").toFile();
    Files.write(png.toPath(), new byte[]{1, 2, 3});
    when(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE)).thenReturn(png);

    AweTestConfigProperties properties = new AweTestConfigProperties();
    properties.setScreenshotPath(tempDir.resolve("shots").toString());
    model = new SeleniumModel()
      .setDriver(driver)
      .setProperties(properties)
      .setCurrentOption("Some Option")
      .setTestTitle("t010_Login Test");
  }

  // ---- Names ----

  @Test
  void failureNameContainsErrorMarker() {
    String name = evidence.buildName("LoginIT", "Some Option", "t010_Login Test", true);

    assertThat(name).contains("-[ERROR]-");
  }

  @Test
  void passNameDoesNotContainErrorMarker() {
    String name = evidence.buildName("LoginIT", "Some Option", "t010_Login Test", false);

    assertThat(name).doesNotContain("[ERROR]");
  }

  @Test
  void nameUsesSortableTimestampWithMilliseconds() {
    String name = evidence.buildName("LoginIT", "opt", "title", false);

    assertThat(name).isEqualTo("LoginIT-2026-09-29_15-04-05-123-opt-title");
  }

  @Test
  void failureNameHasFullLayout() {
    String name = evidence.buildName("LoginIT", "Some Option", "t010_Login Test", true);

    assertThat(name).isEqualTo("LoginIT-2026-09-29_15-04-05-123-[ERROR]-some_option-t010_login_test");
  }

  @Test
  void twoCallsOneMillisecondApartProduceDifferentNames() {
    FailureEvidence first = new FailureEvidence(Clock.fixed(INSTANT, ZoneOffset.UTC));
    FailureEvidence second = new FailureEvidence(Clock.fixed(INSTANT.plusMillis(1), ZoneOffset.UTC));

    assertThat(first.buildName("LoginIT", "opt", "title", true))
      .isNotEqualTo(second.buildName("LoginIT", "opt", "title", true));
  }

  @Test
  void nameToleratesMissingOption() {
    assertThat(evidence.buildName("LoginIT", null, "title", true)).contains("[ERROR]").endsWith("title");
  }

  // ---- Video ----

  @Test
  void passedTestInFailedModeDeletesVideo() throws IOException {
    Path video = Files.createTempFile(tempDir, "video", ".mp4");

    evidence.resolveVideo(video.toFile(), false, false);

    assertThat(video).doesNotExist();
  }

  @Test
  void failedTestInFailedModeKeepsVideo() throws IOException {
    Path video = Files.createTempFile(tempDir, "video", ".mp4");

    evidence.resolveVideo(video.toFile(), true, false);

    assertThat(video).exists();
  }

  @Test
  void passedTestInAllModeKeepsVideo() throws IOException {
    Path video = Files.createTempFile(tempDir, "video", ".mp4");

    evidence.resolveVideo(video.toFile(), false, true);

    assertThat(video).exists();
  }

  @Test
  void nullVideoDoesNotThrow() {
    assertThatCode(() -> evidence.resolveVideo(null, false, false)).doesNotThrowAnyException();
    assertThatCode(() -> evidence.resolveVideo(null, true, false)).doesNotThrowAnyException();
  }

  @Test
  void failedDeleteDoesNotThrow() throws IOException {
    // A non-empty directory cannot be deleted by Files.deleteIfExists
    Path notDeletable = Files.createDirectory(tempDir.resolve("not-deletable"));
    Files.createFile(notDeletable.resolve("child"));

    assertThatCode(() -> evidence.resolveVideo(notDeletable.toFile(), false, false)).doesNotThrowAnyException();
    assertThat(notDeletable).exists();
  }

  // ---- Screenshot ----

  @Test
  void failedTestWithoutScreenshotStoresOneWithErrorName() throws IOException {
    Optional<Path> stored = evidence.captureScreenshotOnFailure(model, "LoginIT", true);

    assertThat(stored).isPresent();
    assertThat(stored.get()).exists().hasFileName(
      "LoginIT-2026-09-29_15-04-05-123-[ERROR]-some_option-t010_login_test.png");
    assertThat(stored.get().getParent()).isEqualTo(tempDir.resolve("shots"));
    assertThat(model.isScreenshotTaken()).isTrue();
    assertThat(shots()).hasSize(1);
  }

  @Test
  void failedTestWithScreenshotAlreadyTakenStoresNoSecondOne() throws IOException {
    model.setScreenshotTaken(true);

    Optional<Path> stored = evidence.captureScreenshotOnFailure(model, "LoginIT", true);

    assertThat(stored).isEmpty();
    assertThat(shots()).isEmpty();
  }

  @Test
  void passedTestStoresNoScreenshot() throws IOException {
    Optional<Path> stored = evidence.captureScreenshotOnFailure(model, "LoginIT", false);

    assertThat(stored).isEmpty();
    assertThat(shots()).isEmpty();
  }

  @Test
  void driverFailureWhileCapturingDoesNotPropagate() throws IOException {
    when(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE))
      .thenThrow(new WebDriverException("session is gone"));

    assertThatCode(() -> evidence.captureScreenshotOnFailure(model, "LoginIT", true)).doesNotThrowAnyException();
    assertThat(shots()).isEmpty();
  }

  @Test
  void missingDriverDoesNotPropagate() {
    model.setDriver(null);

    assertThatCode(() -> evidence.captureScreenshotOnFailure(model, "LoginIT", true)).doesNotThrowAnyException();
  }

  @Test
  void driverWithoutScreenshotSupportDoesNotPropagate() {
    model.setDriver(mock(WebDriver.class));

    assertThatCode(() -> evidence.captureScreenshotOnFailure(model, "LoginIT", true)).doesNotThrowAnyException();
    assertThat(model.isScreenshotTaken()).isFalse();
  }

  // ---- CI evidence output ----

  private static final String SCREENSHOT_NAME = "LoginIT-2026-09-29_15-04-05-123-[ERROR]-some_option-t010_login_test.png";

  @Test
  void screenshotInsideProjectDirPrintsAttachmentMarkerWithRelativePath() {
    environment.put("CI_PROJECT_DIR", tempDir.toString());

    evidence.captureScreenshotOnFailure(model, "LoginIT", true);

    assertThat(output()).contains("[[ATTACHMENT|shots/" + SCREENSHOT_NAME + "]]");
    assertThat(output().split("\\[\\[ATTACHMENT\\|", -1)).hasSize(2);
  }

  @Test
  void screenshotWithoutProjectDirPrintsNoMarkerAndAbsolutePath() {
    evidence.captureScreenshotOnFailure(model, "LoginIT", true);

    assertThat(output()).doesNotContain("[[ATTACHMENT|");
    assertThat(output()).contains("Failure screenshot: " + tempDir.resolve("shots").resolve(SCREENSHOT_NAME));
  }

  @Test
  void screenshotOutsideProjectDirPrintsNoMarker() throws IOException {
    environment.put("CI_PROJECT_DIR", Files.createDirectory(tempDir.resolve("other")).toString());

    evidence.captureScreenshotOnFailure(model, "LoginIT", true);

    assertThat(output()).doesNotContain("[[ATTACHMENT|");
    assertThat(output()).contains("Failure screenshot: " + tempDir.resolve("shots").resolve(SCREENSHOT_NAME));
  }

  @Test
  void screenshotLinkIsBuiltFromJobUrlAndRelativePath() {
    environment.put("CI_PROJECT_DIR", tempDir.toString());
    environment.put("CI_JOB_URL", "https://gitlab.example/group/project/-/jobs/42");

    evidence.captureScreenshotOnFailure(model, "LoginIT", true);

    assertThat(output()).contains(
      "Failure screenshot: https://gitlab.example/group/project/-/jobs/42/artifacts/file/shots/" + SCREENSHOT_NAME);
  }

  @Test
  void jobUrlWithoutProjectDirFallsBackToAbsolutePath() {
    environment.put("CI_JOB_URL", "https://gitlab.example/group/project/-/jobs/42");

    evidence.captureScreenshotOnFailure(model, "LoginIT", true);

    assertThat(output()).doesNotContain("https://gitlab.example");
    assertThat(output()).contains("Failure screenshot: " + tempDir.resolve("shots").resolve(SCREENSHOT_NAME));
  }

  @Test
  void blankEnvironmentValuesAreTreatedAsUnset() {
    environment.put("CI_PROJECT_DIR", " ");
    environment.put("CI_JOB_URL", "");

    evidence.captureScreenshotOnFailure(model, "LoginIT", true);

    assertThat(output()).doesNotContain("[[ATTACHMENT|");
    assertThat(output()).contains("Failure screenshot: " + tempDir.resolve("shots").resolve(SCREENSHOT_NAME));
  }

  @Test
  void skippedScreenshotPrintsNothing() {
    environment.put("CI_PROJECT_DIR", tempDir.toString());
    model.setScreenshotTaken(true);

    evidence.captureScreenshotOnFailure(model, "LoginIT", true);
    evidence.captureScreenshotOnFailure(model, "LoginIT", false);

    assertThat(output()).isEmpty();
  }

  @Test
  void storeScreenshotCopiesFileMarksModelAndAnnouncesIt() throws IOException {
    environment.put("CI_PROJECT_DIR", tempDir.toString());
    File source = Files.write(tempDir.resolve("source-2.png"), new byte[]{9}).toFile();
    Path target = tempDir.resolve("shots").resolve("manual.png");

    evidence.storeScreenshot(model, source, target);

    assertThat(target).exists().hasBinaryContent(new byte[]{9});
    assertThat(model.isScreenshotTaken()).isTrue();
    assertThat(output()).contains("[[ATTACHMENT|shots/manual.png]]");
  }

  @Test
  void storeScreenshotFailurePrintsNothingAndDoesNotMarkModel() throws IOException {
    environment.put("CI_PROJECT_DIR", tempDir.toString());
    Path notADirectory = Files.createFile(tempDir.resolve("not-a-directory"));

    assertThatThrownBy(() -> evidence.storeScreenshot(model, tempDir.resolve("missing.png").toFile(),
      notADirectory.resolve("manual.png"))).isInstanceOf(IOException.class);

    assertThat(model.isScreenshotTaken()).isFalse();
    assertThat(output()).isEmpty();
  }

  @Test
  void keptVideoLinkIsBuiltFromJobUrlAndRelativePath() throws IOException {
    environment.put("CI_PROJECT_DIR", tempDir.toString());
    environment.put("CI_JOB_URL", "https://gitlab.example/group/project/-/jobs/42");
    Path video = Files.createDirectories(tempDir.resolve("shots")).resolve("LoginIT-video.mp4");
    Files.write(video, new byte[]{1});

    evidence.resolveVideo(video.toFile(), true, false);

    assertThat(output()).contains(
      "Failure video: https://gitlab.example/group/project/-/jobs/42/artifacts/file/shots/LoginIT-video.mp4");
    assertThat(output()).doesNotContain("[[ATTACHMENT|");
  }

  @Test
  void keptVideoWithoutCiVariablesPrintsAbsolutePath() throws IOException {
    Path video = Files.createTempFile(tempDir, "video", ".mp4");

    evidence.resolveVideo(video.toFile(), true, false);

    assertThat(output()).contains("Failure video: " + video.toAbsolutePath());
  }

  @Test
  void passedVideoKeptInAllModePrintsNoFailureLine() throws IOException {
    Path video = Files.createTempFile(tempDir, "video", ".mp4");

    evidence.resolveVideo(video.toFile(), false, true);

    assertThat(output()).isEmpty();
  }

  @Test
  void deletedVideoPrintsNothing() throws IOException {
    Path video = Files.createTempFile(tempDir, "video", ".mp4");

    evidence.resolveVideo(video.toFile(), false, false);

    assertThat(output()).isEmpty();
  }

  private String output() {
    return new String(console.toByteArray(), StandardCharsets.UTF_8);
  }

  private java.util.List<Path> shots() throws IOException {
    Path dir = tempDir.resolve("shots");
    if (!Files.exists(dir)) {
      return java.util.Collections.emptyList();
    }
    try (Stream<Path> files = Files.list(dir)) {
      return files.collect(java.util.stream.Collectors.toList());
    }
  }
}
