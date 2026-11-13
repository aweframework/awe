// Type-level assertions for utilities/grid, checked by `npm run typecheck` (not run by Jest)
import {
  MOVE_ROW_SHORTCUTS,
  MoveRowActionType,
  OperationIcon,
  OperationIconName,
  OperationType,
  RowPositionType,
  SELECTION_COLUMN_WIDTH,
  calculateFooterValue,
  extractCellLabels,
  extractCellModel,
  extractCellValue,
  extractCellValues,
  filterRow,
  getCell,
  getCellAttribute,
  getCellModel,
  getCellSuggestData,
  getCellValue,
  getColumnDefinition,
  getColumnWrap,
  getDirection,
  getEditingRow,
  getEditingRowIndex,
  getExistingIndex,
  getFooterValue,
  getGridData,
  getGridIdentifier,
  getGridPrintData,
  getMoveRowKeyPosition,
  getRow,
  getRowIndex,
  getRowNumberWidthStyle,
  getSelectedRowIndex,
  getTranslateFunction,
  getWidth,
  getWidthStyle,
  keySort,
  moveRowInValues,
  sortRow
} from "../../../../src/utilities/grid";

const rows = [{id: 1, name: "Ada", selected: true}, {id: 2, name: "Alan"}];
const options = [{value: 1, label: "One", selected: false}, {value: 2, label: "Two", selected: true}];

// The identifier of the grid is text
const identifier: string = getGridIdentifier({treegrid: true, treeId: "node"});
const defaultIdentifier: string = getGridIdentifier();
// @ts-expect-error treegrid is a flag
getGridIdentifier({treegrid: "yes"});

// The positions and the operations are literals
const before: "BEFORE" = RowPositionType.BEFORE;
const up: "move-row-up" = MoveRowActionType.BEFORE;
const insert: "INSERT" = OperationType.INSERT;
const icon: string = OperationIcon.UPDATE;
const iconName: "trash" = OperationIconName.DELETE;
const width: string = SELECTION_COLUMN_WIDTH;
const shortcutPosition: "BEFORE" | "AFTER" | "FIRST" | "LAST" = MOVE_ROW_SHORTCUTS[0].position;
// @ts-expect-error there is no such position
const unknownPosition = RowPositionType.MIDDLE;
// @ts-expect-error an operation is a literal, not any text
const wrongOperation: "UPDATE" = OperationType.INSERT;

// The position a key moves a row to is a position, or null
const keyPosition: "BEFORE" | "AFTER" | "FIRST" | "LAST" | null = getMoveRowKeyPosition(new KeyboardEvent("keydown"));
// @ts-expect-error the position may be null
const keyDefinite: string = getMoveRowKeyPosition(new KeyboardEvent("keydown"));

// A moved list keeps the type of its rows
const moved: typeof rows = moveRowInValues(rows, "id", 1, RowPositionType.LAST);
const movedByText: typeof rows = moveRowInValues(rows, "id", "1", "FIRST");
// @ts-expect-error the rows are a list
moveRowInValues({id: 1}, "id", 1, "LAST");

// The cell is read from an attribute of the model, or it is the value itself
const attribute: unknown = getCellAttribute({value: 1}, "value");
const cell: unknown = getCell(rows, 0, "name");
// @ts-expect-error the attribute is text
getCellAttribute({value: 1}, 1);
// @ts-expect-error the row index is a number
getCell(rows, "0", "name");

// The cell values of a list of options are text; the cell values of a model are the model values
const selectedText: string = extractCellValue(options);
const modelValue: unknown = extractCellValue({value: 1});
const valueList: unknown[] = extractCellValues(options);
const modelValues: unknown = extractCellValues("text");
const labels: string = extractCellLabels(options);
const model: {value?: unknown; label?: unknown; selected?: unknown} = extractCellModel(options);
const textModel: {value?: unknown} = extractCellModel("text");
const cellValue: unknown = getCellValue(rows, 1, "name");
const footerValue: unknown = getFooterValue({name: "Total"}, "name");
const defaultFooter: unknown = getFooterValue(undefined, "name");
// @ts-expect-error the text of a list of options is not a number
const wrongSelected: number = extractCellValue(options);
// @ts-expect-error the values of a list of options are a list
const wrongValues: string = extractCellValues(options);

// The model of a cell has a value
const cellModel: {value?: unknown; label?: unknown} = getCellModel("a", {model: {values: [{value: "a", label: "A"}]}});
const emptyModel: {value?: unknown} = getCellModel();
// @ts-expect-error the values of a column model are a list of options
getCellModel("a", {model: {values: "a"}});

