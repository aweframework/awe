package com.almis.awe.service.access;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.access.AccessCascade.Layer;
import com.almis.awe.service.access.AccessCascade.OptionAccess;
import com.almis.awe.service.access.AccessCascade.Result;
import com.almis.awe.service.access.AccessSubjects.Subject;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.almis.awe.service.access.AccessData.ALLOW;
import static com.almis.awe.service.access.AccessData.DANGER;
import static com.almis.awe.service.access.AccessData.DENY;
import static com.almis.awe.service.access.AccessData.DESCRIPTION;
import static com.almis.awe.service.access.AccessData.FALSE;
import static com.almis.awe.service.access.AccessData.ICON;
import static com.almis.awe.service.access.AccessData.ICON_TONE;
import static com.almis.awe.service.access.AccessData.LAYER_INHERITED;
import static com.almis.awe.service.access.AccessData.OPTION;
import static com.almis.awe.service.access.AccessData.PREFIX;
import static com.almis.awe.service.access.AccessData.PROFILE;
import static com.almis.awe.service.access.AccessData.PROFILE_ID;
import static com.almis.awe.service.access.AccessData.SUBJECT_PROFILE;
import static com.almis.awe.service.access.AccessData.SUBJECT_USER;
import static com.almis.awe.service.access.AccessData.SUCCESS;
import static com.almis.awe.service.access.AccessData.TITLE;
import static com.almis.awe.service.access.AccessData.TRUE;
import static com.almis.awe.service.access.AccessData.USER;
import static com.almis.awe.service.access.AccessData.USER_ID;
import static com.almis.awe.service.access.AccessData.allRows;
import static com.almis.awe.service.access.AccessData.asServiceData;
import static com.almis.awe.service.access.AccessData.integerOf;
import static com.almis.awe.service.access.AccessData.stringOf;

/**
 * Why side panel of the access window (access-admin): the result of an option for the subject, the cascade of layers
 * with the verdict of each one and the one that decides, and every rule that exists on the option
 */
public class AccessWhyService {

  private static final String OPTION_RULE_LIST_QUERY = "access-option-rule-list";

  private final AccessSubjects subjects;
  private final AccessTexts texts;
  private final QueryService queryService;

  /**
   * Autowired constructor
   *
   * @param subjects     Subjects of the access window
   * @param texts        Texts of the access window
   * @param queryService Query service
   */
  public AccessWhyService(AccessSubjects subjects, AccessTexts texts, QueryService queryService) {
    this.subjects = subjects;
    this.texts = texts;
    this.queryService = queryService;
  }

  /**
   * Retrieve the cascade of an option for the subject: the verdict of each layer and the one that decides
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @param module      Selected module (optional)
   * @param option      Option name
   * @return Steps: module, base profile, global rule, profile rule, user rule and inheritance
   * @throws AWException Error retrieving data
   */
  public ServiceData getAccessWhy(String subjectType, Integer user, Integer profile, String module, String option)
    throws AWException {
    List<Map<String, CellData>> rows = new ArrayList<>();
    Optional<Subject> subject = subjects.get(subjectType, user, profile);
    if (subject.isPresent() && option != null) {
      Result result = subjects.evaluate(subject.get(), module);
      Optional<OptionAccess> found = result.find(option);
      found.ifPresent(access -> rows.addAll(getWhySteps(result, access, subject.get(), module)));
    }
    return asServiceData(rows);
  }

