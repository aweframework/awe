package com.almis.awe.service;

import com.almis.awe.dao.TemplateDao;
import com.almis.awe.exception.AWENotFoundException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.TagList;
import com.almis.awe.model.util.data.DataListUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Class used for testing TemplateService class
 */
@Slf4j
class TemplateServiceTest {

  @InjectMocks
  private TemplateService templateService;

  @Mock
  private MenuService menuService;

  @Mock
  private STGroup elementsTemplateGroup;

  @Mock
  private STGroup helpTemplateGroup;

  @Mock
  private STGroup screensTemplateGroup;

  @Mock
  private QueryService queryService;

  @Mock
  private TemplateDao templateDao;

  /**
   * Initializes json mapper for tests
   */
  @BeforeEach
  public void initBeans() throws Exception {
    MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  void clearInterruptFlag() {
    Thread.interrupted();
  }

  /**
   * Check a tag list missing from the screen raises a not found error instead of an index error
   */
  @Test
  void getTagListNotInScreenThrowsNotFound() {
    // Mock
    ApplicationContext context = mock(ApplicationContext.class);
    AweElements elements = mock(AweElements.class);
    when(context.getBean(AweElements.class)).thenReturn(elements);
    when(elements.getLocaleWithLanguage("ERROR_TITLE_NOT_DEFINED", null, "notification-bulletin"))
      .thenReturn("Error: 'notification-bulletin' has not been defined");
    when(elements.getLocaleWithLanguage("ERROR_MESSAGE_NOT_DEFINED_IN", null, "notification-bulletin", "signin"))
      .thenReturn("'notification-bulletin' has not been defined in 'signin'");
    templateService.setApplicationContext(context);
    Screen screen = new Screen();
    screen.setId("signin");
    screen.setElementList(new ArrayList<>());

    // Run & assert
    assertThatThrownBy(() -> templateService.getTagList(screen, "notification-bulletin"))
      .isInstanceOf(AWENotFoundException.class)
      .hasMessage("'notification-bulletin' has not been defined in 'signin'")
      .extracting(exc -> ((AWENotFoundException) exc).getTitle())
      .isEqualTo("Error: 'notification-bulletin' has not been defined");
  }

  /**
   * Check generate template list empty
   */
  @Test
  void generateTemplateListEmpty() {
    // Mock
    when(templateDao.generateTaglistXml(anyList())).thenReturn("Template [value] tutu lala [otherValue]");
    // Run
    List<String> templates = templateService.generateTaglistTemplate(new TagList(), new DataList());
    // Assert
    assertEquals(0, templates.size());
  }

  /**
   * Check generate template list empty
   */
  @Test
  void generateTemplateListWithValues() {
    // Mock
    when(templateDao.generateTaglistXml(anyList())).thenReturn("Template [value] tutu lala [otherValue]");
    DataList dataList = new DataList();
    DataListUtil.addColumn(dataList, "value", Arrays.asList("lalala", "lerele", null));
    DataListUtil.addColumn(dataList, "otherValue", Arrays.asList("tututu", null, "tititi"));
    dataList.getRows().get(2).put("value", null);

    // Run
    List<String> templates = templateService.generateTaglistTemplate(new TagList(), dataList);

    // Assert
    assertEquals(3, templates.size());
    assertEquals("Template lalala tutu lala tututu", templates.get(0));
    assertEquals("Template lerele tutu lala ", templates.get(1));
    assertEquals("Template [value] tutu lala tititi", templates.get(2));
  }

  /**
   * Check a failed option help keeps the request thread uninterrupted
   */
  @Test
  void generateApplicationHelpTemplateKeepsThreadUninterruptedWhenAnOptionFails() throws Exception {
    // Mock
    Option option = new Option();
    option.setLabel("Option");
    option.setElementList(new ArrayList<>());
    Menu menu = new Menu();
    menu.setElementList(new ArrayList<>(List.of(option)));
    when(menuService.getMenuWithAllRestrictions()).thenReturn(menu);
    for (STGroup templateGroup : List.of(elementsTemplateGroup, helpTemplateGroup, screensTemplateGroup)) {
      when(templateGroup.createStringTemplate(any())).thenReturn(mock(ST.class));
    }
    when(templateDao.generateOptionHelpAsync(any(), any(), anyInt(), anyBoolean()))
      .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Broken help")));

    // Run
    templateService.generateApplicationHelpTemplate(false);

    // Assert
    assertThat(Thread.currentThread().isInterrupted()).isFalse();
  }

  /**
   * Check an interrupted wait for an option help restores the interrupt flag
   */
  @Test
  void generateApplicationHelpTemplateRestoresTheInterruptFlagWhenInterrupted() throws Exception {
    // Mock
    @SuppressWarnings("unchecked")
    Future<ST> interrupted = mock(Future.class);
    when(interrupted.get()).thenThrow(new InterruptedException("interrupted"));
    Option option = new Option();
    option.setLabel("Option");
    option.setElementList(new ArrayList<>());
    Menu menu = new Menu();
    menu.setElementList(new ArrayList<>(List.of(option)));
    when(menuService.getMenuWithAllRestrictions()).thenReturn(menu);
    for (STGroup templateGroup : List.of(elementsTemplateGroup, helpTemplateGroup, screensTemplateGroup)) {
      when(templateGroup.createStringTemplate(any())).thenReturn(mock(ST.class));
    }
    when(templateDao.generateOptionHelpAsync(any(), any(), anyInt(), anyBoolean())).thenReturn(interrupted);

    // Run
    templateService.generateApplicationHelpTemplate(false);

    // Assert
    assertThat(Thread.currentThread().isInterrupted()).isTrue();
  }
}
