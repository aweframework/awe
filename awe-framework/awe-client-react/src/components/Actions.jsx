import React, {useCallback} from "react";
import "./Actions.css";
import {classNames} from "../utilities/components";

import {ActionStatus} from "../redux/actions/actions";
import {Button} from "primereact/button";

const {STATUS_RUNNING, STATUS_STARTED} = ActionStatus;

// Helpers to get style and icon based on status
const getStyle = (status) => {
  switch (status) {
    case STATUS_RUNNING:
      return "p-button-info";
    case STATUS_STARTED:
      return "p-button-warning";
    default:
      return "p-button-help";
  }
};

const getIcon = (status) => {
  switch (status) {
    case STATUS_STARTED:
      return "pi pi-spin pi-spinner";
    case STATUS_RUNNING:
      return "pi pi-play";
    default:
      return null;
  }
};

function Action({parameters}) {
  const {async, type, target, status} = parameters;
  const classes = classNames("action", "p-button-sm", {"p-button-success": async, [getStyle(status)]: !async});
  const icon = getIcon(status);
  const label = [type];
  if (target) label.push(`(${target})`);

  const showInfo = useCallback(() => {
    console.debug(parameters);
  }, [parameters]);

  return (
    <Button
      type="button"
      label={label.join(" ")}
      icon={icon}
      className={classes}
      onClick={showInfo}
    />
  );
}

function ActionStack({elements, index, type}) {
  const actions = elements.map((action, actionIndex) => (
    <Action key={actionIndex} parameters={action} />
  ));

  return (
    <div className="action-stack" style={{ right: (index * 198) + "px" }}>
      {actions.length > 0 && (
        <div className="p-tag p-tag-rounded p-tag-header">
          {type} stack {isNaN(index) ? "" : index + 1}
        </div>
      )}
      {actions}
    </div>
  );
}

export function StackList({elements, type, stacks}) {
  let stackList;
  if (elements) {
    stackList = <ActionStack type={type} elements={elements} />;
  } else if (stacks) {
    stackList = stacks.map((stack, index) => (
      <ActionStack key={index} type={"sync"} elements={stack} index={index} />
    ));
  }
  return <div className={"stack " + type + "-zone"}>{stackList}</div>;
}
