import { useCallback, useMemo, useRef } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { useComponentState } from "./useComponentState";
import { updateModelWithDependencies } from "../redux/thunks/components";
import { translateLabel } from "../utilities";
import useComponent from "./useComponent";
import { groupSuggestions } from "../utilities/suggest";

// Separator between the group and the rest of a label ("Users · Read")
const GROUP_SEPARATOR = " · ";

/**
 * Text of an option in its list: a label that starts with the label of its group and the separator is listed without
 * them, as the header of the group names it (the chosen value keeps the whole label)
 * @param {string} name Translated label of the option
 * @param {string} groupLabel Translated label of its group
 * @returns {string} Text of the option in the list
 */
const getItemText = (name, groupLabel) => groupLabel && name.startsWith(groupLabel + GROUP_SEPARATOR)
  ? name.substring(groupLabel.length + GROUP_SEPARATOR.length)
  : name;

/**
 * Template of an option in the list of a select
 * @param {object} option Option
 * @returns {string} Text of the option
 */
export const selectItemTemplate = (option) => option.itemText ?? option.name;

/**
 * Hook común para selects (simple y múltiple)
 *
 * @param {object} params
 * @param {string} [params.id] - Id del componente (para Redux)
 * @param {object} [params.model] - Modelo (para componentes que no usan Redux)
 * @param {object} [params.address] - Dirección (para componentes que no usan Redux)
 * @param {object} [params.attributes] - Atributos (para componentes no Redux)
 * @param {boolean} [params.multiple] - Si es selección múltiple
 */
export function useSelect({ id, model: propModel, address: propAddress, attributes: propAttrs, multiple = false }) {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const dropdownRef = useRef(null);

  const { address: reduxAddress } = useComponent(id);
  const component = useComponentState(id);

  // --- Obtener datos de Redux si hay id ---
  const reduxData = id ? {
    model: component.model ?? { values: [] },
    attributes: component.attributes ?? {},
    validationRules: component.validationRules ?? {}
  } : {};

  const address = propAddress || reduxAddress;
  const model = propModel || reduxData.model || { values: [] };
  const attributes = propAttrs || reduxData.attributes || {};
  const validationRules = reduxData.validationRules || {};

  // --- Traducir opciones ---
  // The values with a group (a locale key) are listed under its header, as in the AngularJS select: PrimeReact
  // reads the groups from "label" and "items". The option keeps no "group", which PrimeReact would take for a header
  const { options, grouped } = useMemo(() => {
    const toOption = ({ group, ...v }, groupLabel) => {
      const name = translateLabel(v.label, t);
      return {
        ...(!multiple ? { ...v, label: name } : {}),
        name,
        itemText: getItemText(name, groupLabel),
        code: v.value
      };
    };
    const values = model?.values || [];
    const groups = groupSuggestions(values, key => translateLabel(key, t));
    return groups
      ? {
        options: groups.map(({ label, items }) => ({ label, items: items.map(item => toOption(item, label)) })),
        grouped: true
      }
      : { options: values.map(item => toOption(item)), grouped: false };
  }, [model?.values, multiple, t]);
  const groupProps = grouped ? { optionGroupLabel: "label", optionGroupChildren: "items" } : {};

  // --- Valor seleccionado ---
  const getSelectedValue = (values, multiple, t) => {
    if (multiple) {
      return model.values.filter(v => v.selected).map(v => ({
        code: v.value,
        name: translateLabel(v.label, t)
      }));
    } else {
      return model.values.find(v => v.selected)?.value ?? null;
    }
  };

  // Derived from the model, not copied into a state: a copy lags one render behind the store, and react-redux renders
  // store updates made inside effects synchronously, so a select that picks its first value would dispatch again forever
  const selected = useMemo(() => getSelectedValue(model.values, multiple, t), [model?.values, multiple, t]);

  // --- onChange ---
  const onChange = useCallback((e) => {
    const value = e.value ?? e.target?.value;
    if (multiple) {
      const selectedCodes = value.map(v => v.code);
      const values = model.values.map(v => ({ ...v, selected: selectedCodes.includes(v.value) }));
      dispatch(updateModelWithDependencies(address, { values }));
    } else {
      dispatch(updateModelWithDependencies(address, {
        values: model.values.map(v => ({
          ...v,
          selected: v.value === value
        }))
      }));
    }
  }, [address, model, multiple]);

  // --- Quitar un valor (chip de un select múltiple) ---
  const removeValue = useCallback((code) => onChange({ value: selected.filter(v => v.code !== code) }),
    [selected, onChange]);

  return {
    t,
    ref: dropdownRef,
    address,
    model,
    attributes,
    validationRules,
    options,
    groupProps,
    grouped,
    selected,
    onChange,
    removeValue
  };
}
