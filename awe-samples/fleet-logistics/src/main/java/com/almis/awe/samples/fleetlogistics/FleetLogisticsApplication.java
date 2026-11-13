package com.almis.awe.samples.fleetlogistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Fleet and logistics: AWE sample application (React engine). Everything it shows is declared in XML under
 * src/main/resources/application/fleet, so this class only starts Spring Boot.
 */
@SpringBootApplication
public class FleetLogisticsApplication {

  /**
   * Run the application
   *
   * @param args Application arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(FleetLogisticsApplication.class, args);
  }
}
