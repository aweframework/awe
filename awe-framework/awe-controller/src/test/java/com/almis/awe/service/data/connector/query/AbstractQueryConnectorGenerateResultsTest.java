package com.almis.awe.service.data.connector.query;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.NumericService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for {@code AbstractQueryConnector#generateResults}.
 *
 * <p>{@code fillDataList} deliberately tolerates a null {@link ServiceData} by returning an empty
 * {@link DataList} — a queue subscription may deliver no data at all, and
 * {@code QueueQueryConnector#onSubscriptionData} forwards that value straight through.
 * {@code generateResults} did not honour that contract and dereferenced the same null result right
 * after, throwing a NullPointerException. These tests pin both the null path and the ordinary one.
 */
@ExtendWith(MockitoExtension.class)
class AbstractQueryConnectorGenerateResultsTest {

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private BaseConfigProperties baseConfigProperties;

  @Mock
  private AweElements elements;

  @Mock
  private NumericService numericService;

  @Mock
  private EncodeService encodeService;

  @Mock
  private ObjectMapper mapper;

  private TestQueryConnector connector;

  @BeforeEach
  void setUp() {
    connector = new TestQueryConnector(queryUtil, baseConfigProperties, elements, numericService, encodeService, mapper);
  }

  /**
   * The null path returns from {@code fillDataList} before any Spring bean lookup, so the real
   * implementation runs here unstubbed.
   */
  @Test
  @DisplayName("generateResults with a null result returns an empty datalist instead of throwing")
  void generateResultsWithNullResultReturnsEmptyDataList() throws AWException {
    Map<String, QueryParameter> parameterMap = Collections.emptyMap();

    assertThatCode(() -> connector.generateResults(null, new Query(), parameterMap)).doesNotThrowAnyException();

    ServiceData result = connector.generateResults(null, new Query(), parameterMap);
    assertThat(result).isNotNull();
    assertThat(result.getDataList()).isNotNull();
    assertThat(result.getDataList().getRows()).isEmpty();
  }

  @Test
  @DisplayName("generateResults assigns the datalist onto the very instance it was given")
  void generateResultsAssignsTheDataListOntoTheGivenInstance() throws AWException {
    DataList filled = new DataList();
    filled.setRecords(7);
    connector.stubDataList(filled);
    ServiceData given = new ServiceData();

    ServiceData result = connector.generateResults(given, new Query(), Collections.emptyMap());

    assertThat(result).isSameAs(given);
    assertThat(result.getDataList()).isSameAs(filled);
  }

  /**
   * Minimal concrete connector isolating {@code generateResults} from the {@code DataListBuilder}
   * machinery: {@code fillDataList} is only overridden when a datalist is explicitly stubbed, so the
   * null path still exercises the real implementation.
   */
  private static class TestQueryConnector extends AbstractQueryConnector {

    private DataList stubbedDataList;

    TestQueryConnector(QueryUtil queryUtil, BaseConfigProperties baseConfigProperties, AweElements elements,
                       NumericService numericService, EncodeService encodeService, ObjectMapper mapper) {
      super(queryUtil, baseConfigProperties, elements, numericService, encodeService, mapper);
    }

    void stubDataList(DataList dataList) {
      this.stubbedDataList = dataList;
    }

    @Override
    protected DataList fillDataList(ServiceData serviceData, Query query, Map<String, QueryParameter> parameterMap) throws AWException {
      return stubbedDataList != null ? stubbedDataList : super.fillDataList(serviceData, query, parameterMap);
    }

    @Override
    public ServiceData launch(Query query, ObjectNode parameters) {
      return new ServiceData();
    }

    @Override
    public ServiceData subscribe(Query query, ComponentAddress address, ObjectNode parameters) {
      return new ServiceData();
    }
  }
}
