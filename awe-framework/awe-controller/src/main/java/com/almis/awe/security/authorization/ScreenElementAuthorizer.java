package com.almis.awe.security.authorization;

import com.almis.awe.config.AuthorizationMode;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.actions.Action;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.Dialog;
import com.almis.awe.model.entities.screen.component.action.ButtonAction;
import com.almis.awe.model.entities.screen.component.button.Button;
import com.almis.awe.model.entities.screen.component.button.ContextButton;
import com.almis.awe.model.entities.screen.component.button.ContextSeparator;
import com.almis.awe.model.entities.screen.component.grid.Column;
import com.almis.awe.model.entities.services.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Removes from a screen the elements the session user cannot use, when {@code awe.security.authorization.mode} is
 * {@code enforce}: {@code off} and {@code warn} leave the screen as it is.
 *
 * <p>A button, a context button or a column of component button is removed when one of its button actions launches a
 * maintain target, a query or a service whose permission the user does not hold. The {@code server-action} of the
 * action names the action called; its service is the target launched, unless the service is authorized by its
 * target, which is then the {@code target-action} (a maintain target or a query, whichever exists). A target that
 * requires no permission (see {@link PermissionTargetAuthorizer}), granted always or unknown never removes anything.
 * The {@code permission} attribute of the element, when present, replaces that decision. When context buttons are
 * removed, the context separators left at the start, at the end or next to another separator of the context menu are
 * removed too.</p>
 *
 * <p>A button action of type {@code dialog} whose {@code edit-permission} the user does not hold is flagged as a view
 * mode opening ({@code viewMode} of the {@link ButtonAction}). So is every button action of type {@code dialog}
 * opening a dialog or a side panel whose save buttons were all removed: a save button is a button or a context button of the panel with
 * a button action that launches a maintain target, or a service that is not authorized by its target. A panel without
 * save buttons is not affected.</p>
 *
 * <p>It works on the copy of the screen generated for the request, before its components are generated, so both
 * engines receive the screen without the removed elements. Only the permissions decide: the access by screen is not
 * checked here. The decision is made for each request, as it depends on the user.</p>
 */
@Slf4j
public class ScreenElementAuthorizer extends ServiceConfig {

  private static final String BUTTON_COMPONENT = "button";
  private static final String DIALOG_ACTION = "dialog";

  private final SecurityConfigProperties securityConfigProperties;
  private final PermissionTargetAuthorizer permissionTargetAuthorizer;

  /**
   * Autowired constructor
   *
   * @param securityConfigProperties   Security configuration properties
   * @param permissionTargetAuthorizer Permission target authorizer
   */
  public ScreenElementAuthorizer(SecurityConfigProperties securityConfigProperties,
                                 PermissionTargetAuthorizer permissionTargetAuthorizer) {
    this.securityConfigProperties = securityConfigProperties;
    this.permissionTargetAuthorizer = permissionTargetAuthorizer;
  }

  /**
   * Remove from the screen the elements the user cannot use and flag the dialog actions that open in view mode
   *
   * @param screen Copy of the screen generated for the request (it is changed)
   */
  public void apply(Screen screen) {
    if (securityConfigProperties.getAuthorization().getMode() != AuthorizationMode.ENFORCE) {
      return;
    }
    Set<String> panelsWithSaveButtons = panelsWithSaveButtons(screen);
    prune(screen);
    Set<String> viewModePanels = new HashSet<>(panelsWithSaveButtons);
    viewModePanels.removeAll(panelsWithSaveButtons(screen));
    for (ButtonAction action : screen.getElementsByType(ButtonAction.class)) {
      if (DIALOG_ACTION.equals(action.getType()) && (viewModePanels.contains(action.getTarget())
        || action.getEditPermission() != null
        && !permissionTargetAuthorizer.hasPermissions(action.getEditPermission()))) {
        action.setViewMode(true);
      }
    }
  }

  /**
   * Identifiers of the dialogs and side panels of the screen with at least one save button
   */
  private Set<String> panelsWithSaveButtons(Screen screen) {
    Set<String> panels = new HashSet<>();
    for (Dialog panel : screen.getElementsByType(Dialog.class)) {
      boolean saves = panel.getElementsByType(Element.class).stream()
        .filter(element -> element instanceof Button || element instanceof ContextButton)
        .anyMatch(button -> button.getChildrenByType(ButtonAction.class).stream().anyMatch(this::isSave));
      if (saves && panel.getId() != null) {
        panels.add(panel.getId());
      }
    }
    return panels;
  }

