import React from "react";
import {useTranslation} from "react-i18next";
import {RadioButton} from "primereact/radiobutton";
import {classNames} from "../utilities/components";
import {translateLabel} from "../utilities";
import useCheckboxRadio from "../hooks/useCheckboxRadio";
import AweCriterion from "./AweCriterion";
import PropTypes from "prop-types";

function AweInputRadio(props) {
  const { id } = props;
  const { t } = useTranslation();
  const { address, attributes, validationRules, getChecked, onChangeRadio } = useCheckboxRadio(id);

  const { placeholder, required, readonly, label, group, size, error = false } = attributes;
  const classes = classNames({ [`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error });

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules} generateLabel={false}>
      <div className="field-radiobutton">
        <RadioButton
          inputId={address?.component}
          name={group}
          checked={getChecked()}
          placeholder={translateLabel(placeholder, t)}
          onChange={onChangeRadio}
          required={required}
          disabled={readonly}
          className={classes}
        />
        <label className={"cursor-pointer"} htmlFor={address?.component}>{translateLabel(label, t)}</label>
      </div>
    </AweCriterion>
  );
}

AweInputRadio.propTypes = {
  id: PropTypes.string,
};

export default AweInputRadio;
