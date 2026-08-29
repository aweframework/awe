package com.almis.awe.scheduler.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import com.almis.awe.scheduler.constant.TaskConstants;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.scheduler.service.scheduled.JobService;
import com.almis.awe.scheduler.service.scheduled.MaintainJobService;
import com.almis.awe.service.MaintainService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import org.slf4j.MDC;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Future;

import static com.almis.awe.scheduler.constant.TaskConstants.LOG_BY_TASK_EXECUTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
  @Mock
  RestTemplate restTemplate;
  @Mock
  ExecutionLogStore executionLogStore;
  private MaintainJobService maintainJobService;
  private static MockWebServer mockBackEnd;

  @BeforeAll
  static void setUp() throws IOException {
    mockBackEnd = new MockWebServer();
    mockBackEnd.start();
  }

  @AfterAll
  static void tearDown() throws IOException {
    mockBackEnd.close();
  }

  @Test
  void testExecuteJobLocal() throws Exception {
    maintainJobService = new MaintainJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher,
      DataListUtil.getMapper(), executionLogStore, Duration.ofSeconds(5), false, null, false, null, null, restTemplate);
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
      new TaskExecution().setTaskId(1).setExecutionId(1), new JobDataMap());

    assertNotNull(serviceData);
    assertEquals(AnswerType.OK, serviceData.get().getType());
  }

  @Test
  void testExecuteJobLocalErrorInMaintain() throws Exception {
    maintainJobService = new MaintainJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher,
      DataListUtil.getMapper(), executionLogStore, Duration.ofSeconds(5),
      false, null, false, null, null, restTemplate);
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
      new TaskExecution().setTaskId(1).setExecutionId(1), new JobDataMap());

    assertNotNull(serviceData);
    assertEquals(AnswerType.ERROR, serviceData.get().getType());
  }

  @Test
  void testExecuteJobRemote() throws Exception {
    maintainJobService = new MaintainJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher,
      DataListUtil.getMapper(), executionLogStore, Duration.ofSeconds(5),
      true, new URI(String.format("http://localhost:%s", mockBackEnd.getPort())), true, "user", "pass", restTemplate);
    maintainJobService.setApplicationContext(context);

    // Back end response
    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LoginResponse.class))).thenReturn(ResponseEntity.ok(new LoginResponse().setToken("token")));
    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq( ServiceData.class), anyString())).thenReturn(ResponseEntity.ok(new ServiceData()));

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
      new TaskExecution().setTaskId(1).setExecutionId(1), new JobDataMap());

    assertNotNull(serviceData);
    assertEquals(AnswerType.OK, serviceData.get().getType());
  }

  @Test
  void testExecuteJobRemoteErrorInAuthentication() throws Exception {
    final URI uri = new URI(String.format("http://localhost:%s", mockBackEnd.getPort()));
        maintainJobService = new MaintainJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher,
            DataListUtil.getMapper(), executionLogStore, Duration.ofSeconds(5),
            true, uri, true, "user", "pass", restTemplate);
    maintainJobService.setApplicationContext(context);

    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(LoginResponse.class))).thenReturn(ResponseEntity.ok(new LoginResponse()));
    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq( ServiceData.class), anyString())).thenReturn(ResponseEntity.ok(new ServiceData().setType(AnswerType.ERROR)));

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
      new TaskExecution().setTaskId(1).setExecutionId(1), new JobDataMap());

    assertNotNull(serviceData);
    assertEquals(AnswerType.ERROR, serviceData.get().getType());
  }

  @Test
  void testExecuteJobRemoteNoAuth() throws Exception {
    maintainJobService = new MaintainJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher,
      DataListUtil.getMapper(), executionLogStore, Duration.ofSeconds(5),
      true, new URI(String.format("http://localhost:%s", mockBackEnd.getPort())), false, null, null, restTemplate);
    maintainJobService.setApplicationContext(context);

    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq( ServiceData.class), anyString())).thenReturn(ResponseEntity.ok(new ServiceData()));

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
      new TaskExecution().setTaskId(1).setExecutionId(1), new JobDataMap());

    assertNotNull(serviceData);
    assertEquals(AnswerType.OK, serviceData.get().getType());
  }

  @Test
  void testExecuteJobRemoteNoAuthErrorInResponse() throws Exception {
    maintainJobService = new MaintainJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher,
      DataListUtil.getMapper(), executionLogStore, Duration.ofSeconds(5),
      true, new URI(String.format("http://localhost:%s", mockBackEnd.getPort())), false, null, null, restTemplate);
    maintainJobService.setApplicationContext(context);

    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq( ServiceData.class), anyString())).thenReturn(ResponseEntity.ok(new ServiceData().setType(AnswerType.ERROR)));

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
      new TaskExecution().setTaskId(1).setExecutionId(1), new JobDataMap());

    assertNotNull(serviceData);
    assertEquals(AnswerType.ERROR, serviceData.get().getType());
  }

  /**
   * Regression test for GitLab #685: a task parameter named {@code database} must reach the
   * maintain unchanged. Previously the forced {@code parameters.put("database", task.getDatabase())}
   * (now removed) ran AFTER the TaskParameter loop and clobbered any user-supplied {@code database}
   * parameter with the dead {@code task.getDatabase()} value (always null). With the alias-free
   * routing the user value survives and no {@code _database_} routing key is forced.
   */
  @Test
  void testExecuteJobDatabaseParameterPassthrough() throws Exception {
    maintainJobService = new MaintainJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher,
      DataListUtil.getMapper(), executionLogStore, Duration.ofSeconds(5), false, null, false, null, null, restTemplate);
    maintainJobService.setApplicationContext(context);

    Trigger trigger = mock(Trigger.class);
    when(queryUtil.getParameters(isNull(), any(), any())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(trigger.getKey()).thenReturn(new TriggerKey("DummyTrigger"));
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    Future<ServiceData> serviceData = maintainJobService.executeJob(new Task()
        .setTaskId(1)
        .setAction("maintainId")
        .setTrigger(trigger)
        .setParameterList(Arrays.asList(
          new TaskParameter().setSource("1").setName("database").setValue("myDbValue").setType("STRING")
        )),
      new TaskExecution().setTaskId(1).setExecutionId(1), new JobDataMap());

    assertNotNull(serviceData);
    assertEquals(AnswerType.OK, serviceData.get().getType());

    // The "database" task parameter value must survive (not overwritten by the removed forced put)
    ArgumentCaptor<ObjectNode> paramsCaptor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService, times(1)).launchPrivateMaintain(eq("maintainId"), paramsCaptor.capture());
    ObjectNode captured = paramsCaptor.getValue();
    assertEquals("myDbValue", captured.get("database").asText());
    // The launcher default parameter is still set
    assertNotNull(captured.get("launcher"));
  }

  /**
   * D1 corrective / pre-existing MDC leak fix: an unchecked exception from the job body (anything
   * beyond the caught {@link AWException}) must still run {@code endLogging} - MDC cleared, store
   * completion signalled - before propagating, instead of leaking the MDC onto the pooled job
   * thread.
   *
   * <p>Also pins the C1 gate fix: {@code jobResult} stays {@code null} on this path, so the
   * completion summary line renders the abnormal-completion locale, never a misleading "finished
   * with result OK" one.</p>
   */
  @Test
  void unexpectedExceptionInLocalMaintainStillRunsEndLoggingAndPropagates() throws Exception {
    maintainJobService = new MaintainJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher,
      DataListUtil.getMapper(), executionLogStore, Duration.ofSeconds(5), false, null, false, null, null, restTemplate);
    maintainJobService.setApplicationContext(context);

    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getLocaleWithLanguage(anyString(), any(), any(Object[].class)))
      .willAnswer(invocation -> {
        Object[] tokens = Arrays.copyOfRange(invocation.getArguments(), 2, invocation.getArguments().length);
        return invocation.getArgument(0) + "|" + Arrays.toString(tokens);
      });
    Trigger trigger = mock(Trigger.class);
    when(queryUtil.getParameters(isNull(), any(), any())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(trigger.getKey()).thenReturn(new TriggerKey("DummyTrigger"));
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class)))
      .thenThrow(new IllegalStateException("unexpected failure"));

    TaskExecution execution = new TaskExecution().setTaskId(1).setExecutionId(1).setInitialDate(new Date());

    ListAppender<ILoggingEvent> logAppender = attachListAppender();
    try {
      assertThrows(IllegalStateException.class, () -> maintainJobService.executeJob(new Task()
          .setTaskId(1)
          .setAction("maintainId")
          .setTrigger(trigger)
          .setParameterList(Arrays.asList(
            new TaskParameter().setSource("1").setName("1").setValue("1").setType("STRING")
          )),
        execution, new JobDataMap()));
    } finally {
      detachListAppender(logAppender);
    }

    verify(executionLogStore).complete(any(), any());
    assertNull(MDC.get(LOG_BY_TASK_EXECUTION));

    ILoggingEvent summaryEvent = logAppender.list.stream()
      .filter(event -> event.getLevel() == Level.INFO)
      .findFirst()
      .orElseThrow(() -> new AssertionError("No summary line was logged"));
    assertTrue(summaryEvent.getFormattedMessage().startsWith("SCHEDULER_EXECUTION_LOG_COMPLETED_ABNORMALLY|"),
      summaryEvent.getFormattedMessage());
  }

  /**
   * D2: the remote maintain callback SHALL carry the execution-key header unconditionally,
   * independent of the remote endpoint's own secure/insecure mode, so the application side alone
   * decides whether to honor it.
   */
  @Test
  void remoteMaintainCallCarriesTheExecutionKeyHeaderBuiltFromTaskAndExecutionId() throws Exception {
    maintainJobService = new MaintainJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher,
      DataListUtil.getMapper(), executionLogStore, Duration.ofSeconds(5),
      true, new URI(String.format("http://localhost:%s", mockBackEnd.getPort())), false, null, null, restTemplate);
    maintainJobService.setApplicationContext(context);

    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(ServiceData.class), anyString()))
      .thenReturn(ResponseEntity.ok(new ServiceData()));

    Trigger trigger = mock(Trigger.class);
    when(queryUtil.getParameters(isNull(), any(), any())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(trigger.getKey()).thenReturn(new TriggerKey("DummyTrigger"));

    Future<ServiceData> serviceData = maintainJobService.executeJob(new Task()
        .setTaskId(1)
        .setAction("maintainId")
        .setTrigger(trigger)
        .setParameterList(Arrays.asList(
          new TaskParameter().setSource("1").setName("1").setValue("1").setType("STRING")
        )),
      new TaskExecution().setTaskId(7).setExecutionId(9), new JobDataMap());

    assertNotNull(serviceData);
    ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate).postForEntity(anyString(), entityCaptor.capture(), eq(ServiceData.class), anyString());
    assertEquals("7-9", entityCaptor.getValue().getHeaders().getFirst(TaskConstants.EXECUTION_KEY_HEADER));
  }

  /**
   * D2: the header is omitted (never sent with a partial/garbled value) when either id is null,
   * matching {@link com.almis.awe.scheduler.log.ExecutionKey}'s non-null contract.
   */
  @Test
  void remoteMaintainCallOmitsTheExecutionKeyHeaderWhenEitherIdIsNull() throws Exception {
    maintainJobService = new MaintainJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher,
      DataListUtil.getMapper(), executionLogStore, Duration.ofSeconds(5),
      true, new URI(String.format("http://localhost:%s", mockBackEnd.getPort())), false, null, null, restTemplate);
    maintainJobService.setApplicationContext(context);

    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(ServiceData.class), anyString()))
      .thenReturn(ResponseEntity.ok(new ServiceData()));

    Trigger trigger = mock(Trigger.class);
    when(queryUtil.getParameters(isNull(), any(), any())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(trigger.getKey()).thenReturn(new TriggerKey("DummyTrigger"));

    Future<ServiceData> serviceData = maintainJobService.executeJob(new Task()
        .setTaskId(1)
        .setAction("maintainId")
        .setTrigger(trigger)
        .setParameterList(Arrays.asList(
          new TaskParameter().setSource("1").setName("1").setValue("1").setType("STRING")
        )),
      new TaskExecution().setTaskId(null).setExecutionId(9), new JobDataMap());

    assertNotNull(serviceData);
    ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate).postForEntity(anyString(), entityCaptor.capture(), eq(ServiceData.class), anyString());
    assertNull(entityCaptor.getValue().getHeaders().getFirst(TaskConstants.EXECUTION_KEY_HEADER));
  }

  /**
   * D2: the local (non-remote) branch never calls {@code launchRemoteMaintainRest}, so it has no
   * header to carry - regression guard, not new behaviour.
   */
  @Test
  void localMaintainCallNeverInteractsWithRestTemplate() throws Exception {
    maintainJobService = new MaintainJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher,
      DataListUtil.getMapper(), executionLogStore, Duration.ofSeconds(5), false, null, false, null, null, restTemplate);
    maintainJobService.setApplicationContext(context);

    Trigger trigger = mock(Trigger.class);
    when(queryUtil.getParameters(isNull(), any(), any())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(trigger.getKey()).thenReturn(new TriggerKey("DummyTrigger"));
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    maintainJobService.executeJob(new Task()
        .setTaskId(1)
        .setAction("maintainId")
        .setTrigger(trigger)
        .setParameterList(Arrays.asList(
          new TaskParameter().setSource("1").setName("1").setValue("1").setType("STRING")
        )),
      new TaskExecution().setTaskId(7).setExecutionId(9), new JobDataMap());

    verifyNoInteractions(restTemplate);
  }

  private ListAppender<ILoggingEvent> attachListAppender() {
    Logger logger = (Logger) LoggerFactory.getLogger(JobService.class);
    ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);
    return logAppender;
  }

  private void detachListAppender(ListAppender<ILoggingEvent> logAppender) {
    Logger logger = (Logger) LoggerFactory.getLogger(JobService.class);
    logger.detachAppender(logAppender);
  }
}
