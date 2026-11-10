package com.almis.awe.service.screen;

import com.almis.awe.builder.screen.AdvancedBuilder;
import com.almis.awe.builder.screen.ScreenBuilder;
import com.almis.awe.builder.screen.TagBuilder;
import com.almis.awe.builder.screen.WindowBuilder;
import com.almis.awe.builder.screen.criteria.SelectCriteriaBuilder;
import com.almis.awe.builder.screen.criteria.SelectMultipleCriteriaBuilder;
import com.almis.awe.builder.screen.criteria.TextCriteriaBuilder;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.DefaultSearch;
import com.almis.awe.service.SavedSearchService;
import com.almis.awe.service.savedsearch.DefaultSavedSearch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Default saved search of each window with saved searches, applied before the initial load of the screen (#915, S2, S3,
 * S5, S6): it fills the criteria keep-criteria brought nothing for, the context variables keep winning, and the criteria
 * the window no longer has are ignored
 */
@DisplayName("Default searches of the windows of a screen")
class DefaultSearchesTest {

  private static final String VALUES = "{\"CrtNam\":[{\"value\":\"Ana\",\"label\":\"Ana\"}],"
    + "\"CrtCtr\":[{\"value\":\"FR\",\"label\":\"France\"},{\"value\":\"ES\",\"label\":\"Spain\"}],\"CrtSta\":[]}";

  private final JsonMapper mapper = new JsonMapper();
  private SavedSearchService savedSearchService;

  @BeforeEach
  void setUp() {
    savedSearchService = mock(SavedSearchService.class);
  }

  /**
   * Screen "Scr": the window "WinSch" with saved searches (name, bound to the context variable ctxNam, and status, then
   * the advanced countries and code) and the window "WinOff" with an advanced block and no saved searches
   */
  private static Screen screen() throws Exception {
    return new ScreenBuilder().setId("Scr").addTag(new TagBuilder().setSource("center").setType("div")
        .addWindow(new WindowBuilder().setId("WinSch").setSavedSearches(true)
          .addCriteria(new TextCriteriaBuilder().setId("CrtNam").setVariable("ctxNam"))
          .addCriteria(new SelectCriteriaBuilder().setId("CrtSta"))
          .addAdvanced(new AdvancedBuilder().setId("AdvSch")
            .addCriteria(new SelectMultipleCriteriaBuilder().setId("CrtCtr"))
            .addCriteria(new TextCriteriaBuilder().setId("CrtCod"))))
        .addWindow(new WindowBuilder().setId("WinOff")
          .addCriteria(new TextCriteriaBuilder().setId("CrtOff"))
          .addAdvanced(new AdvancedBuilder().setId("AdvOff").addCriteria(new TextCriteriaBuilder().setId("CrtOffAdv")))))
      .build();
  }

  private void mockDefault(String values) {
    when(savedSearchService.getDefaultWindowSearch("Scr", "WinSch")).thenReturn(Optional.of(new DefaultSavedSearch(7, "Mine", values)));
  }

  private static ObjectNode empty() {
    return JsonNodeFactory.instance.objectNode();
  }

  private DefaultSearches.Resolution resolve(ObjectNode stored, ObjectNode request) throws Exception {
    return DefaultSearches.resolve(screen(), stored, request, savedSearchService, ".data");
  }

  @Test
  @DisplayName("Without values from keep-criteria the default search fills the criteria as the client sends them, on a copy")
  void appliedWhenKeepCriteriaBroughtNothing() throws Exception {
    mockDefault(VALUES.substring(0, VALUES.length() - 1) + ",\"$layout\":[\"CrtCtr\"]}");
    ObjectNode stored = empty();

    DefaultSearches.Resolution resolution = resolve(stored, empty());

    assertThat(resolution.parameters().toString()).isEqualTo("{\"CrtNam\":\"Ana\",\"CrtSta\":\"\",\"CrtCtr\":[\"FR\",\"ES\"]}");
    assertThat(resolution.criteria()).isEqualTo(Set.of("CrtNam", "CrtSta", "CrtCtr"));
    DefaultSearch search = resolution.searches().get("WinSch");
    assertThat(search.id()).isEqualTo(7);
    assertThat(search.name()).isEqualTo("Mine");
    assertThat(search.values()).isEqualTo(mapper.readTree(VALUES));
    assertThat(search.layout()).containsExactly("CrtCtr");
    assertThat(resolution.searches()).containsOnlyKeys("WinSch");
    assertThat(stored.isEmpty()).as("The stored parameters of the session are not changed").isTrue();
  }

