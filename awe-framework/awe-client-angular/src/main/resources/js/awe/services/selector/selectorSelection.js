import {getSelectorOptionKey} from './selectorIdentity';

export function asSelectedArray(value) {
  if (value === null || value === undefined) {
    return [];
  }

  return Array.isArray(value) ? value : [value];
}

export function formatSelectedValues(values) {
  return asSelectedArray(values).map(value => {
    if (value && typeof value === 'object' && 'value' in value) {
      return value.value;
    }
    return value;
  });
}

export function getSelectedValueList(values) {
  return formatSelectedValues(values);
}

export function getSelectedKeyList(values) {
  return getSelectedValueList(values).map(getSelectorOptionKey);
}

export function normalizeSelectedValue(selected, multiple) {
  if (selected === null || selected === undefined) {
    return null;
  }

  const normalized = getSelectedValueList(selected);
  if (multiple) {
    return normalized;
  }

  if (normalized.length > 0) {
    return normalized[0];
  }

  return null;
}

export function getDefinedSelectedValues(values) {
  return getSelectedValueList(values).filter(value => value !== null && value !== undefined && value !== '');
}
