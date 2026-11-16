package com.almis.awe.scheduler.listener;

import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.almis.awe.scheduler.bean.event.SchedulerTaskFinishedEvent;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.job.scheduled.SchedulerJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulerJobListenerDeferredTest {

  @Mock
  private ApplicationEventPublisher eventPublisher;
  @Mock
  private JobExecutionContext context;
  @Mock
  private SchedulerJob job;

  private SchedulerJobListener listener() {
    when(context.getJobInstance()).thenReturn(job);
    return new SchedulerJobListener(eventPublisher, new PrototypeRequestBeanHolder());
  }

  @Test
  void aDeferredJobDoesNotPublishTheFinishedEvent() {
    SchedulerJobListener listener = listener();
    when(job.isDeferred()).thenReturn(true);

    listener.jobWasExecuted(context, null);

    verifyNoInteractions(eventPublisher);
  }

  @Test
  void anyOtherJobPublishesTheFinishedEvent() {
    SchedulerJobListener listener = listener();
    when(job.getTask()).thenReturn(new Task());
    when(job.getExecution()).thenReturn(new TaskExecution().setExecutionTime(1));

    listener.jobWasExecuted(context, null);

    verify(eventPublisher).publishEvent(any(SchedulerTaskFinishedEvent.class));
  }
}
