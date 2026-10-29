import React from "react";
import {act, fireEvent} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import useSuggest from "../../../src/hooks/useSuggest";

describe("awe-react-client/test/js/hooks/useSuggestTest.jsx", () => {
  const address = {component: "suggest", view: "report"};

  const Probe = ({autocompleteRef}) => {
    const {onKeyPress} = useSuggest(autocompleteRef, jest.fn(), null, jest.fn(), {address});
    return <input data-testid="probe" onKeyDown={onKeyPress} defaultValue=""/>;
  };

  const renderProbe = () => {
    const onDropdownClick = jest.fn();
    const {getByTestId} = renderWithProviders(<Probe autocompleteRef={{current: {onDropdownClick}}}/>);
    return {input: getByTestId("probe"), onDropdownClick};
  };

  it("opens the dropdown with Space when the input is empty", () => {
    const {input, onDropdownClick} = renderProbe();

    fireEvent.keyDown(input, {key: " ", code: "Space"});

    expect(onDropdownClick).toHaveBeenCalledTimes(1);
  });

  it("does not open the dropdown with Space when the input has text", () => {
    const {input, onDropdownClick} = renderProbe();
    fireEvent.change(input, {target: {value: "text"}});

    fireEvent.keyDown(input, {key: " ", code: "Space"});

    expect(onDropdownClick).not.toHaveBeenCalled();
  });

  it("does not open the dropdown with another key", () => {
    const {input, onDropdownClick} = renderProbe();

    fireEvent.keyDown(input, {key: "a", code: "KeyA"});

    expect(onDropdownClick).not.toHaveBeenCalled();
  });

  it("does not fail when the autocomplete is not mounted", () => {
    const {getByTestId} = renderWithProviders(<Probe autocompleteRef={{current: null}}/>);

    expect(() => fireEvent.keyDown(getByTestId("probe"), {key: " ", code: "Space"})).not.toThrow();
  });

  describe("storing the chosen option (#793)", () => {
    const option = {label: "ADM - Administrator", value: "1"};
    const state = {
      settings: DEFAULT_SETTINGS,
      components: {
        suggest: {
          address,
          model: {values: []},
          attributes: {},
          validationRules: {},
          specificAttributes: {}
        }
      }
    };

    let onChange;
    const ChangeProbe = () => {
      ({onChange} = useSuggest({current: null}, jest.fn(), null, jest.fn(), {address}));
      return null;
    };

    const change = (event) => {
      const {store} = renderWithProviders(<ChangeProbe/>, {preloadedState: state});
      act(() => onChange(event));
      return store.getState().components.suggest.model.values;
    };

    it("stores an option chosen with the keyboard (Enter or Tab on the highlighted option)", () => {
      expect(change({originalEvent: {type: "keydown"}, value: option})).toEqual([{...option, selected: true}]);
    });

    it("stores an option chosen with a click, as before", () => {
      expect(change({originalEvent: {type: "click"}, value: option})).toEqual([{...option, selected: true}]);
    });

    it("stores the options of a multiple suggest chosen with the keyboard", () => {
      expect(change({originalEvent: {type: "keydown"}, value: [option]})).toEqual([{...option, selected: true}]);
    });

    it("does not store the text being typed", () => {
      expect(change({originalEvent: {type: "change"}, value: "adm"})).toEqual([]);
    });
  });
});
