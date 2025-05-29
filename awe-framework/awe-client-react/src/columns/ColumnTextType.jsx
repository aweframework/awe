import React, {Component} from "react";
import {InputText} from 'primereact/inputtext';
import {bindMethods, formatMessage, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import {isEmpty} from "../utilities/general";

export default class ColumnTextType extends Component {

  constructor(props, columnType) {
    super(props);

    // Bind events
    bindMethods(this, ["onChange", "onBlur", "storeChange", "onKeyDown"]);

    this.columnType = columnType;
    this.state = {value: isEmpty(this.props.data.value) ? "" : this.props.data.value};
  }

  onChange(e) {
    this.setState({value: e.target.value, writing: true});
  }

  onKeyDown(e) {
    if (e.key === "Enter") {
      this.storeChange();
    }
  }

  onBlur() {
    this.storeChange();
  }

  storeChange() {
    const {value} = this.state;
    const {address, updateModelWithDependencies, data} = this.props;
    if (data.value !== value) {
      updateModelWithDependencies(address, {values: value});
    }
    this.setState({writing: false});
  }

  render() {
    const {t, placeholder, label, required, readonly, data} = this.props;
    const {value} = this.state;
    const classes = classNames({"p-invalid": data?.error});
    return <div className={classNames("column-editor", classes)}>
      <InputText
        value={value}
        type={this.columnType}
        placeholder={translateLabel(placeholder || label, t) + (required ? " *" : "")}
        required={required}
        disabled={readonly}
        className={classNames("col-12", classes)}
        onChange={this.onChange}
        onBlur={this.onBlur}
        onKeyDown={this.onKeyDown}
        tooltip={formatMessage(data?.error, t)}
        tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
      />
    </div>;
  }
}
