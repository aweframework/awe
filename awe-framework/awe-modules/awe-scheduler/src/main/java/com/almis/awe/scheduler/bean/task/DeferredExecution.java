package com.almis.awe.scheduler.bean.task;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * State of an execution that was accepted and finishes later (one row of AweSchExeExt)
 */
@Data
@Accessors(chain = true)
public class DeferredExecution implements Serializable {
  private Integer taskId;
  private Integer executionId;
  private String externalReference;
  private Date acceptedDate;
  private Date dueDate;
  private Date completionDate;
  private String completionSource;
  private String cancellationStatus;
  private String cancellationError;
}
