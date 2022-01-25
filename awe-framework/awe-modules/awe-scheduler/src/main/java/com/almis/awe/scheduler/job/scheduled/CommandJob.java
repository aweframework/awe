package com.almis.awe.scheduler.job.scheduled;

import com.almis.awe.scheduler.service.CommandJobService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author pvidal
 */
@Slf4j
public class CommandJob extends SchedulerJob {
  /**
   * Autowired constructor
   *
   * @param jobService Command job service
   */
  public CommandJob(CommandJobService jobService) {
    super(jobService);
  }
}
