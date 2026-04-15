package com.almis.awe.service.connector;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.entities.services.ServiceInputParameter;
import com.almis.awe.model.type.ParameterType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.DateUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * Abstract class that contains general method implementations for all ServiceConnectors
 *
 * @author jbellon
 */
@Slf4j
abstract class AbstractServiceConnector extends ServiceConfig implements ServiceConnector {

  private static final String CANT_CREATE_INSTANCE = "Can't create instance of ";

  private final ObjectMapper objectMapper;

  AbstractServiceConnector(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Extract parameters values
   *
   * @param paramsFromXml         XML parameters
   * @param paramsMapFromRequest  Map parameters
   * @param paramsToInvoke        Parameters to invoke
   * @param paramsClassesToInvoke Classes to invoke
   * @throws AWException Error extracting parameters
   */
  void extractParameters(List<ServiceInputParameter> paramsFromXml, Map<String, Object> paramsMapFromRequest, Object[] paramsToInvoke, Class[] paramsClassesToInvoke) throws AWException {
    Integer iteratingParam = 0;

    // Add parameters into the parameter object list
    for (ServiceInputParameter param : paramsFromXml) {
      extractParameter(param, paramsMapFromRequest, paramsToInvoke, paramsClassesToInvoke, iteratingParam++);
    }
  }

  /**
   * Extract parameters values
   *
   * @param parameter             XML parameter
   * @param paramsMapFromRequest  Map parameters
   * @param paramsToInvoke        Parameters to invoke
   * @param paramsClassesToInvoke Classes to invoke
   * @param index                 Parameter index
   * @throws AWException Error extracting parameters
   */
  private void extractParameter(ServiceInputParameter parameter, Map<String, Object> paramsMapFromRequest, Object[] paramsToInvoke, Class[] paramsClassesToInvoke, Integer index) throws AWException {
    if (parameter.getBeanClass() != null) {
      Class beanClass = getParameterClass(parameter);
      if (parameter.isList()) {
        paramsToInvoke[index] = DataListUtil.getParameterBeanListValue(beanClass, paramsMapFromRequest);
        paramsClassesToInvoke[index] = List.class;
      } else if ("JSON".equalsIgnoreCase(parameter.getType())) {
        paramsToInvoke[index] = getParameterJsonBeanValue(parameter, beanClass, paramsMapFromRequest);
        paramsClassesToInvoke[index] = beanClass;
      } else {
        paramsToInvoke[index] = DataListUtil.getParameterBeanValue(beanClass, paramsMapFromRequest);
        paramsClassesToInvoke[index] = beanClass;
      }
    } else {
      if (parameter.isList()) {
        paramsToInvoke[index] = getParameterListValue(parameter, paramsMapFromRequest);
        paramsClassesToInvoke[index] = List.class;
      } else {
        paramsToInvoke[index] = getParameterValue(parameter, paramsMapFromRequest.get(parameter.getName()));
        paramsClassesToInvoke[index] = getParameterClass(parameter);
      }
    }
  }

  /**
   * Get parameter class
   * @param parameter Parameter
   */
  Class getParameterClass(ServiceInputParameter parameter) {
    switch (ParameterType.valueOf(parameter.getType())) {
      case INTEGER:
        return Integer.class;
      case LONG:
        return Long.class;
      case FLOAT:
        return Float.class;
      case DOUBLE:
        return Double.class;
      case DATE, TIME, TIMESTAMP:
        return Date.class;
      case BOOLEAN:
        return Boolean.class;
      case OBJECT:
        try {
          return Class.forName(parameter.getBeanClass());
        } catch (Exception exc) {
          log.error("Error loading parameter class {}, return default Object.class value", parameter.getBeanClass());
          return Object.class;
        }
      case JSON:
        try {
          return Class.forName(parameter.getBeanClass());
        } catch (Exception exc) {
          return JsonNode.class;
        }
      case DATE_RDB, STRING:
      default:
        return String.class;
    }
  }

  /**
   * Extract parameters values
   *
   * @param parameter      Parameter
   * @param parameterValue Parameter value
   */
  Object getParameterValue(ServiceInputParameter parameter, Object parameterValue) {
    switch (ParameterType.valueOf(parameter.getType())) {
      case INTEGER:
        return getIntegerValue(parameterValue);
      case LONG:
        return getLongValue(parameterValue);
      case FLOAT:
        return getFloatValue(parameterValue);
      case DOUBLE:
        return getDoubleValue(parameterValue);
      case BOOLEAN:
        return getBooleanValue(parameterValue);
      case DATE, TIMESTAMP:
        if (parameterValue instanceof String stringValue) {
          return Optional.of(stringValue)
            .filter(StringUtils::isNotBlank)
            .map(DateUtil::web2Date)
            .orElse(null);
        } else {
          return parameterValue;
        }
      case TIME:
        if (parameterValue instanceof String stringValue) {
          return Optional.of(stringValue)
            .filter(StringUtils::isNotBlank)
            .map(DateUtil::web2Time)
            .orElse(null);
        } else {
          return parameterValue;
        }
      case DATE_RDB:
        if (parameterValue instanceof String stringValue) {
          return Optional.of(stringValue)
            .filter(StringUtils::isNotBlank)
            .map(DateUtil::web2RdbDate)
            .orElse(null);
        } else if (parameterValue instanceof Date dateValue) {
          return DateUtil.dat2RDBDate(dateValue);
        } else {
          return parameterValue;
        }
      case OBJECT, STRING:
      default:
        return parameterValue;
    }
  }

  private Integer getIntegerValue(Object parameterValue) {
    return Optional.ofNullable(parameterValue)
      .filter(value -> !"".equals(value))
      .map(value -> value instanceof Integer integerValue ? integerValue : ((Number) normalizeNumericValue(value)).intValue())
      .orElse(null);
  }

  private Long getLongValue(Object parameterValue) {
    return Optional.ofNullable(parameterValue)
      .filter(value -> !"".equals(value))
      .map(value -> value instanceof Long longValue ? longValue : ((Number) normalizeNumericValue(value)).longValue())
      .orElse(null);
  }

  private Float getFloatValue(Object parameterValue) {
    return Optional.ofNullable(parameterValue)
      .filter(value -> !"".equals(value))
      .map(value -> value instanceof Float floatValue ? floatValue : ((Number) normalizeNumericValue(value)).floatValue())
      .orElse(null);
  }

  private Double getDoubleValue(Object parameterValue) {
    return Optional.ofNullable(parameterValue)
      .filter(value -> !"".equals(value))
      .map(value -> value instanceof Double doubleValue ? doubleValue : ((Number) normalizeNumericValue(value)).doubleValue())
      .orElse(null);
  }

  private Boolean getBooleanValue(Object parameterValue) {
    return Optional.ofNullable(parameterValue)
      .filter(value -> !"".equals(value))
      .map(value -> value instanceof Boolean booleanValue ? booleanValue : Boolean.valueOf(String.valueOf(value)))
      .orElse(null);
  }

  private Number normalizeNumericValue(Object parameterValue) {
    if (parameterValue instanceof Number numberValue) {
      return numberValue;
    }
    return new BigDecimal(String.valueOf(parameterValue));
  }

  /**
   * Returns the value array list
   *
   * @param parameter parameter name
   * @param paramsMap map with parameters
   * @return Service call string
   */
  List<Object> getParameterListValue(ServiceInputParameter parameter, Map<String, Object> paramsMap) {

    // Variable definition
    List<Object> parameterList = new ArrayList<>();
    Object parameterValue;
    if (paramsMap.containsKey(parameter.getName()) && !"".equals(paramsMap.get(parameter.getName()))) {
      parameterValue = paramsMap.get(parameter.getName());
      if (parameterValue instanceof Collection parameterListValue) {
        for (Object parameterValueElement : parameterListValue) {
          parameterList.add(getParameterValue(parameter, parameterValueElement));
        }
      } else {
        parameterList.add(getParameterValue(parameter, parameterValue));
      }
    }
    return parameterList;
  }

  /**
   * Retrieve parameter as bean value
   * @param beanClass Bean class
   * @param paramsMap Parameter map
   * @param <T> Bean type
   * @return Bean value
   * @throws AWException
   */
  <T> T getParameterJsonBeanValue(ServiceInputParameter parameter, Class<T> beanClass, Map<String, Object> paramsMap) throws AWException {
    try {
      if (paramsMap.get(parameter.getName()) != null) {
        // Generate row bean
        return objectMapper.treeToValue((JsonNode) paramsMap.get(parameter.getName()), beanClass);
      } else {
        return null;
      }
    } catch (Exception exc) {
      throw new AWException("Error converting json parameter to object", CANT_CREATE_INSTANCE + beanClass.getSimpleName(), exc);
    }
  }
}
