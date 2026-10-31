package com.almis.awe.service.screen;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.component.grid.Grid;
import com.almis.awe.model.entities.screen.data.ScreenComponent;
import com.almis.awe.service.GridLayoutService;
import com.almis.awe.service.WindowStateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Grid toolbar in generated tag lists")
class ScreenComponentGeneratorGridToolbarTest {

  @Test
  @DisplayName("A grid of a generated tag list has no toolbar, because it belongs to no screen whose layout could be stored")
  void tagListGridsHaveNoToolbar() throws Exception {
    AweRequest request = mock(AweRequest.class);
    when(request.getParameterList()).thenReturn(JsonNodeFactory.instance.objectNode());
    ScreenComponentGenerator generator = new ScreenComponentGenerator(request, mock(ScreenModelGenerator.class),
      mock(InitialLoadDao.class), mock(AweElementsDao.class), new BaseConfigProperties(), mock(ScreenConfigurationGenerator.class),
      mock(GridLayoutService.class), mock(WindowStateService.class));
    Grid grid = new Grid();
    grid.setId("TagGrid");
    List<Element> elements = List.of(grid);

    Map<String, ScreenComponent> components = generator.generateTagListComponentMap("Tags", elements);

    Grid generated = (Grid) components.get("TagGrid").getController();
    assertFalse(generated.isShowToolbar());
    assertNull(generated.getScreenId());
  }
}
