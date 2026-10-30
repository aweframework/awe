import { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useComponentState } from "./useComponentState";
import { updateModelWithDependencies as updateThunk } from "../redux/thunks/components";
import { addActionsTop } from "../redux/actions/actions";
import useComponent from "./useComponent";
import { useAdvancedWindow } from "../components/AweWindowAdvanced";

/**
 * useText hook
 * Migrates the logic from AweTextComponent (class) to a reusable hook for text-like criteria.
 * It manages local editing state, sync with model, and dispatches updates and submit actions.
 */
export default function useText(id) {
  const dispatch = useDispatch();
  const { address } = useComponent(id);
  const { model = { values: [] }, attributes = {}, validationRules = {}, context = {} } = useComponentState(id);
  const settings = useSelector(state => state.settings || {});
  const advanced = useAdvancedWindow();

  const getValue = useCallback(() => {
    const values = model.values || [];
    return values.filter(v => v.selected).map(v => v.value).join(", ");
  }, [model.values]);

  const [value, setValue] = useState(getValue());
  const [writing, setWriting] = useState(false);

  // Sync when model changes and we are not writing
  useEffect(() => {
    const newValue = getValue();
    if (!writing && newValue !== value) {
      setValue(newValue);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [model.values]);

  const updateModelWithDependencies = useCallback((payload) => {
    if (address) dispatch(updateThunk(address, payload));
  }, [dispatch, address]);

  const onChange = useCallback((e) => {
    const target = e?.target || {};
    // Fix browser autofill issues: if not focused, persist immediately
    if (typeof document !== 'undefined' && document.activeElement !== target) {
      updateModelWithDependencies({ values: [{ value: target.value, selected: true }] });
    } else {
      setValue(target.value);
      setWriting(true);
    }
  }, [updateModelWithDependencies]);

  const onBlur = useCallback(() => {
    if (getValue() !== value) {
      updateModelWithDependencies({ values: [{ value, selected: true }] });
    }
    setWriting(false);
  }, [getValue, updateModelWithDependencies, value]);

  const onSubmit = useCallback(() => {
    // Store data first
    onBlur();
    // Publish submit action: a search criterion of a window with advanced search (#790) submits from the window, so
    // its own submit button runs although the criterion is inside the advanced block
    const source = context?.source || [];
    const target = advanced?.searchCriteria.includes(address?.component) ? advanced.id : source[source.length - 2];
    if (address) {
      dispatch(addActionsTop([{ type: "submit", target, address }]));
    }
  }, [dispatch, onBlur, address, context?.source, advanced]);

  return useMemo(() => ({
    address,
    model,
    attributes,
    validationRules,
    settings,
    value,
    setValue,
    onChange,
    onBlur,
    onSubmit
  }), [address, model, attributes, validationRules, settings, value, onChange, onBlur, onSubmit]);
}
