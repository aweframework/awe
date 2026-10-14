import React from "react";
import PropTypes from "prop-types";
import {classNames} from "../utilities/components";
import {buildSparkline} from "../utilities/sparkline";

/**
 * Converts the name of an SVG attribute of the shared renderer to its React property
 * @param {string} name Attribute name, like stroke-width
 * @return {string} React property, like strokeWidth
 */
const toProp = name => name.replace(/-([a-z])/g, (_, letter) => letter.toUpperCase());

/**
 * Sparkline column: draws the series of numbers of the cell (a JSON array) as a small SVG chart. The drawing comes
 * from the renderer shared with the AngularJS client; each point has a transparent hit area with its value as the
 * tooltip. The SVG is not focusable, so the keyboard navigation of the grid is not affected.
 */
function ColumnSparkline(props) {
  const {data, align, style, sparklineType, sparklineColor, sparklineNegativeColor, sparklineLastPoint, sparklineMinMax} = props;
  const model = buildSparkline(data?.value, {
    type: sparklineType,
    color: sparklineColor,
    negativeColor: sparklineNegativeColor,
    lastPoint: sparklineLastPoint,
    minMax: sparklineMinMax
  });
  const classes = classNames("column-sparkline", style, data?.style);
  return <span className={classes} style={{textAlign: align || "left"}}>
    {model && <svg
      className="awe-sparkline"
      data-type={model.type}
      viewBox={model.viewBox}
      width={model.width}
      height={model.height}
      role="img"
      aria-label={model.label}
      focusable="false">
      {model.shapes.map((shape, index) => React.createElement(
        shape.tag,
        Object.fromEntries([["key", index], ...Object.entries(shape.attrs).map(([name, value]) => [toProp(name), value])]),
        shape.title === undefined ? null : <title>{shape.title}</title>
      ))}
    </svg>}
  </span>;
}

ColumnSparkline.propTypes = {
  data: PropTypes.object.isRequired,
  align: PropTypes.string,
  style: PropTypes.string,
  sparklineType: PropTypes.string,
  sparklineColor: PropTypes.string,
  sparklineNegativeColor: PropTypes.string,
  sparklineLastPoint: PropTypes.bool,
  sparklineMinMax: PropTypes.bool
};

export default ColumnSparkline;
