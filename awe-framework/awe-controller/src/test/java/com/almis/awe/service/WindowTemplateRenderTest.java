package com.almis.awe.service;

import org.junit.jupiter.api.Test;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders the real window template of the AngularJS client (the file is added to the test classpath by the pom)
 */
class WindowTemplateRenderTest {

  private String render(String header) {
    URL url = getClass().getClassLoader().getResource("angular-templates/templates.stg");
    assertThat(url).as("the AngularJS screen templates on the test classpath").isNotNull();
    STGroup group = new STGroupFile(url, "UTF-8", '$', '$');
    ST window = group.getInstanceOf("window");
    window.add("breadcrumbs", "<li>BREADCRUMB_ITEM</li>");
    window.add("levels", "[LEVELS_JSON]");
    window.add("option", "OPTION_NAME");
    window.add("title", "SCREEN_TITLE");
    window.add("buttons", "<button>BUTTON</button>");
    window.add("center", "<p>CENTER</p>");
    window.add("modal", "");
    window.add("hidden", "");
    window.add("header", header);
    return window.render();
  }

  private void assertBalanced(String html) {
    assertThat(html.split("<div[\\s>]", -1).length - 1).isEqualTo(html.split("</div>", -1).length - 1);
  }

  @Test
  void breadcrumbModeShowsTheBreadcrumbTitleAndButtons() {
    String html = render("breadcrumb");

    assertThat(html).contains("<awe-breadcrumb levels=\"[LEVELS_JSON]\" page-option=\"OPTION_NAME\" page-title=\"SCREEN_TITLE\">").contains("lucide-sprite.svg#house")
      .contains("<button>BUTTON</button>").doesNotContain("window-header-none");
    assertBalanced(html);
  }

  @Test
  void noneModeShowsOnlyTheButtonsRowAboveTheContent() {
    String html = render("none");

    assertThat(html).contains("<button>BUTTON</button>").contains("<p>CENTER</p>")
      .doesNotContain("awe-breadcrumb").doesNotContain("LEVELS_JSON").doesNotContain("SCREEN_TITLE")
      .doesNotContain("<h1");
    assertThat(html.indexOf("<button>BUTTON</button>")).isLessThan(html.indexOf("<p>CENTER</p>"));
    assertBalanced(html);
  }
}
