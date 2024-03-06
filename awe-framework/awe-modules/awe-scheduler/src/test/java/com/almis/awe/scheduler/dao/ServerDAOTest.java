package com.almis.awe.scheduler.dao;

import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.service.QueryService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    assertNull(serverDAO.findServer(121, null));
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
    assertEquals(server, serverDAO.findServer(121, null));
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
    assertEquals(server, serverDAO.findServer(122, null));
  }
}
