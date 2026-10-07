package com.almis.awe.testing.driver;

import com.almis.awe.testing.model.types.EvidenceMode;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.Video;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

/**
 * Evidence that the Playwright session collects on its own, which is all of it evidence and never a test condition: every
 * failure is logged and nothing reaches the test that is running.
 *
 * <ul>
 *   <li><b>Trace.</b> Tracing starts once for the context and each test is a chunk of it ({@code startChunk} when the test
 *   starts, {@code stopChunk} when it ends). The chunk of a failed test is saved as {@code <evidence name>.trace.zip}; the
 *   others are discarded without being written. Open it with {@code npx playwright show-trace file.zip}.</li>
 *   <li><b>Video.</b> The page is recorded while the context lasts, which is the whole test class, since its ordered tests
 *   share the login and the data. When the context is closed the video is kept as {@code <class>.webm} if a test of the
 *   class failed and deleted otherwise, and {@code <class>.video-times.txt} says where each test starts in it.</li>
 * </ul>
 */
@Slf4j
class PlaywrightEvidence {

  static final String TRACE_EXTENSION = ".trace.zip";
  static final String VIDEO_EXTENSION = ".webm";
  static final String TIMES_EXTENSION = ".video-times.txt";

  private final BrowserContext context;
  private final Video video;
  private final Path videoDir;
  private final Path evidenceDir;
  private final EvidenceMode traceMode;
  private final boolean snapshots;
  private final EvidenceMode videoMode;
  private final LongSupplier nanoClock;
  private final long recordingStart;
  private final List<TestRun> runs = new ArrayList<>();
  private BrowserSession.EvidenceListener listener = (label, file, attach) -> { };
  private boolean chunkOpen;
  private String testClass;

  /**
   * Evidence of a session
   *
   * @param context     Context of the session (its tracing records the trace)
   * @param video       Video of the page, or null when it is not recorded
   * @param videoDir    Folder where the browser writes the video, which is removed once the video is resolved
   * @param evidenceDir Folder of the evidence files
   * @param traceMode   When the trace of a test is kept
   * @param snapshots   Whether the trace takes DOM snapshots (they make the run noticeably slower)
   * @param videoMode   When the video of a test class is kept
   * @param nanoClock   Source of time, in nanoseconds, for the times of the tests in the video
   * @param recordingStart Moment (of the same source) when the video started, which is when its page was opened
   */
  PlaywrightEvidence(BrowserContext context, Video video, Path videoDir, Path evidenceDir, EvidenceMode traceMode,
                     boolean snapshots, EvidenceMode videoMode, LongSupplier nanoClock, long recordingStart) {
    this.context = context;
    this.video = video;
    this.videoDir = videoDir;
    this.evidenceDir = evidenceDir;
    this.traceMode = traceMode;
    this.snapshots = snapshots;
    this.videoMode = videoMode;
    this.nanoClock = nanoClock;
    this.recordingStart = recordingStart;
  }

  /**
   * Set who is told about the files that are stored
   *
   * @param listener Listener
   */
  void setListener(BrowserSession.EvidenceListener listener) {
    this.listener = listener;
  }

