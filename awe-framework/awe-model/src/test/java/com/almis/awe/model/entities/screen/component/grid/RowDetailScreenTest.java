package com.almis.awe.model.entities.screen.component.grid;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.Component;
import com.almis.awe.model.util.XmlSerializerTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.WebApplicationContext;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Reading a screen resolves the includes inside a row detail and names its components, which are not screen components
 */
@DisplayName("Row detail inside a screen")
class RowDetailScreenTest {

  private AweElementsDao elementsDao;
  private AweElements aweElements;

  @BeforeEach
  void setUp() throws Exception {
    elementsDao = mock(AweElementsDao.class);
    WebApplicationContext context = mock(WebApplicationContext.class);
    aweElements = new AweElements(context, new BaseConfigProperties(), elementsDao);
    // The screens are read on demand: start with no screen stored
    Field screenMap = AweElements.class.getDeclaredField("screenMap");
    screenMap.setAccessible(true);
    screenMap.set(aweElements, new ConcurrentHashMap<String, Screen>());
  }

  private Screen define(String screenId, String xml) {
    Screen screen = (Screen) XmlSerializerTestUtil.buildMarshaller().getXStream().fromXML(xml);
    when(elementsDao.readXmlFile(eq(Screen.class), any(), eq(screenId + ".xml"))).thenReturn(screen);
    return screen;
  }

  @Test
  @DisplayName("An include inside a row-detail is resolved, and the grid it brings and an inline grid get an identifier")
  void includesAndIdentifiersInsideTheRowDetail() throws Exception {
    define("products", "<screen template=\"window\"><tag source=\"center\">" +
      "<grid id=\"GrdProducts\"><column name=\"id\"/><row-detail><include target-screen=\"orders\" target-source=\"detail\"/></row-detail></grid>" +
      "<grid id=\"GrdOther\"><row-detail><grid server-action=\"data\" target-action=\"Qry\"/></row-detail></grid>" +
      "</tag></screen>");
    define("orders", "<screen template=\"window\"><tag source=\"detail\">" +
      "<grid id=\"GrdOrders\" server-action=\"data\" target-action=\"QryOrders\"><column name=\"order\"/></grid></tag></screen>");

    Screen screen = aweElements.getScreen("products");

    AbstractGrid products = (AbstractGrid) screen.getElementsById("GrdProducts").get(0);
    AbstractGrid other = (AbstractGrid) screen.getElementsById("GrdOther").get(0);
    assertThat(products.getRowDetail().getDetailGrid().getId()).isEqualTo("GrdOrders");
    assertThat(products.getRowDetail().getDefinitionError()).isNull();
    assertThat(other.getRowDetail().getDetailGrid().getId()).isEqualTo("products-grid-1");
    // Only the grids of the screen are found in it
    assertThat(screen.getElementsByType(Component.class)).filteredOn(AbstractGrid.class::isInstance).extracting(Component::getId)
      .containsExactlyInAnyOrder("GrdProducts", "GrdOther");
  }

  @Test
  @DisplayName("Reading a screen resolves a copy: the screen read from the file keeps its row-detail include unresolved")
  void theScreenReadIsNotChanged() throws Exception {
    Screen read = define("products", "<screen template=\"window\"><tag source=\"center\">" +
      "<grid id=\"GrdProducts\"><row-detail><include target-screen=\"orders\" target-source=\"detail\"/></row-detail></grid></tag></screen>");
    define("orders", "<screen template=\"window\"><tag source=\"detail\"><grid id=\"GrdOrders\"/></tag></screen>");

    Screen screen = aweElements.getScreen("products");

    assertThat(((AbstractGrid) screen.getElementsById("GrdProducts").get(0)).getRowDetail().getDetailGrid().getId()).isEqualTo("GrdOrders");
    assertThat(((AbstractGrid) read.getElementsById("GrdProducts").get(0)).getRowDetail().getDetailGrid()).isNull();
  }
}
