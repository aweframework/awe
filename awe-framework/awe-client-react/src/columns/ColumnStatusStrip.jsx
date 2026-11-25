import React, {useRef} from "react";
import PropTypes from "prop-types";
import {Tooltip} from "primereact/tooltip";
import {classNames} from "../utilities/components";
import {buildStatusStrip, statusStripTooltip} from "../utilities/statusStrip";
import "./ColumnStatusStrip.less";

// Show delay of the tooltip of a bar: half of the help tooltip delay of AWE (helpTimeout, 1000 ms) and about half of
// the native title, which felt slow over a strip (#993). The PrimeReact default (0) would flash a tooltip on every bar
// the pointer crosses. Once a tooltip is visible, moving to another bar updates it at once.
export const STATUS_STRIP_TOOLTIP_DELAY = 500;

const BAR = ".awe-status-strip-item";

/**
 * Status strip column: draws the ordered statuses of the cell (a JSON array of {tone, label, title?} items) as bars of
 * their tone. The strip is one image whose accessible name counts the statuses per label; it is not focusable, so the
 * keyboard navigation of the grid is not affected. Hovering a bar shows its label and title in the PrimeReact tooltip:
 * one tooltip per strip, moved to the hovered bar (the hover is delegated to the strip), instead of one per bar, so a
 * grid of long strips does not bind a tooltip to every bar. The rules come from the module shared with the AngularJS
 * client.
 */
function ColumnStatusStrip(props) {
  const {data, align, style} = props;
  const tooltipRef = useRef(null);
  const strip = buildStatusStrip(data?.value);
  const classes = classNames("column-status-strip", style, data?.style);

  const showBar = (event) => {
    const bar = event.target?.closest?.(BAR);
    if (bar) {
      // The tooltip reads its text from the data-pr-tooltip attribute of the target it is shown on
      tooltipRef.current?.show({type: event.type, currentTarget: bar, pageX: event.pageX, pageY: event.pageY});
    }
  };
  const hide = (event) => tooltipRef.current?.hide(event);

  return <span className={classes} style={{textAlign: align || "left"}}>
    {strip && <>
      <Tooltip ref={tooltipRef} position="top" showDelay={STATUS_STRIP_TOOLTIP_DELAY} className="awe-status-strip-tooltip"/>
      <span className="awe-status-strip" role="img" aria-label={strip.label} onMouseOver={showBar} onMouseLeave={hide}
        onFocus={showBar} onBlur={hide}>
        {strip.items.map((item, index) => <span
          key={index}
          className={`awe-status-strip-item awe-status-strip-${item.tone}`}
          data-tone={item.tone}
          data-pr-tooltip={statusStripTooltip(item)}/>)}
      </span>
    </>}
  </span>;
}

ColumnStatusStrip.propTypes = {
  data: PropTypes.object.isRequired,
  align: PropTypes.string,
  style: PropTypes.string
};

export default ColumnStatusStrip;
