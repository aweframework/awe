package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.chart.Chart;
import com.almis.awe.model.entities.screen.component.chart.ChartParameter;
import com.almis.awe.model.entities.screen.component.chart.ChartSerie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public class ChartService extends ServiceConfig {

  private static final String MAIN_DATASOURCE = "main";
  private static final String DATASOURCE_PARAMETER = "datasource";
  private static final String AWE_HINTS = "awe";

  // Autowired parameters
  private final ObjectMapper mapper;
  private final BaseConfigProperties baseConfigProperties;

  /**
   * Autowired constructor
   *
   * @param mapper               Object mapper
   * @param baseConfigProperties Base configuration properties
   */
  public ChartService(ObjectMapper mapper, BaseConfigProperties baseConfigProperties) {
    this.mapper = mapper;
    this.baseConfigProperties = baseConfigProperties;
  }

  /**
   * Render chart with the AWE chart server (ECharts)
   *
   * @param screenName Screen name where chart is
   * @param chartName  Chart identifier
   * @param data       Data to fill the chart
   * @return SVG image as string
   * @throws AWException Error rendering chart
   */
  public String renderChart(String screenName, String chartName, DataList data) throws AWException {
    return renderChart(getChart(screenName, chartName), data);
  }

  /**
   * Render chart with the AWE chart server (ECharts)
   *
   * @param screenName  Screen name where chart is
   * @param chartName   Chart identifier
   * @param datasourceMap Data sources map
   * @return SVG image as string
   * @throws AWException Error rendering chart
   */
  public String renderChart(String screenName, String chartName, Map<String, DataList> datasourceMap) throws AWException {
    return renderChart(getChart(screenName, chartName), datasourceMap);
  }

  /**
   * Render chart with the AWE chart server (ECharts)
   *
   * @param chart Chart
   * @param data  Data to fill the chart
   * @return SVG image as string
   * @throws AWException Error rendering chart
   */
  public String renderChart(Chart chart, DataList data) throws AWException {
    Map<String, DataList> datasourceMap = new HashMap<>();
    datasourceMap.put(MAIN_DATASOURCE, data);
    return renderChart(chart, datasourceMap);
  }

  /**
   * Render chart with the AWE chart server (ECharts)
   *
   * @param chart       Chart
   * @param datasourceMap Data sources map
   * @return SVG image as string
   * @throws AWException Error rendering chart
   */
  public String renderChart(Chart chart, Map<String, DataList> datasourceMap) throws AWException {
    return renderChartWithDatasourceMap(chart, datasourceMap);
  }

  /**
   * Retrieve a chart from a screen and a name
   *
   * @param screenName Screen name
   * @param chartName  Chart name
   * @return Found chart or null
   */
  public Chart getChart(String screenName, String chartName) throws AWException {
    Screen screen = getElements().getScreen(screenName).copy();
    return (Chart) screen.getElementsById(chartName).stream().findFirst().orElse(null);
  }

  /**
   * Render chart with the AWE chart server (ECharts)
   *
   * @param chart       Chart
   * @param datasourceMap Data sources map
   * @return SVG image as string
   * @throws AWException Error rendering chart
   */
  private String renderChartWithDatasourceMap(Chart chart, Map<String, DataList> datasourceMap) throws AWException {
    String response = null;
    if (chart != null) {
      response = generateChartInServer(buildChartRequest(chart, datasourceMap));
    }
    return response;
  }

  /**
   * Build the request of the chart server: the ECharts model of the chart, the rows of the main datasource, the
   * language and the translations of the locale keys of the model. A series that reads another datasource carries
   * its own rows in its client hints ({@code awe.values}). A datasource that is not in the map has no rows
   *
   * @param chart         Chart
   * @param datasourceMap Data sources map
   * @return Chart request
   */
  private Map<String, Object> buildChartRequest(Chart chart, Map<String, DataList> datasourceMap) {
    Map<String, Object> model = chart.getEchartsModel();
    String language = getElements().getLanguage();

    // Translations are taken before adding the rows, so only the strings of the model are translated
    Map<String, String> translations = new LinkedHashMap<>();
    collectTranslations(model, language, translations);
    addSeriesValues(model, chart, datasourceMap);

    Map<String, Object> chartData = new HashMap<>();
    chartData.put("type", "image/svg+xml");
    chartData.put("infile", model);
    chartData.put("values", getRows(datasourceMap, MAIN_DATASOURCE));
    chartData.put("language", language);
    chartData.put("translations", translations);
    return chartData;
  }

  /**
   * Call server and retrieve chart data
   *
   * @param chartData Chart request
   * @return Chart in server
   */
  private String generateChartInServer(Map<String, Object> chartData) throws AWException {
    RestTemplate restTemplate = buildChartRestTemplate(getBean(RestTemplate.class));

    // Generate request
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String url = baseConfigProperties.getChartServerUrl();
    // The request carries the chart data, so it is only logged on debug
    if (log.isDebugEnabled()) {
      log.debug("Generating chart in {} with data: {}", url, mapper.valueToTree(chartData));
    }
    ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, new HttpEntity<>(chartData, headers), String.class);

    // Handle response status
    if (!responseEntity.getStatusCode().is2xxSuccessful()) {
      throw new AWException(String.format("Error retrieving chart from server %d", responseEntity.getStatusCode().value()));
    }

    return responseEntity.getBody();
  }

  /**
   * Build a request-local rest template for chart rendering without mutating the shared bean
   *
   * @param sharedRestTemplate Shared rest template bean
   * @return Chart rendering rest template
   */
  private RestTemplate buildChartRestTemplate(RestTemplate sharedRestTemplate) {
    RestTemplate chartRestTemplate = new RestTemplate(sharedRestTemplate.getRequestFactory());
    chartRestTemplate.setUriTemplateHandler(sharedRestTemplate.getUriTemplateHandler());
    chartRestTemplate.setErrorHandler(sharedRestTemplate.getErrorHandler());

    List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
    messageConverters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
    messageConverters.addAll(sharedRestTemplate.getMessageConverters());
    chartRestTemplate.setMessageConverters(messageConverters);

    return chartRestTemplate;
  }

  /**
   * Add the rows of the series that read another datasource (series chart-parameter {@code datasource}) to their
   * client hints, as {@code awe.values}. The series of the main datasource use the {@code values} of the request
   *
   * @param model         ECharts model
   * @param chart         Chart
   * @param datasourceMap Data sources map
   */
  private void addSeriesValues(Map<String, Object> model, Chart chart, Map<String, DataList> datasourceMap) {
    Map<String, String> serieDatasources = new HashMap<>();
    for (ChartSerie serie : chart.getSerieList()) {
      String datasource = getDatasource(serie);
      if (serie.getId() != null && !MAIN_DATASOURCE.equals(datasource)) {
        serieDatasources.put(serie.getId(), datasource);
      }
    }
    if (serieDatasources.isEmpty()) {
      return;
    }

    // Series of the chart and drilldown series (in the hints of the model)
    List<Object> seriesList = new ArrayList<>(asList(model.get("series")));
    if (model.get(AWE_HINTS) instanceof Map<?, ?> hints && hints.get("drilldown") instanceof Map<?, ?> drilldown) {
      seriesList.addAll(asList(drilldown.get("series")));
    }
    for (Object item : seriesList) {
      if (item instanceof Map<?, ?> serieModel && serieDatasources.containsKey(serieModel.get("id"))) {
        hintsOf(castMap(serieModel)).put("values", getRows(datasourceMap, serieDatasources.get(serieModel.get("id"))));
      }
    }
  }

  /**
   * Retrieve the datasource of a series (chart-parameter {@code datasource}, main by default)
   *
   * @param serie Series
   * @return Datasource name
   */
  private String getDatasource(ChartSerie serie) {
    return Optional.ofNullable(serie.getParameterList()).orElse(Collections.emptyList()).stream()
      .filter(parameter -> DATASOURCE_PARAMETER.equals(parameter.getName()))
      .map(ChartParameter::getValue)
      .findFirst()
      .orElse(MAIN_DATASOURCE);
  }

  /**
   * Retrieve the rows of a datasource, serialized as the client receives them
   *
   * @param datasourceMap Data sources map
   * @param datasource    Datasource name
   * @return Rows of the datasource, empty when the datasource is not in the map
   */
  private List<Map<String, CellData>> getRows(Map<String, DataList> datasourceMap, String datasource) {
    DataList data = datasourceMap.get(datasource);
    return data == null || data.getRows() == null ? Collections.emptyList() : data.getRows();
  }

  /**
   * Collect the translation of the words of all the strings of the model (as the client translates the labels,
   * word by word). Only the words that have a translation are collected
   *
   * @param node         Model node
   * @param language     Language
   * @param translations Translations found
   */
  private void collectTranslations(Object node, String language, Map<String, String> translations) {
    if (node instanceof Map<?, ?> map) {
      map.values().forEach(value -> collectTranslations(value, language, translations));
    } else if (node instanceof Collection<?> collection) {
      collection.forEach(value -> collectTranslations(value, language, translations));
    } else if (node instanceof String text) {
      Arrays.stream(text.split(" "))
        .filter(word -> !word.isEmpty() && !translations.containsKey(word))
        .forEach(word -> {
          String translation = getElements().getLocaleWithLanguage(word, language);
          if (translation != null && !translation.equals(word)) {
            translations.put(word, translation);
          }
        });
    }
  }

  /**
   * Retrieve the client hints of a series model, creating them when needed
   *
   * @param serieModel Series model
   * @return Client hints
   */
  private Map<String, Object> hintsOf(Map<String, Object> serieModel) {
    return castMap(serieModel.computeIfAbsent(AWE_HINTS, key -> new LinkedHashMap<String, Object>()));
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> castMap(Object value) {
    return (Map<String, Object>) value;
  }

  private static List<?> asList(Object value) {
    return value instanceof List<?> list ? list : Collections.emptyList();
  }
}
