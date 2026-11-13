import React, { useCallback, useEffect, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import PropTypes from "prop-types";
import PrimeReact from "primereact/api";
import { ZIndexUtils } from "primereact/utils";
import { Dialog } from "primereact/dialog";
import { Sidebar } from "primereact/sidebar";
import { useDispatch, useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import { getIconNode, translateLabel } from "../utilities";
import { updateAttributes } from "../redux/actions/components";
import { resetGridLayout, saveGridLayout } from "../redux/thunks/gridLayout";
import {
  getColumnGroups,
  mergeColumnLayout,
  moveColumn,
  moveColumnTo,
  setColumnVisible
} from "../utilities/gridLayout";
import { TestAttributes, TestIds, testHook } from "../utilities/testIds";
import { useFocusTrap } from "../hooks/useFocusTrap";
import { OVERLAYS, swallowAnchorClick } from "./AwePopover";
import "./AweGridColumnsDialog.less";

const SIDE_PANEL = "side-panel";
const DIALOG = "dialog";
const POPOVER = "popover";

// Gap between the Columns button and the popover, and the margin the popover keeps from the edges of the viewport
// (pixels, as the placement is computed from the bounding boxes)
const POPOVER_GAP = 4;
const VIEWPORT_MARGIN = 8;
// Width of the popover and the dialog (the side panel takes it as its maximum)
// Width token of the Columns, shared with the AngularJS engine (px: the root font size differs by engine)
const PANEL_STYLE = { width: "var(--awe-grid-columns-panel-width)", maxWidth: "95vw" };

/**
 * Split the columns, in layout order, into the nodes the Columns panel shows: columns without group and groups with their
 * columns nested under the title of the group header
 */
function getNodes(columnModel, headerModel, order) {
  const groups = getColumnGroups(headerModel, columnModel);
  const byName = Object.fromEntries(columnModel.map(column => [column.name, column]));
  const nodes = [];
  order.forEach(name => {
    const group = groups[name];
    const last = nodes[nodes.length - 1];
    if (group === undefined) {
      nodes.push({ column: byName[name] });
    } else if (last?.group === group) {
      last.columns.push(byName[name]);
    } else {
      nodes.push({ group, header: headerModel[group], columns: [byName[name]] });
    }
  });
  return nodes;
}

/**
 * Placement of the popover under the Columns button, aligned to its right edge, in viewport coordinates. It opens
 * above the button when it does not fit under it and there is more room above, and it never leaves the viewport: its
 * maximum height is the room on that side, so a long list of columns scrolls inside it.
 * @param {DOMRect} anchor Bounding box of the Columns button
 * @param {{width: number, height: number}} size Size of the popover
 * @returns {{placement: string, top: number, left: number, maxHeight: number}} Placement
 */
export function getPopoverPlacement(anchor, size) {
  const maxLeft = Math.max(VIEWPORT_MARGIN, window.innerWidth - size.width - VIEWPORT_MARGIN);
  const left = Math.min(Math.max(anchor.right - size.width, VIEWPORT_MARGIN), maxLeft);
  const below = anchor.bottom + POPOVER_GAP;
  const roomBelow = window.innerHeight - VIEWPORT_MARGIN - below;
  const roomAbove = anchor.top - POPOVER_GAP - VIEWPORT_MARGIN;
  if (size.height > roomBelow && roomAbove > roomBelow) {
    const height = Math.min(size.height, roomAbove);
    const top = Math.max(VIEWPORT_MARGIN, anchor.top - POPOVER_GAP - height);
    return { placement: "top", top, left, maxHeight: roomAbove };
  }
  return { placement: "bottom", top: below, left, maxHeight: roomBelow };
}

/**
 * Columns as a popover anchored to the Columns button (columns-panel="popover"): no mask, so the grid stays visible
 * while the changes apply. It closes with Esc or a mouse down outside (the Columns button included, whose click is
 * absorbed so it does not open it again), and keeps the keyboard focus inside while it is shown.
 */
function ColumnsPopover({ anchor, label, root, onHide, children }) {
  const container = useRef(null);
  const [placement, setPlacement] = useState(null);

  // Declared before the focus is moved inside, so the trap keeps the Columns button to give it the focus back
  useFocusTrap(() => container.current, true);

  // Place the popover next to the Columns button, and follow it while the page scrolls or is resized
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

  // Placed again on each render, as its height changes with the status of the last Save or Restore default
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

  // Move the focus to the first column when it opens
  useEffect(() => {
    const element = container.current;
    if (element && !element.contains(document.activeElement)) {
      (element.querySelector(".grid-columns-list input") || element).focus();
    }
  }, []);

  // Close with Esc or a mouse down outside. They are caught before they reach the document, so the popover or dialog
  // where the grid is does not close with them, nor takes a mouse down inside the Columns as a click outside it.
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
      // Overlays shown over the popover (a message) are not outside it, but the dialog where the grid is
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
      className={`grid-columns-dialog grid-columns-popover${placement ? "" : " grid-columns-popover-unanchored"}`}
      data-placement={placement?.placement} {...root}>
      {children}
    </div>,
    document.body
  );
}

ColumnsPopover.propTypes = {
  anchor: PropTypes.object,
  label: PropTypes.string,
  root: PropTypes.object,
  onHide: PropTypes.func,
  children: PropTypes.node
};

/**
 * Columns panel of a grid: it lists the columns to show, hide and reorder them. It is a popover anchored to the
 * Columns button (columns-panel="popover", the default), a modal dialog ("dialog") or a side panel anchored to the
 * right edge ("side-panel", #843), with the same content. Cancel goes back to the layout the grid had when the panel
 * was opened (or at the last Restore default); the close button, Esc and a click outside keep the changes. Save closes
 * the panel when the layout is saved, and keeps it open with an error when it is not.
 * @category Components
 * @subcategory Grid
 */
function AweGridColumnsDialog({ address, attributes, visible, onHide, anchor }) {
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const dragged = useRef(null);
  const sidebar = useRef(null);
  const [status, setStatus] = useState(null);
  // Layout calls waiting for the server: the Columns are busy (aria-busy) while there is any
  const [pendingCalls, setPendingCalls] = useState(0);
  // A Save waits for the server: its button shows the loading look and is disabled, so the layout is not saved twice
  const [saving, setSaving] = useState(false);
  const { columnModel = [], headerModel = [], columnLayout, screenId, columnsPanel } = attributes;
  // Layout Cancel goes back to: the one of the grid when the panel was opened, or the last restored one
  const committed = useRef(columnLayout ?? null);
  // The grid columns-panel, or the application default (gridColumnsPanel setting) when the grid does not set it
  const defaultPanel = useSelector(state => state.settings?.gridColumnsPanel);
  const requested = columnsPanel ?? defaultPanel;
  const panel = requested === SIDE_PANEL || requested === DIALOG ? requested : POPOVER;
  // The side panel keeps the focus inside (the modal dialog of PrimeReact already does, the popover does it itself)
  useFocusTrap(() => sidebar.current?.getElement(), panel === SIDE_PANEL && !!visible);
  const layout = mergeColumnLayout(columnModel, headerModel, columnLayout);
  const total = layout.order.length;
  const visibleCount = total - layout.hidden.length;
  const nodes = getNodes(columnModel, headerModel, layout.order);
  const groups = getColumnGroups(headerModel, columnModel);

  const apply = useCallback((newLayout) => {
    dispatch(updateAttributes(address, { columnLayout: newLayout }));
  }, [dispatch, address]);

  const onToggle = (name, show) => apply(setColumnVisible(columnModel, headerModel, layout, name, show));
  const onMove = (name, delta) => apply(moveColumn(columnModel, headerModel, layout, name, delta));
  const errorMessage = { title: t("GRID_COLUMNS_ERROR_TITLE"), message: t("GRID_COLUMNS_ERROR_MESSAGE") };
  // Save and Restore default are silent when they work, so they feel immediate: only a failure is told
  const finish = (worked) => setStatus(worked ? null : "error");
  // Call a layout service, keeping the Columns busy until the server answers (the thunk never rejects)
  const callLayoutService = async (thunk) => {
    setPendingCalls(count => count + 1);
    try {
      return await dispatch(thunk);
    } finally {
      setPendingCalls(count => count - 1);
    }
  };
  const onReset = async () => {
    // Reset shows the XML at once, and goes back to the previous layout when the stored one could not be removed
    const previous = columnLayout ?? null;
    apply(null);
    const worked = await callLayoutService(resetGridLayout(address, screenId, errorMessage));
    if (worked) {
      committed.current = null;
    } else {
      apply(previous);
    }
    finish(worked);
  };
  // Save closes the Columns once the server answers that it worked (as the close button), so they take the saved layout
  // as the one Cancel goes back to when they are opened again; a failure keeps them open
  const onSave = async () => {
    setSaving(true);
    const worked = await callLayoutService(saveGridLayout(address, screenId, layout, errorMessage));
    setSaving(false);
    finish(worked);
    if (worked) {
      onHide();
    }
  };
  const onCancel = () => {
    apply(committed.current);
    onHide();
  };

  const onDragStart = (event, name) => {
    dragged.current = name;
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = "move";
      event.dataTransfer.setData("text/plain", name);
    }
  };
  const onDrop = (event, targetName) => {
    event.preventDefault();
    const name = dragged.current ?? event.dataTransfer?.getData("text/plain");
    dragged.current = null;
    if (name) {
      apply(moveColumnTo(columnModel, headerModel, layout, name, targetName));
    }
  };

  const siblingsOf = (name) => layout.order.filter(column => groups[column] === groups[name]);

  const renderColumn = (column) => {
    const { name } = column;
    const siblings = siblingsOf(name);
    const index = siblings.indexOf(name);
    const label = translateLabel(column.label, t) || name;
    const isVisible = !layout.hidden.includes(name);
    return <li key={name} className={`grid-column-item${isVisible ? "" : " is-hidden"}`}
      onDragOver={event => event.preventDefault()} onDrop={event => onDrop(event, name)}
      {...testHook(TestIds.gridColumnItem, { attributes: { "column-id": name } })}>
      {getIconNode("lucide:grip-vertical", { tag: "span", className: "grid-column-handle", draggable: "true",
        "aria-label": t("GRID_COLUMNS_DRAG"), onDragStart: event => onDragStart(event, name),
        ...testHook(TestIds.gridColumnHandle) })}
      <input type="checkbox" id={`${address.component}-column-${name}`} checked={isVisible}
        onChange={event => onToggle(name, event.target.checked)}
        {...testHook(TestIds.gridColumnCheckbox)} />
      <label htmlFor={`${address.component}-column-${name}`}>{label}</label>
      <span className="grid-column-actions">
        <button type="button" className="grid-column-move p-link" title={t("GRID_COLUMNS_MOVE_UP")}
          aria-label={t("GRID_COLUMNS_MOVE_UP")} disabled={index <= 0} onClick={() => onMove(name, -1)}
          {...testHook(TestIds.gridColumnUp)}>{getIconNode("pi-arrow-up")}</button>
        <button type="button" className="grid-column-move p-link" title={t("GRID_COLUMNS_MOVE_DOWN")}
          aria-label={t("GRID_COLUMNS_MOVE_DOWN")} disabled={index >= siblings.length - 1} onClick={() => onMove(name, 1)}
          {...testHook(TestIds.gridColumnDown)}>{getIconNode("pi-arrow-down")}</button>
      </span>
    </li>;
  };

  const title = t("GRID_COLUMNS_TITLE");
  const titleId = `${address.component}-columns-title`;
  const header = <div className="grid-columns-header">
    <div className="grid-columns-titles">
      <p id={titleId} className="grid-columns-title">{title}</p>
      <p className="grid-columns-counter"
        {...testHook(TestIds.gridColumnsCounter, { attributes: { [TestAttributes.visibleCount]: String(visibleCount), [TestAttributes.totalCount]: String(total) } })}>
        {`${visibleCount} ${t("SCREEN_TEXT_GRID_OF")} ${total} ${t("GRID_COLUMNS_VISIBLE")}`}
      </p>
    </div>
    <button type="button" className="grid-columns-close p-button p-component p-button-text p-button-icon-only p-button-sm"
      title={t("GRID_COLUMNS_CLOSE")} aria-label={t("GRID_COLUMNS_CLOSE")} onClick={onHide}
      {...testHook(TestIds.gridColumnsClose)}>
      {getIconNode("lucide:x", { tag: "span", "aria-hidden": "true" })}
    </button>
  </div>;

  const footer = <div className="grid-columns-actions">
    <button type="button" className="grid-columns-reset p-button p-component p-button-text p-button-sm" onClick={onReset}
      {...testHook(TestIds.gridColumnsReset)}>
      {getIconNode("lucide:rotate-ccw", { tag: "span", className: "grid-columns-reset-icon", "aria-hidden": "true" })}
      <span className="p-button-label">{t("GRID_COLUMNS_RESET")}</span>
    </button>
    <button type="button" className="p-button p-component p-button-outlined p-button-sm" onClick={onCancel}
      {...testHook(TestIds.gridColumnsCancel)}>{t("GRID_COLUMNS_CANCEL")}</button>
    <button type="button" className={`p-button p-component p-button-sm${saving ? " is-loading" : ""}`} onClick={onSave}
      disabled={saving} {...testHook(TestIds.gridColumnsSave)}>
      {saving && getIconNode("lucide:loader-circle", {
        tag: "span", className: "p-button-icon p-button-icon-left awe-icon-spin", "aria-hidden": "true"
      })}
      <span className="p-button-label">{t(saving ? "GRID_COLUMNS_SAVING" : "GRID_COLUMNS_SAVE")}</span>
    </button>
  </div>;

  const content = <div className="grid-columns-body">
      {status && <div className="grid-columns-status" role="status" {...testHook(TestIds.gridColumnsStatus, { type: status })}>
        {t("GRID_COLUMNS_ERROR_MESSAGE")}
      </div>}
      <ul className="grid-columns-list">
        {nodes.map((node, index) => node.group === undefined ? renderColumn(node.column) :
          <li key={`group-${index}`} className="grid-column-group" {...testHook(TestIds.gridColumnGroup)}>
            <div className="grid-column-group-title">{translateLabel(node.header?.label, t)}</div>
            <ul>{node.columns.map(renderColumn)}</ul>
          </li>)}
      </ul>
    </div>;
  // Save and Restore default are silent: aria-busy tells that they wait for the server (and the Save button its loading look)
  const root = { ...testHook(TestIds.gridColumnsDialog, { owner: address.component, type: panel }),
    "aria-busy": pendingCalls > 0 ? "true" : undefined };

  if (panel === POPOVER) {
    return visible ? <ColumnsPopover anchor={anchor} label={title} root={root} onHide={onHide}>
      {header}
      {content}
      {footer}
    </ColumnsPopover> : null;
  }

  if (panel === SIDE_PANEL) {
    return <Sidebar ref={sidebar} visible={visible} onHide={onHide} position="right" header={header}
      showCloseIcon={false} className="grid-columns-dialog grid-columns-side-panel"
      style={{ width: "100%", maxWidth: PANEL_STYLE.width }} pt={{ root: { ...root, "aria-labelledby": titleId } }}>
      {content}
      {footer}
    </Sidebar>;
  }

  return <Dialog visible={visible} onHide={onHide} header={header} showCloseIcon={false} footer={footer}
    className="grid-columns-dialog" style={PANEL_STYLE} focusOnShow={false}
    pt={{ root: { ...root, "aria-labelledby": titleId } }}>
    {content}
  </Dialog>;
}

AweGridColumnsDialog.propTypes = {
  address: PropTypes.object.isRequired,
  attributes: PropTypes.object.isRequired,
  visible: PropTypes.bool,
  onHide: PropTypes.func,
  // Ref of the Columns button, where the popover is anchored
  anchor: PropTypes.object
};

export default AweGridColumnsDialog;
