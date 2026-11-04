package com.almis.awe.service.access;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.menu.Option;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * View as (S8): the menu a profile or a user would see in a module, as the real menu shows it (an allowed option is shown
 * only when all its parents are), with the number of visible options and the hidden ones with their reason. Nothing is
 * stored and no session is opened.
 */
class AccessMenuPreviewServiceTest extends AccessServiceFixture {

  @Test
  void theMenuOfASubjectHasOnlyTheOptionsItCanReach() throws AWException {
    // operator.xml denies settings: security, allowed by OPE, is not shown as its parent is not
    rules.add(new Object[]{null, OPE, "users", "R"});
    rules.add(new Object[]{null, OPE, "security", "A"});

    List<Map<String, CellData>> menu = previewService.getMenuPreview("profile", null, OPE, null).getDataList().getRows();

    assertThat(menu).extracting(row -> text(row, "preview-id"), row -> text(row, "preview-parent"),
        row -> text(row, "preview-is-leaf"))
      .containsExactly(tuple("tools", "", "false"), tuple("sites", "tools", "true"), tuple("reports", "", "false"),
        tuple("sales", "reports", "true"));
    JsonNode name = (JsonNode) menu.getFirst().get("preview-name").getObjectValue();
    assertThat(List.of(name.get("value").stringValue(), name.get("label").stringValue(), name.get("icon").stringValue()))
      .containsExactly("tools", "MENU_TOOLS", "icon-tools");

    Map<String, CellData> counts = single(previewService.getMenuPreviewCounts("profile", null, OPE, null));
    assertThat(List.of("visible-count", "total-count", "hidden-count")).extracting(column -> counts.get(column).getIntegerValue())
      .containsExactly(4, 8, 4);
    assertThat(List.of("title", "description")).extracting(column -> text(counts, column))
      .containsExactly("SCREEN_TEXT_ACCESS_PREVIEW_COUNT[4, 8]",
        "SCREEN_TEXT_ACCESS_PREVIEW_PARENT_HIDDEN[MENU_SECURITY, MENU_SETTINGS]");

    assertThat(previewService.getMenuPreviewHidden("profile", null, OPE, null).getDataList().getRows())
      .extracting(row -> text(row, "hidden-option-key"), row -> text(row, "hidden-option"), row -> text(row, "hidden-reason"))
      .containsExactly(
        tuple("users", "MENU_USERS", "SCREEN_TEXT_ACCESS_REASON_PROFILE[OPE]"),
        tuple("user-detail", "MENU_USER_DETAIL", "SCREEN_TEXT_ACCESS_REASON_INHERITED[MENU_USERS]"),
        tuple("settings", "MENU_SETTINGS", "SCREEN_TEXT_ACCESS_REASON_BASE[operator]"),
        tuple("security", "MENU_SECURITY", "SCREEN_TEXT_ACCESS_PREVIEW_HIDDEN_BY_PARENT[MENU_SETTINGS]"));
    verifyNoInteractions(menuRestrictionTreeService);
  }

  @Test
  void theOptionsOfAnotherModuleAreHidden() throws AWException {
    List<Map<String, CellData>> menu = previewService.getMenuPreview("user", JMARTIN, null, "Tools").getDataList().getRows();

    assertThat(menu).extracting(row -> text(row, "preview-id")).containsExactly("tools", "sites", "users", "user-detail");
    assertThat(previewService.getMenuPreviewHidden("user", JMARTIN, null, "Tools").getDataList().getRows())
      .extracting(row -> text(row, "hidden-option-key"), row -> text(row, "hidden-reason"))
      .containsExactly(
        tuple("settings", "SCREEN_TEXT_ACCESS_REASON_BASE[operator]"),
        tuple("security", "SCREEN_TEXT_ACCESS_REASON_INHERITED[MENU_SETTINGS]"),
        tuple("reports", "SCREEN_TEXT_ACCESS_REASON_MODULE[Reports]"),
        tuple("sales", "SCREEN_TEXT_ACCESS_REASON_MODULE[Reports]"));
    // Without a hidden option allowed by itself, the description says how the menu is built
    assertThat(text(single(previewService.getMenuPreviewCounts("user", JMARTIN, null, "Tools")), "description"))
      .isEqualTo("SCREEN_TEXT_ACCESS_PREVIEW_HIDDEN_TEXT");
  }

