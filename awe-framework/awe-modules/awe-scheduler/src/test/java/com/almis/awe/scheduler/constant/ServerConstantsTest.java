package com.almis.awe.scheduler.constant;

import org.junit.jupiter.api.Test;

import static com.almis.awe.scheduler.constant.ServerConstants.SERVER_SSH_KEY;
import static com.almis.awe.scheduler.constant.ServerConstants.SERVER_SSH_KEY_PASS;
import static com.almis.awe.scheduler.constant.ServerConstants.SERVER_SSH_PASS;
import static com.almis.awe.scheduler.constant.ServerConstants.SERVER_SSH_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Credential parameter-name constants used to wire the {@code AweSchSrv}
 * credential columns: {@code SshUsr}/{@code SshPwd}/{@code SshKey}.
 *
 * <p>The column names keep their {@code Ssh} prefix because they are published
 * API, but they hold the external server credentials used by every protocol
 * that authenticates: remote command execution over SSH, SFTP file checking
 * and FTP file checking.</p>
 */
class ServerConstantsTest {

  @Test
  void sshUserConstantMatchesColumnName() {
    assertEquals("SshUsr", SERVER_SSH_USER);
  }

  @Test
  void sshPasswordConstantMatchesColumnName() {
    assertEquals("SshPwd", SERVER_SSH_PASS);
  }

  @Test
  void sshKeyConstantMatchesColumnName() {
    assertEquals("SshKey", SERVER_SSH_KEY);
  }

  @Test
  void sshKeyPassphraseConstantMatchesColumnName() {
    assertEquals("SshKeyPass", SERVER_SSH_KEY_PASS);
  }
}
