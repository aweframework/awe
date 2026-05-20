package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.Tag;
import com.almis.awe.model.entities.screen.component.chart.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class ChartServiceTest {

  @InjectMocks
  private ChartService chartService;

  @Mock
  private ApplicationContext context;

  @Mock
  private AweElements aweElements;

  private RestTemplate restTemplate;

  private MockRestServiceServer mockServer;

  @Mock
  private BaseConfigProperties baseConfigProperties;

  @Mock
  private ObjectMapper mapper;

  private final Screen testScreen = new Screen()
    .addElement(new Tag()
      .addElement(new Chart()
        .setXAxisList(Collections.singletonList((ChartAxis) new ChartAxis().setTitle("Fechas").setType("datetime")))
        .setYAxisList(Arrays.asList((ChartAxis) new ChartAxis().setLabel("Temperaturas (ºC)"), (ChartAxis) new ChartAxis().setOpposite(true).setLabel("Lluvias (mm)")))
        .setSerieList(Arrays.asList((ChartSerie) new ChartSerie()
            .setYAxis("0")
            .setXValue("dates")
            .setYValue("serie1")
            .setColor("#A8E0A6")
            .setType("column")
            .setId("serie-1"),
          (ChartSerie) new ChartSerie()
            .setYAxis("1")
            .setXValue("dates")
            .setYValue("serie2")
            .setZValue("serie3")
            .setType("spline")
            .setId("serie-2")
        ))
        .setId("LineChartTest")
        .setType("mixed")
      )
    )
    .addElement(new Chart()
      .setChartTooltip(new ChartTooltip().setSuffix("%"))
      .setSerieList(Arrays.asList(
        (ChartSerie) new ChartSerie()
          .setXValue("category")
          .setYValue("serie1")
          .setParameterList(Arrays.asList(
            (ChartParameter) new ChartParameter().setName("size").setValue("51%").setType("string"),
            (ChartParameter) ((ChartParameter) new ChartParameter().setName("dataLabels").setType("object"))
              .setParameterList(Arrays.asList(
                (ChartParameter) new ChartParameter().setName("distance").setValue("-30").setType("integer"),
                (ChartParameter) new ChartParameter().setName("color").setValue("#ffffff").setType("string"),
                (ChartParameter) new ChartParameter().setName("format").setValue("<b>{point.name}</b>").setType("string")
              ))
          ))
          .setId("main"),
        (ChartSerie) new ChartSerie()
          .setXValue("category")
          .setYValue("subserie1")
          .setParameterList(Arrays.asList(
            (ChartParameter) new ChartParameter().setName("datasource").setValue("detail").setType("string"),
            (ChartParameter) new ChartParameter().setName("size").setValue("80%").setType("string"),
            (ChartParameter) new ChartParameter().setName("innerSize").setValue("60%").setType("string")
          ))
          .setId("detail")
      ))
      .setParameterList(Collections.singletonList(
        (ChartParameter) new ChartParameter().setName("plotOptions")
          .setParameterList(Collections.singletonList(
            (ChartParameter) new ChartParameter().setName("pie")
              .setParameterList(Arrays.asList(
                (ChartParameter) new ChartParameter().setName("size").setValue("75%").setType("string"),
                (ChartParameter) new ChartParameter().setName("shadow").setValue("false").setType("boolean"),
                (ChartParameter) new ChartParameter().setName("center")
                  .setParameterList(Arrays.asList(
                    (ChartParameter) new ChartParameter().setValue("50%").setType("string"),
                    (ChartParameter) new ChartParameter().setValue("50%").setType("string")
                  ))
                  .setType("array")
              ))
              .setType("object")
          ))
          .setType("object")

      ))
      .setId("DrillDownTest")
      .setType("pie")
    );

  /**
   * Initializes json mapper for tests
   */
  @BeforeEach
  public void initBeans() throws Exception {
    restTemplate = new RestTemplate();
    restTemplate.setErrorHandler(new org.springframework.web.client.ResponseErrorHandler() {
      @Override
      public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
        return false;
      }

      @Override
      public void handleError(java.net.URI url, org.springframework.http.HttpMethod method,
                              org.springframework.http.client.ClientHttpResponse response) {
        // Allow tests to assert on non-2xx responses through ChartService
      }
    });
    mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();

    chartService.setApplicationContext(context);
    doReturn(aweElements).when(context).getBean(AweElements.class);
    lenient().when(context.getBean(RestTemplate.class)).thenReturn(restTemplate);
    lenient().when(baseConfigProperties.getHighchartsServerUrl()).thenReturn("https://export.highcharts.com");
    given(aweElements.getScreen(anyString())).willReturn(testScreen);
  }

  @Test
  void renderChart() throws Exception {
    mockServer.expect(requestTo("https://export.highcharts.com"))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withSuccess("Hello World", MediaType.valueOf("image/svg+xml")));

    assertEquals("Hello World", chartService.renderChart("Chart", "LineChartTest", buildLineChartData()));
    mockServer.verify();
  }

  @Test
  void renderChartWithDetails() throws AWException {
    mockServer.expect(requestTo("https://export.highcharts.com"))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withSuccess("Hello World", MediaType.valueOf("image/svg+xml")));

    DataList data = dataList(
      row("id", 1, "category", "asphalt", "serie1", 3),
      row("id", 2, "category", "clean", "serie1", 5),
      row("id", 3, "category", "default", "serie1", 2)
    );

    DataList detail = dataList(
      row("id", 1, "parent", "asphalt", "category", "asphalt1", "subserie1", 1f),
      row("id", 2, "parent", "asphalt", "category", "asphalt2", "subserie1", 0.5f),
      row("id", 3, "parent", "asphalt", "category", "asphalt3", "subserie1", 1.5f),
      row("id", 4, "parent", "clean", "category", "Don Limpio", "subserie1", 4f),
      row("id", 5, "parent", "clean", "category", "Mr Proper", "subserie1", 1f),
      row("id", 6, "parent", "default", "category", "Pépè", "subserie1", 0.2f),
      row("id", 7, "parent", "default", "category", "Blue", "subserie1", 0.9f),
      row("id", 8, "parent", "default", "category", "Vegeta666", "subserie1", 0.4f),
      row("id", 9, "parent", "default", "category", "El Rubius", "subserie1", 0.5f)
    );

    Map<String, DataList> datasources = Map.of(
      "main", data,
      "detail", detail
    );

    assertEquals("Hello World", chartService.renderChart("Chart", "DrillDownTest", datasources));
    mockServer.verify();
  }

  @Test
  void renderChartNotFound() throws Exception {
    assertNull(chartService.renderChart("Chart", "NotFoundChart", new DataList()));
  }

  @Test
  void renderChartNot200() {
    mockServer.expect(requestTo("https://export.highcharts.com"))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withStatus(HttpStatus.NOT_FOUND));

    assertThrows(AWException.class, () -> chartService.renderChart("Chart", "LineChartTest", new DataList()));
    mockServer.verify();
  }

  @Test
  void renderChartDoesNotMutateSharedMessageConvertersOnRepeatedCalls() throws AWException {
    int initialConverterCount = restTemplate.getMessageConverters().size();
    long initialStringConverterCount = restTemplate.getMessageConverters().stream()
      .filter(StringHttpMessageConverter.class::isInstance)
      .count();

    mockServer.expect(ExpectedCount.times(2), requestTo("https://export.highcharts.com"))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withSuccess("Hello World", MediaType.valueOf("image/svg+xml")));

    assertEquals("Hello World", chartService.renderChart("Chart", "LineChartTest", buildLineChartData()));
    assertEquals("Hello World", chartService.renderChart("Chart", "LineChartTest", buildLineChartData()));

    assertEquals(initialConverterCount, restTemplate.getMessageConverters().size());
    assertEquals(initialStringConverterCount, restTemplate.getMessageConverters().stream()
      .filter(StringHttpMessageConverter.class::isInstance)
      .count());
    mockServer.verify();
  }

  @Test
  void renderChartUsesSharedInterceptorsOnlyOncePerRequest() throws AWException {
    AtomicInteger interceptorCalls = new AtomicInteger();
    restTemplate.getInterceptors().add((request, body, execution) -> {
      interceptorCalls.incrementAndGet();
      return execution.execute(request, body);
    });

    mockServer.expect(ExpectedCount.times(2), requestTo("https://export.highcharts.com"))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withSuccess("Hello World", MediaType.valueOf("image/svg+xml")));

    assertEquals("Hello World", chartService.renderChart("Chart", "LineChartTest", buildLineChartData()));
    assertEquals("Hello World", chartService.renderChart("Chart", "LineChartTest", buildLineChartData()));

    assertEquals(2, interceptorCalls.get());
    mockServer.verify();
  }

  private DataList buildLineChartData() {
    return dataList(
      row("dates", 1366927200000L, "id", 1, "serie1", 10, "serie2", 6, "serie3", 0),
      row("dates", 1367013600000L, "id", 2, "serie1", 5, "serie2", 7, "serie3", -2),
      row("dates", 1367186400000L, "id", 3, "serie1", 10, "serie2", 6, "serie3", 10)
    );
  }

  @SafeVarargs
  private final DataList dataList(Map<String, CellData>... rows) {
    DataList dataList = new DataList();
    Arrays.stream(rows).forEach(dataList::addRow);
    return dataList;
  }

  private Map<String, CellData> row(Object... values) {
    Map<String, CellData> row = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      row.put((String) values[i], new CellData(values[i + 1]));
    }
    return row;
  }
}