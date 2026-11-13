import React, { useId, useMemo, useState } from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { Dropdown } from "primereact/dropdown";
import { Calendar } from "primereact/calendar";
import { InputText } from "primereact/inputtext";
import { formatDate, toDate } from "../utilities/dates";
import { TestIds, testHook } from "../utilities/testIds";
import {
  buildConditionsFilter,
  getOperatorLabel,
  getOperators,
  operatorNeedsValue
} from "../utilities/columnFilters";

// A condition without operator (the second one is optional)
const NO_CONDITION = { operator: "", value: "", to: "" };

/**
 * Conditions the custom filter starts with: the ones of the filter of the column, or the operator chosen in the type
 * submenu, and an empty second one
 * @param {object} filter Filter of the column
 * @param {string} operator Operator chosen in the submenu (none for the custom filter)
 * @param {string} type Filter type
 * @returns {object[]} Two conditions
 */
export function getInitialConditions(filter, operator, type) {
  if (operator) {
    return [{ ...NO_CONDITION, operator }, NO_CONDITION];
  }
  const current = (filter?.conditions || []).map(condition => ({ ...NO_CONDITION, ...condition }));
  return [current[0] || { ...NO_CONDITION, operator: getOperators(type)[0] }, current[1] || NO_CONDITION];
}

/**
 * Input of a value of a condition: a date picker on a date column, a text box otherwise
 */
function ConditionValue({ type, value, onChange, label, language }) {
  if (type === "date") {
    return <Calendar value={toDate(value)} onChange={(event) => onChange(formatDate(event.value))} dateFormat="dd/mm/yy"
      appendTo="self" locale={language} ariaLabel={label} inputClassName="grid-column-filter-input"
      className="grid-column-filter-input" />;
  }
  return <InputText value={value ?? ""} onChange={(event) => onChange(event.target.value)} aria-label={label}
    inputMode={type === "number" ? "decimal" : undefined} className="grid-column-filter-input" />;
}

ConditionValue.propTypes = {
  type: PropTypes.string,
  value: PropTypes.string,
  onChange: PropTypes.func,
  label: PropTypes.string,
  language: PropTypes.string
};

/**
 * Custom filter of the autofilter menu of a column (#912, S14): two conditions, each an operator of the type of the
 * column and its value (two for between), joined by And or Or. Accept applies the complete conditions; Cancel closes
 * the menu without changes.
 * @category Components
 * @subcategory Grid
 */
function AweGridColumnFilterCustom({ gridId, column, type, label, filter, operator, onApply, onCancel }) {
  const { t } = useTranslation();
  const language = useSelector(state => state.settings?.language);
  const titleId = useId();
  const [conditions, setConditions] = useState(() => getInitialConditions(filter, operator, type));
  const [union, setUnion] = useState(filter?.union === "or" ? "or" : "and");
  const decimalSeparator = useSelector(state => state.settings?.numericOptions?.decimalCharacter ?? ".");
  const result = useMemo(() => buildConditionsFilter(column, conditions, union, decimalSeparator),
    [column, conditions, union, decimalSeparator]);

  const operators = getOperators(type).map(value => ({ value, label: t(getOperatorLabel(value, type)) }));
  const change = (index, changes) => setConditions(previous => previous.map((condition, position) =>
    position === index ? { ...condition, ...changes } : condition));

  const onUnionKeyDown = (event) => {
    if (["ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown"].includes(event.key)) {
      event.preventDefault();
      setUnion(current => current === "and" ? "or" : "and");
    }
  };

  const conditionRow = (condition, index) => {
    const number = index + 1;
    const options = index === 0 ? operators : [{ value: "", label: t("GRID_FILTER_NO_CONDITION") }, ...operators];
    return <div className="grid-column-filter-condition" key={index}>
      <Dropdown value={condition.operator} options={options} onChange={(event) => change(index, { operator: event.value })}
        appendTo="self" ariaLabel={t("GRID_FILTER_CONDITION_OPERATOR", { number })} placeholder={t("GRID_FILTER_NO_CONDITION")}
        className="grid-column-filter-operator" />
      {operatorNeedsValue(condition.operator) && <ConditionValue type={type} value={condition.value} language={language}
        label={t("GRID_FILTER_CONDITION_VALUE", { number })} onChange={(value) => change(index, { value })} />}
      {condition.operator === "between" && <ConditionValue type={type} value={condition.to} language={language}
        label={t("GRID_FILTER_CONDITION_TO", { number })} onChange={(to) => change(index, { to })} />}
    </div>;
  };

  return <div className="grid-column-filter-custom" role="group" aria-labelledby={titleId}>
    <div className="grid-column-filter-head" id={titleId}>
      <span>{t("GRID_FILTER_CUSTOM_TITLE")}</span>
      <span className="grid-column-filter-head-column">{label}</span>
    </div>
    <div className="grid-column-filter-form">
      {conditionRow(conditions[0], 0)}
      <div className="grid-column-filter-union" role="radiogroup" aria-label={t("GRID_FILTER_UNION")} onKeyDown={onUnionKeyDown}>
        {["and", "or"].map(value => <button key={value} type="button" role="radio" aria-checked={union === value}
          tabIndex={union === value ? 0 : -1} className="grid-column-filter-union-option" onClick={() => setUnion(value)}>
          {t(value === "and" ? "GRID_FILTER_AND" : "GRID_FILTER_OR")}
        </button>)}
      </div>
      {conditionRow(conditions[1], 1)}
    </div>
    <div className="grid-column-filter-footer">
      <button type="button" className="p-button p-component p-button-outlined p-button-sm" onClick={onCancel}
        {...testHook(TestIds.gridColumnFilterCancel, { owner: gridId })}>{t("BUTTON_CANCEL")}</button>
      <button type="button" className="p-button p-component p-button-sm" disabled={!result} onClick={() => onApply(result)}
        {...testHook(TestIds.gridColumnFilterAccept, { owner: gridId })}>{t("BUTTON_ACCEPT")}</button>
    </div>
  </div>;
}

AweGridColumnFilterCustom.propTypes = {
  gridId: PropTypes.string,
  column: PropTypes.object.isRequired,
  type: PropTypes.string.isRequired,
  label: PropTypes.string,
  filter: PropTypes.object,
  operator: PropTypes.string,
  onApply: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired
};

export default AweGridColumnFilterCustom;
