import React, { useCallback, useEffect, useRef, useState } from "react";
import PropTypes from "prop-types";
import { useDispatch } from "react-redux";
import { useTranslation } from "react-i18next";
import { useComponentState } from "../hooks/useComponentState";
import { updateModelWithDependencies } from "../redux/thunks/components";
import { addActionsTop } from "../redux/actions/actions";
import { getIconNode, translateLabel } from "../utilities";
import { TestIds, testHook } from "../utilities/testIds";

// Pause after the last key before the grid is filtered
export const GRID_SEARCH_DELAY = 300;

/**
 * Text of the model of the search: its selected value, the empty text without it
 * @param {object} model Model
 * @returns {string} Text
 */
const modelText = (model) => {
  const selected = (model.values || []).find((option) => option.selected);
  return selected?.value === null || selected?.value === undefined ? "" : String(selected.value);
};

/**
 * Search of a grid: a text field with the search icon (and a clear button when it has text) at the start of the grid
 * toolbar. It is a text criterion of the screen with no query of its own: its value is sent with the requests of the
 * grid; typing filters the grid after a pause, Enter at once, and the clear button or Escape empty it.
 * @category Components
 * @subcategory Grid
 */
function AweGridSearch({ id, grid }) {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { address, model = {}, attributes = {} } = useComponentState(id);
  const stored = modelText(model);
  const [text, setText] = useState(stored);
  const input = useRef(null);
  const pending = useRef(null);
  // Text the grid was last filtered with: the grid is not filtered again for the same text
  const filtered = useRef(stored);
  const written = useRef(stored);

  // A value set from outside (a reset, a restore) is shown and taken as the filtered one
  useEffect(() => {
    if (stored !== written.current) {
      written.current = stored;
      filtered.current = stored;
      setText(stored);
    }
  }, [stored]);

  const cancelPending = useCallback(() => {
    clearTimeout(pending.current);
    pending.current = null;
  }, []);

  useEffect(() => cancelPending, [cancelPending]);

  // Store the text as the value of the criterion
  const store = useCallback((value) => {
    written.current = value;
    dispatch(updateModelWithDependencies(address, { values: [{ value, selected: true }] }));
  }, [dispatch, address]);

  // Filter the grid with a text, unless it is already filtered with it
  const filter = useCallback((value) => {
    cancelPending();
    if (value === filtered.current) {
      return;
    }
    filtered.current = value;
    dispatch(addActionsTop([{ type: "filter", address: { view: address.view, component: grid } }]));
  }, [cancelPending, dispatch, address, grid]);

  const onChange = useCallback((event) => {
    const value = event.target.value;
    setText(value);
    store(value);
    cancelPending();
    pending.current = setTimeout(() => filter(value), GRID_SEARCH_DELAY);
  }, [store, cancelPending, filter]);

  const clear = useCallback(() => {
    setText("");
    store("");
    filter("");
    input.current?.focus();
  }, [store, filter]);

  const onKeyDown = useCallback((event) => {
    if (event.key === "Enter") {
      // Filters the grid and does not submit the screen
      event.preventDefault();
      event.stopPropagation();
      filter(text);
    } else if (event.key === "Escape" && text !== "") {
      event.preventDefault();
      event.stopPropagation();
      clear();
    }
  }, [filter, clear, text]);

  if (!address || attributes.visible === false) {
    return null;
  }

  const clearLabel = t("BUTTON_CLEAR");
  return <div className="grid-search" role="search" criterion-id={address.component}
    {...testHook(TestIds.gridSearch, { owner: grid })}>
    <span className="grid-search-icon">{getIconNode("search", { "aria-hidden": "true" })}</span>
    <input ref={input} type="search" id={address.component} name={address.component} value={text} autoComplete="off"
      placeholder={attributes.placeholder ? translateLabel(attributes.placeholder, t) : undefined}
      aria-label={translateLabel(attributes.label || "BUTTON_SEARCH", t)} disabled={!!attributes.readonly}
      onChange={onChange} onKeyDown={onKeyDown} {...testHook(TestIds.criterionInput)} />
    {text !== "" && !attributes.readonly && <button type="button" className="grid-search-clear" aria-label={clearLabel}
      title={clearLabel} onClick={clear} {...testHook(TestIds.gridSearchClear)}>
      {getIconNode("times", { "aria-hidden": "true" })}
    </button>}
  </div>;
}

AweGridSearch.propTypes = {
  id: PropTypes.string,
  grid: PropTypes.string
};

export default AweGridSearch;
