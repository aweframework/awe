package com.almis.awe.scheduler.job.scheduled;

import com.almis.awe.scheduler.service.scheduled.MaintainJobService;
import lombok.extern.slf4j.Slf4j;

/**
 * Class implements Quartz job Launch a batch thread
 *
 * @author pvidal
 */
@Slf4j
public class MaintainJob extends SchedulerJob {
  /**
   * Autowired constructor
   *
   * @param jobService
   */
  public MaintainJob(MaintainJobService jobService) {
    super(jobService);
  }
}