// The indexes are numbers
const index: number = getRowIndex(rows, "2");
const selectedIndex: number = getSelectedRowIndex(rows);
const editingIndex: number = getEditingRowIndex(rows);
const existing: number = getExistingIndex([-1, "a", 3, 4]);
// @ts-expect-error the index is a number
const wrongIndex: string = getRowIndex(rows, 1);
// @ts-expect-error the id of a row is text or a number
getRowIndex(rows, {});

// The editing row is a row, or an empty row
const editing: {id?: number; name?: string} = getEditingRow(rows);
const editingName: string | undefined = getEditingRow(rows).name;
// @ts-expect-error the editing row may not have a name
const editingDefinite: string = getEditingRow(rows).name;

// The rows are filtered and sorted
const filtered: boolean = filterRow({name: "Ada"}, {name: {matchMode: "startsWith", value: "A"}});
const keyOrder: number = keySort("a", "b", -1);
const rowOrder: number = sortRow(rows[0], rows[1], [{id: "name", order: 1}]);
const direction: 1 | -1 = getDirection("asc");
// @ts-expect-error a filter needs its match mode
filterRow({name: "Ada"}, {name: {value: "A"}});
// @ts-expect-error the sort list has identifiers
sortRow(rows[0], rows[1], [{order: 1}]);
// @ts-expect-error the direction is text
getDirection(1);

// The width styles are text, empty when there is no width
const rowNumberStyle: {flex?: string; width?: string} = getRowNumberWidthStyle(10, 5);
const widthStyle: {flex?: string; width?: string} = getWidthStyle(5, null, "10rem");
const pixels: number = getWidth(5, null);
const flex: string | undefined = getWidthStyle(5).flex;
// @ts-expect-error the width is in pixels
getWidth("5");
// @ts-expect-error the width style may have no flex
const flexDefinite: string = getWidthStyle(5).flex;

// The wrap of a column is a flag and a number of lines, or null
const wrap: {wrap: boolean; maxLines: number | null} = getColumnWrap({wrap: true, maxLines: 3});
const defaultWrap: {wrap: boolean; maxLines: number | null} = getColumnWrap();
// @ts-expect-error the lines are a number
getColumnWrap({maxLines: "3"});

// The footer is a value and a label
const footer: {value: number | null; label: string} = calculateFooterValue({name: "age", summaryType: "sum"}, rows);
// @ts-expect-error the column has a name
calculateFooterValue({summaryType: "sum"}, rows);

// A row and a column are found in a grid
const grid = {attributes: {id: "Grd", columnModel: [{name: "name", sendable: true}]}, address: {view: "report", component: "Grd"}, model: {values: rows}};
const row: (typeof rows)[number] | undefined = getRow(grid, 1);
const column: {name: string} | undefined = getColumnDefinition(grid, "name");
// @ts-expect-error the row may not exist
const rowDefinite: (typeof rows)[number] = getRow(grid, 1);

// The grid data is indexed by text
const data: Record<string, unknown> = getGridData(grid, grid.model, {}, false);
const printData: Record<string, unknown> = getGridPrintData(grid, grid.model, {t: (key: string) => key});
// @ts-expect-error the grid has attributes
getGridData({address: {view: "report"}}, grid.model, {}, false);

// The translation of a cell depends on its component
const translate: (value: never) => unknown = getTranslateFunction({component: "numeric"}, {value: 1});
const suggestData: unknown[] = getCellSuggestData({values: options}, {value: 3});
// @ts-expect-error the attributes have a component text
getTranslateFunction({component: 1}, {value: 1});

export {identifier, defaultIdentifier, before, up, insert, icon, iconName, width, shortcutPosition, unknownPosition, wrongOperation};
export {keyPosition, keyDefinite, moved, movedByText, attribute, cell, selectedText, modelValue, valueList, modelValues};
export {labels, model, textModel, cellValue, footerValue, defaultFooter, wrongSelected, wrongValues, cellModel, emptyModel};
export {index, selectedIndex, editingIndex, existing, wrongIndex, editing, editingName, editingDefinite, filtered, keyOrder};
export {rowOrder, direction, rowNumberStyle, widthStyle, pixels, flex, flexDefinite, wrap, defaultWrap, footer, row, column};
export {rowDefinite, data, printData, translate, suggestData};
