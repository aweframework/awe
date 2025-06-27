import React, {Component} from "react";
import {withTranslation} from "react-i18next";
import {Dropdown} from "primereact/dropdown";
import {formatMessage, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";

class ColumnSelect extends Component {

  constructor(props) {
    super(props);
    this.state = {value: props.data.value};

    // Bind events
    this.onChange = this.onChange.bind(this);
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    const {model, data} = this.props;
    this.setState({value: (model.values.find(value => String(value.value) === String(data.value)) || {}).value});
  }

  onChange(e) {
    e.originalEvent.preventDefault();
    e.originalEvent.stopPropagation();
    const {address, updateModelWithDependencies} = this.props;
    if (this.state.value !== e.target.value) {
      updateModelWithDependencies(address, {values: e.target.value});
      this.setState({value: e.target.value});
    }
  }

  render() {
    const {t, placeholder, label, required, readonly, optional, model, data, style} = this.props;
    const classes = classNames("column-editor", {"p-invalid": data?.error}, style, data?.style);
    return <Dropdown
        value={this.state.value}
        placeholder={translateLabel(placeholder || label, t) + (required ? " *" : "")}
        required={required}
        disabled={readonly}
        options={model.values.map(value => ({...value, label: translateLabel(value.label, t)}))}
        onChange={this.onChange}
        showClear={optional}
        className={classes}
        appendTo={document.body}
        tooltip={formatMessage(data?.error, t)}
        tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
      />;
  }
}

ColumnSelect.propTypes = {
  updateModelWithDependencies: PropTypes.func.isRequired,
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  model: PropTypes.object,
  t: PropTypes.func.isRequired,
  style: PropTypes.string,
  label: PropTypes.string,
  optional: PropTypes.bool,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  placeholder: PropTypes.string,
};

export default withTranslation()(ColumnSelect);
