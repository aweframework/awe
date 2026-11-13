package com.almis.awe.testing.database;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextCustomizer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AweDatabaseContextCustomizerFactoryTest {

  private final AweDatabaseContextCustomizerFactory factory = new AweDatabaseContextCustomizerFactory();

  @AweDatabaseTest(TestDatabase.H2)
  static class OnH2 {
  }

  @AweDatabaseTest(value = TestDatabase.H2, flyway = true)
  static class OnH2Flyway {
  }

  static class WithoutDatabase {
  }

  @AweDatabaseTest(TestDatabase.H2)
  static class OnH2Again {
  }

  static class InheritsTheDatabase extends OnH2 {
  }

  @Test
  void aClassWithoutTheAnnotationGetsNoCustomizer() {
    assertThat(factory.createContextCustomizer(WithoutDatabase.class, List.of())).isNull();
  }

  @Test
  void aSubclassInheritsTheDatabaseOfItsParent() {
    assertThat(factory.createContextCustomizer(InheritsTheDatabase.class, List.of())).isNotNull();
  }

  @Test
  void theCustomizerWritesTheDatabasePropertiesWithTopPrecedence() {
    ContextCustomizer customizer = factory.createContextCustomizer(OnH2.class, List.of());
    GenericApplicationContext context = new GenericApplicationContext();
    context.getEnvironment().getSystemProperties().put("spring.datasource.url", "jdbc:other");

    customizer.customizeContext(context, null);

    assertThat(context.getEnvironment().getProperty("spring.datasource.url")).isEqualTo(TestDatabase.H2.getEmbeddedUrl());
    assertThat(context.getEnvironment().getProperty("spring.sql.init.schema-locations")).isEqualTo("classpath:sql/schema-h2.sql");
    assertThat(context.getEnvironment().getProperty("spring.flyway.enabled")).isNull();
  }

  @Test
  void theFlywayAttributeSwitchesTheMigrationProperties() {
    ContextCustomizer customizer = factory.createContextCustomizer(OnH2Flyway.class, List.of());
    GenericApplicationContext context = new GenericApplicationContext();

    customizer.customizeContext(context, null);

    assertThat(context.getEnvironment().getProperty("spring.flyway.enabled")).isEqualTo("true");
    assertThat(context.getEnvironment().getProperty("awe.database.migration-modules")).isEqualTo("AWE,SCHEDULER,NOTIFIER,awe-boot");
  }

  @Test
  void equalConfigurationsShareTheCachedContext() {
    ContextCustomizer first = factory.createContextCustomizer(OnH2.class, List.of());
    ContextCustomizer same = factory.createContextCustomizer(OnH2Again.class, List.of());
    ContextCustomizer flyway = factory.createContextCustomizer(OnH2Flyway.class, List.of());

    assertThat(first).isEqualTo(same).hasSameHashCodeAs(same).isNotEqualTo(flyway);
  }
}
