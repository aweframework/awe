package com.almis.awe.service.access;

import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.service.access.AccessCascade.Layer;
import com.almis.awe.service.access.AccessCascade.OptionAccess;
import com.almis.awe.service.access.AccessCascade.Result;
import com.almis.awe.service.access.AccessCascade.Warning;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Review of an own rule of a subject (a profile or a user) on its cascade: whether it has no effect or does not make
 * sense. A rule on an option that is not in the menu is obsolete; an allowed option whose parent is denied cannot be
 * reached from the menu; a rule that does not change the access of any option, without module nor in any module of the
 * menu, is redundant. The cascade is evaluated without module: a parent denied by a rule is denied in every module, and
 * a parent hidden only by its module is how the module works, not a finding.
 */
final class AccessReview {

  private AccessReview() {
  }

  /**
   * Group of a finding, in the order the review shows them
   */
  enum Group {
    OBSOLETE, PARENT_DENIED, REDUNDANT;

    /**
     * Position of the group in the review, from 1
     *
     * @return Position
     */
    int position() {
      return ordinal() + 1;
    }

    /**
     * The rule has no effect: removing it changes nothing
     *
     * @return Without effect
     */
    boolean withoutEffect() {
      return this != PARENT_DENIED;
    }
  }

  /**
   * Finding of a rule
   *
   * @param group  Group
   * @param access Access of the option of the rule (null when the option is not in the menu)
   * @param parent Denied parent to allow so the option can be reached (only PARENT_DENIED)
   */
  record Finding(Group group, OptionAccess access, OptionAccess parent) {
  }

  /**
   * Review an own rule of the subject on an option
   *
   * @param result Cascade of the subject
   * @param layer  Layer of the own rules of the subject: PROFILE or USER
   * @param option Option of the rule
   * @return Finding, empty when the rule makes sense
   */
  static Optional<Finding> review(Result result, Layer layer, String option) {
    Optional<OptionAccess> found = result.find(option);
    if (found.isEmpty()) {
      return Optional.of(new Finding(Group.OBSOLETE, null, null));
    }
    OptionAccess access = found.get();
    if (access.warning() == Warning.PARENT_DENIED && "A".equals(result.ruleOf(layer, option))) {
      return Optional.of(new Finding(Group.PARENT_DENIED, access, getTopDeniedParent(result, access)));
    } else if (access.warning() == Warning.REDUNDANT) {
      return Optional.of(new Finding(Group.REDUNDANT, access, null));
    }
    return Optional.empty();
  }

  /**
   * The first denied parent from the top of the menu: allowing a lower one would leave it hidden by the upper one
   */
  private static OptionAccess getTopDeniedParent(Result result, OptionAccess access) {
    Map<Option, OptionAccess> byOption = new IdentityHashMap<>();
    result.options().forEach(optionAccess -> byOption.put(optionAccess.option(), optionAccess));
    OptionAccess top = null;
    for (Option parent = access.parent(); parent != null; parent = byOption.get(parent).parent()) {
      if (!byOption.get(parent).allowed()) {
        top = byOption.get(parent);
      }
    }
    return top;
  }
}
