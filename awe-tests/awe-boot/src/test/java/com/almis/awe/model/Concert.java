package com.almis.awe.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class Concert {
  private String eventDateName;
  private String name;
  @DateTimeFormat(pattern = "dd/MM/yyyy")
  private Date dateOfShow;
  private String userGroupName;
  private String eventHallName;
  private String imageSource;
}
