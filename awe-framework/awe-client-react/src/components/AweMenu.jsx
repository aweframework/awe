import React, { useCallback, useEffect, useMemo, useState } from "react";
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

/**
 * Check if option has children or is a final option
 * @param option    Option to check
 * @param children  Option children
 */
function isFinalOption(option, children) {
  const { actions, menuScreen } = option;
  return menuScreen || (actions.length > 0 && children === undefined);
}

function getSeparatorModel(option, t) {
  const { name, label } = option;
  return label ? {
    key: name,
    label: translateLabel(label, t),
    className: "p-menuitem-separator",
    display: true,
    command: () => { }
  } : { separator: true, display: false };
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
  const { t, deleteStack, addActionsTop, activePath } = props;
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

  const activePath = useMemo(() => findOptionPath(options, currentOption?.option) || [], [options, currentOption?.option]);

  const helperProps = useMemo(() => ({
    t,
    currentOption,
    activePath,
    module,
    addActionsTop: (actions) => dispatch(addActionsTop(actions)),
    deleteStack: () => dispatch(deleteStack()),
  }), [t, currentOption, activePath, module, dispatch]);

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

  const onExpand = (ek) => setExpandedKeys(ek);

  const model = (optionsToItems(options, helperProps) || []).filter(o => o.display);

  const menuType = style.includes("vertical") ? "vertical" : "horizontal";
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
    return (
      <div className="awe-menu-container awe-menu-container-vertical">
        {search}
        <PanelMenu className="w-full md:w-20rem"
          expandedKeys={expandedKeys} onExpandedKeysChange={onExpand}
          model={model} pt={panelMenuPassThrough(model)} />
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
