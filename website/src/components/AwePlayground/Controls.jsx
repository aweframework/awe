import React, {useEffect, useId, useRef, useState} from 'react';
import clsx from 'clsx';
import useBaseUrl from '@docusaurus/useBaseUrl';
import styles from './styles.module.css';
import {spriteId} from './iconNames';

const optionOf = option => (typeof option === 'object' ? option : {value: option, label: option});

/** Radio group drawn as a segmented control: arrow keys move the choice (native radios). */
function Segmented({control, value, onChange, label, optionLabel}) {
  const name = useId();
  return (
    <div className={styles.segmented} role="radiogroup" aria-label={label}>
      {control.options.map(optionOf).map(option => (
        <label key={option.value} className={styles.segment}>
          <input
            type="radio"
            name={name}
            className={styles.visuallyHidden}
            checked={value === option.value}
            onChange={() => onChange(option.value)}
          />
          <span>{optionLabel(option)}</span>
        </label>
      ))}
    </div>
  );
}

/**
 * Choice among many options, as a select-only combobox with a listbox popup (WAI-ARIA pattern; the system select popup
 * is not used). Keys on the button: arrows move, Home/End jump, Enter or Space open and choose, Escape closes.
 */
function Choice({control, value, onChange, label, optionLabel}) {
  const id = useId();
  const listId = `${id}-list`;
  const options = control.options.map(optionOf);
  const selected = Math.max(options.findIndex(option => option.value === value), 0);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(selected);
  const root = useRef(null);

  const show = () => {
    setActive(selected);
    setOpen(true);
  };
  const choose = index => {
    onChange(options[index].value);
    setOpen(false);
  };
  const move = index => setActive(Math.min(Math.max(index, 0), options.length - 1));

  // A press outside closes the list
  useEffect(() => {
    if (!open) return undefined;
    const close = event => root.current && !root.current.contains(event.target) && setOpen(false);
    document.addEventListener('pointerdown', close);
    return () => document.removeEventListener('pointerdown', close);
  }, [open]);

  // The active option stays in view while the arrows move through a long list
  useEffect(() => {
    if (open) document.getElementById(`${id}-${active}`)?.scrollIntoView?.({block: 'nearest'});
  }, [open, active, id]);

  const onKeyDown = event => {
    const keys = {
      ArrowDown: () => (open ? move(active + 1) : show()),
      ArrowUp: () => (open ? move(active - 1) : show()),
      Home: () => (open ? move(0) : show()),
      End: () => (open ? move(options.length - 1) : show()),
      Enter: () => (open ? choose(active) : show()),
      ' ': () => (open ? choose(active) : show()),
      Escape: () => open && setOpen(false),
    };
    if (!keys[event.key]) return;
    event.preventDefault();
    keys[event.key]();
  };

  return (
    <div className={styles.choice} ref={root}>
      <button
        type="button"
        role="combobox"
        className={styles.choiceButton}
        aria-label={label}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        aria-activedescendant={open ? `${id}-${active}` : undefined}
        onClick={() => (open ? setOpen(false) : show())}
        onKeyDown={onKeyDown}
      >
        <span>{optionLabel(options[selected])}</span>
        <svg viewBox="0 0 24 24" aria-hidden="true" className={styles.choiceChevron}><path d="m6 9 6 6 6-6" /></svg>
      </button>
      {open && (
        <ul id={listId} role="listbox" aria-label={label} className={styles.choiceList}>
          {options.map((option, index) => (
            <li
              key={option.value}
              id={`${id}-${index}`}
              role="option"
              aria-selected={index === selected}
              className={clsx(styles.choiceOption, index === active && styles.choiceActive, index === selected && styles.choiceSelected)}
              onPointerEnter={() => setActive(index)}
              onPointerDown={event => event.preventDefault()}
              onClick={() => choose(index)}
            >
              {optionLabel(option)}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function Switch({value, onChange, label}) {
  return (
    <label className={styles.switch}>
      <input type="checkbox" role="switch" className={styles.visuallyHidden} checked={value} onChange={event => onChange(event.target.checked)} />
      <span className={styles.track} aria-hidden="true"><span className={styles.thumb} /></span>
      <span className={styles.switchText}>{label}</span>
    </label>
  );
}

function Text({value, onChange, label}) {
  return <input type="text" className={styles.text} aria-label={label} value={value} onChange={event => onChange(event.target.value)} spellCheck={false} />;
}

/** Icons of the AWE sprite (the same one the client uses), as a radio group. */
function IconPicker({control, value, onChange, label, noneLabel}) {
  const name = useId();
  const sprite = useBaseUrl('/icons/lucide-sprite.svg');
  return (
    <div className={styles.icons} role="radiogroup" aria-label={label}>
      {control.options.map(option => (
        <label key={option} className={styles.iconOption} title={option.replace(/^lucide:/, '') || noneLabel}>
          <input type="radio" name={name} className={styles.visuallyHidden} checked={value === option} onChange={() => onChange(option)} />
          <span className={styles.iconFace}>
            {option ? (
              <svg viewBox="0 0 24 24" aria-hidden="true"><use href={`${sprite}#${spriteId(option)}`} /></svg>
            ) : (
              <span aria-hidden="true">&#8709;</span>
            )}
            <span className={styles.visuallyHidden}>{option.replace(/^lucide:/, '') || noneLabel}</span>
          </span>
        </label>
      ))}
    </div>
  );
}

const WIDGETS = {segmented: Segmented, select: Choice, switch: Switch, text: Text, icon: IconPicker};

/**
 * One control of the spec. Text of the control comes from `label` / `optionLabel` (already translated).
 */
export default function Control({control, value, onChange, label, optionLabel, noneLabel}) {
  const Widget = WIDGETS[control.type];
  const isSwitch = control.type === 'switch';
  return (
    <div className={clsx(styles.control, isSwitch && styles.controlSwitch)}>
      {!isSwitch && <span className={styles.controlLabel} aria-hidden="true">{label}</span>}
      <Widget control={control} value={value} onChange={onChange} label={label} optionLabel={optionLabel} noneLabel={noneLabel} />
    </div>
  );
}
