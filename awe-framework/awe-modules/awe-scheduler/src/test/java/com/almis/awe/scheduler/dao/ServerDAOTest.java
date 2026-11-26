package com.almis.awe.scheduler.dao;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * Class used for testing queries through ActionController
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class ServerDAOTest {

  @InjectMocks
  private ServerDAO serverDAO;

  @Mock
  private QueryService queryService;

  @Mock
  private QueryUtil queryUtil;

  /**
   * Test context loaded
   */
  @Test
  void contextLoads() {
    // Check that controller are active
    assertNotNull(serverDAO);
  }

  /**
   * Test not finding server
   *
   * @throws Exception exception
   */
  @Test
  void findServerEmpty() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(new DataList()));

    // Assert called
    assertNull(serverDAO.findServer(121));
  }

  /**
   * Test finding server
   */
  @Test
  void findServer() throws Exception {
    Server server = new Server()
            .setServerId(121)
            .setActive(true)
            .setName("Server Guay")
            .setHost("192.168.1.21")
            .setTypeOfConnection("FTP")
            .setPort(8080);

    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(DataListUtil.fromBeanList(Collections.singletonList(server))));

    // Assert called
    assertEquals(server, serverDAO.findServer(121));
  }

  /**
   * Test finding server without port
   */
  @Test
  void findServerWithoutPort() throws Exception {
    Server server = new Server()
            .setServerId(122)
            .setActive(false)
            .setName("Server Guay2")
            .setHost("192.168.1.22")
            .setTypeOfConnection("SSH");

    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(DataListUtil.fromBeanList(Collections.singletonList(server))));

    // Assert called
    assertEquals(server, serverDAO.findServer(122));
  }

  /**
   * «Probar todos» of the configuration screen tests the active servers with a connection only: a folder has none
   */
  @Test
  void theActiveServersAreTheOnesWithTheirActiveFlagAndAConnection() throws Exception {
    DataList servers = new DataList();
    servers.addRow(Map.of("IdeSrv", new CellData(3), "Act", new CellData(1), "Pro", new CellData("ssh")));
    servers.addRow(Map.of("IdeSrv", new CellData(4), "Act", new CellData(0), "Pro", new CellData("ssh")));
    servers.addRow(Map.of("IdeSrv", new CellData(5), "Act", new CellData("1"), "Pro", new CellData("ftp")));
    servers.addRow(Map.of("IdeSrv", new CellData(6), "Act", new CellData(1), "Pro", new CellData("Folder")));
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(eq("getConfigurationServerRows"), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(servers));

    assertEquals(List.of(3, 5), serverDAO.findActiveServerIds());
  }
}
