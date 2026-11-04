import React, { useCallback, useEffect, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import PropTypes from "prop-types";
import PrimeReact from "primereact/api";
import { ZIndexUtils } from "primereact/utils";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { getIconNode } from "../utilities";
import { classNames } from "../utilities/components";
import { TestIds, testHook } from "../utilities/testIds";
import {
  RELATIVE_DATE_LABELS,
  RELATIVE_DATES,
  SUBMENU_OPERATORS,
  buildRelativeDateFilter,
  buildValuesFilter,
  getColumnFilterType,
  getInitialChecked,
  getOperatorLabel,
  getValueKey
} from "../utilities/columnFilters";
import { columnValuesAction } from "../redux/thunks/columnFilters";
import { useColumnFilters } from "../hooks/useColumnFilters";
import { useFocusTrap } from "../hooks/useFocusTrap";
import { OVERLAYS, swallowAnchorClick } from "./AwePopover";
import AweGridColumnFilterValues from "./AweGridColumnFilterValues";
import AweGridColumnFilterCustom from "./AweGridColumnFilterCustom";
import "./AweGridColumnFilter.less";

/** Pause after the last key typed in the search before the values are asked again */
export const SEARCH_DELAY = 300;

// Sort icons and labels of each type
const SORT_TEXTS = {
  values: { asc: ["lucide:arrow-up-a-z", "GRID_FILTER_SORT_ASC_TEXT"], desc: ["lucide:arrow-down-z-a", "GRID_FILTER_SORT_DESC_TEXT"] },
  text: { asc: ["lucide:arrow-up-a-z", "GRID_FILTER_SORT_ASC_TEXT"], desc: ["lucide:arrow-down-z-a", "GRID_FILTER_SORT_DESC_TEXT"] },
  number: { asc: ["lucide:arrow-up-0-1", "GRID_FILTER_SORT_ASC_NUMBER"], desc: ["lucide:arrow-down-1-0", "GRID_FILTER_SORT_DESC_NUMBER"] },
  date: { asc: ["lucide:calendar-arrow-up", "GRID_FILTER_SORT_ASC_DATE"], desc: ["lucide:calendar-arrow-down", "GRID_FILTER_SORT_DESC_DATE"] }
};

// Label of the type submenu of each type
const SUBMENU_LABELS = { text: "GRID_FILTER_TEXT_FILTERS", number: "GRID_FILTER_NUMBER_FILTERS", date: "GRID_FILTER_DATE_FILTERS" };

/**
 * Options of the type submenu of a column: its operators, the relative dates of a date column and the custom filter,
 * with separators between the groups
 * @param {string} type Filter type
 * @returns {object[]} Options ({operator}, {relative}, {custom} or {separator})
 */
export function getSubmenuOptions(type) {
  const options = SUBMENU_OPERATORS[type].map(operator => ({ key: operator, operator }));
  if (type === "date") {
    options.push({ key: "separator-relative", separator: true }, ...RELATIVE_DATES.map(relative => ({ key: relative, relative })));
  }
  return [...options, { key: "separator-custom", separator: true }, { key: "custom", custom: true }];
}

// Room under the funnel below which the menu opens above it, when there is more room there
const MIN_ROOM_BELOW = 0.4;

/**
 * Place of the menu under the funnel, inside the window: it goes left when it would go out at the right, and above the
 * funnel when there is little room under it. Its height is the room it has, so its list of values scrolls
 * @param {HTMLElement} anchor Funnel button
 * @param {HTMLElement} menu Menu
 * @returns {object} Style of the menu
 */
function getPlacement(anchor, menu) {
  const rect = anchor.getBoundingClientRect();
  const width = menu?.offsetWidth || 0;
  const right = window.scrollX + document.documentElement.clientWidth;
  const left = Math.max(window.scrollX, Math.min(rect.left + window.scrollX, right - width));
  const below = window.innerHeight - rect.bottom;
  if (below < window.innerHeight * MIN_ROOM_BELOW && rect.top > below) {
    return { top: `${rect.top + window.scrollY}px`, left: `${left}px`, "--grid-column-filter-room": `${rect.top}px`,
      transform: "translateY(-100%)", above: true };
  }
  return { top: `${rect.bottom + window.scrollY}px`, left: `${left}px`, "--grid-column-filter-room": `${below}px` };
}

/**
 * Type submenu of the menu (text, number or date filters): its trigger and the menu at its side, which the arrows go
 * through and Escape or Left close
 */
function ConditionsSubmenu({ gridId, type, onOperator, onRelative, onCustom }) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [flip, setFlip] = useState(false);
  const trigger = useRef(null);
  const menu = useRef(null);
  const options = getSubmenuOptions(type);

  const items = () => [...(menu.current?.querySelectorAll("[role='menuitem']") || [])];
  const close = () => {
    setOpen(false);
    trigger.current?.focus();
  };

  // The menu opens at the side with room, with the focus on its first option
  useLayoutEffect(() => {
    if (open && menu.current) {
      setFlip(menu.current.getBoundingClientRect().right > document.documentElement.clientWidth);
      items()[0]?.focus();
    }
  }, [open]);

  const onTriggerKeyDown = (event) => {
    if (event.key === "ArrowRight") {
      event.preventDefault();
      setOpen(true);
    }
  };

  const onMenuKeyDown = (event) => {
    const list = items();
    const index = list.indexOf(document.activeElement);
    switch (event.key) {
      case "ArrowDown":
        list[(index + 1) % list.length]?.focus();
        break;
      case "ArrowUp":
        list[(index - 1 + list.length) % list.length]?.focus();
        break;
      case "Home":
        list[0]?.focus();
        break;
      case "End":
        list[list.length - 1]?.focus();
        break;
      case "Escape":
      case "ArrowLeft":
        close();
        break;
      default:
        return;
    }
    event.preventDefault();
    event.stopPropagation();
  };

  const choose = (option) => {
    setOpen(false);
    if (option.operator) {
      onOperator(option.operator);
    } else if (option.relative) {
      onRelative(option.relative);
    } else {
      onCustom();
    }
  };

  const optionLabel = (option) => {
    if (option.operator) {
      return `${t(getOperatorLabel(option.operator, type))}…`;
    }
    return option.relative ? t(RELATIVE_DATE_LABELS[option.relative]) : t("GRID_FILTER_CUSTOM");
  };

  return <div className="grid-column-filter-submenu">
    <button ref={trigger} type="button" className="grid-column-filter-option" aria-haspopup="menu" aria-expanded={open}
      onClick={() => setOpen(!open)} onKeyDown={onTriggerKeyDown}
      {...testHook(TestIds.gridColumnFilterOption, { owner: gridId, attributes: { "option-id": "conditions" } })}>
      {getIconNode(type === "date" ? "lucide:calendar" : "lucide:funnel", { tag: "span", "aria-hidden": "true" })}
      <span className="grid-column-filter-option-label">{t(SUBMENU_LABELS[type])}</span>
      {getIconNode("lucide:chevron-right", { tag: "span", "aria-hidden": "true", className: "grid-column-filter-option-end" })}
    </button>
    {open && <div ref={menu} role="menu" aria-label={t(SUBMENU_LABELS[type])} onKeyDown={onMenuKeyDown}
      className={classNames("grid-column-filter-flyout", { "grid-column-filter-flyout-start": flip })}>
      {options.map(option => option.separator ?
        <div key={option.key} role="separator" className="grid-column-filter-separator" /> :
        <button key={option.key} type="button" role="menuitem" tabIndex={-1} className="grid-column-filter-option"
          onClick={() => choose(option)}
          {...testHook(TestIds.gridColumnFilterOption, { owner: gridId, attributes: { "option-id": option.key } })}>
          {optionLabel(option)}
        </button>)}
    </div>}
  </div>;
}

