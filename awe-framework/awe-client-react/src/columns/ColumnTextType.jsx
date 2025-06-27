import React, {Component} from "react";
import {InputText} from 'primereact/inputtext';
import {bindMethods, formatMessage, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import {isEmpty} from "../utilities/general";
import PropTypes from "prop-types";

export default class ColumnTextType extends Component {

  constructor(props, columnType) {
    super(props);

    // Bind events
    bindMethods(this, ["onChange", "onBlur", "storeChange", "onKeyDown"]);

    this.columnType = columnType;
    this.state = {value: isEmpty(this.props.data.value) ? "" : this.props.data.value};
  }

  onChange(e) {
    if (document.activeElement !== e.target) {
      this.storeChange();
    } else {
      this.setState({value: e.target.value, writing: true});
    }
  }

  getValue() {
    const {data = {}} = this.props;
    return data.value;
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

  /**
   * Component was updated
   * @param {object} _prevProps Previous props
   * @param {object} _prevState Previous state
   * @param {object} _snapshot Current snapshot
   */
  componentDidUpdate(_prevProps, _prevState, _snapshot) {
    let newValue = this.getValue();
    if (newValue !== this.state.value && !this.state.writing) {
      this.setState({value: newValue});
    }
  }

  render() {
    const {t, placeholder, label, required, readonly, data, style} = this.props;
    const {value} = this.state;
    const classes = classNames(style, data?.style, {"p-invalid": data?.error});
    return <div className={"column-editor"}>
      <InputText
        value={value}
        type={this.columnType}
        placeholder={translateLabel(placeholder || label, t) + (required ? " *" : "")}
        required={required}
        disabled={readonly}
        className={classNames("w-full", classes)}
        onChange={this.onChange}
        onBlur={this.onBlur}
        onKeyDown={this.onKeyDown}
        tooltip={formatMessage(data?.error, t)}
        tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
      />
    </div>;
  }
}

ColumnTextType.propTypes = {
  updateModelWithDependencies: PropTypes.func.isRequired,
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  t: PropTypes.func.isRequired,
  style: PropTypes.string,
  label: PropTypes.string,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  placeholder: PropTypes.string,
};