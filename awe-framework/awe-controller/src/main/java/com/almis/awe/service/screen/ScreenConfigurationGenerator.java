package com.almis.awe.service.screen;


import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.Component;
import com.almis.awe.model.entities.screen.component.grid.Column;
import com.almis.awe.model.type.AttributeRestrictionType;
import com.almis.awe.service.MenuService;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Manage the specific configuration of a screen
 */
@Slf4j
public class ScreenConfigurationGenerator extends ServiceConfig {

  // Request parameters of the personalization editor: subjectType (everyone, profile or user) and subjectId
  private static final String SUBJECT_TYPE = "subjectType";
  private static final String SUBJECT_ID = "subjectId";
  private static final String PERSONALIZATION_OPTION = "screen-personalization";

  /**
   * Subject the screen has to be rendered for, read from the request. Only a caller with access to the personalization
   * option may choose one; otherwise the parameters are ignored and the screen is rendered as for the session
   *
   * @return Subject, empty when the screen is rendered as for the session
   */
  public Optional<ScreenSubject> getSubject() {
    String type = getRequest().getParameterAsString(SUBJECT_TYPE);
    if (type == null || type.isBlank()) {
      return Optional.empty();
    }
    try {
      if (!getBean(MenuService.class).isAvailableOption(PERSONALIZATION_OPTION, AweConstants.PRIVATE_MENU)) {
        log.warn("A screen subject was requested by a caller without access to the personalization option; it is ignored");
        return Optional.empty();
      }
      String id = getRequest().getParameterAsString(SUBJECT_ID);
      return switch (type) {
        case "everyone" -> Optional.of(new ScreenSubject(null, null));
        case "profile" -> Optional.of(new ScreenSubject(null, Integer.valueOf(id)));
        case "user" -> Optional.of(new ScreenSubject(Integer.valueOf(id), getProfileOfUser(id)));
        default -> Optional.empty();
      };
    } catch (AWException | RuntimeException exc) {
      log.warn("The screen subject could not be resolved; the screen is rendered as for the session", exc);
      return Optional.empty();
    }
  }

  private Integer getProfileOfUser(String userId) throws AWException {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode().put(SUBJECT_ID, userId);
    DataList profile = (DataList) getBean(QueryService.class).launchPrivateQuery(AweConstants.SCREEN_SUBJECT_PROFILE_QUERY, parameters)
      .getVariableMap().get(AweConstants.ACTION_DATA).getObjectValue();
    return profile.getRows().isEmpty() ? null : Integer.valueOf(profile.getRows().get(0).get("value").getStringValue());
  }

  /**
   * Store screen target data in components
   * @param configurationTask Screen configuration future
   * @param screen Screen bean
   */
  public void applyScreenConfiguration(Future<ServiceData> configurationTask, Screen screen)  {
    applyScreenConfiguration(configurationTask, screen, Optional.empty());
  }

  /**
   * Store screen target data in components, with the rules of a subject when there is one
   * @param configurationTask Screen configuration future
   * @param screen Screen bean
   * @param subject Subject the rules are filtered for
   */
  public void applyScreenConfiguration(Future<ServiceData> configurationTask, Screen screen, Optional<ScreenSubject> subject) {
    // Retrieve screen configuration if defined
    try {
      ServiceData screenConfigurationOutput = configurationTask.get();
      DataList screenConfiguration = (DataList) screenConfigurationOutput.getVariableMap().get(AweConstants.ACTION_DATA).getObjectValue();

      // For each column, store value in components
      addScreenConfigurationToComponents(screenConfiguration, screen, subject);
    } catch (ExecutionException exc) {
      log.error(getLocale("ERROR_TITLE_SCREEN_GENERATION_ERROR"), getLocale("ERROR_MESSAGE_SCREEN_CONFIGURATION_DATA", screen.getId()), exc);
    } catch (InterruptedException exc) {
      log.error(getLocale("ERROR_TITLE_SCREEN_GENERATION_ERROR"), getLocale("ERROR_MESSAGE_SCREEN_CONFIGURATION_DATA", screen.getId()), exc);
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Add screen configuration to components
   * @param data Screen configuration data
   */
  private void addScreenConfigurationToComponents(DataList data, Screen screen, Optional<ScreenSubject> subject) {
    // Rules are applied from the least to the most specific one, so the most specific one wins
    List<Map<String, CellData>> rules = new ArrayList<>(subject.map(value -> ScreenSubject.filter(data.getRows(), value)).orElse(data.getRows()));
    rules.sort(Comparator.comparingInt(ScreenConfigurationGenerator::scopeRank)
      .thenComparingInt(rule -> rule.get("screen").getStringValue().isEmpty() ? 0 : 1));

    for (Map<String, CellData> rule : rules) {
      String componentId = rule.get("component").getStringValue();
      String attribute = rule.get("attribute").getStringValue();
      String value = rule.get("value").getStringValue();
      List<Element> elements = screen.getElementsById(componentId);
      if (!elements.isEmpty()) {
        applyRule(attribute, value, (Component) elements.get(0));
      }
    }
  }

  /**
   * Scope of a rule: everyone (0) < profile (1) < user (2)
   * @param rule Rule row
   * @return Scope rank
   */
  private static int scopeRank(Map<String, CellData> rule) {
    if (!rule.get("user").getStringValue().isEmpty()) {
      return 2;
    }
    return rule.get("profile").getStringValue().isEmpty() ? 0 : 1;
  }

  /**
   * Apply rule to component
   * @param attribute Attribute to change
   * @param value Value to set
   * @param component Component to change
   */
  private void applyRule(String attribute, String value, Component component) {
    // Get restriction; an attribute no longer in the catalog must not break the whole screen
    AttributeRestrictionType attributeRestriction;
    try {
      attributeRestriction = AttributeRestrictionType.getEnum(attribute);
    } catch (IllegalArgumentException exc) {
      log.warn("WARNING: Screen configuration parameter '{}' is not a known attribute. Please remove it from screen configuration", attribute);
      return;
    }

    // If property doesn't belong to the component, log it and return
    if (!attributeRestriction.appliesTo(component.getClass())) {
      log.warn("WARNING: Screen configuration parameter '{}' is not a property from '{}' component. Please remove it from screen configuration", attribute, component.getClass().getSimpleName());
      return;
    }

    // Get accessor
    PropertyAccessor myAccessor = PropertyAccessorFactory.forDirectFieldAccess(component);

    // Manage special cases
    switch (attributeRestriction) {
      // Visible / Hidden in columns
      case VISIBLE:
        if (component instanceof Column) {
          myAccessor.setPropertyValue(AttributeRestrictionType.HIDDEN.toString(), String.valueOf(!Boolean.parseBoolean(value)));
        } else {
          myAccessor.setPropertyValue(attributeRestriction.getValue(), value); // NOSONAR java:S4512 name from the AttributeRestrictionType whitelist
        }
        break;
      case REQUIRED:
        myAccessor.setPropertyValue(AttributeRestrictionType.VALIDATION.toString(), String.format("{required:%b}", Boolean.parseBoolean(value)));
        break;
      default:
        // Set property value (by the field name, as the stored attribute may differ in case)
        myAccessor.setPropertyValue(attributeRestriction.getValue(), value); // NOSONAR java:S4512 name from the AttributeRestrictionType whitelist
        break;
    }
  }
}
