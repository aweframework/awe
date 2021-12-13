package com.almis.awe.testing.recorder;

import com.automation.remarks.video.SystemUtils;
import com.automation.remarks.video.recorder.VideoRecorder;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Log4j2
public class DockerFFMpegWrapper {

  public static final String RECORDING_TOOL = "ffmpeg";
  private static final String SEND_CTRL_C_TOOL_NAME = "SendSignalCtrlC.exe";
  private static final String TEM_FILE_NAME = "temporary-";
  private CompletableFuture<String> future;
  private File temporaryFile;

  public void startFFmpeg(String... args) {
    File videoFolder = new File(VideoRecorder.conf().folder());
    if (!videoFolder.exists()) {
      videoFolder.mkdirs();
    }

    this.temporaryFile = getTempFile();
    List<String> commandList = Arrays.asList(RECORDING_TOOL,
      "-y", "-an",
      "-video_size", getDockerScreenSize(),
      "-f", VideoRecorder.conf().ffmpegFormat(),
      "-i", VideoRecorder.conf().ffmpegDisplay(),
      "-framerate", String.valueOf(VideoRecorder.conf().frameRate()),
      "-pix_fmt", VideoRecorder.conf().ffmpegPixelFormat(),
      this.temporaryFile.getAbsolutePath());
    List<String> commands = new ArrayList<>(commandList);
    commands.addAll(Arrays.asList(args));
    commands.add(this.temporaryFile.getAbsolutePath());
    log.info("Launching command: {}", commands);
    this.future = CompletableFuture.supplyAsync(() -> SystemUtils.runCommand(commands));
  }

  public File stopFFmpegAndSave(String filename) {
    String killLog = killDockerFFmpeg();
    log.info("Process kill output: " + killLog);
    File destFile = getFinalFile(filename);
    this.future.whenCompleteAsync((out, errors) -> {
      log.info("Recording output log: " + out + (errors != null ? "; ex: " + errors : ""));
      try {
        Files.move(this.temporaryFile.toPath(), destFile.toPath());
      } catch (Exception exc) {
        this.temporaryFile.renameTo(destFile);
      }
      log.info("Recording finished to: " + destFile.getAbsolutePath());
    });
    return destFile;
  }

  private File getTempFile() {
    try {
      return Files.createTempFile(Paths.get(VideoRecorder.conf().folder()), TEM_FILE_NAME, getVideoFormat()).toFile();
    } catch (Exception exc) {
      return getFileName(TEM_FILE_NAME + "-" + UUID.randomUUID());
    }
  }

  public File getFinalFile(String name) {
    return this.getFileName(name);
  }

  private File getFileName(String filename) {
    String movieFolder = VideoRecorder.conf().folder();
    return Paths.get(movieFolder, filename + getVideoFormat()).toFile();
  }

  private String killDockerFFmpeg() {
    return org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS ? SystemUtils.runCommand(SEND_CTRL_C_TOOL_NAME, SystemUtils.getPidOf(RECORDING_TOOL)) : SystemUtils.runCommand("pkill", "-INT", RECORDING_TOOL);
  }

  private String getDockerScreenSize() {
    return System.getProperty("video.screen.size");
  }

  private String getVideoFormat() {
    return System.getProperty("video.file.extension");
  }

}
