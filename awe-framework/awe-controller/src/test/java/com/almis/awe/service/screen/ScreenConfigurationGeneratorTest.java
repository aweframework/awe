package com.almis.awe.service.screen;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.criteria.Criteria;
import com.almis.awe.service.MenuService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ScreenConfigurationGeneratorTest {

  @InjectMocks
  private ScreenConfigurationGenerator screenConfigurationGenerator;

  @Mock
  ApplicationContext applicationContext;

  @Mock
  AweElements aweElements;

  @Mock
  AweRequest request;

  @Mock
  MenuService menuService;

  @Mock
  QueryService queryService;

  @BeforeEach
  void setUp() {
    screenConfigurationGenerator.setApplicationContext(applicationContext);
  }

  @AfterEach
  void clearInterruptFlag() {
    Thread.interrupted();
  }

  @Test
  void givenInterruptedFuture_applyScreenConfiguration() {
    // Given
    CompletableFuture<ServiceData> configurationTask = CompletableFuture.failedFuture(new InterruptedException());
    Screen screen = new Screen();
    // When
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    // Then
    screenConfigurationGenerator.applyScreenConfiguration(configurationTask, screen);
    verify(aweElements, times(1)).getLocaleWithLanguage(anyString(), eq(null));
  }

  @Test
  void givenFailedFuture_applyScreenConfigurationKeepsThreadUninterrupted() {
    CompletableFuture<ServiceData> configurationTask = CompletableFuture.failedFuture(new IllegalStateException("Connection refused"));
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);

    screenConfigurationGenerator.applyScreenConfiguration(configurationTask, new Screen());

    assertThat(Thread.currentThread().isInterrupted()).isFalse();
  }

  @Test
  void givenInterruptedWait_applyScreenConfigurationRestoresTheInterruptFlag() throws Exception {
    @SuppressWarnings("unchecked")
    Future<ServiceData> interrupted = mock(Future.class);
    when(interrupted.get()).thenThrow(new InterruptedException("interrupted"));
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);

    screenConfigurationGenerator.applyScreenConfiguration(interrupted, new Screen());

    assertThat(Thread.currentThread().isInterrupted()).isTrue();
  }

  private static Map<String, CellData> rule(String user, String profile, String screen, String value) {
    Map<String, CellData> row = new HashMap<>();
    row.put("user", new CellData(user));
    row.put("profile", new CellData(profile));
    row.put("component", new CellData("Crt"));
    row.put("screen", new CellData(screen));
    row.put("attribute", new CellData("placeholder"));
    row.put("value", new CellData(value));
    return row;
  }

  private String applyInOrder(List<Map<String, CellData>> rules) {
    Criteria criteria = new Criteria();
    criteria.setId("Crt");
    Screen screen = new Screen();
    screen.addElement(criteria);
    DataList dataList = new DataList();
    rules.forEach(dataList::addRow);
    ServiceData data = new ServiceData().addVariable(AweConstants.ACTION_DATA, new CellData(dataList));

    screenConfigurationGenerator.applyScreenConfiguration(CompletableFuture.completedFuture(data), screen);
    return criteria.getPlaceholder();
  }

  @Test
  void givenConflictingRulesInAnyOrder_theMostSpecificScopeWins() {
    List<Map<String, CellData>> rules = List.of(rule(null, null, null, "everyone"), rule(null, "ADM", null, "profile"), rule("pablo", null, null, "user"));
    List<List<Map<String, CellData>>> permutations = new ArrayList<>();
    for (int a = 0; a < 3; a++) {
      for (int b = 0; b < 3; b++) {
        for (int c = 0; c < 3; c++) {
          if (a != b && b != c && a != c) {
            permutations.add(List.of(rules.get(a), rules.get(b), rules.get(c)));
          }
        }
      }
    }

    assertThat(permutations).hasSize(6).allSatisfy(order -> assertThat(applyInOrder(order)).isEqualTo("user"));
  }

  @Test
  void givenRulesOfTheSameScope_theScreenSpecificOneWinsOverTheAllScreensOne() {
    List<Map<String, CellData>> rules = new ArrayList<>(List.of(rule(null, "ADM", "Scr", "screen"), rule(null, "ADM", null, "all")));

    assertThat(applyInOrder(rules)).isEqualTo("screen");
    Collections.reverse(rules);
    assertThat(applyInOrder(rules)).isEqualTo("screen");
  }

  @Test
  void givenARuleWithAnUnknownAttribute_itIsSkippedAndTheOtherRulesStillApply() {
    Map<String, CellData> unknown = rule(null, null, null, "ignored");
    unknown.put("attribute", new CellData("index"));

    assertThat(applyInOrder(List.of(unknown, rule(null, null, null, "applied")))).isEqualTo("applied");
  }

  private static Map<String, CellData> scopedRule(Integer userId, Integer profileId, String value) {
    Map<String, CellData> row = rule(userId == null ? null : "user" + userId, profileId == null ? null : "PRO" + profileId, null, value);
    row.put("userId", new CellData(userId == null ? "" : String.valueOf(userId)));
    row.put("profileId", new CellData(profileId == null ? "" : String.valueOf(profileId)));
    return row;
  }

  private List<Map<String, CellData>> scopedRules() {
    // everyone, profile 1, profile 2 and user 7 (profile 1)
    return List.of(scopedRule(null, null, "everyone"), scopedRule(null, 1, "profile1"), scopedRule(null, 2, "profile2"), scopedRule(7, null, "user7"));
  }

  private String valuesSeenBy(ScreenSubject subject) {
    List<Map<String, CellData>> seen = new ArrayList<>();
    ScreenSubject.filter(scopedRules(), subject).forEach(seen::add);
    return seen.stream().map(rule -> rule.get("value").getStringValue()).sorted().reduce("", (a, b) -> a + b + ",");
  }

  private void givenRequest(String type, String id, boolean allowed) throws Exception {
    when(applicationContext.getBean(AweRequest.class)).thenReturn(request);
    when(request.getParameterAsString("subjectType")).thenReturn(type);
    lenient().when(request.getParameterAsString("subjectId")).thenReturn(id);
    lenient().when(applicationContext.getBean(MenuService.class)).thenReturn(menuService);
    lenient().when(menuService.isAvailableOption("screen-personalization", AweConstants.PRIVATE_MENU)).thenReturn(allowed);
  }

  @Test
  void givenTheEveryoneSubject_onlyTheRulesOfEveryoneApply() {
    assertThat(valuesSeenBy(new ScreenSubject(null, null))).isEqualTo("everyone,");
  }

  @Test
  void givenAProfileSubject_everyoneAndThatProfileApply() {
    assertThat(valuesSeenBy(new ScreenSubject(null, 1))).isEqualTo("everyone,profile1,");
  }

  @Test
  void givenAUserSubject_everyoneItsProfileAndItsOwnRulesApply() {
    assertThat(valuesSeenBy(new ScreenSubject(7, 1))).isEqualTo("everyone,profile1,user7,");
  }

  @Test
  void givenARuleOfAUserAndAnotherProfile_theUserDoesNotGetIt() {
    List<Map<String, CellData>> rules = List.of(scopedRule(7, 2, "user7profile2"));
    assertThat(ScreenSubject.filter(rules, new ScreenSubject(7, 1))).isEmpty();
  }

  @Test
  void givenAUserSubject_theProfileIsReadFromTheUser() throws Exception {
    givenRequest("user", "7", true);
    when(applicationContext.getBean(QueryService.class)).thenReturn(queryService);
    DataList profile = new DataList();
    profile.addRow(new HashMap<>(Map.of("value", new CellData("1"))));
    when(queryService.launchPrivateQuery(eq(AweConstants.SCREEN_SUBJECT_PROFILE_QUERY), any(tools.jackson.databind.node.ObjectNode.class)))
      .thenReturn(new ServiceData().addVariable(AweConstants.ACTION_DATA, new CellData(profile)));

    assertThat(screenConfigurationGenerator.getSubject()).contains(new ScreenSubject(7, 1));
  }

  @Test
  void givenACallerWithoutAccessToThePersonalizationOption_theSubjectIsIgnored() throws Exception {
    givenRequest("profile", "1", false);

    assertThat(screenConfigurationGenerator.getSubject()).isEmpty();
  }

  @Test
  void givenNoSubjectParameters_nothingChanges() throws Exception {
    givenRequest(null, null, true);

    assertThat(screenConfigurationGenerator.getSubject()).isEqualTo(Optional.empty());
    verifyNoInteractions(menuService);
  }
}
