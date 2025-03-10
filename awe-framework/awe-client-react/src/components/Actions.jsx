import React, {Component} from "react";
import "./Actions.css";
import {classNames} from "../utilities/components";

import {ActionStatus} from "../redux/actions/actions";
import {Button} from "primereact/button";

const {STATUS_RUNNING, STATUS_STARTED} = ActionStatus;

/**
 * Action component. Single action representation
 *
 * @extends React.Component
 * @category Components
 * @subcategory Actions
 */
class Action extends Component {

  /**
   * Create an action component
   * @param {object} props Action properties
   */
  constructor(props) {
    super(props);
    this.showInfo = this.showInfo.bind(this);
  }

  /**
   * Show action information
   */
  showInfo() {
    console.debug(this.props.parameters);
  }

  /**
   * Get action style depending on status
   * @param {string} status Status
   * @return {string} Action style
   */
  getStyle(status) {
    switch (status) {
      case STATUS_RUNNING:
        return "p-button-info";
      case STATUS_STARTED:
        return "p-button-warning";
      default:
        return "p-button-help";
    }
  }

  /**
   * Get action icon depending on status
   * @param {string} status Status
   * @return {string} Action icon
   */
  getIcon(status) {
    switch (status) {
      case STATUS_STARTED:
        return "pi pi-spin pi-spinner";
      case STATUS_RUNNING:
        return "pi pi-play";
      default:
        return null;
    }
  }

  /**
   * Render component
   * @returns {JSX.Element} Rendered component
   */
  render() {
    const {async, type, target, status} = this.props.parameters;
    const classes = classNames("action", "p-button-sm", {"p-button-success": async, [this.getStyle(status)]: !async});
    let icon = this.getIcon(status);
    let label = [type];
    target && label.push(`(${target})`);
    return <Button type="button" label={label.join(" ")} icon={icon} className={classes} onClick={this.showInfo}/>
  }
}

/**
 * Action stack component. Contains a stack of actions
 *
 * @extends React.Component
 * @category Components
 * @subcategory Actions
 */
class ActionStack extends Component {
  /**
   * Render component
   * @returns {JSX.Element} Rendered component
   */
  render() {
    const {elements, index} = this.props;
    let actions = elements.map((action, actionIndex) => <Action key={actionIndex} parameters={action}/>);

    return <div
      className="action-stack" style={{right: (index * 198) + "px"}}>
      {actions.length > 0 && <div className="p-tag p-tag-rounded p-tag-header">{this.props.type} stack {isNaN(index) ? "" : index + 1}</div>}
      {actions}
    </div>;
  }
}

/**
 * StackList component. Action stack list
 *
 * @extends React.Component
 * @category Components
 * @subcategory Actions
 */
export class StackList extends Component {
  /**
   * Render component
   * @returns {JSX.Element} Rendered component
   */
  render() {
    let stackList;
    const {elements, type, stacks} = this.props;
    if (elements) {
      stackList = <ActionStack type={type} elements={elements}/>
    } else if (stacks) {
      stackList = stacks.map((stack, index) => <ActionStack key={index} type={"sync"} elements={stack} index={index}/>)
    }
    return <div className={"stack " + type + "-zone"}>{stackList}</div>;
  }
}
