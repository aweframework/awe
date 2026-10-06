package com.almis.awe.testing.config;

import com.almis.awe.testing.model.types.BrowserTool;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AweTestConfigPropertiesTest {

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
    .withUserConfiguration(TestConfig.class)
    .withPropertyValues("awe.test.start-url=http://localhost:8080");

  @Test
  void theToolIsSeleniumByDefault() {
    assertThat(new AweTestConfigProperties().getTool()).isEqualTo(BrowserTool.SELENIUM);
    runner.run(context -> assertThat(context.getBean(AweTestConfigProperties.class).getTool()).isEqualTo(BrowserTool.SELENIUM));
  }

  @Test
  void theToolIsReadFromTheConfigurationIgnoringTheCase() {
    runner.withPropertyValues("awe.test.tool=Selenium")
      .run(context -> assertThat(context.getBean(AweTestConfigProperties.class).getTool()).isEqualTo(BrowserTool.SELENIUM));
  }

  @Test
  void anUnknownToolFailsAtStartupAndListsTheSupportedOnes() {
    runner.withPropertyValues("awe.test.tool=cypress").run(context -> {
      assertThat(context).hasFailed();
      Throwable root = context.getStartupFailure();
      while (root.getCause() != null) {
        root = root.getCause();
      }
      assertThat(root).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("'cypress'")
        .hasMessageContaining("awe.test.tool")
        .hasMessageContaining("Supported tools: selenium");
    });
  }

  @Test
  void aBlankToolIsNotAcceptedEither() {
    assertThatThrownBy(() -> BrowserTool.fromName("  ")).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Supported tools: selenium");
    assertThatThrownBy(() -> BrowserTool.fromName(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aToolNameIgnoresCaseAndSurroundingBlanks() {
    assertThat(BrowserTool.fromName(" SeLeNiUm ")).isEqualTo(BrowserTool.SELENIUM);
  }
}
