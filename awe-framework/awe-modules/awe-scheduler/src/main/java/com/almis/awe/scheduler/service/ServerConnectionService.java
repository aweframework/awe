package com.almis.awe.scheduler.service;

import com.almis.awe.builder.client.MessageActionBuilder;
import com.almis.awe.builder.client.css.AddCssClassActionBuilder;
import com.almis.awe.builder.client.css.RemoveCssClassActionBuilder;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.scheduler.bean.file.ServerTestResult;
import com.almis.awe.scheduler.dao.ServerDAO;
import com.almis.awe.scheduler.enums.SshHostKeyPolicy;
import com.almis.awe.scheduler.ssh.SshSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.util.security.SecurityUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

/**
 * Tests the connection of a scheduler external server with the values currently
 * typed in the new/update server screens, without persisting anything. The SSH
 * path honors the same host-key policy and known_hosts file configured for SSH
 * command tasks and SFTP triggers, so the trust diagnosis matches what the
 * scheduler will do at task time, while the check runs under its own shorter
 * interactive timeout. On the update screen the secret fields are
 * rendered blank and mean "keep the stored value", so blank secrets fall back to
 * the stored (decrypted) ones when the server identifier is present and the form
 * keeps the stored connection (type, host, port and user): a stored secret is never
 * sent to a connection typed by the caller. Credential material is never logged.
 * <p>
 * Every outcome is reported to the screen through client actions: the spinner
 * shown while the check runs is swapped back to the plug icon and the test
 * button gets a green (success) or red (failure) state class. Success is
 * otherwise silent; failures additionally carry the localized diagnosis as a
 * message.
 * <p>
 * The configuration screen tests through {@link #testServerConnection} and
 * {@link #testAllServerConnections}: they answer with how long the test took and
 * where it connected, or the generic reason of a failure (never the text of an
 * exception), and keep the test of a stored server as its last test in the
 * session of the user, never in the database.
 */
@Slf4j
public class ServerConnectionService extends ServiceConfig {

  private static final String TEST = "SCHEDULER_SERVER_TEST_";
  private static final String KO_TITLE = TEST + "KO_TITLE";
  private static final String OK_TITLE = TEST + "OK_TITLE";
  private static final String ALL_TITLE = TEST + "ALL_TITLE";
  private static final String ALL_MESSAGE = TEST + "ALL_MESSAGE";
  private static final String ALL_MESSAGE_PARTIAL = TEST + "ALL_MESSAGE_PARTIAL";
  private static final String SECONDS = "SCREEN_TEXT_SCHEDULER_SERVER_TEST_SECONDS";
  private static final String FOLDER_TITLE = TEST + "FOLDER_TITLE";
  private static final String FOLDER_MESSAGE = TEST + "FOLDER_MESSAGE";
  // Reasons of a failure: the end of their locale keys, never the text of an exception
  private static final String CONNECTION_ERROR = "CONNECTION_ERROR";
  private static final String AUTHENTICATION_ERROR = "AUTHENTICATION_ERROR";
  private static final String HOST_KEY_ERROR = "HOST_KEY_ERROR";
  private static final String INVALID_KEY_ERROR = "INVALID_KEY_ERROR";
  private static final String UNSUPPORTED_TYPE = "UNSUPPORTED_TYPE";
  private static final String SERVER_NOT_FOUND = "SERVER_NOT_FOUND";
  private static final String UNEXPECTED_ERROR = "UNEXPECTED_ERROR";
  private static final String FOLDER = "folder";

  private static final String BUTTON_SELECTOR = "#ButTstCnx";
  private static final String BUTTON_ICON_SELECTOR = "#ButTstCnx i";
  private static final String SPINNER_CLASSES = "fa-spinner fa-spin";
  private static final String ICON_CLASS = "fa-plug";
  private static final String SUCCESS_CLASS = "btn-success p-button-success";
  private static final String FAILURE_CLASS = "btn-danger p-button-danger";

  private static final String LOG_CONTEXT = "Server connection test";
  /**
   * Time of «Probar todos»: the request never lasts the sum of the timeouts of every server
   */
  private static final Duration ALL_TESTS_DEADLINE = Duration.ofSeconds(60);

