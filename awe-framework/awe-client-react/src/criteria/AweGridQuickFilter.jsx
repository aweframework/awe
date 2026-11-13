import React, { useCallback, useMemo } from "react";
import PropTypes from "prop-types";
import { useDispatch } from "react-redux";
import { useTranslation } from "react-i18next";
import { useComponentState } from "../hooks/useComponentState";
import { onChoiceKeyDown } from "../hooks/useRadioColumn";
import { updateModelWithDependencies } from "../redux/thunks/components";
import { addActionsTop } from "../redux/actions/actions";
import { translateLabel } from "../utilities";
import { TestIds, testHook } from "../utilities/testIds";

/**
 * Value of an option as text: an empty value (null or undefined) is the empty text
 * @param {*} value Value
 * @returns {string} Value as text
 */
const asText = (value) => (value === null || value === undefined ? "" : String(value));

/**
 * Check if an option shows its count
 * @param {object} option Option
 * @returns {boolean} The option has a count
 */
const hasCount = (option) => option.count !== null && option.count !== undefined && option.count !== "";

/**
 * Quick filters of a grid: a segmented control (one radio per option, "Label N") at the start of the grid toolbar. It is
 * a criterion of the screen whose options come from its initial load (value, label, count and tone): its value is sent
 * with the requests of the grid, choosing an option filters the grid, and every filter of the grid reloads the options
 * (see the filter action), so the counts stay current.
 * @category Components
 * @subcategory Grid
 */
function AweGridQuickFilter({ id, grid }) {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { address, model = {}, attributes = {} } = useComponentState(id);
  const values = useMemo(() => model.values || [], [model.values]);
  // The chosen option, or the first one when none is chosen
  const current = values.find((option) => option.selected) || values[0];

  const choose = useCallback((option) => {
    if (attributes.readonly || (option === current && option.selected)) {
      return;
    }
    dispatch(updateModelWithDependencies(address, { values: values.map((item) => ({ ...item, selected: item === option })) }));
    dispatch(addActionsTop([{ type: "filter", address: { view: address.view, component: grid } }]));
  }, [attributes.readonly, current, values, address, grid, dispatch]);

  if (!address || attributes.visible === false) {
    return null;
  }

  const name = `${address.view}-${address.component}`;
  return <div className="grid-quick-filter" role="radiogroup" aria-label={translateLabel(attributes.label || "SCREEN_TEXT_FILTERS", t)}
    aria-busy={attributes.loading ? "true" : undefined} criterion-id={address.component}
    {...testHook(TestIds.gridQuickFilter, { owner: grid })}>
    {values.map((option, index) => <label key={`${asText(option.value)}-${index}`} className="grid-quick-filter-option"
      option-id={asText(option.value)} {...testHook(TestIds.gridQuickFilterOption)}>
      <input type="radio" name={name} value={asText(option.value)} checked={option === current} disabled={!!attributes.readonly}
        onChange={() => choose(option)} onKeyDown={onChoiceKeyDown}
        {...testHook(TestIds.criterionInput)} />
      <span className="grid-quick-filter-segment">
        <span className="grid-quick-filter-label">{translateLabel(option.label, t)}</span>
        {hasCount(option) && <> <span className="grid-quick-filter-count" data-tone={option.tone || undefined}>{option.count}</span></>}
      </span>
    </label>)}
  </div>;
}

AweGridQuickFilter.propTypes = {
  id: PropTypes.string,
  grid: PropTypes.string
};

export default AweGridQuickFilter;
