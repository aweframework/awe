import React, {useCallback} from "react";
import {InputSwitch} from "primereact/inputswitch";
import {formatMessage} from "../utilities";
import {useTranslation} from "react-i18next";
import {useDispatch} from "react-redux";
import {updateModelWithDependencies} from "../redux/thunks/components";
import {getFirstDefinedValue} from "../utilities/general";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";
import {TestIds, testHook} from "../utilities/testIds";

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

  const checked = !!cellValue;
  const classes = classNames(style, cellStyle, {"p-invalid": error}, {"hidden": !visible});

  return <InputSwitch
    className={classes}
    checked={checked}
    required={getFirstDefinedValue(cellRequired, required, false)}
    disabled={getFirstDefinedValue(cellReadonly, readonly, false)}
    onChange={onChange}
    pt={{ root: testHook(TestIds.criterionInput, { selected: checked }) }}
    tooltip={formatMessage(error, t)}
    tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
  />;
}

ColumnCheckbox.propTypes = {
  address: PropTypes.object,
  attrs: PropTypes.object,
  data: PropTypes.oneOfType([PropTypes.object, PropTypes.array]),
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  style: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
};

export default ColumnCheckbox;