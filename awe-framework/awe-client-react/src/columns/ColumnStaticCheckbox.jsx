import React from "react";
import {InputSwitch} from "primereact/inputswitch";

function ColumnStaticCheckbox(props) {
  const {data} = props;
  return <InputSwitch
    checked={!!data.value}
    disabled={true}
  />;
}

export default ColumnStaticCheckbox;