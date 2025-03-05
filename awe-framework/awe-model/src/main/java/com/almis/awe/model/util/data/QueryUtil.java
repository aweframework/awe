package com.almis.awe.model.util.data;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.almis.awe.model.component.RequestDataHolder;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.dto.SortColumn;
import com.almis.awe.model.entities.maintain.MaintainQuery;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queries.Variable;
import com.almis.awe.model.type.ParameterType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;

/**
 * Abstract query builder
 */
@Slf4j
public class QueryUtil extends ServiceConfig {

  // Autowired services
  private final BaseConfigProperties baseConfigProperties;
  private final DatabaseConfigProperties databaseConfigProperties;
  private final ObjectMapper mapper;
  private final PrototypeRequestBeanHolder prototypeRequestBeanHolder;
  /**
   * QueryUtil constructor
   *
   * @param baseConfigProperties      Base config properties
   * @param databaseConfigProperties  Database config properties
   * @param mapper                    Object mapper
   */
  public QueryUtil(BaseConfigProperties baseConfigProperties, DatabaseConfigProperties databaseConfigProperties, ObjectMapper mapper, PrototypeRequestBeanHolder prototypeRequestBeanHolder) {
    this.baseConfigProperties = baseConfigProperties;
    this.databaseConfigProperties = databaseConfigProperties;
    this.mapper = mapper;
    this.prototypeRequestBeanHolder = prototypeRequestBeanHolder;
  }

  /**
   * Generate sort list
   *
   * @param sortArrayNode Sort parameter
   * @return Query parameter map
   */
  public List<SortColumn> getSortList(ArrayNode sortArrayNode) {
    List<SortColumn> sortList = new ArrayList<>();

    // If there are sort nodes, generate sortList
    for (JsonNode sortNode : sortArrayNode) {
      sortList.add(new SortColumn((ObjectNode) sortNode));
    }

    return sortList;
  }

  /**
   * Prepare query variables if not defined previously
   *
   * @param parameters Parameter list
   * @return Query parameter map
   */
  public Map<String, QueryParameter> getDefaultVariableMap(ObjectNode parameters) {
    Map<String, QueryParameter> variableMap = new HashMap<>();

    // Add sort variable
    JsonNode sortParameter = getRequestParameter(AweConstants.COMPONENT_SORT, parameters);
    if (sortParameter != null && sortParameter.isArray()) {
      ArrayNode sortList = (ArrayNode) sortParameter;
      variableMap.put(AweConstants.QUERY_SORT, new QueryParameter(sortList, true, ParameterType.OBJECT));
    }

    // Add page variable
    JsonNode pageParameter = getRequestParameter(AweConstants.COMPONENT_PAGE, parameters);
    if (pageParameter != null) {
      variableMap.put(AweConstants.QUERY_PAGE, new QueryParameter(pageParameter, false, ParameterType.LONG));
    } else {
      variableMap.put(AweConstants.QUERY_PAGE, new QueryParameter(JsonNodeFactory.instance.numberNode(1), false, ParameterType.LONG));
    }

    // Add max variable
    JsonNode maxParameter = getRequestParameter(AweConstants.COMPONENT_MAX, parameters);
    if (maxParameter != null && !maxParameter.isNull()) {
      variableMap.put(AweConstants.QUERY_MAX, new QueryParameter(maxParameter, false, ParameterType.LONG));
    } else {
      variableMap.put(AweConstants.QUERY_MAX, new QueryParameter(JsonNodeFactory.instance.numberNode(baseConfigProperties.getComponent().getGridRowsPerPage()), false, ParameterType.LONG));
    }

    // Add database variable
    JsonNode aliasParameter = getRequestParameter(databaseConfigProperties.getParameterName(), parameters);
    if (aliasParameter != null && !aliasParameter.isNull()) {
      variableMap.put(AweConstants.QUERY_DATABASE, new QueryParameter(aliasParameter, false, ParameterType.STRING));
    }

    // Add language variable
    JsonNode langParameter = getRequestParameter(AweConstants.QUERY_LANGUAGE, parameters);
    if (langParameter != null && !langParameter.isNull()) {
      variableMap.put(AweConstants.QUERY_LANGUAGE, new QueryParameter(langParameter, false, ParameterType.STRING));
    }

    return variableMap;
  }