  private final ServerDAO serverDAO;
  private final FTPClient ftpClient;
  private final SshHostKeyPolicy hostKeyPolicy;
  private final Path knownHostsPath;
  private final Duration connectTimeout;
  private final Clock clock;
  private final LongSupplier ticker;
  private final Duration allTestsDeadline;
  private final ServerTestResultStore testResults = new ServerTestResultStore(this::getSession);

  /**
   * Autowired constructor
   *
   * @param serverDAO      Server DAO, to resolve stored secrets left blank on the update screen
   * @param ftpClient      FTP client
   * @param hostKeyPolicy  Host-key verification policy
   * @param knownHostsPath Path to the known_hosts file used to persist/read trusted host keys
   * @param connectTimeout Interactive connection-test timeout (connect, authentication and FTP data exchanges)
   */
  public ServerConnectionService(ServerDAO serverDAO, FTPClient ftpClient, SshHostKeyPolicy hostKeyPolicy,
                                 Path knownHostsPath, Duration connectTimeout) {
    this(serverDAO, ftpClient, hostKeyPolicy, knownHostsPath, connectTimeout, Clock.systemDefaultZone(),
      System::nanoTime);
  }

  /**
   * Constructor with the clock of the end of the tests and the ticker that measures how long they take
   *
   * @param serverDAO      Server DAO, to resolve stored secrets left blank on the update screen
   * @param ftpClient      FTP client
   * @param hostKeyPolicy  Host-key verification policy
   * @param knownHostsPath Path to the known_hosts file used to persist/read trusted host keys
   * @param connectTimeout Interactive connection-test timeout (connect, authentication and FTP data exchanges)
   * @param clock          Clock of the end of the tests
   * @param ticker         Nanosecond ticker that measures the duration of the tests
   */
  public ServerConnectionService(ServerDAO serverDAO, FTPClient ftpClient, SshHostKeyPolicy hostKeyPolicy,
                                 Path knownHostsPath, Duration connectTimeout, Clock clock, LongSupplier ticker) {
    this(serverDAO, ftpClient, hostKeyPolicy, knownHostsPath, connectTimeout, clock, ticker, ALL_TESTS_DEADLINE);
  }

  /**
   * Constructor with the clock of the end of the tests, the ticker that measures how long they take and the time of
   * «Probar todos»
   *
   * @param serverDAO        Server DAO, to resolve stored secrets left blank on the update screen
   * @param ftpClient        FTP client
   * @param hostKeyPolicy    Host-key verification policy
   * @param knownHostsPath   Path to the known_hosts file used to persist/read trusted host keys
   * @param connectTimeout   Interactive connection-test timeout (connect, authentication and FTP data exchanges)
   * @param clock            Clock of the end of the tests
   * @param ticker           Nanosecond ticker that measures the duration of the tests
   * @param allTestsDeadline Time of «Probar todos»: no server starts its test after it (the test running then ends
   *                         within its own timeouts)
   */
  public ServerConnectionService(ServerDAO serverDAO, FTPClient ftpClient, SshHostKeyPolicy hostKeyPolicy,
                                 Path knownHostsPath, Duration connectTimeout, Clock clock, LongSupplier ticker,
                                 Duration allTestsDeadline) {
    this.serverDAO = serverDAO;
    this.ftpClient = ftpClient;
    this.hostKeyPolicy = hostKeyPolicy;
    this.knownHostsPath = knownHostsPath;
    this.connectTimeout = connectTimeout;
    this.clock = clock;
    this.ticker = ticker;
    this.allTestsDeadline = allTestsDeadline;
  }

  /**
   * Attempt a real connection and report the outcome. Two invocation shapes:
   * with form values (creation/update screens), where blank secrets fall back
   * to the stored ones when the server identifier is present and the form keeps
   * the stored connection (type, host, port and user); or with only the
   * server identifier (servers list screen), where the whole record is loaded
   * from the stored server.
   *
   * @param type          Connection type (ftp, folder, ssh), blank to test the stored server by id
   * @param host          Host
   * @param port          Port
   * @param user          User
   * @param password      Password typed in the form (blank keeps the stored one on the update screen)
   * @param key           Private key typed in the form (blank keeps the stored one on the update screen)
   * @param keyPassphrase Private key passphrase typed in the form (blank keeps the stored one on the update screen)
   * @param serverId      Server identifier when testing an existing server, null on the creation screen
   * @return ServiceData with the localized outcome and its screen feedback client actions
   * @throws AWException Error retrieving the stored server credentials
   */
  public ServiceData checkServerConnection(String type, String host, Integer port, String user, String password,
                                           String key, String keyPassphrase, Integer serverId) throws AWException {
    Server server = resolve(type, host, port, user, password, key, keyPassphrase, serverId);
    Probe probe = server == null ? Probe.failed(SERVER_NOT_FOUND) : dispatch(server);
    return switch (probe.outcome()) {
      case OK -> success();
      case FAILED -> failure(TEST + probe.reason());
      case NOTHING_TO_TEST -> outcome(AnswerType.WARNING, FOLDER_TITLE, FOLDER_MESSAGE, null);
    };
  }

