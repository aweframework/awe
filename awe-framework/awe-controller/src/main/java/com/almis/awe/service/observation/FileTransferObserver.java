package com.almis.awe.service.observation;

/**
 * Observer of the files the users upload and download, for example to measure them. AWE calls it once per file, after
 * the file has been stored or sent. Without an observer bean nothing is recorded.
 */
@FunctionalInterface
public interface FileTransferObserver {

  /**
   * Observer that does nothing
   */
  FileTransferObserver NONE = (direction, bytes) -> {
  };

  /**
   * Direction of a file transfer
   */
  enum Direction {
    /**
     * File uploaded by the user
     */
    UPLOAD,
    /**
     * File sent to the user
     */
    DOWNLOAD
  }

  /**
   * A file has been transferred
   *
   * @param direction Upload or download
   * @param bytes     Size of the file in bytes
   */
  void transferred(Direction direction, long bytes);
}
