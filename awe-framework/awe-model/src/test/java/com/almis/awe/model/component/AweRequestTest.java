package com.almis.awe.model.component;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.pojo.Planet;
import com.almis.awe.model.util.data.DateUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * DataList, DataListUtil and DataListBuilder tests
 *
 * @author pgarcia
 */
@ExtendWith(MockitoExtension.class)
class AweRequestTest {

  @InjectMocks
  private AweRequest aweRequest;

  @Mock
  private HttpServletRequest request;

  @Mock
  private ObjectMapper mapper;

  /**
   * Test of set parameter
   */
  @Test
  void testSetParameter() {
    // Prepare
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNodeFactory factory = JsonNodeFactory.instance;
    BigDecimal bigDecimal = new BigDecimal(1213123);
    Date date = new Date();
    ArrayNode lista = factory.arrayNode().add("test1").add("test2");

    when(mapper.convertValue(any(Object.class), eq(JsonNode.class)))
            .thenReturn(factory.textNode("test"))
            .thenReturn(factory.textNode("test1"))
      .thenReturn(factory.textNode("test2"))
      .thenReturn(lista)
      .thenReturn(factory.numberNode(1))
      .thenReturn(factory.numberNode(1L))
      .thenReturn(factory.numberNode(1D))
      .thenReturn(factory.numberNode(1F))
      .thenReturn(factory.numberNode(bigDecimal))
      .thenReturn(factory.booleanNode(true))
      .thenReturn(objectMapper.valueToTree(new CellData("121")))
      .thenReturn(objectMapper.convertValue(new Planet().setClimate("rainy"), JsonNode.class));

    aweRequest.setParameter("test", "test");
    aweRequest.setParameter("testList", "test1", "test2");
    aweRequest.setParameter("testList2", Arrays.asList("test1", "test2"));
    aweRequest.setParameter("testInt", 1);
    aweRequest.setParameter("testLong", 1L);
    aweRequest.setParameter("testDouble", 1D);
    aweRequest.setParameter("testFloat", 1F);
    aweRequest.setParameter("testBigDecimal", bigDecimal);
    aweRequest.setParameter("testDate", date);
    aweRequest.setParameter("testNull", (Object) null);
    aweRequest.setParameter("testBoolean", true);
    aweRequest.setParameter("testCellData", new CellData("121"));
    aweRequest.setParameter("testPOJO", new Planet().setClimate("rainy"));

    // Assert
    assertEquals("test", aweRequest.getParameterAsString("test"));
    assertEquals(lista, aweRequest.getParameter("testList"));
    assertEquals(lista, aweRequest.getParameter("testList2"));
    assertEquals(factory.numberNode(1), aweRequest.getParameter("testInt"));
    assertEquals(factory.numberNode(1L), aweRequest.getParameter("testLong"));
    assertEquals(factory.numberNode(1D), aweRequest.getParameter("testDouble"));
    assertEquals(factory.numberNode(1F), aweRequest.getParameter("testFloat"));
    assertEquals(factory.numberNode(bigDecimal), aweRequest.getParameter("testBigDecimal"));

    // Test retrieve value as string
    assertEquals("1", aweRequest.getParameterAsString("testInt"));
    assertEquals("1", aweRequest.getParameterAsString("testLong"));
    assertEquals("1.0", aweRequest.getParameterAsString("testDouble"));
    assertEquals("1.0", aweRequest.getParameterAsString("testFloat"));
    assertEquals("1213123", aweRequest.getParameterAsString("testBigDecimal"));

    assertEquals(DateUtil.dat2WebTimestamp(date), aweRequest.getParameterAsString("testDate"));
    assertNull(aweRequest.getParameterAsString("testNull"));
    assertTrue(aweRequest.getParameter("testBoolean").asBoolean());
    assertEquals("true", aweRequest.getParameterAsString("testBoolean"));
    assertEquals(factory.textNode("121"), aweRequest.getParameter("testCellData"));
    assertEquals("121", aweRequest.getParameterAsString("testCellData"));
    Assertions.assertEquals(objectMapper.convertValue(new Planet().setClimate("rainy"), ObjectNode.class), aweRequest.getParameter("testPOJO"));
  }
}