  /**
   * Prepare query variables if not defined previously
   *
   * @param variables  Variable map
   * @param parameters Parameter list
   * @param index      Index
   * @return Query parameter map
   * @throws AWException Error generating variables
   */
  public Map<String, QueryParameter> getVariableMap(List<Variable> variables, ObjectNode parameters, Integer index) throws AWException {
    Map<String, QueryParameter> variableMap = getDefaultVariableMap(parameters);

    // Get defined variables
    for (Variable variable : variables) {
      if (index == null) {
        JsonNode value = getParameter(variable, parameters);
        boolean isList = variableIsList(variable, parameters);
        if (!variableMap.containsKey(variable.getId()) || allowVariable(variable, value)) {
          variableMap.put(variable.getId(), new QueryParameter(value, isList, ParameterType.valueOf(variable.getType())));
        }
      } else {
        JsonNode parameter = getParameter(variable, parameters);
        if (variableIsList(variable, parameters)) {
          ArrayNode parameterList = (ArrayNode) parameter;
          parameter = parameterList.get(index);
        }
        variableMap.put(variable.getId(), new QueryParameter(parameter, false, ParameterType.valueOf(variable.getType())));
      }
    }

    return variableMap;
  }

  /**
   * Check if add variable into variable map or not
   *
   * @param variable Variable
   * @param value    Json value
   * @return Add variable into variable map
   */
  private boolean allowVariable(Variable variable, JsonNode value) {
    return switch (ParameterType.valueOf(variable.getType())) {
      case SYSTEM_DATE, SYSTEM_TIME, SYSTEM_TIMESTAMP -> true;
      default -> !variable.isOptional() || value != null && !value.isNull();
    };
  }

  /**
   * Prepare query variables if not defined previously
   *
   * @param query      Query
   * @param parameters Parameter list
   * @return Query parameter map
   * @throws AWException Error generating variables
   */
  public Map<String, QueryParameter> getVariableMap(Query query, ObjectNode parameters) throws AWException {
    return getVariableMap(query.getVariableDefinitionList() == null ? new ArrayList<>() : query.getVariableDefinitionList(), parameters, null);
  }

  /**
   * Prepare query variables if not defined previously
   *
   * @param variableMap Variable map
   * @param query       Query
   * @throws AWException Error generating variables
   */
  public void addToVariableMap(Map<String, QueryParameter> variableMap, Query query, ObjectNode parameters) throws AWException {
    Map<String, QueryParameter> queryParameterMap = getVariableMap(query, parameters);
    // Get defined variables
    for (Map.Entry<String, QueryParameter> entry : queryParameterMap.entrySet()) {
      if (!variableMap.containsKey(entry.getKey()) || (!isEmptyParameter(entry.getValue()))) {
        variableMap.put(entry.getKey(), entry.getValue());
      }
    }
  }

  /**
   * Prepare query variables if not defined previously
   *
   * @param dataList DataList
   */
  public void addDataListToRequestParameters(DataList dataList, ObjectNode parameters) {
    // Get defined variables
    List<String> columnList = DataListUtil.getColumnList(dataList);
    for (String columnId : columnList) {
      parameters.set(columnId, DataListUtil.getColumnAsArrayNode(dataList, columnId));
    }
  }

  /**
   * Get variable map for a single index without lists
   *
   * @param query      Query
   * @param parameters Parameter map
   * @return Query parameter map
   * @throws AWException Error generating variables
   */
  public Map<String, QueryParameter> getVariableMap(MaintainQuery query, ObjectNode parameters) throws AWException {
    // Ger variable map with parameter lists
    if (query.getVariableIndex() == null) {
      return getVariableMap((Query) query, null);
    } else {
      return getVariableMap(query.getVariableDefinitionList() == null ? new ArrayList<>() : query.getVariableDefinitionList(), parameters, query.getVariableIndex());
    }
  }

  /**
   * Prepare query variables if not defined previously
   *
   * @param query Query
   * @param page  Page number
   * @param max   Max elements per page
   * @return Query parameter map
   * @throws AWException Error generating variables
   */
  public Map<String, QueryParameter> getVariableMap(Query query, String page, String max) throws AWException {
    Map<String, QueryParameter> variableMap = getVariableMap(query, null);

    // Force page and max if not null
    return forcePageAndMax(variableMap, page, max);
  }

  /**
   * Prepare query variables if not defined previously
   *
   * @return Query parameter map
   */
  public ObjectNode getParameters() {
    return getParameters(getParametersFromRequest());
  }

