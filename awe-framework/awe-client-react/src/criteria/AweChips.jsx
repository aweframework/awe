import React, {useCallback, useRef} from "react";
import PropTypes from "prop-types";
import {useTranslation} from "react-i18next";
import {classNames} from "../utilities/components";
import {getIconCode, translateLabel} from "../utilities";
import {TestIds, testHook} from "../utilities/testIds";
import "./AweChips.less";

// Keys that move the choice of a group of radios, with the step they take
const KEY_STEP = {ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1};

/**
 * Options of a criterion drawn as chips (#993, T37): pills that wrap, the chosen ones in the primary colour.
 * - radio (button-radio with style="awe-chips"): a radio group, a tick before the chosen label; only the chosen chip
 *   (or the first one) is in the tab order, the arrow keys move the choice and Space or Enter choose the focused chip.
 * - toggle (select-multiple with display="toggles"): a group of toggle buttons (aria-pressed), each one in the tab order,
 *   Space or Enter toggle it.
 * The title of an option (its full name, e.g. «Monday» for «M») is its accessible name and its tooltip
 * @param {object} props Options, chosen values, change handler, mode, accessible name and read-only flag
 * @returns {JSX.Element} Group of chips
 */
function AweChips({options, selected, onChange, mode, label, readonly, id}) {
  const {t} = useTranslation();
  const chips = useRef([]);
  const radio = mode === "radio";
  const isChecked = option => selected.some(value => String(value) === String(option.value));
  const chosen = options.findIndex(isChecked);
  const focusable = chosen >= 0 ? chosen : 0;

  const choose = useCallback((index) => {
    const option = options[index];
    if (readonly || !option) {
      return;
    }
    if (radio) {
      if (!selected.some(value => String(value) === String(option.value))) {
        onChange([option.value]);
      }
    } else {
      const pressed = selected.some(value => String(value) === String(option.value));
      onChange(pressed ? selected.filter(value => String(value) !== String(option.value)) : [...selected, option.value]);
    }
  }, [readonly, options, radio, selected, onChange]);

  const onKeyDown = useCallback((event, index) => {
    const step = KEY_STEP[event.key];
    if (radio && step && !readonly && options.length > 1) {
      event.preventDefault();
      const next = (index + step + options.length) % options.length;
      choose(next);
      chips.current[next]?.focus();
    } else if (event.key === " " || event.key === "Enter") {
      event.preventDefault();
      choose(index);
    }
  }, [radio, readonly, options, choose]);

  return (
    <div role={radio ? "radiogroup" : "group"} aria-label={label} id={id} className="awe-chips-group">
      {options.map((option, index) => {
        const checked = isChecked(option);
        const title = option.title ? translateLabel(option.title, t) : undefined;
        return (
          <button
            key={String(option.value)}
            ref={node => { chips.current[index] = node; }}
            type="button"
            role={radio ? "radio" : undefined}
            aria-checked={radio ? checked : undefined}
            aria-pressed={radio ? undefined : checked}
            aria-disabled={readonly || undefined}
            aria-label={title}
            title={title}
            tabIndex={!radio || index === focusable ? 0 : -1}
            option-id={option.value}
            className={classNames("awe-chip", {"awe-chip-checked": checked})}
            onClick={() => choose(index)}
            onKeyDown={event => onKeyDown(event, index)}
            {...testHook(TestIds.criterionInput, {selected: checked})}
          >
            {radio && checked && <span className="awe-chip-tick" aria-hidden="true">{getIconCode("lucide:check")}</span>}
            <span className="awe-chip-label">{translateLabel(option.label, t)}</span>
          </button>
        );
      })}
    </div>
  );
}

AweChips.propTypes = {
  options: PropTypes.array,
  selected: PropTypes.array,
  onChange: PropTypes.func,
  mode: PropTypes.oneOf(["radio", "toggle"]),
  label: PropTypes.string,
  readonly: PropTypes.bool,
  id: PropTypes.string
};

export default AweChips;
