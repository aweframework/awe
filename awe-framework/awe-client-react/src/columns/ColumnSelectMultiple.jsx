import React, {useMemo} from "react";
import {MultiSelect} from "primereact/multiselect";
import {formatMessage, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";
import {multiSelectPassThrough} from "../utilities/testPassThrough";
import {useSelect, selectItemTemplate} from "../hooks/useSelect";
import {compareEqualValues, getFirstDefinedValue} from "../utilities/general";
import {selectChipTemplate} from "../criteria/AweSelectChip";

function ColumnSelectMultiple(props) {
  const {placeholder, label, required, readonly, optional, model, data, attrs, style, address} = props;
  const cellStyle = Array.isArray(data) ? null : data?.style;
  const {readonly: cellReadonly, validationRules = {}, visible = true, error = null} = attrs;
  const {required: cellRequired} = validationRules;
  const selectedValues = useMemo(() => {
    if (Array.isArray(data)) {
      const selectedItems = data.filter(item => item?.selected);
      const items = selectedItems.length > 0 ? selectedItems : data;
      return items
        .map(item => item?.value)
        .filter(value => value !== undefined && value !== null);
    }
    if (Array.isArray(data?.value)) {
      return data.value;
    }
    if (data?.value !== undefined && data?.value !== null) {
      return [data.value];
    }
    return [];
  }, [data]);
  const cellModel = useMemo(() => ({
    values: (model?.values ?? [])
      .map(v => ({...v, selected: selectedValues.some(value => compareEqualValues(value, v.value))}))
  }), [model?.values, selectedValues]);
  const {t, options, groupProps, grouped, selected, onChange, removeValue} =
    useSelect({ model: cellModel, address, multiple: true});
  const disabled = getFirstDefinedValue(cellReadonly, readonly, false);
  const classes = classNames(style, cellStyle, "column-editor", {"p-invalid": error}, {"hidden": !visible});
  const editorPlaceholder = translateLabel(placeholder || label, t) + (required ? " *" : "");
  return (
    <MultiSelect
      value={selected}
      placeholder={editorPlaceholder}
      required={getFirstDefinedValue(cellRequired, required, false)}
      disabled={disabled}
      options={options}
      {...groupProps}
      itemTemplate={selectItemTemplate}
      dataKey="code"
      showSelectAll={!grouped}
      onChange={onChange}
      showClear={optional}
      className={classes}
      display="chip"
      selectedItemTemplate={selectChipTemplate(disabled, removeValue, editorPlaceholder)}
      resetFilterOnHide={true}
      filter
      invalid={error}
      optionLabel="name"
      appendTo={document.body}
      pt={multiSelectPassThrough(address.component)}
      tooltip={formatMessage(error, t)}
      tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
    />
  );
}

ColumnSelectMultiple.propTypes = {
  address: PropTypes.object.isRequired,
  data: PropTypes.oneOfType([PropTypes.object, PropTypes.array]).isRequired,
  attrs: PropTypes.object.isRequired,
  model: PropTypes.object,
  style: PropTypes.string,
  label: PropTypes.string,
  optional: PropTypes.bool,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  placeholder: PropTypes.string,
};

export default ColumnSelectMultiple;
