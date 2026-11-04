import React, {useCallback, useMemo} from "react";
import {useDispatch, useStore} from "react-redux";
import PropTypes from "prop-types";
import {useComponentState} from "../hooks/useComponentState";
import {useColumnFilters} from "../hooks/useColumnFilters";
import {updateModelWithDependencies} from "../redux/thunks/components";
import {getAllComponents} from "../redux/selectors/componentSelectors";
import ViewRegistry from "../redux/registry/ViewRegistry";
import {
  getGridSearchRestore,
  getGridSearchState,
  isGridSearchInEffect,
  parseGridSearchValues
} from "../utilities/gridSavedSearches";
import {SavedSearchesControl} from "./AweWindowSavedSearches";

/**
 * Value of the option a quick filter has chosen: the first option when none is chosen
 * @param {object} component Quick filter
 * @returns {*} Value, null without options
 */
const quickFilterValue = (component) => {
  const values = component?.model?.values || [];
  const current = values.find(option => option.selected) || values[0];
  return current ? current.value : null;
};

/**
 * Text of a grid search: its selected value, the empty text without it
 * @param {object} component Grid search
 * @returns {string} Text
 */
const searchText = (component) => {
  const selected = (component?.model?.values || []).find(option => option.selected);
  return selected?.value === null || selected?.value === undefined ? "" : String(selected.value);
};

/**
 * What a grid holds that a saved search keeps, from the components of the store
 * @param {object} components Components
 * @param {object} grid Grid
 * @returns {object} State of the grid
 */
const readGridState = (components, grid) => {
  const {attributes = {}, specificAttributes = {}} = grid || {};
  const {quickFilter, search} = attributes;
  return getGridSearchState({
    columnFilters: specificAttributes.columnFilters,
    columnFilterLabels: attributes.columnFilterLabels,
    sort: specificAttributes.sort,
    hasQuickFilter: Boolean(quickFilter),
    quickFilter: quickFilterValue(components[quickFilter]),
    hasSearch: Boolean(search),
    search: searchText(components[search])
  });
};

/**
 * Saved searches of a grid (#912, saved-searches="true"), in its toolbar: the same menu and dialogs as the saved searches
 * of a window. A search keeps the column filters of the grid (with the labels of their values), its sort, the value of
 * its quick filter and the text of its search; applying it restores them on the columns the grid has now and reloads
 * the grid from its first page.
 * @category Components
 * @subcategory Grid
 */
function AweGridSavedSearches({id}) {
  const dispatch = useDispatch();
  const store = useStore();
  const grid = useComponentState(id);
  const {address, attributes = {}} = grid || {};
  const quickFilter = useComponentState(attributes.quickFilter);
  const search = useComponentState(attributes.search);
  const {restoreColumnFilters} = useColumnFilters(id);

  const current = useMemo(() => readGridState({[attributes.quickFilter]: quickFilter, [attributes.search]: search}, grid),
    [grid, quickFilter, search, attributes.quickFilter, attributes.search]);

  const getScreen = useCallback(() => {
    const screenId = address && ViewRegistry.get(address.view)?.name;
    return screenId ? {view: address.view, screenId} : null;
  }, [address]);

  const getValues = useCallback(() => {
    const components = getAllComponents(store.getState());
    return readGridState(components, components[id]);
  }, [store, id]);

  const applyValues = useCallback((values) => {
    const components = getAllComponents(store.getState());
    const restored = getGridSearchRestore(values, attributes.columnModel || [],
      {hasQuickFilter: Boolean(components[attributes.quickFilter]), hasSearch: Boolean(components[attributes.search])});
    const quickFilterComponent = components[attributes.quickFilter];
    if ("quickFilter" in restored && quickFilterComponent?.address) {
      const options = quickFilterComponent.model?.values || [];
      // The option of the search, or the first one when the quick filter no longer has it
      const chosen = options.find(option => String(option.value) === String(restored.quickFilter)) || options[0];
      dispatch(updateModelWithDependencies(quickFilterComponent.address,
        {values: options.map(option => ({...option, selected: option === chosen}))}));
    }
    const searchComponent = components[attributes.search];
    if ("search" in restored && searchComponent?.address) {
      dispatch(updateModelWithDependencies(searchComponent.address, {values: [{value: restored.search, selected: true}]}));
    }
    restoreColumnFilters(restored.columnFilters, restored.columnFilterLabels, restored.sort);
  }, [dispatch, store, attributes.columnModel, attributes.quickFilter, attributes.search, restoreColumnFilters]);

  const isInEffect = useCallback((values) => isGridSearchInEffect(current, values), [current]);

  if (!address) {
    return null;
  }
  return <SavedSearchesControl id={id} label={attributes.label} getScreen={getScreen} getValues={getValues}
                               applyValues={applyValues} isInEffect={isInEffect} parseValues={parseGridSearchValues}
                               buttonClassName="p-button-outlined grid-toolbar-button"/>;
}

AweGridSavedSearches.propTypes = {
  id: PropTypes.string
};

export default AweGridSavedSearches;
