package com.almis.awe.test.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Demo data of the scheduler Activity screen for the local hot-reload app (awe-react.demo.scheduler-activity=true in
 * application-hot-reload.properties): on every start it replaces the demo tasks 9012-9020 and their executions of the
 * last 7 days, with dates relative to now. It runs once the scheduler has started, because the start interrupts the
 * running executions, and schedules the demo scheduled tasks before loading their history. The tasks are scheduled
 * with the insert of the scheduler, which creates the Quartz job when it is absent and reschedules it when the scheduler
 * already loaded it at its start (a restart keeps the demo tasks of the last run): the update deletes the job first and
 * fails on that second start, so the loader is the same on every start.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "awe-react.demo.scheduler-activity", havingValue = "true")
public class SchedulerActivityDemoData {

  private static final String SCRIPT_PATH = "db/demo/hsqldb/";
  private static final List<Integer> SCHEDULED_TASKS = List.of(9017, 9018);
  // Demo calendar «Festivos ES 2026», used by task 9017: it is added to the scheduler before the task is scheduled
  private static final int DEMO_CALENDAR = 9001;

  private final DataSource dataSource;
  private final SchedulerService schedulerService;

  /**
   * Constructor
   *
   * @param dataSource       Application data source
   * @param schedulerService Scheduler service
   */
  public SchedulerActivityDemoData(DataSource dataSource, SchedulerService schedulerService) {
    this.dataSource = dataSource;
    this.schedulerService = schedulerService;
  }

  /**
   * Load the demo data once the application (and the scheduler) has started
   */
  @EventListener(ApplicationReadyEvent.class)
  public void load() {
    try {
      runScripts("scheduler-activity-demo-clean.sql", "scheduler-activity-demo-tasks.sql");
      addCalendar();
      for (Integer taskId : SCHEDULED_TASKS) {
        schedule(taskId);
      }
      runScripts("scheduler-activity-demo-history.sql");
      logger.info("[DEMO] Scheduler activity demo data loaded: tasks 9012-9020");
    } catch (DataAccessException exc) {
      logger.warn("[DEMO] Scheduler activity demo data not loaded", exc);
    }
  }

  /**
   * Add (or replace) the demo calendar in the scheduler, so the task that uses it can be scheduled on a clean database
   */
  private void addCalendar() {
    try {
      schedulerService.insertSchedulerCalendar(DEMO_CALENDAR, true, false);
    } catch (AWException exc) {
      logger.warn("[DEMO] Demo calendar {} not added to the scheduler", DEMO_CALENDAR, exc);
    }
  }

  /**
   * Add a demo scheduled task to the scheduler, created when absent, rescheduled when the scheduler already has it
   *
   * @param taskId Task identifier
   */
  private void schedule(Integer taskId) {
    try {
      schedulerService.insertSchedulerTask(taskId);
    } catch (AWException exc) {
      logger.warn("[DEMO] Demo task {} not scheduled", taskId, exc);
    }
  }

  /**
   * Run the demo scripts in order
   *
   * @param scripts Script names
   */
  private void runScripts(String... scripts) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
    Arrays.stream(scripts).map(script -> new ClassPathResource(SCRIPT_PATH + script)).forEach(populator::addScript);
    DatabasePopulatorUtils.execute(populator, dataSource);
  }
}
