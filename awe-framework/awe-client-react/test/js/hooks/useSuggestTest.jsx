import React from "react";
import {fireEvent} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
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
});
