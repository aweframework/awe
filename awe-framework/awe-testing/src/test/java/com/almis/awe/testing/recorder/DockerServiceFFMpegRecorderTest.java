package com.almis.awe.testing.recorder;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DockerServiceFFMpegRecorderTest {

  @Test
  void stopAndSaveReturnsWithoutWaitingWhenTheVideoWasNotRetrieved() {
    DockerServiceFFMpegWrapper wrapper = mock(DockerServiceFFMpegWrapper.class);
    when(wrapper.retrieveVideo("video")).thenReturn(Optional.empty());
    DockerServiceFFMpegRecorder recorder = new DockerServiceFFMpegRecorder(wrapper);

    File result = assertTimeoutPreemptively(Duration.ofSeconds(3), () -> recorder.stopAndSave("video"));

    assertThat(result).isNull();
  }

  @Test
  void stopAndSaveReturnsTheRetrievedVideo() throws Exception {
    File video = File.createTempFile("recorder-test", ".webm");
    video.deleteOnExit();
    DockerServiceFFMpegWrapper wrapper = mock(DockerServiceFFMpegWrapper.class);
    when(wrapper.retrieveVideo("video")).thenReturn(Optional.of(video));
    DockerServiceFFMpegRecorder recorder = new DockerServiceFFMpegRecorder(wrapper);

    assertThat(recorder.stopAndSave("video")).isEqualTo(video);
  }
}
