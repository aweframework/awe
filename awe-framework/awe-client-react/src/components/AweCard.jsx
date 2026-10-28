import React from "react";
import {useTranslation} from "react-i18next";
import PropTypes from "prop-types";
import "./AweCard.less";
import {getIconCode, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import {Components} from "../utilities/structure";
import {TestIds, testHook} from "../utilities/testIds";
import {useComponentState} from "../hooks/useComponentState";
import {buildSparkline} from "../utilities/sparkline";
import {
  CARD_ARROWS,
  CARD_GAUGE_ARC,
  CARD_TARGET_OF,
  CARD_TREND_LABELS,
  cardDeltaText,
  cardField,
  cardNumber,
  cardProgress,
  cardProgressStyle,
  cardSparklineType,
  cardTone,
  cardTrend,
  formatCardNumber,
  isCardVisible
} from "../utilities/card";

/**
 * Converts the name of an SVG attribute of the shared sparkline renderer to its React property
 * @param {string} name Attribute name, like stroke-width
 * @return {string} React property, like strokeWidth
 */
const toProp = name => name.replace(/-([a-z])/g, (_, letter) => letter.toUpperCase());

/**
 * Card (#899): a metric tile (title, value with unit, icon, trend delta, target progress) filled from the first row
 * of its model values over its attributes, with an optional body of child components
 */
function AweCard(props) {
  const {id, elementList = []} = props;
  const {model = {}, attributes = {}} = useComponentState(id) || {};
  const {t, i18n} = useTranslation();
  const language = i18n.language;

  const row = (model.values || [])[0];
  const field = name => cardField(row, attributes, name);
  const text = name => {
    const value = field(name);
    return value === undefined || value === null || value === "" ? null : translateLabel(value, t);
  };

  const {style} = attributes;
  const visible = isCardVisible(field("visible"));
  const decimals = field("numberDecimals");
  const title = text("title") || text("label");
  const subtitle = text("subtitle");
  const icon = field("icon");
  const unit = field("unit");
  const rawValue = field("value");
  const value = cardNumber(rawValue);
  const target = cardNumber(field("target"));
  const description = text("description");
  const footerText = text("footer");
  const caption = text("caption");
  const progress = cardProgress(value, target);
  const tone = field("iconTone") || "primary";
  const sparkline = buildSparkline(field("sparkline"), {type: cardSparklineType(field("sparklineType")), color: "currentColor"});

  const deltaNumber = cardNumber(field("delta"));
  const explicitTrend = field("trend");
  const trend = cardTrend(explicitTrend, deltaNumber);
  const hasValue = rawValue !== undefined && rawValue !== null && rawValue !== "";
  const titleId = `${id}-title`;

  const getDelta = () => {
    if (!trend) {
      return null;
    }
    const tone = cardTone(field("tone"), trend, field("goodTrend"));
    const deltaText = cardDeltaText(deltaNumber, {
      language, unit: field("deltaUnit"), explicitTrend: CARD_ARROWS[explicitTrend] !== undefined
    });
    return <span className={`awe-card-delta awe-card-delta-${tone}`} data-trend={trend} data-tone={tone}
      {...testHook(TestIds.cardDelta)}>
      <span className="awe-card-delta-arrow" aria-hidden="true">{CARD_ARROWS[trend]}</span>
      <span className="awe-card-delta-text">{deltaText}</span>
      <span className="sr-only">{t(CARD_TREND_LABELS[trend])}</span>
    </span>;
  };

  const delta = getDelta();
  const iconTile = icon ?
    <span className={`awe-card-icon awe-card-icon-${tone}`} aria-hidden="true">
      {getIconCode(icon)}
    </span> : null;
  const headerEnd = icon ? iconTile : delta;
  const footerDelta = icon ? delta : null;

  const getHeader = () => {
    if (!title && !subtitle && !headerEnd) {
      return null;
    }
    return <header className="awe-card-header">
      {(title || subtitle) && <div className="awe-card-heading">
        {title && <h3 className="awe-card-title" id={titleId}>{title}</h3>}
        {subtitle && <p className="awe-card-subtitle">{subtitle}</p>}
      </div>}
      {headerEnd}
    </header>;
  };

  const getValue = () => hasValue &&
    <div className="awe-card-value" {...testHook(TestIds.cardValue)}>
      <span className="awe-card-number">{value === null ? String(rawValue) : formatCardNumber(value, language, decimals)}</span>
      {unit && <span className="awe-card-unit">{unit}</span>}
    </div>;

  // The sparkline fills the width of the card: the shapes stretch and the strokes keep their width (CSS)
  const getSparkline = () => sparkline &&
    <div className={`awe-card-sparkline awe-card-sparkline-${tone}`} {...testHook(TestIds.cardSparkline)}>
      <svg className="awe-sparkline" data-type={sparkline.type} viewBox={sparkline.viewBox} preserveAspectRatio="none"
        role="img" aria-label={sparkline.label} focusable="false">
        {sparkline.shapes.map((shape, index) => React.createElement(
          shape.tag,
          Object.fromEntries([["key", index], ...Object.entries(shape.attrs).map(([name, item]) => [toProp(name), item])]),
          shape.title === undefined ? null : <title>{shape.title}</title>
        ))}
      </svg>
    </div>;

  const progressProps = {
    role: "progressbar", "aria-valuemin": "0", "aria-valuemax": "100", "aria-valuenow": progress?.width,
    ...testHook(TestIds.cardProgress)
  };

  const getBar = () =>
    <div className={`awe-card-progress awe-card-progress-${tone}`} {...progressProps}>
      <span className="awe-card-progress-bar" style={{width: `${progress.width}%`}}/>
    </div>;

  // Half ring of the gauge: the bar is the share of the arc given by the clamped width
  const getGauge = () =>
    <div className={`awe-card-progress awe-card-gauge awe-card-progress-${tone}`} {...progressProps}>
      <svg className="awe-card-gauge-arc" viewBox="0 0 100 56" aria-hidden="true" focusable="false">
        <path className="awe-card-gauge-track" d={CARD_GAUGE_ARC} pathLength="100"/>
        <path className="awe-card-gauge-bar" d={CARD_GAUGE_ARC} pathLength="100" strokeDasharray={`${progress.width} 100`}/>
      </svg>
      <span className="awe-card-gauge-value">{`${progress.percent} %`}</span>
    </div>;

  // The gauge shows the percent in the middle of its arc, so its text below only tells the target
  const getProgress = () => {
    if (!progress) {
      return null;
    }
    const gauge = cardProgressStyle(field("progressStyle")) === "gauge";
    const targetText = `${t(CARD_TARGET_OF)} ${formatCardNumber(target, language, decimals)}${unit ? ` ${unit}` : ""}`;
    return <>
      {gauge ? getGauge() : getBar()}
      <p className="awe-card-progress-text">{gauge ? targetText : `${progress.percent} % ${targetText}`}</p>
    </>;
  };

  const getFooter = () => {
    if (!footerText && !footerDelta && !caption) {
      return null;
    }
    return <footer className="awe-card-footer">
      {footerText && <span className="awe-card-footer-text">{footerText}</span>}
      {(footerDelta || caption) && <span className="awe-card-footer-end">
        {footerDelta}
        {caption && <span className="awe-card-caption">{caption}</span>}
      </span>}
    </footer>;
  };

  return <section id={id} className={classNames("awe-card", style, {"hidden": !visible})}
    aria-labelledby={title ? titleId : undefined} {...testHook(TestIds.card, {owner: id})}>
    {getHeader()}
    {getValue()}
    {getSparkline()}
    {elementList.length > 0 && <div className="awe-card-body">{elementList.map((node, index) => Components(node, index))}</div>}
    {getProgress()}
    {description && <p className="awe-card-description">{description}</p>}
    {getFooter()}
  </section>;
}

AweCard.propTypes = {
  id: PropTypes.string,
  elementList: PropTypes.array
};

export default AweCard;
