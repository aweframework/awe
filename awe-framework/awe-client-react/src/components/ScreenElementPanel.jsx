import React from "react";
import PropTypes from "prop-types";
import {useTranslation} from "react-i18next";
import {Button} from "primereact/button";
import {Dropdown} from "primereact/dropdown";
import {InputSwitch} from "primereact/inputswitch";
import {InputText} from "primereact/inputtext";
import {translateLabel} from "../utilities";
import {attributeLabelKey, orderAttributes, ownRule, resolveValue, validateValue} from "../utilities/screenPersonalization";
import "./ScreenElementPanel.less";

/**
 * Identity of the selected element: its label, its type and the scope it is personalized for
 * @returns {React.ReactElement} Header of the side panel
 */
export function ScreenElementPanelHeader({element, scopeLabel}) {
  const {t} = useTranslation();
  return (
    <div className="panel-identity">
      <div className="panel-identity-text">
        <h2 className="panel-identity-title">{element.label || element.id}</h2>
        <p className="panel-identity-subtitle">{element.elementType} · {t("SCR_PER_FOR")} {scopeLabel}</p>
      </div>
    </div>
  );
}

ScreenElementPanelHeader.propTypes = {element: PropTypes.object, scopeLabel: PropTypes.string};

const TRANSLATABLE = ["label", "title", "placeholder"];
const ORIGIN_KEYS = {default: "SCR_PER_ORIGIN_DEFAULT", everyone: "SCR_PER_ORIGIN_EVERYONE", profile: "SCR_PER_ORIGIN_PROFILE", user: "SCR_PER_ORIGIN_USER"};

/** Editor of one attribute, by value type */
function ValueEditor({item, value, label, invalid, onChange}) {
  const props = {id: `scr-per-${item.attribute}`, "aria-label": label, "aria-invalid": invalid || undefined};
  switch (item.valueType) {
    case "BOOLEAN":
      return <InputSwitch {...props} checked={/^true$/i.test(value)} onChange={event => onChange(String(event.value))}/>;
    case "ENUM":
      return <Dropdown {...props} value={value} options={item.allowedValues} onChange={event => onChange(event.value)}/>;
    case "INTEGER":
      return <InputText {...props} inputMode="numeric" value={value} onChange={event => onChange(event.target.value)}/>;
    default:
      return <InputText {...props} maxLength={60} value={value} onChange={event => onChange(event.target.value)}/>;
  }
}

/**
 * Body and footer of the personalization side panel (#792): one row per attribute with its editor, the origin of the
 * effective value, Reset for what the scope overrides and an inline error that blocks Save while the value is invalid
 * @returns {React.ReactElement} Panel content
 */
export default function ScreenElementPanel({element, attributes, rules, scope, pending, onChange, onResetElement, onClose}) {
  const {t} = useTranslation();
  const {common, advanced} = orderAttributes(attributes, element.elementType);
  const row = item => {
    const {attribute} = item;
    const resolved = resolveValue(rules, pending, element.id, attribute, scope, item.value);
    const edit = pending[`${element.id}|${attribute}`];
    const overridden = typeof edit === "string" || (edit === undefined && ownRule(rules.filter(rule => rule.component === element.id), attribute, scope));
    const error = typeof edit === "string" ? validateValue(item.valueType, edit, item.allowedValues) : null;
    // A label that is a locale key shows its text in the current language (the key stays as helper text); a typed one is kept as typed
    const text = typeof edit === "string" || !TRANSLATABLE.includes(attribute) ? resolved.value : translateLabel(resolved.value, t);
    const translated = t(attributeLabelKey(attribute));
    const label = translated === attributeLabelKey(attribute) ? attribute : translated;
    return (
      <div className="scr-per-row" key={attribute}>
        <label htmlFor={`scr-per-${attribute}`} className="scr-per-label">{label}</label>
        <ValueEditor item={item} value={text} label={label} invalid={!!error} onChange={value => onChange(attribute, value)}/>
        {text !== resolved.value && <small className="scr-per-key">{resolved.value}</small>}
        <div className="scr-per-origin">
          {overridden ? t("SCR_PER_ORIGIN_PERSONALIZED") : t(ORIGIN_KEYS[resolved.origin])}
          {overridden && <Button type="button" link className="scr-per-reset" label={t("SCR_PER_RESET_ATTRIBUTE")}
            aria-label={`${t("SCR_PER_RESET_ATTRIBUTE")} ${label}`} onClick={() => onChange(attribute, null)}/>}
        </div>
        {error && <small role="alert" className="scr-per-error">
          {translateLabel(`ERROR_MESSAGE_SCREEN_CONFIGURATION_${error}`, t, ...(error === "VALUE_TOO_LONG" ? [attribute, 60] : [attribute, edit, item.allowedValues.join(", ")]))}
        </small>}
      </div>
    );
  };
  return (
    <div className="scr-per-panel">
      <div className="scr-per-body">
        {common.map(row)}
        {advanced.length > 0 && <details className="scr-per-advanced">
          <summary>{t("SCR_PER_ADVANCED")}</summary>
          {advanced.map(row)}
        </details>}
      </div>
      <div className="scr-per-footer">
        <Button type="button" outlined label={t("SCR_PER_RESET_ELEMENT")} onClick={onResetElement}/>
        <Button type="button" label={t("BUTTON_DONE")} onClick={onClose}/>
      </div>
    </div>
  );
}

ScreenElementPanel.propTypes = {
  element: PropTypes.object.isRequired,
  attributes: PropTypes.array.isRequired,
  rules: PropTypes.array.isRequired,
  scope: PropTypes.object.isRequired,
  pending: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
  onResetElement: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired
};