ConditionsSubmenu.propTypes = {
  gridId: PropTypes.string,
  type: PropTypes.string,
  onOperator: PropTypes.func,
  onRelative: PropTypes.func,
  onCustom: PropTypes.func
};

/**
 * Menu of the autofilter of a column (#912, S14), anchored under its funnel: sort, clear the filter of the column, the
 * type submenu with the custom filter, and the values of the column with their search. Accept applies the filter;
 * Cancel, Escape and a click outside close it without changes, and the focus goes back to the funnel.
 */
function ColumnFilterMenu({ gridId, address, column, type, label, filter, anchor, onSort, onApply, onClose }) {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const container = useRef(null);
  const [placement, setPlacement] = useState(null);
  const [view, setView] = useState({ name: "main" });
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [result, setResult] = useState({ values: [], more: false, loading: true });
  const [checked, setChecked] = useState(() => new Set());
  const [touched, setTouched] = useState(false);
  const seen = useRef(new Set());

  useFocusTrap(() => container.current, true);

  // Under the funnel, following it while the page scrolls or is resized
  useLayoutEffect(() => {
    const place = () => anchor.current && setPlacement(getPlacement(anchor.current, container.current));
    place();
    window.addEventListener("resize", place);
    window.addEventListener("scroll", place, true);
    return () => {
      window.removeEventListener("resize", place);
      window.removeEventListener("scroll", place, true);
    };
  }, [anchor, view.name]);

  // Over the screen and its dialogs, under the overlays opened from its controls
  useLayoutEffect(() => {
    const element = container.current;
    ZIndexUtils.set("overlay", element, PrimeReact.autoZIndex, PrimeReact.zIndex.overlay);
    return () => ZIndexUtils.clear(element);
  }, []);

  // The focus goes to the search, or to the first control of the custom filter
  useEffect(() => {
    const first = container.current?.querySelector(view.name === "main" ?
      "[data-testid='grid-column-filter-search'], button:not([disabled])" : "input, .p-dropdown");
    first?.focus();
  }, [view.name]);

  // The search asks the values again after a short pause
  useEffect(() => {
    if (search === query) {
      return undefined;
    }
    const timer = setTimeout(() => setQuery(search), SEARCH_DELAY);
    return () => clearTimeout(timer);
  }, [search, query]);

  // Values of the column with the other filters of the grid; a newer search or closing the menu cancels the request
  useEffect(() => {
    const controller = new AbortController();
    setResult(previous => ({ ...previous, loading: true }));
    dispatch(columnValuesAction(address, column, query, controller.signal))
      .then(({ values, more }) => {
        if (controller.signal.aborted) {
          return;
        }
        const initial = getInitialChecked(filter, values);
        setChecked(previous => {
          const next = new Set(previous);
          values.map(item => getValueKey(item.value)).filter(key => !seen.current.has(key)).forEach(key => {
            seen.current.add(key);
            if (initial.has(key)) {
              next.add(key);
            }
          });
          return next;
        });
        setResult({ values, more, loading: false });
      })
      .catch(() => !controller.signal.aborted && setResult({ values: [], more: false, loading: false }));
    return () => controller.abort();
  }, [dispatch, address, column, filter, query]);

  // A click outside closes it; a click on the funnel too, without opening it again
  useEffect(() => {
    const onMouseDown = (event) => {
      const target = event.target;
      const overlay = target.closest?.(OVERLAYS);
      if (container.current?.contains(target) || (overlay && !overlay.contains(container.current))) {
        return;
      }
      if (anchor.current?.contains(target)) {
        swallowAnchorClick(anchor.current);
      }
      onClose();
    };
    document.addEventListener("mousedown", onMouseDown, true);
    return () => document.removeEventListener("mousedown", onMouseDown, true);
  }, [anchor, onClose]);

  // Keys stay in the menu (the grid saves or cancels the edited row with Enter and Escape); Escape closes it unless a
  // list of a control of the custom filter is open
  const onKeyDown = (event) => {
    event.stopPropagation();
    const open = container.current?.querySelector(".p-dropdown-panel, .p-datepicker");
    if (event.key === "Escape" && !open) {
      event.preventDefault();
      onClose();
    }
  };

  const stop = (event) => event.stopPropagation();

  const onToggle = useCallback((keys, check) => {
    setTouched(true);
    setChecked(previous => {
      const next = new Set(previous);
      keys.forEach(key => (check ? next.add(key) : next.delete(key)));
      return next;
    });
  }, []);

  const { values, more, loading } = result;
  const searching = query.length > 0;
  const checkedShown = values.filter(item => checked.has(getValueKey(item.value)));

  const accept = () => {
    if (!touched && !searching) {
      onClose();
      return;
    }
    const next = buildValuesFilter(column, values, checked, !searching);
    const labels = Object.fromEntries(checkedShown
      .filter(item => item.value !== null && item.value !== undefined)
      .map(item => [getValueKey(item.value), String(item.label ?? item.value)]));
    onApply(next, labels);
  };

  const sortTexts = SORT_TEXTS[type];
  const option = (key, icon, text, onClick, disabled = false) => <button type="button" className="grid-column-filter-option"
    disabled={disabled} onClick={onClick} {...testHook(TestIds.gridColumnFilterOption, { owner: gridId, attributes: { "option-id": key } })}>
    {getIconNode(icon, { tag: "span", "aria-hidden": "true" })}
    <span className="grid-column-filter-option-label">{text}</span>
  </button>;

  const { above, ...placementStyle } = placement || {};
  return createPortal(<div ref={container} role="dialog" aria-label={t("GRID_FILTER_BUTTON", { label })} tabIndex={-1}
    className={classNames("grid-column-filter-menu", { "grid-column-filter-menu-unplaced": !placement,
      "grid-column-filter-menu-above": above })}
    style={{ position: "absolute", ...placementStyle }} onKeyDown={onKeyDown} onClick={stop} onMouseDown={stop}
    onContextMenu={stop} {...testHook(TestIds.gridColumnFilterMenu, { owner: gridId, attributes: { "column-id": column.name } })}>
    {view.name === "custom" ?
      <AweGridColumnFilterCustom gridId={gridId} column={column} type={type} label={label} filter={filter}
        operator={view.operator} onApply={(next) => onApply(next)} onCancel={onClose} /> :
      <>
        <div className="grid-column-filter-actions">
          {onSort && <>
            {option("sort-asc", sortTexts.asc[0], t(sortTexts.asc[1]), () => onSort("asc"))}
            {option("sort-desc", sortTexts.desc[0], t(sortTexts.desc[1]), () => onSort("desc"))}
            <div role="separator" className="grid-column-filter-separator" />
          </>}
          {option("clear", "lucide:funnel-x", t("GRID_FILTER_CLEAR", { label }), () => onApply(null), !filter)}
          {type !== "values" && <ConditionsSubmenu gridId={gridId} type={type}
            onOperator={(operator) => setView({ name: "custom", operator })}
            onCustom={() => setView({ name: "custom" })}
            onRelative={(relative) => onApply(buildRelativeDateFilter(column, relative))} />}
        </div>
        <div className="grid-column-filter-values">
          <span className="grid-column-filter-search">
            {getIconNode("lucide:search", { tag: "span", "aria-hidden": "true" })}
            <input type="search" value={search} onChange={(event) => setSearch(event.target.value)}
              placeholder={t("GRID_FILTER_SEARCH")} aria-label={t("GRID_FILTER_SEARCH")}
              {...testHook(TestIds.gridColumnFilterSearch, { owner: gridId })} />
          </span>
          {loading && values.length === 0 ?
            <p className="grid-column-filter-note" role="status">{t("GRID_FILTER_LOADING")}</p> :
            <>
              {values.length === 0 && <p className="grid-column-filter-note" role="status">{t("GRID_FILTER_NO_VALUES")}</p>}
              {values.length > 0 && <AweGridColumnFilterValues key={query} gridId={gridId} values={values} checked={checked}
                onToggle={onToggle} tree={type === "date"} label={label} />}
              {more && <p className="grid-column-filter-note" role="status">{t("GRID_FILTER_MORE_VALUES", { count: values.length })}</p>}
            </>}
        </div>
        <div className="grid-column-filter-footer">
          <button type="button" className="p-button p-component p-button-outlined p-button-sm" onClick={onClose}
            {...testHook(TestIds.gridColumnFilterCancel, { owner: gridId })}>{t("BUTTON_CANCEL")}</button>
          <button type="button" className="p-button p-component p-button-sm" onClick={accept}
            disabled={loading || checkedShown.length === 0}
            {...testHook(TestIds.gridColumnFilterAccept, { owner: gridId })}>{t("BUTTON_ACCEPT")}</button>
        </div>
      </>}
  </div>, document.body);
}

