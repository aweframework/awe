package com.almis.awe.scheduler.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.rest.dto.LoginResponse;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.service.scheduled.MaintainJobService;
import com.almis.awe.service.MaintainService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Class used for testing MaintainJobService class
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class MaintainJobServiceTest {

  @Mock
  QueryUtil queryUtil;
  @Mock
  AweElements aweElements;
  @Mock
  ApplicationContext context;
  @Mock
  MaintainService maintainService;
  @Mock
  ExecutionService executionService;
  @Mock
  TaskDAO taskDAO;
  @Mock
  ApplicationEventPublisher eventPublisher;
  private MaintainJobService maintainJobService;
  private static MockWebServer mockBackEnd;

  @BeforeAll
  static void setUp() throws IOException {
    mockBackEnd = new MockWebServer();
    mockBackEnd.start();
  }

  @AfterAll
  static void tearDown() throws IOException {
    mockBackEnd.shutdown();
  }

  @Test
  void testExecuteJobLocal() throws Exception {
    maintainJobService = new MaintainJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher,
      DataListUtil.getMapper(), Duration.ofSeconds(5), false, null, false, null, null);
    maintainJobService.setApplicationContext(context);


    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getProperty(anyString())).willReturn("ES");
    Trigger trigger = mock(Trigger.class);
    when(queryUtil.getParameters(isNull(), any(), any())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(trigger.getKey()).thenReturn(new TriggerKey("DummyTrigger"));
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());


    Future<ServiceData> serviceData = maintainJobService.executeJob(new Task()
        .setTaskId(1)
        .setAction("maintainId")
        .setTrigger(trigger)
        .setParameterList(Arrays.asList(
          new TaskParameter().setSource("1").setName("1").setValue("1").setType("STRING"),
          new TaskParameter().setSource("2").setName("2").setValue("2").setType("INTEGER")
        )),
      new TaskExecution(), new JobDataMap());

    assertNotNull(serviceData);
    assertEquals(AnswerType.OK, serviceData.get().getType());
  }

  @Test
  void testExecuteJobLocalErrorInMaintain() throws Exception {
    maintainJobService = new MaintainJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher,
      DataListUtil.getMapper(), Duration.ofSeconds(5),
      false, null, false, null, null);
    maintainJobService.setApplicationContext(context);

    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getProperty(anyString())).willReturn("ES");
    Trigger trigger = mock(Trigger.class);
    when(queryUtil.getParameters(isNull(), any(), any())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(trigger.getKey()).thenReturn(new TriggerKey("DummyTrigger"));
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class)))
      .thenThrow(new AWException("Error provocado en los tests"));

    Future<ServiceData> serviceData = maintainJobService.executeJob(new Task()
        .setTaskId(1)
        .setAction("maintainId")
        .setTrigger(trigger)
        .setParameterList(Arrays.asList(
          new TaskParameter().setSource("1").setName("1").setValue("1").setType("STRING"),
          new TaskParameter().setSource("2").setName("2").setValue("2").setType("INTEGER")
        )),
      new TaskExecution(), new JobDataMap());

    assertNotNull(serviceData);
    assertEquals(AnswerType.ERROR, serviceData.get().getType());
  }

  @Test
  void testExecuteJobRemote() throws Exception {
    maintainJobService = new MaintainJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher,
      DataListUtil.getMapper(), Duration.ofSeconds(5),
      true, new URI(String.format("http://localhost:%s", mockBackEnd.getPort())), true, "user", "pass");
    maintainJobService.setApplicationContext(context);

    // Back end response
    mockBackEnd.enqueue(new MockResponse()
      .setBody(DataListUtil.getMapper().writeValueAsString(new LoginResponse().setToken("token")))
      .addHeader("Content-Type", "application/json"));
    mockBackEnd.enqueue(new MockResponse()
      .setBody(DataListUtil.getMapper().writeValueAsString(new ServiceData()))
      .addHeader("Content-Type", "application/json"));


    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getProperty(anyString())).willReturn("ES");
    Trigger trigger = mock(Trigger.class);
    when(queryUtil.getParameters(isNull(), any(), any())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(trigger.getKey()).thenReturn(new TriggerKey("DummyTrigger"));

    Future<ServiceData> serviceData = maintainJobService.executeJob(new Task()
        .setTaskId(1)
        .setAction("maintainId")
        .setTrigger(trigger)
        .setParameterList(Arrays.asList(
          new TaskParameter().setSource("1").setName("1").setValue("1").setType("STRING"),
          new TaskParameter().setSource("2").setName("2").setValue("2").setType("INTEGER")
        )),
      new TaskExecution(), new JobDataMap());

    assertNotNull(serviceData);
    assertEquals(AnswerType.OK, serviceData.get().getType());
  }

  @Test
  void testExecuteJobRemoteErrorInAuthentication() throws Exception {
    maintainJobService = new MaintainJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher,
      DataListUtil.getMapper(), Duration.ofSeconds(5),
      true, new URI(String.format("http://localhost:%s", mockBackEnd.getPort())), true, "user", "pass");
    maintainJobService.setApplicationContext(context);

    mockBackEnd.enqueue(new MockResponse()
      .setBody("")
      .addHeader("Content-Type", "application/json"));
    mockBackEnd.enqueue(new MockResponse()
      .setBody(DataListUtil.getMapper().writeValueAsString(new ServiceData().setType(AnswerType.ERROR)))
      .addHeader("Content-Type", "application/json"));

    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getProperty(anyString())).willReturn("ES");
    Trigger trigger = mock(Trigger.class);
    when(queryUtil.getParameters(isNull(), any(), any())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(trigger.getKey()).thenReturn(new TriggerKey("DummyTrigger"));

    Future<ServiceData> serviceData = maintainJobService.executeJob(new Task()
        .setTaskId(1)
        .setAction("maintainId")
        .setTrigger(trigger)
        .setParameterList(Arrays.asList(
          new TaskParameter().setSource("1").setName("1").setValue("1").setType("STRING"),
          new TaskParameter().setSource("2").setName("2").setValue("2").setType("INTEGER")
        )),
      new TaskExecution(), new JobDataMap());

    assertNotNull(serviceData);
    assertEquals(AnswerType.ERROR, serviceData.get().getType());
  }

  @Test
  void testExecuteJobRemoteNoAuth() throws Exception {
    maintainJobService = new MaintainJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher,
      DataListUtil.getMapper(), Duration.ofSeconds(5),
      true, new URI(String.format("http://localhost:%s", mockBackEnd.getPort())), false, null, null);
    maintainJobService.setApplicationContext(context);
    mockBackEnd.enqueue(new MockResponse()
      .setBody(DataListUtil.getMapper().writeValueAsString(new ServiceData()))
      .addHeader("Content-Type", "application/json"));

    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getProperty(anyString())).willReturn("ES");
    Trigger trigger = mock(Trigger.class);
    when(queryUtil.getParameters(isNull(), any(), any())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(trigger.getKey()).thenReturn(new TriggerKey("DummyTrigger"));

    Future<ServiceData> serviceData = maintainJobService.executeJob(new Task()
        .setTaskId(1)
        .setAction("maintainId")
        .setTrigger(trigger)
        .setParameterList(Arrays.asList(
          new TaskParameter().setSource("1").setName("1").setValue("1").setType("STRING"),
          new TaskParameter().setSource("2").setName("2").setValue("2").setType("INTEGER")
        )),
      new TaskExecution(), new JobDataMap());

    assertNotNull(serviceData);
    assertEquals(AnswerType.OK, serviceData.get().getType());
  }

  @Test
  void testExecuteJobRemoteNoAuthErrorInResponse() throws Exception {
    maintainJobService = new MaintainJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher,
      DataListUtil.getMapper(), Duration.ofSeconds(5),
      true, new URI(String.format("http://localhost:%s", mockBackEnd.getPort())), false, null, null);
    maintainJobService.setApplicationContext(context);
    mockBackEnd.enqueue(new MockResponse()
      .setBody(DataListUtil.getMapper().writeValueAsString(new ServiceData().setType(AnswerType.ERROR)))
      .addHeader("Content-Type", "application/json"));

    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getProperty(anyString())).willReturn("ES");
    Trigger trigger = mock(Trigger.class);
    when(queryUtil.getParameters(isNull(), any(), any())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(trigger.getKey()).thenReturn(new TriggerKey("DummyTrigger"));

    Future<ServiceData> serviceData = maintainJobService.executeJob(new Task()
        .setTaskId(1)
        .setAction("maintainId")
        .setTrigger(trigger)
        .setParameterList(Arrays.asList(
          new TaskParameter().setSource("1").setName("1").setValue("1").setType("STRING"),
          new TaskParameter().setSource("2").setName("2").setValue("2").setType("INTEGER")
        )),
      new TaskExecution(), new JobDataMap());

    assertNotNull(serviceData);
    assertEquals(AnswerType.ERROR, serviceData.get().getType());
  }
}
