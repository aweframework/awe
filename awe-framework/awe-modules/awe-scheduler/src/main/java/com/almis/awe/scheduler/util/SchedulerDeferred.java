package com.almis.awe.scheduler.util;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;

import java.util.HashMap;
import java.util.Map;

/**
 * Answer of a task target (a maintain) that means accepted, finishes later: the scheduler keeps the execution
 * running and does not launch its dependencies until the execution is completed (see
 * {@code SchedulerExecutionCompletionService}).
 *
 * <p>The marker is a single row in the data list of the answer, not an {@code AnswerType}: it is the field of
 * {@link ServiceData} that the remote maintain callback transports to the scheduler instance and that a maintain
 * leaves untouched. It has to be the answer of the last step of the maintain.
 */
public final class SchedulerDeferred {

  public static final int MAX_REFERENCE_LENGTH = 256;
  public static final int MAX_DESCRIPTION_LENGTH = 2000;
  private static final String DEFERRED = "schedulerDeferred";
  private static final String REFERENCE = "schedulerDeferredReference";
  private static final String DESCRIPTION = "schedulerDeferredDescription";

  private SchedulerDeferred() {
  }

  /**
   * Build the deferred answer
   *
   * @param externalReference Opaque reference of the work in the external system (optional, 256 characters at most)
   * @param description       Description of the execution while it waits (optional, 2000 characters at most)
   * @return Deferred answer
   */
  public static ServiceData accepted(String externalReference, String description) {
    Map<String, CellData> row = new HashMap<>();
    row.put(DEFERRED, new CellData("true"));
    if (externalReference != null) {
      row.put(REFERENCE, new CellData(externalReference));
    }
    if (description != null) {
      row.put(DESCRIPTION, new CellData(description));
    }
    DataList dataList = new DataList();
    dataList.addRow(row);
    return new ServiceData().setDataList(dataList).setMessage(description == null ? "" : description);
  }

  /**
   * Check if an answer is a deferred one
   *
   * @param answer Answer of the target
   * @return True when the target accepted the work and will finish later
   */
  public static boolean isDeferred(ServiceData answer) {
    return "true".equals(text(answer, DEFERRED));
  }

  /**
   * Get the external reference of a deferred answer
   *
   * @param answer Answer of the target
   * @return Reference (256 characters at most), or null
   */
  public static String externalReference(ServiceData answer) {
    return truncate(text(answer, REFERENCE), MAX_REFERENCE_LENGTH);
  }

  /**
   * Get the description of a deferred answer
   *
   * @param answer Answer of the target
   * @return Description (2000 characters at most), or null
   */
  public static String description(ServiceData answer) {
    return truncate(text(answer, DESCRIPTION), MAX_DESCRIPTION_LENGTH);
  }

  private static String text(ServiceData answer, String column) {
    if (answer == null || answer.getDataList() == null || answer.getDataList().getRows().isEmpty()) {
      return null;
    }
    CellData cell = answer.getDataList().getRows().get(0).get(column);
    return cell == null ? null : cell.getStringValue();
  }

  private static String truncate(String value, int length) {
    return value != null && value.length() > length ? value.substring(0, length) : value;
  }
}
