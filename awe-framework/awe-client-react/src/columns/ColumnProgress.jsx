import React from "react";
import {ProgressBar} from "primereact/progressbar";
import {classNames} from "../utilities/components";
import {extractCellModel} from "../utilities/grid";
import {getFirstDefinedAndNotNullValue} from "../utilities/general";
import {barFill, barScale, barSegments, barZero, hasBarOptions, hasBarZeroMark, toBarNumber} from "../utilities/cellBar";
import PropTypes from "prop-types";

const SEGMENT_TONES = 3;

// Geometry of a fill or a segment: percentages of the track, rounded to keep the markup short. A fill is placed by its
// two edges (not by its width), so the end on the zero falls exactly on the axis, with no gap of a pixel between them
const percent = (value) => `${Number(value.toFixed(4))}%`;
const place = ({left, width}) => ({left: percent(left), right: percent(Math.max(0, 100 - left - width))});

/**
 * Value and text of a field of the row
 * @param {Object} row Row data
 * @param {string} field Field name
 * @return {{value: *, text: string}} Value of the field and its label (or value) as it comes
 */
function fieldOf(row, field) {
  const {label, value} = extractCellModel(row?.[field]);
  return {value, text: String(getFirstDefinedAndNotNullValue(label, value, ""))};
}

/** A lane of the bar: the fill (or the segments) of one value */
function Lane({className, children}) {
  return <span className={classNames("awe-data-bar-lane", className)}>{children}</span>;
}

function Fill({scale, value}) {
  const fill = barFill(scale, value);
  // On a signed scale the end of the fill on the zero axis is square
  return <span style={place(fill)}
    className={classNames("awe-data-bar-fill", hasBarZeroMark(scale) && "awe-data-bar-signed", fill.negative && "awe-data-bar-negative")}/>;
}

/**
 * Data bar behind the number, with a scale, a second value or stacked segments (#831)
 * @param props Column attributes, cell data and the row
 */
function ColumnBar(props) {
  const {data, row, style, barMin, barMax, barSecondField, barStackFields, barLeafOnly} = props;
  const value = toBarNumber(data?.value);
  // A row that is not a leaf of a tree grid has no bar (every row of a flat grid is a leaf)
  if (value === null || (barLeafOnly === true && row?.$leaf === false)) {
    return null;
  }
  const scale = barScale(barMin, barMax);
  const mainText = String(getFirstDefinedAndNotNullValue(data?.label, data?.value, ""));
  const stackFields = (barStackFields || "").split(",").map(field => field.trim()).filter(Boolean);
  const stack = stackFields.map(field => fieldOf(row, field));
  const second = barSecondField ? fieldOf(row, barSecondField) : null;
  const secondValue = second ? toBarNumber(second.value) : null;

  let text = mainText;
  if (stack.length) {
    const parts = stack.filter(part => toBarNumber(part.value) > 0).map(part => part.text);
    text = parts.length ? `${mainText} (${parts.join(" + ")})` : mainText;
  } else if (secondValue !== null) {
    text = `${mainText} / ${second.text}`;
  }
  const segments = barSegments(scale, stack.map(part => part.value));

  return <div className={classNames(style, data?.style, "column-progress", "awe-data-bar", secondValue !== null && "awe-data-bar-two")}
    role="meter" aria-valuemin={scale.min} aria-valuemax={scale.max}
    aria-valuenow={Math.min(Math.max(value, scale.min), scale.max)} aria-valuetext={text} title={text}>
    <span className="awe-data-bar-lanes" aria-hidden="true">
      <Lane>
        {stackFields.length
          ? segments.map(segment => <span key={segment.index} style={place(segment)}
            className={`awe-data-bar-segment awe-data-bar-segment-${segment.index % SEGMENT_TONES}`}/>)
          : <Fill scale={scale} value={value}/>}
      </Lane>
      {secondValue !== null && <Lane className="awe-data-bar-lane-second"><Fill scale={scale} value={secondValue}/></Lane>}
      {hasBarZeroMark(scale) && <span className="awe-data-bar-axis" style={{left: percent(barZero(scale))}}/>}
    </span>
    <span className={classNames("awe-data-bar-value", value < 0 && "awe-data-bar-value-negative")}>{mainText}</span>
  </div>;
}

function ColumnProgress(props) {
  if (hasBarOptions(props)) {
    return <ColumnBar {...props}/>;
  }
  const { data, style } = props;
  const { value } = data;
  const classes = classNames(style, data?.style, "column-progress");
  return <ProgressBar className={classes} style={{ width: "100%" }} value={value || 0} />;
}

ColumnProgress.propTypes = {
  data: PropTypes.object.isRequired,
  style: PropTypes.string,
  row: PropTypes.object,
  barMin: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  barMax: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  barSecondField: PropTypes.string,
  barStackFields: PropTypes.string,
  barLeafOnly: PropTypes.bool
};

export default ColumnProgress;
