import React, { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from 'react-i18next';
import { Menubar } from "primereact/menubar";
import { PanelMenu } from "primereact/panelmenu";
import { useDispatch, useSelector } from "react-redux";
import { useComponentState } from "../hooks/useComponentState";
import { addActionsTop, deleteStack } from "../redux/actions/actions";
import PropTypes from 'prop-types';

import "./AweMenu.css";
import { getIconCode, translateLabel } from "../utilities";
import { useView } from "../hooks/useViewRegistry";
import MenuRegistry from "../redux/registry/MenuRegistry";
import { useMenuOptions } from "../hooks/useMenuRegistry";
import AweMenuSearch from "./AweMenuSearch";
import { menubarPassThrough, panelMenuPassThrough } from "../utilities/testPassThrough";

// The side menu collapses to the rail on desktops only (shell.css): below, the menu is a drawer
const RAIL_QUERY = "(min-width: 1025px)";
// Time the pointer has to come back before the flyout of the rail closes, as the prototype
const FLYOUT_CLOSE_DELAY = 220;
// The flyout starts a little above its option and keeps this margin inside the window, as the prototype
const FLYOUT_OFFSET = 4;
const FLYOUT_MARGIN = 8;

/**
 * Whether the vertical menu of an element is collapsed to the rail
 * @param {Element} element Element of the menu
 * @returns {boolean} The menu is the rail
 */
function isRail(element) {
  const desktop = typeof window.matchMedia === "function" ? window.matchMedia(RAIL_QUERY).matches : true;
  return desktop && !!element?.closest(".layout-static-inactive");
}

/**
 * Top of the flyout of a first level option: aligned with the option and inside the window, as the prototype
 * @param {Element} container Menu container
 * @param {number} index Index of the option in the first level
 * @returns {number|null} Top in pixels, null when the option is not rendered
 */
function flyoutTop(container, index) {
  const panel = container?.querySelectorAll(":scope .p-panelmenu > .p-panelmenu-panel")[index];
  const header = panel?.querySelector(":scope > .p-panelmenu-header");
  if (!header) {
    return null;
  }
  const height = panel.querySelector(":scope > .p-toggleable-content")?.offsetHeight || 0;
  const top = Math.min(header.getBoundingClientRect().top - FLYOUT_OFFSET, window.innerHeight - height - FLYOUT_MARGIN);
  return Math.max(FLYOUT_MARGIN, top);
}

/**
 * Key of the first level option, with options, of the panel that holds an element
 * @param {Element} element Element of the menu
 * @param {object[]} model Items of the first level
 * @returns {string|null|undefined} Key, null when the panel has no options, undefined when the element is in no panel
 */
function flyoutPanelKey(element, model) {
  const panel = element?.closest?.(".p-panelmenu-panel");
  if (!panel) {
    return undefined;
  }
  const panels = Array.from(panel.parentElement.children).filter(child => child.classList.contains("p-panelmenu-panel"));
  const item = model[panels.indexOf(panel)];
  return item?.items ? item.key : null;
}

/**
 * Check if option has children or is a final option
 * @param option    Option to check
 * @param children  Option children
 */
function isFinalOption(option, children) {
  const { actions, menuScreen } = option;
  return menuScreen || (actions.length > 0 && children === undefined);
}

/**
 * Convert a separator option to item: with label it is a section label, without label a divider line
 * @param {object} option Separator option
 * @param {function} t Translation function
 * @returns {object} Item
 */
function getSeparatorModel(option, t) {
  const { name, label } = option;
  return label ? {
    key: name,
    label: translateLabel(label, t),
    className: `${name} p-menuitem-separator`,
    display: true,
    command: () => { }
  } : {
    key: name,
    separator: true,
    divider: true,
    disabled: true,
    className: `${name} p-menuitem-divider`,
    display: true
  };
}

/**
 * Retrieves menu options with expanded attribute set to true
 * @param {object} option Component properties
 * @returns {object} Map of expanded option IDs
 */
function findExpandedKeys(option) {
  const { options } = option;
  return options
    // Filter only arrays with elements
    .filter(optionList => optionList.options?.length)
    // Flat map to get all options and their children
    .flatMap(o => [
      // Check if the current option is expanded
      ...(o.expanded === true ? [o.id] : []),
      // Recursively check child options
      ...(findExpandedKeys(o))
    ]);
}

function getExpandedKeys(optionList) {
  // Reduce to object with ids as keys
  return findExpandedKeys(optionList)
    .reduce((expandedKeys, id) => ({
      ...expandedKeys,
      [id]: true
    }), {});
}

/**
 * Retrieve the names of the options from the first level down to the given option
 * @param {object[]} optionList Option list
 * @param {string} name Option name
 * @returns {string[]|null} Option names of the path, or null when the option is not in the list
 */
export function findOptionPath(optionList, name) {
  for (const option of optionList || []) {
    if (option.name === name) {
      return [option.name];
    }
    const path = findOptionPath(option.options, name);
    if (path) {
      return [option.name, ...path];
    }
  }
  return null;
}

/**
 * Item classes: the option of the open screen is active, and its parent groups are on the active path
 * @param {string} name Option name
 * @param {string[]} activePath Option names from the first level down to the open screen option
 * @returns {string} Item classes
 */
function getItemClassName(name, activePath) {
  const index = activePath.indexOf(name);
  if (index === -1) {
    return name;
  }
  return name + (index === activePath.length - 1 ? " p-menuitem-active" : " p-menuitem-active-path");
}

/**
 * Convert an option to item
 * @param {object} option Option data
 * @param {object} props Properties
 * @returns {object} Item
 */
function optionToItem(option, props) {
  const { t, deleteStack, addActionsTop, activePath, onLaunch } = props;
  const { separator, name, label, description, icon, options, actions } = option;
  let children = optionsToItems(options, props);
  return separator ? getSeparatorModel(option, t) : {
    name,
    key: name,
    active: name === activePath[activePath.length - 1],
    label: translateLabel(label, t),
    ...(description ? { title: translateLabel(description, t) } : {}),
    display: true,
    className: getItemClassName(name, activePath),
    icon: getIconCode(icon, "p-menuitem-icon"),
    ...(!isFinalOption(option, children) && children ? { items: children } : {}),
    ...(isFinalOption(option, children) ? {
      command: () => {
        onLaunch?.();
        deleteStack();
        addActionsTop(actions);
      }
    } : {})
  };
}

/**
 * Map options to items
 * @param {object[]} optionList Option list
 * @param {object} props Properties
 */
function optionsToItems(optionList, props) {
  const { module } = props;
  const moduleValue = (module.model.values.find(item => item.selected) || {}).value || null;
  let filtered = optionList
    .filter(option => (moduleValue === option.module || !option.module) && option.visible && !option.restricted)
    .map(option => optionToItem(option, props));

  return filtered.length === 0 ? undefined : filtered;
}

/**
 * Generate breadcrumbs for an option
 * @param optionList
 * @param breadcrumbs
 * @param props
 */
function generateBreadcrumbs(optionList, breadcrumbs, props) {
  const { t, currentOption } = props;
  return optionList
    .map((option) => {
      const { name, label, options } = option;
      let translated = translateLabel(label || currentOption.title, t);
      let newBreadcrumbs = [...breadcrumbs, { label: translated, name: name }];
      if (name === currentOption.option) {
        return newBreadcrumbs;
      } else if (options.length === 0) {
        return null;
      } else {
        return generateBreadcrumbs(options, newBreadcrumbs, props);
      }
    })
    .filter(option => option !== null)
    .reduce((old, current) => current != null ? current : old, null);
}

function AweMenu(props) {
  const { id, style = "horizontal" } = props;
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const componentModule = useComponentState('module');
  const currentOption = useView("report") || {};
  const options = useMenuOptions();
  // Menu option search is enabled unless the 'menuSearchEnabled' setting is explicitly false
  const searchEnabled = useSelector(state => state.settings.menuSearchEnabled) !== false;
  const module = componentModule?.model ? componentModule : { model: { values: [] } };

  const [expandedKeys, setExpandedKeys] = useState({});
  // Collapsed rail: the first level option whose options show as a flyout, while the pointer or the focus is on it
  const [flyoutKey, setFlyoutKey] = useState(null);
  const flyoutTimer = useRef(null);
  const flyoutKeyRef = useRef(null);
  flyoutKeyRef.current = flyoutKey;
  // Option whose flyout launched an option: it does not open again until the pointer goes to another option
  const launchedKey = useRef(null);
  const containerRef = useRef(null);

  // Launching an option of the flyout closes it, as the prototype
  const onLaunch = useCallback(() => {
    clearTimeout(flyoutTimer.current);
    launchedKey.current = flyoutKeyRef.current;
    setFlyoutKey(null);
  }, []);

  const activePath = useMemo(() => findOptionPath(options, currentOption?.option) || [], [options, currentOption?.option]);

  const helperProps = useMemo(() => ({
    t,
    currentOption,
    activePath,
    module,
    addActionsTop: (actions) => dispatch(addActionsTop(actions)),
    deleteStack: () => dispatch(deleteStack()),
    onLaunch,
  }), [t, currentOption, activePath, module, dispatch, onLaunch]);


  // Initialize expanded keys and breadcrumbs on mount
  useEffect(() => {
    const initialExpanded = getExpandedKeys({ options });
    setExpandedKeys(initialExpanded);
    // initial breadcrumbs
    if (currentOption?.option) {
      MenuRegistry.setBreadcrumbs(currentOption.option, generateBreadcrumbs(options, [], helperProps) || []);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Update breadcrumbs when current option changes
  useEffect(() => {
    if (currentOption?.option) {
      MenuRegistry.setBreadcrumbs(currentOption.option, generateBreadcrumbs(options, [], helperProps) || []);
    }
  }, [currentOption?.option, options, helperProps]);

  // Keep the groups on the path to the open screen expanded, as the AngularJS menu does
  useEffect(() => {
    const groups = activePath.slice(0, -1);
    if (groups.length > 0) {
      setExpandedKeys(expanded => groups.reduce((keys, name) => ({ ...keys, [name]: true }), expanded));
    }
  }, [activePath]);

  // The flyout of the rail only shows an option: PanelMenu reports it among its expanded keys, the user did not expand it
  const onExpand = (ek) => {
    const key = flyoutKeyRef.current;
    if (key && !expandedKeys[key]) {
      const {[key]: flyout, ...expanded} = ek;
      setExpandedKeys(expanded);
    } else {
      setExpandedKeys(ek);
    }
  };

  // Stop the pending close of the flyout when the menu goes away
  useEffect(() => () => clearTimeout(flyoutTimer.current), []);

  const menuType = style.includes("vertical") ? "vertical" : "horizontal";

  // The first level of the vertical menu draws the separators without label as divider lines; the horizontal menu
  // keeps them out of its first level
  const model = (optionsToItems(options, helperProps) || [])
    .filter(o => o.display && (menuType === "vertical" || !o.divider));

  // Place the flyout of the rail next to its option once it is rendered, when its height is known
  useLayoutEffect(() => {
    const top = flyoutKey ? flyoutTop(containerRef.current, model.findIndex(item => item.key === flyoutKey)) : null;
    if (top !== null) {
      containerRef.current.style.setProperty("--_flyout-top", `${top}px`);
    }
  });
  // Reuse the exact predicate the menu applies while rendering options, so the
  // search only surfaces options navigable in the current module.
  const moduleValue = (module.model.values.find(item => item.selected) || {}).value || null;
  const isOptionAllowed = useCallback(
    (option) => (moduleValue === option.module || !option.module) && option.visible && !option.restricted,
    [moduleValue]
  );
  // Launch a selected option exactly as clicking the leaf in the menu would
  const onSelectOption = useCallback((option) => {
    dispatch(deleteStack());
    dispatch(addActionsTop(option.actions));
  }, [dispatch]);

  const search = searchEnabled && model.length > 0 ? (
    <AweMenuSearch options={options} menuType={menuType}
      isAllowed={isOptionAllowed} onSelect={onSelectOption} />
  ) : null;

  if (menuType === "vertical") {
    // Collapsed rail, as the prototype: the options of a first level option open as a flyout while the pointer or the
    // focus is on it, without changing the options the user expanded
    const openFlyout = (event) => {
      const key = isRail(event.currentTarget) ? flyoutPanelKey(event.target, model) : undefined;
      if (key !== undefined && key !== launchedKey.current) {
        launchedKey.current = null;
        clearTimeout(flyoutTimer.current);
        setFlyoutKey(key);
      }
    };
    const closeFlyout = () => {
      launchedKey.current = null;
      clearTimeout(flyoutTimer.current);
      flyoutTimer.current = setTimeout(() => setFlyoutKey(null), FLYOUT_CLOSE_DELAY);
    };
    const onBlur = (event) => {
      if (!event.currentTarget.contains(event.relatedTarget)) {
        closeFlyout();
      }
    };
    const shownKeys = flyoutKey ? {...expandedKeys, [flyoutKey]: true} : expandedKeys;
    // Only the option of the flyout shows its options on the rail (shell.css), not the groups expanded on the path
    const shownModel = flyoutKey ? model.map(item => item.key === flyoutKey ?
      {...item, className: `${item.className} awe-menu-flyout`} : item) : model;
    return (
      <div ref={containerRef} className="awe-menu-container awe-menu-container-vertical"
        onMouseOver={openFlyout} onFocus={openFlyout} onMouseLeave={closeFlyout} onBlur={onBlur}>
        {search}
        <PanelMenu className="w-full"
          expandedKeys={shownKeys} onExpandedKeysChange={onExpand}
          model={shownModel} pt={panelMenuPassThrough(shownModel)} />
      </div>
    );
  } else {
    return (
      <div className="awe-menu-container awe-menu-container-horizontal">
        {search}
        <Menubar model={model} pt={menubarPassThrough()} />
      </div>
    );
  }
}

AweMenu.propTypes = {
  id: PropTypes.string.isRequired,
  style: PropTypes.string
};

export default AweMenu;
