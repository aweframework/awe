import React from 'react';
import TableRenderers from "react-pivottable/TableRenderers";
import {render} from '@testing-library/react';
import TableBarchartRenderer from "../../../../src/components/pivot/TableBarchartRenderer";

const DATA = [
  {Row: "a", Col: "x", Qty: 10}, {Row: "a", Col: "y", Qty: 5}, {Row: "a", Col: "z", Qty: -4},
  {Row: "b", Col: "x", Qty: 2}, {Row: "b", Col: "y", Qty: 7}
];

const renderBarchart = (data = DATA, props = {}) => render(<TableBarchartRenderer data={data} rows={["Row"]} cols={["Col"]}
  aggregatorName="Sum" vals={["Qty"]} {...props}/>);

const heights = row => [...row.querySelectorAll("td.pvtVal")]
  .map(cell => cell.querySelector(".pvtBar")?.style.height ?? null);

describe('TableBarchartRenderer', () => {
  it('keeps the pivot table with the formatted value in every cell', () => {
    const {container} = renderBarchart();

    expect(container.querySelector("table.pvtTable")).not.toBeNull();
    const first = container.querySelector("td.pvtVal");
    expect(first.querySelector(".pvtBarText").textContent).toBe("10.00");
    expect(first.classList.contains("pvtBarCell")).toBe(true);
  });

  it('scales every row against its own maximum with the AngularJS formula', () => {
    const {container} = renderBarchart();
    const rows = container.querySelectorAll("tbody tr");

    expect(heights(rows[0])).toEqual(["71.42857142857143%", "35.714285714285715%", null]);
    expect(heights(rows[1])).toEqual(["20.408163265306126%", "71.42857142857143%", null]);
  });

  it('draws no bar for negative values, like the invalid CSS height of AngularJS', () => {
    const {container} = renderBarchart();
    const negative = container.querySelectorAll("tbody tr")[0].querySelectorAll("td.pvtVal")[2];

    expect(negative.querySelector(".pvtBar")).toBeNull();
    expect(negative.querySelector(".pvtBarText").textContent).toBe("-4.00");
  });

  it('draws the bars of the totals row against its own maximum', () => {
    const {container} = renderBarchart();
    const totals = [...container.querySelectorAll("tbody tr:last-child td.pvtTotal")];

    const percentages = totals.map(cell => parseFloat(cell.querySelector(".pvtBar")?.style.height));
    expect(percentages[0]).toBeCloseTo(71.428, 2);
    expect(percentages[1]).toBeCloseTo(71.428, 2);
    expect(percentages[2]).toBeNaN();
  });

  it('does not wrap the grand total nor the totals column', () => {
    const {container} = renderBarchart();

    expect(container.querySelector("td.pvtGrandTotal .pvtBar")).toBeNull();
    expect(container.querySelector("tbody tr td.pvtTotal:last-child .pvtBar")).toBeNull();
  });

  it('draws only the totals row when only cols are configured', () => {
    const {container} = renderBarchart(DATA, {rows: []});

    expect(container.querySelectorAll("td.pvtVal")).toHaveLength(0);
    expect(container.querySelectorAll("td.pvtTotal .pvtBar")).toHaveLength(2);
    expect(container.querySelector("td.pvtGrandTotal .pvtBar")).toBeNull();
  });

  it('renders the plain table without bars when only rows are configured', () => {
    const {container} = renderBarchart(DATA, {cols: []});

    expect(container.querySelector("table.pvtTable")).not.toBeNull();
    expect(container.querySelectorAll(".pvtBar")).toHaveLength(0);
  });

  it('renders an empty data set without bars', () => {
    const {container} = renderBarchart([]);

    expect(container.querySelector("table.pvtTable")).not.toBeNull();
    expect(container.querySelectorAll(".pvtBar")).toHaveLength(0);
  });

  it('draws no bar in a row whose values are all negative, keeping the text', () => {
    const {container} = renderBarchart([{Row: "a", Col: "x", Qty: -4}, {Row: "a", Col: "y", Qty: -2}]);

    expect(container.querySelectorAll("td.pvtVal .pvtBar")).toHaveLength(0);
    expect([...container.querySelectorAll("td.pvtVal .pvtBarText")].map(text => text.textContent))
      .toEqual(["-4.00", "-2.00"]);
  });

  it('draws no bar in a row whose maximum is zero', () => {
    const {container} = renderBarchart([{Row: "a", Col: "x", Qty: 0}, {Row: "a", Col: "y", Qty: -2}]);

    expect(container.querySelectorAll("td.pvtVal .pvtBar")).toHaveLength(0);
  });

  it('does not fail with a non numeric aggregator', () => {
    const {container} = renderBarchart(DATA, {aggregatorName: "List Unique Values"});

    expect(container.querySelector("table.pvtTable")).not.toBeNull();
    expect(container.querySelectorAll(".pvtBar")).toHaveLength(0);
  });

  it('draws bars with a counting aggregator', () => {
    const {container} = renderBarchart(DATA, {aggregatorName: "Count Unique Values"});

    expect(container.querySelectorAll("td.pvtVal .pvtBar").length).toBeGreaterThan(0);
  });

  describe('with an unexpected table layout', () => {
    afterEach(() => jest.restoreAllMocks());

    it('returns the plain table when the table has no body', () => {
      jest.spyOn(TableRenderers.Table.prototype, "render").mockReturnValue(<table className="plain"><thead/></table>);
      const {container} = renderBarchart();

      expect(container.querySelector("table.plain")).not.toBeNull();
      expect(container.querySelectorAll(".pvtBar")).toHaveLength(0);
    });

    it('returns the plain table when the value rows do not match the row keys', () => {
      jest.spyOn(TableRenderers.Table.prototype, "render").mockReturnValue(
        <table className="plain"><thead/><tbody><tr><td className="pvtVal">1</td></tr></tbody></table>);
      const {container} = renderBarchart();

      expect(container.querySelector("table.plain td.pvtVal").textContent).toBe("1");
      expect(container.querySelectorAll(".pvtBar")).toHaveLength(0);
    });

    it('recognises the cell class among several classes', () => {
      jest.spyOn(TableRenderers.Table.prototype, "render").mockReturnValue(
        <table><thead/><tbody>
          <tr><td className="pvtVal extra">1</td></tr>
          <tr><td className="extra pvtTotal">1</td></tr>
        </tbody></table>);
      const {container} = renderBarchart([{Row: "a", Col: "x", Qty: 1}]);

      expect(container.querySelectorAll(".pvtBar")).toHaveLength(2);
    });
  });
});
