describe('awe-framework/awe-client-angular/src/test/js/services/selectorNormalizer.js', function () {
  const selectorNormalizer = require('./../../../main/resources/js/awe/services/selector/selectorNormalizer.js');
  const normalizerApi = selectorNormalizer.default || selectorNormalizer;

  it('should normalize single selector selections as a scalar round-trip', function () {
    const result = normalizerApi.normalizeSelectorLegacyModel({
      values: [{value: 0, label: 'No'}, {value: 1, label: 'Yes'}],
      selected: null
    }, {
      values: [{value: 0, label: 'No'}, {value: 1, label: 'Yes'}],
      selected: [1]
    }, {
      multiple: false,
      stringifySingle: true
    });

    expect(result.values).toEqual([{value: 0, label: 'No'}, {value: 1, label: 'Yes'}]);
    expect(result.selected).toBe('1');
  });

  it('should normalize multiple selector selections as an array round-trip', function () {
    const result = normalizerApi.normalizeSelectorLegacyModel({
      values: [],
      selected: null
    }, {
      values: [{value: 'A', label: 'Alpha'}, {value: 'B', label: 'Beta'}, {value: 'C', label: 'Gamma'}],
      selected: ['A', 'C']
    }, {
      multiple: true,
      stringifySingle: false
    });

    expect(result.values).toEqual([{value: 'A', label: 'Alpha'}, {value: 'B', label: 'Beta'}, {value: 'C', label: 'Gamma'}]);
    expect(result.selected).toEqual(['A', 'C']);
  });

  it('should normalize empty selector selections preserving values and null selection', function () {
    const result = normalizerApi.normalizeSelectorLegacyModel({
      values: [{value: 'A', label: 'Alpha'}],
      selected: 'A'
    }, {
      values: [{value: 'A', label: 'Alpha'}, {value: 'B', label: 'Beta'}],
      selected: []
    }, {
      multiple: false,
      stringifySingle: true
    });

    expect(result.values).toEqual([{value: 'A', label: 'Alpha'}, {value: 'B', label: 'Beta'}]);
    expect(result.selected).toBeNull();
  });

  it('should preserve pending selection when suggest receives only an unresolved single value', function () {
    const result = normalizerApi.normalizeSuggestLegacyModel({
      values: [{value: 'old', label: 'Old'}],
      storedValues: [{value: 'old', label: 'Old'}],
      selected: 'old',
      pendingSelection: null
    }, {
      selected: ['DjrRepPth']
    }, {
      multiple: false,
      strict: true
    });

    expect(result.values).toEqual([]);
    expect(result.selected).toBe('DjrRepPth');
    expect(result.pendingSelection).toBe('DjrRepPth');
    expect(result.shouldReload).toBeTrue();
  });

  it('should preserve pending selection array when suggest multiple receives unresolved values only', function () {
    const result = normalizerApi.normalizeSuggestLegacyModel({
      values: [{value: 'old', label: 'Old'}],
      storedValues: [{value: 'old', label: 'Old'}],
      selected: ['old'],
      pendingSelection: null
    }, {
      selected: ['A', 'B']
    }, {
      multiple: true,
      strict: true
    });

    expect(result.values).toEqual([]);
    expect(result.storedValues).toEqual([]);
    expect(result.selected).toEqual(['A', 'B']);
    expect(result.pendingSelection).toEqual(['A', 'B']);
    expect(result.shouldReload).toBeTrue();
  });

  it('should preserve previously selected suggest multiple values from stored values when adding a new option', function () {
    const result = normalizerApi.normalizeSuggestLegacyModel({
      values: [{value: 'C', label: 'Gamma'}],
      storedValues: [{value: 'A', label: 'Alpha'}, {value: 'B', label: 'Beta'}],
      selected: ['A', 'B']
    }, {
      values: [{value: 'C', label: 'Gamma'}],
      selected: ['A', 'B', 'C']
    }, {
      multiple: true,
      strict: true
    });

    expect(result.selected).toEqual(['A', 'B', 'C']);
    expect(result.pendingSelection).toBeNull();
    expect(result.shouldReload).toBeFalse();
  });

  it('should merge stored values and keep only selected suggest options', function () {
    const result = normalizerApi.filterSuggestModel({
      storedValues: [{value: 0, label: 'No'}, {value: 1, label: 'Yes'}],
      values: [{value: 1, label: 'Yes'}, {value: 2, label: 'Other'}],
      selected: [2]
    });

    expect(result.values).toEqual([{value: 2, label: 'Other'}]);
    expect(result.storedValues).toEqual([{value: 2, label: 'Other'}]);
  });

  it('should create an ad hoc option for non-strict suggest when selection is unresolved', function () {
    const result = normalizerApi.ensureSuggestVisibleOptions({
      values: [],
      selected: 'free-text'
    }, {
      strict: false
    });

    expect(result.values).toEqual([{value: 'free-text', label: 'free-text', __adHoc: true}]);
    expect(result.selected).toBe('free-text');
  });

  it('should create one ad hoc option per selected value for non-strict suggest multiple', function () {
    const result = normalizerApi.ensureSuggestVisibleOptions({
      values: [],
      selected: ['pei', 'a', 'e']
    }, {
      strict: false
    });

    expect(result.values).toEqual([
      {value: 'pei', label: 'pei', __adHoc: true},
      {value: 'a', label: 'a', __adHoc: true},
      {value: 'e', label: 'e', __adHoc: true}
    ]);
    expect(result.selected).toEqual(['pei', 'a', 'e']);
  });

  it('should preserve existing options and only add missing ad hoc values for non-strict suggest multiple', function () {
    const result = normalizerApi.ensureSuggestVisibleOptions({
      values: [{value: 'pei', label: 'pei'}],
      selected: ['pei', 'a', 'e']
    }, {
      strict: false
    });

    expect(result.values).toEqual([
      {value: 'pei', label: 'pei'},
      {value: 'a', label: 'a', __adHoc: true},
      {value: 'e', label: 'e', __adHoc: true}
    ]);
    expect(result.selected).toEqual(['pei', 'a', 'e']);
  });

  it('should prefer server-backed options over ad hoc duplicates with the same value', function () {
    const result = normalizerApi.ensureSuggestVisibleOptions({
      values: [
        {value: 'free-text', label: 'free-text', __adHoc: true},
        {value: 'free-text', label: 'Server label'}
      ],
      selected: 'free-text'
    }, {
      strict: false
    });

    expect(result.values).toEqual([{value: 'free-text', label: 'Server label'}]);
  });

  it('should prune obsolete ad hoc options that are no longer selected', function () {
    const result = normalizerApi.ensureSuggestVisibleOptions({
      values: [
        {value: 'free-text', label: 'free-text', __adHoc: true},
        {value: 'server', label: 'Server'}
      ],
      selected: 'server'
    }, {
      strict: false
    });

    expect(result.values).toEqual([{value: 'server', label: 'Server'}]);
  });

  it('should keep selected ad hoc options while pruning stale ones', function () {
    const result = normalizerApi.ensureSuggestVisibleOptions({
      values: [
        {value: 'keep', label: 'keep', __adHoc: true},
        {value: 'drop', label: 'drop', __adHoc: true},
        {value: 'server', label: 'Server'}
      ],
      selected: ['keep', 'server']
    }, {
      strict: false
    });

    expect(result.values).toEqual([
      {value: 'keep', label: 'keep', __adHoc: true},
      {value: 'server', label: 'Server'}
    ]);
  });

  it('should prune obsolete ad hoc options from stored values during suggest filtering', function () {
    const result = normalizerApi.filterSuggestModel({
      storedValues: [
        {value: 'keep', label: 'keep', __adHoc: true},
        {value: 'drop', label: 'drop', __adHoc: true}
      ],
      values: [{value: 'server', label: 'Server'}],
      selected: ['keep', 'server']
    });

    expect(result.values).toEqual([
      {value: 'keep', label: 'keep', __adHoc: true},
      {value: 'server', label: 'Server'}
    ]);
    expect(result.storedValues).toEqual([
      {value: 'keep', label: 'keep', __adHoc: true},
      {value: 'server', label: 'Server'}
    ]);
  });

  it('should not create an ad hoc option for strict suggest when selection is unresolved', function () {
    const result = normalizerApi.ensureSuggestVisibleOptions({
      values: [],
      selected: 'free-text'
    }, {
      strict: true
    });

    expect(result.values).toEqual([]);
    expect(result.selected).toBe('free-text');
  });
});
