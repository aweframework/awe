import { replaceWildcard } from '../../../src/utilities';

describe('awe-react-client/test/js/utilities/replaceWildcardTest.js', () => {
  it('replaces every [id] wildcard ignoring case', () => {
    expect(replaceWildcard('A[one]-[ONE]-[two]', { one: 1, two: 'b' })).toBe('A1-1-b');
  });

  it('keeps unknown wildcards', () => {
    expect(replaceWildcard('Save[other]', { one: 1 })).toBe('Save[other]');
  });

  it('returns null for an empty text', () => {
    expect(replaceWildcard(undefined, { one: 1 })).toBeNull();
    expect(replaceWildcard(null, {})).toBeNull();
  });
});
