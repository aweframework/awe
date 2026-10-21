package com.almis.awe.test.integration.service;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.service.MenuService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@DisplayName("Menu service Test")
class MenuServiceTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  protected WebApplicationContext applicationContext;

  @Autowired
  private MenuService menuService;

  @Autowired
  private AweElements aweElements;

  /**
   * Test of check public addresses
   *
   * @throws Exception Test error
   */
  @Test
  @WithAnonymousUser
  void testCheckPublicAddresses() throws Exception {
    assertFalse(menuService.checkOptionAddress(""));
    assertTrue(menuService.checkOptionAddress("screen/signin"));
    assertFalse(menuService.checkOptionAddress("screen/patata"));
    assertFalse(menuService.checkOptionAddress("screen/private/home/information"));
  }

  /**
   * Test of check private addresses
   *
   * @throws Exception Test error
   */
  @Test
  @WithMockUser(username = "test", password = "test")
  void testCheckPrivateAddresses() throws Exception {
    assertFalse(menuService.checkOptionAddress(""));
    assertTrue(menuService.checkOptionAddress("screen/signin"));
    assertFalse(menuService.checkOptionAddress("screen/patata"));
    assertTrue(menuService.checkOptionAddress("screen/private/home/information"));
  }

  /**
   * Check available the public screen list
   *
   * @throws Exception Test failed
   */
  @Test
  void getAvailablePublicScreenList() throws Exception {
    assertEquals(12, menuService.getAvailableScreenList("").getDataList().getRecords());
    assertEquals(3, menuService.getAvailableScreenList("si").getDataList().getRecords());
  }

  /**
   * Check available the private screen list
   *
   * @throws Exception Test failed
   */
  @Test
  @WithMockUser(username = "test", password = "test")
  void getAvailablePrivateScreenList() throws Exception {
    assertEquals(32, menuService.getAvailableScreenList("").getDataList().getRecords());
  }

  /**
   * Check all screen lists: every screen the application loaded, once, sorted by name (not a fixed count, so a new test
   * screen does not break it)
   */
  @Test
  @WithMockUser(username = "test", password = "test")
  void getAllScreenList() {
    Set<String> loaded = aweElements.getScreenList();
    DataList all = menuService.getAllScreenList("").getDataList();
    List<String> listed = column(all, AweConstants.JSON_VALUE_PARAMETER);

    assertThat(loaded).isNotEmpty();
    assertEquals(loaded.size(), all.getRecords());
    assertThat(listed).containsExactlyInAnyOrderElementsOf(loaded).contains("signin");
    List<String> labels = column(all, AweConstants.JSON_LABEL_PARAMETER);
    assertThat(labels).isSortedAccordingTo(String::compareTo);
  }

  /**
   * Check the screen list filtered by the typed text, case insensitive
   */
  @Test
  @WithMockUser(username = "test", password = "test")
  void getAllScreenListFilteredByText() {
    Set<String> expected = aweElements.getScreenList().stream()
      .filter(screen -> screen.toLowerCase().contains("sign"))
      .collect(Collectors.toSet());

    DataList filtered = menuService.getAllScreenList("SIGN").getDataList();

    assertThat(column(filtered, AweConstants.JSON_VALUE_PARAMETER)).isNotEmpty().containsExactlyInAnyOrderElementsOf(expected);
    assertEquals(expected.size(), filtered.getRecords());
  }

  private static List<String> column(DataList dataList, String name) {
    return dataList.getRows().stream()
      .map((Map<String, CellData> row) -> row.get(name).getStringValue())
      .toList();
  }
}
