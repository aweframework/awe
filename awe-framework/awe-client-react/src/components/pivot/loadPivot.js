import {createTableBarchartRenderer} from "./TableBarchartRenderer";

let loading = null;

/**
 * Load react-pivottable on demand (with its stylesheet): it is a heavy package.
 * @returns {Promise<{PivotTableUI: Function, renderers: Object, utilities: Object}>} The pivot table, its renderers
 * (the react-pivottable ones plus the AngularJS "Table Barchart") and its utilities, loaded once
 */
export function loadPivot() {
  loading ??= Promise.all([
    import("react-pivottable/PivotTableUI"),
    import("react-pivottable/TableRenderers"),
    import("react-pivottable/Utilities"),
    import("react-pivottable/pivottable.css")
  ]).then(([pivotTable, tableRenderers, utilities]) => ({
    PivotTableUI: pivotTable.default,
    utilities,
    renderers: {
      ...tableRenderers.default,
      "Table Barchart": createTableBarchartRenderer({TableRenderers: tableRenderers.default, PivotData: utilities.PivotData})
    }
  })).catch(error => {
    // A failed load (network) can be tried again
    loading = null;
    throw error;
  });
  return loading;
}
