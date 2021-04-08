package com.almis.awe.test.unit.scheduler;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.dao.SchedulerDAO;
import com.almis.awe.test.unit.TestUtil;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.*;
import org.springframework.context.ApplicationContext;

import javax.naming.NamingException;
import java.util.*;

import static com.almis.awe.scheduler.constant.JobConstants.TASK_VISIBLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Class used for testing queries through ActionController
 *
 * @author jbellon
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Log4j2
@RunWith(MockitoJUnitRunner.class)
public class SchedulerDAOTest extends TestUtil {

  @InjectMocks
  private SchedulerDAO schedulerDAO;

  @Mock
  private Scheduler scheduler;

  /**
   * Test context loaded
   *
   * @throws NamingException Test error
   */
  @Test
  public void contextLoads() {
    // Check that controller are active
    assertThat(schedulerDAO).isNotNull();
  }

  /**
   * Get executing jobs without scheduler configured
   */
  @Test
  public void getCurrentlyExecutingJobsNoScheduler() throws Exception {
    // Mock and spy
    JobExecutionContext jobExecutionContext = Mockito.mock(JobExecutionContext.class);
    List<JobExecutionContext> jobExecutionContextList = Collections.singletonList(jobExecutionContext);
    //given(scheduler.getCurrentlyExecutingJobs()).willReturn(jobExecutionContextList);

    // Run method
    ServiceData serviceData = schedulerDAO.getCurrentlyExecutingJobs();

    // Assert
    assertThat(serviceData).isNotNull();
  }

  /**
   * Get executing jobs without scheduler configured
   */
  @Test
  public void getCurrentlyExecutingJobsStandby() throws Exception {
    // Mock and spy
    JobExecutionContext jobExecutionContext = Mockito.mock(JobExecutionContext.class);
    Trigger trigger = Mockito.mock(Trigger.class);
    when(jobExecutionContext.getTrigger()).thenReturn(trigger);
    when(trigger.getKey()).thenReturn(new TriggerKey("test", "test"));
    List<JobExecutionContext> jobExecutionContextList = Collections.singletonList(jobExecutionContext);
    when(scheduler.getTriggerState(any())).thenReturn(Trigger.TriggerState.COMPLETE);
    given(scheduler.getCurrentlyExecutingJobs()).willReturn(jobExecutionContextList);
    given(scheduler.isInStandbyMode()).willReturn(false);
    given(scheduler.isStarted()).willReturn(true);

    // Run method
    ServiceData serviceData = schedulerDAO.getCurrentlyExecutingJobs();

    // Assert
    assertEquals(1, serviceData.getDataList().getRecords());
  }

  /**
   * Get executing jobs without scheduler configured
   */
  @Test
  public void getCurrentlyExecutingJobsNoStarted() throws Exception {
    // Mock and spy
    given(scheduler.isInStandbyMode()).willReturn(false);
    given(scheduler.isStarted()).willReturn(false);

    // Run method
    ServiceData serviceData = schedulerDAO.getCurrentlyExecutingJobs();

    // Assert
    assertEquals(0, serviceData.getDataList().getRecords());
  }

  /**
   * Get executing jobs with scheduler configured
   */
  @Test
  public void getCurrentlyExecutingJobsWithScheduler() throws Exception {
    // Mock and spy
    JobExecutionContext jobExecutionContext = Mockito.mock(JobExecutionContext.class);
    given(jobExecutionContext.getTrigger()).willReturn(TriggerBuilder.newTrigger().withIdentity("1", "TEST_GROUP").build());
    List<JobExecutionContext> jobExecutionContextList = Collections.singletonList(jobExecutionContext);
    given(scheduler.getCurrentlyExecutingJobs()).willReturn(jobExecutionContextList);
    given(scheduler.isInStandbyMode()).willReturn(false);
    given(scheduler.isStarted()).willReturn(true);
    given(scheduler.getTriggerState(any())).willReturn(Trigger.TriggerState.NORMAL);

    // Run method
    ServiceData serviceData = schedulerDAO.getCurrentlyExecutingJobs();

    // Assert
    assertEquals(1, serviceData.getDataList().getRecords());
  }

  /**
   * Get configured jobs with scheduler configured
   */
  @Test
  public void getConfiguredJobsWithScheduler() throws Exception {
    // Mock and spy
    JobExecutionContext jobExecutionContext = Mockito.mock(JobExecutionContext.class);
    List<JobExecutionContext> jobExecutionContextList = Collections.singletonList(jobExecutionContext);
    given(scheduler.isInStandbyMode()).willReturn(false);
    given(scheduler.isStarted()).willReturn(true);
    given(scheduler.getTriggerGroupNames()).willReturn(Arrays.asList("1", "2", "3", "TEST_GROUP"));
    Set<TriggerKey> triggerKeySet = new HashSet<>();
    triggerKeySet.add(new TriggerKey("1", "TEST_GROUP"));
    given(scheduler.getTriggerKeys(any())).willReturn(triggerKeySet);
    Trigger trigger = TriggerBuilder.newTrigger().withIdentity("1", "TEST_GROUP").build();
    trigger.getJobDataMap().put(TASK_VISIBLE, true);
    given(scheduler.getTrigger(any())).willReturn(trigger);

    // Run method
    ServiceData serviceData = schedulerDAO.getConfiguredJobs();

    // Assert
    assertEquals(4, serviceData.getDataList().getRecords());
  }
}
