import React from "react";
import {getSizeString} from "../utilities";

function ColumnStaticUploader(props) {
  const {data} = props;
  const {label, size, value} = data || {};
  return <span>{value && `${label} (${getSizeString(size)})`}</span>;
}

export default ColumnStaticUploader;