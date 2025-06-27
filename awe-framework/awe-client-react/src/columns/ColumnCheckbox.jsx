import React, {Component} from "react";
import {InputSwitch} from "primereact/inputswitch";
import {formatMessage} from "../utilities";
import PropTypes from "prop-types";

class ColumnCheckbox extends Component {

  constructor(props) {
    super(props);

    // Bind events
    this.onChange = this.onChange.bind(this);
  }

  onChange(e) {
    const {address, updateModelWithDependencies, data} = this.props;
    const translatedValue = e.target.value ? 1 : 0;
    if (data.value !== translatedValue) {
      updateModelWithDependencies(address, {values: translatedValue});
    }
  }

  render() {
    const {t, required, readonly, data} = this.props;
    return <InputSwitch
      checked={!!data.value}
      required={required}
      disabled={readonly}
      onChange={this.onChange}
      tooltip={formatMessage(data?.error, t)}
      tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
    />;
  }
}

ColumnCheckbox.propTypes = {
  updateModelWithDependencies: PropTypes.func.isRequired,
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  required: PropTypes.bool,
  readonly: PropTypes.bool,
  t: PropTypes.func.isRequired
};

export default ColumnCheckbox;
