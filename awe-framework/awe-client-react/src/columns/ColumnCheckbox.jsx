import React, {useCallback} from "react";
import {InputSwitch} from "primereact/inputswitch";
import {formatMessage} from "../utilities";
import {useTranslation} from "react-i18next";
import {useDispatch} from "react-redux";
import {updateModelWithDependencies} from "../redux/thunks/components";
import {getFirstDefinedValue} from "../utilities/general";
import {classNames} from "../utilities/components";

function ColumnCheckbox(props) {
  const {required, readonly, style, data, attrs, address} = props;
  const {style: cellStyle, value: cellValue} = data;
  const {readonly: cellReadonly, validationRules = {}, visible = true, error = null} = attrs;
  const {required: cellRequired} = validationRules;
  const {t} = useTranslation();
  const dispatch = useDispatch();

  const onChange = useCallback((e) => {
    const translatedValue = e.target.value ? 1 : 0;
    if (cellValue !== translatedValue) {
      dispatch(updateModelWithDependencies(address, {values: translatedValue}));
    }
  }, [data]);

  const classes = classNames(style, cellStyle, {"p-invalid": error}, {"hidden": !visible});

  return <InputSwitch
    className={classes}
    checked={!!cellValue}
    required={getFirstDefinedValue(cellRequired, required, false)}
    disabled={getFirstDefinedValue(cellReadonly, readonly, false)}
    onChange={onChange}
    tooltip={formatMessage(error, t)}
    tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
  />;
}

export default ColumnCheckbox;