  /**
   * Configuration screen: attempt a real connection and tell how long it took and where it connected («Conexión
   * correcta en 0,42 s», user@host:port), or the generic reason of the failure. With form values (a server being
   * created or edited) blank secrets fall back to the stored ones when the server identifier is present and the form
   * keeps the stored connection (type, host, port and user), and nothing is kept; with only the server identifier the
   * stored server is tested and the result is kept as its last test in the session of the user (never in the
   * database)
   *
   * @param type          Connection type (ftp, folder, ssh), blank to test the stored server by id
   * @param host          Host
   * @param port          Port
   * @param user          User
   * @param password      Password typed in the form (blank keeps the stored one)
   * @param key           Private key typed in the form (blank keeps the stored one)
   * @param keyPassphrase Private key passphrase typed in the form (blank keeps the stored one)
   * @param serverId      Server identifier when testing an existing server, null for a new server
   * @return OK with the duration and the target, ERROR with the reason, or WARNING when a folder has nothing to test
   * @throws AWException Error retrieving the stored server credentials
   */
  public ServiceData testServerConnection(String type, String host, Integer port, String user, String password,
                                          String key, String keyPassphrase, Integer serverId) throws AWException {
    boolean stored = isBlank(type) && serverId != null;
    Server server = resolve(type, host, port, user, password, key, keyPassphrase, serverId);
    if (server == null) {
      return failed(Probe.failed(SERVER_NOT_FOUND));
    }
    long start = ticker.getAsLong();
    Probe probe = dispatch(server);
    ServerTestResult result = result(probe, start);
    if (stored && result != null) {
      testResults.record(serverId, result);
    }
    return switch (probe.outcome()) {
      case OK -> new ServiceData()
        .setType(AnswerType.OK)
        .setTitle(getLocale(OK_TITLE, getLocale(SECONDS, result.seconds())))
        .setMessage(target(server));
      case FAILED -> failed(probe);
      case NOTHING_TO_TEST -> new ServiceData()
        .setType(AnswerType.WARNING)
        .setTitle(getLocale(FOLDER_TITLE))
        .setMessage(getLocale(FOLDER_MESSAGE));
    };
  }

  /**
   * Configuration screen, «Probar todos»: test every active server with its stored values and keep each result as its
   * last test in the session of the user. A folder has no connection to test, so it is left untested. Each server is
   * isolated: when it cannot be read or its test breaks, that is its failure with a generic reason (logged) and the
   * others are still tested. No server starts its test once the deadline of the whole test has passed: those are
   * reported as not tested
   *
   * @return OK with how many servers were tested, how many connected, how many failed and how many were not tested
   * @throws AWException The servers cannot be read
   */
  public ServiceData testAllServerConnections() throws AWException {
    int connected = 0;
    int failed = 0;
    int notTested = 0;
    Long begin = null;
    for (Integer serverId : serverDAO.findActiveServerIds()) {
      if (begin != null && ticker.getAsLong() - begin >= allTestsDeadline.toNanos()) {
        notTested++;
        continue;
      }
      TimedResult result = testStoredServer(serverId);
      if (result != null) {
        begin = begin == null ? result.startTick() : begin;
        testResults.record(serverId, result.result());
        if (result.result().ok()) {
          connected++;
        } else {
          failed++;
        }
      }
    }
    String tested = String.valueOf(connected + failed);
    return new ServiceData()
      .setType(AnswerType.OK)
      .setTitle(getLocale(ALL_TITLE))
      .setMessage(notTested == 0
        ? getLocale(ALL_MESSAGE, tested, String.valueOf(connected), String.valueOf(failed))
        : getLocale(ALL_MESSAGE_PARTIAL, tested, String.valueOf(connected), String.valueOf(failed),
        String.valueOf(notTested)));
  }

