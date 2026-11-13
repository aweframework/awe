import React, { useEffect, useMemo, useRef, useState } from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";
import { getIconNode } from "../utilities";
import { TestIds, testHook } from "../utilities/testIds";
import {
  BLANKS_KEY,
  buildDateTree,
  getCheckState,
  getLeafKeys,
  getValueKey
} from "../utilities/columnFilters";

// Key of the "(Select all)" row
const SELECT_ALL = "\u0000all";

/**
 * Text of a value of the list: its label, else its value
 * @param {object} item Value of the list
 * @returns {string} Text
 */
const valueLabel = (item) => String(item.label ?? item.value ?? "");

/**
 * Rows the list shows, in order: (Select all), the values (or the years, months and days of a date column, as far as
 * they are expanded) and (Blanks)
 * @param {object[]} values Values of the list
 * @param {boolean} tree The values are dates shown as a tree
 * @param {object} expanded Expanded nodes by key
 * @param {string} language Language of the month names
 * @param {function} t Translation function
 * @returns {object[]} Rows: key, label, count, level, the keys of the values they check and, for tree nodes, expandable
 */
export function getValueRows(values, tree, expanded, language, t) {
  const blanks = values.filter(item => item.value === null || item.value === undefined);
  const rows = [];
  if (tree) {
    const addNode = (node) => {
      const expandable = node.children.length > 0;
      rows.push({ key: node.key, label: node.label, count: node.count, level: node.level, leafKeys: getLeafKeys(node),
        expandable, expanded: expandable && !!expanded[node.key], value: node.value });
      if (expandable && expanded[node.key]) {
        node.children.forEach(addNode);
      }
    };
    buildDateTree(values, language).forEach(addNode);
  } else {
    values.filter(item => item.value !== null && item.value !== undefined).forEach(item => rows.push({
      key: getValueKey(item.value), label: valueLabel(item), count: item.count, level: 0, leafKeys: [getValueKey(item.value)],
      value: item.value
    }));
  }
  blanks.forEach(item => rows.push({ key: BLANKS_KEY, label: t("GRID_FILTER_BLANKS"), count: item.count, level: 0,
    leafKeys: [BLANKS_KEY], blanks: true }));
  const allKeys = values.map(item => getValueKey(item.value));
  return [{ key: SELECT_ALL, label: t("GRID_FILTER_SELECT_ALL"), level: 0, leafKeys: allKeys, selectAll: true }, ...rows];
}

/**
 * Box of a checkbox row: checked, unchecked or mixed
 * @param {string} state Checked state
 * @returns {JSX.Element} Box
 */
function CheckBox({ state }) {
  return <span className={`grid-column-filter-check grid-column-filter-check-${state}`} aria-hidden="true">
    {state === "checked" && getIconNode("lucide:check", { tag: "span" })}
    {state === "mixed" && getIconNode("lucide:minus", { tag: "span" })}
  </span>;
}

CheckBox.propTypes = {
  state: PropTypes.string
};

/**
 * Values list of the autofilter menu of a column (#912, S14): "(Select all)" (checked, unchecked or mixed), a checkbox
 * per value with its label and its count, "(Blanks)" for the empty values and, on a date column, a tree of years,
 * months and days. It is one stop of the Tab order: the arrows move between its rows, Space or Enter checks them and,
 * in the tree, Right and Left expand and collapse a node.
 * @category Components
 * @subcategory Grid
 */
