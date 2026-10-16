import React from "react";
import PropTypes from "prop-types";
import {Skeleton} from "primereact/skeleton";
import {classNames} from "../utilities/components";
import {TestIds, testHook} from "../utilities/testIds";
import "./AweSkeleton.less";

const DEFAULT_LABEL_LENGTH = 8;
const DEFAULT_TEXTAREA_ROWS = 3;
const LINE_HEIGHT_REM = 1.5;
const FIELD_PADDING_REM = 1;
const BUTTON_PADDING_CH = 4;
const LIST_LINES = 4;
const BAR_HEIGHTS = [45, 70, 55, 85, 60, 40, 75];
const TREND_TYPES = ["line", "area", "spline", "areaspline"];

/**
 * Attributes of the root of a skeleton of a component: the loader test hook is there from the start (the visuals
 * appear after a short delay, see AweSkeleton.less), and it is hidden from assistive technology because it has no content
 * @returns {object} Attributes
 */
const rootProps = () => ({...testHook(TestIds.loader), "aria-hidden": "true"});

const sizeClass = ({size} = {}) => (size === "sm" || size === "lg" ? `awe-skeleton-${size}` : null);
const labelLength = ({label} = {}) => (typeof label === "string" && label ? label.length : DEFAULT_LABEL_LENGTH);

/**
 * Layout classes of the cell that a criterion takes, the ones AweCriterion renders, so its skeleton takes its place
 * @param {object} [attributes] Attributes of the criterion known so far
 * @returns {string} Classes
 */
export const criterionCellClasses = ({style} = {}) => classNames("field", {"col-12": !style}, style);

/**
 * Skeleton of a criterion that is not registered yet: the label and a field with the height of its size
 * @category Components
 * @param {object} [attributes] Attributes of the criterion known so far (label, size, style, areaRows)
 * @param {string} [kind] "field" (default), "textarea" (height of its rows), "check" (box and label) or "list" (option lines)
 * @param {boolean} [generateLabel] Draw the label of the criterion (not for a check, which is its own label)
 * @returns {JSX.Element} Skeleton
 */
export function CriterionSkeleton({attributes = {}, kind = "field", generateLabel = true}) {
  const {style = "", areaRows} = attributes;
  const rows = Number(areaRows) || DEFAULT_TEXTAREA_ROWS;
  const hasLabel = !(typeof style === "string" && style.includes("no-label")) && generateLabel;
  const label = <Skeleton className="awe-skeleton-label" width={`${labelLength(attributes)}ch`}/>;
  let content;
  if (kind === "check") {
    content = <>
      <Skeleton className="awe-skeleton-check"/>
      <Skeleton className="awe-skeleton-label" width={`${labelLength(attributes)}ch`}/>
    </>;
  } else if (kind === "list") {
    content = <>
      {hasLabel && label}
      {Array.from({length: LIST_LINES}, (_, index) =>
        <Skeleton key={index} className="awe-skeleton-line" width={`${90 - (index % 3) * 15}%`}/>)}
    </>;
  } else {
    content = <>
      {hasLabel && label}
      <Skeleton className={classNames("awe-skeleton-field", sizeClass(attributes))}
        height={kind === "textarea" ? `${rows * LINE_HEIGHT_REM + FIELD_PADDING_REM}rem` : undefined}/>
    </>;
  }
  return <div className={classNames("awe-skeleton-criterion", `awe-skeleton-criterion-${kind}`, criterionCellClasses(attributes))}
    {...rootProps()}>{content}</div>;
}

CriterionSkeleton.propTypes = {
  attributes: PropTypes.object,
  kind: PropTypes.string,
  generateLabel: PropTypes.bool
};

/**
 * Skeleton of a button that is not registered yet: a block as wide as its label
 * @category Components
 * @param {object} [attributes] Attributes of the button known so far (label, size)
 * @param {string} [className] Extra classes of the button
 * @returns {JSX.Element} Skeleton
 */
export function ButtonSkeleton({attributes = {}, className}) {
  return <span className={classNames("awe-skeleton-button", "mr-2", "mt-2", sizeClass(attributes), className)} {...rootProps()}>
    <Skeleton width={`${labelLength(attributes) + BUTTON_PADDING_CH}ch`}/>
  </span>;
}

ButtonSkeleton.propTypes = {
  attributes: PropTypes.object,
  className: PropTypes.string
};

/**
 * Hint of the chart that is coming: bars (default), a trend (lines and areas) or a pie
 * @param {object} [echartsModel] Chart model sent by the server
 * @returns {string} "bars", "trend" or "pie"
 */
export function chartHintOf(echartsModel) {
  const type = [].concat(echartsModel?.series ?? [])[0]?.type;
  if (type === "pie") {
    return "pie";
  }
  return TREND_TYPES.includes(type) ? "trend" : "bars";
}

/**
 * Skeleton of a chart that is not drawn yet: the area of the chart with a hint of its type. It is not a loader test
 * hook: a chart in a hidden tab is only drawn when it is shown, and the chart hook already tells whether it is rendered
 * @category Components
 * @param {string} [hint] "bars" (default), "trend" or "pie"
 * @returns {JSX.Element} Skeleton
 */
export function ChartSkeleton({hint = "bars"}) {
  let shapes;
  if (hint === "pie") {
    shapes = <Skeleton className="awe-skeleton-pie" shape="circle"/>;
  } else if (hint === "trend") {
    shapes = <Skeleton className="awe-skeleton-trend"/>;
  } else {
    shapes = BAR_HEIGHTS.map((height, index) => <Skeleton key={index} className="awe-skeleton-bar" height={`${height}%`}/>);
  }
  return <div className={classNames("awe-skeleton-chart", `awe-skeleton-chart-${hint}`)} aria-hidden="true">{shapes}</div>;
}

ChartSkeleton.propTypes = {
  hint: PropTypes.string
};
