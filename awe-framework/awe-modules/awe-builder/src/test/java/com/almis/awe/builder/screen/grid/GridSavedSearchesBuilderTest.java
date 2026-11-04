package com.almis.awe.builder.screen.grid;

import com.almis.awe.model.entities.screen.component.grid.Grid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Builders can offer the saved searches of a grid in its toolbar (#912)
 */
class GridSavedSearchesBuilderTest {

  @Test
  void aGridCanOfferSavedSearches() throws Exception {
    Grid grid = new GridBuilder().setSavedSearches(true).setId("GrdUsr").build();

    assertEquals(Boolean.TRUE, grid.getSavedSearchesConverter());
  }

  @Test
  void aGridOffersNoSavedSearchesByDefault() throws Exception {
    Grid grid = new GridBuilder().setId("GrdUsr").build();

    assertNull(grid.getSavedSearches());
    assertNull(grid.getSavedSearchesConverter());
  }
}
