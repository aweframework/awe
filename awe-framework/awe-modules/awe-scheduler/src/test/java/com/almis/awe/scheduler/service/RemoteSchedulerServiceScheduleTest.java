package com.almis.awe.scheduler.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.feign.RemoteScheduler;
import com.almis.awe.service.MaintainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests of the schedule that the service reads from the request of the screen
 */
@ExtendWith(MockitoExtension.class)
class RemoteSchedulerServiceScheduleTest {

  @Mock
  private SchedulerService schedulerService;

  @Mock
  private RemoteScheduler remoteScheduler;

  @Mock
  private ApplicationContext context;

  @Mock
  private AweRequest request;

  @Mock
  private MaintainService maintainService;

  @Mock
  private QueryUtil queryUtil;

  private final ObjectMapper mapper = new JsonMapper();
  private RemoteSchedulerService service;
  private ObjectNode parameters;

  @BeforeEach
  void setUp() {
    parameters = JsonNodeFactory.instance.objectNode();
    when(request.getParameter(anyString())).thenAnswer(call -> parameters.get(call.<String>getArgument(0)));
    lenient().when(request.getParameterAsString(anyString())).thenAnswer(call -> {
      JsonNode parameter = parameters.get(call.<String>getArgument(0));
      return parameter == null || parameter.isNull() ? null : parameter.asString();
    });
    lenient().when(request.getParameterList()).thenReturn(parameters);
    when(context.getBean(AweRequest.class)).thenReturn(request);
    lenient().when(context.getBean(MaintainService.class)).thenReturn(maintainService);
    lenient().when(context.getBean(QueryUtil.class)).thenReturn(queryUtil);
    lenient().when(queryUtil.getParameters()).thenAnswer(call -> parameters.deepCopy());
    service = new RemoteSchedulerService(schedulerService, remoteScheduler, mapper, false);
    service.setApplicationContext(context);
  }

  private Schedule computeSchedule() throws Exception {
    service.computeNextFireTimes(10);
    ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
    verify(schedulerService).computeNextFireTimes(eq(10), captor.capture());
    return captor.getValue();
  }

  @Test
  void shouldComputeTheFireTimesWhenTheClientSendsEmptyCriteriaAsNull() throws Exception {
    parameters.put("RptTyp", "0").put("RptNum", 1200).putNull("IdeCal");
    parameters.putArray("years");
    parameters.putNull("months");

    Schedule schedule = computeSchedule();

    assertThat(schedule.getRepeatNumber()).isEqualTo(1200);
    assertThat(schedule.getCalendarId()).isNull();
    assertThat(schedule.getYearList()).isEmpty();
    assertThat(schedule.getMonthList()).isNull();
  }

  @Test
  void shouldComputeTheFireTimesWhenTheClientDoesNotSendEmptyCriteriaAtAll() throws Exception {
    parameters.put("RptTyp", "0").put("RptNum", 1200);

    Schedule schedule = computeSchedule();

    assertThat(schedule.getRepeatType()).isZero();
    assertThat(schedule.getRepeatNumber()).isEqualTo(1200);
    assertThat(schedule.getCalendarId()).isNull();
    assertThat(schedule.getInitialDate()).isNull();
    assertThat((List<String>) schedule.getYearList()).isNull();
    assertThat(schedule.getSecondList()).isNull();
  }

  @Test
  void theEditorPreviewComputesTheNextFiveRunsOfThePresetAndDescribesThem() throws Exception {
    parameters.put("TypLch", 1).put("SchPre", "WRK").put("SchTim", "22:30").put("IdeCal", 3).put("RptTyp", "1");
    parameters.putArray("hours").add("4");
    ServiceData next = nextRuns("17/11/2026", "22:30:00");
    when(schedulerService.computeNextFireTimes(eq(5), any(Schedule.class))).thenReturn(next);

    service.getTaskEditorSchedule();

    ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
    verify(schedulerService).getTaskEditorSchedule(captor.capture(), eq("WRK"), eq(next.getDataList().getRows()), eq(true));
    // The runs come from the stored fields the preset writes, not from the ones of the screen
    assertThat(captor.getValue().getRepeatType()).isEqualTo(7);
    assertThat(captor.getValue().getWeekDayList()).containsExactly("1", "2", "3", "4", "5");
    assertThat(captor.getValue().getHourList()).containsExactly("22");
    assertThat(captor.getValue().getCalendarId()).isEqualTo(3);
  }

