package com.almis.awe.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.dao.TemplateDao;
import com.almis.awe.exception.AWENotFoundException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.Tag;
import com.almis.awe.model.entities.screen.component.TagList;
import com.almis.awe.model.util.data.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.regex.Matcher;

/**
 * Manage AWE screen access
 */
@Slf4j
public class TemplateService extends ServiceConfig {

  // Autowired services
  private final MenuService menuService;
  private final STGroup elementsTemplateGroup;
  private final STGroup helpTemplateGroup;
  private final STGroup screensTemplateGroup;
  private final QueryService queryService;
  private final TemplateDao templateDao;

  /**
   * Autowired constructor
   *
   * @param menuService           Menu service
   * @param elementsTemplateGroup Element templates
   * @param helpTemplateGroup     Help templates
   * @param screensTemplateGroup  Screen templates
   * @param queryService          Query service
   * @param templateDao           Template DAO
   */
  public TemplateService(MenuService menuService,
                         @Qualifier("elementsTemplateGroup") STGroup elementsTemplateGroup,
                         @Qualifier("helpTemplateGroup") STGroup helpTemplateGroup,
                         @Qualifier("screensTemplateGroup") STGroup screensTemplateGroup,
                         QueryService queryService,
                         TemplateDao templateDao) {
    this.menuService = menuService;
    this.elementsTemplateGroup = elementsTemplateGroup;
    this.helpTemplateGroup = helpTemplateGroup;
    this.screensTemplateGroup = screensTemplateGroup;
    this.queryService = queryService;
    this.templateDao = templateDao;
  }

  /**
   * Retrieve a screen template for the default option
   *
   * @return Template
   * @throws AWException error generating template
   */
  public String getTemplate() throws AWException {

    // Get screen from option
    Menu menu = menuService.getMenu();
    Screen screen = menuService.getDefaultScreen(menu);

    // Generate screen template
    return generateScreenTemplate(screen, menu, AweConstants.BASE_VIEW, null);
  }

  /**
   * Retrieve a screen template
   *
   * @param view     Screen view
   * @param optionId Screen option identifier
   * @return Template
   * @throws AWException error generating template
   */
  public String getTemplate(String view, String optionId) throws AWException {
    // Get screen from option
    Menu menu = menuService.getMenu();
    Screen screen = menuService.getAvailableOptionScreen(optionId, menu);
    if (screen == null) {
      throw new AWENotFoundException(getLocale("ERROR_TITLE_OPTION_NOT_DEFINED"),
        getLocale("ERROR_MESSAGE_OPTION_HAS_NOT_BEEN_DEFINED", optionId));
    }

    // Generate screen template
    return generateScreenTemplate(screen, menu, view, optionId);
  }

  /**
   * Generate screen template
   *
   * @param screen   Screen object
   * @param menu     Menu
   * @param view     Screen view
   * @param optionId Option identifier
   * @return Screen template
   * @throws AWException Error generating breadcrumbs
   */
  public String generateScreenTemplate(Screen screen, Menu menu, String view, String optionId) throws AWException {
    // Generate template from screen
    ST screenTemplate = screensTemplateGroup.createStringTemplate(screensTemplateGroup.rawGetTemplate(screen.getTemplate()));

    // Call generate method on all sources
    List<Tag> tagList = screen.getElementList();
    for (Tag tag : tagList) {
      // Generate the children
      screenTemplate.add(tag.getSource(), tag.generateTemplate(elementsTemplateGroup));
    }

    // Add screen title
    screenTemplate.add(AweConstants.TEMPLATE_TITLE, screen.getLabel());

    // Add breadcrumbs
    if (AweConstants.REPORT_VIEW.equalsIgnoreCase(view)) {
      screenTemplate.add(AweConstants.TEMPLATE_BREADCRUMBS, generateBreadcrumbTemplate(optionId, menu));
    }

    // Retrieve code
    return screenTemplate.render();
  }

