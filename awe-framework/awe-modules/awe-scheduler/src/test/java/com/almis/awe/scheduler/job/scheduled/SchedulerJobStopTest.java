package com.almis.awe.scheduler.job.scheduled;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.service.scheduled.JobService;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchedulerJobStopTest {

  private final JobService jobService = mock(JobService.class);
  private final SchedulerJob job = new SchedulerJob(jobService) {
  };

  @Test
  void stopCancelsTheRunningProcessOfTheJob() throws Exception {
    CompletableFuture<ServiceData> process = new CompletableFuture<>();
    when(jobService.executeJob(any(), any(), any())).thenReturn(process);
    // The batch waits on the process: stop it from inside, as another thread would
    when(jobService.launchBatch(any(), any())).thenAnswer(invocation -> {
      assertThat(job.stop()).isTrue();
      return new ServiceData();
    });

    job.launch(new JobDataMap());

    assertThat(process.isCancelled()).isTrue();
    assertThat(job.isStopped()).isTrue();
  }

  @Test
  void stopReportsAJobThatHasNotLaunchedItsProcessYet() {
    assertThat(job.stop()).isFalse();
  }
}
