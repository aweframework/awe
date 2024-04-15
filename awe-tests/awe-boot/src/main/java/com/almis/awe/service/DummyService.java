package com.almis.awe.service;

import com.almis.awe.builder.client.SelectActionBuilder;
import com.almis.awe.builder.enumerates.Action;
import com.almis.awe.builder.screen.ScreenBuilder;
import com.almis.awe.builder.screen.TagBuilder;
import com.almis.awe.builder.screen.button.ButtonActionBuilder;
import com.almis.awe.builder.screen.button.ButtonBuilder;
import com.almis.awe.builder.screen.criteria.HiddenCriteriaBuilder;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.Planet;
import com.almis.awe.model.Planets;
import com.almis.awe.model.ProfileModel;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.dto.SortColumn;
import com.almis.awe.model.entities.email.ParsedEmail;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.data.builder.DataListBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.Thread.sleep;

/**
 * Dummy Service class to test the queries that call services
 *
 * @author jbellon
 */
@Service
@Slf4j
public class DummyService extends ServiceConfig {

  public static final String PROFILE_VALUE = "profileValue";
  public static final String PROFILE_NAME = "profileName";
  public static final String STAT_ROW = "stat-row";
  // Autowired services
  private final QueryService queryService;
  private final Random random = new Random();

  /**
   * Autowired constructor
   *
   * @param queryService    Query Service
   */
  public DummyService(QueryService queryService) {
    this.queryService = queryService;
  }

  /**
   * Returns a list of values to test pagination
   *
   * @return ServiceData service data
   * @throws AWException AWE exception
   */
  public ServiceData paginate() throws AWException {
    ServiceData out = new ServiceData();
    String[] data = new String[65];
    for (int i = 0; i < data.length; i++) {
      data[i] = String.valueOf(i);
    }

    DataListBuilder builder = getBean(DataListBuilder.class);
    out.setDataList(builder.setServiceQueryResult(data).build());

    return out;
  }

  /**
   * Returns a list of values to test pagination
   *
   * @return ServiceData ServiceData
   * @throws AWException AWE exception
   */
  public ServiceData paginate(Long page, Long max) throws AWException {
    ServiceData out = new ServiceData();
    String[] data = new String[65];
    for (int i = 0; i < data.length; i++) {
      data[i] = String.valueOf(i);
    }

    int offset = (int) ((page - 1) * max);
    List<String> subset = new ArrayList<>(Arrays.asList(data).subList(offset, (int) (offset + max)));

    DataListBuilder builder = getBean(DataListBuilder.class);
    out.setDataList(
      builder.setServiceQueryResult(subset.toArray(new String[0]))
        .setRecords((long) data.length)
        .setPage(page)
        .setMax(max)
        .build());

    return out;
  }

  /**
   * Returns a simple DataList without post processing
   *
   * @return DataList
   * @throws AWException
   */
  public ServiceData getDummyUnprocessedData() throws AWException {
    ServiceData out = new ServiceData();
    String[] data = new String[]{"Toyota", null, "Mercedes", null, "BMW", "Volkswagen", "Skoda"};

    DataListBuilder builder = getBean(DataListBuilder.class);
    DataList dataList = builder.setServiceQueryResult(data).build();
    DataListUtil.sort(dataList, Collections.singletonList(new SortColumn("value", "asc")), true);
    out.setDataList(dataList);

    return out;
  }

  /**
   * Returns a simple DataList asking for no parameters
   *
   * @return DataList
   * @throws AWException
   */
  public ServiceData returnDatalistNoParams() throws AWException {
    ServiceData out = new ServiceData();
    String[] data = new String[3];
    for (int i = 0; i < data.length; i++) {
      data[i] = String.valueOf(i);
    }

    DataListBuilder builder = getBean(DataListBuilder.class);
    out.setDataList(builder.setServiceQueryResult(data).build());

    return out;
  }

  /**
   * Returns a simple String[] asking for no parameters
   *
   * @return String[]
   */
  public ServiceData returnStringArrayNoParams() {
    ServiceData out = new ServiceData();
    out.setData(new String[]{"a", "b", "c"});

    return out;
  }

  /**
   * Returns a simple String[] asking for two parameters (string)
   *
   * @return String[]
   */
  public ServiceData returnStringArrayTwoStringParams(String name, List<String> fields) {
    ServiceData out = new ServiceData();
    out.setData(new String[]{name, fields.toString().replaceAll("[\\[\\]\\s]", "")});

    return out;
  }

  /**
   * Returns a simple String[] asking for a parameter (number)
   *
   * @return String[]
   */
  public ServiceData returnStringArrayNumberParam(Integer value) {
    ServiceData out = new ServiceData();
    out.setData(new String[]{String.valueOf(value)});

    return out;
  }

