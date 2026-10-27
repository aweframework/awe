const fs = require('fs');
const path = require('path');
import {getVirtualItemSize} from "../../../src/utilities/suggest";

// A long (virtual) suggest list gives every option a fixed height (#885): it has to match the option of the AWE theme
// in the density of <html>, or the options of the compact density get a gap under their text
describe('Virtual suggest option (#885)', () => {
  afterEach(() => document.documentElement.removeAttribute('data-awe-density'));

  it('is as high as an option of the comfortable density (8px padding + 21px line height)', () => {
    document.documentElement.setAttribute('data-awe-density', 'comfortable');
    expect(getVirtualItemSize()).toBe(38);
  });

  it('is as high as an option of the compact density (6px padding + 19.5px line height)', () => {
    document.documentElement.setAttribute('data-awe-density', 'compact');
    expect(getVirtualItemSize()).toBe(32);
  });

  it('takes the comfortable height when <html> has no density or an unknown one', () => {
    expect(getVirtualItemSize()).toBe(38);
    document.documentElement.setAttribute('data-awe-density', 'spacious');
    expect(getVirtualItemSize()).toBe(38);
  });

  it('centres the text of the option in its fixed height', () => {
    const less = fs.readFileSync(path.join(__dirname, '../../../src/criteria/AweSuggest.less'), 'utf8');
    expect(less).toMatch(/\.p-virtualscroller \.p-autocomplete-item \{[^}]*display: flex;[^}]*align-items: center;/);
    expect(less).toMatch(/\.awe-virtual-item \{[^}]*min-width: 0;/);
  });

  it('gives the virtual scroller the height of the density', () => {
    const jsx = fs.readFileSync(path.join(__dirname, '../../../src/criteria/AweSuggest.jsx'), 'utf8');
    expect(jsx).toMatch(/itemSize: getVirtualItemSize\(\)/);
  });
});
