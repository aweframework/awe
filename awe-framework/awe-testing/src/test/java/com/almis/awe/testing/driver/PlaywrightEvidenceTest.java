package com.almis.awe.testing.driver;

import com.almis.awe.testing.model.types.EvidenceMode;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.Video;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Evidence of the Playwright session: the trace of each failed test and the video of a test class that has a failure
 */
class PlaywrightEvidenceTest {

  @TempDir
  Path tempDir;

  private BrowserContext context;
  private Tracing tracing;
  private Video video;
  private AtomicLong nanos;
  private Path evidenceDir;
  private Path videoDir;
  private final List<String> announced = new ArrayList<>();

  @BeforeEach
  void setUp() throws IOException {
    context = mock(BrowserContext.class);
    tracing = mock(Tracing.class);
    video = mock(Video.class);
    when(context.tracing()).thenReturn(tracing);
    nanos = new AtomicLong(1_000_000_000L);
    evidenceDir = tempDir.resolve("evidence");
    videoDir = Files.createDirectories(evidenceDir.resolve("video-tmp"));
  }

  private PlaywrightEvidence evidence(EvidenceMode trace, EvidenceMode videoMode) {
    return evidence(trace, videoMode, false);
  }

  private PlaywrightEvidence evidence(EvidenceMode trace, EvidenceMode videoMode, boolean snapshots) {
    PlaywrightEvidence evidence = new PlaywrightEvidence(context, videoMode == EvidenceMode.OFF ? null : video, videoDir,
      evidenceDir, trace, snapshots, videoMode, nanos::get, 1_000_000_000L);
    evidence.setListener((label, file, attach) -> announced.add(label + "|" + file.getFileName() + "|" + attach));
    return evidence;
  }

  @Test
  void tracingStartsOnceWithScreenshotsButWithoutSnapshotsNorSourcesByDefault() {
    evidence(EvidenceMode.ON_FAILURE, EvidenceMode.OFF).startTracing();

    ArgumentCaptor<Tracing.StartOptions> options = ArgumentCaptor.forClass(Tracing.StartOptions.class);
    verify(tracing).start(options.capture());
    assertThat(options.getValue().screenshots).isTrue();
    assertThat(options.getValue().snapshots).isFalse();
    assertThat(options.getValue().sources).isFalse();
  }

  @Test
  void tracingTakesDomSnapshotsWhenAskedTo() {
    evidence(EvidenceMode.ON_FAILURE, EvidenceMode.OFF, true).startTracing();

    ArgumentCaptor<Tracing.StartOptions> options = ArgumentCaptor.forClass(Tracing.StartOptions.class);
    verify(tracing).start(options.capture());
    assertThat(options.getValue().snapshots).isTrue();
  }

  @Test
  void aFailedTestSavesItsTraceChunkAndAnnouncesIt() {
    PlaywrightEvidence evidence = evidence(EvidenceMode.ON_FAILURE, EvidenceMode.OFF);
    evidence.testStarted("LoginIT", "t010_login");

    evidence.testFinished("t010_login", "LoginIT-2026-[ERROR]-t010_login", true);

    ArgumentCaptor<Tracing.StopChunkOptions> options = ArgumentCaptor.forClass(Tracing.StopChunkOptions.class);
    verify(tracing).startChunk(any(Tracing.StartChunkOptions.class));
    verify(tracing).stopChunk(options.capture());
    assertThat(options.getValue().path).isEqualTo(evidenceDir.resolve("LoginIT-2026-[ERROR]-t010_login.trace.zip"));
    assertThat(announced).containsExactly("Failure trace|LoginIT-2026-[ERROR]-t010_login.trace.zip|true");
  }

  @Test
  void aPassedTestDiscardsItsTraceChunk() {
    PlaywrightEvidence evidence = evidence(EvidenceMode.ON_FAILURE, EvidenceMode.OFF);
    evidence.testStarted("LoginIT", "t010_login");

    evidence.testFinished("t010_login", "LoginIT-2026-t010_login", false);

    verify(tracing).stopChunk();
    verify(tracing, never()).stopChunk(any(Tracing.StopChunkOptions.class));
    assertThat(announced).isEmpty();
  }

  @Test
  void theAlwaysModeSavesThePassedTestsTraceToo() {
    PlaywrightEvidence evidence = evidence(EvidenceMode.ALWAYS, EvidenceMode.OFF);
    evidence.testStarted("LoginIT", "t010_login");

    evidence.testFinished("t010_login", "LoginIT-2026-t010_login", false);

    verify(tracing).stopChunk(any(Tracing.StopChunkOptions.class));
    assertThat(announced).containsExactly("Trace|LoginIT-2026-t010_login.trace.zip|true");
  }

  @Test
  void theOffModeNeitherStartsTracingNorChunks() {
    PlaywrightEvidence evidence = evidence(EvidenceMode.OFF, EvidenceMode.OFF);
    evidence.startTracing();
    evidence.testStarted("LoginIT", "t010_login");
    evidence.testFinished("t010_login", "name", true);

    org.mockito.Mockito.verifyNoInteractions(tracing);
  }

  @Test
  void aChunkThatWasNeverStoppedIsDiscardedWhenTheNextTestStarts() {
    PlaywrightEvidence evidence = evidence(EvidenceMode.ON_FAILURE, EvidenceMode.OFF);
    evidence.testStarted("LoginIT", "t010_login");

    evidence.testStarted("LoginIT", "t020_next");

    org.mockito.InOrder order = inOrder(tracing);
    order.verify(tracing).startChunk(any(Tracing.StartChunkOptions.class));
    order.verify(tracing).stopChunk();
    order.verify(tracing).startChunk(any(Tracing.StartChunkOptions.class));
  }

