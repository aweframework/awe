import {asSelectedArray} from './selectorSelection';

export function cloneSelectorOption(option) {
  return option !== null && typeof option === 'object' ? {...option} : option;
}

export function cloneSelectorOptionList(values) {
  return asSelectedArray(values).map(cloneSelectorOption);
}

export function cloneSelectorModelSlice(model, keys) {
  return keys.reduce((result, key) => {
    if (!(key in model)) {
      return result;
    }

    const value = model[key];
    result[key] = Array.isArray(value)
      ? value.map(cloneSelectorOption)
      : cloneSelectorOption(value);
    return result;
  }, {});
}

export function syncSelectorOptionState(model, nextState) {
  const clonedState = cloneSelectorModelSlice(nextState, ['values', 'storedValues']);

  if ('values' in clonedState) {
    model.values = clonedState.values;
  }

  if ('storedValues' in clonedState) {
    model.storedValues = clonedState.storedValues;
  }
}

export function applySelectorModelSlice(model, nextState) {
  Object.keys(nextState).forEach(key => {
    const value = nextState[key];

    if (key === 'values' || key === 'storedValues') {
      model[key] = cloneSelectorOptionList(value);
      return;
    }

    if (Array.isArray(value)) {
      model[key] = value.slice();
      return;
    }

    if (value !== null && typeof value === 'object') {
      model[key] = {...value};
      return;
    }

    model[key] = value;
  });
}
