package com.almis.awe.service.access;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.service.access.AccessCascade.Layer;
import com.almis.awe.service.access.AccessCascade.OptionAccess;
import com.almis.awe.service.access.AccessCascade.Result;
import com.almis.awe.service.access.AccessCascade.Warning;
import com.almis.awe.service.access.AccessCompareService.Comparison;
import com.almis.awe.service.access.AccessSubjects.Subject;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static com.almis.awe.service.access.AccessData.DANGER;
import static com.almis.awe.service.access.AccessData.FALSE;
import static com.almis.awe.service.access.AccessData.FILTER_ALLOWED;
import static com.almis.awe.service.access.AccessData.FILTER_DENIED;
import static com.almis.awe.service.access.AccessData.INFO;
import static com.almis.awe.service.access.AccessData.LAYER_INHERITED;
import static com.almis.awe.service.access.AccessData.PREFIX;
import static com.almis.awe.service.access.AccessData.PROFILE;
import static com.almis.awe.service.access.AccessData.SEPARATOR;
import static com.almis.awe.service.access.AccessData.SUCCESS;
import static com.almis.awe.service.access.AccessData.TRUE;
import static com.almis.awe.service.access.AccessData.USER;
import static com.almis.awe.service.access.AccessData.WARNING;
import static com.almis.awe.service.access.AccessData.WARNING_ICON;
import static com.almis.awe.service.access.AccessData.asServiceData;

/**
 * Tree of the access window (access-admin): the private menu with the effective access of a profile or a user to every
 * option, the reason of each one, its own rule and its warnings, filtered by the quick filters and the search, and the
 * counts of the quick filters
 */
public class AccessTreeService {

  private static final String FILTER_WITH_RULE = "with-rule";
  private static final String FILTER_WARNINGS = "warnings";
  private static final String FILTER_DIFFERENCES = "differences";

  private final AccessSubjects subjects;
  private final AccessTexts texts;
  private final AccessCompareService compareService;

  /**
   * Autowired constructor
   *
   * @param subjects       Subjects of the access window
   * @param texts          Texts of the access window
   * @param compareService Comparison with another profile or another user
   */
  public AccessTreeService(AccessSubjects subjects, AccessTexts texts, AccessCompareService compareService) {
    this.subjects = subjects;
    this.texts = texts;
    this.compareService = compareService;
  }

  /**
   * Retrieve the private menu as a tree with the effective access of the subject to every option, filtered by the quick
   * filter and the search: a matching option brings its ancestors, marked as context
   *
   * @param subjectType    Subject type: profile or user
   * @param user           User identifier
   * @param profile        Profile identifier
   * @param module         Selected module (optional)
   * @param filter         Quick filter: "", allowed, denied, with-rule or warnings
   * @param search         Search on the label or the name of the option (optional)
   * @param compareWith    Profile or user to compare with: profile:IdePro or user:IdeOpe (optional). It adds the access
   *                       with the profile (a user keeps its own base file and its own rules) or of the user (with its
   *                       profile and its own rules) and why it changes
   * @return Tree rows (empty without subject)
   * @throws AWException Error retrieving data
   */
  public ServiceData getAccessTree(String subjectType, Integer user, Integer profile, String module, String filter,
                                   String search, String compareWith) throws AWException {
    List<Map<String, CellData>> rows = new ArrayList<>();
    Optional<Subject> subject = subjects.get(subjectType, user, profile);
    if (subject.isPresent()) {
      Menu menu = subjects.getMenu();
      Result result = subjects.evaluate(subject.get(), module, menu);
      Comparison comparison = compareService.compare(subject.get(), module, compareWith, menu).orElse(null);
      Predicate<OptionAccess> matches = matchesFilter(filter, comparison).and(matchesSearch(search));
      Map<Option, Boolean> included = includedOptions(result, matches);
      for (OptionAccess access : result.options()) {
        if (included.containsKey(access.option())) {
          Map<String, CellData> row = getTreeRow(access, subject.get(), included.get(access.option()));
          if (comparison != null) {
            compareService.addColumns(row, access, comparison);
          }
          rows.add(row);
        }
      }
    }
    return asServiceData(rows);
  }

  /**
   * Retrieve the options of the quick filter of the tree with the number of options of each one (with the search)
   *
   * @param subjectType    Subject type: profile or user
   * @param user           User identifier
   * @param profile        Profile identifier
   * @param module         Selected module (optional)
   * @param search         Search on the label or the name of the option (optional)
   * @param compareWith    Profile or user to compare with: profile:IdePro or user:IdeOpe (optional). It adds the count of
   *                       the differences
   * @return Quick filter options: pos, value, label, count and tone
   * @throws AWException Error retrieving data
   */
  public ServiceData getAccessTreeCounts(String subjectType, Integer user, Integer profile, String module,
                                         String search, String compareWith) throws AWException {
    Optional<Subject> subject = subjects.get(subjectType, user, profile);
    List<OptionAccess> options = Collections.emptyList();
    Comparison comparison = null;
    if (subject.isPresent()) {
      Menu menu = subjects.getMenu();
      options = subjects.evaluate(subject.get(), module, menu).options().stream().filter(matchesSearch(search))
        .toList();
      comparison = compareService.compare(subject.get(), module, compareWith, menu).orElse(null);
    }
    List<Map<String, CellData>> rows = new ArrayList<>();
    rows.add(getCountRow(1, "", "FILTER_ALL", options, "", comparison));
    rows.add(getCountRow(2, FILTER_ALLOWED, "FILTER_ALLOWED", options, SUCCESS, comparison));
    rows.add(getCountRow(3, FILTER_DENIED, "FILTER_DENIED", options, DANGER, comparison));
    rows.add(getCountRow(4, FILTER_WITH_RULE, "FILTER_WITH_RULE", options, "", comparison));
    rows.add(getCountRow(5, FILTER_WARNINGS, "FILTER_WARNINGS", options, WARNING, comparison));
    if (comparison != null) {
      rows.add(getCountRow(6, FILTER_DIFFERENCES, "FILTER_DIFFERENCES", options, INFO, comparison));
    }
    return asServiceData(rows);
  }