  private ObjectNode getParametersFromRequest() {
    return Optional.ofNullable(getRequest())
        .map(AweRequest::getParametersSafe)
        .orElse(Optional.ofNullable(prototypeRequestBeanHolder.getPrototypeBean())
            .map(RequestDataHolder::getRequestData)
            .orElse(JsonNodeFactory.instance.objectNode()));
  }

  /**
   * Prepare query variables if not defined previously
   *
   * @param parameters Parameters
   * @return Query parameter map
   */
  public ObjectNode getParameters(ObjectNode parameters) {
    return getParameters(Optional.ofNullable(parameters).orElse(JsonNodeFactory.instance.objectNode()), null, null, null);
  }

  /**
   * Prepare query variables if not defined previously
   *
   * @param parametersBean Parameters bean
   * @return Query parameter map
   */
  public <T> ObjectNode getParameters(T parametersBean) {
    ObjectNode parameters = new ObjectMapper().convertValue(parametersBean, ObjectNode.class);
    return getParameters(Optional.ofNullable(parameters).orElse(JsonNodeFactory.instance.objectNode()), null, null, null);
  }

  /**
   * Prepare query variables if not defined previously
   *
   * @param alias Database alias
   * @param page  Page number
   * @param max   Max elements per page
   * @return Query parameter map
   */
  public ObjectNode getParameters(String alias, String page, String max) {
    // Force page and max if not null
    return getParameters(getParameters(), alias, page, max);
  }

  /**
   * Prepare query variables if not defined previously
   *
   * @param alias Database alias
   * @return Query parameter map
   */
  public ObjectNode getParameters(String alias) {
    // Force page and max if not null
    return getParameters(getParameters(), alias, null, null);
  }

  /**
   * Prepare query variables if not defined previously
   *
   * @param parameters Parameters
   * @param alias      Query alias
   * @param page       Page number
   * @param max        Max elements per page
   * @return Query parameter map
   */
  public ObjectNode getParameters(@NonNull ObjectNode parameters, String alias, String page, String max) {
    // Force page and max if not null
    return forceAliasPageAndMax(parameters, alias, page, max);
  }

  /**
   * Prepare query variables if not defined previously
   *
   * @param page Page number
   * @param max  Max elements per page
   * @return Query parameter map
   * @throws AWException Error generating variables
   */
  public Map<String, QueryParameter> getVariableMap(String page, String max) throws AWException {
    Map<String, QueryParameter> variableMap = getDefaultVariableMap(null);

    // Force page and max if not null
    return forcePageAndMax(variableMap, page, max);
  }

  /**
   * Prepare query variables if not defined previously
   *
   * @param variableMap Variable map
   * @param page        Page number
   * @param max         Max elements per page
   * @return Query parameter map
   */
  private Map<String, QueryParameter> forcePageAndMax(Map<String, QueryParameter> variableMap, String page, String max) {
    // Force page if not null
    if (page != null) {
      variableMap.put(AweConstants.QUERY_PAGE, new QueryParameter(JsonNodeFactory.instance.numberNode(Long.parseLong(page)), false, ParameterType.LONG));
    }

    // Force max if not null
    if (max != null) {
      variableMap.put(AweConstants.QUERY_MAX, new QueryParameter(JsonNodeFactory.instance.numberNode(Long.parseLong(max)), false, ParameterType.LONG));
    }

    return variableMap;
  }

  /**
   * Prepare query variables if not defined previously
   *
   * @param parameters Parameters
   * @param alias      Database alias
   * @param page       Page number
   * @param max        Max elements per page
   * @return Query parameter map
   */
  private ObjectNode forceAliasPageAndMax(ObjectNode parameters, String alias, String page, String max) {
    ObjectNode forcedParameters = parameters.deepCopy();
    // Force alias if not null
    if (alias != null) {
      forcedParameters.set(databaseConfigProperties.getParameterName(), JsonNodeFactory.instance.textNode(alias));
    }

    // Force page if not null
    if (page != null) {
      forcedParameters.set(AweConstants.COMPONENT_PAGE, JsonNodeFactory.instance.numberNode(Long.parseLong(page)));
    }

    // Force max if not null
    if (max != null) {
      forcedParameters.set(AweConstants.COMPONENT_MAX, JsonNodeFactory.instance.numberNode(Long.parseLong(max)));
    }

    return forcedParameters;
  }

