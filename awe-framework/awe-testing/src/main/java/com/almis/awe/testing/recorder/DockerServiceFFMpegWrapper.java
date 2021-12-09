package com.almis.awe.testing.recorder;

import com.almis.awe.testing.model.VideoRecorderStartRequest;
import com.almis.awe.testing.model.VideoRecorderStopRequest;
import com.automation.remarks.video.recorder.VideoRecorder;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

@Log4j2
public class DockerServiceFFMpegWrapper {

  private String fileIdentifier;

  public void startFFmpeg(String... args) {
    // Generate rest template and parameters
    RestTemplate restTemplate = new RestTemplate();
    HttpEntity<VideoRecorderStartRequest> request = new HttpEntity<>(new VideoRecorderStartRequest()
      .setSource(VideoRecorder.conf().ffmpegDisplay())
      .setSize(getDockerScreenSize())
      .setFps(VideoRecorder.conf().frameRate())
      .setPixelFormat(VideoRecorder.conf().ffmpegPixelFormat())
      .setFileFormat(getVideoFormat())
      .setExtraInput(Collections.emptyList())
      .setExtraOutput(Arrays.asList(args))
    );

    try {
      // Call /start endpoint
      fileIdentifier = restTemplate.postForObject(getVideoRecorderUrl("/start"), request, String.class);
      log.debug("Launching /start endpoint with id {}", fileIdentifier);
    } catch (RestClientException exc) {
      log.error("Error trying to start video recording", exc);
    }
  }

  public File stopFFmpegAndSave(String filename) {
    RestTemplate restTemplate = new RestTemplate();
    File destFile = getFileName(filename);
    HttpEntity<VideoRecorderStopRequest> request = new HttpEntity<>(new VideoRecorderStopRequest().setId(fileIdentifier));

    try {
      // Create directories if not exists
      File videoFolder = new File(VideoRecorder.conf().folder());
      if (!videoFolder.exists()) {
        Files.createDirectories(videoFolder.toPath());
      }

      // Call /stop endpoint
      log.debug("Launching /stop endpoint for video {}", destFile.getAbsolutePath());
      byte[] video = restTemplate.postForObject(getVideoRecorderUrl("/stop"), request, byte[].class);
      if (video != null) {
        Files.write(destFile.toPath(), video);
      }

      // Retrieve log
      String videoLog = restTemplate.postForObject(getVideoRecorderUrl("/log"), request, String.class);
      log.debug(videoLog);
    } catch (RestClientException | IOException exc) {
      log.error("Error trying to retrieve and store video", exc);
    }

    return destFile;
  }

  private File getFileName(String filename) {
    String movieFolder = VideoRecorder.conf().folder();
    return Paths.get(movieFolder, filename + getVideoFormat()).toFile();
  }

  private String getDockerScreenSize() {
    return System.getProperty("video.screen.size");
  }

  private String getVideoRecorderUrl(String path) {
    return String.format("%s%s", System.getProperty("video.recorder.url"), path);
  }

  private String getVideoFormat() {
    return System.getProperty("video.file.extension");
  }
}
