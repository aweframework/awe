package com.almis.awe.scheduler.autoconfigure;

import com.almis.awe.scheduler.autoconfigure.config.SchedulerConfigProperties;
import com.almis.awe.scheduler.dao.ServerDAO;
import com.almis.awe.scheduler.service.ServerConnectionService;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The server connection test service must be wired by the scheduler starter with
 * the same SSH properties (host-key policy, known_hosts path, connect timeout)
 * used for SSH command tasks and SFTP triggers, so screens can invoke it.
 */
class SchedulerConfigServerConnectionServiceTest {

  @Test
  void serverConnectionServiceBeanIsDefined() {
    SchedulerConfig config = new SchedulerConfig(new SchedulerConfigProperties());

    ServerConnectionService service = config.serverConnectionService(new ServerDAO(null, null), new FTPClient());

    assertNotNull(service);
  }
}
