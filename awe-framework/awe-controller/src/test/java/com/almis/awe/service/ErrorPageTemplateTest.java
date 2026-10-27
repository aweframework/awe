package com.almis.awe.service;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders the error page template of the generic screens the way {@link ErrorPageService} does: with a plain
 * Thymeleaf context, which has no request to resolve context-relative links (#885)
 */
class ErrorPageTemplateTest {

  private static final String TEMPLATES = "../awe-generic-screens/src/main/resources/templates/";

  private String render() {
    FileTemplateResolver resolver = new FileTemplateResolver();
    resolver.setPrefix(TEMPLATES);
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);

    Context context = new Context();
    context.setVariable("icon", "!");
    context.setVariable("title", "Error title");
    context.setVariable("message", "Error message");
    context.setVariable("messagePrefix", "Error:");
    context.setVariable("backButtonText", "Back");
    context.setVariable("homeButtonText", "Home");
    context.setVariable("homeUrl", "/");
    return engine.process(ErrorPageService.TEMPLATE_PAGE_ERROR, context);
  }

  @Test
  void rendersWithoutARequestAndLoadsTheTokens() {
    String html = render();

    assertThat(html)
      .contains("<link rel=\"stylesheet\" href=\"/css/awe-design-tokens/tokens.css\">")
      .contains("Error title")
      .contains("Error message")
      .doesNotContain("layout-dark");
  }
}
