package com.almis.awe.service.access;

import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.access.Profile;
import com.almis.awe.model.entities.access.Restriction;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.type.RestrictionType;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.menu.MenuProvider;
import com.almis.awe.service.menu.MenuRestrictionTreeService;
import com.almis.awe.service.menu.MenuScreenResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Fixture of the tests of the access window services: the private menu, the base profile files, the profiles OPE and ADM,
 * the users and the database rules, served by mocked queries. The locales are their keys with their parameters.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
abstract class AccessServiceFixture {

  static final int OPE = 3;
  static final int ADM = 1;
  static final int JMARTIN = 7;
  static final int NO_RES = 8;

  @Mock
  MenuProvider menuProvider;
  @Mock
  MenuScreenResolver menuScreenResolver;
  @Mock
  QueryService queryService;
  @Mock
  MaintainService maintainService;
  @Mock
  MenuRestrictionTreeService menuRestrictionTreeService;
  @Mock
  AweElements elements;

  AccessSubjects subjects;
  AccessTexts texts;
  AccessTreeService treeService;
  AccessCompareService compareService;
  AccessWhyService whyService;
  AccessChangeService changeService;
  AccessImpactService impactService;
  AccessReviewService reviewService;
  AccessMenuPreviewService previewService;
  // Database rules: user id, profile id, option, restriction
  final List<Object[]> rules = new ArrayList<>();
  // Users of the profiles: user id, user, full name, profile id, own base restriction
  final List<Object[]> users = new ArrayList<>();

  @BeforeEach
  void setUpFixture() throws AWException {
    SecurityConfigProperties security = new SecurityConfigProperties();
    security.setDefaultRestriction("general");
    subjects = new AccessSubjects(menuProvider, queryService, security) {
      @Override
      public AweElements getElements() {
        return elements;
      }

      @Override
      public String getLocale(String locale) {
        return locale;
      }
    };
    texts = new AccessTexts(menuScreenResolver) {
      @Override
      public String getLocale(String locale) {
        return locale;
      }

      @Override
      public String getLocale(String locale, String... parameters) {
        return locale + Arrays.toString(parameters);
      }
    };
    compareService = new AccessCompareService(subjects, texts);
    treeService = new AccessTreeService(subjects, texts, compareService);
    whyService = new AccessWhyService(subjects, texts, queryService);
    changeService = new AccessChangeService(subjects, menuRestrictionTreeService) {
      @Override
      public <T> T getBean(Class<T> clazz) {
        return clazz.cast(maintainService);
      }
    };
    impactService = new AccessImpactService(subjects, texts, queryService);
    reviewService = new AccessReviewService(subjects, texts, queryService, menuRestrictionTreeService);
    previewService = new AccessMenuPreviewService(subjects, texts);
    // tools (module Tools) > sites, users > user-detail; settings > security; reports (module Reports) > sales
    when(menuProvider.getMenu("private")).thenAnswer(invocation -> menu(
      option("tools", "Tools", "MENU_TOOLS", option("sites", null, "MENU_SITES"),
        option("users", null, "MENU_USERS", option("user-detail", null, "MENU_USER_DETAIL"))),
      option("settings", null, "MENU_SETTINGS", option("security", null, "MENU_SECURITY")),
      option("reports", "Reports", "MENU_REPORTS", option("sales", null, "MENU_SALES"))));
    when(elements.getProfile("operator")).thenReturn(profile(restriction("settings", RestrictionType.R)));
    when(elements.getProfile("administrator")).thenReturn(profile());
    when(elements.getProfile("general")).thenReturn(profile(restriction("reports", RestrictionType.R)));
    // Profiles: OPE (operator.xml), ADM (administrator.xml). Users: jmartin (OPE, no own base), norestriction (no profile)
    when(queryService.launchPrivateQuery(eq("access-subject-profile"), any(ObjectNode.class))).thenAnswer(invocation -> {
      JsonNode profile = invocation.<ObjectNode>getArgument(1).get("profile");
      return switch (profile == null || profile.isNull() ? 0 : profile.intValue()) {
        case OPE -> data(row("profile", "OPE", "name", "Operator", "restriction", "operator"));
        case ADM -> data(row("profile", "ADM", "name", "Administrator", "restriction", "administrator"));
        default -> data();
      };
    });
    when(queryService.launchPrivateQuery(eq("access-subject-user"), any(ObjectNode.class))).thenAnswer(invocation -> {
      JsonNode user = invocation.<ObjectNode>getArgument(1).get("user");
      return switch (user == null || user.isNull() ? 0 : user.intValue()) {
        case JMARTIN -> data(row("user", "jmartin", "fullName", "Juan Martin", "profileId", OPE, "profile", "OPE",
          "userRestriction", null, "profileRestriction", "operator"));
        case NO_RES -> data(row("user", "norestriction", "fullName", null, "profileId", null, "profile", null,
          "userRestriction", "", "profileRestriction", null));
        default -> data();
      };
    });
    when(queryService.launchPrivateQuery(eq(AccessSubjects.SUBJECT_RULES_QUERY), any(ObjectNode.class))).thenAnswer(invocation -> {
      ObjectNode parameters = invocation.getArgument(1);
      Integer user = integer(parameters.get("user"));
      Integer profile = integer(parameters.get("profile"));
      String option = parameters.has("option") && !parameters.get("option").isNull()
        ? parameters.get("option").stringValue() : null;
      List<Map<String, CellData>> rows = rules.stream()
        .filter(rule -> Objects.equals(rule[0], user) && Objects.equals(rule[1], profile))
        .filter(rule -> option == null || option.equals(rule[2]))
        .map(rule -> row("option", rule[2], "restriction", rule[3]))
        .toList();
      return data(rows.toArray(Map[]::new));
    });
    // Users of a profile and their own rules, in the order of the rules
    when(queryService.launchPrivateQuery(eq(AccessImpactService.PROFILE_USERS_QUERY), any(ObjectNode.class))).thenAnswer(invocation -> {
      Integer profile = integer(invocation.<ObjectNode>getArgument(1).get("profile"));
      return data(users.stream()
        .filter(user -> Objects.equals(user[3], profile))
        .map(user -> row("userId", user[0], "user", user[1], "fullName", user[2], "userRestriction", user[4]))
        .toArray(Map[]::new));
    });
    // Every active rule, with its identifier: its position in the list of rules
    when(queryService.launchPrivateQuery(eq(AccessReviewService.REVIEW_RULES_QUERY), any(ObjectNode.class))).thenAnswer(invocation ->
      data(IntStream.range(0, rules.size())
        .mapToObj(index -> row("id", index + 1, "userId", rules.get(index)[0], "profileId", rules.get(index)[1],
          "option", rules.get(index)[2], "restriction", rules.get(index)[3]))
        .toArray(Map[]::new)));
    when(queryService.launchPrivateQuery(eq(AccessImpactService.PROFILE_USER_RULES_QUERY), any(ObjectNode.class))).thenAnswer(invocation -> {
      Integer profile = integer(invocation.<ObjectNode>getArgument(1).get("profile"));
      List<Object> ofProfile = users.stream().filter(user -> Objects.equals(user[3], profile)).map(user -> user[0]).toList();
      return data(rules.stream()
        .filter(rule -> rule[0] != null && rule[1] == null && ofProfile.contains(rule[0]))
        .map(rule -> row("userId", rule[0], "option", rule[2], "restriction", rule[3]))
        .toArray(Map[]::new));
    });
  }

