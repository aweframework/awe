package com.almis.awe.scheduler.autoconfigure;

import com.almis.awe.model.service.DataListService;
import com.almis.awe.model.tracker.AweConnectionTracker;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.autoconfigure.config.SchedulerConfigProperties;
import com.almis.awe.scheduler.dao.*;
import com.almis.awe.scheduler.filechecker.FTPFileChecker;
import com.almis.awe.scheduler.filechecker.FileChecker;
import com.almis.awe.scheduler.filechecker.FileClient;
import com.almis.awe.scheduler.filechecker.FolderFileChecker;
import com.almis.awe.scheduler.job.execution.ProgressJob;
import com.almis.awe.scheduler.job.execution.TimeoutJob;
import com.almis.awe.scheduler.job.report.BroadcastReportJob;
import com.almis.awe.scheduler.job.report.EmailReportJob;
import com.almis.awe.scheduler.job.report.MaintainReportJob;
import com.almis.awe.scheduler.job.scheduled.CommandJob;
import com.almis.awe.scheduler.job.scheduled.MaintainJob;
import com.almis.awe.scheduler.listener.SchedulerEventListener;
import com.almis.awe.scheduler.listener.SchedulerJobListener;
import com.almis.awe.scheduler.listener.SchedulerTriggerListener;
import com.almis.awe.scheduler.service.*;
import com.almis.awe.service.BroadcastService;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import org.apache.commons.net.ftp.FTPClient;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * Scheduler module configuration
 */
@Configuration
@EnableConfigurationProperties({SchedulerConfigProperties.class})
public class SchedulerConfig {

  private final SchedulerConfigProperties schedulerConfigProperties;

  @Autowired
  public SchedulerConfig(SchedulerConfigProperties schedulerConfigProperties) {
    this.schedulerConfigProperties = schedulerConfigProperties;
  }

  /**
   * Define Scheduler
   *
   * @return Scheduler
   */
  @Bean
  public Scheduler scheduler(SchedulerFactoryBean factory) {
    return factory.getScheduler();
  }

  /**
   * Define Runtime
   *
   * @return Runtime
   */
  @Bean
  public Runtime runtime() {
    return Runtime.getRuntime();
  }

  /**
   * Define FTP Client
   *
   * @return FTP Client
   */
  @Bean
  @Scope("prototype")
  public FTPClient ftpClient() {
    return new FTPClient();
  }

  /**
   * Define FileClient
   *
   * @return FileClient
   */
  @Bean
  @Scope("prototype")
  public FileClient fileClient() {
    return new FileClient();
  }

  /**
   * Define Scheduler service
   *
   * @return Scheduler service
   */
  @Bean
  public SchedulerService schedulerService(TaskDAO taskDAO, SchedulerDAO schedulerDAO, CalendarDAO calendarDAO) {
    return new SchedulerService(taskDAO, schedulerDAO, calendarDAO);
  }

  /**
   * Define Task service
   *
   * @return Task service
   */
  @Bean
  public TaskService taskService(QueryService queryService, QueryUtil queryUtil, TaskDAO taskDAO) {
    return new TaskService(queryService, queryUtil, taskDAO);
  }

  /**
   * Define Scheduler service
   *
   * @return Scheduler service
   */
  @Bean
  public ExecutionService timeoutService(Scheduler scheduler) {
    return new ExecutionService(scheduler);
  }

  /*
   * JOB SERVICES
   */

