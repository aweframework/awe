/**
 * Colors of the chrome of the charts (texts, axes, tooltip). They come from the AWE design tokens, so that the charts
 * follow the light and the dark mode, the surface and the primary color of the application. ECharts cannot read a
 * token (`light-dark()` stays unresolved in the custom property), so each token is resolved by the browser as the
 * `color` of a probe element inside the chart, which also follows the `color-scheme` of that element.
 */

/**
 * Semantic token of each role of a chart. Roles never use the palette primitives
 */
export const CHART_TOKENS = {
  text: "--awe-color-text",
  title: "--awe-color-text-emphasis",
  muted: "--awe-color-text-muted",
  axis: "--awe-color-chart-axis",
  split: "--awe-color-border-subtle",
  tooltipBackground: "--awe-color-overlay",
  tooltipBorder: "--awe-color-overlay-border",
  tooltipText: "--awe-color-overlay-text",
  surface: "--awe-color-surface"
} as const;

export type ChartColorRole = keyof typeof CHART_TOKENS;

/**
 * Resolved color (rgb) of each role. A role whose token is not defined in the page is left out, so it keeps the
 * ECharts default; the text color is always there
 */
export type ChartColors = Partial<Record<ChartColorRole, string>> & {text: string};

type Node = Record<string, unknown>;

const DARK_QUERY = "(prefers-color-scheme: dark)";
// What changes the mode, the surface or the palette of the application
const ROOT_ATTRIBUTES = ["class", "style", "data-awe-primary", "data-awe-surface"];

/**
 * Resolve the colors of the chart roles in the place where the chart is
 * @param {Element} element Element that holds the chart
 * @returns {object|null} Colors of the roles, null when there is no DOM or the text token is not defined
 */
export function resolveChartColors(element?: Element | null): ChartColors | null {
  if (!element || typeof document === "undefined" || typeof getComputedStyle !== "function") {
    return null;
  }
  // An undefined token would make the probe inherit the color of the element, so only the defined ones are resolved
  const declared = getComputedStyle(element);
  const defined = (Object.entries(CHART_TOKENS) as Array<[ChartColorRole, string]>)
    .filter(([, token]) => declared.getPropertyValue(token).trim() !== "");
  const probe = document.createElement("span");
  probe.style.display = "none";
  element.appendChild(probe);
  try {
    const resolved = Object.fromEntries(defined
      .map(([role, token]) => {
        probe.style.color = `var(${token})`;
        return [role, getComputedStyle(probe).color];
      })
      .filter(([, color]) => color)) as Partial<Record<ChartColorRole, string>>;
    return resolved.text ? resolved as ChartColors : null;
  } finally {
    probe.remove();
  }
}

/**
 * Compare two sets of colors
 * @param {object|null} left Colors
 * @param {object|null} right Colors
 * @returns {boolean} Both have the same colors
 */
export function sameColors(left: ChartColors | null, right: ChartColors | null): boolean {
  if (!left || !right) {
    return left === right;
  }
  return (Object.keys(CHART_TOKENS) as ChartColorRole[]).every(role => left[role] === right[role]);
}

const isNode = (value: unknown): value is Node => Boolean(value) && typeof value === "object" && !Array.isArray(value);

/**
 * Add the properties that a node does not have, at any depth. The node is not changed
 * @param {object} node Node, may be missing
 * @param {object} defaults Properties to add
 * @returns {object} New node
 */
function withDefaults(node: unknown, defaults: Node): Node {
  const result: Node = isNode(node) ? {...node} : {};
  Object.entries(defaults).forEach(([key, value]) => {
    if (result[key] === undefined) {
      result[key] = value;
    } else if (isNode(value) && isNode(result[key])) {
      result[key] = withDefaults(result[key], value);
    }
  });
  return result;
}

/**
 * Apply a function to an element or to each element of a list
 * @param {object|object[]|undefined} value Element or list
 * @param {function} apply Function
 * @returns {object|object[]|undefined} Result
 */
