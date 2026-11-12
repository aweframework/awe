import React, {useState} from "react";
import {act, fireEvent} from "@testing-library/react";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {updateAttributes} from "../../../src/redux/actions/components";
import {renderWithProviders} from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";

const mockTable = {renders: 0};

// Counts the renders of the table of the grid, which renders every row of the page
jest.mock("primereact/datatable", () => {
  const actual = jest.requireActual("primereact/datatable");
  const {forwardRef, createElement} = jest.requireActual("react");
  const DataTable = forwardRef((props, ref) => {
    mockTable.renders++;
    return createElement(actual.DataTable, {...props, ref});
  });
  return {...actual, DataTable};
});

/**
 * A grid renders its table only when something of the grid changes: not when the component that holds it renders again
 * and not more than once to show its first rows (#962)
 */
describe("awe-react-client/test/js/performance/GridRenderTest.jsx", () => {
  const ADDRESS = {component: "grid", view: "report"};
  const rows = Array.from({length: 5}, (_, i) => ({id: i + 1, name: `name ${i}`, selected: false}));
  const state = (attributes = {}) => ({
    settings: DEFAULT_SETTINGS,
    screen: {size: {width: 100, height: 100}, breadcrumbs: [], report: {name: "opcion", option: "opcion"}},
    components: {
      grid: {
        address: ADDRESS,
        model: {values: rows, records: rows.length},
        attributes: {columnModel: [{name: "name", label: "Name"}], headerModel: [], buttonModel: [], visible: true, loadAll: true, ...attributes},
        specificAttributes: {sort: []}
      }
    }
  });

  function Holder() {
    const [count, setCount] = useState(0);
    return <>
      <button data-testid="holder" onClick={() => setCount(count + 1)}>render {count}</button>
      <AweGrid id="grid"/>
    </>;
  }

  beforeEach(() => {
    mockTable.renders = 0;
  });

  it("renders its table once to show its rows", () => {
    renderWithProviders(<AweGrid id="grid"/>, {preloadedState: state()});

    expect(mockTable.renders).toBe(1);
  });

  it("does not render its table again when the component that holds it renders", () => {
    const {getByTestId} = renderWithProviders(<Holder/>, {preloadedState: state()});
    mockTable.renders = 0;

    fireEvent.click(getByTestId("holder"));
    fireEvent.click(getByTestId("holder"));

    expect(getByTestId("holder").textContent).toBe("render 2");
    expect(mockTable.renders).toBe(0);
  });

  it("renders its table again when its attributes change", () => {
    const {store} = renderWithProviders(<AweGrid id="grid"/>, {preloadedState: state()});
    mockTable.renders = 0;

    act(() => {
      store.dispatch(updateAttributes(ADDRESS, {pagerValues: [5, 10]}));
    });

    expect(mockTable.renders).toBe(1);
  });
});
