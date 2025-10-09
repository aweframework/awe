import React, {useEffect, useMemo, useState} from "react";
import {useTranslation} from 'react-i18next';
import {Menubar} from "primereact/menubar";
import {PanelMenu} from "primereact/panelmenu";
import {useDispatch, useSelector} from "react-redux";
import {addActionsTop, deleteStack} from "../redux/actions/actions";
import {updateBreadcrumbs} from "../redux/actions/menu";
import PropTypes from 'prop-types';

import "./AweMenu.css";
import {getIconCode, translateLabel} from "../utilities";

/**
 * Check if option has children or is a final option
 * @param option    Option to check
 * @param children  Option children
 */
function isFinalOption(option, children) {
  const {actions, menuScreen} = option;
  return menuScreen || (actions.length > 0 && children === undefined)
}

function getSeparatorModel(option, t) {
  const {name, label} = option;
  return label ? {
    key: name,
    label: translateLabel(label, t),
    className: "p-menuitem-separator",
    display: true,
    command: () => {}
  } : {separator: true, display: false};
}

/**
 * Retrieves menu options with expanded attribute set to true
 * @param {object} option Component properties
 * @returns {object} Map of expanded option IDs
 */
function findExpandedKeys(option) {
  const {options} = option;
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
      }), {})
}

/**
 * Convert an option to item
 * @param {object} option Option data
 * @param {object} props Properties
 * @returns {object} Item
 */
function optionToItem(option, props) {
  const {t, deleteStack, addActionsTop, currentOption, disabled} = props;
  const {separator, name, label, icon, options, actions} = option;
  let children = optionsToItems(options, props);
  return separator ? getSeparatorModel(option, t) : {
    name, disabled,
    key: name,
    label: translateLabel(label, t),
    display: true,
    className: name + (name === currentOption.option ? " p-menuitem-active" : ""),
    icon: getIconCode(icon, "p-menuitem-icon"),
    ...(!isFinalOption(option, children) && children ? {items: children} : {}),
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
  const {module} = props;
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
  const {t, currentOption} = props;
  return optionList
    .map((option) => {
      const {name, label, options} = option;
      let translated = translateLabel(label || currentOption.title, t);
      let newBreadcrumbs = [...breadcrumbs, {label: translated, name: name}];
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
  const { options, breadcrumbs, currentOption, disabled, module } = useSelector(state => ({
    options: state.menu.options,
    breadcrumbs: state.menu.breadcrumbs || {},
    currentOption: state.view.report || {},
    disabled: state.actions.running,
    module: state.components['module'] || { model: { values: [] } }
  }));

  const [expandedKeys, setExpandedKeys] = useState({});

  const helperProps = useMemo(() => ({
    t,
    currentOption,
    disabled,
    module,
    addActionsTop: (actions) => dispatch(addActionsTop(actions)),
    deleteStack: () => dispatch(deleteStack()),
  }), [t, currentOption, disabled, module, dispatch]);

  // Initialize expanded keys and breadcrumbs on mount
  useEffect(() => {
    const initialExpanded = getExpandedKeys({ options });
    setExpandedKeys(initialExpanded);
    // initial breadcrumbs
    if (currentOption?.option) {
      dispatch(updateBreadcrumbs(currentOption.option, generateBreadcrumbs(options, [], helperProps) || []));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Update breadcrumbs when current option changes
  useEffect(() => {
    if (currentOption?.option && breadcrumbs?.option !== currentOption.option) {
      dispatch(updateBreadcrumbs(currentOption.option, generateBreadcrumbs(options, [], helperProps) || []));
    }
  }, [currentOption?.option, breadcrumbs?.option, options, helperProps, dispatch]);

  const onExpand = (ek) => setExpandedKeys(ek);

  const model = (optionsToItems(options, helperProps) || []).filter(o => o.display);

  if (style.includes("vertical")) {
    return <PanelMenu className="w-full md:w-20rem" aria-disabled={disabled}
                      expandedKeys={expandedKeys} onExpandedKeysChange={onExpand}
                      model={model}/>;
  } else {
    return <Menubar aria-disabled={disabled} model={model}/>;
  }
}

AweMenu.propTypes = {
  id: PropTypes.string.isRequired,
  style: PropTypes.string
};

export default AweMenu;