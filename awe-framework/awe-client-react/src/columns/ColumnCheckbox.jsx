import React, {useCallback} from "react";
import {InputSwitch} from "primereact/inputswitch";
import {formatMessage} from "../utilities";
import {useTranslation} from "react-i18next";
import {useDispatch} from "react-redux";
import {updateModelWithDependencies} from "../redux/thunks/components";

function ColumnCheckbox(props) {
  const {required, readonly, data, address} = props;
  const {t} = useTranslation();
  const dispatch = useDispatch();

  const onChange = useCallback((e) => {
    const translatedValue = e.target.value ? 1 : 0;
    if (data.value !== translatedValue) {
      dispatch(updateModelWithDependencies(address, {values: translatedValue}));
    }
  }, [data]);

  return <InputSwitch
    checked={!!data.value}
    required={required}
    disabled={readonly}
    onChange={onChange}
    tooltip={formatMessage(data?.error, t)}
    tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
  />;
}

export default ColumnCheckbox;