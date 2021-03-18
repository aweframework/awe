package com.almis.awe.test.bean;

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
  //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
  private Date dateOfShow;
  private String userGroupName;
  private String eventHallName;
  private String imageSource;
}
