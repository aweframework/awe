import React, {useCallback, useEffect, useLayoutEffect, useRef, useState} from "react";
import PropTypes from "prop-types";
import {OverlayPanel} from "primereact/overlaypanel";
import {useTranslation} from "react-i18next";
import {visibleChipCount} from "../utilities/chips";
import {TestIds, testHook} from "../utilities/testIds";
import {getIconNode} from "../utilities";
import "./AweSelectMultiple.less";

/**
 * Value of a select-multiple (#922) or a suggest-multiple with display="summary" (#845) as the chips that fit in its
 * field, and a "+K" counter that opens the list of all of them
 * @param {object[]} values Chosen values, in option order ({code, name})
 * @param {function} onRemove Called with the code of the value to remove; without it the values cannot be removed
 */
function AweSelectMultipleChips({values, onRemove}) {
  const {t} = useTranslation();
  const rowRef = useRef(null);
  const counterRef = useRef(null);
  const panelRef = useRef(null);
  const [shown, setShown] = useState(values.length);
  const [expanded, setExpanded] = useState(false);
  const hidden = Math.max(values.length - shown, 0);
  const names = values.map(value => value.name).join("\u0000");

  const measure = useCallback(() => {
    const row = rowRef.current;
    if (row) {
      const widths = Array.from(row.querySelectorAll("[data-chip]")).map(chip => chip.scrollWidth);
      const gap = parseFloat(getComputedStyle(row).columnGap) || 0;
      setShown(visibleChipCount(widths, row.clientWidth, counterRef.current?.offsetWidth ?? 0, gap));
    }
  }, []);

  // Measured again after every change of the chips, and once more when the counter appears and its width is known
  useLayoutEffect(measure, [measure, names, shown]);
  useEffect(() => {
    if (typeof ResizeObserver !== "undefined") {
      const observer = new ResizeObserver(measure);
      observer.observe(rowRef.current);
      return () => observer.disconnect();
    }
  }, [measure]);
  useEffect(() => {
    if (!hidden) {
      panelRef.current?.hide();
    }
  }, [hidden]);

  const giveFocusBack = () => {
    setExpanded(false);
    const active = document.activeElement;
    if (counterRef.current && (!active || active === document.body || panelRef.current?.getElement()?.contains(active))) {
      counterRef.current.focus();
    }
  };

  return <span ref={rowRef} className="awe-select-multiple-chips">
    {values.map((value, index) =>
      <span key={value.code} data-chip className={`awe-cell-chip${index < shown ? "" : " awe-select-multiple-chip-measure"}`}
            title={value.name} aria-hidden={index < shown ? undefined : true} {...testHook(TestIds.selectChoice)}>
        {value.name}
      </span>)}
    {hidden > 0 &&
      <button ref={counterRef} type="button" className="awe-cell-chip awe-cell-chip-more awe-select-multiple-more"
              aria-haspopup="dialog" aria-expanded={expanded} aria-label={t("SCREEN_TEXT_SELECTED_VALUES_SHOW_ALL", {count: hidden})}
              onClick={event => {
                event.stopPropagation();
                setExpanded(!expanded);
                panelRef.current.toggle(event, event.currentTarget.closest(".p-multiselect, .p-autocomplete"));
              }}>
        +{hidden}
      </button>}
    <OverlayPanel ref={panelRef} dismissable className="awe-select-multiple-panel" onHide={giveFocusBack}>
      {/* The panel is a portal: its clicks must not reach the field and open the option list */}
      <div role="presentation" onClick={event => event.stopPropagation()}>
        <ul className="awe-select-multiple-list">
          {values.map(value =>
            <li key={value.code}>
              <span title={value.name}>{value.name}</span>
              {onRemove &&
                <button type="button" aria-label={t("SCREEN_TEXT_REMOVE_VALUE", {label: value.name})}
                        onClick={() => onRemove(value.code)} {...testHook(TestIds.selectChoiceClose)}>
                  {getIconNode("pi-times", {tag: "span", "aria-hidden": "true"})}
                </button>}
            </li>)}
        </ul>
      </div>
    </OverlayPanel>
  </span>;
}

AweSelectMultipleChips.propTypes = {
  values: PropTypes.array,
  onRemove: PropTypes.func
};

export default AweSelectMultipleChips;
