import React from "react";
import ColumnTextView from "./ColumnTextView";

/**
 * Formatted text column: a text view whose label is html, sanitized with the policy of the grid cells
 * @param {object} props Column properties
 * @returns {JSX.Element} Formatted text column
 */
function ColumnFormattedText(props) {
  return <ColumnTextView {...props} html/>;
}

export default ColumnFormattedText;
