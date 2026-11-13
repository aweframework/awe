package com.almis.awe.testing.config;

import com.almis.awe.testing.model.types.BrowserTool;
import com.almis.awe.testing.model.types.EvidenceMode;
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
        .hasMessageContaining("Supported tools: selenium, playwright");
    });
  }

  @Test
  void thePlaywrightTraceAndVideoAreKeptOnFailureUnlessTheyAreConfigured() {
    assertThat(new AweTestConfigProperties().getPlaywright().getTrace()).isEqualTo(EvidenceMode.ON_FAILURE);
    assertThat(new AweTestConfigProperties().getPlaywright().getVideo()).isEqualTo(EvidenceMode.ON_FAILURE);
    runner.withPropertyValues("awe.test.playwright.trace=off", "awe.test.playwright.video=always").run(context -> {
      assertThat(context.getBean(AweTestConfigProperties.class).getPlaywright().getTrace()).isEqualTo(EvidenceMode.OFF);
      assertThat(context.getBean(AweTestConfigProperties.class).getPlaywright().getVideo()).isEqualTo(EvidenceMode.ALWAYS);
    });
    assertThat(new AweTestConfigProperties().getPlaywright().isTraceSnapshots()).isFalse();
    runner.withPropertyValues("awe.test.playwright.trace-snapshots=true").run(context ->
      assertThat(context.getBean(AweTestConfigProperties.class).getPlaywright().isTraceSnapshots()).isTrue());
    runner.withPropertyValues("awe.test.playwright.trace=on-failure").run(context ->
      assertThat(context.getBean(AweTestConfigProperties.class).getPlaywright().getTrace()).isEqualTo(EvidenceMode.ON_FAILURE));
  }

  @Test
  void thePlaywrightSandboxIsLeftToTheAutomaticRuleUnlessItIsConfigured() {
    assertThat(new AweTestConfigProperties().getPlaywright().getNoSandbox()).isNull();
    runner.run(context -> assertThat(context.getBean(AweTestConfigProperties.class).getPlaywright().getNoSandbox()).isNull());
    runner.withPropertyValues("awe.test.playwright.no-sandbox=false")
      .run(context -> assertThat(context.getBean(AweTestConfigProperties.class).getPlaywright().getNoSandbox()).isFalse());
    runner.withPropertyValues("awe.test.playwright.no-sandbox=true")
      .run(context -> assertThat(context.getBean(AweTestConfigProperties.class).getPlaywright().getNoSandbox()).isTrue());
  }

  @Test
  void aBlankToolIsNotAcceptedEither() {
    assertThatThrownBy(() -> BrowserTool.fromName("  ")).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Supported tools: selenium, playwright");
    assertThatThrownBy(() -> BrowserTool.fromName(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aToolNameIgnoresCaseAndSurroundingBlanks() {
    assertThat(BrowserTool.fromName(" SeLeNiUm ")).isEqualTo(BrowserTool.SELENIUM);
    assertThat(BrowserTool.fromName(" Playwright ")).isEqualTo(BrowserTool.PLAYWRIGHT);
  }
}
