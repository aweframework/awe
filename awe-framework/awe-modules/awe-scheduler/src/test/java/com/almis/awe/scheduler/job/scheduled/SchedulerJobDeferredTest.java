package com.almis.awe.scheduler.job.scheduled;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.service.scheduled.JobService;
import com.almis.awe.scheduler.util.SchedulerDeferred;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchedulerJobDeferredTest {

  private final JobService jobService = mock(JobService.class);
  private final SchedulerJob job = new SchedulerJob(jobService) {
  };

  @Test
  void aJobWhoseExecutionWasDeferredReportsItSoNoFinishedEventIsPublished() throws Exception {
    Future<ServiceData> answer = CompletableFuture.completedFuture(SchedulerDeferred.accepted("job-77", "Sent"));
    when(jobService.executeJob(any(), any(), any())).thenReturn(answer);
    when(jobService.launchBatch(job, answer)).thenReturn(SchedulerDeferred.accepted("job-77", "Sent"));

    job.launch(new JobDataMap());

    assertThat(job.isDeferred()).isTrue();
  }

  @Test
  void anyOtherJobIsNotDeferred() throws Exception {
    Future<ServiceData> answer = CompletableFuture.completedFuture(new ServiceData());
    when(jobService.executeJob(any(), any(), any())).thenReturn(answer);
    when(jobService.launchBatch(job, answer)).thenReturn(new ServiceData());

    job.launch(new JobDataMap());

    assertThat(job.isDeferred()).isFalse();
  }
}
