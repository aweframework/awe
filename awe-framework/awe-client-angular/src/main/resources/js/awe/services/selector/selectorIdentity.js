export function getSelectorOptionKey(option) {
  if (option && typeof option === 'object' && 'value' in option) {
    return String(option.value);
  }

  return String(option);
}

export function selectorValueEquals(left, right) {
  return getSelectorOptionKey(left) === getSelectorOptionKey(right);
}

export function includesSelectorValue(values, target) {
  return values.some(value => selectorValueEquals(value, target));
}

export function mergeUniqueSelectorOptions(values) {
  return values.reduce((prev, option) => {
    const existingIndex = prev.findIndex(current => selectorValueEquals(current, option));

    if (existingIndex === -1) {
      return [...prev, option];
    }

    const existing = prev[existingIndex];
    if (existing.__adHoc && !option.__adHoc) {
      return [
        ...prev.slice(0, existingIndex),
        option,
        ...prev.slice(existingIndex + 1)
      ];
    }

    return prev;
  }, []);
}
