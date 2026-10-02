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
  'avatar', 'avatarName', 'criterionUnit', 'gridRowEdit', 'wizardStepNumber'
];
const REACT_ONLY_ATTRIBUTES = ['editing'];

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
