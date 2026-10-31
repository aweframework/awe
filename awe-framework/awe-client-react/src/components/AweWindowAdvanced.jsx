import React, {createContext, useCallback, useContext, useEffect, useLayoutEffect, useMemo, useRef, useState} from "react";
import {useTranslation} from "react-i18next";
import {useDispatch, useStore} from "react-redux";
import {Sidebar} from "primereact/sidebar";
import PropTypes from "prop-types";
import {getIconNode, translateLabel} from "../utilities";
import {getFilterChip, isFilterInEffect} from "../utilities/advancedSearch";
import {TestAttributes, TestIds, testHook} from "../utilities/testIds";
import {useMultipleComponents} from "../hooks/useComponentState";
import {useFocusTrap} from "../hooks/useFocusTrap";
import {
  restoreMultipleModelWithDependencies,
  updateMultipleModelsWithDependencies
} from "../redux/thunks/components";
import {addActionsTop} from "../redux/actions/actions";
import {saveWindowAdvancedState} from "../redux/thunks/windowAdvancedState";
import {getAllComponents} from "../redux/selectors/componentSelectors";
import ViewRegistry from "../redux/registry/ViewRegistry";
import "./AweWindowAdvanced.less";

export const ADVANCED_MODE_INLINE = "inline";
export const ADVANCED_MODE_SIDE_PANEL = "side-panel";
// Width of the filter panel of the design, relative to the text size and not to the root font size, which differs
// between the engines (it takes the whole width of a small screen)
const PANEL_MAX_WIDTH = "39em";

/**
 * Advanced search of a window (#790): a window that holds an advanced block shares with it its mode, its criteria lists
 * and the open state. The block draws the disclosure row where it is and keeps its children mounted: hidden below the
 * row while an inline window is collapsed, or moved into the side panel of the window, so dependencies, validation,
 * defaults, printing and export do not change. Windows without advanced block provide nothing.
 */
export const AdvancedSearchContext = createContext(null);

/**
 * Advanced search of the enclosing window
 * @return {object|null} Window id and label, mode, advanced and search criteria, open state and its setter; null outside
 * a window with an advanced block
 */
export function useAdvancedWindow() {
  return useContext(AdvancedSearchContext);
}

const noFilters = [];

/**
 * State of the advanced search of a window
 * @param {object} options Window attributes
 * @param {string} options.id Window id
 * @param {string} options.label Window label
 * @param {string} options.advancedMode Mode: inline or side-panel (only sent when the window holds an advanced block)
 * @param {string[]} options.advancedCriteria Advanced criteria of the window
 * @param {string[]} options.searchCriteria Criteria of the window that are search filters
 * @param {boolean} options.advancedOpen Open state the user left (only sent when it is stored): an inline block starts
 * open with it, a side panel always starts closed
 * @return {object|null} Context of the advanced search, null when the window has no advanced block
 */
export function useAdvancedSearch({id, label, advancedMode, advancedCriteria, searchCriteria, advancedOpen}) {
  const enabled = Boolean(advancedMode) || Array.isArray(advancedCriteria);
  const mode = advancedMode === ADVANCED_MODE_SIDE_PANEL ? ADVANCED_MODE_SIDE_PANEL : ADVANCED_MODE_INLINE;
  const [open, setOpen] = useState(() => enabled && mode === ADVANCED_MODE_INLINE && advancedOpen === true);
  return useMemo(() => enabled ? {
    id, label, mode, open, setOpen,
    advancedCriteria: advancedCriteria || noFilters,
    searchCriteria: searchCriteria || noFilters
  } : null, [enabled, id, label, mode, open, advancedCriteria, searchCriteria]);
}

/**
 * Advanced criteria in use: a criterion counts when its value differs from its default
 * @param {string[]} ids Criteria ids
 * @return {object[]} Criteria of the state in use
 */
function useFiltersInEffect(ids) {
  const components = useMultipleComponents(ids);
  return useMemo(() => ids.map(id => components[id]).filter(isFilterInEffect), [ids, components]);
}