ColumnFilterMenu.propTypes = {
  gridId: PropTypes.string,
  address: PropTypes.object,
  column: PropTypes.object,
  type: PropTypes.string,
  label: PropTypes.string,
  filter: PropTypes.object,
  anchor: PropTypes.object,
  onSort: PropTypes.func,
  onApply: PropTypes.func,
  onClose: PropTypes.func
};

/**
 * Funnel of a grid column that declares a filter (#912, S13), in its header: outlined while the column is not filtered
 * and filled while it is. It opens the autofilter menu of the column.
 * @category Components
 * @subcategory Grid
 */
function AweGridColumnFilter({ address, column, label, onSort }) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const button = useRef(null);
  const gridId = address?.component;
  const { columnFilters, applyColumnFilter } = useColumnFilters(gridId);
  const type = getColumnFilterType(column);
  const filter = columnFilters.find(item => item.id === column.name);

  const close = useCallback(() => {
    setOpen(false);
    button.current?.focus();
  }, []);

  const apply = useCallback((next, labels) => {
    close();
    applyColumnFilter(column.name, next, labels);
  }, [close, applyColumnFilter, column.name]);

  const sort = useCallback((direction) => {
    close();
    onSort(direction);
  }, [close, onSort]);

  if (!type) {
    return null;
  }

  // The header of the column sorts and drags it: the funnel keeps its own clicks and keys
  const onClick = (event) => {
    event.stopPropagation();
    event.currentTarget.focus();
    setOpen(!open);
  };
  const stop = (event) => event.stopPropagation();
  const title = t("GRID_FILTER_BUTTON", { label });
  return <>
    <button ref={button} type="button" title={title} aria-label={title} aria-haspopup="dialog" aria-expanded={open}
      className={classNames("grid-column-filter-button", { "grid-column-filter-active": !!filter })}
      onClick={onClick} onKeyDown={stop} onMouseDown={stop}
      {...testHook(TestIds.gridColumnFilter, { owner: gridId, active: !!filter, attributes: { "column-id": column.name } })}>
      {getIconNode("lucide:funnel", { tag: "span", "aria-hidden": "true" })}
    </button>
    {open && <ColumnFilterMenu gridId={gridId} address={address} column={column} type={type} label={label} filter={filter}
      anchor={button} onSort={onSort ? sort : undefined} onApply={apply} onClose={close} />}
  </>;
}

AweGridColumnFilter.propTypes = {
  address: PropTypes.object,
  column: PropTypes.object.isRequired,
  label: PropTypes.string,
  onSort: PropTypes.func
};

export default AweGridColumnFilter;