  /**
   * Options to show: the matching ones (false) and their ancestors (true, context)
   */
  private static Map<Option, Boolean> includedOptions(Result result, Predicate<OptionAccess> matches) {
    Map<Option, OptionAccess> byOption = new IdentityHashMap<>();
    result.options().forEach(access -> byOption.put(access.option(), access));
    Map<Option, Boolean> included = new IdentityHashMap<>();
    result.options().stream().filter(matches).forEach(access -> {
      included.put(access.option(), false);
      for (Option parent = access.parent(); parent != null; parent = byOption.get(parent).parent()) {
        included.putIfAbsent(parent, true);
      }
    });
    return included;
  }

  /**
   * Options of a quick filter; only differences filters nothing without a comparison
   */
  private static Predicate<OptionAccess> matchesFilter(String filter, Comparison comparison) {
    return switch (Optional.ofNullable(filter).orElse("")) {
      case FILTER_ALLOWED -> OptionAccess::allowed;
      case FILTER_DENIED -> access -> !access.allowed();
      case FILTER_WITH_RULE -> access -> !access.ownRule().isEmpty();
      case FILTER_WARNINGS -> access -> access.warning() != null;
      case FILTER_DIFFERENCES -> comparison == null ? access -> true : comparison::differs;
      default -> access -> true;
    };
  }

  private Predicate<OptionAccess> matchesSearch(String search) {
    if (StringUtils.isBlank(search)) {
      return access -> true;
    }
    String text = search.trim();
    return access -> StringUtils.containsIgnoreCase(access.option().getName(), text)
      || StringUtils.containsIgnoreCase(texts.getOptionLabel(access.option()), text);
  }

  private Map<String, CellData> getTreeRow(OptionAccess access, Subject subject, boolean context) {
    Option option = access.option();
    Map<String, CellData> row = new HashMap<>();
    row.put("option-id", new CellData(option.getName()));
    row.put("parent-option", new CellData(access.parent() == null ? null : access.parent().getName()));
    row.put("option-is-leaf", new CellData(option.getOptions().isEmpty() ? TRUE : FALSE));
    row.put("option-name", texts.getOptionNameCell(option));
    row.put("option-key", new CellData(option.getName()));
    row.put("option-icon", new CellData(Optional.ofNullable(option.getIcon()).orElse("")));
    row.put("option-subtitle", new CellData(option.getName() + SEPARATOR + AccessTexts.getSubjectName(subject)));
    // Sent by the screen to the maintains: the option of the selected rows
    row.put("access-options", new CellData(option.getName()));
    row.put("access", new CellData(access.allowed() ? FILTER_ALLOWED : FILTER_DENIED));
    row.put("access-text", new CellData(texts.getAccessText(access.allowed())));
    row.put("access-tone", new CellData(AccessTexts.getAccessTone(access.allowed())));
    row.put("reason", new CellData(texts.getReason(access, subject)));
    row.put("reason-layer", new CellData(getReasonLayer(access)));
    row.put("own-rule", new CellData(access.ownRule()));
    row.put("own-rule-text", new CellData(texts.getRuleText(access.ownRule())));
    row.put("own-rule-tone", new CellData(AccessTexts.getRuleTone(access.ownRule())));
    row.put("warning", new CellData(access.warning() == null ? ""
      : texts.getLocale(PREFIX + "WARNING_" + access.warning().name())));
    row.put("warning-code", new CellData(getWarningCode(access.warning())));
    row.put("warning-icon", new CellData(access.warning() == null ? "" : WARNING_ICON));
    row.put("context", new CellData(context ? TRUE : FALSE));
    return row;
  }

  private static Map<String, CellData> getCountRow(int position, String value, String label,
                                                   List<OptionAccess> options, String tone, Comparison comparison) {
    Map<String, CellData> row = new HashMap<>();
    row.put("pos", new CellData(position));
    row.put("value", new CellData(value));
    row.put("label", new CellData(PREFIX + label));
    row.put("count", new CellData((int) options.stream().filter(matchesFilter(value, comparison)).count()));
    row.put("tone", new CellData(tone));
    return row;
  }

  private static String getReasonLayer(OptionAccess access) {
    if (access.inherited() && access.layer() != Layer.MODULE) {
      return LAYER_INHERITED;
    }
    return switch (access.layer()) {
      case MODULE -> "module";
      case BASE_PROFILE -> "base";
      case GLOBAL -> "global";
      case PROFILE -> PROFILE;
      case USER -> USER;
      default -> "none";
    };
  }

  private static String getWarningCode(Warning warning) {
    if (warning == null) {
      return "";
    }
    return warning == Warning.PARENT_DENIED ? "parent-denied" : "redundant";
  }
}
