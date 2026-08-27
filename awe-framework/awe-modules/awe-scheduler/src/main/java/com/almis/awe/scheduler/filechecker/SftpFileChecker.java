package com.almis.awe.scheduler.filechecker;

import com.almis.awe.scheduler.bean.file.File;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.dao.FileDAO;
import com.almis.awe.scheduler.enums.SshHostKeyPolicy;
import com.almis.awe.scheduler.ssh.SshSupport;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;

import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Checks a scheduler file-existence trigger against a remote host over SFTP,
 * symmetrical to {@link FTPFileChecker}. Reuses the SSH credential model
 * (encrypted password / private key + passphrase) and the host-key
 * verification policy shared with the SSH command executor through
 * {@link SshSupport}, so trust decisions are consistent across both.
 * <p>
 * Never throws: a connection, authentication or listing failure is logged and
 * reported as "no changes", matching the FTP checker's contract so a temporarily
 * unreachable server does not break the scheduler polling loop.
 */
@Slf4j
public class SftpFileChecker extends Connector {

  private static final String LOG_CONTEXT = "Sftp";

  private final Duration connectTimeout;
  private final SshClient sshClient;

  /**
   * Autowired constructor
   *
   * @param fileDAO        File DAO
   * @param hostKeyPolicy  Host-key verification policy
   * @param knownHostsPath Path to the known_hosts file used to persist/read trusted host keys
   * @param connectTimeout Connect and authentication timeout
   */
  public SftpFileChecker(FileDAO fileDAO, SshHostKeyPolicy hostKeyPolicy, Path knownHostsPath, Duration connectTimeout) {
    super(fileDAO);
    this.connectTimeout = connectTimeout;
    this.sshClient = SshClient.setUpDefaultClient();
    this.sshClient.setServerKeyVerifier(SshSupport.buildServerKeyVerifier(hostKeyPolicy, knownHostsPath));
    this.sshClient.start();
  }

  @Override
  protected List<SftpClient.DirEntry> connectAndGetFiles(Task task) {
    File file = task.getFile();
    com.almis.awe.scheduler.bean.file.Server server = file.getServer();
    List<SftpClient.DirEntry> entries = new ArrayList<>();

    try (ClientSession session = sshClient.connect(server.getUser(), server.getHost(), server.getPort())
      .verify(connectTimeout)
      .getSession()) {
      SshSupport.registerIdentities(session, server, LOG_CONTEXT);
      session.auth().verify(connectTimeout);

      log.debug("[SFTP Connection] Connected to the server by SFTP: {}({})", server.getName(), server.getServerId());

      try (SftpClient sftpClient = SftpClientFactory.instance().createSftpClient(session)) {
        for (SftpClient.DirEntry entry : sftpClient.readDir(file.getFilePath())) {
          // Skip directories (including the "." and ".." entries the protocol returns), so only
          // actual files can trigger the task even when a folder name matches the file pattern
          if (!entry.getAttributes().isDirectory()) {
            entries.add(entry);
          }
        }
      }
    } catch (IOException exc) {
      log.error("[SFTP Connection] Error connecting to the server by SFTP: {}({})", server.getName(), server.getServerId(), exc);
    }

    return entries;
  }

  @Override
  public String checkForChanges(Task task) {
    String changedFile = null;
    File file = task.getFile();

    // Iterate on every file and check for changes
    for (SftpClient.DirEntry entry : connectAndGetFiles(task)) {
      Date lastModification = toDate(entry.getAttributes().getModifyTime());
      if (lastModification != null
        && checkFileModifications(task, entry.getFilename(), resolveRemotePath(file.getFilePath(), entry.getFilename()), lastModification)) {
        changedFile = entry.getFilename();
      }
    }

    // return if there are changes
    return changedFile;
  }

  /**
   * Release the SSH client's background resources on context shutdown.
   */
  @PreDestroy
  public void shutdown() {
    sshClient.stop();
  }

  /**
   * Convert an SFTP modification timestamp to a date. A server which does not report
   * the modification time leaves the attribute unset, in which case the entry cannot
   * be compared against the stored modification and is skipped.
   *
   * @param modifyTime Modification time reported by the server
   * @return Date, or null when the server did not report it
   */
  private Date toDate(FileTime modifyTime) {
    if (modifyTime == null) {
      log.warn("[SFTP Connection] Server did not report a modification time, skipping entry");
      return null;
    }
    return new Date(modifyTime.toMillis());
  }
}
