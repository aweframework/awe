import React from "react";
import {InputText} from "primereact/inputtext";
import {classNames} from "../utilities/components";
import {translateLabel} from "../utilities";
import {useTranslation} from "react-i18next";
import AweCriterion from "./AweCriterion";
import useText from "../hooks/useText";
import PropTypes from "prop-types";
import {TestIds, testHook} from "../utilities/testIds";

function AweInputPassword(props) {
  const { id } = props;
  const { t } = useTranslation();
  const { address, attributes, validationRules, value, onChange, onBlur, onSubmit } = useText(id);

  const { placeholder, required, readonly, size, error } = attributes;
  const classes = classNames({ [`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error });

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      <InputText
        id={address?.component}
        type="password"
        value={value}
        className={classes}
        placeholder={translateLabel(placeholder, t)}
        onChange={onChange}
        onBlur={onBlur}
        onKeyDown={e => e.key === "Enter" && onSubmit()}
        required={required}
        disabled={readonly}
        {...testHook(TestIds.criterionInput)}
      />
    </AweCriterion>
  );
}

AweInputPassword.propTypes = {
  id: PropTypes.string,
};

export default AweInputPassword;
