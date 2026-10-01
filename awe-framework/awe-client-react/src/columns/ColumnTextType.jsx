import React, {useCallback, useEffect, useState} from "react";
import {InputText} from 'primereact/inputtext';
import {formatMessage, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import {getFirstDefinedValue, isEmpty} from "../utilities/general";
import PropTypes from "prop-types";
import {TestIds, testHook} from "../utilities/testIds";
import {updateModelWithDependencies} from "../redux/thunks/components";
import {useTranslation} from "react-i18next";
import {useDispatch} from "react-redux";

function ColumnTextType(props) {
  const { placeholder, label, required, readonly, data, attrs, style, inputType, address } = props;
  const {style: cellStyle, value: cellValue} = data;
  const {readonly: cellReadonly, validationRules = {}, visible = true, error = null} = attrs;
  const {required: cellRequired} = validationRules;

  const [value, setValue] = useState(isEmpty(cellValue) ? "" : cellValue);
  const [writing, setWriting] = useState(false);

  const { t } = useTranslation();
  const dispatch = useDispatch();


  const storeChange = useCallback(() => {
    if (cellValue !== value) {
      dispatch(updateModelWithDependencies(address, { values: value }));
    }
    setWriting(false);
  }, [address, cellValue, value]);

  const onChange = useCallback((e) => {
    if (document.activeElement !== e.target) {
      storeChange();
    } else {
      setValue(e.target.value);
      setWriting(true);
    }
  }, [storeChange]);

  const onKeyDown = useCallback((e) => {
    if (e.key === "Enter") {
      storeChange();
    }
  }, [storeChange]);

  const onBlur = useCallback(() => {
    storeChange();
  }, [storeChange]);

  // Sync local state from external data when not writing
  useEffect(() => {
    const newValue = cellValue;
    if (!writing && newValue !== value) {
      setValue(isEmpty(newValue) ? "" : newValue);
    }
  }, [cellValue, writing]);

  // Force re-render when error changes
  useEffect(() => {
    // Este useEffect se ejecutará cada vez que error cambie
    // No necesita hacer nada específico, solo asegurar el re-render
  }, [error]);


  const classes = classNames("column-editor", style, {"p-invalid": error}, {"hidden": !visible});
  return <div className={classes}>
    <InputText
      value={value}
      type={inputType || "text"}
      placeholder={translateLabel(placeholder || label, t) + (required ? " *" : "")}
      required={getFirstDefinedValue(cellRequired, required, false)}
      disabled={getFirstDefinedValue(cellReadonly, readonly, false)}
      className={classNames("w-full", cellStyle)}
      onChange={onChange}
      onBlur={onBlur}
      onKeyDown={onKeyDown}
      tooltip={formatMessage(error, t)}
      tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
      {...testHook(TestIds.criterionInput)}
    />
  </div>;
}

ColumnTextType.propTypes = {
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  attrs: PropTypes.object.isRequired,
  style: PropTypes.string,
  label: PropTypes.string,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  placeholder: PropTypes.string,
  inputType: PropTypes.string,
};

export default ColumnTextType;