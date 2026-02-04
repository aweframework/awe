import React, {useCallback} from "react";
import {useTranslation} from "react-i18next";
import {Checkbox} from "primereact/checkbox";
import {classNames} from "../utilities/components";
import {InputSwitch} from "primereact/inputswitch";
import {translateLabel} from "../utilities";
import useCheckboxRadio from "../hooks/useCheckboxRadio";
import AweCriterion from "./AweCriterion";
import PropTypes from "prop-types";

function AweInputCheckbox(props) {
  const { id } = props;
  const { t } = useTranslation();
  const { address, attributes, validationRules, getChecked, onChangeCheckbox } = useCheckboxRadio(id);

  const { style = "", placeholder, required, readonly, label, size, error = false } = attributes;
  const isSwitch = (style || "").includes("switch");
  const classes = classNames("", { [`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error });

  const onChangeSwitch = useCallback((e) => {
    onChangeCheckbox({ target: { ...e.target, checked: e.target.value } });
  }, [onChangeCheckbox]);

  if (isSwitch) {
    return (
      <AweCriterion address={address} attributes={attributes} validationRules={validationRules} generateLabel={false}>
        <div className="field-checkbox">
          <InputSwitch
            inputId={address?.component}
            checked={getChecked()}
            placeholder={translateLabel(placeholder, t)}
            onChange={onChangeSwitch}
            required={required}
            disabled={readonly}
            className={classes}
          />
          <label className={"cursor-pointer"} htmlFor={address?.component}>{translateLabel(label, t)}</label>
        </div>
      </AweCriterion>
    );
  }

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules} generateLabel={false}>
      <div className="field-checkbox">
        <Checkbox
          inputId={address?.component}
          checked={getChecked()}
          placeholder={translateLabel(placeholder, t)}
          onChange={onChangeCheckbox}
          required={required}
          disabled={readonly}
          className={classes}
        />
        <label className={"cursor-pointer"} htmlFor={address?.component}>{translateLabel(label, t)}</label>
      </div>
    </AweCriterion>
  );
}

AweInputCheckbox.propTypes = {
  id: PropTypes.string,
};

export default AweInputCheckbox;
