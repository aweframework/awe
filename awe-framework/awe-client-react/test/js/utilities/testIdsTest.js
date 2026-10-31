import fs from 'fs';
import path from 'path';
import { TestAttributes, TestIds, testHook } from '../../../src/utilities/testIds';

// Vocabulary shared with the AngularJS client (single source of truth for the data-testid values)
const ANGULAR_VOCABULARY = path.resolve(
  __dirname, '../../../../awe-client-angular/src/main/resources/js/awe/data/testIds.js'
);

/**
 * Entries which only exist in the React client (PrimeReact renders parts AngularJS does not have).
 * Anything else must be identical to the AngularJS vocabulary.
 */
const REACT_ONLY_TEST_IDS = [
  'avatar', 'avatarName', 'criterionError', 'criterionUnit', 'gridRowEdit', 'selectTrigger', 'tagList', 'wizardStepNumber',
  'gridColumnsStatus', 'errorBoundary', 'errorBoundaryTitle', 'errorBoundaryMessage', 'errorBoundaryDetails', 'errorBoundaryRetry', 'errorBoundaryReload'
];
const REACT_ONLY_ATTRIBUTES = ['editing', 'deleted', 'stepNumber', 'value'];

/**
 * Read the entries of a frozen constant ("export const Name = Object.freeze({ key: "value", ... })")
 */
function readConstant(source, name) {
  const body = new RegExp(`export const ${name} = Object\\.freeze\\(\\{([\\s\\S]*?)\\}\\);`).exec(source);
  expect(body).not.toBeNull();
  const entries = {};
  const entry = /^\s*([A-Za-z]+):\s*"([^"]+)"/gm;
  let match;
  while ((match = entry.exec(body[1])) !== null) {
    entries[match[1]] = match[2];
  }
  return entries;
}

describe('awe-react-client/test/js/utilities/testIdsTest.js', () => {
  const source = fs.readFileSync(ANGULAR_VOCABULARY, 'utf8');
  const angularIds = readConstant(source, 'TestIds');
  const angularAttributes = readConstant(source, 'TestAttributes');

  it('reads the AngularJS vocabulary', () => {
    expect(Object.keys(angularIds).length).toBeGreaterThan(50);
    expect(angularIds.criterionInput).toBe('criterion-input');
    expect(angularAttributes.testId).toBe('data-testid');
  });

  it('keeps every shared test id identical to the AngularJS client', () => {
    Object.entries(TestIds)
      .filter(([key]) => !REACT_ONLY_TEST_IDS.includes(key))
      .forEach(([key, value]) => expect([key, value]).toEqual([key, angularIds[key]]));
  });

  it('keeps every shared attribute identical to the AngularJS client', () => {
    Object.entries(TestAttributes)
      .filter(([key]) => !REACT_ONLY_ATTRIBUTES.includes(key))
      .forEach(([key, value]) => expect([key, value]).toEqual([key, angularAttributes[key]]));
  });

  it('declares the React only entries explicitly and nothing else', () => {
    const reactOnly = Object.keys(TestIds).filter(key => !(key in angularIds));
    expect(reactOnly.sort()).toEqual([...REACT_ONLY_TEST_IDS].sort());
    REACT_ONLY_TEST_IDS.forEach(key => expect(Object.values(angularIds)).not.toContain(TestIds[key]));
  });

  it('declares the React only attributes explicitly and nothing else', () => {
    const reactOnly = Object.keys(TestAttributes).filter(key => !(key in angularAttributes));
    expect(reactOnly.sort()).toEqual([...REACT_ONLY_ATTRIBUTES].sort());
  });

  it('names the criterion of a chip of the filters in effect of a window, as the AngularJS client does', () => {
    expect(TestAttributes.filterId).toBe('data-filter-id');
    expect(angularAttributes.filterId).toBe('data-filter-id');
  });

  it('names the parts of the saved searches of a window, as the AngularJS client does', () => {
    const SAVED_SEARCHES = {
      windowSavedSearches: 'window-saved-searches',
      windowSavedSearchesMenu: 'window-saved-searches-menu',
      windowSavedSearchItem: 'window-saved-search-item',
      windowSavedSearchSave: 'window-saved-search-save',
      windowSavedSearchManage: 'window-saved-search-manage',
      windowSavedSearchDialog: 'window-saved-search-dialog',
      windowSavedSearchName: 'window-saved-search-name',
      windowSavedSearchProfile: 'window-saved-search-profile',
      windowSavedSearchAccept: 'window-saved-search-accept',
      windowSavedSearchCancel: 'window-saved-search-cancel',
      windowSavedSearchManageDialog: 'window-saved-search-manage-dialog',
      windowSavedSearchRow: 'window-saved-search-row',
      windowSavedSearchRename: 'window-saved-search-rename',
      windowSavedSearchRenameInput: 'window-saved-search-rename-input',
      windowSavedSearchRenameAccept: 'window-saved-search-rename-accept',
      windowSavedSearchRenameCancel: 'window-saved-search-rename-cancel',
      windowSavedSearchDelete: 'window-saved-search-delete',
      windowSavedSearchManageClose: 'window-saved-search-manage-close'
    };
    Object.entries(SAVED_SEARCHES).forEach(([key, value]) => {
      expect([key, TestIds[key]]).toEqual([key, value]);
      expect([key, angularIds[key]]).toEqual([key, value]);
    });
    expect(TestAttributes.savedSearchId).toBe('data-saved-search-id');
    expect(angularAttributes.savedSearchId).toBe('data-saved-search-id');
  });

  it('uses unique values', () => {
    const values = Object.values(TestIds);
    expect(new Set(values).size).toBe(values.length);
  });

  it('is frozen', () => {
    expect(Object.isFrozen(TestIds)).toBe(true);
    expect(Object.isFrozen(TestAttributes)).toBe(true);
  });

  describe('testHook', () => {
    it('builds the data-testid attribute', () => {
      expect(testHook(TestIds.grid)).toEqual({ 'data-testid': 'grid' });
    });

    it('adds the owner and the state attributes as strings', () => {
      expect(testHook(TestIds.selectOption, { owner: 'Sta', selected: true, disabled: false }))
        .toEqual({
          'data-testid': 'select-option',
          'data-testid-owner': 'Sta',
          'data-selected': 'true',
          'data-disabled': 'false'
        });
    });

    it('skips undefined state and owner, and keeps explicit attributes', () => {
      expect(testHook(TestIds.alert, { owner: undefined, selected: undefined, type: 'danger', attributes: { 'row-id': 'r1' } }))
        .toEqual({ 'data-testid': 'alert', 'data-type': 'danger', 'row-id': 'r1' });
    });
  });
});
