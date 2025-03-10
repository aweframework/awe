import React, {Component} from "react";
import {withTranslation} from 'react-i18next';
import {Menubar} from "primereact/menubar";
import {PanelMenu} from "primereact/panelmenu";
import {connect} from "react-redux";
import {addActionsTop, deleteStack} from "../redux/actions/actions";
import {updateBreadcrumbs} from "../redux/actions/screen";
import PropTypes from 'prop-types';

import "./AweMenu.css";
import {bindMethods, getIconCode, translateLabel} from "../utilities";

/**
 * Check if option has children or is a final option
 * @param option    Option to check
 * @param children  Option children
 */
function isFinalOption(option, children) {
  const {actions, menuScreen} = option;
  return menuScreen || (actions.length > 0 && children === undefined)
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
  return separator ? {separator: true} : {
    name, disabled,
    key: name,
    label: translateLabel(label, t),
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
    .filter(option => (moduleValue === option.module || !option.module) && option.visible && !option.restricted && !option.separator)
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
      let newBreadcrumbs = breadcrumbs.concat({label: translated, name: name});
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

class AweMenu extends Component {

  /**
   * Create a grid
   * @param {object} props Grid properties
   */
  constructor(props) {
    super(props);
    bindMethods(this, ["onExpand"]);
  }

  onExpand(expandedKeys) {
    this.setState({...this.state, expandedKeys})
  }

  onUpdateBreadcrumbs(props) {
    const {currentOption, breadcrumbs, updateBreadcrumbs, options} = props;
    if (currentOption.option && breadcrumbs.option !== currentOption.option) {
      // Update breadcrumbs
      updateBreadcrumbs(currentOption.option, generateBreadcrumbs(options, [], props) || []);
    }
  }


  /**
   * Component was updated
   * @param {object} prevProps Previous props
   * @param {object} prevState Previous state
   * @param {object} snapshot Current snapshot
   */
  componentDidUpdate(prevProps, prevState, snapshot) {
    this.onUpdateBreadcrumbs(this.props);
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    const expandedKeys = {};
    this.onUpdateBreadcrumbs(this.props);
    this.setState({expandedKeys});
  }

  render() {
    const {options, disabled = false, style = "horizontal"} = this.props;
    const {expandedKeys = []} = this.state || {};
    if (style.includes("vertical")) {
      return <PanelMenu className="w-full md:w-20rem" aria-disabled={disabled}
        expandedKeys={expandedKeys} onExpandedKeysChange={this.onExpand}
        model={optionsToItems(options, this.props)}/>;
    } else {
      return <Menubar aria-disabled={disabled}
        model={optionsToItems(options, this.props)}/>;
    }
  }
}

AweMenu.propTypes = {
  id: PropTypes.string.isRequired,
  style: PropTypes.string,
  status: PropTypes.object,
  options: PropTypes.array.isRequired,
  breadcrumbs: PropTypes.oneOfType([PropTypes.array, PropTypes.object]),
  currentOption: PropTypes.object.isRequired,
  disabled: PropTypes.bool.isRequired,
  module: PropTypes.object
};

/**
 * Map state to props
 * @param state State
 * @param ownProps Properties
 * @returns {object} Properties to map
 */
function mapStateToProps(state, ownProps) {
  const {id, style} = ownProps;
  return {
    id,
    style,
    status: state.menu.status,
    options: state.menu.options,
    breadcrumbs: state.screen.breadcrumbs || [],
    currentOption: state.screen.report || {},
    disabled: state.actions.running,
    module: state.components['module'] || {model: {values: []}},
  };
}

/**
 * Connect component to redux store
 * @param component Component to connect
 * @returns {function} connect method
 */
const connectComponent = (component) => connect(mapStateToProps, {
  addActionsTop,
  deleteStack,
  updateBreadcrumbs
})(withTranslation()(component));

export default connectComponent(AweMenu);
