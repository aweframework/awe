package com.almis.awe.model.dao;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.model.entities.queries.Queries;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.util.XmlSerializerTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AweElementsDao tests
 */
class AweElementsDaoTest {

  private AweElementsDao elementsDao;
  private BaseConfigProperties baseConfigProperties;

  @BeforeEach
  void setUp() {
    baseConfigProperties = new BaseConfigProperties();
    baseConfigProperties.setModuleList(new String[]{"module-one", "module-two"});
    elementsDao = new AweElementsDao(XmlSerializerTestUtil.buildSerializer(), baseConfigProperties);
  }

  /**
   * Test that a screen file is resolved to the expected definition: directly under the screen
   * folder, in a subdirectory, and honoring module order (first module wins for shared files)
   *
   * @param fileName      Screen file to resolve
   * @param expectedLabel Expected label of the resolved screen
   */
  @ParameterizedTest
  @CsvSource({
    "flat-screen.xml, FLAT_SCREEN_MODULE_ONE",
    "sub-screen.xml, SUB_SCREEN_MODULE_ONE",
    "shared-screen.xml, SHARED_SCREEN_MODULE_ONE"
  })
  void readXmlFileResolvesScreen(String fileName, String expectedLabel) {
    Screen screen = elementsDao.readXmlFile(Screen.class, "/screen/", fileName);

    assertNotNull(screen);
    assertEquals(expectedLabel, screen.getLabel());
  }

  /**
   * Test that a missing file returns null without throwing
   */
  @Test
  void readXmlFileReturnsNullWhenMissing() {
    assertNull(elementsDao.readXmlFile(Screen.class, "/screen/", "not-a-screen.xml"));
  }

  /**
   * Test that global files are merged across modules with first module precedence
   */
  @Test
  void readXmlFilesMergesModulesWithPrecedence() {
    Map<String, Query> storage = new ConcurrentHashMap<>();

    elementsDao.readXmlFiles(Queries.class, storage, "/global/Queries.xml");

    assertEquals("serviceModuleOne", storage.get("sharedQuery").getService());
    assertTrue(storage.containsKey("moduleOneQuery"));
    assertTrue(storage.containsKey("moduleTwoQuery"));
  }

  /**
   * Test that unparseable global files (malformed XML or unmappable content) neither throw
   * nor corrupt the entries read from the other modules
   */
  @Test
  void readXmlFilesToleratesMalformedModuleFile() {
    baseConfigProperties.setModuleList(new String[]{"module-one", "module-broken", "module-unmapped"});
    Map<String, Query> storage = new ConcurrentHashMap<>();

    assertDoesNotThrow(() -> elementsDao.readXmlFiles(Queries.class, storage, "/global/Queries.xml"));

    assertEquals("serviceModuleOne", storage.get("sharedQuery").getService());
    assertTrue(storage.containsKey("moduleOneQuery"));
    assertFalse(storage.containsKey("brokenQuery"));
    assertFalse(storage.containsKey("unmappedQuery"));
  }

  /**
   * Test that unparseable files inside a folder scan (malformed XML or unmappable content)
   * do not wipe the valid files of the folder
   */
  @Test
  void readModuleFolderXmlFileToleratesMalformedFile() throws Exception {
    Map<String, Screen> storage = elementsDao.readModuleFolderXmlFile(Screen.class, "/application/module-broken/screen/").get();

    assertTrue(storage.containsKey("valid-screen"));
    assertFalse(storage.containsKey("broken"));
    assertFalse(storage.containsKey("unmapped"));
  }

  /**
   * Test that the resource folder derivation anchors the base path to the classpath-relative
   * occurrence, even when the absolute URL contains the same segments earlier
   */
  @Test
  void findResourceFolderAnchorsBasePathMatching() {
    String basePath = "/application/module-one/screen/";
    String ambiguousUrl = "file:/checkout/application/module-one/screen/workspace/target/classes/application/module-one/screen/sub/sub-screen.xml";

    assertEquals("/application/module-one/screen/sub/", AweElementsDao.findResourceFolder(ambiguousUrl, basePath));
  }
}