  @Test
  void aRepetitionThatCannotBeComputedYetIsAnIncompletePreview() throws Exception {
    // A field of the preset to fill: the runs are not computed
    parameters.put("TypLch", 1).put("SchPre", "DAY").put("SchTim", "");
    service.getTaskEditorSchedule();
    verify(schedulerService, never()).computeNextFireTimes(anyInt(), any(Schedule.class));
    verify(schedulerService).getTaskEditorSchedule(any(Schedule.class), eq("DAY"), eq(List.of()), eq(false));

    // Custom fields that cannot make a trigger
    parameters.put("SchPre", "CUS").put("RptTyp", "1");
    when(schedulerService.computeNextFireTimes(eq(5), any(Schedule.class)))
      .thenThrow(new AWException("Error computing next fire times"));
    service.getTaskEditorSchedule();
    verify(schedulerService).getTaskEditorSchedule(any(Schedule.class), eq("CUS"), eq(List.of()), eq(false));
  }

  @Test
  void aRepetitionWhoseEndHasPassedIsACompletePreviewWithoutRuns() throws Exception {
    // The trigger of a repetition that ended cannot be built: the preview says it ended instead of asking to fill it
    parameters.put("TypLch", 1).put("SchPre", "DAY").put("SchTim", "06:00:00").put("EndDat", "18/11/2020")
      .put("EndTim", "23:37:42");

    service.getTaskEditorSchedule();

    verify(schedulerService, never()).computeNextFireTimes(anyInt(), any(Schedule.class));
    verify(schedulerService).getTaskEditorSchedule(any(Schedule.class), eq("DAY"), eq(List.of()), eq(true));
  }

