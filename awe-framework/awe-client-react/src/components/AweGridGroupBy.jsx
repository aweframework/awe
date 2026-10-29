import React, { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import PropTypes from "prop-types";
import PrimeReact from "primereact/api";
import { ZIndexUtils } from "primereact/utils";
import { useTranslation } from "react-i18next";
import { getIconNode, translateLabel } from "../utilities";
import { getGroupingState } from "../utilities/gridGrouping";
import { applyColumnLayout } from "../utilities/gridLayout";
import { TestIds, testHook } from "../utilities/testIds";
import { useFocusTrap } from "../hooks/useFocusTrap";
import { useGroupingActions } from "../hooks/useGridGrouping";
import { OVERLAYS, swallowAnchorClick } from "./AwePopover";
import { getPopoverPlacement } from "./AweGridColumnsDialog";
import "./AweGridGroups.less";

const PANEL_STYLE = { width: "var(--awe-grid-columns-panel-width)", maxWidth: "95vw" };
const ICON_BUTTON = "p-button p-component p-button-outlined p-button-sm p-button-icon-only grid-toolbar-button grid-group-icon-button";

/**
 * Popover of the Group by button, as the popover of the Columns (#813): anchored to its button, without mask, closed
 * with Esc or a mouse down outside, and with the keyboard focus kept inside while it is shown
 */
function GroupByPopover({ anchor, label, root, onHide, children }) {
  const container = useRef(null);
  const [placement, setPlacement] = useState(null);

  // Declared before the focus is moved inside, so the trap keeps the Group by button to give it the focus back
  useFocusTrap(() => container.current, true);

  const place = useCallback(() => {
    const element = container.current;
    const anchorElement = anchor?.current;
    if (!element || !anchorElement?.isConnected) {
      setPlacement(null);
      return;
    }
    const { width, height } = element.getBoundingClientRect();
    const next = getPopoverPlacement(anchorElement.getBoundingClientRect(), { width, height });
    setPlacement(previous => previous && previous.placement === next.placement && previous.top === next.top &&
      previous.left === next.left && previous.maxHeight === next.maxHeight ? previous : next);
  }, [anchor]);

  useLayoutEffect(place);

  useLayoutEffect(() => {
    window.addEventListener("resize", place);
    window.addEventListener("scroll", place, true);
    return () => {
      window.removeEventListener("resize", place);
      window.removeEventListener("scroll", place, true);
    };
  }, [place]);

  // Stack over the screen, a maximized grid and the dialogs where the grid is
  useLayoutEffect(() => {
    const element = container.current;
    ZIndexUtils.set("overlay", element, PrimeReact.autoZIndex, PrimeReact.zIndex.overlay);
    return () => ZIndexUtils.clear(element);
  }, []);

  // Move the focus to the first option when it opens
  useEffect(() => {
    const element = container.current;
    if (element && !element.contains(document.activeElement)) {
      (element.querySelector("button, input") || element).focus();
    }
  }, []);

  // Close with Esc or a mouse down outside, caught before they reach the document (as the Columns popover)
  useEffect(() => {
    const onKeyDown = (event) => {
      if (event.key === "Escape") {
        event.stopPropagation();
        event.preventDefault();
        onHide();
      }
    };
    const onMouseDown = (event) => {
      const target = event.target;
      if (container.current?.contains(target)) {
        event.stopPropagation();
        return;
      }
      const anchorElement = anchor?.current;
      const overlay = target.closest?.(OVERLAYS);
      if (overlay && !(anchorElement && overlay.contains(anchorElement))) {
        return;
      }
      if (anchorElement?.contains(target)) {
        swallowAnchorClick(anchorElement);
      }
      onHide();
    };
    window.addEventListener("keydown", onKeyDown, true);
    window.addEventListener("mousedown", onMouseDown, true);
    return () => {
      window.removeEventListener("keydown", onKeyDown, true);
      window.removeEventListener("mousedown", onMouseDown, true);
    };
  }, [anchor, onHide]);

  const style = placement ? {
    top: `${placement.top}px`, left: `${placement.left}px`, maxHeight: `${Math.max(0, placement.maxHeight)}px`
  } : {};
  return createPortal(
    <div ref={container} role="dialog" aria-label={label} tabIndex={-1} style={{ position: "fixed", ...PANEL_STYLE, ...style }}
      className={`grid-columns-dialog grid-columns-popover grid-group-by-panel${placement ? "" : " grid-columns-popover-unanchored"}`}
      data-placement={placement?.placement} {...root}>
      {children}
    </div>,
    document.body
  );
}

GroupByPopover.propTypes = {
  anchor: PropTypes.object,
  label: PropTypes.string,
  root: PropTypes.object,
  onHide: PropTypes.func,
  children: PropTypes.node
};

/**
 * Grouping controls of the grid toolbar (#833): Expand all and Collapse all while the rows are grouped, and the Group
 * by button of a grid with group-selector, whose popover changes the grouping levels at runtime (No grouping, or the
 * checked columns in the order they were checked). Everything applies at once, without a server call.
 * @category Components
 * @subcategory Grid
 */
function AweGridGroupBy({ address, attributes }) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const button = useRef(null);
  const grouping = useMemo(() => getGroupingState(attributes), [attributes]);
  const { expandAll, collapseAll, setLevels } = useGroupingActions(address, grouping);
  const { levels, active, selector } = grouping;
  const { columnModel = [], headerModel = [], columnLayout } = attributes;
  // Columns that can group: the visible ones with a label and without summary type, and the ones grouping now
  const options = useMemo(() => applyColumnLayout(columnModel, headerModel, columnLayout)
    .filter(column => (column.label && !column.summaryType) || levels.includes(column.name)),
  [columnModel, headerModel, columnLayout, levels]);
  const close = useCallback(() => setOpen(false), []);

  if (!active && !selector) {
    return null;
  }

  const onCheck = (name, checked) => setLevels(checked ? [...levels, name] : levels.filter(level => level !== name));
  const openPanel = (event) => {
    event.currentTarget.focus();
    setOpen(true);
  };
  const title = t("GRID_GROUP_BY");
  const iconButton = (testId, label, icon, onClick) => <button type="button" className={ICON_BUTTON} title={label}
    aria-label={label} onClick={onClick} {...testHook(testId)}>
    {getIconNode(icon, { tag: "span", className: "p-button-icon", "aria-hidden": "true" })}
  </button>;

  return <>
    {active && iconButton(TestIds.gridGroupExpandAll, t("GRID_GROUP_EXPAND_ALL"), "lucide:chevrons-up-down", expandAll)}
    {active && iconButton(TestIds.gridGroupCollapseAll, t("GRID_GROUP_COLLAPSE_ALL"), "lucide:chevrons-down-up", collapseAll)}
    {selector && <button ref={button} type="button" className="p-button p-component p-button-outlined p-button-sm grid-toolbar-button"
      title={title} aria-haspopup="dialog" aria-expanded={open} onClick={openPanel} {...testHook(TestIds.gridGroupBy)}>
      {getIconNode("lucide:layers", { tag: "span", "aria-hidden": "true" })} <span className="p-button-label">{title}</span>
    </button>}
    {selector && open && <GroupByPopover anchor={button} label={title} onHide={close}
      root={testHook(TestIds.gridGroupByPanel, { owner: address.component })}>
      <div className="grid-columns-header">
        <p className="grid-columns-title">{title}</p>
      </div>
      <ul className="grid-columns-list">
        <li className="grid-column-item">
          <button type="button" className="grid-group-none" aria-pressed={levels.length === 0} onClick={() => setLevels([])}
            {...testHook(TestIds.gridGroupNone)}>{t("GRID_GROUP_NONE")}</button>
        </li>
        {options.map(column => {
          const { name } = column;
          const level = levels.indexOf(name) + 1;
          const id = `${address.component}-group-by-${name}`;
          return <li key={name} className="grid-column-item" {...testHook(TestIds.gridGroupOption, { attributes: { "data-column": name } })}>
            <input type="checkbox" id={id} checked={level > 0} onChange={event => onCheck(name, event.target.checked)} />
            <label htmlFor={id}>{translateLabel(column.label, t) || name}</label>
            {level > 0 && <span className="grid-group-level" aria-hidden="true">{level}</span>}
          </li>;
        })}
      </ul>
    </GroupByPopover>}
  </>;
}

AweGridGroupBy.propTypes = {
  address: PropTypes.object.isRequired,
  attributes: PropTypes.object.isRequired
};

export default AweGridGroupBy;
