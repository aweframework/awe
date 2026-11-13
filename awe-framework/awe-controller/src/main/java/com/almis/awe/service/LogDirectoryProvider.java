package com.almis.awe.service;

import java.nio.file.Path;

/**
 * Directory whose log files the log viewer ({@code get-log-file} action) may read, besides the log manager path
 * ({@code ${logging.file.path}${awe.application.log.group}}). An application that writes logs in another
 * folder and wants to show them in the viewer declares one as a bean.
 */
@FunctionalInterface
public interface LogDirectoryProvider {

  /**
   * Retrieve the log directory
   *
   * @return Log directory
   */
  Path getLogDirectory();
}