  /**
   * Retrieve the result of an option for the subject, as a card: whether it can open it, and the reason (the layer that
   * decides)
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @param module      Selected module (optional)
   * @param option      Option name
   * @return One row (title, description, icon and iconTone), none without subject or option
   * @throws AWException Error retrieving data
   */
  public ServiceData getAccessResult(String subjectType, Integer user, Integer profile, String module, String option)
    throws AWException {
    List<Map<String, CellData>> rows = new ArrayList<>();
    Optional<Subject> subject = subjects.get(subjectType, user, profile);
    if (subject.isPresent() && option != null) {
      Optional<OptionAccess> found = subjects.evaluate(subject.get(), module).find(option);
      found.ifPresent(access -> {
        Map<String, CellData> row = new HashMap<>();
        row.put(TITLE, new CellData(texts.getLocale(PREFIX + (access.allowed() ? "RESULT_ALLOWED" : "RESULT_DENIED"),
          AccessTexts.getSubjectName(subject.get()), texts.getOptionLabel(access.option()))));
        row.put(DESCRIPTION, new CellData(texts.getReason(access, subject.get())));
        row.put(ICON, new CellData(access.allowed() ? "lucide:circle-check" : "lucide:ban"));
        row.put(ICON_TONE, new CellData(access.allowed() ? SUCCESS : DANGER));
        rows.add(row);
      });
    }
    return asServiceData(rows);
  }

  /**
   * Retrieve every active rule on an option: global, profile and user rules
   *
   * @param option Option name
   * @return Rules: id, who-type, who, who-sub, rule and rule-text
   * @throws AWException Error retrieving data
   */
  public ServiceData getOptionRules(String option) throws AWException {
    ObjectNode parameters = allRows().put(OPTION, option);
    List<Map<String, CellData>> rules = new ArrayList<>(queryService
      .launchPrivateQuery(OPTION_RULE_LIST_QUERY, parameters).getDataList().getRows());
    List<Map<String, CellData>> rows = rules.stream()
      // A rule with a user and a profile is ignored by the real menu
      .filter(rule -> integerOf(rule.get(USER_ID)) == null || integerOf(rule.get(PROFILE_ID)) == null)
      .sorted(Comparator.comparingInt(AccessWhyService::ruleOrder)
        .thenComparing(rule -> Optional.ofNullable(integerOf(rule.get("id"))).orElse(0)))
      .map(this::getOptionRuleRow)
      .toList();
    return asServiceData(rows);
  }

  private List<Map<String, CellData>> getWhySteps(Result result, OptionAccess access, Subject subject, String module) {
    String name = access.option().getName();
    boolean inherited = access.inherited() && access.layer() != Layer.MODULE;
    List<Map<String, CellData>> steps = new ArrayList<>();

    // Module
    steps.add(getWhyStep(1, "module", "STEP_MODULE", getModuleDetail(access, module),
      access.moduleOption() != null ? DENY : "", access.layer() == Layer.MODULE));

    // Base profile, global rule, profile rule and user rule on the option itself
    String base = result.ruleOf(Layer.BASE_PROFILE, name);
    steps.add(getWhyStep(2, "base", "STEP_BASE",
      texts.getLocale(PREFIX + (base.isEmpty() ? "DETAIL_BASE_NONE" : "DETAIL_BASE_RULE"), subject.restriction()), base,
      !inherited && access.layer() == Layer.BASE_PROFILE));
    String global = result.ruleOf(Layer.GLOBAL, name);
    steps.add(getWhyStep(3, "global", "STEP_GLOBAL",
      texts.getLocale(PREFIX + (global.isEmpty() ? "DETAIL_GLOBAL_NONE" : "DETAIL_GLOBAL_RULE")), global,
      !inherited && access.layer() == Layer.GLOBAL));
    String profile = result.ruleOf(Layer.PROFILE, name);
    steps.add(getWhyStep(4, PROFILE, "STEP_PROFILE", getProfileDetail(subject, profile), profile,
      !inherited && access.layer() == Layer.PROFILE));
    String user = result.ruleOf(Layer.USER, name);
    steps.add(getWhyStep(5, USER, "STEP_USER", getUserDetail(subject, user), user,
      !inherited && access.layer() == Layer.USER));

    // Inheritance from an ancestor
    String inheritedDetail = inherited
      ? texts.getLocale(PREFIX + "DETAIL_INHERITED", texts.getOptionLabel(access.ruleOption()),
      texts.getLayerReason(access, subject))
      : texts.getLocale(PREFIX + "DETAIL_NOT_INHERITED");
    steps.add(getWhyStep(6, LAYER_INHERITED, "STEP_INHERITANCE", inheritedDetail,
      inherited ? verdictOf(access) : "", inherited));
    return steps;
  }