  @Test
  @DisplayName("A search saved without a layout applies its values with an empty layout")
  void searchWithoutLayout() throws Exception {
    mockDefault(VALUES);

    DefaultSearches.Resolution resolution = resolve(empty(), empty());

    assertThat(resolution.searches().get("WinSch").layout()).isEmpty();
    assertThat(resolution.parameters().get("CrtNam").asString()).isEqualTo("Ana");
  }

  @Test
  @DisplayName("When keep-criteria stored the criteria of the window, the last search used wins and no default is read")
  void notAppliedWhenKeepCriteriaHasTheWindow() throws Exception {
    mockDefault(VALUES);
    ObjectNode stored = empty().put("CrtNam", "");

    DefaultSearches.Resolution resolution = resolve(stored, empty());

    assertThat(resolution.parameters()).isSameAs(stored);
    assertThat(resolution.parameters().toString()).isEqualTo("{\"CrtNam\":\"\"}");
    assertThat(resolution.searches()).isEmpty();
    assertThat(resolution.criteria()).isEmpty();
    verify(savedSearchService, never()).getDefaultWindowSearch(any(), any());
  }

  @Test
  @DisplayName("A criterion whose context variable comes in the request keeps it: the default search does not fill it")
  void contextVariableWins() throws Exception {
    mockDefault(VALUES);

    DefaultSearches.Resolution resolution = resolve(empty(), empty().put("ctxNam", "Bob"));

    assertThat(resolution.parameters().has("CrtNam")).isFalse();
    assertThat(resolution.criteria()).isEqualTo(Set.of("CrtSta", "CrtCtr"));
    assertThat(resolution.searches().get("WinSch").values().has("CrtNam")).as("The search keeps it as its Reset target").isTrue();
  }

  @Test
  @DisplayName("Criteria the window no longer has are ignored in the values and in the layout, without an error")
  void removedCriteriaAreIgnored() throws Exception {
    mockDefault("{\"Gone\":[{\"value\":1}],\"CrtNam\":[{\"value\":\"Ana\"}],\"$layout\":[\"Gone\",\"CrtNam\",\"CrtCtr\",\"CrtCtr\",3]}");

    DefaultSearches.Resolution resolution = resolve(empty(), empty());

    assertThat(resolution.parameters().toString()).isEqualTo("{\"CrtNam\":\"Ana\"}");
    DefaultSearch search = resolution.searches().get("WinSch");
    assertThat(search.values().toString()).isEqualTo("{\"CrtNam\":[{\"value\":\"Ana\"}]}");
    assertThat(search.layout()).isEqualTo(List.of("CrtCtr"));
  }

  @Test
  @DisplayName("A window without saved searches is left as today: its default search is never read")
  void windowWithoutSavedSearchesIsUntouched() throws Exception {
    ObjectNode stored = empty();

    DefaultSearches.Resolution resolution = resolve(stored, empty());

    assertThat(resolution.parameters()).isSameAs(stored);
    assertThat(resolution.searches()).isEmpty();
    verify(savedSearchService, never()).getDefaultWindowSearch("Scr", "WinOff");
    assertThat(DefaultSearches.resolve(screen(), stored, empty(), null, ".data").searches()).isEmpty();
  }

  @Test
  @DisplayName("A default search that cannot be read or applied leaves the screen as it would be without it")
  void errorsLeaveTheScreenWithoutDefault() throws Exception {
    ObjectNode stored = empty();
    for (String values : List.of("not json", "[]", "{\"CrtNam\":\"Ana\"}")) {
      mockDefault(values);
      DefaultSearches.Resolution resolution = resolve(stored, empty());
      assertThat(resolution.parameters()).as(values).isSameAs(stored);
      assertThat(resolution.searches()).as(values).isEmpty();
    }

    when(savedSearchService.getDefaultWindowSearch("Scr", "WinSch")).thenThrow(new IllegalStateException("down"));
    assertThat(resolve(stored, empty()).searches()).isEmpty();
    assertThat(stored.isEmpty()).isTrue();
  }
}
