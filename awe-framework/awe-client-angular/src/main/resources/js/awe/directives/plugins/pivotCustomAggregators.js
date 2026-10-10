/**
 * Build the custom pivot aggregators that apply a number format
 * @param {Object} tpl Pivot aggregator templates ($.pivotUtilities.aggregatorTemplates)
 * @param {Function} numberFormat Number formatter
 * @return {Object} Aggregators by name
 */
export function buildCustomAggregators(tpl, numberFormat) {
  return {
    "Custom Sum": {fn: tpl.sum(numberFormat), label: "Custom Sum"},
    "Custom Average": {fn: tpl.average(numberFormat), label: "Custom Average"},
    "Custom Minimum": {fn: tpl.min(numberFormat), label: "Custom Minimum"},
    "Custom Maximum": {fn: tpl.max(numberFormat), label: "Custom Maximum"},
    "Custom Sum over Sum": {fn: tpl.sumOverSum(numberFormat), label: "Custom Sum over Sum"},
    // sumOverSumBound80(upper, formatter)
    "Custom 80% Upper Bound": {fn: tpl.sumOverSumBound80(true, numberFormat), label: "Custom 80% Upper Bound"},
    "Custom 80% Lower Bound": {fn: tpl.sumOverSumBound80(false, numberFormat), label: "Custom 80% Lower Bound"}
  };
}
