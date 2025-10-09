import React, {useCallback, useRef} from "react";
import {Dropdown} from "primereact/dropdown";
import {translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import AweCriterion from "./AweCriterion";
import {useSelect} from "../hooks/useSelect";

function AweSelect(props) {

  const { id } = props;
  const {t, ref, address, attributes, validationRules, options, selected, onChange} =
    useSelect({id, multiple: false});

  const {placeholder, required, readonly, optional, size, error} = attributes;
  const classes = classNames({[`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error });
  return <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      <Dropdown
      ref={ref}
      id={address.component}
      value={selected}
      placeholder={translateLabel(placeholder, t)}
      onChange={onChange}
      required={required}
      disabled={readonly}
      className={classes}
      invalid={error}
      options={options}
      showClear={optional}
      filter={options.length > 5}
      filterBy="label"
    />
  </AweCriterion>;
}

export default AweSelect;