  /**
   * Generates screen breadcrumbs
   *
   * @param optionId Option identifier
   * @param menu     Menu
   * @return Breadcrumbs
   * @throws AWException Error generating breadcrumbs
   */
  private List<ST> generateBreadcrumbTemplate(String optionId, Menu menu) throws AWException {
    // Variable definition
    List<ST> breadcrumbs = new ArrayList<>();
    Option option = menuService.getOptionByName(optionId, menu);

    // Generate breadcrumbs
    if (option != null) {
      Option nextOption = option;
      while (nextOption.getParent() != null) {
        ST breadcrumb = elementsTemplateGroup.createStringTemplate(elementsTemplateGroup.rawGetTemplate(AweConstants.TEMPLATE_BREADCRUMB));

        nextOption = nextOption.getParent();
        breadcrumb.add("text", nextOption.getLabel());
        breadcrumbs.add(0, breadcrumb);
      }
    }
    return breadcrumbs;
  }

  /**
   * Generate application help template
   *
   * @param developers Help for developers
   * @return Application help template
   * @throws AWException Error generating breadcrumbs
   */
  public String generateApplicationHelpTemplate(boolean developers) throws AWException {
    Menu menu = menuService.getMenuWithAllRestrictions();

    // Generate template from screen
    ST screenTemplate = screensTemplateGroup.createStringTemplate(screensTemplateGroup.rawGetTemplate(AweConstants.HELP_TEMPLATE));
    ST applicationTemplate = helpTemplateGroup.createStringTemplate(helpTemplateGroup.rawGetTemplate(AweConstants.TEMPLATE_HELP_APPLICATION));
    List<Future<ST>> contents = generateMenuHelp(menu, menu.getElementList(), 1, developers);

    // Add application template
    for (Future<ST> content : contents) {
      try {
        applicationTemplate.add(AweConstants.TEMPLATE_CONTENT, content.get());
      } catch (Exception exc) {
        // Only show a log if a screen fails
        log.error("Error generating application help", exc);
        Thread.currentThread().interrupt();
      }
    }

    screenTemplate.add(AweConstants.TEMPLATE_HELP, applicationTemplate);

    // Retrieve code
    return screenTemplate.render();
  }

  /**
   * Generate option help template
   *
   * @param menu       Menu
   * @param optionId   Option identifier
   * @param developers Help for developers
   * @return Application help template
   * @throws AWException Error generating breadcrumbs
   */
  public String generateOptionHelpTemplate(Menu menu, String optionId, boolean developers) throws AWException {
    // Retrieve code
    Option option = menuService.getOptionByName(optionId, menu);
    return templateDao.generateOptionHelp(menu, option, 1, developers).render();
  }

  /**
   * Generate error template
   *
   * @param exc Exception
   * @return Error screen template
   */
  public String generateErrorTemplate(AWException exc) {
    ST errorTemplate = screensTemplateGroup.createStringTemplate(screensTemplateGroup.rawGetTemplate(AweConstants.ERROR_TEMPLATE));
    errorTemplate.add(AweConstants.TEMPLATE_TITLE, exc.getTitle());
    errorTemplate.add(AweConstants.TEMPLATE_MESSAGE, exc.getMessage());

    // Retrieve error template
    return errorTemplate.render();
  }

  /**
   * Generate menu help
   *
   * @param elementList Option list
   * @param level       Option level
   * @param developers  Help for developers
   * @return Screen template
   */
  private List<Future<ST>> generateMenuHelp(Menu menu, List<Element> elementList, Integer level, boolean developers) {
    List<Future<ST>> templateList = new CopyOnWriteArrayList<>();

    // Call generate method on all elements
    for (Element element : elementList) {
      Option option = (Option) element;
      if (option.getLabel() != null && !option.isRestricted()) {
        // Generate option template
        templateList.add(templateDao.generateOptionHelpAsync(menu, option, level, developers));

        // Generate the children
        templateList.addAll(generateMenuHelp(menu, option.getElementList(), level + 1, developers));
      }
    }

    // Retrieve code
    return templateList;
  }

