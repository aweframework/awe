import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import PropTypes from "prop-types";
import { useDispatch, useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import { OverlayPanel } from "primereact/overlaypanel";
import { Calendar } from "primereact/calendar";
import { useComponentState } from "../hooks/useComponentState";
import { updateModelWithDependencies } from "../redux/thunks/components";
import { addActionsTop } from "../redux/actions/actions";
import { getIconNode, translateLabel } from "../utilities";
import { formatDate, isDateRangeValid, toDate } from "../utilities/dates";
import { calendarPassThrough } from "../utilities/testPassThrough";
import { TestIds, testHook } from "../utilities/testIds";

// Quick filter shown as a dropdown button, and the date range quick filter (presets and a custom range)
export const QUICK_FILTER_DROPDOWN = "dropdown";
export const QUICK_FILTER_DATE_RANGE = "date-range";

/**
 * Check if a quick filter is shown as a menu button (a dropdown or a date range) instead of a segmented control
 * @param {string} display Display of the quick filter
 * @returns {boolean} The quick filter is a menu button
 */
export const isMenuQuickFilter = (display) => display === QUICK_FILTER_DROPDOWN || display === QUICK_FILTER_DATE_RANGE;

/**
 * Text of a custom date range: both dates (dd/MM/yyyy) joined by a dash, an ellipsis for the open end
 * @param {string} from Start date
 * @param {string} to End date
 * @returns {string} Text of the range
 */
export const formatDateRange = (from, to) => `${from || "…"} – ${to || "…"}`;

/**
 * Check if an option shows its count
 * @param {object} option Option
 * @returns {boolean} The option has a count
 */
const hasCount = (option) => option.count !== null && option.count !== undefined && option.count !== "";

/**
 * Quick filter of a grid as a compact menu button of the toolbar (display="dropdown" or display="date-range"): the button
 * shows «Label: chosen option» and opens a menu of the options (with their counts). Choosing an option filters the grid
 * as the segmented control does. A date range filter adds «Custom range…», two dates with Apply and Clear: it sends its
 * preset (the chosen option) as its own value and the custom range as <id>.from and <id>.to (dd/MM/yyyy), as the date range
 * criterion does, and shows the range on the button while it is set.
 * @category Components
 * @subcategory Grid
 */
function AweGridQuickFilterMenu({ id, grid }) {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const language = useSelector(state => state.settings?.language);
  const { address, model = {}, attributes = {} } = useComponentState(id);
  const values = useMemo(() => model.values || [], [model.values]);
  const [from = "", to = ""] = model.range || [];
  const dateRange = attributes.display === QUICK_FILTER_DATE_RANGE;
  const custom = dateRange && Boolean(from || to);
  // The chosen option, or the first one when none is chosen; none while a custom range is set
  const current = custom ? null : values.find((option) => option.selected) || values[0];

  const buttonRef = useRef(null);
  const overlayRef = useRef(null);
  const menuRef = useRef(null);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(["", ""]);
  const menuId = address ? `${address.view}-${address.component}-menu` : undefined;

  // The chosen option (or the first one) takes the focus when the menu opens
  useEffect(() => {
    if (open && !editing) {
      const items = [...(menuRef.current?.querySelectorAll("[role^='menuitem']") || [])];
      (items.find(item => item.getAttribute("aria-checked") === "true") || items[0])?.focus({ preventScroll: true });
    }
  }, [open, editing]);

  const close = useCallback((focus) => {
    overlayRef.current?.hide();
    setOpen(false);
    setEditing(false);
    if (focus) {
      buttonRef.current?.focus();
    }
  }, []);

  const show = useCallback((event) => {
    overlayRef.current?.show(event, buttonRef.current);
    setOpen(true);
  }, []);

  const filter = useCallback((modelData) => {
    dispatch(updateModelWithDependencies(address, modelData));
    dispatch(addActionsTop([{ type: "filter", address: { view: address.view, component: grid } }]));
  }, [dispatch, address, grid]);

  const choose = (option) => {
    close(true);
    if (option === current && option.selected) {
      return;
    }
    filter({
      values: values.map((item) => ({ ...item, selected: item === option })),
      ...(dateRange ? { range: ["", ""] } : {})
    });
  };

  // Clear: back to the option of an empty value (any date), or the first one
  const clear = () => {
    close(true);
    const empty = values.find((option) => option.value === "" || option.value === null || option.value === undefined) || values[0];
    filter({ values: values.map((item) => ({ ...item, selected: item === empty })), range: ["", ""] });
  };

  const apply = () => {
    const [start, end] = draft;
    if (!start && !end) {
      clear();
      return;
    }
    close(true);
    filter({ values: values.map((item) => ({ ...item, selected: false })), range: [start, end] });
  };

  const editRange = () => {
    setDraft([from, to]);
    setEditing(true);
  };

  const onButtonKeyDown = (event) => {
    if ((event.key === "ArrowDown" || event.key === "ArrowUp") && !open) {
      event.preventDefault();
      show(event);
    }
  };

  const onMenuKeyDown = (event) => {
    const items = [...menuRef.current.querySelectorAll("[role^='menuitem']")];
    const index = items.indexOf(document.activeElement);
    const moves = { ArrowDown: index + 1, ArrowUp: index < 0 ? items.length - 1 : index - 1, Home: 0, End: items.length - 1 };
    if (event.key in moves) {
      event.preventDefault();
      items[(moves[event.key] + items.length) % items.length]?.focus();
    } else if (event.key === "Escape") {
      event.preventDefault();
      event.stopPropagation();
      close(true);
    } else if (event.key === "Tab") {
      close(false);
    }
  };

  const onRangeKeyDown = (event) => {
    if (event.key === "Escape") {
      event.preventDefault();
      event.stopPropagation();
      close(true);
    }
  };

  if (!address || attributes.visible === false) {
    return null;
  }

  const label = attributes.label ? translateLabel(attributes.label, t) : "";
  const text = custom ? formatDateRange(from, to) : translateLabel(current?.label, t) || "";
  const valid = isDateRangeValid(draft[0], draft[1]);
  const customLabel = t("DATE_RANGE_CUSTOM", { defaultValue: "Custom range…" });
  const owner = { owner: grid };
  return <div className="grid-quick-filter-dropdown" criterion-id={address.component}
    aria-busy={attributes.loading ? "true" : undefined} {...testHook(TestIds.gridQuickFilter, owner)}>
    <button ref={buttonRef} type="button" className="grid-quick-filter-toggle" aria-haspopup="menu" aria-expanded={open}
      aria-controls={open ? menuId : undefined} disabled={!!attributes.readonly}
      onClick={(event) => (open ? close(false) : show(event))} onKeyDown={onButtonKeyDown}
      {...testHook(TestIds.gridQuickFilterToggle, owner)}>
      {dateRange && getIconNode("lucide:calendar", { className: "grid-quick-filter-toggle-icon", "aria-hidden": "true" })}
      {label && <span className={dateRange ? "grid-quick-filter-toggle-sr" : "grid-quick-filter-toggle-label"}>{`${label}:`}</span>}
      {label && " "}
      <span className="grid-quick-filter-toggle-value">{text}</span>
      {getIconNode("lucide:chevron-down", { className: "grid-quick-filter-toggle-chevron", "aria-hidden": "true" })}
    </button>
    <OverlayPanel ref={overlayRef} dismissable closeOnEscape={false} className="grid-quick-filter-overlay"
      onHide={() => { setOpen(false); setEditing(false); }}>
      {!editing && <div ref={menuRef} id={menuId} role="menu" aria-label={label || t("SCREEN_TEXT_FILTERS")}
        className="grid-quick-filter-menu" onKeyDown={onMenuKeyDown} {...testHook(TestIds.gridQuickFilterMenu, owner)}>
        {values.map((option, index) => <button key={`${option.value}-${index}`} type="button" role="menuitemradio"
          tabIndex={-1} aria-checked={option === current} className="grid-quick-filter-menu-option"
          option-id={option.value === null || option.value === undefined ? "" : String(option.value)}
          onClick={() => choose(option)} {...testHook(TestIds.gridQuickFilterOption)}>
          <span className="grid-quick-filter-menu-check" aria-hidden="true">{option === current && getIconNode("lucide:check")}</span>
          <span className="grid-quick-filter-menu-text">{translateLabel(option.label, t)}</span>
          {hasCount(option) && <> <span className="grid-quick-filter-count" data-tone={option.tone || undefined}>{option.count}</span></>}
        </button>)}
        {dateRange && <>
          <div className="grid-quick-filter-menu-separator" role="separator" />
          <button type="button" role="menuitemradio" tabIndex={-1} aria-checked={custom} className="grid-quick-filter-menu-option"
            onClick={editRange} {...testHook(TestIds.gridQuickFilterCustom)}>
            <span className="grid-quick-filter-menu-check" aria-hidden="true">{custom && getIconNode("lucide:check")}</span>
            <span className="grid-quick-filter-menu-text">{customLabel}</span>
          </button>
        </>}
      </div>}
      {editing && <div role="group" aria-label={customLabel} className="grid-quick-filter-range" onKeyDown={onRangeKeyDown}
        {...testHook(TestIds.gridQuickFilterRange, owner)}>
        {[0, 1].map((index) => {
          const dateLabel = index === 0 ? t("DATE_RANGE_FROM", { defaultValue: "From" }) : t("DATE_RANGE_TO", { defaultValue: "To" });
          return <label key={index} className="grid-quick-filter-range-date">
            <span className="grid-quick-filter-range-label">{dateLabel}</span>
            <Calendar value={toDate(draft[index])} dateFormat="dd/mm/yy" appendTo="self" locale={language} showButtonBar
              autoFocus={index === 0} ariaLabel={dateLabel} invalid={index === 1 && !valid}
              minDate={index === 1 ? toDate(draft[0]) || undefined : undefined}
              maxDate={index === 0 ? toDate(draft[1]) || undefined : undefined}
              onChange={(event) => setDraft(index === 0 ? [formatDate(event.value), draft[1]] : [draft[0], formatDate(event.value)])}
              pt={calendarPassThrough(address.component, undefined,
                testHook(index === 0 ? TestIds.dateRangeFrom : TestIds.dateRangeTo, owner))} />
          </label>;
        })}
        <div className="grid-quick-filter-range-actions">
          <button type="button" className="p-button p-component p-button-sm p-button-text" onClick={clear}
            {...testHook(TestIds.gridQuickFilterRangeClear, owner)}>{t("BUTTON_CLEAR")}</button>
          <button type="button" className="p-button p-component p-button-sm" onClick={apply} disabled={!valid}
            {...testHook(TestIds.gridQuickFilterRangeApply, owner)}>{t("DATE_RANGE_APPLY", { defaultValue: "Apply" })}</button>
        </div>
      </div>}
    </OverlayPanel>
  </div>;
}

AweGridQuickFilterMenu.propTypes = {
  id: PropTypes.string,
  grid: PropTypes.string
};

export default AweGridQuickFilterMenu;
