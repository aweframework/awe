package com.almis.awe.scheduler.constant;

import org.junit.jupiter.api.Test;

import static com.almis.awe.scheduler.constant.ServerConstants.SERVER_SSH_KEY;
import static com.almis.awe.scheduler.constant.ServerConstants.SERVER_SSH_KEY_PASS;
import static com.almis.awe.scheduler.constant.ServerConstants.SERVER_SSH_PASS;
import static com.almis.awe.scheduler.constant.ServerConstants.SERVER_SSH_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SSH credential parameter-name constants used to wire the {@code AweSchSrv}
 * credential columns (design decision 3): {@code SshUsr}/{@code SshPwd}/{@code SshKey}.
 *
 * <p>These are DELIBERATELY NEW constants, distinct from the dead
 * {@code SERVER_USER}/{@code SERVER_PASS} ({@code LchUsr}/{@code LchPwd})
 * which map onto the unrelated launcher table columns.</p>
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
