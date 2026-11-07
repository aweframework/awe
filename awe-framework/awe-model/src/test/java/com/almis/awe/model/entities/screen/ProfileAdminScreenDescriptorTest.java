package com.almis.awe.model.entities.screen;

import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.component.Dialog;
import com.almis.awe.model.entities.screen.component.action.ButtonAction;
import com.almis.awe.model.entities.screen.component.action.Dependency;
import com.almis.awe.model.entities.screen.component.action.DependencyAction;
import com.almis.awe.model.entities.screen.component.action.DependencyElement;
import com.almis.awe.model.entities.screen.component.button.AbstractButton;
import com.almis.awe.model.entities.screen.component.criteria.AbstractCriteria;
import com.almis.awe.model.entities.screen.component.grid.Column;
import com.almis.awe.model.util.XmlSerializerTestUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The profiles screen (profile-admin) declares the permissions of its actions: creating and duplicating need
 * profiles.create, editing a profile without profiles.update opens the panel in view mode, and the permissions of the
 * profile are a field of the same panel, loaded with their own targets only for the users that can read them
 */
@DisplayName("Profile admin screen descriptor")
class ProfileAdminScreenDescriptorTest {

  private static final Path SCREEN = Path.of(
    "../awe-generic-screens/src/main/resources/application/awe/screen/profiles/profile-admin.xml").normalize();

  private static Screen screen;

  @BeforeAll
  static void readScreen() throws IOException {
    screen = (Screen) XmlSerializerTestUtil.buildMarshaller().getXStream().fromXML(Files.readString(SCREEN));
  }

  @Test
  @DisplayName("New profile and Duplicate only open the profile panel, so they declare profiles.create")
  void creationButtonsDeclareProfilesCreate() {
    assertThat(button("ButNew").getPermission()).isEqualTo("profiles.create");
    assertThat(button("CtxButDup").getPermission()).isEqualTo("profiles.create");
    assertThat(button("ButPnlDup").getPermission()).isEqualTo("profiles.create");
    assertThat(dialogOpenings(button("CtxButDup"), "PnlPro")).extracting(ButtonAction::getEditPermission)
      .containsOnlyNulls();
  }

  @Test
  @DisplayName("The edit openings of the profile panel require profiles.update to edit")
  void editOpeningsRequireProfilesUpdate() {
    assertThat(dialogOpenings(column("EdtBtn"), "PnlPro")).extracting(ButtonAction::getEditPermission)
      .containsExactly("profiles.update");
    assertThat(dialogOpenings(button("CtxButUpd"), "PnlPro")).extracting(ButtonAction::getEditPermission)
      .containsExactly("profiles.update");
  }

  @Test
  @DisplayName("The permissions are a field of the profile panel, loaded only for the users that can read them")
  void permissionsAreAFieldOfTheProfilePanel() {
    Dialog panel = screen.getElementsByType(Dialog.class).stream().filter(dialog -> "PnlPro".equals(dialog.getId()))
      .findFirst().orElseThrow();
    AbstractCriteria permissions = criterion(panel, "Per");

    assertThat(permissions.getComponentType()).isEqualTo("select-multiple");
    assertThat(permissions.getElementsByType(Dependency.class))
      .filteredOn(dependency -> dependency.getTargetAction() != null)
      .extracting(Dependency::getTargetAction).containsExactly("ProPerSel");
    assertThat(permissions.getElementsByType(DependencyAction.class)).extracting(DependencyAction::getTargetAction)
      .containsExactly("ProPerCat");
    assertThat(permissions.getElementsByType(DependencyElement.class)).extracting(DependencyElement::getId)
      .contains("PerAcc");
    assertThat(criterion(screen, "PerAcc").getTargetAction()).isEqualTo("ProPerAcc");
    assertThat(criterion(screen, "PerUpd").getValue()).isEqualTo("1");
    assertThat(maintains(button("ButPnlDupNew"))).containsExactly("ProDup");
    assertThat(maintains(button("ButPnlUpd"))).containsExactly("ProUpd");
  }

  private static AbstractCriteria criterion(Element parent, String id) {
    List<AbstractCriteria> criteria = parent.getElementsByType(AbstractCriteria.class).stream()
      .filter(criterion -> id.equals(criterion.getId())).toList();
    assertThat(criteria).as(id).hasSize(1);
    return criteria.getFirst();
  }

  private static AbstractButton button(String id) {
    List<AbstractButton> buttons = screen.getElementsByType(AbstractButton.class).stream()
      .filter(button -> id.equals(button.getId())).toList();
    assertThat(buttons).as(id).hasSize(1);
    return buttons.getFirst();
  }

  private static Column column(String name) {
    List<Column> columns = screen.getElementsByType(Column.class).stream()
      .filter(column -> name.equals(column.getName())).toList();
    assertThat(columns).as(name).hasSize(1);
    return columns.getFirst();
  }

  private static List<ButtonAction> dialogOpenings(Element element, String panel) {
    return element.getChildrenByType(ButtonAction.class).stream()
      .filter(action -> "dialog".equals(action.getType()) && panel.equals(action.getTarget())).toList();
  }

  private static List<String> maintains(Element element) {
    return element.getChildrenByType(ButtonAction.class).stream()
      .filter(action -> "maintain".equals(action.getServerAction())).map(ButtonAction::getTargetAction).toList();
  }
}