  /**
   * Define Maintain job service
   *
   * @return Scheduler service
   */
  @Bean
  public MaintainJobService maintainJobService(ExecutionService executionService, MaintainService maintainService, QueryUtil queryUtil, TaskDAO taskDAO, ApplicationEventPublisher eventPublisher) {
    return new MaintainJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher, schedulerConfigProperties.getTaskTimeout());
  }

  /**
   * Define Command job service
   *
   * @return Scheduler service
   */
  @Bean
  public CommandJobService commandJobService(ExecutionService executionService, MaintainService maintainService, QueryUtil queryUtil, TaskDAO taskDAO, ApplicationEventPublisher eventPublisher, CommandDAO commandDAO) {
    return new CommandJobService(executionService, maintainService, queryUtil, taskDAO, eventPublisher, commandDAO, schedulerConfigProperties.getTaskTimeout());
  }

  /*
   * JOB TYPES
   */

  /**
   * Define progress job
   *
   * @return Scheduler job
   */
  @Bean
  @Scope("prototype")
  public ProgressJob progressJob(ApplicationEventPublisher eventPublisher) {
    return new ProgressJob(eventPublisher);
  }

  /**
   * Define timeout job
   *
   * @return Scheduler job
   */
  @Bean
  @Scope("prototype")
  public TimeoutJob timeoutJob() {
    return new TimeoutJob();
  }

  /**
   * Define Maintain job
   *
   * @return Scheduler job
   */
  @Bean
  @Scope("prototype")
  public MaintainJob maintainJob(MaintainJobService jobService) {
    return new MaintainJob(jobService);
  }

  /**
   * Define Command job
   *
   * @return Scheduler job
   */
  @Bean
  @Scope("prototype")
  public CommandJob commandJob(CommandJobService jobService) {
    return new CommandJob(jobService);
  }

  /**
   * Define maintain report job
   *
   * @return Scheduler job
   */
  @Bean
  @Scope("prototype")
  public MaintainReportJob maintainReportJob(QueryUtil queryUtil, MaintainService maintainService) {
    return new MaintainReportJob(queryUtil, maintainService);
  }

  /**
   * Define email report job
   *
   * @return Scheduler job
   */
  @Bean
  @Scope("prototype")
  public EmailReportJob emailReportJob(QueryUtil queryUtil, MaintainService maintainService, QueryService queryService) {
    return new EmailReportJob(queryUtil, maintainService, queryService);
  }

  /**
   * Define broadcast report job
   *
   * @return Scheduler job
   */
  @Bean
  @Scope("prototype")
  public BroadcastReportJob broadcastReportJob(BroadcastService broadcastService) {
    return new BroadcastReportJob(broadcastService);
  }

  /*
   * DAO
   */

  /**
   * Database Data Object Access
   *
   * @return Database DAO
   */
  @Bean
  public DatabaseDAO databaseDAO(QueryService queryService) {
    return new DatabaseDAO(queryService);
  }

  /**
   * Calendar Data Object Access
   *
   * @return Calendar DAO
   */
  @Bean
  public CalendarDAO calendarDAO(Scheduler scheduler, QueryService queryService, QueryUtil queryUtil, DataListService dataListService) {
    return new CalendarDAO(scheduler, queryService, queryUtil, dataListService);
  }

  /**
   * Scheduler Data Object Access
   *
   * @return Scheduler DAO
   */
  @Bean
  public SchedulerDAO schedulerDAO(Scheduler scheduler, CalendarDAO calendarDAO, TaskService taskService,
                                   SchedulerTriggerListener triggerListener, SchedulerJobListener jobListener) {
    return new SchedulerDAO(scheduler, schedulerConfigProperties.isTasksLoadOnStart(), calendarDAO, taskService, triggerListener, jobListener);
  }

  /**
   * Task Data Object Access
   *
   * @return Task DAO
   */
  @Bean
  public TaskDAO taskDAO(Scheduler scheduler, QueryService queryService, MaintainService maintainService,
                         QueryUtil queryUtil, CalendarDAO calendarDAO, ServerDAO serverDAO, FileChecker fileChecker,
                         DataListService dataListService) {
    return new TaskDAO(scheduler, schedulerConfigProperties.getStoredExecutions(), schedulerConfigProperties.getExecutionLogPath(), queryService, maintainService, queryUtil, calendarDAO, serverDAO, fileChecker, dataListService);
  }

  /**
   * File Data Object Access
   *
   * @return File DAO
   */
  @Bean
  public FileDAO fileDAO(MaintainService maintainService, QueryUtil queryUtil) {
    return new FileDAO(maintainService, queryUtil);
  }

  /**
   * Server Data Object Access
   *
   * @return File DAO
   */
  @Bean
  public ServerDAO serverDAO(QueryService queryService, QueryUtil queryUtil, DataListService dataListService) {
    return new ServerDAO(queryService, queryUtil, dataListService);
  }

  /**
   * Server Data Object Access
   *
   * @return File DAO
   */
  @Bean
  public CommandDAO commandDAO(Runtime runtime) {
    return new CommandDAO(runtime);
  }

  /*
   * Checkers
   */

  /**
   * Define file checker
   *
   * @return File checker
   */
  @Bean
  public FileChecker fileChecker(FTPFileChecker ftpFileChecker, FolderFileChecker folderFileChecker) {
    return new FileChecker(ftpFileChecker, folderFileChecker);
  }

  /**
   * Define ftp file checker
   *
   * @return FTP File checker
   */
  @Bean
  public FTPFileChecker ftpFileChecker(FileDAO fileDAO, FTPClient ftpClient) {
    return new FTPFileChecker(fileDAO, ftpClient);
  }

  /**
   * Define folder file checker
   *
   * @return Folder File checker
   */
  @Bean
  public FolderFileChecker folderFileChecker(FileDAO fileDAO, FileClient fileClient) {
    return new FolderFileChecker(fileDAO, fileClient);
  }

  /*********************************************************************************************************************
   LISTENERS
   ********************************************************************************************************************/
  @Bean
  public SchedulerTriggerListener schedulerTriggerListener(TaskDAO taskDAO) {
    return new SchedulerTriggerListener(taskDAO);
  }

  @Bean
  public SchedulerJobListener schedulerJobListener(ApplicationEventPublisher eventPublisher) {
    return new SchedulerJobListener(eventPublisher);
  }

  @Bean
  public SchedulerEventListener schedulerEventListener(BroadcastService broadcastService, AweConnectionTracker connectionTracker,
                                                       TaskDAO taskDAO) {
    return new SchedulerEventListener(broadcastService, connectionTracker, taskDAO);
  }
}