  /**
   * Returns a simple String[] asking for a parameter (number)
   *
   * @return String[]
   */
  public ServiceData returnStringArrayLongParam(Long value) {
    ServiceData out = new ServiceData();
    out.setData(new String[]{String.valueOf(value)});

    return out;
  }

  /**
   * Returns a simple String[] asking for a parameter (number)
   *
   * @return String[]
   */
  public ServiceData returnStringArrayDoubleParam(Double value) {
    ServiceData out = new ServiceData();
    out.setData(new String[]{String.valueOf(value)});
    return out;
  }

  /**
   * Returns a simple String[] asking for a parameter (number)
   *
   * @return String[]
   */
  public ServiceData returnStringArrayFloatParam(Float value) {
    ServiceData out = new ServiceData();
    out.setData(new String[]{String.valueOf(value)});

    return out;
  }

  /**
   * Returns a simple String[] asking for a parameter (boolean)
   *
   * @return String[]
   */
  public ServiceData returnStringArrayBooleanParam(Boolean value) {
    ServiceData out = new ServiceData();
    out.setData(new String[]{String.valueOf(value)});

    return out;
  }

  /**
   * Pretends to answer an OK from a maintain
   *
   * @return ServiceData
   */
  public ServiceData returnMaintainOkNoParams() {
    ServiceData out = new ServiceData();

    out.setType(AnswerType.OK);
    out.setTitle("Operation successful");
    out.setMessage("The selected maintain operation has been successfully performed");

    return out;
  }

  /**
   * Pretends to answer an OK from a maintain
   *
   * @return ServiceData
   */
  public ServiceData returnMaintainOkMessageParam(String message) {
    ServiceData out = new ServiceData();

    out.setType(AnswerType.OK);
    out.setTitle("Operation successful");
    out.setMessage(message);

    return out;
  }

  /**
   * Pretends to answer an OK from a maintain
   *
   * @return ServiceData
   */
  public ServiceData returnMaintainOkTitleMessageParam(String title, String message) {
    ServiceData out = new ServiceData();

    out.setType(AnswerType.OK);
    out.setTitle(title);
    out.setMessage(message);

    return out;
  }

  public ServiceData sendMail() {
    ServiceData out = new ServiceData();
    try {
      ParsedEmail email = new ParsedEmail()
        .setFrom(new InternetAddress("david.fuentes@almis.com"))
        .setTo(List.of(new InternetAddress("dfuentes.almis@gmail.com")))
        .setReplyTo(List.of(new InternetAddress("david.fuentes.other@almis.com")))
        .setCc(List.of(new InternetAddress("dovixman@gmail.com")))
        .setCco(List.of(new InternetAddress("dovixmancosas@gmail.com")))
        .setSubject("Test message")
        .setBody("<div style='background-color:red;'>Test div message</div>")
        .addAttachment("FileName.test", new File("tst.jpg"));
      getBean(EmailService.class).sendEmail(email);
    } catch (AddressException e) {
      e.printStackTrace();
    }
    return out;
  }

  /**
   * Returns the system date
   *
   * @return Service Data
   */
  public ServiceData getDate() {

    ServiceData serviceData = new ServiceData();
    try {
      // Generate date
      DateFormat df = new SimpleDateFormat("mm/dd/yyyy HH:mm:ss.S");
      Date date = df.parse("23/10/1978 15:06:23.232");

      // Get system version
      serviceData.setDataList(new DataList());
      DataListUtil.addColumnWithOneRow(serviceData.getDataList(), "value", date);
    } catch (Exception exc) {
      // Avoid exception
    }

    return serviceData;
  }

  /**
   * Returns the system date
   *
   * @return Service Data
   */
  public ServiceData getDateList() {

    ServiceData serviceData = new ServiceData();
    try {
      // Generate date
      List<Date> dates = new ArrayList<>();
      DateFormat df = new SimpleDateFormat("mm/dd/yyyy HH:mm:ss.S");
      dates.add(df.parse("23/10/1978 15:06:23.232"));
      dates.add(df.parse("11/02/2015 03:30:12.123"));
      dates.add(df.parse("01/08/2020 13:26:55.111"));

      // Get system version
      serviceData.setDataList(new DataList());
      DataListUtil.addColumn(serviceData.getDataList(), "date1", dates);
      DataListUtil.addColumn(serviceData.getDataList(), "date2", dates);
      DataListUtil.addColumn(serviceData.getDataList(), "date3", dates);
      DataListUtil.addColumn(serviceData.getDataList(), "date4", dates);
      DataListUtil.addColumn(serviceData.getDataList(), "date5", dates);
      DataListUtil.addColumn(serviceData.getDataList(), "date6", dates);
      DataListUtil.addColumn(serviceData.getDataList(), "date7", dates);
    } catch (Exception exc) {
      // Avoid exception
    }

    return serviceData;
  }

