import React from "react";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";

function ColumnStaticColor(props) {
  const {data, style} = props;
  const {style: cellStyle, value} = data || {};
  const classes = classNames("colorpicker ml-2 " + (value ? "" : "no-color"), style, cellStyle);
  return <>
    <span>{value}</span>
    {value && <span className={classes} style={{backgroundColor: value || null}}/>}
  </>;
}

ColumnStaticColor.propTypes = {
  data: PropTypes.oneOfType([PropTypes.object, PropTypes.array]),
  style: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
};

export default ColumnStaticColor;