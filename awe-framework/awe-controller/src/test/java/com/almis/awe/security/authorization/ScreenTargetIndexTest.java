package com.almis.awe.security.authorization;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.Frame;
import com.almis.awe.model.entities.screen.component.action.ButtonAction;
import com.almis.awe.model.entities.screen.component.action.Dependency;
import com.almis.awe.model.entities.screen.component.criteria.Criteria;
import com.almis.awe.model.entities.screen.component.grid.Grid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests of the index that maps each target to the menu options whose screens use it (issue #914)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScreenTargetIndexTest {

  @Mock private ApplicationContext applicationContext;
  @Mock private AweElements aweElements;

  private ScreenTargetIndex index;
  private Menu privateMenu;
  private Menu publicMenu;

  @BeforeEach
  void setUp() throws Exception {
    index = new ScreenTargetIndex(new BaseConfigProperties());
    index.setApplicationContext(applicationContext);
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);

    privateMenu = new Menu();
    publicMenu = new Menu();
    when(aweElements.getMenu("private")).thenReturn(privateMenu);
    when(aweElements.getMenu("public")).thenReturn(publicMenu);
  }

  private Option option(String name, String screen) {
    Option option = new Option();
    option.setName(name);
    option.setScreen(screen);
    return option;
  }

  private Grid grid(String targetAction) {
    Grid grid = new Grid();
    grid.setTargetAction(targetAction);
    return grid;
  }

  private ButtonAction button(String targetAction) {
    ButtonAction button = new ButtonAction();
    button.setTargetAction(targetAction);
    return button;
  }

  private void screen(String id, Element... elements) throws AWException {
    Screen screen = new Screen();
    screen.setId(id);
    for (Element element : elements) {
      screen.addElement(element);
    }
    when(aweElements.getScreen(id)).thenReturn(screen);
  }

  @Test
  @DisplayName("Collects the target-action of components, button actions and dependencies, and check-target")
  void collectsEveryTargetKind() throws Exception {
    Grid grid = grid("QueryOfGrid");
    Dependency dependency = new Dependency();
    dependency.setTargetAction("QueryOfDependency");
    grid.addElement(dependency);
    Criteria criteria = new Criteria();
    criteria.setCheckTarget("QueryOfCheck");
    screen("users", grid, button("MaintainOfButton"), criteria);
    privateMenu.addElement(option("users", "users"));

    assertThat(index.getOptionsUsing("QueryOfGrid")).containsExactly("users");
    assertThat(index.getOptionsUsing("QueryOfDependency")).containsExactly("users");
    assertThat(index.getOptionsUsing("MaintainOfButton")).containsExactly("users");
    assertThat(index.getOptionsUsing("QueryOfCheck")).containsExactly("users");
  }

  @Test
  @DisplayName("A target that no screen uses is not indexed")
  void unknownTargetIsEmpty() throws Exception {
    screen("users", grid("QueryOfGrid"));
    privateMenu.addElement(option("users", "users"));

    assertThat(index.getOptionsUsing("Other")).isEmpty();
    assertThat(index.getOptionsUsing(null)).isEmpty();
  }

  @Test
  @DisplayName("Sub-options are indexed")
  void subOptionsAreIndexed() throws Exception {
    Option parent = option("tools", null);
    parent.addElement(option("users", "users"));
    privateMenu.addElement(parent);
    screen("users", button("UsrUpd"));

    assertThat(index.getOptionsUsing("UsrUpd")).containsExactly("users");
  }

  @Test
  @DisplayName("A target shared by two screens belongs to both options")
  void sharedTargetHasTwoOptions() throws Exception {
    screen("first", button("Shared"));
    screen("second", button("Shared"));
    privateMenu.addElement(option("firstOption", "first"));
    privateMenu.addElement(option("secondOption", "second"));

    assertThat(index.getOptionsUsing("Shared")).containsExactlyInAnyOrder("firstOption", "secondOption");
  }

  @Test
  @DisplayName("The screens of the public menu are indexed")
  void publicMenuIsIndexed() throws Exception {
    screen("login", button("Signin"));
    publicMenu.addElement(option("login", "login"));

    assertThat(index.getOptionsUsing("Signin")).containsExactly("login");
  }

  @Test
  @DisplayName("The screen of a frame is followed, even when the frames are circular")
  void framesAreFollowed() throws Exception {
    Frame frame = new Frame();
    frame.setScreen("inner");
    Frame loop = new Frame();
    loop.setScreen("outer");
    screen("outer", frame);
    screen("inner", button("InnerTarget"), loop);
    privateMenu.addElement(option("outerOption", "outer"));

    assertThat(index.getOptionsUsing("InnerTarget")).containsExactly("outerOption");
  }

  @Test
  @DisplayName("Dynamic and menu screens cannot be enumerated and are skipped")
  void dynamicOptionsAreSkipped() throws Exception {
    Option dynamic = option("dynamic", "dynamicScreen");
    dynamic.setDynamicScreen(true);
    Option menuScreen = option("menuScreen", "menuScreen");
    menuScreen.setMenuScreen(true);
    Option service = option("service", "serviceScreen");
    service.setDynamicScreenService("ScreenQuery");
    screen("dynamicScreen", button("DynamicTarget"));
    screen("menuScreen", button("MenuTarget"));
    screen("serviceScreen", button("ServiceTarget"));
    privateMenu.addElement(dynamic);
    privateMenu.addElement(menuScreen);
    privateMenu.addElement(service);

    assertThat(index.getOptionsUsing("DynamicTarget")).isEmpty();
    assertThat(index.getOptionsUsing("MenuTarget")).isEmpty();
    assertThat(index.getOptionsUsing("ServiceTarget")).isEmpty();
  }

  @Test
  @DisplayName("A screen that cannot be read is skipped and the rest are indexed")
  void brokenScreenIsSkipped() throws Exception {
    when(aweElements.getScreen("broken")).thenThrow(new AWException("broken screen"));
    screen("good", button("GoodTarget"));
    privateMenu.addElement(option("brokenOption", "broken"));
    privateMenu.addElement(option("goodOption", "good"));

    assertThat(index.getOptionsUsing("GoodTarget")).containsExactly("goodOption");
  }

  @Test
  @DisplayName("The index is rebuilt after the elements are reloaded")
  void rebuiltAfterReload() throws Exception {
    when(aweElements.getReloadVersion()).thenReturn(1L);
    screen("users", button("Before"));
    privateMenu.addElement(option("users", "users"));
    assertThat(index.getOptionsUsing("Before")).containsExactly("users");

    // Same version: the index is not rebuilt
    screen("users", button("After"));
    assertThat(index.getOptionsUsing("After")).isEmpty();

    // New version: rebuilt
    when(aweElements.getReloadVersion()).thenReturn(2L);
    assertThat(index.getOptionsUsing("After")).containsExactly("users");
    assertThat(index.getOptionsUsing("Before")).isEmpty();
  }

  @Test
  @DisplayName("The targets of the screen shared by every option (the menu screen) are never restricted")
  void menuScreenTargetsAreExempt() throws Exception {
    privateMenu.setScreen("shell");
    screen("shell", button("ShellTarget"));
    screen("users", button("ShellTarget"), button("UsrUpd"));
    privateMenu.addElement(option("users", "users"));

    assertThat(index.getOptionsUsing("ShellTarget")).isEmpty();
    assertThat(index.getOptionsUsing("UsrUpd")).containsExactly("users");
  }
}
