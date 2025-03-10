import React, {Component} from "react";

export default class ColumnStaticColor extends Component {

  render() {
    const {data} = this.props;
    const {value} = data || {};
    return <>
      <span>{value}</span>
      {value && <span className={"colorpicker ml-2 " + (value ? "" : "no-color")} style={{backgroundColor: value || null}}/>}
    </>
  }
}
