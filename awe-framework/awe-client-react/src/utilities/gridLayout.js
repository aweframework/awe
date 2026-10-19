/**
 * Grid column layout utilities: the order and visibility of the columns chosen by the user, and the group headers
 * computed from the membership of the columns.
 *
 * The columns the user can manage are the ones the screen does not hide (`hidden` is not set). A column never leaves
 * its group header: each group keeps the positions it has in the screen and its columns are reordered among them, and
 * the columns without group are reordered among themselves.
 *
 * @category Utilities
 * @namespace GridLayout
 */

const NO_GROUP = "none";

// A stored layout that is not well formed (lists that are not lists, ids that are not text) is ignored
const idList = (value) => (Array.isArray(value) ? value.filter(id => typeof id === "string") : []);

const getManageable = (columnModel = []) => columnModel.filter(column => !column.hidden);

/**
 * Retrieve the group header index of each column that belongs to a group
 *
 * @param {Array} headerModel Group headers of the grid
 * @param {Array} columnModel Column model of the grid
 * @returns {Object} Group index by column name
 * @memberOf GridLayout
 */
export function getColumnGroups(headerModel = [], columnModel = []) {
  const groups = {};
  headerModel.forEach((header, index) => {
    let names = header.columnNames;
    if (!Array.isArray(names)) {
      // Headers sent without their members: they are the visible columns that follow the start column
      const visible = columnModel.filter(column => !column.hidden);
      const start = visible.findIndex(column => column.name === header.startColumnName);
      names = start < 0 ? [] : visible.slice(start, start + (header.numberOfColumns || 0)).map(column => column.name);
    }
    names.forEach(name => {
      groups[name] = index;
    });
  });
  return groups;
}

const groupKey = (groups, name) => (name in groups ? String(groups[name]) : NO_GROUP);

/**
 * Order the members of a group: known columns by their stored position and new ones at their own position
 */
function orderMembers(members, storedPosition) {
  const ordered = members.filter(name => name in storedPosition)
    .sort((first, second) => storedPosition[first] - storedPosition[second]);
  members.forEach((name, index) => {
    if (!(name in storedPosition)) {
      ordered.splice(Math.min(index, ordered.length), 0, name);
    }
  });
  return ordered;
}

/**
 * Merge a layout with the columns of the screen: unknown columns are ignored, new columns are visible at their
 * position, columns never leave their group and at least one column stays visible
 *
 * @param {Array} columnModel Whole column model of the grid
 * @param {Array} headerModel Group headers of the grid
 * @param {{order: string[], hidden: string[]}} [layout] Layout to merge
 * @returns {{order: string[], hidden: string[]}} Complete layout
 * @memberOf GridLayout
 */
export function mergeColumnLayout(columnModel = [], headerModel = [], layout) {
  const names = getManageable(columnModel).map(column => column.name);
  const groups = getColumnGroups(headerModel, columnModel);
  const stored = layout && typeof layout === "object" ? layout : {};
  const storedPosition = {};
  idList(stored.order).forEach(name => {
    if (names.includes(name) && !(name in storedPosition)) {
      storedPosition[name] = Object.keys(storedPosition).length;
    }
  });

  const slots = {};
  const members = {};
  names.forEach((name, position) => {
    const key = groupKey(groups, name);
    (slots[key] = slots[key] || []).push(position);
    (members[key] = members[key] || []).push(name);
  });

  const order = new Array(names.length);
  Object.keys(members).forEach(key => {
    orderMembers(members[key], storedPosition).forEach((name, index) => {
      order[slots[key][index]] = name;
    });
  });

  let hidden = [...new Set(idList(stored.hidden).filter(name => names.includes(name)))];
  if (hidden.length >= names.length) {
    hidden = [];
  }
  return { order, hidden };
}

/**
 * Retrieve the columns to show: the ones of the screen, in the order of the layout and without the hidden ones
 *
 * @param {Array} columnModel Whole column model of the grid
 * @param {Array} headerModel Group headers of the grid
 * @param {{order: string[], hidden: string[]}} [layout] Layout of the user
 * @returns {Array} Visible columns
 * @memberOf GridLayout
 */
export function applyColumnLayout(columnModel = [], headerModel = [], layout) {
  if (!layout) {
    return getManageable(columnModel);
  }
  const { order, hidden } = mergeColumnLayout(columnModel, headerModel, layout);
  const byName = Object.fromEntries(getManageable(columnModel).map(column => [column.name, column]));
  return order.filter(name => !hidden.includes(name)).map(name => byName[name]);
}

