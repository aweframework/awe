package com.almis.awe.dao;

import com.almis.awe.exception.AWEUnauthorizedException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.screen.data.AweThreadInitialization;
import com.almis.awe.model.type.LoadType;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The initial loads of a screen are client launches: the queries of the screen are checked before they are launched
 */
class InitialLoadDaoTest {

  private static final String TARGET = "UsrLst";

  private final QueryService queryService = mock(QueryService.class);
  private final InitialLoadDao initialLoadDao = new InitialLoadDao(queryService);

  @ParameterizedTest
  @NullSource
  @EnumSource(value = LoadType.class, names = {"ENUM", "MENU"}, mode = EnumSource.Mode.EXCLUDE)
  void refusedQueryOfTheScreenIsNotLaunched(LoadType loadType) throws Exception {
    doThrow(new AWEUnauthorizedException("Title", "Refused")).when(queryService).authorizeClientLaunch(TARGET);

    assertThatThrownBy(() -> initialLoadDao.launchInitialLoad(initialization(loadType)))
      .isInstanceOf(AWEUnauthorizedException.class).hasMessage("Refused");
    verify(queryService, never()).launchPrivateQuery(anyString(), any(ObjectNode.class));
  }

  @Test
  void allowedQueryOfTheScreenIsLaunched() throws Exception {
    ServiceData data = new ServiceData();
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(data);

    assertThat(initialLoadDao.launchInitialLoad(initialization(LoadType.QUERY)).get()).isSameAs(data);
    verify(queryService).authorizeClientLaunch(TARGET);
  }

  @ParameterizedTest
  @EnumSource(value = LoadType.class, names = {"ENUM", "MENU"})
  void enumeratedAndMenuRestrictionLoadsAreNotChecked(LoadType loadType) throws Exception {
    when(queryService.launchEnumQuery(anyString())).thenReturn(new ServiceData());
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    assertThat(initialLoadDao.launchInitialLoad(initialization(loadType)).get()).isNotNull();
    verify(queryService, never()).authorizeClientLaunch(anyString());
  }

  private static AweThreadInitialization initialization(LoadType loadType) {
    return new AweThreadInitialization()
      .setInitialLoadType(loadType)
      .setTarget(TARGET)
      .setParameters(JsonNodeFactory.instance.objectNode());
  }
}
