package com.almis.awe.component;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.queries.DatabaseConnectionInfo;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class AweDatabaseContextHolderTest {

  @InjectMocks
  private AweDatabaseContextHolder aweDatabaseContextHolder;
  @Mock
  private QueryService queryService;
  @Mock
  private AweElements aweElements;


  @Test
  void givenDataSourceConnections_thenGetDataSources() throws AWException {
    // Given
    DatabaseConnectionInfo databaseConnectionInfo = new DatabaseConnectionInfo()
            .setAlias("DummyDataBase")
            .setDriver("org.hsqldb.jdbc.JDBCDriver")
            .setUrl("jdbc:hsqldb:file:target/tests/db")
            .setUser("foo")
            .setPassword("dummyPass");
    DataList dataList = DataListUtil.fromBeanList(new ArrayList<>(Collections.singletonList(databaseConnectionInfo)));
    // When
    when(queryService.launchPrivateQuery(AweConstants.DATABASE_CONNECTIONS_QUERY, "1", "0")).thenReturn(new ServiceData().setDataList(dataList));
    // Assert
    assertNotNull(aweDatabaseContextHolder.getDataSources().get("DummyDataBase"));
  }

  @Test
  void givenJndiDataSourceProperties_thenGetDataSources() {
    // Given
    DataSourceProperties properties = new DataSourceProperties();
    properties.setJndiName("datasource/dummyDatabase");
    // Asserts
    assertNotNull(aweDatabaseContextHolder.getDataSource(properties));
  }

  @Test
  void givenMultiDatabaseEnabledAndDataSourceMapContainsAlias_getDataSource() throws AWException {
    // Given
    setField(aweDatabaseContextHolder, "multiDatabaseEnable", true);
    aweDatabaseContextHolder.getDataSourceMap().put("dummyAlias", mock(DataSource.class));
    // Asserts
    assertNotNull(aweDatabaseContextHolder.getDataSource("dummyAlias"));
  }

  @Test
  void givenMultiDatabaseEnabledAndEmptyDataSourceMapContainsAlias_shouldThrowAWException() {
    // Given
    setField(aweDatabaseContextHolder, "multiDatabaseEnable", true);
    given(aweElements.getLanguage()).willReturn("ES");
    given(aweElements.getLocaleWithLanguage(anyString(), anyString())).willReturn("LOCALE");
    // Asserts
    assertThrows(AWException.class, () -> aweDatabaseContextHolder.getDataSource("dummyAlias"));
  }

}