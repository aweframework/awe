import React from "react";
import {ProgressBar} from "primereact/progressbar";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";

function ColumnProgress(props) {
  const { data, style } = props;
  const { value } = data;
  const classes = classNames(style, data?.style, "column-progress");
  return <ProgressBar className={classes} style={{ width: "100%" }} value={value || 0} />;
}

ColumnProgress.propTypes = {
  data: PropTypes.object.isRequired,
  style: PropTypes.string
};

export default ColumnProgress;
