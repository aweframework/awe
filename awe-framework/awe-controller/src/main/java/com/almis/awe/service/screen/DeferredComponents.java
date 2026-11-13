package com.almis.awe.service.screen;

import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.component.Component;
import com.almis.awe.model.entities.screen.component.Dialog;
import com.almis.awe.model.entities.screen.component.container.TabContainer;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Finds the components whose data is deferred until their dialog or tab page is opened for the first time.
 * <p>
 * A component is deferred when any of its ancestors is a dialog or a tab page (tabcontainer) with the
 * <code>deferred</code> attribute. A deferred container is loaded as a unit: when it is opened, it loads its own
 * components except those which are inside another (nested) deferred container, which wait for their own opening.
 * </p>
 */
public final class DeferredComponents {

  private DeferredComponents() {
  }

  /**
   * Check if the element is a deferred container (a dialog or tab page with the deferred attribute)
   *
   * @param element Element
   * @return Element is a deferred container
   */
  public static boolean isDeferredContainer(Element element) {
    return (element instanceof Dialog dialog && dialog.isDeferred())
      || (element instanceof TabContainer tabContainer && tabContainer.isDeferred());
  }

  /**
   * Retrieve all the components below a deferred container, at any depth
   *
   * @param root Element to inspect (a screen)
   * @return Deferred components, by identity
   */
  public static Set<Component> getDeferredComponents(Element root) {
    Set<Component> deferred = Collections.newSetFromMap(new IdentityHashMap<>());
    collectDeferred(root, false, deferred);
    return deferred;
  }

  private static void collectDeferred(Element parent, boolean insideDeferred, Set<Component> deferred) {
    if (parent.getElementList() == null) {
      return;
    }
    for (Element child : parent.getElementList()) {
      if (insideDeferred && child instanceof Component component) {
        deferred.add(component);
      }
      collectDeferred(child, insideDeferred || isDeferredContainer(child), deferred);
    }
  }

  /**
   * Find a deferred container by its identifier
   *
   * @param root        Element to inspect (a screen)
   * @param containerId Container identifier
   * @return Deferred container, or null if there is none with that identifier
   */
  public static Element findDeferredContainer(Element root, String containerId) {
    if (containerId == null) {
      return null;
    }
    return root.getElementsByType(Component.class).stream()
      .filter(DeferredComponents::isDeferredContainer)
      .filter(container -> containerId.equals(container.getId()))
      .findFirst()
      .orElse(null);
  }

  /**
   * Retrieve the components that load when a deferred container is opened: the ones below it which are not inside
   * another deferred container (the nested container itself is included, so it can be opened later)
   *
   * @param container Deferred container
   * @return Components to load
   */
  public static List<Component> getComponentsToLoad(Element container) {
    Set<Component> nested = getDeferredComponents(container);
    return container.getElementsByType(Component.class).stream()
      .filter(component -> !nested.contains(component))
      .toList();
  }
}
