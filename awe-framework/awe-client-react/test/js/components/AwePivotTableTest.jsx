import React from 'react';
import {findByText, fireEvent, getByText, screen, waitFor} from '@testing-library/react';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AwePivotTable from "../../../src/components/AwePivotTable";

describe('awe-react-client/test/js/components/AwePivotTableTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      pivotTable: {
        address: {component: 'pivotTable', view: 'report'},
        model: {values: [{Als: 12}]},
        attributes: {
          component: "grid",
          style: "estilo-especifico",
          cols: "Als"
        }
      },
    }
  };

  it('renders Awe Pivot Table component', () => {
    renderWithProviders(<AwePivotTable id="pivotTable" style="estilo-especifico"/>, {preloadedState});

    expect(document.querySelector("div.estilo-especifico")).not.toBeNull();
    expect(document.querySelector("table.pvtUi")).not.toBeNull();
  });

  it('renders Awe Pivot Table component with cols', async () => {
    renderWithProviders(<AwePivotTable id="pivotTable" style="estilo-especifico"/>, {preloadedState});

    expect(document.querySelector("div.estilo-especifico")).not.toBeNull();
    expect(document.querySelector("table.pvtUi")).not.toBeNull();
    await waitFor(() => findByText(document.querySelector("td.pvtCols"), "Als"));
    expect(await findByText(document.querySelector("td.pvtCols"), "Als")).toBeDefined();
  });

  it('renders Awe Pivot Table component with cols and change dropdown', async () => {
    renderWithProviders(<AwePivotTable id="pivotTable" style="estilo-especifico"/>, {preloadedState});

    expect(document.querySelector("div.estilo-especifico")).not.toBeNull();
    expect(document.querySelector("table.pvtUi")).not.toBeNull();
    expect(await findByText(document.querySelector("td.pvtCols"), "Als")).toBeDefined();
    await waitFor(() => screen.findByText("Count", {exact: false}));

    // Click on count button
    fireEvent.click(screen.getByText("Count", {exact: false}));
    await waitFor(() =>  screen.findByText("Sum over Sum", {exact: false}));

    // Click on Sum over Sum button
    fireEvent.click(screen.getByText("Sum over Sum", {exact: false}));
    await waitFor(() => findByText(document.querySelector(".pvtDropdownActiveValue"), "Sum over Sum", {exact: false}));

    // Click on Sum over Sum button
    fireEvent.click(getByText(document.querySelector(".pvtDropdownActiveValue"), "Sum over Sum", {exact: false}));

    await waitFor(() => screen.findByText("Sum over Sum", {exact: false}));
    expect(await screen.findByText("Sum over Sum", {exact: false})).toBeDefined();
  });
});
