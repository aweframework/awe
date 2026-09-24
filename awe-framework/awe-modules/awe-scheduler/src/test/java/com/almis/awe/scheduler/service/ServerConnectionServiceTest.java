package com.almis.awe.scheduler.service;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.scheduler.dao.ServerDAO;
import com.almis.awe.scheduler.enums.SshHostKeyPolicy;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Connection test suite for the scheduler server screens: an embedded in-process
 * MINA SshServer on an ephemeral port exercises the SSH path (auth outcomes and
 * host-key policy), a mocked FTPClient exercises the FTP path, and a mocked
 * ServerDAO proves the update-screen fallback to stored credentials. The service
 * must never persist anything: it has no maintain collaborator at all. Every
 * outcome is reported back to the screen as client actions: the in-flight
 * spinner is swapped back to the plug icon and the button gets a green/red
 * state class; success is otherwise silent while failures carry the localized
 * diagnosis as a message.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServerConnectionServiceTest {

  private static final String TEST_USER = "sshuser";
  private static final String TEST_PASSWORD = "sshpass";
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final String BUTTON_SELECTOR = "#ButTstCnx";
  private static final String BUTTON_ICON_SELECTOR = "#ButTstCnx i";

  @Mock
  private ServerDAO serverDAO;
  @Mock
  private FTPClient ftpClient;
  @Mock
  private AweElements aweElements;
  @Mock
  private ApplicationContext context;

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
    sshServer.start();
    port = sshServer.getPort();
  }

  @AfterEach
  void stopEmbeddedServer() throws IOException {
    if (sshServer != null) {
      sshServer.stop(true);
    }
  }

  /**
   * A reachable SSH server accepting the typed password reports success:
   * green button and localized success message
   */
  @Test
  void sshPasswordAuthenticationReportsSuccess() throws Exception {
    ServerConnectionService service = service(SshHostKeyPolicy.ACCEPT_ALL);

    ServiceData result = service.checkServerConnection("ssh", "127.0.0.1", port, TEST_USER, TEST_PASSWORD, null, null, null);

    assertSuccess(result);
  }

  /**
   * A private key typed in the form authenticates without a password
   */
  @Test
  void sshPrivateKeyAuthenticationReportsSuccess() throws Exception {
    sshServer.setPublickeyAuthenticator((username, key, session) -> TEST_USER.equals(username));
    ServerConnectionService service = service(SshHostKeyPolicy.ACCEPT_ALL);

    ServiceData result = service.checkServerConnection("ssh", "127.0.0.1", port, TEST_USER, null, privateKeyPem(), null, null);

    assertSuccess(result);
  }

  /**
   * A wrong password is reported as an authentication failure, not a generic error
   */
  @Test
  void sshWrongPasswordReportsAuthenticationFailure() throws Exception {
    ServerConnectionService service = service(SshHostKeyPolicy.ACCEPT_ALL);

    ServiceData result = service.checkServerConnection("ssh", "127.0.0.1", port, TEST_USER, "wrong-password", null, null, null);

    assertFailure(result, "SCHEDULER_SERVER_TEST_AUTHENTICATION_ERROR");
  }

  /**
   * An unreachable host/port is reported as a connection failure
   */
  @Test
  void sshUnreachablePortReportsConnectionFailure() throws Exception {
    sshServer.stop(true);
    ServerConnectionService service = service(SshHostKeyPolicy.ACCEPT_ALL);

    ServiceData result = service.checkServerConnection("ssh", "127.0.0.1", port, TEST_USER, TEST_PASSWORD, null, null, null);

    assertFailure(result, "SCHEDULER_SERVER_TEST_CONNECTION_ERROR");
  }

  /**
   * Under the STRICT policy an unknown host is reported as a host-key rejection,
   * distinct from an authentication failure
   */
  @Test
  void sshUnknownHostUnderStrictPolicyReportsHostKeyRejection() throws Exception {
    ServerConnectionService service = service(SshHostKeyPolicy.STRICT);

    ServiceData result = service.checkServerConnection("ssh", "127.0.0.1", port, TEST_USER, TEST_PASSWORD, null, null, null);

    assertFailure(result, "SCHEDULER_SERVER_TEST_HOST_KEY_ERROR");
  }

  /**
   * The trust-on-first-use policy persists the accepted host key, proving the
   * configured host-key machinery drives the test connection too
   */
  @Test
  void sshFirstUsePolicyPersistsHostKey() throws Exception {
    Path knownHosts = tempDir.resolve("tofu/known_hosts");
    ServerConnectionService service = new ServerConnectionService(serverDAO, ftpClient,
      SshHostKeyPolicy.ACCEPT_ON_FIRST_USE, knownHosts, CONNECT_TIMEOUT);
    mockLocales(service);

    ServiceData result = service.checkServerConnection("ssh", "127.0.0.1", port, TEST_USER, TEST_PASSWORD, null, null, null);

    assertSuccess(result);
    assertTrue(Files.exists(knownHosts));
  }

  /**
   * A private key that cannot be parsed is reported as an invalid key,
   * before any connection is attempted
   */
  @Test
  void sshGarbageKeyReportsInvalidKey() throws Exception {
    ServerConnectionService service = service(SshHostKeyPolicy.ACCEPT_ALL);

    ServiceData result = service.checkServerConnection("ssh", "127.0.0.1", port, TEST_USER, null, "this is not a private key", null, null);

    assertFailure(result, "SCHEDULER_SERVER_TEST_INVALID_KEY_ERROR");
  }

  /**
   * FTP with credentials: connect + login + disconnect, reported as success,
   * with the interactive check timeout applied to connect and data exchanges
   */
  @Test
  void ftpLoginReportsSuccessAndDisconnects() throws Exception {
    when(ftpClient.login("ftpuser", "ftppass")).thenReturn(true);
    ServerConnectionService service = service(SshHostKeyPolicy.ACCEPT_ALL);

    ServiceData result = service.checkServerConnection("ftp", "127.0.0.1", 21, "ftpuser", "ftppass", null, null, null);

    assertSuccess(result);
    verify(ftpClient).setConnectTimeout(10000);
    verify(ftpClient).setDefaultTimeout(10000);
    verify(ftpClient).setDataTimeout(10000);
    verify(ftpClient).connect("127.0.0.1", 21);
    verify(ftpClient).logout();
    verify(ftpClient).disconnect();
    verifyNoInteractions(serverDAO);
  }

  /**
   * FTP without a user attempts an anonymous connection and never logs in
   */
  @Test
  void ftpWithoutUserSkipsLogin() throws Exception {
    ServerConnectionService service = service(SshHostKeyPolicy.ACCEPT_ALL);

    ServiceData result = service.checkServerConnection("ftp", "127.0.0.1", 21, null, null, null, null, null);

    assertSuccess(result);
    verify(ftpClient, never()).login(anyString(), any());
  }

  /**
   * An FTP login rejection is reported as an authentication failure
   */
  @Test
  void ftpLoginRejectionReportsAuthenticationFailure() throws Exception {
    when(ftpClient.login("ftpuser", "wrong")).thenReturn(false);
    ServerConnectionService service = service(SshHostKeyPolicy.ACCEPT_ALL);

    ServiceData result = service.checkServerConnection("ftp", "127.0.0.1", 21, "ftpuser", "wrong", null, null, null);

    assertFailure(result, "SCHEDULER_SERVER_TEST_AUTHENTICATION_ERROR");
    verify(ftpClient).disconnect();
  }

  /**
   * An FTP connect error is reported as a connection failure
   */
  @Test
  void ftpConnectErrorReportsConnectionFailure() throws Exception {
    doThrow(new IOException("Connection refused")).when(ftpClient).connect("127.0.0.1", 21);
    ServerConnectionService service = service(SshHostKeyPolicy.ACCEPT_ALL);

    ServiceData result = service.checkServerConnection("ftp", "127.0.0.1", 21, "ftpuser", "ftppass", null, null, null);

    assertFailure(result, "SCHEDULER_SERVER_TEST_CONNECTION_ERROR");
  }

  /**
   * A connect failure followed by the legacy client's NullPointerException on
   * logout (QUIT over a control connection that never opened) still reports the
   * connection failure: the cleanup can never replace the outcome
   */
  @Test
  void ftpCleanupFailureAfterConnectErrorStillReportsConnectionFailure() throws Exception {
    doThrow(new IOException("Connection refused")).when(ftpClient).connect("8.8.8.8", 21);
    doThrow(new NullPointerException("this._controlOutput is null")).when(ftpClient).logout();
    ServerConnectionService service = service(SshHostKeyPolicy.ACCEPT_ALL);

    ServiceData result = service.checkServerConnection("ftp", "8.8.8.8", 21, "ftpuser", "ftppass", null, null, null);

    assertFailure(result, "SCHEDULER_SERVER_TEST_CONNECTION_ERROR");
  }

  /**
   * A runtime failure raised by the legacy FTP client inside the check itself
   * is reported as a connection failure, never leaked as a raw error
   */
  @Test
  void ftpRuntimeFailureReportsConnectionFailure() throws Exception {
    doThrow(new NullPointerException("this._controlOutput is null")).when(ftpClient).connect("8.8.8.8", 21);
    ServerConnectionService service = service(SshHostKeyPolicy.ACCEPT_ALL);

    ServiceData result = service.checkServerConnection("ftp", "8.8.8.8", 21, "ftpuser", "ftppass", null, null, null);

    assertFailure(result, "SCHEDULER_SERVER_TEST_CONNECTION_ERROR");
  }

  /**
   * Folder servers resolve to a path on the scheduler host per task, so there is
   * no server-level connection to test: the outcome says so explicitly, with no
   * green/red state on the button
   */
  @Test
  void folderReportsNothingToTest() throws Exception {
    ServerConnectionService service = service(SshHostKeyPolicy.ACCEPT_ALL);

    ServiceData result = service.checkServerConnection("folder", "localhost", 0, null, null, null, null, null);

    assertEquals("SCHEDULER_SERVER_TEST_FOLDER_TITLE", result.getTitle());
    assertEquals("SCHEDULER_SERVER_TEST_FOLDER_MESSAGE", result.getMessage());
    assertMessageAction(result, "warning", "SCHEDULER_SERVER_TEST_FOLDER_TITLE", "SCHEDULER_SERVER_TEST_FOLDER_MESSAGE");
    assertIconRestored(result);
    assertNoStateClass(result);
    verifyNoInteractions(ftpClient);
  }

  /**
   * An unknown connection type is reported explicitly instead of silently succeeding
   */
  @Test
  void unknownTypeReportsUnsupported() throws Exception {
    ServerConnectionService service = service(SshHostKeyPolicy.ACCEPT_ALL);

    ServiceData result = service.checkServerConnection("gopher", "127.0.0.1", 70, null, null, null, null, null);

    assertFailure(result, "SCHEDULER_SERVER_TEST_UNSUPPORTED_TYPE");
  }

  /**
   * Update screen: secrets are rendered blank and mean "keep the stored value",
   * so a blank password with a server identifier tests the stored credential
   */
  @Test
  void blankSecretsWithServerIdFallBackToStoredCredentials() throws Exception {
    when(serverDAO.findServer(7)).thenReturn(new Server().setServerId(7).setPassword(TEST_PASSWORD));
    ServerConnectionService service = service(SshHostKeyPolicy.ACCEPT_ALL);

    ServiceData result = service.checkServerConnection("ssh", "127.0.0.1", port, TEST_USER, "", null, null, 7);

    assertSuccess(result);
    verify(serverDAO).findServer(7);
  }

  /**
   * Servers list screen: with only the selected row identifier (no connection
   * fields), the whole server is loaded from the stored record and tested
   */
  @Test
  void idOnlyInvocationTestsTheStoredServer() throws Exception {
    when(serverDAO.findServer(9)).thenReturn(new Server()
      .setServerId(9)
      .setTypeOfConnection("ssh")
      .setHost("127.0.0.1")
      .setPort(port)
      .setUser(TEST_USER)
      .setPassword(TEST_PASSWORD));
    ServerConnectionService service = service(SshHostKeyPolicy.ACCEPT_ALL);

    ServiceData result = service.checkServerConnection(null, null, null, null, null, null, null, 9);

    assertSuccess(result);
    verify(serverDAO).findServer(9);
  }

  /**
   * Servers list screen: an identifier that no longer resolves to a stored
   * server is reported as a localized failure, never a crash
   */
  @Test
  void idOnlyInvocationWithUnknownIdReportsServerNotFound() throws Exception {
    when(serverDAO.findServer(99)).thenReturn(null);
    ServerConnectionService service = service(SshHostKeyPolicy.ACCEPT_ALL);

    ServiceData result = service.checkServerConnection(null, null, null, null, null, null, null, 99);

    assertFailure(result, "SCHEDULER_SERVER_TEST_SERVER_NOT_FOUND");
  }

  /**
   * Update screen: a typed secret always wins over the stored one, so edited
   * credentials are validated before saving
   */
  @Test
  void typedSecretsOverrideStoredCredentials() throws Exception {
    when(serverDAO.findServer(7)).thenReturn(new Server().setServerId(7).setPassword("stored-obsolete-password"));
    ServerConnectionService service = service(SshHostKeyPolicy.ACCEPT_ALL);

    ServiceData result = service.checkServerConnection("ssh", "127.0.0.1", port, TEST_USER, TEST_PASSWORD, null, null, 7);

    assertSuccess(result);
  }

  private void assertSuccess(ServiceData result) {
    assertNoMessageAction(result);
    assertIconRestored(result);
    assertStateClass(result, "btn-success p-button-success");
  }

  private void assertFailure(ServiceData result, String messageKey) {
    assertEquals("SCHEDULER_SERVER_TEST_KO_TITLE", result.getTitle());
    assertEquals(messageKey, result.getMessage());
    assertMessageAction(result, "error", "SCHEDULER_SERVER_TEST_KO_TITLE", messageKey);
    assertIconRestored(result);
    assertStateClass(result, "btn-danger p-button-danger");
  }

  private void assertMessageAction(ServiceData result, String severity, String title, String message) {
    ClientAction action = findAction(result, "message", null);
    assertEquals(severity, action.getParameters().get("type"));
    assertEquals(title, action.getParameters().get("title"));
    assertEquals(message, action.getParameters().get("message"));
  }

  private void assertNoMessageAction(ServiceData result) {
    boolean present = result.getClientActionList().stream().anyMatch(action -> "message".equals(action.getType()));
    assertEquals(false, present);
  }

  private void assertIconRestored(ServiceData result) {
    ClientAction spinnerRemoval = findAction(result, "remove-class", BUTTON_ICON_SELECTOR);
    assertEquals("fa-spinner fa-spin", spinnerRemoval.getParameters().get("targetAction"));
    ClientAction iconRestore = findAction(result, "add-class", BUTTON_ICON_SELECTOR);
    assertEquals("fa-plug", iconRestore.getParameters().get("targetAction"));
  }

  private void assertStateClass(ServiceData result, String stateClass) {
    ClientAction action = findAction(result, "add-class", BUTTON_SELECTOR);
    assertEquals(stateClass, action.getParameters().get("targetAction"));
  }

  private void assertNoStateClass(ServiceData result) {
    boolean present = result.getClientActionList().stream()
      .anyMatch(action -> "add-class".equals(action.getType()) && BUTTON_SELECTOR.equals(action.getTarget()));
    assertEquals(false, present);
  }

  private ClientAction findAction(ServiceData result, String type, String target) {
    return result.getClientActionList().stream()
      .filter(action -> type.equals(action.getType()))
      .filter(action -> target == null || target.equals(action.getTarget()))
      .findFirst()
      .orElseGet(() -> fail("Missing client action " + type + (target == null ? "" : " on " + target)));
  }

  private ServerConnectionService service(SshHostKeyPolicy policy) {
    ServerConnectionService service = new ServerConnectionService(serverDAO, ftpClient, policy,
      tempDir.resolve(policy.name() + "/known_hosts"), CONNECT_TIMEOUT);
    mockLocales(service);
    return service;
  }

  private void mockLocales(ServerConnectionService service) {
    service.setApplicationContext(context);
    doReturn(aweElements).when(context).getBean(AweElements.class);
    when(aweElements.getLanguage()).thenReturn("en");
    when(aweElements.getLocaleWithLanguage(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  private String privateKeyPem() throws Exception {
    java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter.INSTANCE
      .writePrivateKey(generator.generateKeyPair(), "test-key",
        (org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyEncryptionContext) null, out);
    return out.toString(java.nio.charset.StandardCharsets.UTF_8);
  }
}
