package com.almis.awe.testing.recorder;

import com.automation.remarks.video.enums.RecorderType;
import com.automation.remarks.video.recorder.IVideoRecorder;
import com.automation.remarks.video.recorder.ffmpeg.LinuxFFmpegRecorder;
import com.automation.remarks.video.recorder.ffmpeg.MacFFmpegRecorder;
import com.automation.remarks.video.recorder.ffmpeg.WindowsFFmpegRecorder;
import com.automation.remarks.video.recorder.monte.MonteRecorder;
import org.apache.commons.lang3.SystemUtils;

public class SeleniumRecorderFactory {
  private SeleniumRecorderFactory() {}
  public static IVideoRecorder getRecorder(RecorderType recorderType) {
    if (RecorderType.FFMPEG.equals(recorderType)) {
      if ("browser".equals(System.getProperty("isDocker"))) {
        return new DockerFFMpegRecorder();
      } else if ("browserRecorder".equals(System.getProperty("isDocker"))) {
        return new DockerServiceFFMpegRecorder();
      } else if (SystemUtils.IS_OS_WINDOWS) {
        return new WindowsFFmpegRecorder();
      } else if (SystemUtils.IS_OS_MAC) {
        return new MacFFmpegRecorder();
      } else {
        return new LinuxFFmpegRecorder();
      }
    } else {
      return new MonteRecorder();
    }
  }
}