  @Test
  void aFinishWithoutStartDoesNothing() {
    evidence(EvidenceMode.ON_FAILURE, EvidenceMode.OFF).testFinished("t010_login", "name", true);

    verify(tracing, never()).stopChunk(any(Tracing.StopChunkOptions.class));
    verify(tracing, never()).stopChunk();
  }

  @Test
  void tracingFailuresNeverReachTheTest() {
    doThrow(new PlaywrightException("tracing is gone")).when(tracing).start(any(Tracing.StartOptions.class));
    doThrow(new PlaywrightException("tracing is gone")).when(tracing).startChunk(any(Tracing.StartChunkOptions.class));
    doThrow(new PlaywrightException("tracing is gone")).when(tracing).stopChunk(any(Tracing.StopChunkOptions.class));
    PlaywrightEvidence evidence = evidence(EvidenceMode.ON_FAILURE, EvidenceMode.OFF);

    assertThatCode(() -> {
      evidence.startTracing();
      evidence.testStarted("LoginIT", "t010_login");
      evidence.testFinished("t010_login", "name", true);
    }).doesNotThrowAnyException();
    assertThat(announced).isEmpty();
  }

  @Test
  void aClassWithAFailureKeepsItsVideoAndTheTimesOfItsTests() throws IOException {
    PlaywrightEvidence evidence = evidence(EvidenceMode.OFF, EvidenceMode.ON_FAILURE);
    nanos.set(1_000_000_000L + TimeUnit.MILLISECONDS.toNanos(1_500));
    evidence.testStarted("LoginIT", "t010_login");
    nanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(6_300));
    evidence.testFinished("t010_login", "n1", false);
    nanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(10));
    evidence.testStarted("LoginIT", "t020_next");
    evidence.testFinished("t020_next", "n2", true);

    evidence.finishVideo();

    Path target = evidenceDir.resolve("LoginIT.webm");
    verify(video).saveAs(target);
    verify(video).delete();
    assertThat(Files.readAllLines(evidenceDir.resolve("LoginIT.video-times.txt"))).anySatisfy(line -> assertThat(line)
        .contains("00:01.500").contains("PASSED").contains("t010_login"))
      .anySatisfy(line -> assertThat(line).contains("00:07.810").contains("FAILED").contains("t020_next"));
    assertThat(announced).containsExactly("Failure video|LoginIT.webm|false");
    assertThat(videoDir).doesNotExist();
  }

  @Test
  void theVideoOfAClassIsNamedWithCharactersThatAnyFileSystemAccepts() {
    PlaywrightEvidence evidence = evidence(EvidenceMode.OFF, EvidenceMode.ON_FAILURE);
    evidence.testStarted("com.almis.it/Login IT: <first>", "t010_login");
    evidence.testFinished("t010_login", "n1", true);

    evidence.finishVideo();

    verify(video).saveAs(evidenceDir.resolve("com.almis.it_Login_IT___first_.webm"));
    assertThat(evidenceDir.resolve("com.almis.it_Login_IT___first_.video-times.txt")).exists();
  }

  @Test
  void aClassWithoutFailuresDeletesItsVideo() {
    PlaywrightEvidence evidence = evidence(EvidenceMode.OFF, EvidenceMode.ON_FAILURE);
    evidence.testStarted("LoginIT", "t010_login");
    evidence.testFinished("t010_login", "n1", false);

    evidence.finishVideo();

    verify(video, never()).saveAs(any(Path.class));
    verify(video).delete();
    assertThat(evidenceDir.resolve("LoginIT.video-times.txt")).doesNotExist();
    assertThat(announced).isEmpty();
    assertThat(videoDir).doesNotExist();
  }

  @Test
  void theAlwaysModeKeepsTheVideoOfAPassedClass() {
    PlaywrightEvidence evidence = evidence(EvidenceMode.OFF, EvidenceMode.ALWAYS);
    evidence.testStarted("LoginIT", "t010_login");
    evidence.testFinished("t010_login", "n1", false);

    evidence.finishVideo();

    verify(video).saveAs(evidenceDir.resolve("LoginIT.webm"));
    assertThat(announced).containsExactly("Video|LoginIT.webm|false");
  }

  @Test
  void withoutVideoThereIsNothingToFinish() {
    PlaywrightEvidence evidence = evidence(EvidenceMode.OFF, EvidenceMode.OFF);
    evidence.testStarted("LoginIT", "t010_login");
    evidence.testFinished("t010_login", "n1", true);

    assertThatCode(evidence::finishVideo).doesNotThrowAnyException();
    org.mockito.Mockito.verifyNoInteractions(video);
  }

  @Test
  void videoFailuresNeverReachTheTestRun() {
    doThrow(new PlaywrightException("video is gone")).when(video).saveAs(any(Path.class));
    doThrow(new PlaywrightException("video is gone")).when(video).delete();
    PlaywrightEvidence evidence = evidence(EvidenceMode.OFF, EvidenceMode.ON_FAILURE);
    evidence.testStarted("LoginIT", "t010_login");
    evidence.testFinished("t010_login", "n1", true);

    assertThatCode(evidence::finishVideo).doesNotThrowAnyException();
    assertThat(announced).isEmpty();
  }
}
