package com.almis.awe.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.Favourite;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavouriteServiceTest {

  @InjectMocks
  FavouriteService favouriteService;
  @Mock
  QueryUtil queryUtil;
  @Mock
  QueryService queryService;
  @Mock
  MaintainService maintainService;


  @Test
  void clickFavourite() throws AWException {
    // Prepare
    when(queryUtil.getParameters(any(), anyString(), anyString())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList()));
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    // Do
    favouriteService.clickFavourite("user", "option");

    // Check
    verify(maintainService, times(1)).launchPrivateMaintain(anyString(), any(ObjectNode.class));
  }

  @Test
  void checkFavouritesNoFav() throws AWException {
    // Prepare
    when(queryUtil.getParameters(any(), anyString(), anyString())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList()));

    // Do
    ServiceData serviceData = favouriteService.checkFavourites("user", "option");

    // Check
    assertEquals("update-controller", serviceData.getClientActionList().get(0).getType());
    assertEquals("remove-class", serviceData.getClientActionList().get(1).getType());
  }

  @Test
  void checkFavouritesWithFav() throws AWException {
    // Prepare
    when(queryUtil.getParameters(any(), anyString(), anyString())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(
      DataListUtil.fromBeanList(List.of(new Favourite().setOption("test")))
    ));

    // Do
    ServiceData serviceData = favouriteService.checkFavourites("user", "test");

    // Check
    assertEquals("update-controller", serviceData.getClientActionList().get(0).getType());
    assertEquals("add-class", serviceData.getClientActionList().get(1).getType());
  }

  @Test
  void getFavourites() throws AWException {
    // Prepare
    when(queryUtil.getParameters(any(), anyString(), anyString())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(
      DataListUtil.fromBeanList(List.of(
        new Favourite().setOption("test"),
        new Favourite().setOption("test2")))
    ));

    // Do
    List<Favourite> favourites = favouriteService.getFavourites("user");

    // Check
    assertEquals(2, favourites.size());
  }
}