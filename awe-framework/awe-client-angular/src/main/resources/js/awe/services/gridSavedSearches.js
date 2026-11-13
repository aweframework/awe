import {
  getGridSearchRestore,
  getGridSearchState,
  parseGridSearchValues
} from "awe-client-react/utilities/gridSavedSearches";
import {initSavedSearches} from "./savedSearches";

// Saved searches of a grid (#912, saved-searches="true"): the Saved searches menu of the window (#790) in the toolbar of
// the grid. A search keeps the column filters of the grid (with the labels of their values), its sort, the value of its
// quick filter and the text of its search, read and restored with the rules of the React client
// (awe-client-react/utilities/gridSavedSearches), so a search saved in one client applies in the other. The default
// search of the grid (#945) is applied by the server before its initial load: the grid takes its column filters and its
// sort without a reload (its quick filter and its search already have their values), and Clear all of the column filters
// leaves it: no column filters nor sort, as the XML. The quick filter and the search keep their own controls.

/**
 * Text of a value: an empty value (null, undefined or an empty list) is the empty text
 * @param {*} value Value
 * @returns {string} Text
 */
const asText = value => {
  const first = Array.isArray(value) ? value[0] : value;
  return first === null || first === undefined ? "" : String(first);
};

/**
 * Option a quick filter has chosen: the one with its value, or the first one
 * @param {object} model Model of the quick filter
 * @param {*} value Value
 * @returns {object|undefined} Option
 */
const chosenOption = (model, value) => {
  const options = (model && model.values) || [];
  return options.find(option => asText(option.value) === asText(value)) || options[0];
};

/**
 * Initialize the saved searches of a grid that opts in, in its toolbar
 * @param {object} scope Toolbar scope
 * @param {object} elem Toolbar element
 * @param {object} component Grid component
 * @param {object} services $compile, Control, Utilities, $actionController, $settings, Storage and Connection services
 * @returns {object} Saved searches of the grid
 */
export function initGridSavedSearches(scope, elem, component, {$compile, Control, Utilities, $actionController, $settings, Storage, Connection}) {
  const {view, component: gridId} = component.address;
  const controller = () => component.controller || {};
  const addressOf = id => ({view, component: id});
  const modelOf = id => (id ? Control.getAddressModel(addressOf(id)) : null);

  /**
   * State of the grid a search keeps
   * @returns {object} State
   */
  const readState = () => {
    const {quickFilter, search} = controller();
    const quickFilterOption = quickFilter ? chosenOption(modelOf(quickFilter), (modelOf(quickFilter) || {}).selected) : null;
    return getGridSearchState({
      columnFilters: component.columnFilters,
      columnFilterLabels: component.columnFilterLabels,
      sort: component.grid ? component.grid.sorting : [],
      hasQuickFilter: Boolean(quickFilter),
      quickFilter: quickFilterOption ? quickFilterOption.value : null,
      hasSearch: Boolean(search),
      search: search ? asText((modelOf(search) || {}).selected) : ""
    });
  };

  // The button names the search applied while the state of the grid is the one it had once applied
  const source = {
    snapshot: readState,
    currentValues: readState,
    parseValues: parseGridSearchValues,
    // The state of the search on the columns the grid has now; the grid reloads from its first page
    apply(values, onApplied) {
      const {quickFilter, search} = controller();
      const restored = getGridSearchRestore(values, controller().columnModel || [],
        {hasQuickFilter: Boolean(quickFilter), hasSearch: Boolean(search)});
      if ("quickFilter" in restored) {
        const option = chosenOption(modelOf(quickFilter), restored.quickFilter);
        Control.changeModelAttribute(addressOf(quickFilter), {selected: option ? asText(option.value) : ""}, true);
      }
      if ("search" in restored) {
        Control.changeModelAttribute(addressOf(search), {selected: restored.search}, true);
      }
      component.restoreColumnFilters(restored.columnFilters, restored.columnFilterLabels, restored.sort);
      onApplied();
    }
  };

  const defaultSearch = controller().defaultSearch || null;
  const saved = initSavedSearches(scope, elem, {
    windowId: gridId,
    title: controller().label,
    view,
    screenName: () => ((Storage.get("screen") || {})[view] || {}).name,
    source,
    defaultSearch
  }, {$compile, Control, Utilities, $actionController, $settings, Connection});

  if (defaultSearch) {
    // Only the columns the grid has now: the quick filter and the search are left as the server set them
    const seeded = getGridSearchRestore(defaultSearch, controller().columnModel || []);
    component.seedColumnFilters(seeded.columnFilters, seeded.columnFilterLabels, seeded.sort);
  }

  /**
   * Leave the default search in effect (#945): no column filters nor sort, as the XML, in one reload
   * @returns {boolean} A default search was in effect
   */
  component.leaveDefaultSearch = () => {
    if (!saved.applied) {
      return false;
    }
    saved.cleared();
    component.restoreColumnFilters([], {}, []);
    return true;
  };

  return saved;
}
