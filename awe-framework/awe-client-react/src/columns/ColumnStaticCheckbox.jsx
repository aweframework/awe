import React from "react";
import {InputSwitch} from "primereact/inputswitch";
import PropTypes from "prop-types";

function ColumnStaticCheckbox(props) {
  const {data} = props;
  return <InputSwitch
    checked={!!data.value}
    disabled={true}
  />;
}

ColumnStaticCheckbox.propTypes = {
  data: PropTypes.oneOfType([PropTypes.object, PropTypes.array]),
};

export default ColumnStaticCheckbox;