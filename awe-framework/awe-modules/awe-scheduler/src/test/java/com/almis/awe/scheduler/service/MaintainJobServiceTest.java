package com.almis.awe.scheduler.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.service.MaintainService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.concurrent.Future;

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

  @InjectMocks
  private MaintainJobService maintainJobService;
  @Mock
  QueryUtil queryUtil;
  @Mock
  AweElements aweElements;
  @Mock
  ApplicationContext context;
  @Mock
  MaintainService maintainService;

  /**
   * Initializes json mapper for tests
   */
  @BeforeEach
  public void initBeans() throws Exception {
    maintainJobService.setApplicationContext(context);
  }

  /**
   * Test context loaded
   */
  @Test
  void contextLoads() {
    // Check that controller are active
    assertNotNull(maintainJobService);
  }

  @Test
  void testExecuteJob() throws AWException {
    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getProperty(anyString())).willReturn("ES");
    Trigger trigger = mock(Trigger.class);
    when(queryUtil.getParameters(isNull(), any(), any())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(trigger.getKey()).thenReturn(new TriggerKey("DummyTrigger"));
    when(maintainService.launchPrivateMaintain(isNull(), any(ObjectNode.class))).thenReturn(new ServiceData());


    Future<ServiceData> serviceData = maintainJobService.executeJob(new Task()
                    .setTaskId(1)
                    .setTrigger(trigger)
                    .setParameterList(Arrays.asList(
                            new TaskParameter().setSource("1").setName("1").setValue("1").setType("STRING"),
                            new TaskParameter().setSource("2").setName("2").setValue("2").setType("INTEGER")
                    )),
            new TaskExecution(), new JobDataMap());

    assertNotNull(serviceData);
  }
}
