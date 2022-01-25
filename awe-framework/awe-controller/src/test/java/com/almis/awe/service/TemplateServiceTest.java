package com.almis.awe.service;

import com.almis.awe.dao.TemplateDao;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.entities.screen.component.TagList;
import com.almis.awe.model.util.data.DataListUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.stringtemplate.v4.STGroup;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
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
}
