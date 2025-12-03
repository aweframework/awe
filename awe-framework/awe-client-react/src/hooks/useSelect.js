import {useCallback, useEffect, useMemo, useRef, useState} from "react";
import {useTranslation} from "react-i18next";
import {useDispatch, useSelector} from "react-redux";
import {updateModelWithDependencies} from "../redux/thunks/components";
import {translateLabel} from "../utilities";
import useComponent from "./useComponent";

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
export function useSelect({id, model: propModel, address: propAddress, attributes: propAttrs, multiple = false}) {
  const {t} = useTranslation();
  const dispatch = useDispatch();
  const dropdownRef = useRef(null);

  const { address: reduxAddress } = useComponent(id);

  // --- Obtener datos de Redux si hay id ---
  const reduxData = useSelector((state) => (id ? {
    model: state.components[id]?.model ?? {values: []},
    attributes: state.components[id]?.attributes ?? {},
    validationRules: state.components[id]?.validationRules ?? {}
  } : {}));

  const address = propAddress || reduxAddress;
  const model = propModel || reduxData.model || {values: []};
  const attributes = propAttrs || reduxData.attributes || {};
  const validationRules = reduxData.validationRules || {};

  // --- Traducir opciones ---
  const options = useMemo(
    () => (model?.values || []).map(v => ({
      ...(!multiple ? {...v, label: translateLabel(v.label, t)} : {}),
      name: translateLabel(v.label, t),
      code: v.value
    })),
    [model?.values, t]
  );

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

  const [selected, setSelected] = useState(getSelectedValue(model.values, multiple, t));

  // --- onChange ---
  const onChange = useCallback((e) => {
    const value = e.value ?? e.target?.value;
    if (multiple) {
      const selectedCodes = value.map(v => v.code);
      const values = model.values.map(v => ({...v, selected: selectedCodes.includes(v.value)}));
      dispatch(updateModelWithDependencies(address, {values}));
    } else {
      dispatch(updateModelWithDependencies(address, {
        values: model.values.map(v => ({
          ...v,
          selected: v.value === value
        }))
      }));
    }
  }, [address, model, multiple]);

  useEffect(() => {
    setSelected(getSelectedValue(model.values, multiple, t));
  }, [model?.values]);

  return {
    t,
    ref: dropdownRef,
    address,
    model,
    attributes,
    validationRules,
    options,
    selected,
    onChange
  };
}
