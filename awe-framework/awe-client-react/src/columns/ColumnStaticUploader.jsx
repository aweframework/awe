import React, {Component} from "react";
import {getSizeString} from "../utilities";
import PropTypes from "prop-types";

export default class ColumnStaticUploader extends Component {

  render() {
    const {data} = this.props;
    const {label, size, value} = data || {};
    return <span>{value && `${label} (${getSizeString(size)})`}</span>;
  }
}

ColumnStaticUploader.propTypes = {
  data: PropTypes.object.isRequired,
};