  @Test
  void sectionTitlesAreShownAndDividersAndInvisibleOptionsAreNot() throws AWException {
    // A section title, a divider line (a separator without label, drawn as a line by the menu), and an option that the
    // menu never shows (opened from a screen) with an option under it
    Option section = Option.builder().name("admin-section").label("MENU_SECTION").separator(true)
      .elementList(new ArrayList<>()).build();
    Option divider = Option.builder().name("divider").separator(true).elementList(new ArrayList<>()).build();
    Option hidden = Option.builder().name("detail").screen("detail").invisible(true)
      .elementList(new ArrayList<>(List.<Element>of(option("detail-child", null, "MENU_DETAIL_CHILD")))).build();
    when(menuProvider.getMenu("private")).thenAnswer(invocation -> menu(section,
      option("tools", null, "MENU_TOOLS", option("sites", null, "MENU_SITES"), hidden), divider));

    assertThat(previewService.getMenuPreview("profile", null, ADM, null).getDataList().getRows())
      .extracting(row -> text(row, "preview-id")).containsExactly("admin-section", "tools", "sites");
    Map<String, CellData> counts = single(previewService.getMenuPreviewCounts("profile", null, ADM, null));
    assertThat(List.of("visible-count", "total-count", "hidden-count")).extracting(column -> counts.get(column).getIntegerValue())
      .containsExactly(2, 2, 0);
    assertThat(previewService.getMenuPreviewHidden("profile", null, ADM, null).getDataList().getRows()).isEmpty();
  }

  @Test
  void theBannerNamesTheSubjectAndTheModule() throws AWException {
    assertThat(banner(previewService.getMenuPreviewBanner("user", JMARTIN, null, "Tools"))).containsExactly("true",
      "SCREEN_TEXT_ACCESS_PREVIEW_TITLE[jmartin] · SCREEN_TEXT_ACCESS_SUBJECT_PROFILE[OPE] · SCREEN_TEXT_ACCESS_PREVIEW_MODULE[Tools]",
      "SCREEN_TEXT_ACCESS_PREVIEW_TEXT[jmartin]", "lucide:eye", "info");
    assertThat(banner(previewService.getMenuPreviewBanner("profile", null, OPE, ""))).containsExactly("true",
      "SCREEN_TEXT_ACCESS_PREVIEW_TITLE[OPE] · SCREEN_TEXT_ACCESS_PREVIEW_ALL_MODULES",
      "SCREEN_TEXT_ACCESS_PREVIEW_TEXT[OPE]", "lucide:eye", "info");
    assertThat(text(single(previewService.getMenuPreviewBanner("profile", null, null, null)), "visible")).isEqualTo("false");
  }

  @Test
  void withoutSubjectThereIsNoMenu() throws AWException {
    assertThat(previewService.getMenuPreview("user", null, null, null).getDataList().getRows()).isEmpty();
    assertThat(previewService.getMenuPreviewCounts("user", null, null, null).getDataList().getRows()).isEmpty();
    assertThat(previewService.getMenuPreviewHidden("user", null, null, null).getDataList().getRows()).isEmpty();
  }

  private static Map<String, CellData> single(ServiceData data) {
    assertThat(data.getDataList().getRows()).hasSize(1);
    return data.getDataList().getRows().getFirst();
  }

  private static List<String> banner(ServiceData data) {
    Map<String, CellData> row = single(data);
    return List.of("visible", "title", "description", "icon", "iconTone").stream().map(column -> text(row, column)).toList();
  }
}
