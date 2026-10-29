import React, { useCallback, useMemo, useRef } from "react";
import { useDispatch } from "react-redux";
import { updateAttributes } from "../redux/actions/components";
import { buildGroupedRows, getGroupingState, isDisplayRow, isGroupRow } from "../utilities/gridGrouping";
import AweGridGroupCell from "../components/AweGridGroups";

/**
 * Actions on the grouping of a grid (#833): they change its attributes only, so nothing goes to the server. The
 * expanded groups are stored with the levels they belong to, so they are kept while the levels stay the same.
 * @param {object} address Grid address
 * @param {object} grouping Grouping state of the grid (getGroupingState)
 * @returns {{toggleGroup: function, expandAll: function, collapseAll: function, setLevels: function}} Actions
 */
export function useGroupingActions(address, grouping) {
  const dispatch = useDispatch();
  // The grouping of the last render: a toggle rendered before the last change (the cell of another group row, which the
  // table may not render again) must change the current expansion, not the one it was rendered with
  const current = useRef(grouping);
  current.current = grouping;
  const storeExpansion = useCallback((all, paths) => {
    dispatch(updateAttributes(address, { groupExpansion: { all, paths, levels: current.current.levels } }));
  }, [dispatch, address]);

  // A click on the toggle also reaches its row, which toggles it too: both read the same expansion, so it changes once
  const toggleGroup = useCallback((path) => {
    const { all, paths } = current.current.expansion;
    storeExpansion(all, { ...paths, [path]: !(paths[path] ?? all) });
  }, [storeExpansion]);
  const expandAll = useCallback(() => storeExpansion(true, {}), [storeExpansion]);
  const collapseAll = useCallback(() => storeExpansion(false, {}), [storeExpansion]);
  const setLevels = useCallback((newLevels) => dispatch(updateAttributes(address, { groupLevels: newLevels })), [dispatch, address]);
  return { toggleGroup, expandAll, collapseAll, setLevels };
}

/**
 * Row grouping of a grid (#833). While the rows are grouped, the grid shows the display rows built from its data rows
 * (the model keeps the data rows), with every row in view (no pagination), the sort of the grid applied here (the table
 * does not sort by itself) and the group and subtotal rows out of the selection: a click on a group row toggles it.
 * @param {object} options Grid data
 * @param {object} options.address Grid address
 * @param {object} options.attributes Grid attributes
 * @param {object[]} options.sort Sort of the grid
 * @param {object[]} options.values Data rows
 * @param {string} options.firstColumn Name of the first visible column, where the group label goes
 * @param {function} options.onSelect Selection change of the grid
 * @param {function} options.onContextMenu Context menu of the grid
 * @param {function} options.t Translator
 * @returns {{active: boolean, values: object[], tableProps: object, renderCell: function, rowNumberOf: function|undefined}}
 * Whether the rows are grouped, the rows to show, the DataTable properties that replace the regular ones while grouped,
 * the body of the cells of the display rows and the number of each data row (grouped only)
 */
export function useGridGrouping({ address, attributes, sort, values, firstColumn, onSelect, onContextMenu, t }) {
  const grouping = useMemo(() => getGroupingState(attributes), [attributes]);
  const { active, levels, expansion, subtotals } = grouping;
  const { toggleGroup } = useGroupingActions(address, grouping);

  const displayValues = useMemo(() => active ? buildGroupedRows(values, {
    groupBy: levels, columns: attributes.columnModel, sort, expansion, subtotals, t
  }) : values, [active, values, levels, attributes.columnModel, sort, expansion, subtotals, t]);

  // The data rows are numbered in the order they are shown, the display rows have no number
  const rowNumbers = useMemo(() => {
    const numbers = new Map();
    if (active) {
      displayValues.filter(row => !isDisplayRow(row)).forEach((row, index) => numbers.set(row, index + 1));
    }
    return numbers;
  }, [active, displayValues]);
  const rowNumberOf = useCallback(row => rowNumbers.get(row) ?? null, [rowNumbers]);

  const onRowClick = useCallback(({ data }) => {
    if (isGroupRow(data)) {
      toggleGroup(data.$group.path);
    }
  }, [toggleGroup]);

  // The display rows never reach the selection of the grid
  const onSelectionChange = useCallback((event) => {
    const { value } = event;
    if (Array.isArray(value)) {
      onSelect({ ...event, value: value.filter(row => !isDisplayRow(row)) });
    } else if (!isDisplayRow(value)) {
      onSelect(event);
    }
  }, [onSelect]);

  // The display rows have no context menu: the right click stays in the row, so the grid does not open its menu either
  const onRowContextMenu = useCallback((event) => {
    if (isDisplayRow(event?.data)) {
      event.originalEvent?.stopPropagation?.();
      return;
    }
    onContextMenu(event);
  }, [onContextMenu]);

  const isDataRow = useCallback(({ data }) => !isDisplayRow(data), []);
  const showSelectionElement = useCallback(data => !isDisplayRow(data), []);

  const tableProps = useMemo(() => active ? {
    lazy: true,
    paginatorTemplate: "",
    first: 0,
    rows: Math.max(displayValues.length, 1),
    totalRecords: displayValues.length,
    isDataSelectable: isDataRow,
    showSelectionElement,
    onRowClick,
    onSelectionChange,
    onContextMenuSelectionChange: onSelectionChange,
    onContextMenu: onRowContextMenu
  } : {}, [active, displayValues, isDataRow, showSelectionElement, onRowClick, onSelectionChange, onRowContextMenu]);

  const renderCell = (row, column) => <AweGridGroupCell row={row} column={column} firstColumn={firstColumn}
    onToggle={toggleGroup} t={t} />;

  return { active, values: displayValues, tableProps, renderCell, rowNumberOf: active ? rowNumberOf : undefined };
}
