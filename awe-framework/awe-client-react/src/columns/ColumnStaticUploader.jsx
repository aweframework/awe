import React from "react";
import {getSizeString} from "../utilities";
import PropTypes from "prop-types";

function ColumnStaticUploader(props) {
  const {data} = props;
  const {label, size, value} = data || {};
  return <span>{value && `${label} (${getSizeString(size)})`}</span>;
}

ColumnStaticUploader.propTypes = {
  data: PropTypes.oneOfType([PropTypes.object, PropTypes.array]),
};

export default ColumnStaticUploader;