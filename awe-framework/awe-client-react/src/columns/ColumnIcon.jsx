import React from "react";
import {useTranslation} from "react-i18next";
import {classNames} from "../utilities/components";
import {TestAttributes, TestIds, testHook} from "../utilities/testIds";
import {getIconCode, translateLabel} from "../utilities";
import PropTypes from "prop-types";

const TONE = /(?:^|\s)bg-(default|primary|success|info|warning|danger)(?=\s|$)/;

/**
 * Tone of a legacy icon cell: a bg-<tone> style with an inverse (white) glyph, which Bootstrap painted as a badge. Without
 * Bootstrap nothing paints the background, so the glyph stays white on white; the tone makes it a badge of the design system
 * @param {string} icon Icon value
 * @param {string} classes Classes of the icon
 * @returns {string|undefined} Tone
 */
function getLegacyTone(icon, classes) {
  return /(?:^|\s)fa-inverse(?=\s|$)/.test(icon || "") ? TONE.exec(classes)?.[1] : undefined;
}

function ColumnIcon(props) {
  const {t} = useTranslation();
  const {data} = props;
  const {icon, style, title, label} = data;
  const tone = getLegacyTone(icon, classNames(style, data?.style));
  const classes = classNames(style, data?.style, tone && ["awe-icon-badge", `awe-icon-badge-${tone}`]);

  return <span className="icon-container" title={translateLabel(title || label, t)}
    {...testHook(TestIds.columnIcon, {attributes: {[TestAttributes.icon]: icon}})}>{getIconCode(icon, classes)}</span>;
}

ColumnIcon.propTypes = {
  data: PropTypes.oneOfType([PropTypes.object, PropTypes.array]),
};

export default ColumnIcon;