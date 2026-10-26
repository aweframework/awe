import React from 'react';
import {act, findByText, fireEvent, getByText, screen, waitFor} from '@testing-library/react';
import {updateModel} from "../../../src/redux/actions/components";
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
    await waitFor(() =>  screen.findByText("Sum over Sum"));

    // Click on Sum over Sum button
    fireEvent.click(screen.getByText("Sum over Sum"));
    await waitFor(() => findByText(document.querySelector(".pvtDropdownActiveValue"), "Sum over Sum", {exact: false}));

    // Click on Sum over Sum button
    fireEvent.click(getByText(document.querySelector(".pvtDropdownActiveValue"), "Sum over Sum", {exact: false}));

    await waitFor(() => screen.findByText("Sum over Sum"));
    expect(await screen.findByText("Sum over Sum")).toBeDefined();
  });

  const withAttributes = (attributes, values = [{Als: 12, Qty: 1234.5}]) => ({
    settings: DEFAULT_SETTINGS,
    components: {
      pivotTable: {
        address: {component: 'pivotTable', view: 'report'},
        model: {values},
        attributes: {component: "pivotTable", cols: "Als", ...attributes}
      }
    }
  });

  it('applies renderer and aggregator attributes as initial values', async () => {
    renderWithProviders(<AwePivotTable id="pivotTable"/>,
      {preloadedState: withAttributes({renderer: "Heatmap", aggregator: "Sum", aggregationField: "Qty"})});

    await waitFor(() => expect(document.querySelectorAll(".pvtDropdownCurrent")[0].textContent).toContain("Table Heatmap"));
    expect(document.querySelectorAll(".pvtDropdownCurrent")[1].textContent).toMatch(/Sum$/);
  });

  it('draws the bars of the Table Barchart renderer', async () => {
    const state = withAttributes({renderer: "Table Barchart", rows: "Als", cols: "Qty", aggregator: "Sum", aggregationField: "Als"});
    state.components.pivotTable.model = {values: [{Als: 8, Qty: "a"}, {Als: 4, Qty: "b"}]};
    renderWithProviders(<AwePivotTable id="pivotTable"/>, {preloadedState: state});

    await waitFor(() => expect(document.querySelectorAll(".pvtVal .pvtBar").length).toBe(2));
    // Every row is scaled on its own, the totals row against its own maximum
    expect([...document.querySelectorAll(".pvtVal .pvtBar")].map(bar => bar.style.height))
      .toEqual(["71.42857142857143%", "71.42857142857143%"]);
    expect([...document.querySelectorAll("td.pvtTotal .pvtBar")].map(bar => parseFloat(bar.style.height).toFixed(1)))
      .toEqual(["71.4", "35.7"]);
  });

  it('keeps the aggregator chosen by the user over the XML one', async () => {
    renderWithProviders(<AwePivotTable id="pivotTable"/>, {preloadedState: withAttributes({aggregator: "Sum", aggregationField: "Qty"})});

    await waitFor(() => expect(document.querySelectorAll(".pvtDropdownCurrent")[1].textContent).toMatch(/Sum$/));
    fireEvent.click(document.querySelectorAll(".pvtDropdownCurrent")[1]);
    fireEvent.click(await screen.findByText("Maximum"));
    await waitFor(() => expect(document.querySelectorAll(".pvtDropdownCurrent")[1].textContent).toMatch(/Maximum$/));
  });

  it('shows the reloaded data after a user change, without keeping a copy of the old data', async () => {
    const {store} = renderWithProviders(<AwePivotTable id="pivotTable"/>, {preloadedState: withAttributes({aggregator: "Sum", aggregationField: "Qty"})});

    await waitFor(() => expect(document.querySelectorAll(".pvtDropdownCurrent")[1].textContent).toMatch(/Sum$/));
    fireEvent.click(document.querySelectorAll(".pvtDropdownCurrent")[1]);
    fireEvent.click(await screen.findByText("Maximum"));
    await waitFor(() => expect(document.querySelectorAll(".pvtDropdownCurrent")[1].textContent).toMatch(/Maximum$/));
    expect(store.getState().components.pivotTable.attributes).not.toHaveProperty("data");

    act(() => store.dispatch(updateModel({component: 'pivotTable', view: 'report'}, {values: [{Als: 77, Qty: 5}]})));
    expect(await findByText(document.querySelector("table.pvtTable"), "77")).toBeDefined();
  });

  it('sorts with set-pivot-sorters orders even when sort-method is absolute', async () => {
    renderWithProviders(<AwePivotTable id="pivotTable"/>, {preloadedState: withAttributes(
      {cols: "", rows: "Als", sortMethod: "absolute", sorters: {Als: ["b", "a"]}}, [{Als: "a"}, {Als: "b"}])});

    await waitFor(() => expect([...document.querySelectorAll("th.pvtRowLabel")].map(th => th.textContent)).toEqual(["b", "a"]));
  });

  it('uses the values field chosen by the user over the aggregation-field', async () => {
    renderWithProviders(<AwePivotTable id="pivotTable"/>, {preloadedState: withAttributes(
      {aggregator: "Sum", aggregationField: "Als", vals: "Qty"})});

    await waitFor(() => expect(screen.getAllByText("1,234.50").length).toBeGreaterThan(0));
  });

  it('renders a formatted Custom Sum', async () => {
    renderWithProviders(<AwePivotTable id="pivotTable"/>, {preloadedState: withAttributes({
      aggregator: "Custom Sum", aggregationField: "Qty", decimalNumbers: 2, thousandSeparator: ".", decimalSeparator: ","})});

    await waitFor(() => expect(screen.getAllByText("1.234,50").length).toBeGreaterThan(0));
  });

  describe('context menu', () => {
    const menuState = (contextMenu = [{id: "pivotOption1"}, {id: "pivotSeparator"}, {id: "pivotOption2"}]) => ({
      settings: DEFAULT_SETTINGS,
      components: {
        pivotTable: {
          address: {component: 'pivotTable', view: 'report'},
          model: {values: [{Als: 12, Qty: 1234.5}]},
          attributes: {component: "pivotTable", cols: "Als", contextMenu}
        },
        pivotOption1: {
          address: {component: 'pivotOption1', view: 'report'},
          attributes: {label: "pivot option 1", visible: true, actions: [{type: "screen", target: "Other"}]}
        },
        pivotSeparator: {
          address: {component: 'pivotSeparator', view: 'report'},
          attributes: {separator: true, visible: true}
        },
        pivotOption2: {
          address: {component: 'pivotOption2', view: 'report'},
          attributes: {label: "pivot option 2", visible: false}
        }
      }
    });

    it('opens the configured menu with a right click on the pivot table', async () => {
      const {container} = renderWithProviders(<AwePivotTable id="pivotTable"/>, {preloadedState: menuState()});
      await waitFor(() => expect(container.querySelector(".pvtUi")).not.toBeNull());

      fireEvent.contextMenu(container.querySelector(".pvtUi"));

      expect(await screen.findByText("pivot option 1")).toBeDefined();
      expect(screen.queryByText("pivot option 2")).toBeNull();
    });

    it('launches the actions of the chosen option', async () => {
      const {container, dispatchSpy} = renderWithProviders(<AwePivotTable id="pivotTable"/>,
        {preloadedState: menuState(), spyDispatch: true});
      await waitFor(() => expect(container.querySelector(".pvtUi")).not.toBeNull());
      fireEvent.contextMenu(container.querySelector(".pvtUi"));

      fireEvent.click(await screen.findByText("pivot option 1"));

      const launched = dispatchSpy.mock.calls.map(([action]) => action)
        .filter(action => action.type === 'ADD_ACTIONS_TOP').flatMap(action => action.payload);
      expect(launched).toEqual([{type: "screen", target: "Other", address: {component: 'pivotTable', view: 'report'}}]);
    });

    it('does not render a menu without context menu options', async () => {
      const {container} = renderWithProviders(<AwePivotTable id="pivotTable"/>, {preloadedState: menuState([])});
      await waitFor(() => expect(container.querySelector(".pvtUi")).not.toBeNull());

      fireEvent.contextMenu(container.querySelector(".pvtUi"));

      expect(document.querySelector(".p-contextmenu")).toBeNull();
    });
  });
});
