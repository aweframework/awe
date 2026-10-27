package com.almis.awe.dao;

import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.service.MenuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.stringtemplate.v4.STGroup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Icon of the options in the application help (#885, S15): Lucide for a mapped name, its current font otherwise
 */
class TemplateDaoTest {

  private static final String SVG_START = "<svg class=\"awe-icon\" aria-hidden=\"true\" focusable=\"false\">"
    + "<use href=\"icons/lucide-sprite.svg#";

  private TemplateDao templateDao;

  @BeforeEach
  void setUp(@TempDir Path folder) throws IOException {
    // Loaded as the starter loads the help templates (TemplateConfig)
    Path helpFile = Files.writeString(folder.resolve("help.stg"),
      "helpOption(e,title,level,content,icon) ::= \"$icon$\"\n");
    STGroup helpGroup = new STGroup('$', '$');
    helpGroup.loadGroupFile("", helpFile.toUri().toURL().toExternalForm());
    templateDao = new TemplateDao(mock(MenuService.class), mock(AweElementsDao.class), helpGroup);
  }

  private String optionIcon(String icon) {
    Option option = new Option().setIcon(icon);
    option.setName("option");
    return templateDao.generateOptionHelp(new Menu(), option, 1, false).render();
  }

  @Test
  void rendersTheLucideIconOfAMappedName() {
    assertThat(optionIcon("sitemap")).isEqualTo("<i role=\"icon\" class=\"help-icon text-primary\">"
      + SVG_START + "network\"></use></svg></i>");
    assertThat(optionIcon("mdi:arrow_back")).isEqualTo("<i role=\"icon\" class=\"help-icon text-primary\">"
      + SVG_START + "arrow-left\"></use></svg></i>");
    assertThat(optionIcon("lucide:house")).isEqualTo("<i role=\"icon\" class=\"help-icon text-primary\">"
      + SVG_START + "house\"></use></svg></i>");
  }

  @Test
  void mapsTheFontAwesomeModifiersOfALucideIcon() {
    assertThat(optionIcon("refresh fa-spin fa-fw text-info")).isEqualTo(
      "<i role=\"icon\" class=\"help-icon text-primary\">"
        + "<svg class=\"awe-icon awe-icon-spin text-info\" aria-hidden=\"true\" focusable=\"false\">"
        + "<use href=\"icons/lucide-sprite.svg#refresh-cw\"></use></svg></i>");
  }

  @Test
  void keepsTheCurrentFontForNamesWithoutEquivalence() {
    assertThat(optionIcon("youtube-play"))
      .isEqualTo("<i role=\"icon\" class=\"help-icon text-primary fa fa-youtube-play fa-fw\"></i>");
    assertThat(optionIcon("mdi:not_mapped"))
      .isEqualTo("<i role=\"icon\" class=\"help-icon text-primary material-icons\">not_mapped</i>");
  }

  @Test
  void rendersNothingWithoutIconOrForAnUnknownLucideName() {
    assertThat(optionIcon(null)).isEmpty();
    assertThat(optionIcon("lucide:not-a-lucide-icon")).isEmpty();
  }
}
