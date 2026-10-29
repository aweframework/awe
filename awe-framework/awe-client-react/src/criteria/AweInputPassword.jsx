import React, {useEffect, useRef, useState} from "react";
import {useDispatch} from "react-redux";
import {criterionPlaceholder, requiredAria} from "../utilities/labels";
import {InputText} from "primereact/inputtext";
import {classNames} from "../utilities/components";
import {getIconNode, translateLabel} from "../utilities";
import {useTranslation} from "react-i18next";
import AweCriterion from "./AweCriterion";
import useText from "../hooks/useText";
import useCapsLock from "../hooks/useCapsLock";
import PropTypes from "prop-types";
import useEnterKey from "../hooks/useEnterKey";
import {TestIds, testHook} from "../utilities/testIds";
import {useInSigninPage, useSigninAlert, useSigningIn} from "../components/SigninPage";
import {updateModelWithDependencies} from "../redux/thunks/components";

function AweInputPassword(props) {
  const { id } = props;
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { address, attributes, validationRules, value, setValue, onChange, onBlur, onSubmit } = useText(id);
  const capsLockOn = useCapsLock();
  // Every password has a show / hide button at the end of the field. On the sign-in page it is drawn as the prototype
  // (the sign-in toggle) with a Caps Lock warning under the field
  const signin = useInSigninPage();
  // and the field is disabled while the login runs
  const signingIn = useSigningIn();
  const [shown, setShown] = useState(false);
  const input = useRef(null);

  // A failed login on the sign-in page clears the password and gives it the focus, to type it again
  const alertId = useSigninAlert()?.id;
  useEffect(() => {
    if (alertId !== undefined && address) {
      setValue("");
      dispatch(updateModelWithDependencies(address, { values: [{ value: "", selected: true }] }));
      input.current?.focus();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [alertId]);

  const {required, readonly, size, error } = attributes;
  const onKeyDown = useEnterKey(onSubmit);
  const capsId = `${address?.component}-caps`;
  const capsWarning = signin && capsLockOn;
  const classes = classNames({ [`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error,
    "caps-lock": capsLockOn && !signin, "signin-password": signin, "password-field": !signin });

  const note = capsWarning && <small id={capsId} className="help help--warning signin-caps">
    {getIconNode("lucide:triangle-alert", {"aria-hidden": "true"})}
    <span>{t("SCREEN_TEXT_CAPS_LOCK_ON")}</span>
  </small>;

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules} note={note}>
      <InputText
        ref={input}
        id={address?.component}
        type={shown ? "text" : "password"}
        value={value}
        className={classes}
        placeholder={criterionPlaceholder(attributes, validationRules, t)}
        onChange={onChange}
        onBlur={onBlur}
        onKeyDown={onKeyDown}
        required={required}
        {...requiredAria(attributes, validationRules)}
        disabled={readonly || signingIn}
        aria-describedby={capsWarning ? capsId : undefined}
        {...testHook(TestIds.criterionInput)}
      />
      {/* The type of the input is only visual: the value, the validation and the model do not change */}
      <button type="button" className={signin ? "signin-password-toggle" : "password-toggle"} aria-controls={address?.component}
                         aria-pressed={shown} disabled={readonly || signingIn} onClick={() => setShown(show => !show)}
                         aria-label={t(shown ? "SCREEN_TEXT_HIDE_PASSWORD" : "SCREEN_TEXT_SHOW_PASSWORD")}>
        {getIconNode(shown ? "lucide:eye-off" : "lucide:eye", {"aria-hidden": "true"})}
      </button>
      {capsLockOn && !signin && getIconNode("pi-arrow-circle-up", {className: "caps-lock-icon", "aria-hidden": "true"})}
    </AweCriterion>
  );
}

AweInputPassword.propTypes = {
  id: PropTypes.string,
};

export default AweInputPassword;
