package com.almis.awe.testing.driver;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.model.types.BrowserTool;
import com.almis.awe.testing.model.types.BrowserType;
import com.almis.awe.testing.model.types.EvidenceMode;
import com.microsoft.playwright.PlaywrightException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The evidence that a real headless browser leaves: a readable trace per failed test and one video per test class
 */
@RequiresPlaywrightBrowser
class PlaywrightBrowserEvidenceBrowserTest {

  @TempDir
  Path tempDir;

  private BrowserSession open(EvidenceMode trace, EvidenceMode video) throws IOException {
    AweTestConfigProperties properties = new AweTestConfigProperties();
    properties.setTool(BrowserTool.PLAYWRIGHT);
    properties.setBrowser("firefox".equals(PlaywrightTestBrowser.engine()) ? BrowserType.HEADLESS_FIREFOX : BrowserType.HEADLESS_CHROME);
    properties.setBrowserWidth(640);
    properties.setBrowserHeight(480);
    properties.setScreenshotPath(tempDir.toString());
    properties.getPlaywright().setTrace(trace);
    properties.getPlaywright().setVideo(video);
    SeleniumModel model = new SeleniumModel().setProperties(properties);
    try {
      return new NoDownloadPlaywrightBrowserDriverFactory().create(model, "LoginIT");
    } catch (PlaywrightException exc) {
      if (!PlaywrightTestBrowser.isNotInstalled(exc)) {
        throw exc;
      }
      assumeTrue(false, "Playwright browser is not installed: " + exc.getMessage());
      return null;
    }
  }

  private void runTest(BrowserSession session, String name, boolean failed) throws InterruptedException {
    session.testStarted("LoginIT", name);
    session.browser().open("data:text/html,<button id='go' onclick=\"document.title='clicked'\">Go</button>");
    session.browser().click(Locator.css("#go"));
    // Some frames for the video to have something to show
    Thread.sleep(700);
    session.testFinished(name, "LoginIT-" + name + (failed ? "-[ERROR]" : ""), failed);
  }

  private List<String> files() throws IOException {
    try (Stream<Path> stream = Files.walk(tempDir)) {
      return stream.filter(Files::isRegularFile).map(path -> tempDir.relativize(path).toString()).sorted().toList();
    }
  }

  @Test
  void aFailedTestLeavesAReadableTraceAndTheClassLeavesItsVideoAndTimes() throws Exception {
    BrowserSession session = open(EvidenceMode.ON_FAILURE, EvidenceMode.ON_FAILURE);
    try {
      runTest(session, "t010_login", false);
      runTest(session, "t020_next", true);
    } finally {
      session.close();
    }

    assertThat(files()).containsExactly("LoginIT-t020_next-[ERROR].trace.zip", "LoginIT.video-times.txt", "LoginIT.webm");
    try (ZipFile zip = new ZipFile(tempDir.resolve("LoginIT-t020_next-[ERROR].trace.zip").toFile())) {
      assertThat(zip.stream().map(entry -> entry.getName())).contains("trace.trace");
    }
    assertThat(Files.size(tempDir.resolve("LoginIT.webm"))).isGreaterThan(0);
    assertThat(Files.readAllLines(tempDir.resolve("LoginIT.video-times.txt"))).hasSize(3)
      .anySatisfy(line -> assertThat(line).contains("PASSED").contains("t010_login"))
      .anySatisfy(line -> assertThat(line).contains("FAILED").contains("t020_next"));
  }

  @Test
  void aClassThatPassesLeavesNoTraceNoVideoAndNoFolder() throws Exception {
    BrowserSession session = open(EvidenceMode.ON_FAILURE, EvidenceMode.ON_FAILURE);
    try {
      runTest(session, "t010_login", false);
      runTest(session, "t020_next", false);
    } finally {
      session.close();
    }

    assertThat(files()).isEmpty();
    try (Stream<Path> stream = Files.list(tempDir)) {
      assertThat(stream).isEmpty();
    }
  }

  @Test
  void theOffModesLeaveNothingEvenOnFailure() throws Exception {
    BrowserSession session = open(EvidenceMode.OFF, EvidenceMode.OFF);
    try {
      runTest(session, "t010_login", true);
    } finally {
      session.close();
    }

    assertThat(files()).isEmpty();
  }
}
