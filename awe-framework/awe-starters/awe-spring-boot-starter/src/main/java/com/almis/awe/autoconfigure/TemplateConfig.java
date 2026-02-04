package com.almis.awe.autoconfigure;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.dao.TemplateDao;
import com.almis.awe.listener.TemplateErrorListener;
import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.service.HelpService;
import com.almis.awe.service.MenuService;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

/**
 * Manage template configuration
 */
@Configuration
@Order(HIGHEST_PRECEDENCE)
public class TemplateConfig {

  // Autowired services
  private final BaseConfigProperties baseConfigProperties;

  /**
   * Autowired constructor
   *
   * @param baseConfigProperties Base config properties
   */
  @Autowired
  public TemplateConfig(BaseConfigProperties baseConfigProperties) {
    this.baseConfigProperties = baseConfigProperties;
  }

  /**
   * Returns the list of paths found in all modules defined
   *
   * @param filePath suffix of file
   * @return Path list
   */
  private List<STGroupFile> getPaths(String filePath) {
    List<STGroupFile> paths = new ArrayList<>();

    for (String module : baseConfigProperties.getModuleList()) {
      String path = Paths.get(baseConfigProperties.getPaths().getTemplates(), module, filePath).toString();
      ClassPathResource resource = new ClassPathResource(path);
      if (resource.exists()) {
        paths.add(new STGroupFile(resource.getPath()));
      }
    }

    return paths;
  }

  /**
   * Define String Template group
   *
   * @param errorListener Error listener to attach
   * @return Template group
   */
  private STGroup defineGroup(STErrorListener errorListener, String filePath) {
    STGroup group = new STGroup('$', '$');

    // Attach listener
    group.setListener(errorListener);

    // Retrieve group files
    for (STGroupFile file : getPaths(filePath)) {
      group.loadGroupFile("", file.url.toExternalForm());
    }

    return group;
  }

  /**
   * Retrieve template error listener
   * @return Error listener
   */
  @Bean
  public STErrorListener templateErrorListener() {
    return new TemplateErrorListener();
  }

  /**
   * Retrieve elements template group
   *
   * @param errorListener Error listener
   * @return Partials template group
   */
  @Bean("elementsTemplateGroup")
  public STGroup elementsTemplateGroup(STErrorListener errorListener) {
    return defineGroup(errorListener, "elements.stg");
  }

  /**
   * Retrieve help template group
   *
   * @return Partials template group
   */
  @Bean("helpTemplateGroup")
  public STGroup helpTemplateGroup(STErrorListener errorListener) {
    return defineGroup(errorListener, "help.stg");
  }

  /**
   * Retrieve screens template group
   *
   * @return Partials template group
   */
  @Bean("screensTemplateGroup")
  public STGroup screensTemplateGroup(STErrorListener errorListener) {
    return defineGroup(errorListener, "templates.stg");
  }

  // ///////////////////////////////////////////
  // DAO
  // ///////////////////////////////////////////

  /**
   * Template DAO
   *
   * @param menuService       Menu service
   * @param aweElementsDao    AWE Elements DAO
   * @param helpTemplateGroup Help template group
   * @return Template service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public TemplateDao templateDao(MenuService menuService, AweElementsDao aweElementsDao, @Qualifier("helpTemplateGroup") STGroup helpTemplateGroup) {
    return new TemplateDao(menuService, aweElementsDao, helpTemplateGroup);
  }

  // ///////////////////////////////////////////
  // SERVICES
  // ///////////////////////////////////////////

  /**
   * Template service
   *
   * @param menuService           Menu service
   * @param elementsTemplateGroup Elements template group
   * @param helpTemplateGroup     Help template group
   * @param screensTemplateGroup  Screens template group
   * @param queryService          Query service
   * @return Template service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public TemplateService templateService(MenuService menuService,
                                         @Qualifier("elementsTemplateGroup") STGroup elementsTemplateGroup,
                                         @Qualifier("helpTemplateGroup") STGroup helpTemplateGroup,
                                         @Qualifier("screensTemplateGroup") STGroup screensTemplateGroup,
                                         QueryService queryService,
                                         TemplateDao templateDao) {
    return new TemplateService(menuService, elementsTemplateGroup, helpTemplateGroup, screensTemplateGroup, queryService, templateDao);
  }

  /**
   * Help service
   *
   * @param templateService Template service
   * @return Help service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public HelpService helpService(TemplateService templateService, MenuService menuService) {
    return new HelpService(templateService, menuService, baseConfigProperties);
  }
}
