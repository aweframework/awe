package com.almis.awe.scheduler.builder.task;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.calendar.Calendar;
import com.almis.awe.scheduler.bean.file.File;
import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskDependency;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.enums.JobType;
import com.almis.awe.scheduler.enums.TriggerType;
import com.almis.awe.scheduler.factory.JobFactory;
import com.almis.awe.scheduler.factory.TriggerFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;

import java.util.List;

import static com.almis.awe.scheduler.constant.JobConstants.TASK;

@Getter
@Setter
@Log4j2
@Accessors(chain = true)
public abstract class TaskBuilder {

  private Integer index;
  private String site;
  private Task task;
  private Scheduler scheduler;

  /**
   * Set configuration data
   *
   * @param task Task
   * @return Task builder
   */
  public TaskBuilder setData(Task task) {
    this.task = task;

    // Retrieve task builder
    return this;
  }

  /**
   * Build the task
   *
   * @return Task built
   */
  public Task build() throws AWException {
    // Generate trigger
    generateTrigger();

    // Generate job
    generateJob();

    // Retrieve task
    return getTask();
  }

  /**
   * Fill parameters from datalist
   *
   * @param taskParameterList Task parameter list
   * @return Task builder
   */
  public TaskBuilder setParameters(List<TaskParameter> taskParameterList) {
    task.setParameterList(taskParameterList);
    return this;
  }

  /**
   * Fill dependencies from datalist
   *
   * @param taskDependencyList Task dependency list
   * @return Task builder
   */
  public TaskBuilder setDependencies(List<TaskDependency> taskDependencyList) {
    task.setDependencyList(taskDependencyList);
    return this;
  }

  /**
   * Retrieve calendar id
   *
   * @return Calendar ID
   */
  public Integer getCalendarId() {
    return task.getCalendarId();
  }

  /**
   * Retrieve file
   *
   * @return File
   */
  public File getFile() {
    return task.getFile();
  }

  /**
   * Set calendar to task
   *
   * @param calendar Calendar
   * @return Task builder
   */
  public TaskBuilder setCalendar(Calendar calendar) {
    task.setCalendar(calendar);
    return this;
  }

  /**
   * Set server to file
   *
   * @param server File server
   * @return Task builder
   */
  public TaskBuilder setFileServer(Server server) {
    task.getFile().setServer(server);
    return this;
  }

  /**
   * Generate scheduled trigger
   *
   * @throws AWException Error generating trigger for task
   */
  private void generateTrigger() throws AWException {
    // Create trigger object
    task.setTrigger(TriggerFactory.getInstance(TriggerType.TASK, defineJobData()));
    log.debug("[SCHEDULER][TASK_QUERY {}][TRIGGER] Trigger generated", getTask().getTrigger().getKey().toString());
  }

  /**
   * Creates a new Job and retrieves it. It also saves the Job into the task
   *
   * @throws AWException Error generating job for task
   */
  private void generateJob() throws AWException {
    // Create and set the job to the Task
    task.setJob(JobFactory.getInstance(JobType.valueOf(task.getExecutionType()), defineJobData()));
    log.debug("[SCHEDULER][TASK_QUERY][JOB {}] Job generated", task.getJob() != null ? task.getJob().getKey().toString() : "");
  }

  /**
   * Define job datamap
   *
   * @return job datamap
   */
  private JobDataMap defineJobData() {
    // Define job data
    JobDataMap data = new JobDataMap();
    data.put("id", task.getTaskId().toString());
    data.put(TASK, task);

    return data;
  }
}
