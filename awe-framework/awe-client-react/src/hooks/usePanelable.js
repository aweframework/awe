import {useEffect, useMemo} from "react";
import {useDispatch} from "react-redux";
import {updateModelWithDependencies as updateThunk} from "../redux/thunks/components";
import useComponent from "./useComponent";

/**
 * Shared hook to manage selectable values.
 * - Computes activeIndex from model.values
 * - Ensures a default selection (index 0) when there are values but none selected (configurable)
 * - Exposes a selectIndex helper to update selection
 *
 * Options:
 *  - multi: when true, supports multiple selection returning activeIndex as an array of selected indices
 *  - defaultFirst: when true (default), auto-selects the first item if none selected (applies to single-select mode)
 */
export function usePanelable(model = {values: []}, address, options = {}) {
  // Initialize as component
  useComponent(address.component);

  const { multi = false, defaultFirst = true } = options;
  const dispatch = useDispatch();
  const updateModelWithDependencies = (addr, payload) => dispatch(updateThunk(addr, payload));

  const values = model.values || [];
  const activeIndex = useMemo(() => {
    if (multi) {
      return values.reduce((acc, item, idx) => (item.selected ? acc.concat(idx) : acc), []);
    }
    return values.findIndex(item => item.selected);
  }, [multi, values]);

  useEffect(() => {
    if (!multi && defaultFirst && (values || []).length > 0 && (activeIndex == null || activeIndex < 0)) {
      updateModelWithDependencies(address, {
        values: values.map((item, index) => ({...item, selected: index === 0}))
      });
    }
  }, [address, activeIndex, defaultFirst, multi, updateModelWithDependencies, values]);

  const selectIndex = (indexOrIndexes) => {
    if (multi) {
      const indexes = Array.isArray(indexOrIndexes) ? indexOrIndexes : [indexOrIndexes];
      updateModelWithDependencies(address, {
        values: values.map((item, idx) => ({...item, selected: indexes.includes(idx)}))
      });
    } else {
      const index = Array.isArray(indexOrIndexes) ? indexOrIndexes[0] : indexOrIndexes;
      updateModelWithDependencies(address, {
        values: values.map((item, idx) => ({...item, selected: idx === index}))
      });
    }
  };

  return useMemo(() => ({values, activeIndex, selectIndex}),
    [values, activeIndex, selectIndex]);
}