  List<Map<String, CellData>> tree(String subjectType, Integer user, Integer profile, String module, String filter,
                                   String search) throws AWException {
    return tree(subjectType, user, profile, module, filter, search, null);
  }

  List<Map<String, CellData>> tree(String subjectType, Integer user, Integer profile, String module, String filter,
                                   String search, String compareWith) throws AWException {
    return treeService.getAccessTree(subjectType, user, profile, module, filter, search, compareWith).getDataList()
      .getRows();
  }

  static List<org.assertj.core.groups.Tuple> contextOf(List<Map<String, CellData>> rows) {
    return rows.stream().map(row -> tuple(text(row, "option-id"), text(row, "context"))).toList();
  }

  static List<org.assertj.core.groups.Tuple> counts(ServiceData data) {
    return data.getDataList().getRows().stream()
      .map(row -> tuple(row.get("pos").getIntegerValue(), text(row, "value"), text(row, "label"),
        row.get("count").getIntegerValue(), text(row, "tone")))
      .toList();
  }

  static Map<String, CellData> row(List<Map<String, CellData>> rows, String option) {
    return rows.stream().filter(row -> option.equals(text(row, "option-id"))).findFirst()
      .orElseThrow(() -> new AssertionError("No row " + option));
  }

  static String text(Map<String, CellData> row, String column) {
    CellData cell = row.get(column);
    assertThat(cell).as("column %s", column).isNotNull();
    return cell.getStringValue();
  }

  static Integer integer(JsonNode node) {
    return node == null || node.isNull() ? null : node.intValue();
  }

  static Map<String, CellData> row(Object... values) {
    Map<String, CellData> row = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      row.put((String) values[i], new CellData(values[i + 1]));
    }
    return row;
  }

  @SafeVarargs
  static ServiceData data(Map<String, CellData>... rows) {
    return new ServiceData().setDataList(new DataList().setRows(Arrays.stream(rows).collect(Collectors.toList()))
      .setRecords(rows.length));
  }

  static Profile profile(Restriction... restrictions) {
    return Profile.builder().restrictions(new ArrayList<>(List.of(restrictions))).build();
  }

  static Restriction restriction(String option, RestrictionType type) {
    return Restriction.builder().option(option).restrictionType(type).build();
  }

  static Menu menu(Option... options) {
    Menu menu = Menu.builder().elementList(new ArrayList<>(Arrays.<Element>asList(options))).build();
    menu.defineRelationship();
    return menu;
  }

  static Option option(String name, String module, String label, Option... children) {
    return Option.builder().name(name).module(module).label(label).icon("icon-" + name)
      .elementList(new ArrayList<>(Arrays.<Element>asList(children))).build();
  }
}
