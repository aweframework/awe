import React, {useMemo} from "react";
import {Dropdown} from "primereact/dropdown";
import {formatMessage, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";
import {dropdownPassThrough, selectValueTemplate} from "../utilities/testPassThrough";
import {useSelect} from "../hooks/useSelect";
import {compareEqualValues, getFirstDefinedValue} from "../utilities/general";

function ColumnSelect(props) {
  const {placeholder, label, required, readonly, optional, model, data, attrs, style, address} = props;
  const {style: cellStyle, value: cellValue} = data;
  const {readonly: cellReadonly, validationRules = {}, visible = true, error = null} = attrs;
  const {required: cellRequired} = validationRules;
  const cellModel = useMemo( () => ({ values: (model?.values ?? [])
      .map(v => ({...v, selected: compareEqualValues(v.value, cellValue)}))}),
    [model?.values, cellValue]);
  const {t, options, selected, onChange} =
    useSelect({ model: cellModel, address, multiple: false});
  const classes = classNames(style, cellStyle, "column-editor", {"p-invalid": data?.error}, {"hidden": !visible});
  return (
    <Dropdown
      value={selected}
      placeholder={translateLabel(placeholder || label, t) + (required ? " *" : "")}
      required={getFirstDefinedValue(cellRequired, required, false)}
      disabled={getFirstDefinedValue(cellReadonly, readonly, false)}
      options={options}
      onChange={onChange}
      showClear={optional}
      className={classes}
      invalid={error}
      appendTo={document.body}
      valueTemplate={selectValueTemplate}
      pt={dropdownPassThrough(address.component)}
      tooltip={formatMessage(error, t)}
      tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
    />
  );
}

ColumnSelect.propTypes = {
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  attrs: PropTypes.object.isRequired,
  model: PropTypes.object,
  style: PropTypes.string,
  label: PropTypes.string,
  optional: PropTypes.bool,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  placeholder: PropTypes.string,
};

export default ColumnSelect;