  /**
   * Generates a taglist template from a screen and a taglist id
   *
   * @param tagListId Taglist identifier
   * @return Taglist template
   * @throws AWException error generating taglist template
   */
  public TagList getTagList(String tagListId) throws AWException {
    Menu menu = menuService.getMenu();
    return getTagList(menu, tagListId);
  }


  /**
   * Generates a taglist template from a screen and a taglist id
   *
   * @param menu Menu
   * @param tagListId Taglist identifier
   * @return Taglist template
   * @throws AWException error generating taglist template
   */
  public TagList getTagList(Menu menu, String tagListId) throws AWException {
    return getTagList(menuService.getDefaultScreen(menu), tagListId);
  }

  /**
   * Generates a taglist template from a screen and a taglist id
   *
   * @param menu      Menu
   * @param optionId  Option identifier
   * @param tagListId Taglist identifier
   * @return Taglist template
   * @throws AWException error generating taglist template
   */
  public TagList getTagList(Menu menu, String optionId, String tagListId) throws AWException {
    Screen screen = menuService.getAvailableOptionScreen(optionId, menu);
    if (screen == null) {
      throw new AWENotFoundException(getLocale("ERROR_TITLE_OPTION_NOT_DEFINED"), getLocale("ERROR_MESSAGE_OPTION_HAS_NOT_BEEN_DEFINED", optionId));
    }
    return getTagList(screen, tagListId);
  }

  /**
   * Retrieve taglist
   *
   * @param screen    Screen
   * @param tagListId Taglist identifier
   * @return Taglist template
   */
  public TagList getTagList(Screen screen, String tagListId) {
    return (TagList) (screen.getElementsById(tagListId).get(0));
  }

  /**
   * Retrieve taglist data
   *
   * @param tagList TagList
   * @return ServiceData Taglist data
   * @throws AWException error generating taglist template
   */
  public ServiceData loadTagListData(TagList tagList) throws AWException {
    // Check initial load attribute
    if (tagList.getTargetAction() != null) {
      return queryService.launchPrivateQuery(tagList.getTargetAction(), "1", tagList.getMax() == null ? "0" : tagList.getMax().toString());
    }
    return new ServiceData().setDataList(new DataList());
  }

  /**
   * Generates a taglist template from taglist and data
   *
   * @param tagList Tag list
   * @param data Data to fill
   * @return Tag list template
   */
  public List<String> generateTaglistTemplate(TagList tagList, DataList data) {
    // For each row, generate the children code
    return generateTaglistData(data, templateDao.generateTaglistXml(tagList.getElementList()));
  }

  /**
   * Render taglist template
   *
   * @param templateList Template
   * @return Template rendered
   */
  public String renderTagList(List<Element> templateList) {
    StringBuilder builder = new StringBuilder();
    // Call generate method on all children
    for (Element template : templateList) {
      // Generate the children
      builder.append(template.generateTemplate(elementsTemplateGroup).render());
    }
    return builder.toString();
  }

  /**
   * Generate taglist data
   *
   * @param data     Data
   * @param template Row template
   */
  private List<String> generateTaglistData(DataList data, String template) {
    List<String> tagList = new ArrayList<>();
    if (data != null) {
      for (Map<String, CellData> row : data.getRows()) {
        String elementTemplate = template;
        // Create the matcher
        Matcher matcher = TagList.wildcard.matcher(elementTemplate);
        while (matcher.find()) {
          String keyCol = matcher.group(1);
          CellData dat = row.get(keyCol);
          if (dat != null) {
            elementTemplate = elementTemplate.replace("[" + keyCol + "]", StringUtil.fixHTMLValue(dat.getStringValue()));
          }
        }
        tagList.add(elementTemplate);
      }
    }
    return tagList;
  }

  /**
   * Generates an empty screen
   *
   * @return Empty screen
   */
  public String generateEmptyScreen() {
    // Get empty screen
    return AweConstants.EMPTY_TEMPLATE;
  }
}