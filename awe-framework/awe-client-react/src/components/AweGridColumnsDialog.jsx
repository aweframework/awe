import React, { useCallback, useRef, useState } from "react";
import PropTypes from "prop-types";
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
import "./AweGridColumnsDialog.less";

const SIDE_PANEL = "side-panel";

/**
 * Split the columns, in layout order, into the nodes the dialog shows: columns without group and groups with their
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
 * Dialog that lists the columns of a grid to show, hide and reorder them. With columns-panel="side-panel" it is a
 * side panel anchored to the right edge instead of a modal dialog (#843).
 * @category Components
 * @subcategory Grid
 */
function AweGridColumnsDialog({ address, attributes, visible, onHide }) {
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const dragged = useRef(null);
  const sidebar = useRef(null);
  const [status, setStatus] = useState(null);
  const { columnModel = [], headerModel = [], columnLayout, screenId, columnsPanel } = attributes;
  // The grid columns-panel, or the application default (gridColumnsPanel setting) when the grid does not set it
  const defaultPanel = useSelector(state => state.settings?.gridColumnsPanel);
  const asSidePanel = (columnsPanel ?? defaultPanel) === SIDE_PANEL;
  // The side panel keeps the focus inside (the modal dialog of PrimeReact already does)
  useFocusTrap(() => sidebar.current?.getElement(), asSidePanel && !!visible);
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
  const onShowAll = () => apply({ order: layout.order, hidden: [] });
  const errorMessage = { title: t("GRID_COLUMNS_ERROR_TITLE"), message: t("GRID_COLUMNS_ERROR_MESSAGE") };
  const finish = (worked) => setStatus(worked ? "saved" : "error");
  const onReset = async () => {
    // Reset shows the XML at once, and goes back to the previous layout when the stored one could not be removed
    const previous = columnLayout ?? null;
    apply(null);
    const worked = await dispatch(resetGridLayout(address, screenId, errorMessage));
    if (!worked) {
      apply(previous);
    }
    finish(worked);
  };
  const onSave = async () => finish(await dispatch(saveGridLayout(address, screenId, layout, errorMessage)));

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
    return <li key={name} className="grid-column-item"
      onDragOver={event => event.preventDefault()} onDrop={event => onDrop(event, name)}
      {...testHook(TestIds.gridColumnItem, { attributes: { "column-id": name } })}>
      {getIconNode("pi-bars", { tag: "span", className: "grid-column-handle", draggable: "true",
        "aria-label": t("GRID_COLUMNS_DRAG"), onDragStart: event => onDragStart(event, name),
        ...testHook(TestIds.gridColumnHandle) })}
      <input type="checkbox" id={`${address.component}-column-${name}`} checked={isVisible}
        onChange={event => onToggle(name, event.target.checked)}
        {...testHook(TestIds.gridColumnCheckbox)} />
      <label htmlFor={`${address.component}-column-${name}`}>{label}</label>
      <button type="button" className="grid-column-move p-link" title={t("GRID_COLUMNS_MOVE_UP")}
        aria-label={t("GRID_COLUMNS_MOVE_UP")} disabled={index <= 0} onClick={() => onMove(name, -1)}
        {...testHook(TestIds.gridColumnUp)}>{getIconNode("pi-arrow-up")}</button>
      <button type="button" className="grid-column-move p-link" title={t("GRID_COLUMNS_MOVE_DOWN")}
        aria-label={t("GRID_COLUMNS_MOVE_DOWN")} disabled={index >= siblings.length - 1} onClick={() => onMove(name, 1)}
        {...testHook(TestIds.gridColumnDown)}>{getIconNode("pi-arrow-down")}</button>
    </li>;
  };

  const footer = <div className="grid-columns-actions">
    <button type="button" className="p-button p-component p-button-text p-button-sm" onClick={onShowAll}
      {...testHook(TestIds.gridColumnsShowAll)}>{t("GRID_COLUMNS_SHOW_ALL")}</button>
    <button type="button" className="p-button p-component p-button-text p-button-sm" onClick={onReset}
      {...testHook(TestIds.gridColumnsReset)}>{t("GRID_COLUMNS_RESET")}</button>
    <button type="button" className="p-button p-component p-button-sm" onClick={onSave}
      {...testHook(TestIds.gridColumnsSave)}>{t("GRID_COLUMNS_SAVE")}</button>
    <button type="button" className="p-button p-component p-button-text p-button-sm" onClick={onHide}
      {...testHook(TestIds.gridColumnsClose)}>{t("GRID_COLUMNS_CLOSE")}</button>
  </div>;

  const content = <div>
      {status && <div className="grid-columns-status" role="status" {...testHook(TestIds.gridColumnsStatus, { type: status })}>
        {t(status === "saved" ? "GRID_COLUMNS_SAVED" : "GRID_COLUMNS_ERROR_MESSAGE")}
      </div>}
      <div className="grid-columns-counter"
        {...testHook(TestIds.gridColumnsCounter, { attributes: { [TestAttributes.visibleCount]: String(visibleCount), [TestAttributes.totalCount]: String(total) } })}>
        {`${visibleCount} ${t("SCREEN_TEXT_GRID_OF")} ${total} ${t("GRID_COLUMNS_VISIBLE")}`}
      </div>
      <ul className="grid-columns-list">
        {nodes.map((node, index) => node.group === undefined ? renderColumn(node.column) :
          <li key={`group-${index}`} className="grid-column-group" {...testHook(TestIds.gridColumnGroup)}>
            <div className="grid-column-group-title">{translateLabel(node.header?.label, t)}</div>
            <ul>{node.columns.map(renderColumn)}</ul>
          </li>)}
      </ul>
    </div>;
  const root = testHook(TestIds.gridColumnsDialog, { owner: address.component, type: asSidePanel ? SIDE_PANEL : "dialog" });

  if (asSidePanel) {
    return <Sidebar ref={sidebar} visible={visible} onHide={onHide} position="right"
      header={<span className="grid-columns-title">{t("GRID_COLUMNS_TITLE")}</span>}
      className="grid-columns-dialog grid-columns-side-panel" style={{ width: "100%", maxWidth: "28rem" }}
      pt={{ root: root }}>
      {content}
      {footer}
    </Sidebar>;
  }

  return <Dialog visible={visible} onHide={onHide} header={t("GRID_COLUMNS_TITLE")} footer={footer}
    className="grid-columns-dialog" style={{ width: "28rem", maxWidth: "95vw" }} focusOnShow={false}
    pt={{ root: root }}>
    {content}
  </Dialog>;
}

AweGridColumnsDialog.propTypes = {
  address: PropTypes.object.isRequired,
  attributes: PropTypes.object.isRequired,
  visible: PropTypes.bool,
  onHide: PropTypes.func
};

export default AweGridColumnsDialog;
