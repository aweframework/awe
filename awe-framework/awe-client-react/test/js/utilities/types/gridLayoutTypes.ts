// Type-level assertions for utilities/gridLayout, checked by `npm run typecheck` (not run by Jest)
import {
  ColumnLayout,
  applyColumnLayout,
  buildHeader,
  getColumnGroups,
  mergeColumnLayout,
  moveColumn,
  moveColumnTo,
  setColumnVisible
} from "../../../../src/utilities/gridLayout";

type Column = {name: string; hidden?: boolean; label: string};
const columnModel: Column[] = [{name: "a", label: "A"}, {name: "b", label: "B", hidden: true}];
const headerModel = [{columnNames: ["a"], titleText: "Group"}, {startColumnName: "a", numberOfColumns: 2}];
const layout: ColumnLayout = {order: ["a"], hidden: []};
// @ts-expect-error the order is a list of names
const wrongLayout: ColumnLayout = {order: "a", hidden: []};

// The group of each column is indexed by the column name
const groups: Record<string, number> = getColumnGroups(headerModel, columnModel);

// The merged layout is always complete
const merged: {order: string[]; hidden: string[]} = mergeColumnLayout(columnModel, headerModel, layout);
const mergedWithout: ColumnLayout = mergeColumnLayout(columnModel, headerModel);
// @ts-expect-error the layout to merge is optional but must have lists
mergeColumnLayout(columnModel, headerModel, {order: 1});

// The visible columns keep the type of the columns of the model
const visible: Column[] = applyColumnLayout(columnModel, headerModel, layout);
const visibleLabels: string[] = applyColumnLayout(columnModel, headerModel).map(column => column.label);
// @ts-expect-error the columns are not texts
const wrongVisible: string[] = applyColumnLayout(columnModel, headerModel, layout);

// The header keeps the type of the columns and adds the group headers
const header = buildHeader(visible, headerModel, columnModel);
const grouped: Column[] = header.grouped;
const top: number = header.columns.length;
// @ts-expect-error the grouped columns are not texts
const wrongGrouped: string[] = header.grouped;

// Moving or hiding a column answers with a layout
const moved: ColumnLayout = moveColumn(columnModel, headerModel, layout, "a", 1);
const movedTo: ColumnLayout = moveColumnTo(columnModel, headerModel, layout, "a", "b");
const hidden: ColumnLayout = setColumnVisible(columnModel, headerModel, layout, "a", false);
// @ts-expect-error the shift is a number
moveColumn(columnModel, headerModel, layout, "a", "1");
// @ts-expect-error the visibility is a boolean
setColumnVisible(columnModel, headerModel, layout, "a", "no");

export {wrongLayout, groups, merged, mergedWithout, visible, visibleLabels, wrongVisible, grouped, top, wrongGrouped, moved, movedTo, hidden};
