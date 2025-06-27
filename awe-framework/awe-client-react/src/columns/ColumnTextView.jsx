import React, {Component} from "react";
import {Badge} from 'primereact/badge';
import {withTranslation} from "react-i18next";
import {bindMethods, getIconCode, translateLabel} from "../utilities";
import "./ColumnTextView.less";
import {classNames, getVisibleTextData} from "../utilities/components";
import PropTypes from "prop-types";

class ColumnTextView extends Component {

  constructor(props) {
    super(props);

    // Bind events
    bindMethods(this, ["onClick", "onKeyDown", "onAction"]);
  }

  onClick() {
    this.onAction("click");
  }

  onKeyDown() {
    this.onAction("keyDown");
  }

  onAction(event) {
    const {address, updateModelWithDependencies, actions, addActionsTop} = this.props;
    // Change event
    updateModelWithDependencies(address, {event});

    if (actions.length > 0) {
      addActionsTop(actions.map(action => ({...action, address: {...address}})));
    }
  }

  render() {
    const {t, data, align, style} = this.props;
    const {title, label, value} = data;
    const icon = data.icon || this.props.icon || undefined;
    const unit = data.unit || this.props.unit || null;
    const classes = classNames("text-view", style, data?.style);
    return <button className={classes} title={translateLabel(title || label, t)} onClick={this.onClick} onKeyDown={this.onKeyDown}>
      <span className={"text-view-icon"}>{getIconCode(icon, "fa-fw")}</span>
      <span className={"text-view-text"} style={{textAlign: align}}>{getVisibleTextData(label || value, t)}</span>
      {unit && <Badge value={translateLabel(unit, t)} severity="secondary" style={{justifyContent: "center"}}/>}
    </button>;
  }
}

ColumnTextView.propTypes = {
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  align: PropTypes.string,
  icon: PropTypes.string,
  actions: PropTypes.array,
  style: PropTypes.string,
  unit: PropTypes.string,
  t: PropTypes.func.isRequired,
  updateModelWithDependencies: PropTypes.func.isRequired,
  addActionsTop: PropTypes.func.isRequired
};

export default withTranslation()(ColumnTextView);
