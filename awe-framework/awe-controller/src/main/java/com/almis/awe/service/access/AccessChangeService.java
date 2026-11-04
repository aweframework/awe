package com.almis.awe.service.access;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.access.AccessSubjects.Subject;
import com.almis.awe.service.menu.MenuRestrictionTreeService;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.almis.awe.service.access.AccessData.ALLOW;
import static com.almis.awe.service.access.AccessData.DENY;
import static com.almis.awe.service.access.AccessData.OPTION;
import static com.almis.awe.service.access.AccessData.PROFILE;
import static com.almis.awe.service.access.AccessData.PROFILE_ID;
import static com.almis.awe.service.access.AccessData.RESTRICTION;
import static com.almis.awe.service.access.AccessData.USER;
import static com.almis.awe.service.access.AccessData.USER_ID;
import static com.almis.awe.service.access.AccessData.distinct;
import static com.almis.awe.service.access.AccessData.integerOf;
import static com.almis.awe.service.access.AccessData.stringOf;

/**
 * Changes of the access window (access-admin): allow, deny or remove the own rule of the subject on some options, and
 * remove or switch the rules of an option of any subject. The own rules are stored by
 * {@link MenuRestrictionTreeService}, with the audit of the restriction maintains; a rule of an option is removed or
 * switched by its identifier, with the same audit.
 */
public class AccessChangeService extends ServiceConfig {

  // Remove and update one rule by identifier, audited in HISAweScrRes as the restriction maintains
  static final String DELETE_RULE = "delete-restriction-by-id";
  static final String UPDATE_RULE = "update-restriction-by-id";
  private static final String RULE = "rule";

  private final AccessSubjects subjects;
  private final MenuRestrictionTreeService menuRestrictionTreeService;

  /**
   * Autowired constructor
   *
   * @param subjects                   Subjects of the access window
   * @param menuRestrictionTreeService Menu restriction tree service (stores the rules)
   */
  public AccessChangeService(AccessSubjects subjects, MenuRestrictionTreeService menuRestrictionTreeService) {
    this.subjects = subjects;
    this.menuRestrictionTreeService = menuRestrictionTreeService;
  }

  /**
   * Allow one or more options to the subject with its own rule
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @param options     Option names
   * @return Service data
   * @throws AWException Error storing the rules
   */
  public ServiceData allowOptions(String subjectType, Integer user, Integer profile, List<String> options)
    throws AWException {
    Subject subject = subjects.getToChange(subjectType, user, profile);
    for (String option : distinct(options)) {
      menuRestrictionTreeService.allowMenuOption(subject.ruleUser(), subject.ruleProfile(), option);
    }
    return new ServiceData();
  }

  /**
   * Deny one or more options to the subject with its own rule
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @param options     Option names
   * @return Service data
   * @throws AWException Error storing the rules
   */
  public ServiceData denyOptions(String subjectType, Integer user, Integer profile, List<String> options)
    throws AWException {
    Subject subject = subjects.getToChange(subjectType, user, profile);
    for (String option : distinct(options)) {
      menuRestrictionTreeService.restrictMenuOption(subject.ruleUser(), subject.ruleProfile(), option);
    }
    return new ServiceData();
  }

  /**
   * Remove the own rule of the subject on one or more options (the options without own rule are skipped)
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @param options     Option names
   * @return Service data
   * @throws AWException Error removing the rules
   */
  public ServiceData removeOptionRules(String subjectType, Integer user, Integer profile, List<String> options)
    throws AWException {
    Subject subject = subjects.getToChange(subjectType, user, profile);
    for (String option : distinct(options)) {
      if (!subjects.getRules(subject.ruleUser(), subject.ruleProfile(), option).isEmpty()) {
        menuRestrictionTreeService.removeRestriction(subject.ruleUser(), subject.ruleProfile(), option);
      }
    }
    return new ServiceData();
  }

  /**
   * Remove rules of an option, of any subject (a profile, a user or global), as the side panel of an option lists them:
   * only the row of each identifier, another row of the same subject and option stays; an unknown rule is skipped
   *
   * @param rules Rule identifiers (IdeAweScrRes): one, or the list sent by a grid
   * @return Service data
   * @throws AWException Error removing the rules
   */
  public ServiceData removeRules(List<Integer> rules) throws AWException {
    for (Integer rule : Optional.ofNullable(rules).orElse(Collections.emptyList())) {
      Optional<Map<String, CellData>> found = subjects.getRule(rule);
      if (found.isPresent()) {
        getBean(MaintainService.class).launchPrivateMaintain(DELETE_RULE, getRuleParameters(rule, found.get(),
          stringOf(found.get().get(RULE))));
      }
    }
    return new ServiceData();
  }

  /**
   * Switch rules of an option, of any subject, from allow to deny and from deny to allow, as the side panel of an option
   * lists them: only the row of each identifier; an unknown rule is skipped
   *
   * @param rules Rule identifiers (IdeAweScrRes): one, or the list sent by a grid
   * @return Service data
   * @throws AWException Error storing the rules
   */
  public ServiceData switchRules(List<Integer> rules) throws AWException {
    for (Integer rule : Optional.ofNullable(rules).orElse(Collections.emptyList())) {
      Optional<Map<String, CellData>> found = subjects.getRule(rule);
      if (found.isPresent()) {
        String switched = DENY.equals(stringOf(found.get().get(RULE))) ? ALLOW : DENY;
        getBean(MaintainService.class).launchPrivateMaintain(UPDATE_RULE, getRuleParameters(rule, found.get(),
          switched));
      }
    }
    return new ServiceData();
  }

  /**
   * Parameters of the maintains of a rule by identifier: the identifier, and its subject, option and rule for the audit
   * (HISAweScrRes), as the restriction maintains store them
   */
  private static ObjectNode getRuleParameters(Integer rule, Map<String, CellData> found, String restriction) {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.put("id", rule);
    parameters.put(USER, integerOf(found.get(USER_ID)));
    parameters.put(PROFILE, integerOf(found.get(PROFILE_ID)));
    parameters.put(OPTION, stringOf(found.get(OPTION)));
    parameters.put(RESTRICTION, restriction);
    parameters.put("active", 1);
    return parameters;
  }
}
