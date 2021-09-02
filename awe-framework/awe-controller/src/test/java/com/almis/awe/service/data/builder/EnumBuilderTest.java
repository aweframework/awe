package com.almis.awe.service.data.builder;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.Global;
import com.almis.awe.model.entities.enumerated.EnumeratedGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnumBuilderTest {

  @InjectMocks
  private EnumBuilder enumBuilder;

  @Mock
  private ApplicationContext context;

  @Mock
  private AweElements aweElements;

  @BeforeEach
  public void initBeans() throws Exception {
    MockitoAnnotations.openMocks(this);
    enumBuilder.setApplicationContext(context);
    doReturn(aweElements).when(context).getBean(AweElements.class);
  }

  @Test
  void getEnumeratedAsDataList_withMockEnum_returnDataList() throws AWException {
    // Given
    EnumeratedGroup enumeratedGroup = new EnumeratedGroup();
    enumeratedGroup.setId("mockEnum");
    List<Global> enumerateList = new ArrayList<>();
    Global enumYes = new Global();
    enumYes.setLabel("LABEL_YES");
    enumYes.setValue("1");
    Global enumNo = new Global();
    enumNo.setLabel("LABEL_NO");
    enumNo.setValue("0");
    enumerateList.add(enumYes);
    enumerateList.add(enumNo);
    enumeratedGroup.setOptionList(enumerateList);

    when(aweElements.getEnumerated(anyString())).thenReturn(enumeratedGroup);
    // Run
    enumBuilder.setEnumerated(anyString()).build();
    // Assert
    assertEquals(2, enumBuilder.getEnumeratedAsDataList().getRows().size());
  }

  @Test
  void getEnumeratedAsDataList_withNullEnum_throwAWException() throws AWException {
    // When
    when(aweElements.getEnumerated(anyString())).thenReturn(mock(EnumeratedGroup.class));
    // Assert
    assertThrows(AWException.class, () -> {
      // Run
      enumBuilder.setEnumerated(anyString()).getEnumeratedAsDataList();
    });
  }
}