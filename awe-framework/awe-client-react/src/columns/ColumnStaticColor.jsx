import React, {Component} from "react";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";

export default class ColumnStaticColor extends Component {

  render() {
    const {data, style} = this.props;
    const classes = classNames("colorpicker ml-2 " + (value ? "" : "no-color"), style, data?.style);
    const {value} = data || {};
    return <>
      <span>{value}</span>
      {value && <span className={classes} style={{backgroundColor: value || null}}/>}
    </>
  }
}

ColumnStaticColor.propTypes = {
  data: PropTypes.object.isRequired,
  style: PropTypes.string,
};