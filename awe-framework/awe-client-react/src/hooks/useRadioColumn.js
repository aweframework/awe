import {useCallback, useMemo} from "react";
import {useDispatch} from "react-redux";
import {useTranslation} from "react-i18next";
import {updateModelWithDependencies} from "../redux/thunks/components";
import {extractCellValue} from "../utilities/grid";
import {getFirstDefinedValue, isEmpty} from "../utilities/general";
import {translateLabel} from "../utilities";

const KEY_STEP = {ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1};

/**
 * Move the choice of a cell to its next or previous enabled option and focus it.
 * The options are the radio inputs of the group the key was pressed in.
 * @param {Event} event Key down event on one option
 */
function moveChoice(event) {
  const step = KEY_STEP[event.key];
  if (!step) {
    return;
  }
  const group = event.target.closest("[role=radiogroup]");
  const options = Array.from(group?.querySelectorAll("input[type=radio]") ?? []).filter(input => !input.disabled);
  const current = options.indexOf(event.target);
  if (options.length < 2 || current < 0) {
    return;
  }
  event.preventDefault();
  const next = options[(current + step + options.length) % options.length];
  next.focus();
  next.click();
}

/**
 * Logic of a radio or button-radio column. The options come from the list of the column, like the ones of a select
 * column, and the choice is the value of the cell.
 * @param {object} props Column properties (address, model, label, data, attrs, readonly, required)
 * @return {object} Options, choice and handlers
 */
export default function useRadioColumn(props) {
  const {readonly, required, data = {}, attrs = {}, address, label, model} = props;
  const {readonly: cellReadonly, validationRules = {}, error = null, visible = true} = attrs;
  const {t} = useTranslation();
  const dispatch = useDispatch();

  const cellValue = extractCellValue(data);
  const chosen = isEmpty(cellValue) ? null : cellValue;
  const disabled = !!getFirstDefinedValue(cellReadonly, readonly, false);
  const name = `${address.view}-${address.component}-${address.row}-${address.column}`;

  const options = useMemo(() => (model?.values ?? []).map(option => ({
    value: option.value,
    label: translateLabel(option.label, t) || String(option.value ?? ""),
    disabled: !!option.disabled,
    inputId: `${name}-${option.value}`,
    checked: chosen !== null && String(chosen) === String(option.value)
  })), [model?.values, chosen, name, t]);

  const onSelect = useCallback((option) => {
    if (!option.checked && !disabled) {
      dispatch(updateModelWithDependencies(address, {values: option.value}));
    }
  }, [disabled, address, dispatch]);

  return {
    options,
    disabled,
    visible,
    error,
    name,
    groupLabel: translateLabel(label, t),
    required: !!getFirstDefinedValue(validationRules.required, required, false),
    onSelect,
    onKeyDown: moveChoice,
    t
  };
}
