import React from "react";
import {useTranslation} from "react-i18next";
import {getIconNode, translateLabel} from "../utilities";
import "./ColumnTextView.less";
import "./ColumnLink.less";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";
import {useColumnActions} from "./ColumnButton";
import {getFirstDefinedValue} from "../utilities/general";
import {rowText} from "../components/AweGridCells";

/**
 * Link of a column with actions: the label of the cell, drawn as a link, launches the actions of the column on its row
 * as a button column does (click or Enter), and has no address to navigate to. Like a button column, a readonly or
 * disabled cell launches nothing and a hidden cell is not shown
 */
function ColumnActionLink({data, attrs = {}, disabled, address, actions, classes, label, title, align}) {
  const launchActions = useColumnActions(address, actions);
  const {visible = true, readonly: cellReadonly, disabled: cellDisabled} = attrs;
  const inactive = getFirstDefinedValue(cellReadonly, cellDisabled, disabled, false);
  const launch = () => {
    if (!inactive) {
      launchActions();
    }
  };
  const onKeyDown = (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
      launch();
    }
  };
  return <a
    role="link"
    tabIndex={inactive ? -1 : 0}
    aria-disabled={inactive ? "true" : undefined}
    className={classNames(classes, "column-link-action", {"hidden": !visible})}
    title={title}
    style={{textAlign: align || "left"}}
    onClick={launch}
    onKeyDown={onKeyDown}>{label || data.value}</a>;
}

ColumnActionLink.propTypes = {
  data: PropTypes.object.isRequired,
  attrs: PropTypes.object,
  disabled: PropTypes.bool,
  address: PropTypes.object.isRequired,
  actions: PropTypes.array.isRequired,
  classes: PropTypes.string,
  label: PropTypes.string,
  title: PropTypes.string,
  align: PropTypes.string
};

/**
 * Link column: without actions the value of the cell is an external address opened in a new tab, announced by a
 * decorative external-link icon and a hidden text (WCAG G201); with actions the link launches them (see
 * ColumnActionLink). As in a button column, the title of the cell, else the one of the column, is the tooltip. With a
 * secondary field, its value of the row goes in one line under the link (#993), as the secondary line of the rich cells
 */
function ColumnLink(props) {
  const {row, secondaryField} = props;
  const secondary = secondaryField ? rowText(row, secondaryField) : "";
  if (secondary === "") {
    return <ColumnLinkContent {...props}/>;
  }
  return <div className="awe-cell-lines awe-cell-link">
    <ColumnLinkContent {...props}/>
    <span className="awe-cell-secondary" title={secondary}>{secondary}</span>
  </div>;
}

/**
 * The link of a link column
 */
function ColumnLinkContent(props) {
  const { data, attrs, disabled, align, style, address, actions = [], title: columnTitle } = props;
  const { t } = useTranslation();
  const { value, label, title: cellTitle } = data;
  const classes = classNames("link", style, data?.style);
  const tooltip = translateLabel(getFirstDefinedValue(cellTitle, columnTitle), t) || undefined;
  if (actions.length > 0) {
    return <ColumnActionLink data={data} attrs={attrs} disabled={disabled} address={address} actions={actions}
                             classes={classes}
                             label={translateLabel(label, t)} title={tooltip} align={align}/>;
  }
  return <a
    href={value}
    className={classNames(classes, "column-link-external")}
    title={tooltip}
    style={{ textAlign: align || "left" }}
    target="_blank" rel="noreferrer">
    {translateLabel(label, t) || value}
    {getIconNode("lucide:external-link", {tag: "span", className: "column-link-external-icon", "aria-hidden": "true"})}
    <span className="p-hidden-accessible">{` ${t("LINK_OPENS_IN_NEW_TAB")}`}</span>
  </a>;
}

ColumnLinkContent.propTypes = {
  data: PropTypes.object.isRequired,
  address: PropTypes.object,
  attrs: PropTypes.object,
  disabled: PropTypes.bool,
  actions: PropTypes.array,
  align: PropTypes.string,
  style: PropTypes.string,
  title: PropTypes.string
};

ColumnLink.propTypes = {
  row: PropTypes.object,
  secondaryField: PropTypes.string,
  data: PropTypes.object.isRequired,
  address: PropTypes.object,
  attrs: PropTypes.object,
  disabled: PropTypes.bool,
  actions: PropTypes.array,
  align: PropTypes.string,
  style: PropTypes.string,
  title: PropTypes.string
};

export default ColumnLink;
