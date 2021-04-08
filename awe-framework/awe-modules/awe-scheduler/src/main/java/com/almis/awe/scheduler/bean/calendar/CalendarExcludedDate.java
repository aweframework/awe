package com.almis.awe.scheduler.bean.calendar;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class CalendarExcludedDate {
  private Integer id;
  private Integer calendarId;
  private Date date;
  private String description;
}
