import {
  getSelectorOptionKey,
  includesSelectorValue,
  mergeUniqueSelectorOptions,
} from './selectorIdentity';
import {
  asSelectedArray,
  formatSelectedValues,
  getDefinedSelectedValues,
  getSelectedKeyList,
  normalizeSelectedValue,
} from './selectorSelection';
import {cloneSelectorOptionList} from './selectorModelState';

function createAdHocOption(value) {
  return {value: value, label: value, __adHoc: true};
}

function pruneObsoleteAdHocOptions(values, selected) {
  const selectedKeys = getSelectedKeyList(selected);

  return asSelectedArray(values).filter(option => !option.__adHoc || selectedKeys.includes(getSelectorOptionKey(option)));
}

export function filterSelectedValues(values, selected, multiple) {
  let filtered = [];
  let selectedMap = {};

  if (selected !== null && selected !== undefined) {
    asSelectedArray(selected).forEach(value => {
      selectedMap[String(value)] = value;
    });

    asSelectedArray(values).forEach(element => {
      let key = getSelectorOptionKey(element);
      if (key in selectedMap) {
        filtered.push(selectedMap[key]);
      }
    });
  }

  if (multiple) {
    return filtered;
  }

  if (filtered.length > 0) {
    return filtered[0];
  }

  return null;
}

export function normalizeIncomingSelected(selected, multiple) {
  return normalizeSelectedValue(selected, multiple);
}

export function normalizeSelectorLegacyModel(currentModel, incomingData, options = {}) {
  const {multiple = false, stringifySingle = false} = options;
  const normalizedModel = {...currentModel, ...incomingData};

  normalizedModel.values = 'values' in incomingData
    ? cloneSelectorOptionList(incomingData.values)
    : cloneSelectorOptionList(currentModel.values);

  if ('selected' in incomingData) {
    normalizedModel.selected = filterSelectedValues(
      normalizedModel.values,
      formatSelectedValues(incomingData.selected),
      multiple
    );

    if (!multiple && stringifySingle) {
      normalizedModel.selected = normalizedModel.selected === null ? null : String(normalizedModel.selected);
    }
  }

  return normalizedModel;
}

export function filterSuggestModel(model) {
  const normalizedModel = {
    ...model,
    values: cloneSelectorOptionList(model.values),
    storedValues: cloneSelectorOptionList(model.storedValues),
  };

  normalizedModel.values = pruneObsoleteAdHocOptions(normalizedModel.values, normalizedModel.selected);
  normalizedModel.storedValues = pruneObsoleteAdHocOptions(normalizedModel.storedValues, normalizedModel.selected);

  if (normalizedModel.selected !== null && normalizedModel.selected !== undefined) {
    const selected = getSelectedKeyList(normalizedModel.selected);
    normalizedModel.values = mergeUniqueSelectorOptions([...normalizedModel.storedValues, ...normalizedModel.values])
      .filter(element => selected.includes(getSelectorOptionKey(element)));
  }

  normalizedModel.storedValues = cloneSelectorOptionList(normalizedModel.values);
  return normalizedModel;
}

export function ensureSuggestVisibleOptions(model, options = {}) {
  const {strict = true} = options;
  const normalizedModel = {
    ...model,
    values: pruneObsoleteAdHocOptions(
      cloneSelectorOptionList(model.values),
      model.selected
    ),
  };

  if (!strict) {
    const selectedValues = getDefinedSelectedValues(normalizedModel.selected);

    selectedValues.forEach(value => {
      const exists = includesSelectorValue(normalizedModel.values, value);
      if (!exists) {
        normalizedModel.values.push(createAdHocOption(value));
      }
    });

    normalizedModel.values = mergeUniqueSelectorOptions(normalizedModel.values);
  }

  return normalizedModel;
}

export function normalizeSuggestLegacyModel(currentModel, incomingData, options = {}) {
  const {multiple = false, strict = true} = options;
  const normalizedModel = {...currentModel, ...incomingData};

  normalizedModel.values = 'values' in incomingData
    ? cloneSelectorOptionList(incomingData.values)
    : cloneSelectorOptionList(currentModel.values);

  normalizedModel.storedValues = 'storedValues' in incomingData
    ? cloneSelectorOptionList(incomingData.storedValues)
    : cloneSelectorOptionList(currentModel.storedValues);

  normalizedModel.pendingSelection = currentModel.pendingSelection || null;
  normalizedModel.shouldReload = false;

  if ('selected' in incomingData) {
    const selectableValues = mergeUniqueSelectorOptions([...normalizedModel.storedValues, ...normalizedModel.values]);
    const mappedSelected = filterSelectedValues(
      selectableValues,
      formatSelectedValues(incomingData.selected),
      multiple
    );
    const normalizedSelected = normalizeIncomingSelected(incomingData.selected, multiple);
    const unresolvedSelected = !('values' in incomingData) && asSelectedArray(normalizedSelected).length > 0 &&
      ((multiple && mappedSelected.length === 0) || (!multiple && mappedSelected === null));

    normalizedModel.selected = unresolvedSelected ? normalizedSelected : mappedSelected;
    normalizedModel.pendingSelection = unresolvedSelected ? normalizedSelected : null;
    normalizedModel.shouldReload = unresolvedSelected;

    if (unresolvedSelected) {
      normalizedModel.values = [];
      normalizedModel.storedValues = [];
    }
  }

  return ensureSuggestVisibleOptions(normalizedModel, {strict});
}

export default {
  ensureSuggestVisibleOptions,
  filterSuggestModel,
  filterSelectedValues,
  normalizeIncomingSelected,
  normalizeSelectorLegacyModel,
  normalizeSuggestLegacyModel,
};
