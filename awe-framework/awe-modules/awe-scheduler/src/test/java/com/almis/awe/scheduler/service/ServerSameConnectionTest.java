package com.almis.awe.scheduler.service;

import com.almis.awe.scheduler.bean.file.Server;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * When the form of a saved server tests its stored connection (#993), so that its stored secrets may be used: the same
 * connection type, host, port and user, read as the connection reads them (the default port of the type when there is
 * none, the host without case or surrounding spaces). A port that cannot be known is never the same port
 */
class ServerSameConnectionTest {

  @Test
  void aStoredServerWithoutPortIsTheSameConnectionAsTheFormWithTheDefaultPortOfItsType() {
    Server stored = server("ssh", "sftp.banks.local", null);

    assertThat(ServerConnectionService.sameConnection(stored, server("ssh", "sftp.banks.local", 22))).isTrue();
    assertThat(ServerConnectionService.sameConnection(stored, server("ssh", "sftp.banks.local", 2222))).isFalse();
  }

  @Test
  void theHostIsTheSameWhateverItsCaseAndSurroundingSpaces() {
    assertThat(ServerConnectionService.sameConnection(server("ssh", "SFTP.Banks.local", 22),
      server("ssh", "  sftp.banks.LOCAL ", 22))).isTrue();
  }

  @Test
  void aPortThatCannotBeKnownOnBothSidesIsNotTheSameConnection() {
    assertThat(ServerConnectionService.sameConnection(server(null, "sftp.banks.local", null),
      server(null, "sftp.banks.local", null))).isFalse();
  }

  private static Server server(String type, String host, Integer port) {
    return new Server().setTypeOfConnection(type).setHost(host).setPort(port).setUser("probe");
  }
}
