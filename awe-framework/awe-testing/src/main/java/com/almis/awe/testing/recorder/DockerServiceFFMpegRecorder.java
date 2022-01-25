package com.almis.awe.testing.recorder;

import com.automation.remarks.video.exception.RecordingException;
import com.automation.remarks.video.recorder.VideoRecorder;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DockerServiceFFMpegRecorder extends VideoRecorder {
  private final DockerServiceFFMpegWrapper ffmpegWrapper = new DockerServiceFFMpegWrapper();

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

  public File stopAndSave(String filename) {
    File file = this.getFfmpegWrapper().stopFFmpegAndSave(filename);
    this.waitForVideoCompleted(file);
    this.setLastVideo(file);
    return file;
  }

  private void waitForVideoCompleted(File video) {
    try {
      Awaitility.await().atMost(10L, TimeUnit.SECONDS).pollDelay(1L, TimeUnit.SECONDS).ignoreExceptions().until(video::exists);
    } catch (ConditionTimeoutException var3) {
      throw new RecordingException(var3.getMessage());
    }
  }
}
