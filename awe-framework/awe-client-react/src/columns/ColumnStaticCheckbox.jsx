import React, {Component} from "react";
import {InputSwitch} from "primereact/inputswitch";

export default class ColumnStaticCheckbox extends Component {

  render() {
    const {data} = this.props;
    return <InputSwitch
      checked={!!data.value}
      disabled={true}
    />;
  }
}