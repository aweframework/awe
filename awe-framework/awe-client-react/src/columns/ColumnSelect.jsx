import React, {useMemo} from "react";
import {Dropdown} from "primereact/dropdown";
import {formatMessage, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";
import {useSelect} from "../hooks/useSelect";
import {compareEqualValues} from "../utilities/general";

function ColumnSelect(props) {
  const {placeholder, label, required, readonly, optional, model, data, style, address} = props;
  const cellModel = useMemo( () => ({ values: (model?.values ?? [])
      .map(v => ({...v, selected: compareEqualValues(v.value, data.value)}))}),
    [model?.values, data.value]);
  const {t, options, selected, onChange} =
    useSelect({ model: cellModel, address, multiple: false});
  const classes = classNames("column-editor", {"p-invalid": data?.error}, style, data?.style);
  return (
    <Dropdown
      value={selected}
      placeholder={translateLabel(placeholder || label, t) + (required ? " *" : "")}
      required={required}
      disabled={readonly}
      options={options}
      onChange={onChange}
      showClear={optional}
      className={classes}
      invalid={data?.error}
      appendTo={document.body}
      tooltip={formatMessage(data?.error, t)}
      tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
    />
  );
}

ColumnSelect.propTypes = {
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  model: PropTypes.object,
  style: PropTypes.string,
  label: PropTypes.string,
  optional: PropTypes.bool,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  placeholder: PropTypes.string,
};

export default ColumnSelect;
