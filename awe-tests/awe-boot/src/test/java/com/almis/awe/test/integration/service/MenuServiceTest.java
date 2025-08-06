package com.almis.awe.test.integration.service;

import com.almis.awe.service.MenuService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@DisplayName("Menu service Test")
class MenuServiceTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  protected WebApplicationContext applicationContext;

  @Autowired
  private MenuService menuService;

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
    assertEquals(11, menuService.getAvailableScreenList("").getDataList().getRecords());
    assertEquals(2, menuService.getAvailableScreenList("si").getDataList().getRecords());
  }

  /**
   * Check available the private screen list
   *
   * @throws Exception Test failed
   */
  @Test
  @WithMockUser(username = "test", password = "test")
  void getAvailablePrivateScreenList() throws Exception {
    assertEquals(31, menuService.getAvailableScreenList("").getDataList().getRecords());
  }

  /**
   * Check all screen lists
   */
  @Test
  @WithMockUser(username = "test", password = "test")
  void getAllScreenList() {
    assertEquals(128, menuService.getAllScreenList("").getDataList().getRecords());
  }
}
