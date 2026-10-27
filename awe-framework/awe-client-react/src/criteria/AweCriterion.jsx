import React from 'react';
import {classNames} from "../utilities/components";
import {formatMessage, getHelpTooltipNode, getIconCode, getIconNode, translateLabel} from "../utilities";
import {useTranslation} from "react-i18next";
import {useSelector} from "react-redux";
import {selectSettings} from "../redux/selectors/stateSelectors";
import "./AweCriterion.less";
import {TestIds, testHook} from "../utilities/testIds";
import PropTypes from "prop-types";
import {CriterionSkeleton} from "../components/AweSkeleton";
import {useContextMenu} from "../hooks/useContextMenu";

// Pixels added to the width of a character by the size of the criterion
const SIZE_CHAR_INCREMENT = {md: 1, lg: 3};

const AweCriterion = ({children, note, address, attributes, validationRules,
                        generateLabel = true, generateIcon = true,
                        generateUnit = true, groupClass = "p-inputgroup", skeletonKind = "field"}) => {
  const {label, style, help, helpImage, icon, unit, size, error, visible, readonly, leftLabel} = attributes || {};
  const {t} = useTranslation();
  const settings = useSelector(selectSettings);
  const {onContextMenu, contextMenuTemplate} = useContextMenu({address, contextMenu: attributes?.contextMenu});

  // Label on the left: its width is the number of characters of the contract by the pixels of a character of the size
  const leftChars = Number.parseInt(leftLabel, 10);
  const hasLeftLabel = leftChars > 0;
  const charSize = (settings.pixelsPerCharacter ?? 7) + (SIZE_CHAR_INCREMENT[size] ?? 0);
  const labelWidth = hasLeftLabel ? {width: `${leftChars * charSize}px`} : {};

  const getHelpIcon = () => {
    if (help || helpImage) {
      return getIconNode("pi-question-circle", {role: "icon", className: "help-icon"});
    }
    return null;
  };

  const getLabel = () => {
    if (style?.includes("no-label")) {
      return null;
    } else if (label) {
      return (
        <>
          {getHelpTooltipNode(help, helpImage, t, `.help-target-${address.component}`)}
          <label
            htmlFor={address.component}
            className={`block help-target-${address.component}`}
            data-pr-position="bottom"
            data-pr-at="left+6 bottom"
            style={{...(help || helpImage ? {cursor: "pointer"} : {}), ...labelWidth}}
            data-pr-showdelay={settings.helpTimeout}
          >
            {getHelpIcon()}{translateLabel(label, t)}
          </label>
        </>
      );
    } else {
      return <div className="block" style={labelWidth}>&nbsp;</div>;
    }
  };

  const getIcon = () => {
    return icon ? <span className="p-inputgroup-addon">{getIconCode(icon)}</span> : null;
  };

  const getUnit = () => {
    if (unit) {
      return (
        <span className={classNames("p-inputgroup-addon", {[`text-${size}`]: size, [`p-inputtext-${size}`]: size})}
              {...testHook(TestIds.criterionUnit)}>
          {translateLabel(unit, t)}
        </span>
      );
    }
    return null;
  };

  const getValidation = () => {
    if (error) {
      return (
        <small className="p-invalid" {...testHook(TestIds.criterionError)}>
          {getIconNode("lucide:circle-alert", {className: "criterion-error-icon", "aria-hidden": "true"})}
          {formatMessage(error, t)}
        </small>
      );
    }
    return null;
  };

  // Not registered yet: a skeleton with the shape of the criterion takes its place
  if (!address?.component) {
    return <CriterionSkeleton attributes={attributes} kind={skeletonKind} generateLabel={generateLabel}/>;
  }

  // Component classes
  const classes = classNames("field", {
    invisible: attributes.invisible,
    "hidden": !visible,
    "p-disabled": readonly,
    "required": validationRules?.required,
    "col-12": !style,
    "label-left": hasLeftLabel
  }, style);

  return (
    <div className={classes} criterion-id={address.component} onContextMenu={onContextMenu}>
      {generateLabel && getLabel()}
      {!generateLabel && getHelpTooltipNode(help, helpImage, t, `.help-target-${address.component}`)}
      <div className={classNames(groupClass, {[`help-target-${address.component}`]: !generateLabel && (help || helpImage)})}
           style={{position: "relative"}}
           data-pr-position="bottom" data-pr-at="left+6 bottom" data-pr-showdelay={settings.helpTimeout}>
        {generateIcon && getIcon()}
        {children}
        {generateUnit && getUnit()}
      </div>
      {getValidation()}
      {note}
      {contextMenuTemplate()}
    </div>
  );
};

AweCriterion.propTypes = {
  children: PropTypes.node,
  // Shown under the field (a help or a warning of the criterion)
  note: PropTypes.node,
  address: PropTypes.object,
  attributes: PropTypes.object,
  validationRules: PropTypes.object,
  generateLabel: PropTypes.bool,
  generateIcon: PropTypes.bool,
  generateUnit: PropTypes.bool,
  groupClass: PropTypes.string,
  skeletonKind: PropTypes.string
};

export default AweCriterion;
