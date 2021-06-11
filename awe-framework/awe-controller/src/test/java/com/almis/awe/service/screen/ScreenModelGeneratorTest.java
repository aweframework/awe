package com.almis.awe.service.screen;

import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.entities.screen.data.ComponentModel;
import com.almis.awe.model.entities.screen.data.ScreenComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Screen Model generator tests
 */
class ScreenModelGeneratorTest {

  @InjectMocks
  private ScreenModelGenerator screenModelGenerator;

  /**
   * Initialize beans
   */
  @BeforeEach
  public void initBeans() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void generateComponentModelFromDataList() {
    ScreenComponent screenComponent = new ScreenComponent().setModel(new ComponentModel());
    screenModelGenerator.generateComponentModelFromDataList(new DataList(), screenComponent);
    assertEquals(Long.valueOf(0), screenComponent.getModel().getRecords());
  }
}