  /**
   * Test of a stored server of «Probar todos», null when there is nothing to test (a folder, or a server deleted since
   * the list was read). Any failure to read or to test it is its failure with a generic reason, never the text of the
   * exception, which is only logged
   */
  private TimedResult testStoredServer(Integer serverId) {
    Server server;
    try {
      server = serverDAO.findServer(serverId);
    } catch (AWException | RuntimeException exc) {
      log.warn("[{}] Server {} could not be read to test it", LOG_CONTEXT, serverId, exc);
      long now = ticker.getAsLong();
      return new TimedResult(now, ServerTestResult.failure(UNEXPECTED_ERROR, 0, LocalDateTime.now(clock)));
    }
    if (server == null || FOLDER.equalsIgnoreCase(server.getTypeOfConnection())) {
      return null;
    }
    long start = ticker.getAsLong();
    Probe probe;
    try {
      probe = dispatch(server);
    } catch (RuntimeException exc) {
      log.warn("[{}] The test of server {} failed unexpectedly", LOG_CONTEXT, serverId, exc);
      probe = Probe.failed(UNEXPECTED_ERROR);
    }
    return new TimedResult(start, result(probe, start));
  }

  /**
   * Result of a test and the tick when it started
   */
  private record TimedResult(long startTick, ServerTestResult result) {
  }

  /**
   * Server to test: the stored one when only its identifier is given (null when it does not exist), or the form values
   * with the stored secrets in place of the blank ones
   */
  private Server resolve(String type, String host, Integer port, String user, String password, String key,
                         String keyPassphrase, Integer serverId) throws AWException {
    if (isBlank(type) && serverId != null) {
      return serverDAO.findServer(serverId);
    }
    Server form = new Server()
      .setServerId(serverId)
      .setTypeOfConnection(type)
      .setHost(host)
      .setPort(port)
      .setUser(user);
    Server stored = findStoredServer(serverId, password, key, keyPassphrase);
    // The stored secrets only go to the stored connection, never to a host, port, user or type typed by the caller
    if (stored != null && !sameConnection(stored, form)) {
      log.debug("[{}] The form changes the connection of server {}: its stored secrets are not used", LOG_CONTEXT,
        serverId);
      stored = null;
    }
    return form
      .setPassword(firstNonBlank(password, stored == null ? null : stored.getPassword()))
      .setKey(firstNonBlank(key, stored == null ? null : stored.getKey()))
      .setKeyPassphrase(firstNonBlank(keyPassphrase, stored == null ? null : stored.getKeyPassphrase()));
  }

  /**
   * The form tests the stored connection: same connection type, host (case insensitive), port (the default one of the
   * type when there is none) and user. A port that cannot be known on either side is never the same port
   */
  static boolean sameConnection(Server stored, Server form) {
    Integer storedPort = port(stored);
    return storedPort != null && storedPort.equals(port(form))
      && trimmed(stored.getTypeOfConnection()).equalsIgnoreCase(trimmed(form.getTypeOfConnection()))
      && trimmed(stored.getHost()).equalsIgnoreCase(trimmed(form.getHost()))
      && trimmed(stored.getUser()).equals(trimmed(form.getUser()));
  }

  /**
   * Port of a connection, the default one of its type when it has none; null when it cannot be known
   */
  private static Integer port(Server server) {
    try {
      return server.getPort();
    } catch (RuntimeException exc) {
      return null;
    }
  }

  private static String trimmed(String value) {
    return value == null ? "" : value.trim();
  }

  /**
   * Result of a test that ended now, null when there was nothing to test
   */
  private ServerTestResult result(Probe probe, long start) {
    long millis = Math.max(0, (ticker.getAsLong() - start) / 1_000_000);
    LocalDateTime now = LocalDateTime.now(clock);
    return switch (probe.outcome()) {
      case OK -> ServerTestResult.success(millis, now);
      case FAILED -> ServerTestResult.failure(probe.reason(), millis, now);
      case NOTHING_TO_TEST -> null;
    };
  }

  /**
   * Failure of the configuration screen: the generic reason of its locale
   */
  private ServiceData failed(Probe probe) {
    return new ServiceData()
      .setType(AnswerType.ERROR)
      .setTitle(getLocale(KO_TITLE))
      .setMessage(getLocale(TEST + probe.reason()));
  }

