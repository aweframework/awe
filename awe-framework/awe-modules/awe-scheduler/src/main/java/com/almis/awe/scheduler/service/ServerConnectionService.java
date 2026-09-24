package com.almis.awe.scheduler.service;

import com.almis.awe.builder.client.MessageActionBuilder;
import com.almis.awe.builder.client.css.AddCssClassActionBuilder;
import com.almis.awe.builder.client.css.RemoveCssClassActionBuilder;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.scheduler.bean.file.Server;
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
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests the connection of a scheduler external server with the values currently
 * typed in the new/update server screens, without persisting anything. The SSH
 * path honors the same host-key policy and known_hosts file configured for SSH
 * command tasks and SFTP triggers, so the trust diagnosis matches what the
 * scheduler will do at task time, while the check runs under its own shorter
 * interactive timeout. On the update screen the secret fields are
 * rendered blank and mean "keep the stored value", so blank secrets fall back to
 * the stored (decrypted) ones when the server identifier is present. Credential
 * material is never logged.
 * <p>
 * Every outcome is reported to the screen through client actions: the spinner
 * shown while the check runs is swapped back to the plug icon and the test
 * button gets a green (success) or red (failure) state class. Success is
 * otherwise silent; failures additionally carry the localized diagnosis as a
 * message.
 */
@Slf4j
public class ServerConnectionService extends ServiceConfig {

  private static final String KO_TITLE = "SCHEDULER_SERVER_TEST_KO_TITLE";
  private static final String CONNECTION_ERROR = "SCHEDULER_SERVER_TEST_CONNECTION_ERROR";
  private static final String AUTHENTICATION_ERROR = "SCHEDULER_SERVER_TEST_AUTHENTICATION_ERROR";
  private static final String HOST_KEY_ERROR = "SCHEDULER_SERVER_TEST_HOST_KEY_ERROR";
  private static final String INVALID_KEY_ERROR = "SCHEDULER_SERVER_TEST_INVALID_KEY_ERROR";
  private static final String FOLDER_TITLE = "SCHEDULER_SERVER_TEST_FOLDER_TITLE";
  private static final String FOLDER_MESSAGE = "SCHEDULER_SERVER_TEST_FOLDER_MESSAGE";
  private static final String UNSUPPORTED_TYPE = "SCHEDULER_SERVER_TEST_UNSUPPORTED_TYPE";
  private static final String SERVER_NOT_FOUND = "SCHEDULER_SERVER_TEST_SERVER_NOT_FOUND";

  private static final String BUTTON_SELECTOR = "#ButTstCnx";
  private static final String BUTTON_ICON_SELECTOR = "#ButTstCnx i";
  private static final String SPINNER_CLASSES = "fa-spinner fa-spin";
  private static final String ICON_CLASS = "fa-plug";
  private static final String SUCCESS_CLASS = "btn-success p-button-success";
  private static final String FAILURE_CLASS = "btn-danger p-button-danger";

  private static final String LOG_CONTEXT = "Server connection test";

  private final ServerDAO serverDAO;
  private final FTPClient ftpClient;
  private final SshHostKeyPolicy hostKeyPolicy;
  private final Path knownHostsPath;
  private final Duration connectTimeout;

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
    this.serverDAO = serverDAO;
    this.ftpClient = ftpClient;
    this.hostKeyPolicy = hostKeyPolicy;
    this.knownHostsPath = knownHostsPath;
    this.connectTimeout = connectTimeout;
  }

  /**
   * Attempt a real connection and report the outcome. Two invocation shapes:
   * with form values (creation/update screens), where blank secrets fall back
   * to the stored ones when the server identifier is present; or with only the
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
    if (isBlank(type) && serverId != null) {
      Server storedServer = serverDAO.findServer(serverId);
      if (storedServer == null) {
        return failure(SERVER_NOT_FOUND);
      }
      return dispatch(storedServer.getTypeOfConnection(), storedServer.getHost(), storedServer.getPort(),
        storedServer.getUser(), storedServer.getPassword(), storedServer.getKey(), storedServer.getKeyPassphrase());
    }

    Server stored = findStoredServer(serverId, password, key, keyPassphrase);
    String effectivePassword = firstNonBlank(password, stored == null ? null : stored.getPassword());
    String effectiveKey = firstNonBlank(key, stored == null ? null : stored.getKey());
    String effectiveKeyPassphrase = firstNonBlank(keyPassphrase, stored == null ? null : stored.getKeyPassphrase());

    return dispatch(type, host, port, user, effectivePassword, effectiveKey, effectiveKeyPassphrase);
  }

  /**
   * Route the check to the matching connection type.
   *
   * @param type          Connection type (ftp, folder, ssh)
   * @param host          Host
   * @param port          Port
   * @param user          User
   * @param password      Password
   * @param key           Private key
   * @param keyPassphrase Private key passphrase
   * @return ServiceData with the localized outcome
   */
  private ServiceData dispatch(String type, String host, Integer port, String user, String password,
                               String key, String keyPassphrase) {
    switch (type == null ? "" : type.toLowerCase(Locale.ROOT)) {
      case "folder":
        return outcome(AnswerType.WARNING, FOLDER_TITLE, FOLDER_MESSAGE, null);
      case "ftp":
        return checkFtpConnection(host, port, user, password);
      case "ssh":
        return checkSshConnection(host, port, user, password, key, keyPassphrase);
      default:
        return failure(UNSUPPORTED_TYPE);
    }
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
  private ServiceData checkFtpConnection(String host, Integer port, String user, String password) {
    try {
      int timeoutMillis = (int) connectTimeout.toMillis();
      ftpClient.setConnectTimeout(timeoutMillis);
      ftpClient.setDefaultTimeout(timeoutMillis);
      ftpClient.setDataTimeout(timeoutMillis);
      ftpClient.connect(host, port);
      if (!isBlank(user) && !ftpClient.login(user, password)) {
        return failure(AUTHENTICATION_ERROR);
      }
      return success();
    } catch (IOException | RuntimeException exc) {
      log.debug("[{}] FTP connection failed", LOG_CONTEXT, exc);
      return failure(CONNECTION_ERROR);
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
  private ServiceData checkSshConnection(String host, Integer port, String user, String password,
                                         String key, String keyPassphrase) {
    Collection<KeyPair> keyPairs = Collections.emptyList();
    if (!isBlank(key)) {
      keyPairs = parseKeyPairs(key, keyPassphrase);
      if (keyPairs == null || keyPairs.isEmpty()) {
        return failure(INVALID_KEY_ERROR);
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
        return failure(hostKeyRejected.get() ? HOST_KEY_ERROR : CONNECTION_ERROR);
      }

      try (ClientSession openSession = session) {
        for (KeyPair keyPair : keyPairs) {
          openSession.addPublicKeyIdentity(keyPair);
        }
        if (!isBlank(password)) {
          openSession.addPasswordIdentity(password);
        }
        openSession.auth().verify(connectTimeout);
        return success();
      } catch (IOException exc) {
        log.debug("[{}] SSH authentication failed", LOG_CONTEXT, exc);
        return failure(hostKeyRejected.get() ? HOST_KEY_ERROR : AUTHENTICATION_ERROR);
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
}
