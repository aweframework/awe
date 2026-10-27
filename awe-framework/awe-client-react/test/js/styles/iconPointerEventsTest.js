const fs = require('fs');
const path = require('path');

// The Lucide svg of an icon (#885) is decorative: a click on the icon must reach the element that holds it, as it reached
// the glyph before. PrimeReact only takes a click on an element whose parent is a button, a link or an input as a click
// on that control (DomHandler.isClickable); a click whose target is the svg, or its <use>, is taken as a click on the
// grid row, and the save button of an edited row unselected the row before saving it
const MAIN_CSS = path.join(__dirname, '../../../src/main.css');

const withoutComments = css => css.replace(/\/\*[^]*?\*\//g, '');
// The declarations of the rules whose selector is exactly the selector
const rules = (css, selector) => [...withoutComments(css).matchAll(/([^{}]+)\{([^}]*)\}/g)]
  .filter(([, selectors]) => selectors.split(',').map(s => s.trim()).includes(selector))
  .map(([, , declarations]) => declarations);

describe('AWE icon pointer events (#885)', () => {
  it('lets the clicks on the svg of an icon go to the element that holds it', () => {
    const css = fs.readFileSync(MAIN_CSS, 'utf8');
    expect(rules(css, '.awe-icon').join(';')).toMatch(/pointer-events:\s*none;/);
  });
});
