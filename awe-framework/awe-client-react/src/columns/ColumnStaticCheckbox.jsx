import React, {Component} from "react";
import {InputSwitch} from "primereact/inputswitch";
import PropTypes from "prop-types";

export default class ColumnStaticCheckbox extends Component {

  render() {
    const {data} = this.props;
    return <InputSwitch
      checked={!!data.value}
      disabled={true}
    />;
  }
}

ColumnStaticCheckbox.propTypes = {
  data: PropTypes.object.isRequired,
};