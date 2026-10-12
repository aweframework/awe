import {screen} from "@testing-library/react";
import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

describe("awe-react-client/test/js/columns/ColumnCellStyleTest.jsx", () => {
  it("puts the style class that the query assigns to a cell on the cell text", () => {
    renderWithProviders(Columns({address: {view: "report", component: "grid", row: "1", column: "heat"}},
      {value: 42, label: "42", style: "scale-pos-3"}));

    expect(screen.getByText("42")).toHaveClass("p-cell-text", "scale-pos-3");
  });

  it("leaves a cell without style untouched", () => {
    renderWithProviders(Columns({address: {view: "report", component: "grid", row: "1", column: "heat"}},
      {value: 7, label: "7"}));

    expect(screen.getByText("7").className).not.toMatch(/scale-/);
  });
});
