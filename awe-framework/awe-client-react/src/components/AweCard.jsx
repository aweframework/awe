import React from "react";
import {useTranslation} from "react-i18next";
import PropTypes from "prop-types";
import "./AweCard.less";
import "./AweCardPreview.less";
import {getIconCode, getIconNode, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import {Components} from "../utilities/structure";
import {TestIds, testHook} from "../utilities/testIds";
import {useComponentState} from "../hooks/useComponentState";
import useMaximize from "../hooks/useMaximize";
import {buildSparkline} from "../utilities/sparkline";
import ColumnStatusStrip from "../columns/ColumnStatusStrip";
import {
  CARD_ARROWS,
  CARD_GAUGE_ARC,
  CARD_TARGET_OF,
  CARD_TREND_LABELS,
  cardChipTone,
  cardDeltaText,
  cardField,
  cardNumber,
  cardPills,
  cardSegments,
  cardTiles,
  cardProgress,
  cardProgressStyle,
  cardSparklineType,
  cardStack,
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

/** Style of a child of the card shown at the right of its header instead of its body (#993) */
export const CARD_HEADER_ACTIONS = "awe-card-header-actions";

/**
 * Whether a child of the card goes to its header: it has the header actions style
 * @param {object} node Child of the card
 * @return {boolean} The child goes to the header
 */
const isHeaderAction = node => typeof node.style === "string" && node.style.split(/\s+/).includes(CARD_HEADER_ACTIONS);

/**
 * Card (#899): a metric tile (title, value with unit, icon, trend delta, target progress) filled from the first row
 * of its model values over its attributes, with an optional body of child components. A card can also show a row of
 * pills under its heading (pills field), a status strip that takes its width (strip field) and a stacked bar of the
 * parts of a whole (stack field, #993). A child with the awe-card-header-actions style goes to the right of the header
 * (a view switch, buttons) and, with the maximize attribute, a button at the end of the header grows the card over its
 * maximize target and restores it, like a window (useMaximize, #993)
 */
function AweCard(props) {
  const {id, elementList = []} = props;
  const {model = {}, attributes = {}} = useComponentState(id) || {};
  const {t, i18n} = useTranslation();
  const language = i18n.language;
  const {ref: cardRef, iconMaximized, style: maximizeStyle, className: maximizeClassName, toggle: toggleMaximized} =
    useMaximize({contained: true});
  const headerActions = elementList.filter(isHeaderAction);
  const bodyElements = elementList.filter(node => !isHeaderAction(node));

  const row = (model.values || [])[0];
  const field = name => cardField(row, attributes, name);
  const text = name => {
    const value = field(name);
    return value === undefined || value === null || value === "" ? null : translateLabel(value, t);
  };

  const {style, maximize} = attributes;
  const maximizable = maximize === true || maximize === "true";
  const visible = isCardVisible(field("visible"));
  const decimals = field("numberDecimals");
  // A title may be a list of segments, the strong ones bold (#993, T37)
  const titleSegments = cardSegments(field("title"));
  const title = titleSegments
    ? titleSegments.map((segment, index) => segment.strong ? <strong key={index}>{segment.text}</strong> : segment.text)
    : text("title") || text("label");
  const subtitle = text("subtitle");
  const chipText = text("chip");
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
  const pills = cardPills(field("pills"));
  const stripValue = field("strip");
  const tiles = cardTiles(field("tiles"));
  const tilesLabel = text("tilesLabel");
  const details = text("details");
  const detailsLabel = text("detailsLabel");
  const stack = cardStack(field("stack")).map(segment => ({
    ...segment, text: `${segment.value} ${segment.label === null ? "" : translateLabel(segment.label, t)}`.trim()
  }));

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
  const chipTone = cardChipTone(field("chipTone"));
  // Status chip at the top right, before the icon or the delta (#836): its text tells the status, the dot and the colour only repeat it
  const chip = chipText ?
    <span className={`awe-card-chip awe-card-chip-${chipTone}`} data-tone={chipTone} {...testHook(TestIds.cardChip)}>
      {chipText}
    </span> : null;
  const footerDelta = icon ? delta : null;

  // The maximize button of a window: its title and accessible name tell what it does next
  const getMaximizeButton = () => {
    if (!maximizable) {
      return null;
    }
    const maximizeTitle = t(iconMaximized ? "SCREEN_TEXT_RESTORE" : "SCREEN_TEXT_MAXIMIZE");
    return <button type="button" className="maximize-button awe-card-maximize-button" title={maximizeTitle}
      aria-label={maximizeTitle} aria-pressed={iconMaximized} onClick={toggleMaximized}
      {...testHook(TestIds.cardMaximizeButton)}>
      {getIconNode(iconMaximized ? "pi-window-minimize" : "pi-window-maximize")}
    </button>;
  };

  const getHeader = () => {
    const maximizeButton = getMaximizeButton();
    if (!title && !subtitle && !chip && !headerEnd && !headerActions.length && !maximizeButton) {
      return null;
    }
    return <header className="awe-card-header">
      {(title || subtitle) && <div className="awe-card-heading">
        {title && <h3 className="awe-card-title" id={titleId}>{title}</h3>}
        {subtitle && <p className="awe-card-subtitle">{subtitle}</p>}
      </div>}
      {headerActions.map((node, index) => Components(node, index))}
      {chip}
      {headerEnd}
      {maximizeButton}
    </header>;
  };

  // Facts of the card, as pills under its heading: a code (what the application runs) in the monospace font
  const getPills = () => pills.length > 0 &&
    <ul className="awe-card-pills">
      {pills.map((pill, index) => <li key={index} className="awe-card-pill">
        {pill.icon && <span className="awe-card-pill-icon" aria-hidden="true">{getIconCode(pill.icon)}</span>}
        {pill.label && <span className="awe-card-pill-label">{translateLabel(pill.label, t)}</span>}
        {pill.label && pill.code && <span className="awe-card-pill-separator" aria-hidden="true">·</span>}
        {pill.code && <code className="awe-card-pill-code">{pill.code}</code>}
      </li>)}
    </ul>;

  // Status strip of the card: the strip of the status strip column, taller and as wide as the card
  const getStrip = () => stripValue !== undefined && stripValue !== null && stripValue !== "" &&
    <div className="awe-card-strip">
      <ColumnStatusStrip data={{value: stripValue}}/>
    </div>;

  // Tiles of the card (#993, T37): an ordered list of small boxes, named by their label above them
  const getTiles = () => tiles.length > 0 &&
    <div className="awe-card-tiles-block">
      {tilesLabel && <span className="awe-card-tiles-label" id={`${id}-tiles`}>{tilesLabel}</span>}
      <ol className="awe-card-tiles" aria-labelledby={tilesLabel ? `${id}-tiles` : undefined}>
        {tiles.map((tile, index) => <li key={index} className="awe-card-tile">
          <span className="awe-card-tile-label">{tile.label}</span>
          {tile.value && <strong className="awe-card-tile-value">{tile.value}</strong>}
          {tile.hint && <span className="awe-card-tile-hint">{tile.hint}</span>}
        </li>)}
      </ol>
    </div>;

  // Details of the card (#993, T37): a note folded under its label
  const getDetails = () => details &&
    <details className="awe-card-details">
      <summary>{detailsLabel || details}</summary>
      <p>{details}</p>
    </details>;

  // Stacked bar under the value: each segment as wide as its share, its text (count and label) told as one image
  const getStack = () => stack.length > 0 &&
    <div className="awe-card-stack" role="img" aria-label={stack.map(segment => segment.text).join(", ")}>
      {stack.map((segment, index) => <span key={index} className={`awe-card-stack-segment awe-card-stack-${segment.tone}`}
        data-tone={segment.tone} style={{flexGrow: segment.value}} title={segment.text}/>)}
    </div>;

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

  return <section id={id} ref={cardRef} className={classNames("awe-card", style, {"hidden": !visible}, maximizeClassName)}
    style={maximizeStyle} aria-labelledby={title ? titleId : undefined}
    {...testHook(TestIds.card, {owner: id, maximized: maximizable ? iconMaximized : undefined})}>
    {getHeader()}
    {getPills()}
    {getValue()}
    {getStack()}
    {getSparkline()}
    {getStrip()}
    {getTiles()}
    {bodyElements.length > 0 && <div className="awe-card-body">{bodyElements.map((node, index) => Components(node, index))}</div>}
    {getProgress()}
    {description && <p className="awe-card-description">{description}</p>}
    {getDetails()}
    {getFooter()}
  </section>;
}

AweCard.propTypes = {
  id: PropTypes.string,
  elementList: PropTypes.array
};

export default AweCard;