  /**
   * Whether a button action saves: it launches a maintain target, or a service that is not authorized by its target
   */
  private boolean isSave(ButtonAction action) {
    String serverAction = action.getServerAction();
    if (serverAction == null || serverAction.isBlank()) {
      return false;
    }
    try {
      AweElements elements = getElements();
      Action called = elements.getAction(serverAction);
      String serviceId = called == null || called.getCall() == null ? null : called.getCall().getService();
      Service service = serviceId == null ? null : elements.getService(serviceId);
      if (service == null) {
        return false;
      }
      String targetId = action.getTargetAction();
      return !service.isAuthorizedByTarget()
        || targetId != null && !targetId.isBlank() && elements.getMaintain(targetId) != null;
    } catch (AWException exc) {
      log.debug("Targets of the server action {} not found", serverAction, exc);
      return false;
    }
  }

  /**
   * Remove the elements the user cannot use from the children of an element, and from theirs
   */
  private void prune(Element parent) {
    List<Element> children = parent.getElementList();
    List<Element> kept = new ArrayList<>(children.size());
    for (Element child : children) {
      if (isAllowed(child)) {
        prune(child);
        kept.add(child);
      }
    }
    if (kept.size() != children.size()) {
      parent.setElementList(withoutOrphanSeparators(kept));
    }
  }

  /**
   * Remove the context separators that are left first, last or after another separator among the context buttons and
   * separators of a list (the context menu); the other elements of the list do not separate anything
   */
  private List<Element> withoutOrphanSeparators(List<Element> elements) {
    List<Element> orphans = new ArrayList<>();
    Element previous = null;
    for (Element element : elements) {
      if (element instanceof ContextButton || element instanceof ContextSeparator) {
        if (element instanceof ContextSeparator && !(previous instanceof ContextButton)) {
          orphans.add(element);
        } else {
          previous = element;
        }
      }
    }
    if (previous instanceof ContextSeparator) {
      orphans.add(previous);
    }
    List<Element> kept = new ArrayList<>(elements);
    kept.removeIf(element -> orphans.stream().anyMatch(orphan -> orphan == element));
    return kept;
  }

  private boolean isAllowed(Element element) {
    return switch (element) {
      case Button button -> isAllowed(button, button.getPermission());
      case ContextButton button -> isAllowed(button, button.getPermission());
      case Column column when BUTTON_COMPONENT.equals(column.getComponentType()) ->
        isAllowed(column, column.getPermission());
      default -> true;
    };
  }

  /**
   * Decide with the permission of the element, or else with the targets its button actions launch
   */
  private boolean isAllowed(Element element, String permission) {
    if (permission != null) {
      return permissionTargetAuthorizer.hasPermissions(permission);
    }
    return element.getChildrenByType(ButtonAction.class).stream().allMatch(this::canLaunch);
  }

  private boolean canLaunch(ButtonAction action) {
    String serverAction = action.getServerAction();
    if (serverAction == null || serverAction.isBlank()) {
      return true;
    }
    try {
      AweElements elements = getElements();
      Action called = elements.getAction(serverAction);
      String serviceId = called == null || called.getCall() == null ? null : called.getCall().getService();
      if (serviceId == null) {
        return true;
      }
      Service service = elements.getService(serviceId);
      if (service == null || !service.isAuthorizedByTarget()) {
        return permissionTargetAuthorizer.canLaunch(serviceId, TargetKind.SERVICE);
      }
      return canLaunchTarget(elements, action.getTargetAction());
    } catch (AWException exc) {
      log.debug("Targets of the server action {} not found", serverAction, exc);
      return true;
    }
  }

  /**
   * Check the target of a service authorized by its target: the maintain target or the query with that identifier
   */
  private boolean canLaunchTarget(AweElements elements, String targetId) throws AWException {
    if (targetId == null || targetId.isBlank()) {
      return true;
    }
    boolean maintain = elements.getMaintain(targetId) == null
      || permissionTargetAuthorizer.canLaunch(targetId, TargetKind.MAINTAIN);
    boolean query = elements.getQuery(targetId) == null
      || permissionTargetAuthorizer.canLaunch(targetId, TargetKind.QUERY);
    return maintain && query;
  }
}
