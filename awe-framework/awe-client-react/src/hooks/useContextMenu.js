import React, { useCallback, useEffect, useMemo, useRef } from "react";
import { ContextMenu } from "primereact/contextmenu";
import { useDispatch } from "react-redux";
import { useTranslation } from "react-i18next";
import { getIconCode, translateLabel } from "../utilities";
import { contextMenuLinkTemplate, contextMenuPassThrough } from "../utilities/testPassThrough";
import { addActionsTop as addActionsTopAction } from "../redux/actions/actions";
import { updateModelWithDependencies as updateThunk } from "../redux/thunks/components";
import { useContextMenuAttributes } from "./useComponentState";
import { registerContextMenu, registerContextMenuAnchor } from "../utilities/contextMenuRegistry";

/**
 * Map the options of a context menu to the items of a PrimeReact ContextMenu
 * @param {Array} contextMenu Options declared by the component ({id, contextMenu})
 * @param {object} props {t, addActionsTop, raiseEvent, address, optionAttributes}
 * @returns {Array} Menu items
 */
export function mapContextMenu(contextMenu, props) {
  const { t, addActionsTop, raiseEvent, address, optionAttributes } = props;
  return (contextMenu || [])
    .filter(option => optionAttributes[option.id])
    .map(option => ({ ...optionAttributes[option.id], optionId: option.id }))
    .map(option => {
      const items = mapContextMenu(option.contextMenu, props);
      return {
        optionId: option.optionId,
        template: contextMenuLinkTemplate,
        label: translateLabel(option.label, t),
        icon: getIconCode(option.icon, "p-menuitem-icon"),
        disabled: option.disabled,
        visible: option.visible,
        separator: option.separator,
        command: () => {
          addActionsTop((option.actions || []).map(action => ({ ...action, address })));
          // The context option stores its click event, as AngularJS does
          raiseEvent?.({ ...address, component: option.optionId }, "click");
        },
        ...(items.length > 0 ? { items } : {})
      };
    });
}

/**
 * Context menu of a component: the options it declares with context-button open on right click
 * @param {object} params {address: owner address of the actions, contextMenu: options declared by the component,
 * openWithoutVisibleOptions: open the menu whenever the component declares options}
 * @returns {{onContextMenu: function, contextMenuTemplate: function}} onContextMenu receives the mouse event
 * (or an object with it as originalEvent) and shows the menu when it has visible options, so the browser menu
 * stays otherwise. A grid opens it whenever it declares options (openWithoutVisibleOptions): the right click selects
 * the row and the dependencies of its options resolve after the click, so they cannot be told apart from options that
 * stay hidden; contextMenuTemplate renders the menu next to the component
 */
export function useContextMenu({ address, contextMenu, openWithoutVisibleOptions = false }) {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const optionAttributes = useContextMenuAttributes(contextMenu);
  const cmRef = useRef(null);

  const addActionsTop = useCallback((actions) => dispatch(addActionsTopAction(actions)), [dispatch]);
  const raiseEvent = useCallback((addr, event) => dispatch(updateThunk(addr, { event })), [dispatch]);
  const items = useMemo(
    () => mapContextMenu(contextMenu, { t, addActionsTop, raiseEvent, address, optionAttributes }),
    [contextMenu, t, addActionsTop, raiseEvent, address, optionAttributes]);
  const canOpen = openWithoutVisibleOptions
    ? items.length > 0
    : items.some(item => item.visible !== false && !item.separator);

  const onContextMenu = useCallback((data) => {
    const event = data?.originalEvent ?? data;
    if (!canOpen || !event) {
      return;
    }
    // Nested components (a criterion in a tab) open only the menu of the closest one
    event.preventDefault?.();
    event.stopPropagation?.();
    cmRef.current?.show(event);
  }, [canOpen]);

  // The show-context-menu action opens the menu under a button: no mouse event, so its position comes from the button.
  // PrimeReact places the menu one pixel below and right of pageX/pageY
  const canOpenRef = useRef(canOpen);
  canOpenRef.current = canOpen;
  const showUnder = useCallback((anchor) => {
    if (!cmRef.current || !anchor?.getBoundingClientRect) {
      return false;
    }
    const rect = anchor.getBoundingClientRect();
    cmRef.current.show({
      pageX: rect.left + window.scrollX - 1,
      pageY: rect.bottom + window.scrollY - 1,
      preventDefault: () => null,
      stopPropagation: () => null
    });
    return true;
  }, []);
  const { view, component } = address || {};
  useEffect(
    () => registerContextMenu({ view, component }, { show: showUnder, canOpen: () => canOpenRef.current }),
    [view, component, showUnder]);

  const contextMenuTemplate = useCallback(() => items.length > 0
    ? <ContextMenu model={items} ref={cmRef} breakpoint="767px" pt={contextMenuPassThrough()} />
    : null, [items]);

  return { onContextMenu, contextMenuTemplate };
}

/**
 * Register a button as the element the show-context-menu action hangs a menu from
 * @param {object} address Address of the button (a cell address for a button of a grid row)
 * @returns {{current: Element}} Ref to give to the PrimeReact Button (it holds the button element)
 */
export function useContextMenuAnchor(address) {
  const buttonRef = useRef(null);
  const { view, component, row, column } = address || {};
  useEffect(
    () => registerContextMenuAnchor({ view, component, row, column }, () => buttonRef.current),
    [view, component, row, column]);
  return buttonRef;
}

export default useContextMenu;