  /**
   * Returns the system date
   *
   * @return Service Data
   */
  public ServiceData getElapsedTimeList() {

    ServiceData serviceData = new ServiceData();

    // Get system version
    serviceData.setDataList(new DataList());
    DataListUtil.addColumn(serviceData.getDataList(), "ms1", Collections.singletonList(1200 * 24 * 60 * 60 * 1000L));
    DataListUtil.addColumn(serviceData.getDataList(), "ms2", Collections.singletonList(400 * 24 * 60 * 60 * 1000L));
    DataListUtil.addColumn(serviceData.getDataList(), "ms3", Collections.singletonList(70 * 24 * 60 * 60 * 1000L));
    DataListUtil.addColumn(serviceData.getDataList(), "ms4", Collections.singletonList(35 * 24 * 60 * 60 * 1000L));
    DataListUtil.addColumn(serviceData.getDataList(), "ms5", Collections.singletonList(18 * 24 * 60 * 60 * 1000L));
    DataListUtil.addColumn(serviceData.getDataList(), "ms6", Collections.singletonList(9 * 24 * 60 * 60 * 1000L));
    DataListUtil.addColumn(serviceData.getDataList(), "ms7", Collections.singletonList(3 * 24 * 60 * 60 * 1000L));
    DataListUtil.addColumn(serviceData.getDataList(), "ms8", Collections.singletonList(8 * 60 * 60 * 1000L));
    DataListUtil.addColumn(serviceData.getDataList(), "ms9", Collections.singletonList(5 * 60 * 1000L));
    DataListUtil.addColumn(serviceData.getDataList(), "ms10", Collections.singletonList(7 * 1000L));
    DataListUtil.addColumn(serviceData.getDataList(), "ms11", Collections.singletonList(222L));

    // Get calendar 3 years ago
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    calendar.add(Calendar.YEAR, -3);
    calendar.add(Calendar.MONTH, -1);
    DataListUtil.addColumn(serviceData.getDataList(), "dateSince", Collections.singletonList(calendar.getTime()));

    return serviceData;
  }

  /**
   * Wait some seconds and retrieve a screen data
   *
   * @return
   */
  public ServiceData waitSomeSeconds(Integer seconds) throws AWException {
    try {
      int secondsToWait = random.nextInt(4) - 2 + seconds;
      logger.info("Waiting {} seconds", secondsToWait);
      sleep(secondsToWait * 1000L);
      logger.info("Waiting finished!");
    } catch (Exception exc) {
      Thread.currentThread().interrupt();
      throw new AWException("Interrupted thread exception", exc);
    }
    return new ServiceData();
  }

  /**
   * Do nothing
   *
   * @return
   */
  public ServiceData doNothing() {
    logger.info("Launching a test service");
    return new ServiceData();
  }

  /**
   * Fill suggest multiple with data
   *
   * @return
   */
  public ServiceData testSuggestMultiple() {
    logger.info("Launching a suggest multiple select action");
    return new ServiceData().addClientAction(new SelectActionBuilder("SugMul", Arrays.asList("test", "pei")).build());
  }

  /**
   * Retrieve dummy data
   *
   * @param planet Planet bean
   * @return Service data
   */
  public ServiceData getDummyData(Planet planet) {
    return new ServiceData().setDataList(DataListUtil.fromBeanList(List.of(planet)));
  }

  /**
   * Retrieve dummy data
   *
   * @param planet Planet bean
   * @return Service data
   */
  public ServiceData getDummyData(JsonNode planet) {
    return new ServiceData()
      .setTitle("tutu")
      .setMessage("lala");
  }

  /**
   * Retrieve dummy data
   *
   * @param planetList Planet bean list
   * @return Service data
   */
  public ServiceData getDummyData(List<Planet> planetList) {
    return new ServiceData().setDataList(DataListUtil.fromBeanList(planetList));
  }

  /**
   * Retrieve dummy data
   *
   * @param planets Planets bean
   * @return Service data
   */
  public ServiceData getDummyData(Planets planets) {
    return new ServiceData().setDataList(DataListUtil.fromBeanList(List.of(planets)));
  }

  /**
   * Retrieve service data for computed
   *
   * @return Service Data
   */
  public ServiceData testGetServiceDataForComputed() {
    DataList dataList = new DataList();

    DataListUtil.addColumnWithOneRow(dataList, "integer", 1);
    DataListUtil.addColumnWithOneRow(dataList, "text", "text");
    DataListUtil.addColumnWithOneRow(dataList, "empty", null);
    DataListUtil.addColumnWithOneRow(dataList, "float", 1.2f);
    DataListUtil.addColumnWithOneRow(dataList, "double", 1.2d);
    DataListUtil.addColumnWithOneRow(dataList, "zero", 0);

    return new ServiceData().setDataList(dataList);
  }

