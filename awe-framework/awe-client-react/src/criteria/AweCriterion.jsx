import React from 'react';
import {classNames} from "../utilities/components";
import {formatMessage, getHelpTooltipNode, getIconCode, translateLabel} from "../utilities";
import {useTranslation} from "react-i18next";
import {useSelector} from "react-redux";
import "./AweCriterion.less";
import {TestIds, testHook} from "../utilities/testIds";
import PropTypes from "prop-types";

const AweCriterion = ({children, address, attributes, validationRules,
                        generateLabel = true, generateIcon = true,
                        generateUnit = true, groupClass = "p-inputgroup"}) => {
  const {label, style, help, helpImage, icon, unit, size, error, visible, readonly} = attributes || {};
  const {t} = useTranslation();
  const {settings} = useSelector((state) => ({settings: state.settings}));

  const getHelpIcon = () => {
    if (help || helpImage) {
      return <i role="icon" className={`help-icon pi pi-question-circle`} />;
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
            style={help || helpImage ? {cursor: "pointer"} : {}}
            data-pr-showdelay={settings.helpTimeout}
          >
            {getHelpIcon()}{translateLabel(label, t)}
          </label>
        </>
      );
    } else {
      return <div className="block mb-2">&nbsp;</div>;
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
      return <small className="p-invalid">{formatMessage(error, t)}</small>;
    }
    return null;
  };

  // Component classes
  const classes = classNames("field", {
    invisible: attributes.invisible,
    "hidden": !visible,
    "p-disabled": readonly,
    "required": validationRules?.required,
    "col-12": !style,
  }, style);

  return (
    <div className={classes} criterion-id={address.component}>
      {generateLabel && getLabel()}
      <div className={classNames(groupClass)} style={{position: "relative"}}>
        {generateIcon && getIcon()}
        {children}
        {generateUnit && getUnit()}
      </div>
      {getValidation()}
    </div>
  );
};

AweCriterion.propTypes = {
  children: PropTypes.node,
  address: PropTypes.object,
  attributes: PropTypes.object,
  validationRules: PropTypes.object,
  generateLabel: PropTypes.bool,
  generateIcon: PropTypes.bool,
  generateUnit: PropTypes.bool,
  groupClass: PropTypes.string
};

export default AweCriterion;
