package com.almis.awe.scheduler.factory;

import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.builder.task.TaskBuilder;
import com.almis.awe.scheduler.enums.TaskLaunchType;
import com.almis.awe.scheduler.enums.TriggerType;
import com.almis.awe.scheduler.util.TaskUtil;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.Trigger;

import static com.almis.awe.scheduler.constant.JobConstants.TASK;
import static com.almis.awe.scheduler.constant.TaskConstants.MANUAL_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.WEBHOOK_GROUP;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The WEBHOOK launch type: a task without trigger of its own, launched by an immediate trigger in its own group
 */
class WebhookLaunchTypeTest {

  @Test
  void theLaunchTypeHasItsValueAndItsGroup() {
    assertThat(TaskLaunchType.valueOf(4)).isEqualTo(TaskLaunchType.WEBHOOK);
    assertThat(TaskUtil.getGroupForLaunchType(4)).isEqualTo(WEBHOOK_GROUP);
    assertThat(TaskUtil.getLaunchTypeForGroup(WEBHOOK_GROUP)).isEqualTo(TaskLaunchType.WEBHOOK);
    assertThat(TaskUtil.getGroupForLaunchType(0)).isEqualTo(MANUAL_GROUP);
  }

  @Test
  void onlyTheManualAndWebhookTasksAreLaunchedOnDemand() {
    assertThat(TaskUtil.isLaunchedOnDemand(TaskLaunchType.MANUAL.getValue())).isTrue();
    assertThat(TaskUtil.isLaunchedOnDemand(TaskLaunchType.WEBHOOK.getValue())).isTrue();
    assertThat(TaskUtil.isLaunchedOnDemand(TaskLaunchType.SCHEDULED.getValue())).isFalse();
    assertThat(TaskUtil.isLaunchedOnDemand(TaskLaunchType.FILE_TRACKING.getValue())).isFalse();
  }

  @Test
  void theFactoryBuildsAWebhookTaskInItsGroup() throws Exception {
    Task task = new Task().setTaskId(7).setExecutionType(1).setLaunchType(4)
      .setSchedule(new Schedule().setRepeatType(1).setRepeatNumber(1));

    TaskBuilder builder = TaskFactory.getInstance(task, null, null);

    assertThat(builder.getTask().getGroup()).isEqualTo(WEBHOOK_GROUP);
    assertThat(builder.build().getTrigger().getKey().getGroup()).isEqualTo(WEBHOOK_GROUP);
  }

  @Test
  void theImmediateTriggerOfAWebhookLaunchIsInTheWebhookGroup() throws Exception {
    Task task = new Task().setTaskId(7).setLaunchType(4);
    JobDataMap dataMap = new JobDataMap();
    dataMap.put(TASK, task);

    Trigger trigger = TriggerFactory.getInstance(TriggerType.WEBHOOK, dataMap);

    assertThat(trigger.getKey().getGroup()).isEqualTo(WEBHOOK_GROUP);
    assertThat(trigger.getKey().getName()).startsWith("7-");
    assertThat(task.getGroup()).isEqualTo(WEBHOOK_GROUP);
    assertThat(task.getLaunchType()).isEqualTo(TaskLaunchType.WEBHOOK.getValue());
  }
}