/**
 * Restore criteria to their defaults (as the restore action does), with their dependencies
 * @return {function(string[])} Restore the criteria with these ids
 */
function useRestoreDefaults() {
  const dispatch = useDispatch();
  const store = useStore();
  return useCallback((ids) => {
    const components = getAllComponents(store.getState());
    const list = ids.map(id => components[id]).filter(component => component?.address);
    if (list.length) {
      dispatch(restoreMultipleModelWithDependencies(list.map(({address}) => ({address})), false));
    }
  }, [dispatch, store]);
}

/**
 * Save the open state of an inline advanced block as the state of the user. The screen is the one of the view that holds
 * the criteria of the window (its name is the screen id the server sent). Nothing is saved when it is unknown.
 * @param {string} windowId Window id
 * @param {string[]} criteria Criteria of the window
 * @return {function(boolean)} Save the open state
 */
export function useSaveAdvancedState(windowId, criteria) {
  const {t} = useTranslation();
  const dispatch = useDispatch();
  const store = useStore();
  return useCallback((open) => {
    const components = getAllComponents(store.getState());
    const view = criteria.map(criterion => components[criterion]?.address?.view).find(Boolean);
    const screenId = view && ViewRegistry.get(view)?.name;
    if (screenId) {
      dispatch(saveWindowAdvancedState({view, component: windowId}, screenId, open, {
        title: t("SCREEN_TEXT_ADVANCED_STATE_ERROR_TITLE"),
        message: t("SCREEN_TEXT_ADVANCED_STATE_ERROR_MESSAGE")
      }));
    }
  }, [dispatch, store, windowId, criteria, t]);
}

/**
 * Id of the region below the disclosure row that holds the advanced criteria of an inline window
 * @param {string} id Window id
 * @return {string} Region id
 */
export const getAdvancedRegionId = (id) => `${id}-advanced-region`;

/**
 * Id of the side panel of a window
 * @param {string} id Window id
 * @return {string} Panel id
 */
export const getAdvancedPanelId = (id) => `${id}-advanced-panel`;

/**
 * Button that shows the advanced criteria below it (inline, with a chevron) or opens the side panel, with the count of
 * advanced criteria in use
 */
export const AdvancedToggle = React.forwardRef(function AdvancedToggle({id, mode, open, advancedCriteria, onToggle}, ref) {
  const {t} = useTranslation();
  const count = useFiltersInEffect(advancedCriteria).length;
  const inline = mode === ADVANCED_MODE_INLINE;
  // The side panel opens with the same Advanced filters button as the inline mode
  const text = t(inline && open ? "SCREEN_TEXT_HIDE_ADVANCED_FILTERS" : "SCREEN_TEXT_ADVANCED_FILTERS");
  return <button ref={ref} type="button" onClick={onToggle}
                 className="window-advanced-toggle p-button p-component p-button-text p-button-sm"
                 aria-expanded={open}
                 aria-controls={inline ? getAdvancedRegionId(id) : getAdvancedPanelId(id)}
                 aria-haspopup={inline ? undefined : "dialog"}
                 {...testHook(TestIds.windowAdvancedToggle, {owner: id})}>
    {getIconNode("lucide:sliders-horizontal", {className: "window-advanced-toggle-icon", "aria-hidden": "true"})}
    <span>{text}</span>
    {count > 0 && <>
      <span className="window-advanced-count" aria-hidden="true" {...testHook(TestIds.windowAdvancedCount, {owner: id})}>{count}</span>
      <span className="p-hidden-accessible">({count})</span>
    </>}
    {inline ?
      getIconNode("lucide:chevron-down", {className: "window-advanced-chevron", "aria-hidden": "true"}) :
      getIconNode("lucide:panel-right", {className: "window-advanced-toggle-icon", "aria-hidden": "true"})}
  </button>;
});

AdvancedToggle.propTypes = {
  id: PropTypes.string,
  mode: PropTypes.string,
  open: PropTypes.bool,
  advancedCriteria: PropTypes.array,
  onToggle: PropTypes.func
};

