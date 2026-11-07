import React from "react";
import {criterionPlaceholder, requiredAria} from "../utilities/labels";
import {MultiSelect} from "primereact/multiselect";
import {classNames} from "../utilities/components";
import AweCriterion from "./AweCriterion";
import AweSelectMultipleChips from "./AweSelectMultipleChips";
import {useSelect, selectItemTemplate} from "../hooks/useSelect";
import PropTypes from "prop-types";
import {multiSelectPassThrough} from "../utilities/testPassThrough";
import {selectChipTemplate} from "./AweSelectChip";

function AweSelectMultiple(props) {

  const {id} = props;
  const {t, ref, address, attributes, validationRules, options, groupProps, grouped, selected, onChange, removeValue} =
    useSelect({id, multiple: true});

  const {required, readonly, optional, size, error, display} = attributes;
  const placeholder = criterionPlaceholder(attributes, validationRules, t, false);
  // display="chip" keeps the chips of PrimeReact, one remove icon each, which grow with the values. By default the field
  // stays one control high: the chips that fit, then a counter that lists them all (#922)
  const oneLine = display !== "chip";
  const classes = classNames({
    [`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error,
    "awe-select-multiple": oneLine, "awe-select-multiple-clearable": oneLine && optional && selected?.length
  });
  // A readonly field lists its values without offering to remove them
  const chipsTemplate = value => value && value.code === selected[0].code
    ? <AweSelectMultipleChips values={selected} onRemove={readonly ? undefined : removeValue}/>
    : null;
  return <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      <MultiSelect
        ref={ref}
        inputId={id}
        value={selected}
        placeholder={placeholder}
        onChange={onChange}
        required={required}
        disabled={readonly}
        className={classes}
        display={oneLine ? "comma" : "chip"}
        selectedItemTemplate={oneLine ? chipsTemplate : selectChipTemplate(readonly, removeValue, placeholder)}
        resetFilterOnHide={true}
        filter
        invalid={error}
        optionLabel="name"
        options={options}
        {...groupProps}
        itemTemplate={selectItemTemplate}
        dataKey="code"
        showSelectAll={!grouped}
        showClear={optional}
        pt={{...multiSelectPassThrough(id), input: requiredAria(attributes, validationRules)}}
      />
    </AweCriterion>;
}

AweSelectMultiple.propTypes = {
  id: PropTypes.string,
};

export default AweSelectMultiple;
