package com.almis.awe.scheduler.descriptor;

import com.almis.awe.scheduler.enums.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Stop button of the task lists is shown for the selected task while it is running: its dependency compares the
 * status column with the value the task list gives to a running task.
 */
@DisplayName("Stop button status")
class StopButtonStatusTest {

  @ParameterizedTest
  @ValueSource(strings = {"scheduler-tasks", "scheduler-dashboard"})
  void stopButtonIsShownForTheRunningStatus(String screen) throws Exception {
    Document document;
    try (InputStream stream = getClass().getClassLoader()
        .getResourceAsStream("application/awe-scheduler/screen/" + screen + ".xml")) {
      assertThat(stream).as(screen).isNotNull();
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      document = factory.newDocumentBuilder().parse(stream);
    }

    List<String> statusValues = new ArrayList<>();
    NodeList buttons = document.getElementsByTagName("button");
    for (int i = 0; i < buttons.getLength(); i++) {
      Element button = (Element) buttons.item(i);
      if ("ButStp".equals(button.getAttribute("id"))) {
        NodeList elements = button.getElementsByTagName("dependency-element");
        for (int j = 0; j < elements.getLength(); j++) {
          Element element = (Element) elements.item(j);
          if ("Sta".equals(element.getAttribute("column"))) {
            statusValues.add(element.getAttribute("value"));
          }
        }
      }
    }

    assertThat(statusValues).containsExactly(String.valueOf(TaskStatus.JOB_RUNNING.getValue()));
  }
}