/**
 * Filters in effect of a window, in the disclosure row after the toggle and a divider: a removable chip per criterion
 * whose value differs from its default (a hidden advanced one is marked), and Clear all. It is not shown while no filter
 * is in effect.
 */
export function FilterChips({id, searchCriteria, advancedCriteria, advancedHidden}) {
  const {t} = useTranslation();
  const filters = useFiltersInEffect(searchCriteria);
  const restore = useRestoreDefaults();
  if (filters.length === 0) {
    return null;
  }
  const title = t("SCREEN_TEXT_FILTERED_BY");
  return <div className="window-filter-chips" role="group" aria-label={title}
              {...testHook(TestIds.windowFilterChips, {owner: id})}>
    <span className="window-filter-chips-title">{title}</span>
    {filters.map(component => {
      const criterion = component.address.component;
      const hidden = advancedHidden && advancedCriteria.includes(criterion);
      const {label, value, more} = getFilterChip(component, t);
      return <span key={criterion} className={`window-filter-chip${hidden ? " window-filter-chip-hidden" : ""}`}
                   {...testHook(TestIds.windowFilterChip, {owner: id, attributes: {[TestAttributes.filterId]: criterion}})}>
        {hidden && getIconNode("lucide:eye-off", {className: "window-filter-chip-marker", "aria-hidden": "true"})}
        <span className="window-filter-chip-text">{label}{value !== null && <>: <b>{value}</b>{more}</>}</span>
        {hidden && <span className="p-hidden-accessible">({t("SCREEN_TEXT_HIDDEN_ADVANCED_FILTER")})</span>}
        <button type="button" className="window-filter-chip-remove p-link"
                aria-label={`${t("SCREEN_TEXT_REMOVE_FILTER")} ${label}`}
                onClick={() => restore([criterion])}
                {...testHook(TestIds.windowFilterChipRemove, {owner: id})}>
          {getIconNode("lucide:x", {"aria-hidden": "true"})}
        </button>
      </span>;
    })}
    <button type="button" className="window-filter-clear-all p-button p-component p-button-text p-button-sm"
            onClick={() => restore(searchCriteria)} {...testHook(TestIds.windowFilterClearAll, {owner: id})}>
      {t("SCREEN_TEXT_CLEAR_ALL_FILTERS")}
    </button>
  </div>;
}

FilterChips.propTypes = {
  id: PropTypes.string,
  searchCriteria: PropTypes.array,
  advancedCriteria: PropTypes.array,
  advancedHidden: PropTypes.bool
};

/**
 * Reveal the advanced criteria when the validation fails in one of them while they are hidden
 */
export function AdvancedReveal({advancedCriteria, hidden, onReveal}) {
  const components = useMultipleComponents(advancedCriteria);
  const invalid = advancedCriteria.some(criterion => components[criterion]?.attributes?.error);
  useEffect(() => {
    if (invalid && hidden) {
      onReveal();
    }
  }, [invalid]); // eslint-disable-line react-hooks/exhaustive-deps
  return null;
}

AdvancedReveal.propTypes = {
  advancedCriteria: PropTypes.array,
  hidden: PropTypes.bool,
  onReveal: PropTypes.func
};

/**
 * Hidden holder of the children of the advanced block of a side-panel window while the panel is closed
 */
export function AdvancedHolder({container, holderRef}) {
  useLayoutEffect(() => {
    if (!container.parentNode) {
      holderRef.current.appendChild(container);
    }
  }, [container, holderRef]);
  return <div ref={holderRef} className="window-advanced-holder" hidden/>;
}

AdvancedHolder.propTypes = {
  container: PropTypes.object,
  holderRef: PropTypes.object
};

/**
 * Body of the side panel: it takes the container of the children of the advanced block while it is shown, and gives it
 * back to the holder of the block when it closes
 */
function PanelCriteria({container, holderRef}) {
  const host = useRef(null);
  useLayoutEffect(() => {
    // The holder is rendered by the block before the panel opens
    const holder = holderRef.current;
    host.current.appendChild(container);
    return () => {
      holder?.appendChild(container);
    };
  }, [container, holderRef]);
  return <div ref={host} className="window-advanced-panel-criteria"/>;
}

