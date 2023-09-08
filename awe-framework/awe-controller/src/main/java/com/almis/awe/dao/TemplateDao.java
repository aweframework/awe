package com.almis.awe.dao;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.service.MenuService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/*
 * File Imports
 */

/**
 * Initial load runner
 * Launches initial load values
 *
 * @author Pablo GARCIA - 20/MAR/2017
 */
public class TemplateDao {

  // Autowired services
  private final MenuService menuService;
  private final AweElementsDao aweElementsDao;
  private final STGroup helpTemplateGroup;

  /**
   * Autowired constructor
   *
   * @param menuService       Menu service
   * @param aweElementsDao    AWE Elements DAO
   * @param helpTemplateGroup Help templates
   */
  public TemplateDao(MenuService menuService,
                     AweElementsDao aweElementsDao,
                     @Qualifier("helpTemplateGroup") STGroup helpTemplateGroup) {
    this.menuService = menuService;
    this.aweElementsDao = aweElementsDao;
    this.helpTemplateGroup = helpTemplateGroup;
  }

  /**
   * Generate option template
   *
   * @param option     Option
   * @param level      Option level
   * @param developers Help for developers
   * @return Screen template
   */
  @Async("threadHelpPoolTaskExecutor")
  public Future<ST> generateOptionHelpAsync(Option option, Integer level, boolean developers) {
    // Retrieve code
    return CompletableFuture.completedFuture(generateOptionHelp(option, level, developers));
  }

  /**
   * Generate option template
   *
   * @param option     Option
   * @param level      Option level
   * @param developers Help for developers
   * @return Screen template
   */
  public ST generateOptionHelp(Option option, Integer level, boolean developers) {
    // Generate template from screen
    ST optionTemplate = helpTemplateGroup.createStringTemplate(helpTemplateGroup.rawGetTemplate(AweConstants.TEMPLATE_HELP_OPTION));

    String optionLabel = option.getLabel();
    if (option.getScreen() != null && !option.isMenuScreen()) {
      Screen screen;
      try {
        screen = menuService.getOptionScreen(option.getName());
      } catch (AWException ex) {
        throw new IllegalArgumentException(ex);
      }
      optionLabel = screen.getLabel();
      optionTemplate.add(AweConstants.TEMPLATE_CONTENT, generateScreenHelp(screen, developers));
    }

    // Add screen title
    optionTemplate.add(AweConstants.TEMPLATE_E, option);
    optionTemplate.add(AweConstants.TEMPLATE_TITLE, optionLabel);
    optionTemplate.add(AweConstants.TEMPLATE_LEVEL, level);
    optionTemplate.add(AweConstants.TEMPLATE_ICON, getIconTemplate(option.getIcon()));

    // Retrieve code
    return optionTemplate;
  }

  private String getIconTemplate(String iconValue) {
    String iconTemplate = "";
    if (iconValue != null) {
      String[] iconValues = iconValue.split(":");
      String icon = iconValue;
      String family = "fa";
      if (iconValues.length == 2) {
        family = iconValues[0];
        icon = iconValues[1];
      }

      switch (family) {
        case "mdi":
          iconTemplate = "<i role=\"icon\" class=\"help-icon text-primary material-icons\">" + icon + "</i>";
          break;
        case "fa":
        default:
          iconTemplate = "<i role=\"icon\" class=\"help-icon text-primary fa fa-" + icon + " fa-2x fa-fw\"></i>";
          break;
      }
    }
    return iconTemplate;
  }

  /**
   * Generate screen template
   *
   * @param screen     Screen
   * @param developers Help for developers
   * @return Screen template
   */
  private ST generateScreenHelp(Screen screen, boolean developers) {
    // Generate template from screen
    ST screenTemplate = helpTemplateGroup.createStringTemplate(helpTemplateGroup.rawGetTemplate(AweConstants.TEMPLATE_HELP_SCREEN));
    List<ST> contents = new CopyOnWriteArrayList<>();

    // Call generate method on all sources
    for (Element element : screen.getElementList()) {
      // Generate the children
      contents.add(element.generateHelpTemplate(helpTemplateGroup, null, developers));
    }

    // Add screen title
    screenTemplate.add(AweConstants.TEMPLATE_E, screen);
    screenTemplate.add(AweConstants.TEMPLATE_CONTENT, contents);

    // Retrieve code
    return screenTemplate;
  }

  /**
   * Generate taglist XML from object
   *
   * @param tagListElements TagList elements
   * @return TagList elements XML template
   */
  public String generateTaglistXml(List<Element> tagListElements) {
    return tagListElements.stream().map(aweElementsDao::toXMLString).collect(Collectors.joining());
  }
}