package com.almis.awe.scheduler.filechecker;

import com.almis.awe.scheduler.bean.file.File;
import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.dao.FileDAO;
import com.almis.awe.scheduler.enums.SshHostKeyPolicy;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.quartz.TriggerBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Embedded-MINA test suite for SftpFileChecker: spins up an in-process SshServer
 * with the SFTP subsystem on an ephemeral port, so the file-trigger path over
 * SFTP (auth, directory listing, modification detection, host-key policy) can be
 * verified without an external host.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SftpFileCheckerTest {

  private static final String TEST_USER = "sftpuser";
  private static final String TEST_PASSWORD = "sftppass";
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

  @Mock
  private FileDAO fileDAO;

  @TempDir
  Path tempDir;

  private SshServer sshServer;
  private Path remoteRoot;
  private int port;
  private SftpFileChecker fileChecker;

  @BeforeEach
  void startEmbeddedServer() throws IOException {
    remoteRoot = Files.createDirectories(tempDir.resolve("remote"));

    sshServer = SshServer.setUpDefaultServer();
    sshServer.setPort(0);
    sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(tempDir.resolve("host-key.ser")));
    sshServer.setPasswordAuthenticator((username, password, session) -> TEST_USER.equals(username) && TEST_PASSWORD.equals(password));
    sshServer.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
    sshServer.setFileSystemFactory(new VirtualFileSystemFactory(remoteRoot));
    sshServer.start();
    port = sshServer.getPort();
  }

  @AfterEach
  void stopEmbeddedServer() throws IOException {
    if (fileChecker != null) {
      fileChecker.shutdown();
    }
    if (sshServer != null) {
      sshServer.stop(true);
    }
  }

  /**
   * A file matching the pattern which was never seen before is reported as changed
   * and registered as a new modification
   */
  @Test
  void checkForChangesDetectsNewFile() throws Exception {
    Files.writeString(remoteRoot.resolve("report.txt"), "content");
    fileChecker = checker(SshHostKeyPolicy.ACCEPT_ALL);

    String changedFile = fileChecker.checkForChanges(task(passwordServer()));

    assertEquals("report.txt", changedFile);
    verify(fileDAO).addModification(any(Task.class), eq("/report.txt"), any(Date.class), eq(false));
  }

  /**
   * A file which does not match the configured pattern is ignored
   */
  @Test
  void checkForChangesIgnoresNonMatchingFile() throws Exception {
    Files.writeString(remoteRoot.resolve("report.pom"), "content");
    fileChecker = checker(SshHostKeyPolicy.ACCEPT_ALL);

    String changedFile = fileChecker.checkForChanges(task(passwordServer()));

    assertNull(changedFile);
    verify(fileDAO, never()).addModification(any(Task.class), anyString(), any(Date.class), anyBoolean());
  }

  /**
   * An already known file whose modification date moved forward is reported as changed
   */
  @Test
  void checkForChangesDetectsModifiedFile() throws Exception {
    Files.writeString(remoteRoot.resolve("report.txt"), "content");
    fileChecker = checker(SshHostKeyPolicy.ACCEPT_ALL);

    Task task = task(passwordServer());
    Map<String, Date> modifications = new HashMap<>();
    modifications.put("/report.txt", new Date(0));
    task.getFile().setFileModifications(modifications);

    String changedFile = fileChecker.checkForChanges(task);

    assertEquals("report.txt", changedFile);
    verify(fileDAO).addModification(any(Task.class), eq("/report.txt"), any(Date.class), eq(true));
  }

  /**
   * Directories are never reported as trigger files, even when their name matches the pattern
   */
  @Test
  void checkForChangesIgnoresDirectories() throws Exception {
    Files.createDirectories(remoteRoot.resolve("folder.txt"));
    fileChecker = checker(SshHostKeyPolicy.ACCEPT_ALL);

    String changedFile = fileChecker.checkForChanges(task(passwordServer()));

    assertNull(changedFile);
    verify(fileDAO, never()).addModification(any(Task.class), anyString(), any(Date.class), anyBoolean());
  }

  /**
   * Authentication with a private key identity works, mirroring the SSH command executor
   */
  @Test
  void checkForChangesAuthenticatesWithPrivateKey() throws Exception {
    Files.writeString(remoteRoot.resolve("report.txt"), "content");
    sshServer.setPublickeyAuthenticator((username, key, session) -> TEST_USER.equals(username));
    fileChecker = checker(SshHostKeyPolicy.ACCEPT_ALL);

    Server server = new Server()
      .setServerId(1)
      .setName("sftp-server")
      .setHost("127.0.0.1")
      .setPort(port)
      .setTypeOfConnection("SSH")
      .setUser(TEST_USER)
      .setKey(privateKeyPem(generateKeyPair()));

    String changedFile = fileChecker.checkForChanges(task(server));

    assertEquals("report.txt", changedFile);
  }

  /**
   * A connection failure never propagates: the checker reports no changes
   */
  @Test
  void checkForChangesReturnsNullWhenConnectionFails() throws Exception {
    fileChecker = checker(SshHostKeyPolicy.ACCEPT_ALL);
    sshServer.stop(true);

    String changedFile = fileChecker.checkForChanges(task(passwordServer()));

    assertNull(changedFile);
    verify(fileDAO, never()).addModification(any(Task.class), anyString(), any(Date.class), anyBoolean());
  }

  /**
   * A wrong password never propagates either
   */
  @Test
  void checkForChangesReturnsNullOnAuthenticationFailure() throws Exception {
    Files.writeString(remoteRoot.resolve("report.txt"), "content");
    fileChecker = checker(SshHostKeyPolicy.ACCEPT_ALL);

    Server server = passwordServer().setPassword("wrong-password");

    assertNull(fileChecker.checkForChanges(task(server)));
  }

  /**
   * A STRICT host-key policy with no pre-provisioned known_hosts entry rejects the host
   */
  @Test
  void checkForChangesRejectsUnknownHostUnderStrictPolicy() throws Exception {
    Files.writeString(remoteRoot.resolve("report.txt"), "content");
    fileChecker = checker(SshHostKeyPolicy.STRICT);

    String changedFile = fileChecker.checkForChanges(task(passwordServer()));

    assertNull(changedFile);
    verify(fileDAO, never()).addModification(any(Task.class), anyString(), any(Date.class), anyBoolean());
  }

  /**
   * The trust-on-first-use policy persists the accepted host key to known_hosts
   */
  @Test
  void checkForChangesPersistsHostKeyOnFirstUse() throws Exception {
    Files.writeString(remoteRoot.resolve("report.txt"), "content");
    Path knownHosts = tempDir.resolve("known_hosts_tofu/known_hosts");
    fileChecker = new SftpFileChecker(fileDAO, SshHostKeyPolicy.ACCEPT_ON_FIRST_USE, knownHosts, CONNECT_TIMEOUT);

    assertEquals("report.txt", fileChecker.checkForChanges(task(passwordServer())));
    assertNotNull(knownHosts);
    assertEquals(true, Files.exists(knownHosts));
  }

  /**
   * A configured directory without a trailing separator must still produce a
   * well-formed remote path, so the tracked path identifies the file on the server.
   */
  @Test
  void checkForChangesTracksWellFormedPathWithoutTrailingSeparator() throws Exception {
    Files.createDirectories(remoteRoot.resolve("incoming"));
    Files.writeString(remoteRoot.resolve("incoming/report.txt"), "content");
    fileChecker = checker(SshHostKeyPolicy.ACCEPT_ALL);

    Task task = task(passwordServer());
    task.getFile().setFilePath("/incoming");

    String changedFile = fileChecker.checkForChanges(task);

    assertEquals("report.txt", changedFile);
    verify(fileDAO).addModification(any(Task.class), eq("/incoming/report.txt"), any(Date.class), eq(false));
  }

  private SftpFileChecker checker(SshHostKeyPolicy policy) {
    return new SftpFileChecker(fileDAO, policy, tempDir.resolve(policy.name() + "/known_hosts"), CONNECT_TIMEOUT);
  }

  private java.security.KeyPair generateKeyPair() throws Exception {
    java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    return generator.generateKeyPair();
  }

  private String privateKeyPem(java.security.KeyPair keyPair) throws Exception {
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter.INSTANCE
      .writePrivateKey(keyPair, "test-key",
        (org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyEncryptionContext) null, out);
    return out.toString(java.nio.charset.StandardCharsets.UTF_8);
  }

  private Server passwordServer() {
    return new Server()
      .setServerId(1)
      .setName("sftp-server")
      .setHost("127.0.0.1")
      .setPort(port)
      .setTypeOfConnection("SSH")
      .setUser(TEST_USER)
      .setPassword(TEST_PASSWORD);
  }

  private Task task(Server server) {
    Task task = new Task();
    task.setTrigger(TriggerBuilder.newTrigger().build());
    task.setFile(new File()
      .setFilePattern(".*\\.txt")
      .setFilePath("/")
      .setFileServerId(server.getServerId())
      .setFileModifications(new HashMap<>())
      .setServer(server));
    return task;
  }
}
