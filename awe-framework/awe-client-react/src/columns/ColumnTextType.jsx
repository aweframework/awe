import React, {useCallback, useEffect, useState} from "react";
import {InputText} from 'primereact/inputtext';
import {formatMessage, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import {isEmpty} from "../utilities/general";
import PropTypes from "prop-types";

function ColumnTextType(props) {
  const { t, placeholder, label, required, readonly, data, style, inputType, address, updateModelWithDependencies } = props;

  const [value, setValue] = useState(isEmpty(data?.value) ? "" : data.value);
  const [writing, setWriting] = useState(false);

  const storeChange = useCallback(() => {
    if (data.value !== value) {
      updateModelWithDependencies(address, { values: value });
    }
    setWriting(false);
  }, [address, data.value, updateModelWithDependencies, value]);

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
    const newValue = data?.value;
    if (!writing && newValue !== value) {
      setValue(isEmpty(newValue) ? "" : newValue);
    }
  }, [data?.value, writing, value]);

  const classes = classNames(style, data?.style, {"p-invalid": data?.error});

  return <div className={"column-editor"}>
    <InputText
      value={value}
      type={inputType || "text"}
      placeholder={translateLabel(placeholder || label, t) + (required ? " *" : "")}
      required={required}
      disabled={readonly}
      className={classNames("w-full", classes)}
      onChange={onChange}
      onBlur={onBlur}
      onKeyDown={onKeyDown}
      tooltip={formatMessage(data?.error, t)}
      tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
    />
  </div>;
}

ColumnTextType.propTypes = {
  updateModelWithDependencies: PropTypes.func.isRequired,
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  t: PropTypes.func.isRequired,
  style: PropTypes.string,
  label: PropTypes.string,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  placeholder: PropTypes.string,
  inputType: PropTypes.string,
};

export default ColumnTextType;