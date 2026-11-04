import { useCallback, useMemo } from "react";
import { useDispatch } from "react-redux";
import { useComponentState } from "./useComponentState";
import { addActionsTop } from "../redux/actions/actions";
import { getGridIdentifier } from "../utilities/grid";
import { setColumnFilter } from "../utilities/columnFilters";

// No column filters, and no labels of their values
const NO_FILTERS = Object.freeze([]);
const NO_LABELS = Object.freeze({});

/**
 * Column filters of a grid (#912) and the ways to change them. A change reloads the grid from the server from its first
 * page, also a grid that loads all its rows (load-all), and the row being edited is validated and cancelled first, as a
 * change of page or sort does.
 * @param {string} id Grid identifier
 * @returns {{columnFilters: object[], labels: object, applyColumnFilter: function, removeColumnFilter: function, clearColumnFilters: function, restoreColumnFilters: function}}
 * The column filters in effect, the labels of their values by column, and the functions that apply the filter of a
 * column (null removes it; with the labels of its values), remove the filter of a column, remove them all and restore
 * the column filters, their labels and the sort of a saved search (#912)
 */
export function useColumnFilters(id) {
  const dispatch = useDispatch();
  const { address, attributes = {}, model, specificAttributes = {} } = useComponentState(id) || {};
  const columnFilters = specificAttributes.columnFilters || NO_FILTERS;
  const labels = attributes.columnFilterLabels || NO_LABELS;
  const { validateOnSave = true } = attributes;
  const values = model?.values;
  const gridId = getGridIdentifier(attributes);

  const reload = useCallback((nextFilters, nextLabels, changes = []) => {
    const editingRow = (values || []).find(row => row.$row?.editing);
    const validate = validateOnSave && editingRow ? [{ type: "validate-row", address: { ...address, row: editingRow[gridId] } }] : [];
    const cancel = editingRow ? [{ type: "cancel-row", address }] : [];
    dispatch(addActionsTop([...validate, ...cancel, ...changes,
      { type: "change-column-filters", address, parameters: { columnFilters: nextFilters, labels: nextLabels } },
      { type: "filter", address }]));
  }, [dispatch, address, values, validateOnSave, gridId]);

  const applyColumnFilter = useCallback((name, filter, valueLabels = null) => {
    const otherLabels = Object.fromEntries(Object.entries(labels).filter(([column]) => column !== name));
    const nextLabels = filter?.values && valueLabels ? { ...otherLabels, [name]: valueLabels } : otherLabels;
    reload(setColumnFilter(columnFilters, name, filter), nextLabels);
  }, [reload, columnFilters, labels]);

  const removeColumnFilter = useCallback((name) => applyColumnFilter(name, null), [applyColumnFilter]);

  const clearColumnFilters = useCallback(() => reload([], {}), [reload]);

  const restoreColumnFilters = useCallback((nextFilters, nextLabels, sort) => reload(nextFilters, nextLabels,
    [{ type: "change-sort", address, parameters: { sort: sort.map(item => ({ ...item, order: item.direction === "asc" ? 1 : -1 })) } }]),
  [reload, address]);

  return useMemo(() => ({ columnFilters, labels, applyColumnFilter, removeColumnFilter, clearColumnFilters, restoreColumnFilters }),
    [columnFilters, labels, applyColumnFilter, removeColumnFilter, clearColumnFilters, restoreColumnFilters]);
}

export default useColumnFilters;
