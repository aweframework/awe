package com.almis.awe.service.access;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.service.access.AccessCascade.OptionAccess;
import com.almis.awe.service.access.AccessSubjects.CompareCandidate;
import com.almis.awe.service.access.AccessSubjects.Subject;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.almis.awe.model.constant.AweConstants.JSON_LABEL_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_VALUE_PARAMETER;
import static com.almis.awe.service.access.AccessData.FALSE;
import static com.almis.awe.service.access.AccessData.FILTER_ALLOWED;
import static com.almis.awe.service.access.AccessData.FILTER_DENIED;
import static com.almis.awe.service.access.AccessData.PREFIX;
import static com.almis.awe.service.access.AccessData.TRUE;
import static com.almis.awe.service.access.AccessData.asServiceData;

/**
 * Comparison of the access window (access-admin): the access of the subject with another profile, or the access of
 * another user, to show it next to its own access and why it changes. A user is simulated with the other profile, keeping
 * its own base file and its own rules; another user has its real profile, its base file and its own rules. Nothing is
 * stored. The profile or the user to compare with is one suggest (compare-with) that searches both.
 */
public class AccessCompareService {

  private static final String DIFFERS_ICON = "lucide:arrow-left-right";
  private static final String SUGGEST_GROUP = "group";

  private final AccessSubjects subjects;
  private final AccessTexts texts;

  /**
   * Autowired constructor
   *
   * @param subjects Subjects of the access window
   * @param texts    Texts of the access window
   */
  public AccessCompareService(AccessSubjects subjects, AccessTexts texts) {
    this.subjects = subjects;
    this.texts = texts;
  }

  /**
   * Access of every option with another profile or of another user, to compare it with the access of the subject
   *
   * @param subject Subject to compare with: the subject with the other profile, or the other user
   * @param access  Access of every option of the subject to compare with
   */
  record Comparison(Subject subject, Map<Option, OptionAccess> access) {

    OptionAccess of(OptionAccess subjectAccess) {
      return access.get(subjectAccess.option());
    }

    boolean differs(OptionAccess subjectAccess) {
      return of(subjectAccess).allowed() != subjectAccess.allowed();
    }
  }

  /**
   * Access of every option with the profile or of the user to compare with, on the same menu
   *
   * @param subject     Subject
   * @param module      Selected module (optional)
   * @param compareWith Profile or user to compare with: profile:IdePro or user:IdeOpe (optional)
   * @param menu        Menu of the subject
   * @return Comparison, empty without a profile or a user to compare with
   * @throws AWException Error retrieving data
   */
  Optional<Comparison> compare(Subject subject, String module, String compareWith, Menu menu) throws AWException {
    Optional<Subject> other = subjects.getCompared(subject, compareWith);
    if (other.isEmpty()) {
      return Optional.empty();
    }
    Map<Option, OptionAccess> access = new IdentityHashMap<>();
    subjects.evaluate(other.get(), module, menu).options()
      .forEach(optionAccess -> access.put(optionAccess.option(), optionAccess));
    return Optional.of(new Comparison(other.get(), access));
  }

  /**
   * Options of the compare-with suggest: the profiles and the users that contain the text, profiles first, or the selected
   * one. The value is profile:IdePro or user:IdeOpe, the label is the code and the name ("ADM - Administrator",
   * "test - Test user") and the group is the locale key of its type (PARAMETER_PROFILE, PARAMETER_USER), which the
   * suggest shows as a header
   *
   * @param suggest Text to search, or the selected value
   * @return Options: value, label and group
   * @throws AWException Error retrieving data
   */
  public ServiceData getCompareSuggest(String suggest) throws AWException {
    List<Map<String, CellData>> rows = new ArrayList<>();
    for (CompareCandidate candidate : subjects.getCompareCandidates(suggest)) {
      String name = StringUtils.isBlank(candidate.name()) ? "" : " - " + candidate.name();
      Map<String, CellData> row = new HashMap<>();
      row.put(JSON_VALUE_PARAMETER, new CellData(candidate.target().value()));
      row.put(JSON_LABEL_PARAMETER, new CellData(candidate.code() + name));
      row.put(SUGGEST_GROUP, new CellData(candidate.target().isUser() ? "PARAMETER_USER" : "PARAMETER_PROFILE"));
      rows.add(row);
    }
    return asServiceData(rows);
  }

  /**
   * Access with the profile or of the user to compare with, why it changes ("Same" when it does not) and the mark of a differing row
   *
   * @param row        Row of the tree
   * @param access     Access of the subject to the option of the row
   * @param comparison Comparison
   */
  void addColumns(Map<String, CellData> row, OptionAccess access, Comparison comparison) {
    OptionAccess other = comparison.of(access);
    boolean differs = comparison.differs(access);
    row.put("compare-access", new CellData(other.allowed() ? FILTER_ALLOWED : FILTER_DENIED));
    row.put("compare-access-text", new CellData(texts.getAccessText(other.allowed())));
    row.put("compare-access-tone", new CellData(AccessTexts.getAccessTone(other.allowed())));
    row.put("compare-differs", new CellData(differs ? TRUE : FALSE));
    row.put("compare-reason", new CellData(differs ? texts.getReason(other, comparison.subject())
      : texts.getLocale(PREFIX + "COMPARE_SAME")));
    row.put("compare-icon", new CellData(differs ? DIFFERS_ICON : ""));
  }
}
