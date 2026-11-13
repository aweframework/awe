package com.almis.awe.service.access;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.service.access.AccessCascade.Layer;
import com.almis.awe.service.access.AccessCascade.OptionAccess;
import com.almis.awe.service.access.AccessSubjects.Subject;
import com.almis.awe.service.menu.MenuScreenResolver;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.Optional;

import static com.almis.awe.model.constant.AweConstants.JSON_ICON_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_LABEL_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_VALUE_PARAMETER;
import static com.almis.awe.service.access.AccessData.ALLOW;
import static com.almis.awe.service.access.AccessData.DANGER;
import static com.almis.awe.service.access.AccessData.DENY;
import static com.almis.awe.service.access.AccessData.PREFIX;
import static com.almis.awe.service.access.AccessData.SUCCESS;

/**
 * Texts of the access window (access-admin), in the language of the session: the label of an option, the reason of an
 * access (the layer that decides it), and the text and tone of an access and of a rule
 */
public class AccessTexts extends ServiceConfig {

  private final MenuScreenResolver menuScreenResolver;

  /**
   * Autowired constructor
   *
   * @param menuScreenResolver Menu screen resolver
   */
  public AccessTexts(MenuScreenResolver menuScreenResolver) {
    this.menuScreenResolver = menuScreenResolver;
  }

  /**
   * Label of an option: its label, else the label of its screen, else its name
   *
   * @param option Option
   * @return Label
   */
  String getOptionLabel(Option option) {
    String label = Optional.ofNullable(option.getLabel())
      .orElse(Optional.ofNullable(option.getScreen())
        .map(screen -> {
          try {
            return menuScreenResolver.getScreen(screen);
          } catch (Exception exc) {
            return null;
          }
        })
        .map(Element::getLabel)
        .orElse(option.isSeparator() ? "ELEMENT_TYPE_CONTEXTSEPARATOR" : null));
    return label == null ? option.getName() : getLocale(label);
  }

  /**
   * The option as a cell: its name as value, its label and its icon (a separator without icon has a divider)
   *
   * @param option Option
   * @return Cell
   */
  CellData getOptionNameCell(Option option) {
    return new CellData(JsonNodeFactory.instance.objectNode()
      .put(JSON_VALUE_PARAMETER, option.getName())
      .put(JSON_ICON_PARAMETER, Optional.ofNullable(option.getIcon())
        .orElse(option.isSeparator() ? "minus fa-rotate-90" : null))
      .put(JSON_LABEL_PARAMETER, getOptionLabel(option)));
  }

  /**
   * Reason of an access: no rules, the option it inherits from, or the layer that decides
   *
   * @param access  Access of an option
   * @param subject Subject
   * @return Reason
   */
  String getReason(OptionAccess access, Subject subject) {
    if (access.layer() == Layer.NONE) {
      return getLocale(PREFIX + "REASON_NONE");
    } else if (access.inherited() && access.layer() != Layer.MODULE) {
      return getLocale(PREFIX + "REASON_INHERITED", getOptionLabel(access.ruleOption()));
    }
    return getLayerReason(access, subject);
  }

  /**
   * Reason of the layer that decides, without the inheritance
   *
   * @param access  Access of an option
   * @param subject Subject
   * @return Reason
   */
  String getLayerReason(OptionAccess access, Subject subject) {
    return switch (access.layer()) {
      case MODULE -> getLocale(PREFIX + "REASON_MODULE", access.moduleOption().getModule());
      case BASE_PROFILE -> getLocale(PREFIX + "REASON_BASE", subject.restriction());
      case GLOBAL -> getLocale(PREFIX + "REASON_GLOBAL");
      case PROFILE -> getLocale(PREFIX + "REASON_PROFILE", subject.profileAcronym());
      case USER -> getLocale(PREFIX + "REASON_USER", subject.userName());
      default -> getLocale(PREFIX + "REASON_NONE");
    };
  }

  String getAccessText(boolean allowed) {
    return getLocale(PREFIX + (allowed ? "ALLOWED" : "DENIED"));
  }

  static String getAccessTone(boolean allowed) {
    return allowed ? SUCCESS : DANGER;
  }

  String getRuleText(String rule) {
    return switch (Optional.ofNullable(rule).orElse("")) {
      case ALLOW -> getLocale(PREFIX + "ALLOW");
      case DENY -> getLocale(PREFIX + "DENY");
      default -> "";
    };
  }

  /**
   * Tone of the tag of a rule: success to allow, danger to deny, none without rule
   *
   * @param rule Rule: "A", "R" or none
   * @return Tone
   */
  static String getRuleTone(String rule) {
    return switch (Optional.ofNullable(rule).orElse("")) {
      case ALLOW -> SUCCESS;
      case DENY -> DANGER;
      default -> "";
    };
  }

  /**
   * Name of the subject: the user name of a user, the acronym of a profile
   *
   * @param subject Subject
   * @return Name
   */
  static String getSubjectName(Subject subject) {
    return Optional.ofNullable(subject.layer() == Layer.USER ? subject.userName() : subject.profileAcronym()).orElse("");
  }
}
