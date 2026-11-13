import React from "react";
import {render} from "@testing-library/react";
import {configureStore, createSelector} from "@reduxjs/toolkit";
import {Provider, useSelector} from "react-redux";

// react-redux and reselect check in development that a selector returns the same result for the same state: the Jest
// setup turns their warnings into errors, so a selector that renders again on every dispatch fails the suite
describe("awe-react-client/test/js/performance/SelectorStabilityCheckTest.jsx", () => {
  const store = configureStore({reducer: (state = {value: 1}) => state});

  it("fails when a useSelector returns a different result for the same state", () => {
    const Unstable = () => {
      useSelector(state => ({value: state.value}));
      return null;
    };
    jest.spyOn(console, "error").mockImplementation(() => null);

    expect(() => render(<Provider store={store}><Unstable/></Provider>))
      .toThrow(/returned a different result when called with the same parameters/);

    console.error.mockRestore();
  });

  it("fails when an input selector of createSelector returns a different result for the same arguments", () => {
    const selectUnstable = createSelector([state => ({value: state.value})], input => input.value);

    expect(() => selectUnstable({value: 1})).toThrow(/input selector returned a different result/);
  });

  it("does not fail on a stable selector", () => {
    const Stable = () => {
      useSelector(state => state.value);
      return null;
    };

    expect(() => render(<Provider store={store}><Stable/></Provider>)).not.toThrow();
  });
});