function each(value: unknown, apply: (node: Node) => Node): unknown {
  if (Array.isArray(value)) {
    return value.map(apply);
  }
  return isNode(value) ? apply(value) : value;
}

const line = (color?: string) => ({lineStyle: {color}});

/**
 * Check if a series draws its labels outside the marks (not on top of them, where the text takes the contrast color)
 * @param {object} serie Series
 * @returns {boolean} The series has labels that are drawn over the surface of the chart
 */
function hasOutsideLabel(serie: Node): boolean {
  // A pie draws its labels outside unless the series says otherwise
  const label = serie.label ?? (serie.type === "pie" ? {} : undefined);
  if (!isNode(label) || label.show === false || String(label.position ?? "").startsWith("inside")) {
    return false;
  }
  const points = Array.isArray(serie.data) ? serie.data : [];
  return !points.some(point => isNode(point) && isNode(point.label) && String(point.label.position ?? "").startsWith("inside"));
}

/**
 * Apply the colors to a built option. The colors that the option already has (the ones that the model sets) stay
 * @param {object} option ECharts option, which is not changed
 * @param {object|null} colors Colors of the roles
 * @returns {object} Option with the colors of the chrome
 */
export function applyChartColors<T extends object>(option: T, colors: ChartColors | null | undefined): T {
  if (!colors) {
    return option;
  }
  const source = option as Node;
  const result: Node = {...source};
  const set = (key: string, apply: (node: Node) => Node) => {
    if (source[key] !== undefined) {
      result[key] = each(source[key], apply);
    }
  };
  set("title", title => withDefaults(title, {textStyle: {color: colors.title}, subtextStyle: {color: colors.muted}}));
  set("legend", legend => withDefaults(legend, {
    textStyle: {color: colors.text}, pageTextStyle: {color: colors.text},
    pageIconColor: colors.text, pageIconInactiveColor: colors.muted
  }));
  set("tooltip", tooltip => withDefaults(tooltip, {
    backgroundColor: colors.tooltipBackground, borderColor: colors.tooltipBorder, textStyle: {color: colors.tooltipText}
  }));
  const axis = (node: Node) => withDefaults(node, {
    axisLine: line(colors.axis), axisTick: line(colors.axis), axisLabel: {color: colors.muted},
    nameTextStyle: {color: colors.text}, splitLine: line(colors.split)
  });
  set("xAxis", axis);
  set("yAxis", axis);
  set("dataZoom", zoom => withDefaults(zoom, {textStyle: {color: colors.muted}}));
  set("graphic", element => element.type === "text" ? withDefaults(element, {style: {fill: colors.text}}) : element);
  if (Array.isArray(source.series)) {
    result.series = source.series.map(serie => isNode(serie) && hasOutsideLabel(serie)
      ? withDefaults(serie, {label: {color: colors.text, textBorderWidth: 0}})
      : serie);
  }
  return result as T;
}

/**
 * Call back when the color mode of the application changes: the mode class of the page or of the body (the AngularJS
 * client sets it there), the surface and the palette (attributes and properties of the root element), or the
 * preference of the system
 * @param {function} callback Callback, which may be called without a change of the colors
 * @returns {function} Stop observing
 */
export function observeColorMode(callback: () => void): () => void {
  const stops: Array<() => void> = [];
  if (typeof MutationObserver !== "undefined" && typeof document !== "undefined") {
    const observer = new MutationObserver(() => callback());
    observer.observe(document.documentElement, {attributes: true, attributeFilter: ROOT_ATTRIBUTES});
    if (document.body) {
      observer.observe(document.body, {attributes: true, attributeFilter: ["class"]});
    }
    stops.push(() => observer.disconnect());
  }
  if (typeof window !== "undefined" && typeof window.matchMedia === "function") {
    const query = window.matchMedia(DARK_QUERY);
    const onChange = () => callback();
    query.addEventListener("change", onChange);
    stops.push(() => query.removeEventListener("change", onChange));
  }
  return () => stops.forEach(stop => stop());
}
