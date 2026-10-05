package com.almis.awe.testing.recorder;

import com.automation.remarks.video.exception.RecordingException;
import com.automation.remarks.video.recorder.VideoRecorder;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DockerServiceFFMpegRecorder extends VideoRecorder {
  private final DockerServiceFFMpegWrapper ffmpegWrapper;

  public DockerServiceFFMpegRecorder() {
    this(new DockerServiceFFMpegWrapper());
  }

  /**
   * Recorder using the given wrapper (injectable for testing)
   *
   * @param ffmpegWrapper Wrapper of the recorder service
   */
  public DockerServiceFFMpegRecorder(DockerServiceFFMpegWrapper ffmpegWrapper) {
    this.ffmpegWrapper = ffmpegWrapper;
  }

  public DockerServiceFFMpegWrapper getFfmpegWrapper() {
    return this.ffmpegWrapper;
  }

  public void start() {
    this.getFfmpegWrapper().startFFmpeg(
      "-c:v libvpx-vp9",
      "-deadline realtime",
      "-cpu-used 5"
    );
  }

  /**
   * Stop the recording and store the video. It never waits for a video that was not retrieved.
   *
   * @param filename Video name, without extension
   * @return The stored video, or null when it could not be retrieved
   */
  public File stopAndSave(String filename) {
    Optional<File> video = this.getFfmpegWrapper().retrieveVideo(filename);
    if (!video.isPresent()) {
      log.warn("Video recording {} could not be retrieved. The test result is not affected", filename);
      return null;
    }

    File file = video.get();
    try {
      this.waitForVideoCompleted(file);
    } catch (RecordingException exc) {
      log.warn("Video recording {} was not completed. The test result is not affected", filename, exc);
      return null;
    }
    this.setLastVideo(file);
    return file;
  }

  private void waitForVideoCompleted(File video) {
    try {
      Awaitility.await().atMost(20L, TimeUnit.SECONDS).pollDelay(1L, TimeUnit.SECONDS).ignoreExceptions().until(video::exists);
    } catch (ConditionTimeoutException var3) {
      throw new RecordingException(var3.getMessage());
    }
  }
}
