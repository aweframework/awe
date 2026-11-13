import React from 'react';
import {waitFor} from '@testing-library/react';
import {DEFAULT_SETTINGS} from "../../../../src/redux/actions/settings";
import {renderWithProviders} from "../../test-utils";
import AwePivotTable from "../../../../src/components/AwePivotTable";

jest.mock("../../../../src/components/pivot/loadPivot", () => ({loadPivot: jest.fn(() => Promise.reject(new Error("offline")))}));

describe('AwePivotTable when react-pivottable cannot be loaded', () => {
  it('logs the error and keeps the skeleton instead of crashing the screen', async () => {
    const error = jest.spyOn(console, "error").mockImplementation(() => {});

    renderWithProviders(<AwePivotTable id="pivotTable"/>, {preloadedState: {
      settings: DEFAULT_SETTINGS,
      components: {pivotTable: {address: {component: "pivotTable", view: "report"}, model: {values: []}, attributes: {}}}
    }});

    await waitFor(() => expect(error).toHaveBeenCalled());
    expect(document.querySelector(".awe-pivot-table .p-skeleton")).not.toBeNull();
    error.mockRestore();
  });
});