  private String getModuleDetail(OptionAccess access, String module) {
    if (StringUtils.isBlank(module)) {
      return texts.getLocale(PREFIX + "DETAIL_NO_MODULE");
    }
    return access.moduleOption() != null
      ? texts.getLocale(PREFIX + "DETAIL_MODULE_HIDDEN", access.moduleOption().getModule(), module)
      : texts.getLocale(PREFIX + "DETAIL_MODULE_VISIBLE", module);
  }

  private String getProfileDetail(Subject subject, String profile) {
    return subject.profileAcronym() == null ? texts.getLocale(PREFIX + "DETAIL_NO_PROFILE")
      : texts.getLocale(PREFIX + (profile.isEmpty() ? "DETAIL_PROFILE_NONE" : "DETAIL_PROFILE_RULE"),
      subject.profileAcronym());
  }

  private String getUserDetail(Subject subject, String user) {
    return subject.layer() != Layer.USER ? texts.getLocale(PREFIX + "DETAIL_USER_NOT_APPLICABLE")
      : texts.getLocale(PREFIX + (user.isEmpty() ? "DETAIL_USER_NONE" : "DETAIL_USER_RULE"), subject.userName());
  }

  private Map<String, CellData> getWhyStep(int step, String layer, String title, String detail, String verdict,
                                           boolean decides) {
    Map<String, CellData> row = new HashMap<>();
    row.put("step", new CellData(step));
    row.put("layer", new CellData(layer));
    row.put(TITLE, new CellData(texts.getLocale(PREFIX + title)));
    row.put("detail", new CellData(detail));
    row.put("verdict", new CellData(verdict));
    row.put("verdict-text", new CellData(texts.getRuleText(verdict)));
    row.put("verdict-tone", new CellData(AccessTexts.getRuleTone(verdict)));
    row.put("decides", new CellData(decides ? TRUE : FALSE));
    row.put("decides-text", new CellData(decides ? texts.getLocale(PREFIX + "DECIDES") : ""));
    return row;
  }

  private Map<String, CellData> getOptionRuleRow(Map<String, CellData> rule) {
    String whoType;
    String who;
    String whoSub;
    if (integerOf(rule.get(USER_ID)) != null) {
      whoType = SUBJECT_USER;
      who = stringOf(rule.get(USER));
      whoSub = stringOf(rule.get("userName"));
    } else if (integerOf(rule.get(PROFILE_ID)) != null) {
      whoType = SUBJECT_PROFILE;
      who = stringOf(rule.get(PROFILE));
      whoSub = stringOf(rule.get("profileName"));
    } else {
      whoType = "global";
      who = texts.getLocale(PREFIX + "WHO_GLOBAL");
      whoSub = texts.getLocale(PREFIX + "REASON_GLOBAL");
    }
    String ruleValue = stringOf(rule.get("rule"));
    Map<String, CellData> row = new HashMap<>();
    row.put("id", new CellData(integerOf(rule.get("id"))));
    // Sent by the screen to access-remove-rule: the rule of the selected row
    row.put("access-rule", new CellData(integerOf(rule.get("id"))));
    row.put("who-type", new CellData(whoType));
    row.put("who", new CellData(who));
    row.put("who-sub", new CellData(whoSub));
    row.put("rule", new CellData(ruleValue));
    row.put("rule-text", new CellData(texts.getRuleText(ruleValue)));
    row.put("rule-tone", new CellData(AccessTexts.getRuleTone(ruleValue)));
    return row;
  }

  private static int ruleOrder(Map<String, CellData> rule) {
    if (integerOf(rule.get(USER_ID)) != null) {
      return 2;
    }
    return integerOf(rule.get(PROFILE_ID)) != null ? 1 : 0;
  }

  private static String verdictOf(OptionAccess access) {
    return access.allowed() ? ALLOW : DENY;
  }
}
