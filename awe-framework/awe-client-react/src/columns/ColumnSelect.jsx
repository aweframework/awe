import React, {Component} from "react";
import {withTranslation} from "react-i18next";
import {Dropdown} from "primereact/dropdown";
import {formatMessage} from "../utilities";
import {classNames} from "../utilities/components";

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
    const {t, placeholder, label, required, readonly, optional, model, data} = this.props;
    const classes = classNames("column-editor", {"p-invalid": data?.error});
    return <Dropdown
        value={this.state.value}
        placeholder={t(placeholder || label) + (required ? " *" : "")}
        required={required}
        disabled={readonly}
        options={model.values.map(value => ({...value, label: t(value.label)}))}
        onChange={this.onChange}
        showClear={optional}
        className={classes}
        appendTo={document.body}
        tooltip={formatMessage(data?.error, t)}
        tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
      />;
  }
}

export default withTranslation()(ColumnSelect);
