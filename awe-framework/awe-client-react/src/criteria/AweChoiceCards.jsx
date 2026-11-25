import React, {useCallback, useId, useRef} from "react";
import PropTypes from "prop-types";
import {useTranslation} from "react-i18next";
import {classNames} from "../utilities/components";
import {getIconCode, translateLabel} from "../utilities";
import {TestIds, testHook} from "../utilities/testIds";
import "./AweChoiceCards.less";

// Keys that move the choice of a group of radios, with the step they take
const KEY_STEP = {ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1};

/**
 * Options of a button-radio criterion drawn as cards (style="awe-choice-cards", #993): a radio group whose radios are
 * cards with the icon, the label and the description of their option, and a tick on the chosen one. As a group of
 * radios, only the chosen card (or the first one) is in the tab order, the arrow keys move the choice and Space or
 * Enter choose the focused card
 * @param {object} props Options, chosen value, change handler, accessible name and read-only flag
 * @returns {JSX.Element} Radio group of cards
 */
function AweChoiceCards({options, value, onChange, label, readonly, id}) {
  const {t} = useTranslation();
  const prefix = useId();
  const cards = useRef([]);
  const chosen = options.findIndex(option => String(option.value) === String(value));
  const focusable = chosen >= 0 ? chosen : 0;

  const choose = useCallback((index) => {
    if (!readonly && options[index] && String(options[index].value) !== String(value)) {
      onChange({value: options[index].value});
    }
  }, [readonly, options, value, onChange]);

  const onKeyDown = useCallback((event, index) => {
    const step = KEY_STEP[event.key];
    if (step && !readonly && options.length > 1) {
      event.preventDefault();
      const next = (index + step + options.length) % options.length;
      choose(next);
      cards.current[next]?.focus();
    } else if (event.key === " " || event.key === "Enter") {
      event.preventDefault();
      choose(index);
    }
  }, [readonly, options, choose]);

  return (
    <div role="radiogroup" aria-label={label} id={id} className="awe-choice-cards-group">
      {options.map((option, index) => {
        const checked = index === chosen;
        const titleId = `${prefix}-${index}-title`;
        const descriptionId = `${prefix}-${index}-description`;
        return (
          <button
            key={String(option.value)}
            ref={node => { cards.current[index] = node; }}
            type="button"
            role="radio"
            aria-checked={checked}
            aria-disabled={readonly || undefined}
            aria-labelledby={titleId}
            aria-describedby={option.description ? descriptionId : undefined}
            tabIndex={index === focusable ? 0 : -1}
            option-id={option.value}
            className={classNames("awe-choice", {"awe-choice-checked": checked})}
            onClick={() => choose(index)}
            onKeyDown={event => onKeyDown(event, index)}
            {...testHook(TestIds.criterionInput, {selected: checked})}
          >
            <span className="awe-choice-head">
              {option.icon && <span className="awe-choice-icon" aria-hidden="true">{getIconCode(option.icon)}</span>}
              <span className="awe-choice-title" id={titleId}>{translateLabel(option.label, t)}</span>
            </span>
            {option.description &&
              <span className="awe-choice-description" id={descriptionId}>{translateLabel(option.description, t)}</span>}
            <span className="awe-choice-tick" aria-hidden="true">{getIconCode("lucide:check")}</span>
          </button>
        );
      })}
    </div>
  );
}

AweChoiceCards.propTypes = {
  options: PropTypes.array,
  value: PropTypes.any,
  onChange: PropTypes.func,
  label: PropTypes.string,
  readonly: PropTypes.bool,
  id: PropTypes.string
};

export default AweChoiceCards;
