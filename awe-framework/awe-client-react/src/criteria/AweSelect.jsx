import React from "react";
import {Dropdown} from "primereact/dropdown";
import {translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import AweCriterion from "./AweCriterion";
import {useSelect} from "../hooks/useSelect";
import {Skeleton} from "primereact/skeleton";
import PropTypes from "prop-types";

function AweSelect(props) {

  const { id, style: propsStyle } = props;
  const {t, ref, address, attributes, validationRules, options, selected, onChange} =
    useSelect({id, multiple: false});

  // If address is undefined, return skeleton
  if (!address) {
    return <Skeleton width="10rem" height="2rem" style={propsStyle}/>;
  }

  const {placeholder, required, readonly, optional, size, error} = attributes;
  const classes = classNames({[`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error });
  return <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      <Dropdown
      ref={ref}
      id={id}
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

AweSelect.propTypes = {
  id: PropTypes.string,
  style: PropTypes.string
};

export default AweSelect;