  /**
   * Start tracing the context, with the screenshots of the Trace Viewer, the DOM snapshots when they are asked for and never
   * the sources
   */
  void startTracing() {
    if (traceMode == EvidenceMode.OFF) {
      return;
    }
    try {
      context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(snapshots).setSources(false));
    } catch (RuntimeException exc) {
      log.warn("The Playwright trace could not be started. The tests go on without trace", exc);
    }
  }

  /**
   * A test starts: a new chunk of the trace and its place in the video
   *
   * @param testClass Test class
   * @param testName  Test name
   */
  void testStarted(String testClass, String testName) {
    this.testClass = testClass;
    if (videoMode != EvidenceMode.OFF) {
      runs.add(new TestRun(testName, nanoClock.getAsLong() - recordingStart));
    }
    if (traceMode == EvidenceMode.OFF) {
      return;
    }
    try {
      if (chunkOpen) {
        // The test before it never ended: its chunk is worth nothing
        chunkOpen = false;
        context.tracing().stopChunk();
      }
      context.tracing().startChunk(new Tracing.StartChunkOptions().setTitle(testName));
      chunkOpen = true;
    } catch (RuntimeException exc) {
      log.warn("The Playwright trace of the test could not be started. The test goes on without trace", exc);
    }
  }

  /**
   * A test ends: its trace is saved when it is evidence and discarded otherwise, and its result is noted for the video
   *
   * @param testName     Test name
   * @param evidenceName Base name of the evidence files of the test
   * @param failed       Whether it failed
   */
  void testFinished(String testName, String evidenceName, boolean failed) {
    if (!runs.isEmpty()) {
      runs.get(runs.size() - 1).failed = failed;
    }
    if (traceMode == EvidenceMode.OFF || !chunkOpen) {
      return;
    }
    chunkOpen = false;
    try {
      if (failed || traceMode == EvidenceMode.ALWAYS) {
        Path target = evidenceDir.resolve(evidenceName + TRACE_EXTENSION);
        Files.createDirectories(evidenceDir);
        context.tracing().stopChunk(new Tracing.StopChunkOptions().setPath(target));
        log.info("Playwright trace stored at {}", target);
        listener.stored(failed ? "Failure trace" : "Trace", target, true);
      } else {
        context.tracing().stopChunk();
      }
    } catch (IOException | RuntimeException exc) {
      log.warn("The Playwright trace of the test could not be stored. The test result is not affected", exc);
    }
  }

  /**
   * The context is closed, so the video is complete: keep it with the times of the tests when it is evidence, delete it
   * otherwise
   */
  void finishVideo() {
    if (video == null) {
      return;
    }
    try {
      boolean failed = runs.stream().anyMatch(run -> run.failed);
      if (videoMode == EvidenceMode.ALWAYS || failed) {
        keepVideo(failed);
      }
      video.delete();
    } catch (RuntimeException exc) {
      log.warn("The Playwright video could not be resolved. The test result is not affected", exc);
    } finally {
      removeVideoDir();
    }
  }

  private void keepVideo(boolean failed) {
    try {
      // The name of a test class can carry what a file name cannot (a display name with a slash, a colon...)
      String name = testClass == null ? "playwright" : testClass.replaceAll("[^A-Za-z0-9._-]", "_");
      Files.createDirectories(evidenceDir);
      Path target = evidenceDir.resolve(name + VIDEO_EXTENSION);
      video.saveAs(target);
      Files.write(evidenceDir.resolve(name + TIMES_EXTENSION), times(), StandardCharsets.UTF_8);
      log.info("Playwright video stored at {}", target);
      listener.stored(failed ? "Failure video" : "Video", target, false);
    } catch (IOException | RuntimeException exc) {
      log.warn("The Playwright video could not be stored. The test result is not affected", exc);
    }
  }

  private List<String> times() {
    List<String> lines = new ArrayList<>();
    lines.add("# Start of each test from the beginning of the video (approximate), its result and its name");
    runs.forEach(run -> lines.add(String.format("%s  %s  %s", offset(run.startNanos), run.failed ? "FAILED" : "PASSED", run.name)));
    return lines;
  }

  private static String offset(long nanos) {
    long millis = TimeUnit.NANOSECONDS.toMillis(Math.max(nanos, 0));
    return String.format("%02d:%02d.%03d", millis / 60_000, millis / 1000 % 60, millis % 1000);
  }

  private void removeVideoDir() {
    if (videoDir == null || !Files.isDirectory(videoDir)) {
      return;
    }
    try (Stream<Path> files = Files.walk(videoDir)) {
      files.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
        try {
          Files.deleteIfExists(path);
        } catch (IOException exc) {
          log.debug("Could not delete {}", path, exc);
        }
      });
    } catch (IOException | RuntimeException exc) {
      log.debug("Could not clean the folder of the Playwright video {}", videoDir, exc);
    }
  }

  /**
   * A test of the class and where it starts in the video
   */
  private static final class TestRun {
    private final String name;
    private final long startNanos;
    private boolean failed;

    private TestRun(String name, long startNanos) {
      this.name = name;
      this.startNanos = startNanos;
    }
  }
}
