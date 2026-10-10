import $ from "jquery";
import {buildCustomAggregators} from "../../../../main/resources/js/awe/directives/plugins/pivotCustomAggregators";

describe("pivotCustomAggregators", () => {
  let tpl;
  const marker = (value) => "<" + value + ">";
  const records = [{n: 10, d: 100}, {n: 20, d: 100}, {n: 30, d: 100}];

  const compute = (aggregator) => {
    const instance = aggregator.fn(["n", "d"])({}, [], []);
    records.forEach((record) => instance.push(record));
    return instance;
  };

  beforeAll(() => {
    global.jQuery = global.$ = $;
    require("../../../../main/resources/js/lib/pivotTable/pivot.js");
    tpl = $.pivotUtilities.aggregatorTemplates;
  });

  it("applies the custom number format to both 80% bound aggregators", () => {
    const aggregators = buildCustomAggregators(tpl, marker);

    expect(compute(aggregators["Custom 80% Upper Bound"]).format(1)).toBe("<1>");
    expect(compute(aggregators["Custom 80% Lower Bound"]).format(1)).toBe("<1>");
  });

  it("computes the upper bound and the lower bound separately", () => {
    const aggregators = buildCustomAggregators(tpl, marker);
    const upper = compute(aggregators["Custom 80% Upper Bound"]).value();
    const lower = compute(aggregators["Custom 80% Lower Bound"]).value();
    const ratio = 60 / 300;

    expect(upper).toBeGreaterThan(ratio);
    expect(lower).toBeLessThan(ratio);
    expect(upper).not.toBe(lower);
  });
});
