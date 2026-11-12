package com.almis.awe.samples.helpdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Help desk: AWE sample application (React engine). Everything it shows is declared in XML under
 * src/main/resources/application/helpdesk, so this class only starts Spring Boot.
 */
@SpringBootApplication
public class HelpDeskApplication {

  /**
   * Run the application
   *
   * @param args Application arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(HelpDeskApplication.class, args);
  }
}
