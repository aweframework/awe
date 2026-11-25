package com.almis.awe.scheduler.bean.task;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * How a task is launched, as the task catalog summarizes it («When»): its launch type, its repetition, its calendar,
 * the file it waits for and the tasks it runs after
 */
@Data
@Accessors(chain = true)
public class TaskLaunch {
  /**
   * Launch type ({@code TaskLaunchType} value)
   */
  private Integer launchType;
  /**
   * Repetition type: 0 seconds, 1 minutes, 2 hours (an interval of repeatNumber), 3 days, 4 months, 5 years, 6 once,
   * 7 custom
   */
  private Integer repeatType;
  /**
   * Interval of the repetition
   */
  private Integer repeatNumber;
  /**
   * Days of the week, as the task editor numbers them (0 Sunday, 1 Monday to 6 Saturday); they are converted to the
   * Quartz days when the cron is built
   */
  private List<String> weekDays;
  /**
   * Days of the month
   */
  private List<String> days;
  /**
   * Hours of the day
   */
  private List<String> hours;
  /**
   * Minutes of the hour
   */
  private List<String> minutes;
  /**
   * Seconds of the minute (only the «When» summary of the task editor tells them)
   */
  private List<String> seconds;
  /**
   * Date of a launch on a date
   */
  private LocalDate date;
  /**
   * Time of a launch on a date (HH:mm:ss)
   */
  private String time;
  /**
   * Name of the calendar of a scheduled task
   */
  private String calendar;
  /**
   * Name of the server a file task checks
   */
  private String server;
  /**
   * Folder a file task checks
   */
  private String path;
  /**
   * Pattern of the files a file task waits for
   */
  private String pattern;
  /**
   * Tasks after which the task runs, in their order
   */
  private List<Parent> parents = new ArrayList<>();

  /**
   * Task after which a task runs, and whether it launches it also when it ends with warning or with error
   *
   * @param name      Name of the parent task
   * @param onWarning The parent launches its dependencies when it ends with warning
   * @param onError   The parent launches its dependencies when it ends with error
   */
  public record Parent(String name, boolean onWarning, boolean onError) {
  }
}