/**
 * Compute the group headers from the membership of the visible columns: each header spans the visible columns of its
 * group and a group without visible columns is not shown.
 *
 * @param {Array} visibleColumns Visible columns in display order
 * @param {Array} headerModel Group headers of the grid
 * @param {Array} columnModel Whole column model of the grid
 * @returns {{columns: Array, grouped: Array}} Top level columns and group headers, and the columns under a header
 * @memberOf GridLayout
 */
export function buildHeader(visibleColumns = [], headerModel = [], columnModel = visibleColumns) {
  const groups = getColumnGroups(headerModel, columnModel);
  const columns = [];
  const grouped = [];
  let current = null;
  visibleColumns.forEach(column => {
    if (column.name in groups) {
      const group = groups[column.name];
      if (current?.group === group) {
        current.header.numberOfColumns += 1;
      } else {
        current = { group, header: { ...headerModel[group], startColumnName: column.name, numberOfColumns: 1 } };
        columns.push(current.header);
      }
      grouped.push(column);
    } else {
      current = null;
      columns.push(column);
    }
  });
  return { columns, grouped };
}

/**
 * Move a column among its siblings (the columns of its group, or the ones without group)
 *
 * @param {Array} columnModel Whole column model of the grid
 * @param {Array} headerModel Group headers of the grid
 * @param {{order: string[], hidden: string[]}} layout Current layout
 * @param {string} name Column to move
 * @param {number} delta Positions to move (negative moves it up)
 * @returns {{order: string[], hidden: string[]}} New layout, the same one when the column cannot move
 * @memberOf GridLayout
 */
export function moveColumn(columnModel, headerModel, layout, name, delta) {
  const merged = mergeColumnLayout(columnModel, headerModel, layout);
  const groups = getColumnGroups(headerModel, columnModel);
  if (!merged.order.includes(name)) {
    return layout;
  }
  const key = groupKey(groups, name);
  const slots = merged.order.map((column, position) => (groupKey(groups, column) === key ? position : -1)).filter(position => position >= 0);
  const siblings = slots.map(position => merged.order[position]);
  const from = siblings.indexOf(name);
  const to = from + delta;
  if (to < 0 || to >= siblings.length) {
    return layout;
  }
  siblings.splice(to, 0, siblings.splice(from, 1)[0]);
  const order = [...merged.order];
  slots.forEach((position, index) => {
    order[position] = siblings[index];
  });
  return { order, hidden: merged.hidden };
}

/**
 * Put a column where one of its siblings is (the drop of a drag and drop). Columns of other groups are ignored.
 *
 * @param {Array} columnModel Whole column model of the grid
 * @param {Array} headerModel Group headers of the grid
 * @param {{order: string[], hidden: string[]}} layout Current layout
 * @param {string} name Column to move
 * @param {string} targetName Sibling that takes the position of the column
 * @returns {{order: string[], hidden: string[]}} New layout, the same one when the column cannot move there
 * @memberOf GridLayout
 */
export function moveColumnTo(columnModel, headerModel, layout, name, targetName) {
  const merged = mergeColumnLayout(columnModel, headerModel, layout);
  const groups = getColumnGroups(headerModel, columnModel);
  if (!merged.order.includes(name) || !merged.order.includes(targetName) || groupKey(groups, name) !== groupKey(groups, targetName)) {
    return layout;
  }
  const siblings = merged.order.filter(column => groupKey(groups, column) === groupKey(groups, name));
  return moveColumn(columnModel, headerModel, layout, name, siblings.indexOf(targetName) - siblings.indexOf(name));
}

/**
 * Show or hide a column. The last visible column cannot be hidden.
 *
 * @param {Array} columnModel Whole column model of the grid
 * @param {Array} headerModel Group headers of the grid
 * @param {{order: string[], hidden: string[]}} layout Current layout
 * @param {string} name Column to change
 * @param {boolean} visible Show or hide
 * @returns {{order: string[], hidden: string[]}} New layout
 * @memberOf GridLayout
 */
export function setColumnVisible(columnModel, headerModel, layout, name, visible) {
  const merged = mergeColumnLayout(columnModel, headerModel, layout);
  if (!merged.order.includes(name)) {
    return layout;
  }
  if (visible) {
    return { order: merged.order, hidden: merged.hidden.filter(column => column !== name) };
  }
  if (merged.hidden.includes(name) || merged.order.length - merged.hidden.length <= 1) {
    return layout;
  }
  return { order: merged.order, hidden: [...merged.hidden, name] };
}