  /**
   * Where the test connected: user@host:port, without the user when there is none
   */
  private static String target(Server server) {
    String address = server.getHost() + (server.getPort() > 0 ? ":" + server.getPort() : "");
    return isBlank(server.getUser()) ? address : server.getUser() + "@" + address;
  }

  /**
   * Route the check to the matching connection type.
   *
   * @param server Server to test, with its secrets
   * @return Outcome of the test
   */
  private Probe dispatch(Server server) {
    String type = server.getTypeOfConnection();
    return switch (type == null ? "" : type.toLowerCase(Locale.ROOT)) {
      case FOLDER -> new Probe(Outcome.NOTHING_TO_TEST, null);
      case "ftp" -> checkFtpConnection(server.getHost(), server.getPort(), server.getUser(), server.getPassword());
      case "ssh" -> checkSshConnection(server.getHost(), server.getPort(), server.getUser(), server.getPassword(),
        server.getKey(), server.getKeyPassphrase());
      default -> Probe.failed(UNSUPPORTED_TYPE);
    };
  }

  /**
   * Resolve the stored server when a secret was left blank on the update screen.
   *
   * @param serverId      Server identifier, null on the creation screen
   * @param password      Password typed in the form
   * @param key           Private key typed in the form
   * @param keyPassphrase Private key passphrase typed in the form
   * @return Stored server with decrypted secrets, or null when not needed or not found
   * @throws AWException Error retrieving the stored server
   */
  private Server findStoredServer(Integer serverId, String password, String key, String keyPassphrase) throws AWException {
    if (serverId == null || (!isBlank(password) && !isBlank(key) && !isBlank(keyPassphrase))) {
      return null;
    }
    return serverDAO.findServer(serverId);
  }

  /**
   * Connect (and log in when a user is given) to an FTP server, then disconnect.
   *
   * @param host     Host
   * @param port     Port
   * @param user     User, blank for an anonymous connection
   * @param password Password
   * @return ServiceData with the localized outcome
   */
  private Probe checkFtpConnection(String host, Integer port, String user, String password) {
    try {
      int timeoutMillis = (int) connectTimeout.toMillis();
      ftpClient.setConnectTimeout(timeoutMillis);
      ftpClient.setDefaultTimeout(timeoutMillis);
      ftpClient.setDataTimeout(timeoutMillis);
      ftpClient.connect(host, port);
      if (!isBlank(user) && !ftpClient.login(user, password)) {
        return Probe.failed(AUTHENTICATION_ERROR);
      }
      return Probe.OK;
    } catch (IOException | RuntimeException exc) {
      log.debug("[{}] FTP connection failed", LOG_CONTEXT, exc);
      return Probe.failed(CONNECTION_ERROR);
    } finally {
      disconnectFtpQuietly();
    }
  }

  /**
   * Open and authenticate an SSH session with the same host-key policy and
   * known_hosts file the scheduler uses at task time, under the interactive
   * check timeout, then close it. An authenticated session is the proof: no
   * command is executed.
   *
   * @param host          Host
   * @param port          Port
   * @param user          User
   * @param password      Password
   * @param key           Private key
   * @param keyPassphrase Private key passphrase
   * @return ServiceData with the localized outcome
   */
  private Probe checkSshConnection(String host, Integer port, String user, String password,
                                         String key, String keyPassphrase) {
    Collection<KeyPair> keyPairs = Collections.emptyList();
    if (!isBlank(key)) {
      keyPairs = parseKeyPairs(key, keyPassphrase);
      if (keyPairs == null || keyPairs.isEmpty()) {
        return Probe.failed(INVALID_KEY_ERROR);
      }
    }

    AtomicBoolean hostKeyRejected = new AtomicBoolean(false);
    SshClient sshClient = SshClient.setUpDefaultClient();
    ServerKeyVerifier policyVerifier = SshSupport.buildServerKeyVerifier(hostKeyPolicy, knownHostsPath);
    sshClient.setServerKeyVerifier((clientSession, remoteAddress, serverKey) -> {
      boolean accepted = policyVerifier.verifyServerKey(clientSession, remoteAddress, serverKey);
      if (!accepted) {
        hostKeyRejected.set(true);
      }
      return accepted;
    });
    sshClient.start();

    try {
      ClientSession session;
      try {
        session = sshClient.connect(user, host, port).verify(connectTimeout).getSession();
      } catch (IOException exc) {
        log.debug("[{}] SSH connection failed", LOG_CONTEXT, exc);
        return Probe.failed(hostKeyRejected.get() ? HOST_KEY_ERROR : CONNECTION_ERROR);
      }

      try (ClientSession openSession = session) {
        for (KeyPair keyPair : keyPairs) {
          openSession.addPublicKeyIdentity(keyPair);
        }
        if (!isBlank(password)) {
          openSession.addPasswordIdentity(password);
        }
        openSession.auth().verify(connectTimeout);
        return Probe.OK;
      } catch (IOException exc) {
        log.debug("[{}] SSH authentication failed", LOG_CONTEXT, exc);
        return Probe.failed(hostKeyRejected.get() ? HOST_KEY_ERROR : AUTHENTICATION_ERROR);
      }
    } finally {
      sshClient.stop();
    }
  }

