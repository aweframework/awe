package com.almis.awe.security.authorization;

import com.almis.awe.config.AuthorizationMode;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.actions.Action;
import com.almis.awe.model.entities.actions.Call;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.permissions.PermissionCatalog;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.Tag;
import com.almis.awe.model.entities.screen.component.Dialog;
import com.almis.awe.model.entities.screen.component.SidePanel;
import com.almis.awe.model.entities.screen.component.action.ButtonAction;
import com.almis.awe.model.entities.screen.component.button.AbstractButton;
import com.almis.awe.model.entities.screen.component.button.Button;
import com.almis.awe.model.entities.screen.component.button.ContextButton;
import com.almis.awe.model.entities.screen.component.button.ContextSeparator;
import com.almis.awe.model.entities.screen.component.grid.Column;
import com.almis.awe.model.entities.screen.component.grid.Grid;
import com.almis.awe.model.entities.services.Service;
import com.almis.awe.security.audit.SecurityAuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The screen elements a user cannot use, in enforce mode: the buttons, context buttons and button columns whose actions
 * launch a target without the user permissions are removed, an element permission replaces that decision, and a
 * dialog action without its edit permission is flagged to open in view mode
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScreenElementAuthorizerTest {

  @Mock private ApplicationContext applicationContext;
  @Mock private AweElements aweElements;
  @Mock private SecurityAuditService securityAuditService;
  @Mock private ObjectProvider<SecurityAuditService> auditProvider;

  private SecurityConfigProperties properties;
  private ScreenElementAuthorizer authorizer;

  @BeforeEach
  void setUp() throws Exception {
    properties = new SecurityConfigProperties();
    properties.getAuthorization().setMode(AuthorizationMode.ENFORCE);
    PermissionTargetAuthorizer permissionTargetAuthorizer = new PermissionTargetAuthorizer(properties);
    permissionTargetAuthorizer.setApplicationContext(applicationContext);
    authorizer = new ScreenElementAuthorizer(properties, permissionTargetAuthorizer);
    authorizer.setApplicationContext(applicationContext);
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(applicationContext.getBeanProvider(SecurityAuditService.class)).thenReturn(auditProvider);
    when(auditProvider.getIfAvailable()).thenReturn(securityAuditService);
    when(securityAuditService.isEnabled()).thenReturn(true);

    // Dispatcher actions (their service is authorized by the target) and an action calling its own service
    action("maintain", "launchMaintain");
    action("data", "launchQuery");
    action("export-users", "UsrExp");
    when(aweElements.getService("launchMaintain"))
      .thenReturn(Service.builder().id("launchMaintain").authorization("target").build());
    when(aweElements.getService("launchQuery"))
      .thenReturn(Service.builder().id("launchQuery").authorization("target").build());
    when(aweElements.getService("UsrExp"))
      .thenReturn(Service.builder().id("UsrExp").permission("users.export").build());
    when(aweElements.getMaintain("UsrDel")).thenReturn(new Target().setName("UsrDel").setPermission("users.delete"));
    when(aweElements.getMaintain("OwnUpd")).thenReturn(new Target().setName("OwnUpd").setPermission("self.update"));
    when(aweElements.getMaintain("Free")).thenReturn(new Target().setName("Free"));
    when(aweElements.getQuery("UsrLst")).thenReturn(new Query().setId("UsrLst").setPermission("users.read"));
    when(aweElements.getQuery("PrfLst")).thenReturn(new Query().setId("PrfLst").setPermission("profiles.read"));

    PermissionCatalog catalog = mock(PermissionCatalog.class);
    when(catalog.getGrantedAlways()).thenReturn(Set.of("self.read", "self.update"));
    when(aweElements.getPermissionCatalog()).thenReturn(catalog);
    SecurityContextHolder.getContext()
      .setAuthentication(new TestingAuthenticationToken("jane", null, "ROLE_operator", "PERM_users.read"));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void action(String id, String service) throws Exception {
    when(aweElements.getAction(id)).thenReturn(Action.builder().id(id).call(Call.builder().service(service).build())
      .build());
  }

  private static ButtonAction server(String serverAction, String targetAction) {
    return ButtonAction.builder().type("server").serverAction(serverAction).targetAction(targetAction).build();
  }

  private static ButtonAction dialog(String editPermission) {
    return ButtonAction.builder().type("dialog").target("PnlUsr").editPermission(editPermission).build();
  }

  private static <T extends Element> T withActions(T element, String id, ButtonAction... actions) {
    element.setId(id);
    for (ButtonAction action : actions) {
      element.addElement(action);
    }
    return element;
  }

  private static Button button(String id, ButtonAction... actions) {
    return withActions(new Button(), id, actions);
  }

  private static Screen screen(Element... elements) {
    Tag tag = new Tag();
    for (Element element : elements) {
      tag.addElement(element);
    }
    return (Screen) new Screen().setId("users").addElement(tag);
  }

  private static List<String> buttons(Screen screen) {
    return screen.getElementsByType(AbstractButton.class).stream().map(Element::getId).toList();
  }

  @Test
  void aButtonLaunchingATargetWithoutTheUserPermissionIsRemoved() {
    Screen screen = screen(button("ButDel", ButtonAction.builder().type("confirm").target("DelMsg").build(),
      server("maintain", "UsrDel")), button("ButExp", server("export-users", null)), button("ButLst",
      server("data", "UsrLst")));

    authorizer.apply(screen);

    assertThat(buttons(screen)).containsExactly("ButLst");
    verify(securityAuditService, never()).failure(any(), any(), any(), any());
  }

  @ParameterizedTest
  @EnumSource(value = AuthorizationMode.class, names = {"WARN", "OFF"})
  void warnAndOffLeaveTheScreenAsItIs(AuthorizationMode mode) {
    properties.getAuthorization().setMode(mode);
    ButtonAction open = dialog("users.update");
    ButtonAction openPanel = dialog(null);
    Screen screen = screen(button("ButDel", server("maintain", "UsrDel")), button("ButEdt", open),
      button("ButOpn", openPanel), panel(new SidePanel(), button("ButPnlDel", server("maintain", "UsrDel"))));

    authorizer.apply(screen);

    assertThat(buttons(screen)).containsExactly("ButDel", "ButEdt", "ButOpn", "ButPnlDel");
    assertThat(open.getViewMode()).isNull();
    assertThat(openPanel.getViewMode()).isNull();
  }

  @Test
  void targetsGrantedAlwaysWithoutPermissionOrUnknownKeepTheButton() {
    Screen screen = screen(button("ButOwn", server("maintain", "OwnUpd")), button("ButFre", server("maintain", "Free")),
      button("ButUnk", server("maintain", "Missing")), button("ButNav", ButtonAction.builder().type("screen")
        .target("home").build()), button("ButAct", server("unknown-action", "UsrDel")));

    authorizer.apply(screen);

    assertThat(buttons(screen)).containsExactly("ButOwn", "ButFre", "ButUnk", "ButNav", "ButAct");
  }

  @Test
  void withRequireUndeclaredAButtonLaunchingAMaintainWithoutPermissionNeedsDefaultWrite() {
    properties.getPermissions().setRequireUndeclared(true);
    Screen screen = screen(button("ButFre", server("maintain", "Free")));

    authorizer.apply(screen);

    assertThat(buttons(screen)).isEmpty();

    SecurityContextHolder.getContext()
      .setAuthentication(new TestingAuthenticationToken("jane", null, "ROLE_operator", "PERM_default.write"));
    Screen granted = screen(button("ButFre", server("maintain", "Free")));
    authorizer.apply(granted);

    assertThat(buttons(granted)).containsExactly("ButFre");
  }

  @Test
  void theElementPermissionReplacesTheDerivedDecision() {
    Button shown = button("ButDel", server("maintain", "UsrDel"));
    shown.setPermission("users.read");
    Button hidden = button("ButNew", dialog(null));
    hidden.setPermission("users.read users.create");

    Screen screen = screen(shown, hidden);
    authorizer.apply(screen);

    assertThat(buttons(screen)).containsExactly("ButDel");
  }

  @Test
  void contextButtonsAndButtonColumnsAreRemovedInsideAGrid() {
    Grid grid = new Grid();
    grid.setId("GrdUsr");
    Column text = new Column();
    text.setName("Nam");
    Column delete = withActions(new Column(), null, server("maintain", "UsrDel"));
    delete.setName("DelBtn");
    delete.setComponentType("button");
    Column edit = withActions(new Column(), null, dialog(null));
    edit.setName("EdtBtn");
    edit.setComponentType("button");
    Column protectedColumn = withActions(new Column(), null, dialog(null));
    protectedColumn.setName("PrtBtn");
    protectedColumn.setComponentType("button");
    protectedColumn.setPermission("users.update");
    grid.addElement(text).addElement(delete).addElement(edit).addElement(protectedColumn)
      .addElement(withActions(new ContextButton(), "CtxDel", server("maintain", "UsrDel")))
      .addElement(withActions(new ContextButton(), "CtxEdt", dialog(null)));

    Screen screen = screen(grid);
    authorizer.apply(screen);

    assertThat(screen.getElementsByType(Column.class)).extracting(Column::getName).containsExactly("Nam", "EdtBtn");
    assertThat(buttons(screen)).containsExactly("CtxEdt");
  }

  @Test
  void theSeparatorsLeftLeadingTrailingOrConsecutiveInAContextMenuAreRemoved() {
    Grid grid = new Grid();
    grid.setId("GrdUsr");
    grid.addElement(separator("SepFirst"))
      .addElement(withActions(new ContextButton(), "CtxDelFirst", server("maintain", "UsrDel")))
      .addElement(withActions(new ContextButton(), "CtxEdt", dialog(null)))
      .addElement(separator("SepMiddle"))
      .addElement(withActions(new ContextButton(), "CtxDel", server("maintain", "UsrDel")))
      .addElement(new Column())
      .addElement(separator("SepNext"))
      .addElement(withActions(new ContextButton(), "CtxLst", server("data", "UsrLst")))
      .addElement(separator("SepLast"))
      .addElement(withActions(new ContextButton(), "CtxDelLast", server("maintain", "UsrDel")));

    Screen screen = screen(grid);
    authorizer.apply(screen);

    assertThat(contextMenu(grid)).containsExactly("CtxEdt", "SepMiddle", "CtxLst");
  }

  @Test
  void theSeparatorsAreKeptWhenNothingIsRemoved() {
    Grid grid = new Grid();
    grid.setId("GrdUsr");
    grid.addElement(separator("SepFirst"))
      .addElement(withActions(new ContextButton(), "CtxEdt", dialog(null)))
      .addElement(separator("SepLast"));

    authorizer.apply(screen(grid));

    assertThat(contextMenu(grid)).containsExactly("SepFirst", "CtxEdt", "SepLast");
  }

  private static ContextSeparator separator(String name) {
    ContextSeparator separator = new ContextSeparator();
    separator.setName(name);
    return separator;
  }

  private static List<String> contextMenu(Element parent) {
    return parent.getElementList().stream()
      .filter(element -> element instanceof ContextButton || element instanceof ContextSeparator)
      .map(element -> element instanceof ContextSeparator separator ? separator.getName() : element.getId()).toList();
  }

  @Test
  void aDialogActionWithoutTheEditPermissionOpensInViewMode() {
    ButtonAction update = dialog("users.update");
    ButtonAction read = dialog("users.read");
    ButtonAction free = dialog(null);
    ButtonAction notDialog = ButtonAction.builder().type("value").target("PnlMod").editPermission("users.update")
      .build();

    authorizer.apply(screen(button("ButEdt", update), button("ButViw", read), button("ButOpn", free),
      button("ButVal", notDialog)));

    assertThat(update.getViewMode()).isTrue();
    assertThat(read.getViewMode()).isNull();
    assertThat(free.getViewMode()).isNull();
    assertThat(notDialog.getViewMode()).isNull();
  }

  private static Dialog panel(Dialog panel, Element... elements) {
    panel.setId("PnlUsr");
    for (Element element : elements) {
      panel.addElement(element);
    }
    return panel;
  }

  @Test
  void aPanelWhoseSaveButtonsAreAllRemovedOpensInViewModeFromEveryOpening() {
    ButtonAction fromButton = dialog(null);
    ButtonAction fromContext = dialog("users.read");
    ButtonAction otherPanel = ButtonAction.builder().type("dialog").target("PnlHlp").build();
    Grid grid = new Grid();
    grid.setId("GrdUsr");
    grid.addElement(withActions(new ContextButton(), "CtxEdt", fromContext));

    Screen screen = screen(button("ButOpn", fromButton), button("ButHlp", otherPanel), grid,
      panel(new SidePanel(), button("ButPnlCnl", ButtonAction.builder().type("close").build()),
        button("ButPnlDel", server("maintain", "UsrDel")),
        withActions(new ContextButton(), "CtxPnlExp", server("export-users", null))));
    authorizer.apply(screen);

    assertThat(buttons(screen)).containsExactly("ButOpn", "ButHlp", "CtxEdt", "ButPnlCnl");
    assertThat(fromButton.getViewMode()).isTrue();
    assertThat(fromContext.getViewMode()).isTrue();
    assertThat(otherPanel.getViewMode()).isNull();
  }

  @Test
  void aPanelWithASaveButtonLeftKeepsItsOpeningsAsTheyAre() {
    ButtonAction open = dialog(null);
    ButtonAction edit = dialog("users.update");

    authorizer.apply(screen(button("ButOpn", open), button("ButEdt", edit),
      panel(new Dialog(), button("ButPnlDel", server("maintain", "UsrDel")),
        button("ButPnlSav", server("maintain", "Free")))));

    assertThat(open.getViewMode()).isNull();
    assertThat(edit.getViewMode()).isTrue();
  }

  @Test
  void aPanelWithoutSaveButtonsIsNotAffected() {
    ButtonAction open = dialog(null);
    Screen screen = screen(button("ButOpn", open), panel(new SidePanel(),
      button("ButPnlCnl", ButtonAction.builder().type("close").build()),
      button("ButPnlPrf", server("data", "PrfLst"))));

    authorizer.apply(screen);

    assertThat(buttons(screen)).containsExactly("ButOpn", "ButPnlCnl");
    assertThat(open.getViewMode()).isNull();
  }
}
