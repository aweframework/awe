import React from "react";
import PropTypes from "prop-types";
import { getIconNode } from "../utilities";
import { isGroupRow } from "../utilities/gridGrouping";
import { TestIds, testHook } from "../utilities/testIds";
import "./AweGridGroups.less";

/**
 * Toggle of a group row: it shows or hides the content of the group. Enter and Space toggle it here, so they do not
 * reach the row or the grid.
 */
function GroupToggle({ path, expanded, onToggle, t }) {
  const label = t(expanded ? "GRID_GROUP_COLLAPSE" : "GRID_GROUP_EXPAND");
  const toggle = () => onToggle(path);
  const onKeyDown = (event) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      event.stopPropagation();
      toggle();
    }
  };
  return <button type="button" className="grid-group-toggle" aria-expanded={expanded} aria-label={label} title={label}
    onClick={toggle} onKeyDown={onKeyDown} {...testHook(TestIds.gridGroupToggle)}>
    {getIconNode("lucide:chevron-right", { tag: "span", "aria-hidden": "true" })}
  </button>;
}

GroupToggle.propTypes = {
  path: PropTypes.string.isRequired,
  expanded: PropTypes.bool,
  onToggle: PropTypes.func.isRequired,
  t: PropTypes.func.isRequired
};

/**
 * Cell of a group or subtotal row of a grouped grid (#833). The first visible column shows the toggle, the label and
 * the count of a group (indented by its level), or "Subtotal {label}"; the columns with summary type show the
 * aggregate of the group; the other cells are empty, so the label can run over them.
 * @category Components
 * @subcategory Grid
 */
function AweGridGroupCell({ row, column, firstColumn, onToggle, t }) {
  const group = isGroupRow(row) ? row.$group : null;
  const { level, label, aggregates } = group ?? row.$subtotal;
  if (column === firstColumn) {
    // Indentation by level: the first level is not indented
    const style = { "--_group-level": level - 1 };
    return group ? <span className="grid-group-label" style={style}>
      <GroupToggle path={group.path} expanded={group.expanded} onToggle={onToggle} t={t} />
      <span className="grid-group-text">{label}</span>
      <span className="grid-group-count">{group.count}</span>
    </span> : <span className="grid-subtotal-label" style={style}>{`${t("GRID_GROUP_SUBTOTAL")} ${label}`}</span>;
  }
  const aggregate = aggregates[column];
  return aggregate ? <span className="grid-group-aggregate">{aggregate.label}</span> : null;
}

AweGridGroupCell.propTypes = {
  row: PropTypes.object.isRequired,
  column: PropTypes.string.isRequired,
  firstColumn: PropTypes.string,
  onToggle: PropTypes.func.isRequired,
  t: PropTypes.func.isRequired
};

export default AweGridGroupCell;
