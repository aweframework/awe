package com.almis.awe.testing.recorder;

import com.automation.remarks.video.exception.RecordingException;
import com.automation.remarks.video.recorder.VideoRecorder;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DockerFFMpegRecorder extends VideoRecorder {
  private final DockerFFMpegWrapper ffmpegWrapper;

  public DockerFFMpegRecorder() {
    this(new DockerFFMpegWrapper());
  }

  /**
   * Recorder using the given wrapper (injectable for testing)
   *
   * @param ffmpegWrapper Wrapper of the ffmpeg process
   */
  public DockerFFMpegRecorder(DockerFFMpegWrapper ffmpegWrapper) {
    this.ffmpegWrapper = ffmpegWrapper;
  }

  public DockerFFMpegWrapper getFfmpegWrapper() {
    return this.ffmpegWrapper;
  }

  public void start() {
    this.getFfmpegWrapper().startFFmpeg(
      "-c:v", "libvpx",
      "-cpu-used", "-5",
      "-deadline", "realtime",
      "-qp", "0"
    );
  }

  /**
   * Stop the recording and store the video. Never throws and never waits for a recording that was not started.
   *
   * @param filename Video name, without extension
   * @return The stored video, or null when it could not be retrieved
   */
  public File stopAndSave(String filename) {
    if (!this.getFfmpegWrapper().isStarted()) {
      log.warn("No video recording was started, nothing to stop");
      return null;
    }

    try {
      File file = this.getFfmpegWrapper().stopFFmpegAndSave(filename);
      this.waitForVideoCompleted(file);
      this.setLastVideo(file);
      return file;
    } catch (RuntimeException exc) {
      log.warn("Video recording {} could not be retrieved. The test result is not affected", filename, exc);
      return null;
    }
  }

  private void waitForVideoCompleted(File video) {
    try {
      log.info("Waiting 10 seconds while {} exists", video.getAbsolutePath());
      Awaitility.await().atMost(10L, TimeUnit.SECONDS).pollDelay(1L, TimeUnit.SECONDS).ignoreExceptions().until(video::exists);
    } catch (ConditionTimeoutException var3) {
      throw new RecordingException(var3.getMessage());
    }
  }
}
