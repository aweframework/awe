package com.almis.awe.test.service;

import com.almis.awe.builder.enumerates.Action;
import com.almis.awe.builder.screen.ScreenBuilder;
import com.almis.awe.builder.screen.TagBuilder;
import com.almis.awe.builder.screen.button.ButtonActionBuilder;
import com.almis.awe.builder.screen.button.ButtonBuilder;
import com.almis.awe.builder.screen.criteria.HiddenCriteriaBuilder;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.dto.SortColumn;
import com.almis.awe.model.entities.email.ParsedEmail;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.EmailService;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.data.builder.DataListBuilder;
import com.almis.awe.test.bean.Planet;
import com.almis.awe.test.bean.Planets;
import com.almis.awe.test.model.ProfileModel;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

/**
 * Dummy Service class to test the queries that call services
 *
 * @author jbellon
 */
@Slf4j
@Service
public class DummyService extends ServiceConfig {

  public static final String PROFILE_VALUE = "profileValue";
  public static final String PROFILE_NAME = "profileName";

  // Autowired services
  private WebApplicationContext context;
  private Random random = new Random();
  private final QueryService queryService;

  /**
   * Autowired constructor
   *
   * @param context Context
   */
  @Autowired
  public DummyService(WebApplicationContext context, QueryService queryService) {
    this.context = context;
    this.queryService = queryService;
  }

  /**
   * Returns a list of values to test pagination
   *
   * @return
   * @throws AWException
   */
  public ServiceData paginate() throws AWException {
    ServiceData out = new ServiceData();
    String[] data = new String[65];
    for (int i = 0; i < data.length; i++) {
      data[i] = i + "";
    }

    DataListBuilder builder = context.getBean(DataListBuilder.class);
    out.setDataList(builder.setServiceQueryResult(data).build());

    return out;
  }

  /**
   * Returns a list of values to test pagination
   *
   * @return
   * @throws AWException
   */
  public ServiceData paginate(Long page, Long max) throws AWException {
    ServiceData out = new ServiceData();
    String[] data = new String[65];
    for (int i = 0; i < data.length; i++) {
      data[i] = i + "";
    }

    int offset = (int) ((page - 1) * max);
    List<String> subset = new ArrayList<>(Arrays.asList(data).subList(offset, (int) (offset + max)));

    DataListBuilder builder = context.getBean(DataListBuilder.class);
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

    DataListBuilder builder = context.getBean(DataListBuilder.class);
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
      data[i] = i + "";
    }

    DataListBuilder builder = context.getBean(DataListBuilder.class);
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
        .setTo(Arrays.asList(new InternetAddress("dfuentes.almis@gmail.com")))
        .setReplyTo(Arrays.asList(new InternetAddress("david.fuentes.other@almis.com")))
        .setCc(Arrays.asList(new InternetAddress("dovixman@gmail.com")))
        .setCco(Arrays.asList(new InternetAddress("dovixmancosas@gmail.com")))
        .setSubject("Test message")
        .setBody("<div style='background-color:red;'>Test div message</div>")
        .addAttachment("FileName.test", new File("C:\\Users\\dfuentes\\Pictures\\Saved Pictures\\tst.jpg"));
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
      // Non useful
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
      // Non useful
    }

    return serviceData;
  }

  /**
   * Retrieve dummy data
   *
   * @param planet Planet bean
   * @return Service data
   */
  public ServiceData getDummyData(Planet planet) {
    return new ServiceData();
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
    return new ServiceData();
  }

  /**
   * Retrieve dummy data
   *
   * @param planets Planets bean
   * @return Service data
   */
  public ServiceData getDummyData(Planets planets) {
    return new ServiceData();
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
      .setStyle("flex align-items-center justify-content-center pt-2 gap-2");
    for (ProfileModel profileModel : profileModels) {
      TagBuilder statPanel = generateStatPanel(profileModel.getName(), profileModel.getValue().toString(), new TagBuilder()
        .setType("div")
        .setStyle("p-card-footer")
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
          .setStyle("flex align-items-center justify-content-center pt-2 gap-2")
          .addTag(generateStatPanel(profileModel.getName(), profileModel.getValue().toString(), new TagBuilder()))
        )
      ).build());
  }

  private TagBuilder generateStatPanel(String title, String value, TagBuilder footer) {
    return new TagBuilder()
      .setType("div")
      .setStyle("w-2")
      .addTag(new TagBuilder()
        .setType("div")
        .setStyle("p-card")
        .addTag(new TagBuilder()
          .setType("div")
          .setStyle("p-card-body")
          .addTag(new TagBuilder()
            .setType("div")
            .setStyle("p-card-title")
            .setText(title),
            new TagBuilder()
              .setType("div")
              .setStyle("p-card-content")
              .setText(value),
            footer
          )
        )
      );
  }

  /**
   * @return ServiceData
   *
   * @throws NoSuchAlgorithmException NoSuchAlgorithmException
   * @author Santiago ROZAS
   * <p> * fill line chart with dummy data
   */
  public ServiceData dummyLineService(String years) throws NoSuchAlgorithmException {
    ServiceData serviceData = new ServiceData();
    DataList dataList = new DataList();

    // Parámetros
    double mu = 0.05;
    double sigma = 0.5;
    double dt = 1d;

    // Calculamos el primer día del gráfico en función de la selección
    Date firstDay;
    if (years == null)
      years = "currentYear";

    if (NumberUtils.isNumber(years)) {
      firstDay = DateUtils.addYears(new Date(), -NumberUtils.stringToInt(years));
    } else if (years.equals("currentYear")) {
      firstDay = DateUtils.truncate(new Date(), Calendar.YEAR);
    } else {
      firstDay = DateUtils.addYears(new Date(), -12);
    }

    long diff = new Date().getTime() - firstDay.getTime();
    int n = (int) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    // Arrays para almacenar el tiempo y los valores de la serie
    Date[] dateSeries = new Date[n + 1];
    Double[] values = new Double[n + 1];
    Double[] amounts = new Double[n + 1];
    values[0] = 0.0;  dateSeries[0] = firstDay;
    amounts[0] = 250000d;
    Random rand = SecureRandom.getInstanceStrong();

    for (int i = 1; i <= n; i++) {
      double dW = rand.nextGaussian() * Math.sqrt(dt);
      values[i] = values[i - 1] + mu * dt + sigma * dW;
      dateSeries[i] = DateUtils.addDays(dateSeries[i - 1], (int) dt);
      amounts[i] = amounts[0]*(1d+values[i]/100d);
    }

    DataListUtil.addColumn(dataList, "dates", Arrays.asList(dateSeries));
    DataListUtil.addColumn(dataList, "profitability", Arrays.asList(values));
    DataListUtil.addColumn(dataList, "amount", List.of(amounts));
    return serviceData.setDataList(dataList);
  }
}