  /**
   * Retrieve parameter
   *
   * @param variable   Variable
   * @param parameters Parameters
   * @return Parameter
   * @throws AWException Error retrieving variable value
   */
  public JsonNode getParameter(Variable variable, ObjectNode parameters) throws AWException {
    // Variable definition
    JsonNode parameter = variable.getName() != null ? getRequestParameter(variable.getName(), parameters) : null;

    // Check value as static
    String stringParameter;
    if (variable.getSession() != null) {
      stringParameter = (String) getSession().getParameter(variable.getSession());
    } else if (variable.getProperty() != null) {
      stringParameter = getElements().getProperty(variable.getProperty());
    } else {
      stringParameter = variable.getValue();
    }

    // Retrieve parameter
    return getParameter(stringParameter, parameter, ParameterType.valueOf(variable.getType()), variable.getId());
  }

  /**
   * Retrieve parameter as Json
   *
   * @param stringValue String value
   * @param parameter   Parameter
   * @param type        Type
   * @param variableId  Variable id
   * @return Parameter as JSON
   * @throws AWException Error retrieving variable value
   */
  public JsonNode getParameter(String stringValue, JsonNode parameter, ParameterType type, String variableId) throws AWException {
    JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
    JsonNode output;

    // If parameter is not json, generate it
    switch (type) {
      case DOUBLE, FLOAT, INTEGER, LONG:
        output = getNumberParameter(parameter, stringValue, variableId, type);
        break;
      case OBJECT:
        output = getObjectParameter(parameter, stringValue);
        break;
      case DATE, TIME, TIMESTAMP, STRINGN:
        output = getStringWithNullsParameter(parameter, stringValue);
        break;
      case SYSTEM_DATE:
        output = nodeFactory.textNode(DateUtil.dat2WebDate(new Date()));
        break;
      case SYSTEM_TIME:
        output = nodeFactory.textNode(DateUtil.dat2WebTime(new Date()));
        break;
      case SYSTEM_TIMESTAMP:
        output = nodeFactory.textNode(DateUtil.dat2WebTimestamp(new Date()));
        break;
      case NULL:
        output = nodeFactory.nullNode();
        break;
      case STRING_TO_LIST:
        // Retrieve string value as list
        ArrayNode valueList = nodeFactory.arrayNode();
        Arrays.stream(StringUtils.split(stringValue, ","))
            .map(String::trim)
            .forEach(valueList::add);

        output = valueList;
        break;
      case STRING, STRINGB, STRINGL, STRINGR, STRING_ENCRYPT,
           STRING_HASH_RIPEMD160, STRING_HASH_PBKDF_2_W_HMAC_SHA_1, STRING_HASH_SHA:
      default:
        output = getStringParameter(parameter, stringValue);
        break;
    }

    // Retrieve Json node
    return output;
  }

  private JsonNode getNumberParameter(JsonNode parameter, String stringParameter, String variableId, ParameterType type) {
    JsonNode output = parameter;
    if (parameter == null) {
      output = generateNumberNode(variableId, stringParameter, type);
    } else if (!parameter.isNumber() && !parameter.isArray() && parameter.isTextual()) {
      output = generateNumberNode(variableId, parameter.asText(), type);
    }
    return output;
  }

  private JsonNode getObjectParameter(JsonNode parameter, String stringParameter) throws AWException {
    JsonNode output = parameter;
    if (parameter == null && stringParameter != null) {
      try {
        output = mapper.reader().readTree(stringParameter);
      } catch (IOException exc) {
        throw new AWException(getLocale("ERROR_MESSAGE_PARSING_OBJECT", stringParameter), exc);
      }
    }
    return output;
  }

  private JsonNode getStringWithNullsParameter(JsonNode parameter, String stringParameter) {
    JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
    JsonNode output = parameter;
    if (parameter == null) {
      if (stringParameter == null) {
        output = nodeFactory.nullNode();
      } else {
        output = nodeFactory.textNode(stringParameter);
      }
    }
    return output;
  }

  private JsonNode getStringParameter(JsonNode parameter, String stringParameter) {
    JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
    JsonNode output = parameter;
    if (parameter == null && stringParameter != null) {
      output = nodeFactory.textNode(stringParameter);
    } else if (parameter == null || parameter.isNull()) {
      output = nodeFactory.textNode("");
    }
    return output;
  }

  /**
   * Retrieve parameter from request
   *
   * @param name       Parameter name
   * @param parameters Parameters list
   * @return Parameter
   */
  public JsonNode getRequestParameter(String name, ObjectNode parameters) {
    ObjectNode safeParameters = Optional.ofNullable(parameters).orElse(JsonNodeFactory.instance.objectNode());
    return safeParameters.has(name) ? safeParameters.get(name) : getRequestParameter(name);
  }

