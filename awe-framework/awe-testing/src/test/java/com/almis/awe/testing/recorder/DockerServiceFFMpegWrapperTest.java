package com.almis.awe.testing.recorder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DockerServiceFFMpegWrapperTest {

  @TempDir
  Path tempDir;

  private RestTemplate restTemplate;
  private DockerServiceFFMpegWrapper wrapper;

  @BeforeEach
  void setUp() {
    System.setProperty("video.folder", tempDir.toString());
    System.setProperty("video.file.extension", ".webm");
    System.setProperty("video.recorder.url", "http://recorder:3000");
    System.setProperty("video.screen.size", "1280x720");
    restTemplate = mock(RestTemplate.class);
    wrapper = new DockerServiceFFMpegWrapper(() -> restTemplate);
  }

  @AfterEach
  void tearDown() {
    System.clearProperty("video.folder");
    System.clearProperty("video.file.extension");
    System.clearProperty("video.recorder.url");
    System.clearProperty("video.screen.size");
  }

  @Test
  void retrievesTheVideoWhenTheRecorderReturnsIt() throws IOException {
    when(restTemplate.postForObject(endsWith("/start"), any(), org.mockito.ArgumentMatchers.eq(String.class))).thenReturn("id-1");
    when(restTemplate.postForObject(endsWith("/stop"), any(), org.mockito.ArgumentMatchers.eq(byte[].class))).thenReturn(new byte[]{1, 2, 3});
    when(restTemplate.postForObject(endsWith("/log"), any(), org.mockito.ArgumentMatchers.eq(String.class))).thenReturn("log");

    wrapper.startFFmpeg();
    Optional<java.io.File> video = wrapper.retrieveVideo("video");

    assertThat(video).isPresent();
    assertThat(Files.readAllBytes(video.get().toPath())).containsExactly(1, 2, 3);
  }

  @Test
  void reportsNoVideoWhenStopFails() {
    when(restTemplate.postForObject(endsWith("/start"), any(), org.mockito.ArgumentMatchers.eq(String.class))).thenReturn("id-1");
    when(restTemplate.postForObject(endsWith("/stop"), any(), org.mockito.ArgumentMatchers.eq(byte[].class)))
      .thenThrow(new ResourceAccessException("Unexpected end of file from server"));

    wrapper.startFFmpeg();

    assertThat(wrapper.retrieveVideo("video")).isEmpty();
    assertThat(tempDir.resolve("video.webm")).doesNotExist();
  }

  @Test
  void reportsNoVideoWhenStopReturnsNoBytes() {
    when(restTemplate.postForObject(endsWith("/start"), any(), org.mockito.ArgumentMatchers.eq(String.class))).thenReturn("id-1");
    when(restTemplate.postForObject(endsWith("/stop"), any(), org.mockito.ArgumentMatchers.eq(byte[].class))).thenReturn(new byte[0]);

    wrapper.startFFmpeg();

    assertThat(wrapper.retrieveVideo("video")).isEmpty();
  }

  @Test
  void skipsStopWhenStartFailed() {
    when(restTemplate.postForObject(endsWith("/start"), any(), org.mockito.ArgumentMatchers.eq(String.class)))
      .thenThrow(new ResourceAccessException("Connection refused"));

    wrapper.startFFmpeg();

    assertThat(wrapper.retrieveVideo("video")).isEmpty();
    verify(restTemplate, never()).postForObject(endsWith("/stop"), any(), org.mockito.ArgumentMatchers.eq(byte[].class));
  }

  @Test
  void aFailedStartForgetsTheIdentifierOfThePreviousRecording() {
    when(restTemplate.postForObject(endsWith("/start"), any(), org.mockito.ArgumentMatchers.eq(String.class)))
      .thenReturn("id-1")
      .thenThrow(new ResourceAccessException("Connection refused"));

    wrapper.startFFmpeg();
    wrapper.startFFmpeg();

    assertThat(wrapper.retrieveVideo("video")).isEmpty();
    verify(restTemplate, never()).postForObject(endsWith("/stop"), any(), org.mockito.ArgumentMatchers.eq(byte[].class));
  }

  @Test
  void keepsTheVideoWhenOnlyTheLogFails() {
    when(restTemplate.postForObject(endsWith("/start"), any(), org.mockito.ArgumentMatchers.eq(String.class))).thenReturn("id-1");
    when(restTemplate.postForObject(endsWith("/stop"), any(), org.mockito.ArgumentMatchers.eq(byte[].class))).thenReturn(new byte[]{1});
    when(restTemplate.postForObject(endsWith("/log"), any(), org.mockito.ArgumentMatchers.eq(String.class)))
      .thenThrow(new ResourceAccessException("log down"));

    wrapper.startFFmpeg();

    assertThat(wrapper.retrieveVideo("video")).isPresent();
  }
}
