package com.almis.awe.samples.vetclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Veterinary clinic: AWE sample application (React engine). Everything it shows is declared in XML under
 * src/main/resources/application/vetclinic, so this class only starts Spring Boot.
 */
@SpringBootApplication
public class VetClinicApplication {

  /**
   * Run the application
   *
   * @param args Application arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(VetClinicApplication.class, args);
  }
}
