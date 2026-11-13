import React from 'react';
import {fireEvent, screen, within} from '@testing-library/react';
import i18n from 'i18next';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {ADD_ACTIONS_TOP} from "../../../src/redux/actions/actions";
import {renderWithProviders} from "../test-utils";
import AweGridColumnFilterChips from "../../../src/components/AweGridColumnFilterChips";

// Column filters in effect of a grid (#912, S16): a chip per filtered column and Clear all
describe('awe-react-client/test/js/components/AweGridColumnFilterChipsTest.jsx', () => {

  const PROFILE = {id: "ProTxt", fields: ["Pro"], type: "values", values: ["OPE", "VIE"]};
  const LAST_ACCESS = {id: "LstAcc", type: "date", conditions: [{operator: "between", value: "01/10/2026", to: "31/10/2026"}]};

  beforeAll(() => {
    i18n.addResourceBundle('en-GB', 'translation', {
      SCREEN_TEXT_FILTERED_BY: "Filtered by",
      SCREEN_TEXT_CLEAR_ALL_FILTERS: "Clear all",
      SCREEN_TEXT_REMOVE_FILTER: "Remove filter"
    }, true, true);
  });

  const render = (columnFilters, attributes = {}) => renderWithProviders(<AweGridColumnFilterChips id="grid"/>, {
    spyDispatch: true,
    preloadedState: {
      settings: DEFAULT_SETTINGS,
      components: {
        grid: {
          address: {component: 'grid', view: 'report'},
          model: {values: []},
          attributes: {
            columnModel: [{name: "ProTxt", label: "Profile"}, {name: "LstAcc", label: "Last access", hidden: true}],
            columnFilterLabels: {ProTxt: {OPE: "Operator", VIE: "Viewer"}},
            ...attributes
          },
          specificAttributes: {columnFilters}
        }
      }
    }
  });

  // Client actions queued on top of the action stack
  const queued = (dispatchSpy) => dispatchSpy.mock.calls
    .map(([action]) => action)
    .filter(action => action.type === ADD_ACTIONS_TOP)
    .flatMap(action => action.payload);

  it('shows a chip per filtered column, a hidden one included, with the summary of its filter', () => {
    render([PROFILE, LAST_ACCESS]);

    const chips = screen.getByRole("group", {name: "Filtered by"});
    expect(within(chips).getAllByTestId("grid-column-filter-chip").map(chip => chip.textContent)).toEqual([
      "Profile: Operator, Viewer", "Last access: 01/10/2026 – 31/10/2026"]);
  });

  it('is not shown while no column is filtered', () => {
    render([]);

    expect(screen.queryByRole("group", {name: "Filtered by"})).toBeNull();
  });

  it('removes the filter of a chip and reloads the grid', () => {
    const {dispatchSpy} = render([PROFILE, LAST_ACCESS]);

    fireEvent.click(screen.getByRole("button", {name: "Remove filter Profile: Operator, Viewer"}));

    expect(queued(dispatchSpy).map(({type, parameters}) => [type, parameters])).toEqual([
      ["change-column-filters", {columnFilters: [LAST_ACCESS], labels: {}}],
      ["filter", undefined]
    ]);
  });

  it('removes every column filter with Clear all and reloads the grid', () => {
    const {dispatchSpy} = render([PROFILE, LAST_ACCESS]);

    fireEvent.click(screen.getByRole("button", {name: "Clear all"}));

    expect(queued(dispatchSpy).map(({type, parameters}) => [type, parameters])).toEqual([
      ["change-column-filters", {columnFilters: [], labels: {}}],
      ["filter", undefined]
    ]);
  });
});
