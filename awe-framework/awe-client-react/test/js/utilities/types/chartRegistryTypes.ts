// Type-level assertions for utilities/chartRegistry, checked by `npm run typecheck` (not run by Jest)
import {
  CHART_SIZE,
  ChartImage,
  ChartSize,
  getChartImage,
  getPrintOrientation,
  registerChart,
  unregisterChart
} from "../../../../src/utilities/chartRegistry";

// The image size of each page orientation
const portrait: ChartSize = CHART_SIZE.PORTRAIT;
const width: number = CHART_SIZE.LANDSCAPE.width;
// @ts-expect-error there is no such orientation
const unknownSize = CHART_SIZE.SQUARE;

// A chart draws its SVG image for a size
const chart: ChartImage = {getImage: ({width: w, height: h}) => `<svg width="${w}" height="${h}"/>`};
// @ts-expect-error a chart must draw its image
const notChart: ChartImage = {};
// @ts-expect-error the image is text
const wrongChart: ChartImage = {getImage: () => 1};

registerChart("Chart", chart);
unregisterChart("Chart", chart);
// @ts-expect-error the identifier is text
registerChart(1, chart);

// The image is undefined when the chart is not rendered
const image: string | undefined = getChartImage("Chart", "LANDSCAPE");
// @ts-expect-error the image can be undefined
const alwaysImage: string = getChartImage("Chart", "PORTRAIT");

// The orientation of the page comes from the components of the screen
const orientation: string = getPrintOrientation({
  Orientation: {address: {component: "reportOrientation"}, model: {values: [{value: "LANDSCAPE", selected: true}]}}
});
const defaultOrientation: string = getPrintOrientation();
const nullOrientation: string = getPrintOrientation(null);
// @ts-expect-error the components are an object
getPrintOrientation("LANDSCAPE");

export {portrait, width, unknownSize, notChart, wrongChart, image, alwaysImage, orientation, defaultOrientation, nullOrientation};
