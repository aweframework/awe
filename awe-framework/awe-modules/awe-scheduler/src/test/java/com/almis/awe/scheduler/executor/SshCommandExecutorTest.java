package com.almis.awe.scheduler.executor;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.dao.ServerDAO;
import com.almis.awe.scheduler.enums.SshHostKeyPolicy;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyEncryptionContext;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.common.util.threads.ThreadUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.command.AbstractCommandSupport;
import org.apache.sshd.server.command.CommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.TriggerBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Embedded-MINA test suite for SshCommandExecutor: spins up an in-process
 * SshServer on an ephemeral port with a scripted CommandFactory so the SSH
 * exec path (auth, command construction, exit code, host-key policy) can be
 * verified without an external host.
 */
@ExtendWith(MockitoExtension.class)
class SshCommandExecutorTest {

  private static final String TEST_USER = "sshuser";
  private static final String TEST_PASSWORD = "sshpass";
  // Well beyond the ~2MB default SSH channel window so the drain ordering actually matters.
  private static final int LARGE_OUTPUT_BYTES = 4 * 1024 * 1024;

  @Mock
  private ServerDAO serverDAO;

  @Mock
  private CommandStreamLogger commandStreamLogger;

  @TempDir
  Path tempDir;

  private SshServer sshServer;
  private int port;