  /**
   * Retrieve parameter from request
   *
   * @param name Parameter name
   * @return Parameter
   */
  public JsonNode getRequestParameter(String name) {
    return getParametersFromRequest().get(name);
  }

  /**
   * Finds out if a variable value is a list
   *
   * @param variable   Variable
   * @param parameters Parameters
   * @return isList
   */
  public boolean variableIsList(@NonNull Variable variable, ObjectNode parameters) {
    boolean list = false;

    if (variable.getName() != null) {
      JsonNode nodeValue = getRequestParameter(variable.getName(), parameters);
      list = nodeValue != null && nodeValue.isArray() && nodeValue instanceof ArrayNode;
    } else if (variable.getValue() != null) {
      list = variable.getValue().contains(",");
    }
    list = ParameterType.MULTIPLE_SEQUENCE.equals(ParameterType.valueOf(variable.getType())) || list;

    return list;
  }

  /**
   * Generate a number node from string
   *
   * @param value Value
   * @param type  Type
   * @return Number node
   */
  private JsonNode generateNumberNode(String name, String value, ParameterType type) {
    JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
    JsonNode parameter;

    if (value == null) {
      parameter = nodeFactory.nullNode();
    } else {
      try {
        parameter = switch (type) {
          case DOUBLE -> nodeFactory.numberNode(Double.parseDouble(value));
          case FLOAT -> nodeFactory.numberNode(Float.parseFloat(value));
          case LONG -> nodeFactory.numberNode(Long.parseLong(value));
          default -> nodeFactory.numberNode(Integer.parseInt(value));
        };
      } catch (NumberFormatException exc) {
        // If a parameter is an un parseable number, set it to NULL
        log.info(getLocale("INFO_MESSAGE_PARSING_NUMBER_NULL", name, value));
        parameter = nodeFactory.nullNode();
      }
    }
    return parameter;
  }

  /**
   * Check if a string variable is empty or not
   *
   * @param value String value
   * @return String is null or empty
   */
  public boolean isEmptyString(String value) {
    return isNullString(value) || value.isEmpty();
  }

  /**
   * Check if a string variable is null or not
   *
   * @param value String value
   * @return String is null or empty
   */
  public boolean isNullString(String value) {
    return value == null;
  }

  /**
   * Check if a json node variable is empty or not
   *
   * @param variable Variable as json
   * @return Variable is empty
   */
  public boolean isEmptyVariable(JsonNode variable) {
    return isNullVariable(variable) || variable.asText().isEmpty();
  }

  /**
   * Check if a json node variable is null or not
   *
   * @param variable Variable as json
   * @return Variable is null
   */
  public boolean isNullVariable(JsonNode variable) {
    return variable == null || variable.isNull();
  }

  /**
   * Check if a parameter variable is empty or not
   *
   * @param parameter Variable as json
   * @return Variable is empty
   */
  public boolean isEmptyParameter(QueryParameter parameter) {
    return parameter == null || isEmptyVariable(parameter.getValue());
  }

  /**
   * Retrieve database alias
   *
   * @param parameters Query parameters
   * @return Database alias
   */
  public String getDatabaseAlias(Map<String, QueryParameter> parameters) {
    // Alias is current database
    String alias = null;

    // Check if call refers to a specific database
    if (parameters != null && parameters.get(AweConstants.QUERY_DATABASE) != null && parameters.get(AweConstants.QUERY_DATABASE).getValue() != null) {
      alias = parameters.get(AweConstants.QUERY_DATABASE).getValue().asText();
    }

    // Retrieve alias
    return alias;
  }

  /**
   * Retrieve full sql statement as string
   *
   * @param sql        SQL Statement
   * @param parameters Parameter list
   * @return SQL statement
   */
  public String getFullSQL(String sql, List<Object> parameters) {
    return parameters
        .stream()
        .map(this::formatParameter)
        .reduce(sql, (fixed, binding) -> fixed.replaceFirst("\\?", Matcher.quoteReplacement(binding)));
  }

  /**
   * Format log parameters
   *
   * @param binding Binding to format
   * @return Formatted parameter
   */
  private String formatParameter(Object binding) {
    if (binding instanceof String stringBinding) {
      return MessageFormat.format("''{0}''", StringUtil.shortenText(stringBinding, 25, "..."));
    } else if (binding instanceof Date dateBinding) {
      return MessageFormat.format("(timestamp ''{0}'')", DateUtil.dat2SqlTimeString(dateBinding));
    }
    return binding.toString();
  }
}
