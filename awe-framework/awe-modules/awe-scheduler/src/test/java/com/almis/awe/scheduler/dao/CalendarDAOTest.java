package com.almis.awe.scheduler.dao;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.QueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.context.ApplicationContext;

import javax.naming.NamingException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;

/**
 * Class used for testing queries through CalendarDAO class
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class CalendarDAOTest {

  @InjectMocks
  private CalendarDAO calendarDAO;

  @Mock
  private QueryService queryService;

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private Scheduler scheduler;

  @Mock
  private ApplicationContext context;

  @Mock
  private AweElements aweElements;

  @Mock
  ObjectMapper mapper;

  /**
   * Initializes json mapper for tests
   */
  @BeforeEach
  public void initBeans() throws Exception {
    calendarDAO.setApplicationContext(context);
  }

  /**
   * Check triggers contains calendars
   *
   * @throws Exception Test error
   */
  @Test
  void checkTriggersContainsCalendars() throws Exception {
    // Mock
    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getLanguage()).willReturn("ES");
    given(aweElements.getLocaleWithLanguage(anyString(), anyString())).willReturn("LOCALE");
    given(queryUtil.getParameters(any(), any(), any())).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(new DataList()));
    prepareCalendarForTests(1);
    // Check that controller are active
    assertEquals(new ServiceData().setTitle("LOCALE").setMessage("LOCALE"), calendarDAO.checkTriggersContainsCalendar(1, 2, 3));
  }

  /**
   * Check triggers contains calendars without calendar list
   *
   * @throws NamingException Test error
   */
  @Test
  void checkTriggersContainsCalendarsNoCalendar() throws Exception {
    // Mock
    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getLanguage()).willReturn("ES");
    given(aweElements.getLocaleWithLanguage(anyString(), anyString())).willReturn("LOCALE");
    prepareCalendarForTests(null);

    // Check that controller are active
    assertEquals(new ServiceData().setTitle("LOCALE").setMessage("LOCALE"), calendarDAO.checkTriggersContainsCalendar());
  }

  /**
   * Check triggers contains calendars without calendar list
   *
   * @throws NamingException Test error
   */
  @Test
  void checkTriggersContainsCalendarsOtherCalendar() throws Exception {
    // Mock
    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getLanguage()).willReturn("ES");
    given(aweElements.getLocaleWithLanguage(anyString(), anyString())).willReturn("LOCALE");
    prepareCalendarForTests(8);

    // Check that controller are active
    assertEquals(new ServiceData().setTitle("LOCALE").setMessage("LOCALE"), calendarDAO.checkTriggersContainsCalendar());
  }

  /**
   * Check triggers contains calendars without calendar list
   *
   * @throws NamingException Test error
   */
  @Test
  void checkTriggersContainsCalendarsEmpty() throws Exception {
    // Mock
    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getLanguage()).willReturn("ES");
    given(aweElements.getLocaleWithLanguage(anyString(), anyString())).willReturn("LOCALE");
    prepareCalendarForTests(1);

    // Check that controller are active
    assertEquals(new ServiceData().setTitle("LOCALE").setMessage("LOCALE"), calendarDAO.checkTriggersContainsCalendar());
  }

  /**
   * Delete scheduler calendar
   *
   * @throws NamingException Test error
   */
  @Test
  void deleteSchedulerCalendar() throws Exception {
    // Mock
    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getLanguage()).willReturn("ES");
    given(aweElements.getLocaleWithLanguage(anyString(), anyString())).willReturn("LOCALE");
    given(queryUtil.getParameters()).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(new DataList()));

    prepareCalendarForTests(1);

    // Delete calendars
    calendarDAO.deleteSchedulerCalendar(Arrays.asList(1, 2, 3));

    // Check that controller are active
    assertEquals(new ServiceData().setTitle("LOCALE").setMessage("LOCALE"), calendarDAO.checkTriggersContainsCalendar());
  }

  /**
   * Prepare calendar mocks
   */
  private void prepareCalendarForTests(Integer calendarId) throws Exception {
    // Mock
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put("calendarId", new CellData(calendarId));
    row.put("name", new CellData("Calendario guachi"));
    row.put("active", new CellData(true));
    row.put("id", new CellData(1));
    row.put("date", new CellData(new Date()));
    row.put("description", new CellData("Fecha guachi"));
    row.put("taskId", new CellData(1));
    row.put("launchType", new CellData(1));
    dataList.addRow(row);

    Set<TriggerKey> triggerSet = new HashSet<>();
    triggerSet.add(new TriggerKey("1", "DummyTrigger"));
    triggerSet.add(new TriggerKey("2", "DummyTrigger"));
    given(scheduler.getTriggerKeys(any())).willReturn(triggerSet);
    given(scheduler.getTrigger(any())).willReturn(TriggerBuilder.newTrigger().modifiedByCalendar("1").build());
  }

}
