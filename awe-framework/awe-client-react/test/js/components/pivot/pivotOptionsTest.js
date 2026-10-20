import {
  absoluteSort,
  buildCustomAggregators,
  buildPivotProps,
  mapRendererName,
  PIVOT_RENDERER_NAMES,
  XML_ONLY_ATTRIBUTES
} from "../../../../src/components/pivot/pivotOptions";

describe('pivotOptions', () => {
  const settings = {numericOptions: {digitGroupSeparator: ',', decimalCharacter: '.', decimalPlaces: 2}, pivotOptions: {numGroup: 5000}};

  it('maps AngularJS renderer names to react-pivottable names', () => {
    expect(mapRendererName("Table")).toBe("Table");
    expect(mapRendererName("Heatmap")).toBe("Table Heatmap");
    expect(mapRendererName("Row Heatmap")).toBe("Table Row Heatmap");
    expect(mapRendererName("Col Heatmap")).toBe("Table Col Heatmap");
    expect(mapRendererName("Table Barchart")).toBe(PIVOT_RENDERER_NAMES["Table Barchart"]);
    expect(mapRendererName("Table Barchart")).toBe("Table");
    expect(mapRendererName(undefined)).toBeUndefined();
  });

  it('sorts numeric values by absolute value', () => {
    expect(["-5", "3", "-1", "x"].sort(absoluteSort)).toEqual(["x", "-1", "3", "-5"]);
    expect(absoluteSort("a", "b")).toBe(0);
  });

  it('builds initial props from attributes', () => {
    const props = buildPivotProps({renderer: "Heatmap", aggregator: "Sum", aggregationField: "Als", sortMethod: "absolute"}, settings);
    expect(props.rendererName).toBe("Table Heatmap");
    expect(props.aggregatorName).toBe("Sum");
    expect(props.vals).toEqual(["Als"]);
    expect(typeof props.sorters).toBe("function");
    expect(props.sorters("any")).toBe(absoluteSort);
    expect(props.menuLimit).toBe(5000);
  });

  it('natural sort without set-pivot-sorters has no sorters', () => {
    const props = buildPivotProps({sortMethod: "natural"}, settings);
    expect(props.vals).toBeUndefined();
    expect(props.sorters).toBeUndefined();
    expect(props.aggregatorName).toBeUndefined();
  });

  it('applies set-pivot-sorters orders first, like AngularJS sortAs, then the sort method', () => {
    const absolute = buildPivotProps({sortMethod: "absolute", sorters: {Als: ["b", "a"]}}, settings);
    expect(["a", "b"].sort(absolute.sorters("Als"))).toEqual(["b", "a"]);
    expect(absolute.sorters("Other")).toBe(absoluteSort);
    const natural = buildPivotProps({sorters: {Als: ["b", "a"]}}, settings);
    expect(["a", "b"].sort(natural.sorters("Als"))).toEqual(["b", "a"]);
    expect(natural.sorters("Other")).toBeUndefined();
  });

  it('takes menuLimit from settings or falls back to 5000', () => {
    expect(buildPivotProps({}, {pivotOptions: {numGroup: 10}}).menuLimit).toBe(10);
    expect(buildPivotProps({}, {}).menuLimit).toBe(5000);
  });

  it('does not forward raw XML attributes', () => {
    const props = buildPivotProps({renderer: "Table", aggregator: "Sum", aggregationField: "A", sortMethod: "absolute",
      decimalNumbers: 2, thousandSeparator: ".", decimalSeparator: ",", totalColumnPlacement: "left", totalRowPlacement: "top"}, settings);
    XML_ONLY_ATTRIBUTES.forEach(name => expect(props).not.toHaveProperty(name));
    expect(XML_ONLY_ATTRIBUTES).toContain("totalRowPlacement");
  });

  it('formats Custom aggregators with attribute and numeric option defaults', () => {
    const format = (attrs, opts = settings) => {
      const aggs = buildCustomAggregators(attrs, opts);
      const agg = aggs["Custom Sum"](["v"])();
      agg.push({v: 1234.5});
      return agg.format(agg.value());
    };
    expect(Object.keys(buildCustomAggregators({}, settings))).toEqual(expect.arrayContaining(["Custom Sum", "Custom Average",
      "Custom Minimum", "Custom Maximum", "Custom Sum over Sum", "Custom 80% Upper Bound", "Custom 80% Lower Bound", "Sum"]));
    expect(format({decimalNumbers: 2, thousandSeparator: ".", decimalSeparator: ","})).toBe("1.234,50");
    // AngularJS parity: no decimal-numbers means 0 decimals (its numeric mDec default is null)
    expect(format({})).toBe("1,235");
    expect(format({}, {})).toBe("1,235");
    // Server settings use the AngularJS names (WebNumberOptions)
    expect(format({}, {numericOptions: {mDec: 1, aSep: ".", aDec: ","}})).toBe("1.234,5");
  });

  it('computes the Custom 80% bounds over sum/sum like pivottable.js', () => {
    const aggs = buildCustomAggregators({decimalNumbers: 6}, {});
    const run = (name) => {
      const agg = aggs[name](["num", "den"])();
      [{num: 1, den: 4}, {num: 2, den: 6}, {num: "x", den: "y"}].forEach(record => agg.push(record));
      return agg.format(agg.value());
    };
    expect(run("Custom 80% Upper Bound")).toBe("0.502628");
    expect(run("Custom 80% Lower Bound")).toBe("0.153799");
    expect(aggs["Custom 80% Upper Bound"]([])().numInputs).toBe(2);
    expect(aggs["Custom 80% Upper Bound"](["num", "den"])().numInputs).toBe(0);
  });

  it('puts non-numeric values first in the absolute sort', () => {
    expect(absoluteSort("x", "3")).toBe(-1);
    expect(absoluteSort("3", "x")).toBe(1);
  });

  it('leaves renderer and aggregator to the library when they are not set', () => {
    const props = buildPivotProps({}, {});
    expect(props).not.toHaveProperty("rendererName");
    expect(props).not.toHaveProperty("aggregatorName");
  });
});
