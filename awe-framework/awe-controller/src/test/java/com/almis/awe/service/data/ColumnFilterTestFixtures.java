package com.almis.awe.service.data;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.almis.awe.model.util.data.QueryUtil;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * Shared fixtures of the grid column autofilter tests (#912): query utilities on given elements and elements whose
 * localization answers the key
 */
public final class ColumnFilterTestFixtures {

  /**
   * JSON mapper of the query utilities
   */
  public static final JsonMapper MAPPER = new JsonMapper();

  private ColumnFilterTestFixtures() {
  }

  /**
   * Query utilities that read the localization (and the rest of the elements) from the given elements
   *
   * @param elements Elements, usually a mock
   * @return Query utilities
   */
  public static QueryUtil queryUtil(AweElements elements) {
    return new QueryUtil(new BaseConfigProperties(), new DatabaseConfigProperties(), MAPPER, new PrototypeRequestBeanHolder()) {
      @Override
      public AweElements getElements() {
        return elements;
      }
    };
  }

  /**
   * Stub the localization of mocked elements: a key answers itself, and a key with parameters answers the key followed
   * by its parameters. The stubs are lenient, so a test may use only one of them.
   *
   * @param elements Mocked elements
   * @return The same elements
   */
  public static AweElements stubLocales(AweElements elements) {
    lenient().when(elements.getLocaleWithLanguage(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(elements.getLocaleWithLanguage(anyString(), any(), any(Object[].class)))
      .thenAnswer(invocation -> invocation.getArgument(0) + " " + invocation.getArgument(2));
    return elements;
  }
}