  @Test
  void savingAScheduledTaskWithAPresetStoresTheFieldsOfThePresetInItsLaunchRow() throws Exception {
    parameters.put("TypLch", 1).put("SchPre", "MON").put("SchTim", "06:15").put("SchMdy", 15).put("RptTyp", "0")
      .put("RptNum", 30).put("schExeTime", "10:00:00").put("IdeCal", 3);
    parameters.putArray("hours").add("4").add("5");
    parameters.putArray("years").add("2027");

    service.applyTaskEditorSchedule(42);

    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq("updateTaskEditorSchedule"), captor.capture());
    ObjectNode stored = captor.getValue();
    assertThat(stored.get("taskId").asInt()).isEqualTo(42);
    assertThat(stored.get("RptTyp").asInt()).isEqualTo(7);
    assertThat(stored.get("RptNum").isNull()).isTrue();
    assertThat(stored.get("days").toString()).isEqualTo("[\"15\"]");
    assertThat(stored.get("hours").toString()).isEqualTo("[\"6\"]");
    assertThat(stored.get("minutes").toString()).isEqualTo("[\"15\"]");
    assertThat(stored.get("seconds").toString()).isEqualTo("[\"0\"]");
    assertThat(stored.get("years").toString()).isEqualTo("[]");
    assertThat(stored.get("weekDays").toString()).isEqualTo("[]");
    assertThat(stored.get("schExeDate").isNull()).isTrue();
    assertThat(stored.get("schExeTime").isNull()).isTrue();
  }

  @Test
  void aFileTaskACustomPresetOrNoPresetKeepTheLaunchRowAsTheScreenSentIt() throws Exception {
    parameters.put("TypLch", 2).put("SchPre", "MON").put("SchTim", "06:15").put("SchMdy", 15);
    service.applyTaskEditorSchedule(42);
    parameters.put("TypLch", 1).put("SchPre", "CUS");
    service.applyTaskEditorSchedule(42);
    parameters.remove("SchPre");
    service.applyTaskEditorSchedule(42);

    verify(maintainService, never()).launchPrivateMaintain(anyString(), any(ObjectNode.class));
  }

  @Test
  void theFieldsOfAPresetAreSelectedInTheStoredCriteriaOfTheScreen() {
    // The «When» of the screen decides: its launch type may not have followed it yet
    parameters.put("TskWhn", 1).put("TypLch", 0).put("SchPre", "WEK").put("SchTim", "08:05");
    parameters.putArray("SchWkd").add("6").add("0");

    List<ClientAction> actions = service.getTaskEditorScheduleFields().getClientActionList();

    Map<String, String> selected = new HashMap<>();
    actions.forEach(action -> {
      assertThat(action.getType()).isEqualTo("select");
      selected.put(action.getTarget(), ((CellData) action.getParameters().get("values")).getStringValue());
    });
    assertThat(selected).containsEntry("RptTyp", "[\"7\"]").containsEntry("weekDays", "[\"6\",\"0\"]")
      .containsEntry("hours", "[\"8\"]").containsEntry("minutes", "[\"5\"]").containsEntry("seconds", "[\"0\"]")
      .containsEntry("days", "[]").containsEntry("RptNum", "[]").containsEntry("schExeDate", "[]");
    // A custom preset or another launch type select nothing
    parameters.put("SchPre", "CUS");
    assertThat(service.getTaskEditorScheduleFields().getClientActionList()).isEmpty();
    parameters.put("SchPre", "WEK").put("TskWhn", 2);
    assertThat(service.getTaskEditorScheduleFields().getClientActionList()).isEmpty();
  }

  @Test
  void changingADayOnWorkdaysMakesTheRepetitionWeeklyAndChoosingWorkdaysFillsItsDays() {
    parameters.put("TskWhn", 1).put("SchPre", "WRK").put("SchWkdPre", "WRK").put("SchTim", "22:30");
    parameters.putArray("SchWkd").add("1").add("2").add("3");

    Map<String, String> changed = selected(service.getTaskEditorScheduleFields().getClientActionList());

    // The days were changed on «Workdays»: the repetition is weekly on those days, and its stored fields follow it
    assertThat(changed).containsEntry("SchWkdPre", "[\"WEK\"]").containsEntry("SchPre", "[\"WEK\"]")
      .containsEntry("SchWkd", "[\"1\",\"2\",\"3\"]").containsEntry("weekDays", "[\"1\",\"2\",\"3\"]");

    // «Workdays» just chosen (its days were filled for another preset): its days are selected
    parameters.put("SchWkdPre", "WEK");
    Map<String, String> chosen = selected(service.getTaskEditorScheduleFields().getClientActionList());
    assertThat(chosen).containsEntry("SchWkdPre", "[\"WRK\"]").containsEntry("SchWkd", "[\"1\",\"2\",\"3\",\"4\",\"5\"]")
      .containsEntry("weekDays", "[\"1\",\"2\",\"3\",\"4\",\"5\"]").doesNotContainKey("SchPre");

    // Its own days: only the stored fields
    parameters.put("SchWkdPre", "WRK");
    parameters.putArray("SchWkd").add("1").add("2").add("3").add("4").add("5");
    assertThat(selected(service.getTaskEditorScheduleFields().getClientActionList()))
      .doesNotContainKeys("SchWkdPre", "SchWkd", "SchPre");
  }

  @Test
  void theCustomRepetitionTellsThePresetItMatchesAndGoingBackSelectsItsFields() {
    parameters.put("TskWhn", 1).put("SchPre", "CUS").put("RptTyp", "7");
    parameters.putArray("weekDays").add("1").add("2").add("3").add("4").add("5");
    parameters.putArray("hours").add("22");
    parameters.putArray("minutes").add("30");
    parameters.putArray("seconds").add("0");

    assertThat(service.getTaskEditorScheduleMatch().getDataList().getRows().get(0).get("value").getStringValue())
      .isEqualTo("WRK");
    assertThat(selected(service.getTaskEditorScheduleMatchFields().getClientActionList()))
      .containsEntry("SchPre", "[\"WRK\"]").containsEntry("SchTim", "[\"22:30:00\"]")
      .containsEntry("SchWkd", "[\"1\",\"2\",\"3\",\"4\",\"5\"]").containsEntry("SchWkdPre", "[\"WRK\"]");

    // Several hours: no preset can say it
    parameters.putArray("hours").add("8").add("14");
    assertThat(service.getTaskEditorScheduleMatch().getDataList().getRows().get(0).get("value").getStringValue()).isEmpty();
    assertThat(service.getTaskEditorScheduleMatchFields().getClientActionList()).isEmpty();
  }

  private static Map<String, String> selected(List<ClientAction> actions) {
    Map<String, String> selected = new HashMap<>();
    actions.forEach(action -> {
      assertThat(action.getType()).isEqualTo("select");
      selected.put(action.getTarget(), ((CellData) action.getParameters().get("values")).getStringValue());
    });
    return selected;
  }

  private static ServiceData nextRuns(String date, String time) {
    Map<String, CellData> row = new HashMap<>();
    row.put("nextDate", new CellData(date));
    row.put("nextTime", new CellData(time));
    DataList dataList = new DataList();
    dataList.addRow(row);
    return new ServiceData().setDataList(dataList);
  }
}
