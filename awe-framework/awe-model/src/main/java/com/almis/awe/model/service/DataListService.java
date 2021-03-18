package com.almis.awe.model.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import lombok.NonNull;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;

import java.lang.reflect.Field;
import java.util.*;

public class DataListService extends ServiceConfig {

  private final ConversionService conversionService;

  private static final String CANT_CREATE_INSTANCE = "Can't create instance of ";

  @Autowired
  public DataListService(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  /**
   * Return the datalist as bean list
   *
   * @param dataList  datalist
   * @param beanClass bean class
   * @param <T>       class type
   * @return bean list
   * @throws AWException AWE exception
   */
  public <T> List<T> asBeanList(@NonNull DataList dataList, Class<T> beanClass) throws AWException {
    List<T> list = new ArrayList<>();

    for (Map<String, CellData> row : dataList.getRows()) {
      try {
        // Store row bean
        list.add(getParameterBeanValue(beanClass, row.entrySet()
                .stream()
                .collect(HashMap::new, (m,v)->m.put(v.getKey(), Optional.ofNullable(v.getValue()).orElse(new CellData()).getObjectValue()), HashMap::putAll)));

      } catch (Exception exc) {
        throw new AWException("Error converting datalist into a bean list", "Cannot create instance of " + beanClass.getSimpleName(), exc);
      }
    }
    return list;
  }

  /**
   * Retrieve parameter as bean value from JSON. You can use Spring Formatter SPI with annotations
   *
   * @param beanClass Bean class
   * @param paramsMap Parameter map
   * @param <T>       Bean type
   * @return Bean value
   * @throws AWException AWE Exception
   */
  public <T> T getParameterBeanValue(Class<T> beanClass, Map<String, Object> paramsMap) throws AWException {
    T parameterBean;

    try {
      // Generate row bean
      parameterBean = beanClass.getConstructor().newInstance();
      ConfigurablePropertyAccessor parameterBeanAccessor = PropertyAccessorFactory.forBeanPropertyAccess(parameterBean);
      parameterBeanAccessor.setConversionService(conversionService);

      for (Field field : getAllFields(beanClass)) {
        if (paramsMap.containsKey(field.getName())) {
          parameterBeanAccessor.setPropertyValue(field.getName(), paramsMap.get(field.getName()));
        }
      }

    } catch (Exception exc) {
      throw new AWException("Error generating bean value", CANT_CREATE_INSTANCE + beanClass.getSimpleName(), exc);
    }

    return parameterBean;
  }

  /**
   * Retrieve parameter as bean list value. You can use Spring Formatter SPI with annotations.
   *
   * @param beanClass Bean class
   * @param paramsMap Parameter map
   * @return Bean list
   * @throws AWException AWE exception
   */
  @SuppressWarnings("unchecked")
  public <T> List<T> getParameterBeanListValue(Class<T> beanClass, Map<String, Object> paramsMap) throws AWException {
    List<T> list = null;

    // Set field value if found in row
    for (Field field : beanClass.getDeclaredFields()) {
      if (paramsMap.containsKey(field.getName())) {
        List<T> valueList = (List<T>) paramsMap.get(field.getName());
        if (list == null) {
          list = initializeList(valueList, beanClass);
        }

        // Store value in bean list
        int index = 0;
        for (T value : valueList) {
          T listBean = list.get(index);
          ConfigurablePropertyAccessor parameterBeanAccessor = PropertyAccessorFactory.forBeanPropertyAccess(listBean);
          parameterBeanAccessor.setConversionService(conversionService);
          parameterBeanAccessor.setPropertyValue(field.getName(), value);
          index++;
        }
      }
    }

    return list;
  }

  /**
   * Get all fields of bean
   *
   * @param type Bean class
   * @return List of fields
   */
  private static List<Field> getAllFields(Class<?> type) {
    List<Field> fields = new ArrayList<>(Arrays.asList(type.getDeclaredFields()));
    if (type.getSuperclass() != null) {
      fields.addAll(getAllFields(type.getSuperclass()));
    }

    return fields;
  }

  /**
   * Initialize bean list
   *
   * @param valueList Value list of field
   * @param beanClass Bean class
   * @param <T>       bean class
   * @return Initialized bean list
   * @throws AWException AWE exception
   */
  private <T> List<T> initializeList(List<T> valueList, Class<T> beanClass) throws AWException {
    List<T> beanList = new ArrayList<>();
    // Initialize list if first defined
    if (!valueList.isEmpty()) {
      for (int i = 0, t = valueList.size(); i < t; i++) {
        // Initialize list
        try {
          // Generate row bean
          T parameterBean = beanClass.getConstructor().newInstance();
          beanList.add(parameterBean);
        } catch (Exception exc) {
          throw new AWException("Error converting datalist into a bean list", CANT_CREATE_INSTANCE + beanClass.getSimpleName(), exc);
        }
      }
    }

    return beanList;
  }
}
