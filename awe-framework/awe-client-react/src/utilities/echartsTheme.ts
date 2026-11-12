/**
 * Colors of the chrome of the charts (texts, axes, tooltip). They come from the AWE design tokens, so that the charts
 * follow the light and the dark mode, the surface and the primary color of the application. ECharts cannot read a
 * token (`light-dark()` stays unresolved in the custom property), so each token is resolved by the browser as the
 * `color` of a probe element inside the chart, which also follows the `color-scheme` of that element.
 */

import {colorSeries, each, withDefaults} from "./echartsStyle";

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
 * Tokens of the colors of the series (categorical palette) and of the 5 steps of the color scale of a heatmap
 */
export const SERIES_TOKENS = Array.from({length: 8}, (_, index) => `--awe-color-chart-series-${index + 1}`);
export const SCALE_TOKENS = Array.from({length: 5}, (_, index) => `--awe-color-scale-${index + 1}`);

/**
 * Resolved color (rgb) of each role. A role whose token is not defined in the page is left out, so it keeps the
 * ECharts default; the text color is always there
 */
export type ChartColors = Partial<Record<ChartColorRole, string>> & {
  text: string;
  /** Colors of the series, in order; missing when the page does not define the palette tokens */
  series?: string[];
  /** Colors of the heatmap scale, from the lowest to the highest; missing when the page does not define them */
  scale?: string[];
};

type Node = Record<string, unknown>;

const DARK_QUERY = "(prefers-color-scheme: dark)";
// What changes the mode, the surface or the palette of the application
const ROOT_ATTRIBUTES = ["class", "style", "data-awe-primary", "data-awe-surface"];

const SRGB = /^color\(srgb\s+([\d.e+-]+)\s+([\d.e+-]+)\s+([\d.e+-]+)(?:\s*\/\s*([\d.e+-]+%?))?\s*\)$/;

/**
 * Computed color of the probe, in a notation that ECharts can read. A token made with `color-mix()` computes to
 * `color(srgb r g b)`, which ECharts does not parse (it would draw it black), so it is written as `rgb()`/`rgba()`
 * @param {HTMLElement} probe Probe element
 * @returns {string} Color
 */
function probeColor(probe: HTMLElement): string {
  const color = getComputedStyle(probe).color;
  const match = SRGB.exec(color ?? "");
  if (!match) {
    return color;
  }
  const [red, green, blue] = match.slice(1, 4).map(channel => Math.round(Math.min(1, Math.max(0, Number(channel))) * 255));
  const alpha = match[4];
  if (alpha === undefined) {
    return `rgb(${red}, ${green}, ${blue})`;
  }
  const opacity = alpha.endsWith("%") ? Number(alpha.slice(0, -1)) / 100 : Number(alpha);
  return `rgba(${red}, ${green}, ${blue}, ${opacity})`;
}

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
        return [role, probeColor(probe)];
      })
      .filter(([, color]) => color)) as Partial<Record<ChartColorRole, string>>;
    if (!resolved.text) {
      return null;
    }
    // A list is used whole or not at all: a palette with holes would repeat or skip colors
    const list = (tokens: string[]): string[] | undefined => {
      const colors = tokens.map(token => {
        if (declared.getPropertyValue(token).trim() === "") {
          return "";
        }
        probe.style.color = `var(${token})`;
        return probeColor(probe);
      });
      return colors.every(Boolean) ? colors : undefined;
    };
    const series = list(SERIES_TOKENS);
    const scale = list(SCALE_TOKENS);
    return {...resolved, ...series ? {series} : {}, ...scale ? {scale} : {}} as ChartColors;
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
  const sameList = (one?: string[], other?: string[]) => one === other || (one?.length === other?.length && Boolean(one) && one!.every((color, index) => color === other![index]));
  return (Object.keys(CHART_TOKENS) as ChartColorRole[]).every(role => left[role] === right[role]) &&
    sameList(left.series, right.series) && sameList(left.scale, right.scale);
}

const isNode = (value: unknown): value is Node => Boolean(value) && typeof value === "object" && !Array.isArray(value);

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
  const heatmap = Array.isArray(source.series) && source.series.some(serie => isNode(serie) && serie.type === "heatmap");
  set("visualMap", map => withDefaults(map, {
    textStyle: {color: colors.text}, ...heatmap && colors.scale ? {inRange: {color: colors.scale}} : {}
  }));
  set("radar", radar => withDefaults(radar, {
    axisName: {color: colors.text}, axisLine: line(colors.axis), splitLine: line(colors.split)
  }));
  set("dataZoom", zoom => withDefaults(zoom, {textStyle: {color: colors.muted}}));
  set("graphic", element => element.type === "text" ? withDefaults(element, {style: {fill: colors.text}}) : element);
  if (Array.isArray(source.series)) {
    result.series = source.series.map(serie => {
      if (!isNode(serie)) {
        return serie;
      }
      const styled = colorSeries(serie, colors);
      return hasOutsideLabel(styled) ? withDefaults(styled, {label: {color: colors.text, textBorderWidth: 0}}) : styled;
    });
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
