import React from "react";
import {InputText} from 'primereact/inputtext';
import {classNames} from "../utilities/components";
import {criterionPlaceholder, requiredAria} from "../utilities/labels";
import {useTranslation} from "react-i18next";
import AweCriterion from "./AweCriterion";
import useText from "../hooks/useText";
import PropTypes from "prop-types";
import useEnterKey from "../hooks/useEnterKey";
import {TestIds, testHook} from "../utilities/testIds";
import {useSigningIn} from "../components/SigninPage";

function AweInputText(props) {
  const { id } = props;
  const { t } = useTranslation();
  const { address, attributes, validationRules, value, onChange, onBlur, onSubmit } = useText(id);
  // On the sign-in page the field is disabled while the login runs
  const signingIn = useSigningIn();

  const {required, readonly, size, error } = attributes;
  const onKeyDown = useEnterKey(onSubmit);
  const classes = classNames({ [`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error });

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      <InputText
        id={address?.component}
        value={value}
        className={classes}
        placeholder={criterionPlaceholder(attributes, validationRules, t)}
        onChange={onChange}
        onBlur={onBlur}
        onKeyDown={onKeyDown}
        required={required}
        {...requiredAria(attributes, validationRules)}
        disabled={readonly || signingIn}
        {...testHook(TestIds.criterionInput)}
      />
    </AweCriterion>
  );
}

AweInputText.propTypes = {
  id: PropTypes.string,
};

export default AweInputText;
