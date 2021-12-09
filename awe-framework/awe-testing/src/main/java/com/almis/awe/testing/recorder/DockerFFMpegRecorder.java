package com.almis.awe.testing.recorder;

import com.automation.remarks.video.exception.RecordingException;
import com.automation.remarks.video.recorder.VideoRecorder;
import lombok.extern.log4j.Log4j2;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Log4j2
public class DockerFFMpegRecorder extends VideoRecorder {
  private final DockerFFMpegWrapper ffmpegWrapper = new DockerFFMpegWrapper();

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

  public File stopAndSave(String filename) {
    File file = this.getFfmpegWrapper().stopFFmpegAndSave(filename);
    this.waitForVideoCompleted(file);
    this.setLastVideo(file);
    return file;
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
