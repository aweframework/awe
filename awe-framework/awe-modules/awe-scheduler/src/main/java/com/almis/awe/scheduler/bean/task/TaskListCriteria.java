package com.almis.awe.scheduler.bean.task;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TaskListCriteria {
  private Integer taskId;
  private Integer launchType;
  private Integer active;
}
