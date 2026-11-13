package com.almis.awe.testing.recorder;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DockerFFMpegRecorderTest {

  @Test
  void stopAndSaveWithoutAStartedRecordingReturnsNullWithoutWaiting() {
    DockerFFMpegRecorder recorder = new DockerFFMpegRecorder();

    assertThat(assertTimeoutPreemptively(Duration.ofSeconds(3), () -> recorder.stopAndSave("video"))).isNull();
  }

  @Test
  void stopAndSaveReturnsNullInsteadOfThrowingWhenTheFileNeverAppears() {
    DockerFFMpegWrapper wrapper = mock(DockerFFMpegWrapper.class);
    when(wrapper.isStarted()).thenReturn(true);
    when(wrapper.stopFFmpegAndSave("video")).thenThrow(new IllegalStateException("kill failed"));
    DockerFFMpegRecorder recorder = new DockerFFMpegRecorder(wrapper);

    assertThat(assertTimeoutPreemptively(Duration.ofSeconds(3), () -> recorder.stopAndSave("video"))).isNull();
  }
}
