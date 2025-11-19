import React from "react";
import {classNames} from "../utilities/components";

function ColumnStaticColor(props) {
  const {data, style} = props;
  const {style: cellStyle, value} = data || {};
  const classes = classNames("colorpicker ml-2 " + (value ? "" : "no-color"), style, cellStyle);
  return <>
    <span>{value}</span>
    {value && <span className={classes} style={{backgroundColor: value || null}}/>}
  </>;
}

export default ColumnStaticColor;