  /**
   * Parse the private key eagerly so an unreadable key is reported as such,
   * distinct from an authentication failure.
   *
   * @param key           Private key
   * @param keyPassphrase Private key passphrase
   * @return Parsed key pairs, or null when the key cannot be read
   */
  private Collection<KeyPair> parseKeyPairs(String key, String keyPassphrase) {
    FilePasswordProvider passwordProvider = isBlank(keyPassphrase)
      ? FilePasswordProvider.EMPTY
      : FilePasswordProvider.of(keyPassphrase);
    try {
      return SecurityUtils.getKeyPairResourceParser()
        .loadKeyPairs(null, NamedResource.ofName("scheduler-server-connection-test"), passwordProvider, key);
    } catch (IOException | GeneralSecurityException exc) {
      log.debug("[{}] The provided SSH private key could not be parsed", LOG_CONTEXT);
      return null;
    }
  }

  /**
   * Silent success: the green button is the only feedback.
   *
   * @return ServiceData
   */
  private ServiceData success() {
    ServiceData serviceData = new ServiceData();
    restoreButtonIcon(serviceData);
    serviceData.addClientAction(new AddCssClassActionBuilder(BUTTON_SELECTOR, SUCCESS_CLASS).build());
    return serviceData;
  }

  private ServiceData failure(String messageKey) {
    return outcome(AnswerType.ERROR, KO_TITLE, messageKey, FAILURE_CLASS);
  }

  /**
   * Build a diagnosed outcome: localized title/message plus the screen feedback
   * client actions (message dialog, icon restore, button state class).
   *
   * @param severity   Message severity shown on screen
   * @param titleKey   Title locale
   * @param messageKey Message locale
   * @param stateClass Class applied to the test button, null for no state feedback
   * @return ServiceData
   */
  private ServiceData outcome(AnswerType severity, String titleKey, String messageKey, String stateClass) {
    String title = getLocale(titleKey);
    String message = getLocale(messageKey);
    ServiceData serviceData = new ServiceData()
      .setTitle(title)
      .setMessage(message);
    restoreButtonIcon(serviceData);
    if (stateClass != null) {
      serviceData.addClientAction(new AddCssClassActionBuilder(BUTTON_SELECTOR, stateClass).build());
    }
    serviceData.addClientAction(new MessageActionBuilder(severity, title, message).build());
    return serviceData;
  }

  /**
   * Swap the in-flight spinner back to the button's plug icon.
   *
   * @param serviceData ServiceData receiving the client actions
   */
  private void restoreButtonIcon(ServiceData serviceData) {
    serviceData.addClientAction(new RemoveCssClassActionBuilder(BUTTON_ICON_SELECTOR, SPINNER_CLASSES).build());
    serviceData.addClientAction(new AddCssClassActionBuilder(BUTTON_ICON_SELECTOR, ICON_CLASS).build());
  }

  private void disconnectFtpQuietly() {
    try {
      ftpClient.logout();
      ftpClient.disconnect();
    } catch (IOException | RuntimeException exc) {
      log.debug("[{}] FTP disconnect after the test failed", LOG_CONTEXT, exc);
    }
  }

  private static String firstNonBlank(String typed, String stored) {
    return isBlank(typed) ? stored : typed;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private enum Outcome {
    OK, FAILED, NOTHING_TO_TEST
  }

  /**
   * Outcome of a test and the reason of a failure (the end of its locale keys)
   */
  private record Probe(Outcome outcome, String reason) {
    private static final Probe OK = new Probe(Outcome.OK, null);

    private static Probe failed(String reason) {
      return new Probe(Outcome.FAILED, reason);
    }
  }
}
