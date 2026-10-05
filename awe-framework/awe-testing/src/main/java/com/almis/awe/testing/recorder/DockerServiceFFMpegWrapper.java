package com.almis.awe.testing.recorder;

import com.almis.awe.testing.model.VideoRecorderStartRequest;
import com.almis.awe.testing.model.VideoRecorderStopRequest;
import com.automation.remarks.video.recorder.VideoRecorder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public class DockerServiceFFMpegWrapper {

  private final Supplier<RestTemplate> restTemplateSupplier;
  private String fileIdentifier;

  /**
   * Wrapper calling the recorder service with a plain {@link RestTemplate}
   */
  public DockerServiceFFMpegWrapper() {
    this(RestTemplate::new);
  }

  /**
   * Wrapper calling the recorder service with the given {@link RestTemplate} (injectable for testing)
   *
   * @param restTemplateSupplier Supplier of the rest template
   */
  public DockerServiceFFMpegWrapper(Supplier<RestTemplate> restTemplateSupplier) {
    this.restTemplateSupplier = restTemplateSupplier;
  }

  /**
   * Start the recording. Never throws: a recording that cannot be started leaves no identifier, so a later
   * stop is skipped.
   *
   * @param args Extra ffmpeg output arguments
   */
  public void startFFmpeg(String... args) {
    // Forget the previous recording: a failed start must not leave a stale identifier behind
    fileIdentifier = null;

    // Generate rest template and parameters
    RestTemplate restTemplate = restTemplateSupplier.get();
    HttpEntity<VideoRecorderStartRequest> request = new HttpEntity<>(new VideoRecorderStartRequest()
      .setSource(VideoRecorder.conf().ffmpegDisplay())
      .setSize(getDockerScreenSize())
      .setFps(VideoRecorder.conf().frameRate())
      .setPixelFormat(VideoRecorder.conf().ffmpegPixelFormat())
      .setFileFormat(getVideoFormat())
      .setExtraInput(Collections.emptyList())
      .setExtraOutput(Arrays.asList(args)),
      jsonHeaders()
    );

    try {
      // Call /start endpoint
      fileIdentifier = restTemplate.postForObject(getVideoRecorderUrl("/start"), request, String.class);
      log.debug("Launching /start endpoint with id {}", fileIdentifier);
    } catch (RuntimeException exc) {
      fileIdentifier = null;
      log.warn("Error trying to start video recording. The test goes on without video", exc);
    }
  }

  /**
   * Stop the recording and store the video.
   *
   * @param filename Video name, without extension
   * @return Destination file, which may not exist when the video could not be retrieved
   * @deprecated The returned file hides a failed retrieval. Use {@link #retrieveVideo(String)}
   */
  @Deprecated
  public File stopFFmpegAndSave(String filename) {
    retrieveVideo(filename);
    return getFileName(filename);
  }

  /**
   * Stop the recording and store the video. Never throws: a recording is evidence, never a reason to fail a test.
   *
   * @param filename Video name, without extension
   * @return The stored video, or empty when there was no recording, the recorder failed or it returned no bytes
   */
  public Optional<File> retrieveVideo(String filename) {
    if (fileIdentifier == null) {
      log.warn("No video recording was started, nothing to stop");
      return Optional.empty();
    }

    RestTemplate restTemplate = restTemplateSupplier.get();
    File destFile = getFileName(filename);
    HttpEntity<VideoRecorderStopRequest> request = new HttpEntity<>(new VideoRecorderStopRequest().setId(fileIdentifier), jsonHeaders());
    fileIdentifier = null;

    boolean stored = false;
    try {
      // Create directories if not exists
      File videoFolder = new File(VideoRecorder.conf().folder());
      if (!videoFolder.exists()) {
        Files.createDirectories(videoFolder.toPath());
      }

      // Call /stop endpoint
      log.debug("Launching /stop endpoint for video {}", destFile.getAbsolutePath());
      byte[] video = restTemplate.postForObject(getVideoRecorderUrl("/stop"), request, byte[].class);
      if (video != null && video.length > 0) {
        Files.write(destFile.toPath(), video);
        stored = true;
      } else {
        log.warn("The video recorder returned no video for {}", destFile.getAbsolutePath());
      }
    } catch (IOException | RuntimeException exc) {
      log.warn("Error trying to retrieve and store video {}", destFile.getAbsolutePath(), exc);
    }

    // Retrieve log (diagnostics only, it never discards the video)
    try {
      String videoLog = restTemplate.postForObject(getVideoRecorderUrl("/log"), request, String.class);
      log.debug(videoLog);
    } catch (RuntimeException exc) {
      log.debug("Could not retrieve the video recorder log", exc);
    }

    return stored ? Optional.of(destFile) : Optional.empty();
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

  /**
   * Headers declaring the JSON wire format the recorder service expects
   *
   * <p>The recorder parses request bodies with {@code bodyParser.json()} only, and a bare
   * {@code RestTemplate} serialises a body sent without an explicit content type with whichever
   * converter sorts first — an XML one, when an XML provider reaches the test classpath
   * transitively. The recorder then sees an empty body, {@code /start} fails with "No input
   * specified", and the follow-up {@code /stop} with a null id crashes the service
   * (aweframework/awe#742).</p>
   *
   * @return headers with the JSON content type
   */
  private HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}