function AweGridColumnFilterValues({ gridId, values, checked, onToggle, tree = false, label }) {
  const { t, i18n } = useTranslation();
  const [expanded, setExpanded] = useState(() => {
    // A single year starts open
    const years = tree ? buildDateTree(values, i18n.language) : [];
    return years.length === 1 ? { [years[0].key]: true } : {};
  });
  const [focusIndex, setFocusIndex] = useState(0);
  const moved = useRef(false);
  const list = useRef(null);
  const rows = useMemo(() => getValueRows(values, tree, expanded, i18n.language, t), [values, tree, expanded, i18n.language, t]);
  const current = Math.min(focusIndex, rows.length - 1);

  // The row reached with the keyboard takes the focus
  useEffect(() => {
    if (moved.current) {
      moved.current = false;
      list.current?.querySelectorAll("[data-row-index]")[current]?.focus();
    }
  }, [current, rows]);

  const stateOf = (row) => getCheckState(row.leafKeys.filter(key => checked.has(key)).length, row.leafKeys.length);
  const toggle = (row) => onToggle(row.leafKeys, stateOf(row) !== "checked");
  const setOpen = (row, open) => setExpanded(previous => ({ ...previous, [row.key]: open }));
  const moveTo = (index) => {
    moved.current = true;
    setFocusIndex(Math.max(0, Math.min(index, rows.length - 1)));
  };

  const onKeyDown = (event) => {
    const row = rows[current];
    switch (event.key) {
      case "ArrowDown":
        moveTo(current + 1);
        break;
      case "ArrowUp":
        moveTo(current - 1);
        break;
      case "Home":
        moveTo(0);
        break;
      case "End":
        moveTo(rows.length - 1);
        break;
      case " ":
      case "Enter":
        toggle(row);
        break;
      case "ArrowRight":
        if (row.expandable && !row.expanded) {
          setOpen(row, true);
        } else if (row.expandable) {
          moveTo(current + 1);
        }
        break;
      case "ArrowLeft":
        if (row.expandable && row.expanded) {
          setOpen(row, false);
        } else if (row.level > 0) {
          moveTo(rows.slice(0, current).map(item => item.level).lastIndexOf(row.level - 1));
        }
        break;
      default:
        return;
    }
    event.preventDefault();
    event.stopPropagation();
  };

  const role = tree ? "treeitem" : "checkbox";
  return <div ref={list} className="grid-column-filter-list" role={tree ? "tree" : "group"}
    aria-label={t("GRID_FILTER_VALUES", { label })}>
    {rows.map((row, index) => {
      const state = stateOf(row);
      const hook = row.selectAll ?
        testHook(TestIds.gridColumnFilterSelectAll, { owner: gridId }) :
        testHook(TestIds.gridColumnFilterValue, { owner: gridId, selected: state === "checked",
          type: row.blanks ? "blanks" : undefined,
          attributes: { "option-id": row.blanks ? "" : String(row.value ?? row.key) } });
      return <div key={row.key} role={role} data-row-index={index} tabIndex={index === current ? 0 : -1}
        aria-checked={state === "mixed" ? "mixed" : state === "checked"}
        aria-expanded={tree && row.expandable ? row.expanded : undefined}
        aria-level={tree ? row.level + 1 : undefined}
        className="grid-column-filter-value" style={{ "--grid-column-filter-level": row.level }}
        onClick={() => {
          setFocusIndex(index);
          toggle(row);
        }} onKeyDown={onKeyDown} {...hook}>
        {tree && <span className="grid-column-filter-toggle" aria-hidden="true" onClick={(event) => {
          event.stopPropagation();
          if (row.expandable) {
            setFocusIndex(index);
            setOpen(row, !row.expanded);
          }
        }}>{row.expandable && getIconNode(row.expanded ? "lucide:chevron-down" : "lucide:chevron-right", { tag: "span" })}</span>}
        <CheckBox state={state} />
        <span className="grid-column-filter-value-label">{row.label}</span>
        {!row.selectAll && row.count !== undefined && row.count !== null &&
          <span className="grid-column-filter-value-count">{row.count}</span>}
      </div>;
    })}
  </div>;
}

AweGridColumnFilterValues.propTypes = {
  gridId: PropTypes.string,
  values: PropTypes.array.isRequired,
  checked: PropTypes.instanceOf(Set).isRequired,
  onToggle: PropTypes.func.isRequired,
  tree: PropTypes.bool,
  label: PropTypes.string
};

export default AweGridColumnFilterValues;
