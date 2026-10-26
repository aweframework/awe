import {aggregatorTemplates as tpl, aggregators, numberFormat, sortAs} from "react-pivottable/Utilities";

/** Default number of values in the pivot filter menu (AngularJS pivotOptions.numGroup) */
const DEFAULT_MENU_LIMIT = 5000;

/**
 * AngularJS renderer names mapped to react-pivottable renderer names.
 * "Table Barchart" is a custom renderer registered by AwePivotTable (see TableBarchartRenderer).
 */
export const PIVOT_RENDERER_NAMES = {
  "Table": "Table",
  "Heatmap": "Table Heatmap",
  "Row Heatmap": "Table Row Heatmap",
  "Col Heatmap": "Table Col Heatmap",
  "Table Barchart": "Table Barchart"
};

/** XML attributes that react-pivottable does not understand and must not be forwarded */
export const XML_ONLY_ATTRIBUTES = ["renderer", "aggregator", "aggregationField", "sortMethod", "decimalNumbers",
  "thousandSeparator", "decimalSeparator", "totalColumnPlacement", "totalRowPlacement"];

/**
 * Map an AngularJS renderer name to the react-pivottable one
 * @param {string} renderer Renderer name
 * @returns {string|undefined} react-pivottable renderer name
 */
export function mapRendererName(renderer) {
  return renderer ? (PIVOT_RENDERER_NAMES[renderer] ?? renderer) : undefined;
}

/**
 * Sort by absolute value (same semantics as AngularJS pivotSortFunction "absolute")
 * @param {*} as First value
 * @param {*} bs Second value
 * @returns {number} Comparison
 */
export function absoluteSort(as, bs) {
  const a = parseFloat(as);
  const b = parseFloat(bs);
  if (Number.isNaN(a) && Number.isNaN(b)) {
    return 0;
  }
  if (Number.isNaN(a)) {
    return -1;
  }
  if (Number.isNaN(b)) {
    return 1;
  }
  return Math.abs(a) - Math.abs(b);
}

/**
 * 80% confidence bound over sum/sum (not provided by react-pivottable 0.11, ported from pivottable.js)
 * @param {boolean} upper Upper bound when true, lower otherwise
 * @param {Function} formatter Number formatter
 * @returns {Function} Aggregator template
 */
function sumOverSumBound80(upper, formatter) {
  return ([num, denom]) => () => ({
    sumNum: 0,
    sumDenom: 0,
    push(record) {
      if (!Number.isNaN(parseFloat(record[num]))) {
        this.sumNum += parseFloat(record[num]);
      }
      if (!Number.isNaN(parseFloat(record[denom]))) {
        this.sumDenom += parseFloat(record[denom]);
      }
    },
    value() {
      const sign = upper ? 1 : -1;
      return (0.821187207574908 / this.sumDenom + this.sumNum / this.sumDenom + 1.2815515655446004 * sign *
        Math.sqrt(0.410593603787454 / (this.sumDenom * this.sumDenom) +
          (this.sumNum * (1 - this.sumNum / this.sumDenom)) / (this.sumDenom * this.sumDenom))) /
        (1 + 1.642374415149816 / this.sumDenom);
    },
    format: formatter,
    numInputs: typeof num !== "undefined" && typeof denom !== "undefined" ? 0 : 2
  });
}

/**
 * Build the custom aggregators (library ones plus the formatted "Custom *" ones)
 * @param {Object} attributes Pivot attributes (decimalNumbers, thousandSeparator, decimalSeparator)
 * @param {Object} settings Application settings (numericOptions)
 * @returns {Object} Aggregators by name
 */
export function buildCustomAggregators(attributes = {}, settings = {}) {
  const numeric = settings.numericOptions ?? {};
  const format = numberFormat({
    // Like AngularJS: only the server mDec option sets default decimals, otherwise none
    digitsAfterDecimal: attributes.decimalNumbers ?? numeric.mDec ?? 0,
    thousandsSep: attributes.thousandSeparator ?? numeric.aSep ?? numeric.digitGroupSeparator ?? ",",
    decimalSep: attributes.decimalSeparator ?? numeric.aDec ?? numeric.decimalCharacter ?? "."
  });
  return {
    ...aggregators,
    "Custom Sum": tpl.sum(format),
    "Custom Average": tpl.average(format),
    "Custom Minimum": tpl.min(format),
    "Custom Maximum": tpl.max(format),
    "Custom Sum over Sum": tpl.sumOverSum(format),
    "Custom 80% Upper Bound": sumOverSumBound80(true, format),
    "Custom 80% Lower Bound": sumOverSumBound80(false, format)
  };
}

/**
 * Whether the value is a plain object (set-pivot-sorters orders), not a sorter function
 * @param {*} value Value
 * @returns {boolean} Plain object
 */
function isPlainObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

/**
 * Build the react-pivottable props derived from the AWE pivot attributes (initial values)
 * @param {Object} attributes Pivot attributes
 * @param {Object} settings Application settings
 * @returns {Object} react-pivottable props
 */
export function buildPivotProps(attributes = {}, settings = {}) {
  const {renderer, aggregator, aggregationField, sortMethod} = attributes;
  const props = {
    aggregators: buildCustomAggregators(attributes, settings),
    menuLimit: settings.pivotOptions?.numGroup ?? DEFAULT_MENU_LIMIT
  };
  if (renderer) {
    props.rendererName = mapRendererName(renderer);
  }
  if (aggregator) {
    props.aggregatorName = aggregator;
  }
  if (aggregationField) {
    props.vals = [aggregationField];
  }
  // Like AngularJS: set-pivot-sorters orders (sortAs) first, then the sort method for the other attributes
  const orders = isPlainObject(attributes.sorters) ? attributes.sorters : {};
  const fallback = sortMethod === "absolute" ? absoluteSort : undefined;
  if (fallback || Object.keys(orders).length > 0) {
    props.sorters = attribute => attribute in orders ? sortAs(orders[attribute]) : fallback;
  }
  return props;
}