  @BeforeEach
  void startEmbeddedServer() throws IOException {
    sshServer = SshServer.setUpDefaultServer();
    sshServer.setPort(0);
    sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(tempDir.resolve("host-key.ser")));
    sshServer.setPasswordAuthenticator((username, password, session) -> TEST_USER.equals(username) && TEST_PASSWORD.equals(password));
    sshServer.setCommandFactory(scriptedCommandFactory());
    sshServer.start();
    port = sshServer.getPort();
  }

  @AfterEach
  void stopEmbeddedServer() throws IOException {
    if (sshServer != null) {
      sshServer.stop(true);
    }
  }

  @Test
  void successfulExecutionReturnsExitCodeAndLogsOutput() throws AWException {
    SshCommandExecutor executor = newExecutor(SshHostKeyPolicy.ACCEPT_ON_FIRST_USE, Duration.ofSeconds(10));
    given(serverDAO.findServer(1)).willReturn(sshServer(TEST_PASSWORD));

    Task task = generateTask(1, "echo-test", new ArrayList<>());

    Integer exitCode = executor.execute(task, new String[0], 10);

    assertEquals(0, exitCode);
    assertEquals("RECEIVED:echo-test\n", captureOutput(task));
  }

  @Test
  void nonZeroExitCodeIsPropagatedAndStderrLogged() throws AWException {
    SshCommandExecutor executor = newExecutor(SshHostKeyPolicy.ACCEPT_ON_FIRST_USE, Duration.ofSeconds(10));
    given(serverDAO.findServer(1)).willReturn(sshServer(TEST_PASSWORD));

    Task task = generateTask(1, "FAIL_EXIT", new ArrayList<>());

    Integer exitCode = executor.execute(task, new String[0], 10);

    assertEquals(7, exitCode);
    assertEquals("boom\n", captureError(task));
  }

  @Test
  void parametersAreAppendedToRemoteCommand() throws AWException {
    SshCommandExecutor executor = newExecutor(SshHostKeyPolicy.ACCEPT_ON_FIRST_USE, Duration.ofSeconds(10));
    given(serverDAO.findServer(1)).willReturn(sshServer(TEST_PASSWORD));

    ArrayList<TaskParameter> parameters = new ArrayList<>();
    parameters.add(new TaskParameter().setValue("param1"));
    parameters.add(new TaskParameter().setValue("param2"));
    Task task = generateTask(1, "myscript", parameters);

    Integer exitCode = executor.execute(task, new String[0], 10);

    assertEquals(0, exitCode);
    assertEquals("RECEIVED:myscript param1 param2\n", captureOutput(task));
  }

  @Test
  void commandWithCommandPathChangesDirectoryAndRunsFromPath() throws AWException {
    SshCommandExecutor executor = newExecutor(SshHostKeyPolicy.ACCEPT_ON_FIRST_USE, Duration.ofSeconds(10));
    given(serverDAO.findServer(1)).willReturn(sshServer(TEST_PASSWORD));

    Task task = generateTask(1, "run.sh", new ArrayList<>());
    task.setCommandPath("/opt/app");

    Integer exitCode = executor.execute(task, new String[0], 10);

    assertEquals(0, exitCode);
    assertEquals("RECEIVED:cd '/opt/app' && run.sh\n", captureOutput(task));
  }

  @Test
  void blankCommandPathRunsCommandDirectlyWithoutCd() throws AWException {
    SshCommandExecutor executor = newExecutor(SshHostKeyPolicy.ACCEPT_ON_FIRST_USE, Duration.ofSeconds(10));
    given(serverDAO.findServer(1)).willReturn(sshServer(TEST_PASSWORD));

    Task task = generateTask(1, "run.sh", new ArrayList<>());
    task.setCommandPath("");

    Integer exitCode = executor.execute(task, new String[0], 10);

    assertEquals(0, exitCode);
    assertEquals("RECEIVED:run.sh\n", captureOutput(task));
  }

  @Test
  void authenticationFailureReturnsNonZeroWithoutThrowing() throws AWException {
    SshCommandExecutor executor = newExecutor(SshHostKeyPolicy.ACCEPT_ON_FIRST_USE, Duration.ofSeconds(10));
    given(serverDAO.findServer(1)).willReturn(sshServer("wrong-password"));

    Task task = generateTask(1, "echo-test", new ArrayList<>());

    Integer exitCode = executor.execute(task, new String[0], 10);

    assertEquals(1, exitCode);
  }

  @Test
  void timeoutClosesChannelAndReturnsNonZero() throws AWException {
    SshCommandExecutor executor = newExecutor(SshHostKeyPolicy.ACCEPT_ON_FIRST_USE, Duration.ofSeconds(10));
    given(serverDAO.findServer(1)).willReturn(sshServer(TEST_PASSWORD));

    Task task = generateTask(1, "SLEEP_LONG", new ArrayList<>());

    Integer exitCode = executor.execute(task, new String[0], 1);

    assertEquals(1, exitCode);
  }

  @Test
  void strictPolicyWithEmptyKnownHostsRejectsUnknownHost() throws AWException {
    SshCommandExecutor executor = newExecutor(SshHostKeyPolicy.STRICT, Duration.ofSeconds(10));
    given(serverDAO.findServer(1)).willReturn(sshServer(TEST_PASSWORD));

    Task task = generateTask(1, "echo-test", new ArrayList<>());

    Integer exitCode = executor.execute(task, new String[0], 10);

    assertEquals(1, exitCode);
  }

  @Test
  void concurrentExecutionsOnSameExecutorDoNotInterfere() throws AWException {
    SshCommandExecutor executor = newExecutor(SshHostKeyPolicy.ACCEPT_ON_FIRST_USE, Duration.ofSeconds(10));
    given(serverDAO.findServer(1)).willReturn(sshServer(TEST_PASSWORD));

    Task taskA = generateTask(1, "echo-A", new ArrayList<>());
    Task taskB = generateTask(1, "echo-B", new ArrayList<>());

    CompletableFuture<Integer> futureA = CompletableFuture.supplyAsync(() -> executor.execute(taskA, new String[0], 10));
    CompletableFuture<Integer> futureB = CompletableFuture.supplyAsync(() -> executor.execute(taskB, new String[0], 10));

    assertEquals(0, futureA.join());
    assertEquals(0, futureB.join());
  }

  @Test
  void largeOutputBeyondChannelWindowDoesNotTimeout() throws AWException {
    // A command that emits several MB before exiting overflows the SSH channel window (~2MB).
    // If stdout/stderr are only read AFTER waitFor(CLOSED), the remote back-pressures and CLOSED
    // never arrives -> false timeout. Draining concurrently before waitFor must return exit 0.
    CountingStreamLogger drainingLogger = new CountingStreamLogger();
    SshCommandExecutor executor = new SshCommandExecutor(drainingLogger, serverDAO,
        SshHostKeyPolicy.ACCEPT_ON_FIRST_USE, tempDir.resolve("known_hosts"), Duration.ofSeconds(10));
    given(serverDAO.findServer(1)).willReturn(sshServer(TEST_PASSWORD));

    Task task = generateTask(1, "LARGE_OUTPUT", new ArrayList<>());

    Integer exitCode = executor.execute(task, new String[0], 15);

    assertEquals(0, exitCode, "Large output must not cause a false timeout");
    assertTrue(drainingLogger.bytesFor("OUTPUT") >= LARGE_OUTPUT_BYTES,
        "Full command output must be drained/captured (got " + drainingLogger.bytesFor("OUTPUT") + " bytes)");
  }

  @Test
  void keyAuthenticationWithoutPasswordRunsCommand() throws Exception {
    KeyPair keyPair = generateKeyPair();
    sshServer.setPublickeyAuthenticator((username, publicKey, session) -> TEST_USER.equals(username));

    SshCommandExecutor executor = newExecutor(SshHostKeyPolicy.ACCEPT_ON_FIRST_USE, Duration.ofSeconds(10));
    given(serverDAO.findServer(1)).willReturn(sshServerWithKey(privateKeyPem(keyPair, null), null));

    Task task = generateTask(1, "echo-test", new ArrayList<>());

    Integer exitCode = executor.execute(task, new String[0], 10);

    assertEquals(0, exitCode);
    assertEquals("RECEIVED:echo-test\n", captureOutput(task));
  }

  @Test
  void encryptedKeyAuthenticationUsesConfiguredPassphrase() throws Exception {
    String passphrase = "keypass";
    KeyPair keyPair = generateKeyPair();
    sshServer.setPublickeyAuthenticator((username, publicKey, session) -> TEST_USER.equals(username));

    SshCommandExecutor executor = newExecutor(SshHostKeyPolicy.ACCEPT_ON_FIRST_USE, Duration.ofSeconds(10));
    given(serverDAO.findServer(1)).willReturn(sshServerWithKey(privateKeyPem(keyPair, passphrase), passphrase));

    Task task = generateTask(1, "echo-test", new ArrayList<>());

    Integer exitCode = executor.execute(task, new String[0], 10);

    assertEquals(0, exitCode);
    assertEquals("RECEIVED:echo-test\n", captureOutput(task));
  }

  @Test
  void ed25519KeyAuthenticationRunsCommand() throws Exception {
    KeyPair keyPair = SecurityUtils.getKeyPairGenerator(SecurityUtils.EDDSA).generateKeyPair();
    sshServer.setPublickeyAuthenticator((username, publicKey, session) -> TEST_USER.equals(username));

    SshCommandExecutor executor = newExecutor(SshHostKeyPolicy.ACCEPT_ON_FIRST_USE, Duration.ofSeconds(10));
    given(serverDAO.findServer(1)).willReturn(sshServerWithKey(privateKeyPem(keyPair, null), null));

    Task task = generateTask(1, "echo-test", new ArrayList<>());

    Integer exitCode = executor.execute(task, new String[0], 10);

    assertEquals(0, exitCode);
    assertEquals("RECEIVED:echo-test\n", captureOutput(task));
  }

  private String captureOutput(Task task) {
    ArgumentCaptor<InputStream> captor = ArgumentCaptor.forClass(InputStream.class);
    verify(commandStreamLogger).log(eq(task), captor.capture(), eq("OUTPUT"));
    return readAll(captor.getValue());
  }

  private String captureError(Task task) {
    ArgumentCaptor<InputStream> captor = ArgumentCaptor.forClass(InputStream.class);
    verify(commandStreamLogger).log(eq(task), captor.capture(), eq("ERROR"));
    return readAll(captor.getValue());
  }

  private String readAll(InputStream inputStream) {
    try {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exc) {
      throw new IllegalStateException(exc);
    }
  }

  private SshCommandExecutor newExecutor(SshHostKeyPolicy hostKeyPolicy, Duration connectTimeout) {
    Path knownHostsPath = tempDir.resolve("known_hosts");
    return new SshCommandExecutor(commandStreamLogger, serverDAO, hostKeyPolicy, knownHostsPath, connectTimeout);
  }

  private Server sshServer(String password) {
    Server server = new Server();
    server.setTypeOfConnection("ssh");
    server.setHost("localhost");
    server.setPort(port);
    server.setActive(true);
    server.setUser(TEST_USER);
    server.setPassword(password);
    return server;
  }

  private Server sshServerWithKey(String privateKey, String passphrase) {
    Server server = new Server();
    server.setTypeOfConnection("ssh");
    server.setHost("localhost");
    server.setPort(port);
    server.setActive(true);
    server.setUser(TEST_USER);
    server.setKey(privateKey);
    server.setKeyPassphrase(passphrase);
    return server;
  }

  private KeyPair generateKeyPair() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    return generator.generateKeyPair();
  }

  private String privateKeyPem(KeyPair keyPair, String passphrase) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    if (passphrase == null) {
      OpenSSHKeyPairResourceWriter.INSTANCE.writePrivateKey(keyPair, "test-key", (OpenSSHKeyEncryptionContext) null, out);
    } else {
      OpenSSHKeyEncryptionContext context = new OpenSSHKeyEncryptionContext();
      context.setCipherName("AES");
      context.setCipherType("256");
      context.setCipherMode("CTR");
      context.setPassword(passphrase);
      OpenSSHKeyPairResourceWriter.INSTANCE.writePrivateKey(keyPair, "test-key", context, out);
    }
    return out.toString(StandardCharsets.UTF_8);
  }

  private Task generateTask(Integer serverId, String action, ArrayList<TaskParameter> parameters) {
    Task task = new Task();
    task.setServerId(serverId);
    task.setAction(action);
    task.setParameterList(parameters);
    task.setTrigger(TriggerBuilder.newTrigger().build());
    return task;
  }

  /**
   * Scripted server-side command used by the embedded SshServer: interprets
   * a small set of magic action names so tests can assert exit codes,
   * stdout/stderr content, and slow (timeout-triggering) commands without
   * depending on an external OS shell.
   */
  private CommandFactory scriptedCommandFactory() {
    return (channelSession, command) -> new ScriptedCommand(command);
  }

  /**
   * Real (non-mock) stream logger that fully drains the stream it is given and records how many
   * bytes it read per type. Used by the large-output test so the drain threads actually consume
   * the channel window (a Mockito mock would no-op and never relieve back-pressure).
   */
  private static final class CountingStreamLogger extends CommandStreamLogger {

    private final AtomicLong outputBytes = new AtomicLong();
    private final AtomicLong errorBytes = new AtomicLong();

    @Override
    public void log(Task task, InputStream inputStream, String type) {
      AtomicLong counter = "OUTPUT".equals(type) ? outputBytes : errorBytes;
      byte[] buffer = new byte[8192];
      try {
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
          counter.addAndGet(read);
        }
      } catch (IOException exc) {
        // Test-only drain: swallow, the assertion on captured bytes reflects what was read.
      }
    }

    private long bytesFor(String type) {
      return "OUTPUT".equals(type) ? outputBytes.get() : errorBytes.get();
    }
  }

  private static final class ScriptedCommand extends AbstractCommandSupport {

    private ScriptedCommand(String command) {
      super(command, ThreadUtils.newCachedThreadPool("ssh-test-" + Integer.toHexString(command.hashCode())));
    }

    @Override
    public void run() {
      String command = getCommand();
      int exit = 0;
      try {
        OutputStream out = getOutputStream();
        OutputStream err = getErrorStream();
        if (command.contains("SLEEP_LONG")) {
          Thread.sleep(3000);
        } else if (command.contains("LARGE_OUTPUT")) {
          byte[] chunk = new byte[8192];
          java.util.Arrays.fill(chunk, (byte) 'x');
          int written = 0;
          while (written < LARGE_OUTPUT_BYTES) {
            out.write(chunk);
            written += chunk.length;
          }
          out.flush();
        } else if (command.contains("FAIL_EXIT")) {
          err.write("boom\n".getBytes(StandardCharsets.UTF_8));
          err.flush();
          exit = 7;
        } else {
          out.write(("RECEIVED:" + command + "\n").getBytes(StandardCharsets.UTF_8));
          out.flush();
        }
      } catch (Exception exc) {
        exit = 1;
      }
      onExit(exit);
    }
  }
}