PanelCriteria.propTypes = {
  container: PropTypes.object,
  holderRef: PropTypes.object
};

/**
 * Side panel of a window with its advanced block: Reset to defaults, Cancel (back to the values they had when it
 * opened; Esc and the close button cancel too) and Show results (it closes and submits the window as Enter does)
 */
export function AdvancedPanel({id, label, open, advancedCriteria, searchCriteria, container, holderRef, onClose}) {
  const {t} = useTranslation();
  const dispatch = useDispatch();
  const store = useStore();
  const restore = useRestoreDefaults();
  const sidebar = useRef(null);
  const snapshot = useRef([]);

  // The values of the advanced criteria when the panel opens, for Cancel
  useEffect(() => {
    if (open) {
      const components = getAllComponents(store.getState());
      snapshot.current = advancedCriteria.map(criterion => components[criterion])
        .filter(component => component?.address)
        .map(({address, model}) => ({address, values: model?.values || []}));
    }
  }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

  useFocusTrap(() => sidebar.current?.getElement(), open);

  const cancel = useCallback(() => {
    const components = getAllComponents(store.getState());
    const changed = snapshot.current
      .filter(({address, values}) => components[address.component]?.model?.values !== values)
      .map(({address, values}) => ({address, data: {values}}));
    if (changed.length) {
      dispatch(updateMultipleModelsWithDependencies(changed));
    }
    onClose();
  }, [dispatch, store, onClose]);

  const apply = useCallback(() => {
    onClose();
    // As Enter in a criterion: submit from the first advanced criterion (or the first one of the window) with the window
    // as target, so the submit button of the window runs when the screen has more
    const first = getAllComponents(store.getState())[advancedCriteria[0] ?? searchCriteria[0]];
    if (first?.address) {
      dispatch(addActionsTop([{type: "submit", target: id, address: first.address}]));
    }
  }, [dispatch, store, onClose, advancedCriteria, searchCriteria, id]);

  return <Sidebar
    ref={sidebar}
    id={getAdvancedPanelId(id)}
    className="side-panel window-advanced-panel"
    style={{width: "100%", maxWidth: PANEL_MAX_WIDTH}}
    position="right"
    header={<span className="side-panel-title">{translateLabel(label, t)}</span>}
    visible={open}
    onHide={cancel}
    pt={{
      root: {...testHook(TestIds.windowAdvancedPanel, {owner: id, open}), role: "dialog", "aria-modal": "true",
        "aria-label": translateLabel(label, t)},
      closeButton: {...testHook(TestIds.sidePanelClose), "aria-label": t("BUTTON_CANCEL")}
    }}>
    <PanelCriteria container={container} holderRef={holderRef}/>
    <div className="modal-footer">
      <button type="button" className="window-advanced-reset p-button p-component p-button-text p-button-sm"
              onClick={() => restore(advancedCriteria)} {...testHook(TestIds.windowAdvancedReset, {owner: id})}>
        {getIconNode("lucide:rotate-ccw", {tag: "span", className: "window-advanced-reset-icon", "aria-hidden": "true"})}
        <span className="p-button-label">{t("SCREEN_TEXT_RESET_DEFAULTS")}</span>
      </button>
      <span className="window-advanced-footer-spacer"/>
      <button type="button" className="p-button p-component p-button-outlined p-button-sm"
              onClick={cancel} {...testHook(TestIds.windowAdvancedCancel, {owner: id})}>
        {t("BUTTON_CANCEL")}
      </button>
      <button type="button" className="p-button p-component p-button-sm"
              onClick={apply} {...testHook(TestIds.windowAdvancedApply, {owner: id})}>
        {t("SCREEN_TEXT_SHOW_RESULTS")}
      </button>
    </div>
  </Sidebar>;
}

AdvancedPanel.propTypes = {
  id: PropTypes.string,
  label: PropTypes.string,
  open: PropTypes.bool,
  advancedCriteria: PropTypes.array,
  searchCriteria: PropTypes.array,
  container: PropTypes.object,
  holderRef: PropTypes.object,
  onClose: PropTypes.func
};