  /**
   * Dynamic screen generation
   *
   * @return Service data with dynamic screen
   */
  public ServiceData dynamicScreen() throws AWException {
    // Get profile list
    ServiceData serviceData = queryService.launchPrivateQuery("getProfiles");
    List<ProfileModel> profileModels = DataListUtil.asBeanList(serviceData.getDataList(), ProfileModel.class);
    TagBuilder profileModelCards = new TagBuilder()
      .setType("div")
      .setStyle("row");
    for (ProfileModel profileModel : profileModels) {
      TagBuilder statPanel = generateStatPanel(profileModel.getName(), profileModel.getValue().toString(), true);
      ((TagBuilder) statPanel.getElementList().get(0))
        .addTag(new TagBuilder()
          .setType("div")
          .setStyle(STAT_ROW)
          .addTag(new TagBuilder()
            .setType("div")
            .setStyle("stat-cell bordered no-border-t no-padding-hr")
            .addButton(new ButtonBuilder()
              .setId("Button" + profileModel.getValue().toString())
              .setLabel("BUTTON_VIEW")
              .addButtonAction(new ButtonActionBuilder()
                .setType(Action.VALUE)
                .setTarget(PROFILE_NAME)
                .setValue(profileModel.getName()))
              .addButtonAction(new ButtonActionBuilder()
                .setType(Action.VALUE)
                .setTarget(PROFILE_VALUE)
                .setValue(profileModel.getValue().toString()))
              .addButtonAction(new ButtonActionBuilder()
                .setType(Action.SCREEN)
                .setTarget("dynamic-subscreen"))
            )
          )
        );
      profileModelCards.addTag(statPanel);
    }

    // Generate screen with profile list
    return new ServiceData().setData(new ScreenBuilder()
      .setTemplate("window")
      .setLabel("MENU_TEST_DYNAMIC_SCREEN")
      .addTag(new TagBuilder()
        .setSource("center")
        .addCriteria(new HiddenCriteriaBuilder().setId(PROFILE_NAME))
        .addCriteria(new HiddenCriteriaBuilder().setId(PROFILE_VALUE))
        .addTag(profileModelCards)
      )
      .build());
  }

  /**
   * Dynamic screen generation
   *
   * @return Service data with dynamic screen
   */
  public ServiceData dynamicSubScreen() throws AWException {
    // Get profile list
    ProfileModel profileModel = new ProfileModel()
      .setName(getRequest().getParameterAsString(PROFILE_NAME))
      .setValue(Integer.parseInt(getRequest().getParameterAsString(PROFILE_VALUE)));

    // Generate screen with profile list
    return new ServiceData().setData(new ScreenBuilder()
      .setTemplate("window")
      .setLabel("MENU_TEST_DYNAMIC_SUB_SCREEN")
      .addTag(new TagBuilder()
        .setSource("buttons")
        .addButton(new ButtonBuilder()
          .setId("ButtonBack")
          .setLabel("BUTTON_BACK")
          .addButtonAction(new ButtonActionBuilder()
            .setType(Action.BACK))
        )
      )
      .addTag(new TagBuilder()
        .setSource("center")
        .addTag(new TagBuilder()
          .setType("div")
          .setStyle("row")
          .addTag(generateStatPanel(profileModel.getName(), profileModel.getValue().toString(), false))
        )
      ).build());
  }

  private TagBuilder generateStatPanel(String title, String value, boolean more) {
    return new TagBuilder()
      .setType("div")
      .setStyle("col-xs-2")
      .addTag(new TagBuilder()
        .setType("div")
        .setStyle("stat-panel text-center")
        .addTag(new TagBuilder()
          .setType("div")
          .setStyle(STAT_ROW)
          .addTag(new TagBuilder()
            .setType("div")
            .setStyle("stat-cell bg-success padding-sm text-xs text-semibold")
            .setText(title)
          )
        )
        .addTag(new TagBuilder()
          .setType("div")
          .setStyle(STAT_ROW)
          .addTag(new TagBuilder()
            .setType("div")
            .setStyle("stat-cell bordered no-border-t no-padding-hr" + (more ? " no-border-b" : ""))
            .setText(value)
          ))
      );
  }

  public ServiceData testWithDates(Date date, Date time, Date timestamp, Date systemDate, Date systemTime, Date systemTimestamp) throws Exception {
    if (date == null || time == null || timestamp == null || systemDate == null || systemTime == null || systemTimestamp == null) {
      throw new RuntimeException();
    }
    return new ServiceData();
  }
}
