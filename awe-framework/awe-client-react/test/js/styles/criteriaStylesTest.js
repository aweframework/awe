const fs = require('fs');
const path = require('path');

// The AWE 5 criteria of the React engine (#885): label above the field, field, states and message styled with the
// --awe-* tokens over the PrimeReact markup of the criteria (the screen XML and the test hooks do not change)
const CRITERIA = path.join(__dirname, '../../../src/criteria');
const CRITERIA_STYLES = fs.readdirSync(CRITERIA).filter(file => file.endsWith('.less')).sort();

const read = file => fs.readFileSync(path.join(CRITERIA, file), 'utf8');
const withoutComments = css => css.replace(/\/\*[^]*?\*\//g, '').replace(/^\s*\/\/.*$/gm, '');
const escape = selector => selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
// The declarations of the first rule whose selector list holds the selector
const rule = (css, selector) => {
  const match = withoutComments(css).match(new RegExp(`(?:^|[},])\\s*(?:[^{}]*,\\s*)?${escape(selector)}\\s*(?:,[^{}]*)?\\{([^}]*)\\}`, 'm'));
  return match ? match[1] : null;
};

describe('AWE 5 criteria styles (#885)', () => {
  const criterion = read('AweCriterion.less');

  it('finds the styles of the criteria', () => {
    expect(CRITERIA_STYLES).toEqual(expect.arrayContaining(['AweCriterion.less', 'AweInputColor.less',
      'AweInputDateRange.less', 'AweInputNumeric.less', 'AweInputUploader.less', 'AweSuggest.less']));
  });

  it('draws the clear icon of a select smaller than its arrow and muted, so it does not read as a second arrow', () => {
    const clear = rule(criterion, '.p-dropdown .p-dropdown-clear-icon');
    expect(clear).toMatch(/width: var\(--awe-size-icon-sm\);/);
    expect(clear).toMatch(/height: var\(--awe-size-icon-sm\);/);
    expect(clear).toMatch(/color: var\(--awe-color-field-icon\);/);
  });

  it.each(CRITERIA_STYLES)('styles %s with tokens only: no colour literals, sizes or mode selectors', file => {
    // The breakpoints of the column grid are LESS variables of the media queries (a media query cannot read a token)
    const css = withoutComments(read(file)).replace(/@media[^{]*/g, '').replace(/^@(sm|md|lg|xl):[^;]*;/gm, '');
    expect(css).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
    expect(css).not.toMatch(/\b(rgba?|hsla?|oklch|color-mix)\(/);
    expect(css).not.toMatch(/:\s*(white|black)\b/);
    // Only hairlines are written in px: every other size comes from a token
    expect(css.match(/\b(?!1px\b)\d+(\.\d+)?px\b/g)).toBeNull();
    expect(css).not.toMatch(/\d(\.\d+)?(rem|em|ms|s)\b/);
    expect(css).not.toMatch(/\.dark\b|\.light\b|data-awe-density|prefers-color-scheme|layout-(dark|light)/);
    (css.match(/var\(--[\w-]+/g) || []).forEach(variable => expect(variable).toMatch(/^var\(--(awe-|_)/));
    expect(css).not.toMatch(/var\(--awe-palette-/);
  });

  it('spaces the criteria with the form gaps of the density', () => {
    expect(criterion).toMatch(/@gutter: ~"calc\(var\(--awe-gap-form-row\) \/ 2\) calc\(var\(--awe-gap-form-column\) \/ 2\)";/);
    expect(rule(criterion, '.field')).toMatch(/margin-bottom: 0;/);
  });

  it('draws the label above the field in the label style, with the required mark and the help icon', () => {
    const label = rule(criterion, '.field > label');
    expect(label).toMatch(/margin: 0 0 var\(--awe-gap-label\);/);
    expect(label).toMatch(/font-size: var\(--awe-font-size-label\);/);
    expect(label).toMatch(/font-weight: var\(--awe-font-weight-medium\);/);
    expect(label).toMatch(/line-height: var\(--awe-line-height-snug\);/);
    expect(label).toMatch(/color: var\(--awe-color-field-label\);/);
    // The empty label of a criterion without label keeps the height of a label
    expect(rule(criterion, '.field > div.block')).toBe(label);
    expect(rule(criterion, '.required label:after')).toMatch(/color: var\(--awe-color-required\);/);
    expect(rule(criterion, '.help-icon')).toMatch(/color: var\(--awe-color-field-help\);/);
  });

  it('gives the fields the control height of the density', () => {
    expect(rule(criterion, '.field .p-inputtext:not(textarea):not(.p-dropdown-label)')).toMatch(/min-height: var\(--awe-size-control\);/);
    expect(rule(criterion, '.field .p-dropdown')).toMatch(/min-height: var\(--awe-size-control\);/);
    expect(rule(criterion, '.field .p-inputtext-sm.p-inputtext')).toMatch(/min-height: var\(--awe-size-control-sm\);/);
    expect(rule(criterion, '.field .p-inputtext-lg.p-inputtext')).toMatch(/min-height: var\(--awe-size-control-lg\);/);
    // The label of a select field keeps no vertical padding of its own: the field takes the control height, as the
    // text fields do, instead of growing with the line height of the page
    ['.field .p-dropdown .p-dropdown-label', '.field .p-multiselect .p-multiselect-label'].forEach(selector => {
      const label = rule(criterion, selector);
      expect(label).toMatch(/display: flex;/);
      expect(label).toMatch(/align-items: center;/);
      expect(label).toMatch(/padding-top: 0;/);
      expect(label).toMatch(/padding-bottom: 0;/);
    });
  });

  it('marks an invalid field: danger halo on focus, invalid placeholder and the message with its icon', () => {
    const focus = rule(criterion, '.field .p-inputtext.p-invalid:enabled:focus');
    expect(focus).toMatch(/border-color: var\(--awe-color-danger\);/);
    expect(focus).toMatch(/box-shadow: var\(--awe-shadow-field-invalid\);/);
    expect(rule(criterion, '.field .p-dropdown.p-invalid.p-focus')).toBe(focus);
    expect(rule(criterion, '.field .p-invalid::placeholder')).toMatch(/color: var\(--awe-color-field-placeholder-invalid\);/);
    const message = rule(criterion, '.field > small.p-invalid');
    expect(message).toMatch(/font-size: var\(--awe-font-size-xs\);/);
    expect(message).toMatch(/color: var\(--awe-color-danger-text\);/);
    expect(rule(criterion, '.criterion-error-icon > .awe-icon')).toMatch(/width: var\(--awe-size-icon-sm\);/);
  });

  it('draws a read-only criterion filled and readable instead of fading the whole criterion', () => {
    expect(rule(criterion, '.field.p-disabled')).toMatch(/opacity: 1;/);
    const readonly = rule(criterion, '.field .p-inputtext:disabled');
    expect(readonly).toMatch(/opacity: 1;/);
    expect(readonly).toMatch(/color: var\(--awe-color-field-text\);/);
    expect(readonly).toMatch(/background: var\(--awe-color-field-filled\);/);
    expect(readonly).toMatch(/box-shadow: none;/);
    expect(rule(criterion, '.field .p-dropdown.p-disabled')).toBe(readonly);
  });

  it('lines up checkboxes, radios and switches with the fields of the row', () => {
    const choice = rule(criterion, '.field .field-checkbox');
    expect(choice).toMatch(/gap: var\(--awe-gap-inline\);/);
    expect(choice).toMatch(/min-height: var\(--awe-size-control\);/);
    expect(rule(criterion, '.field .field-radiobutton')).toBe(choice);
  });

  it('draws the button radio and the button checkbox as a segmented control', () => {
    const track = rule(criterion, '.field .p-selectbutton.p-buttonset');
    expect(track).toMatch(/flex-wrap: wrap;/);
    expect(track).toMatch(/background: var\(--awe-segmented-track\);/);
    expect(track).toMatch(/border: 1px solid var\(--awe-segmented-border\);/);
    expect(rule(criterion, '.field .p-selectbutton.p-buttonset .p-button')).toMatch(/color: var\(--awe-segmented-text\);/);
    const selected = rule(criterion, '.field .p-selectbutton.p-buttonset .p-button.p-highlight');
    expect(selected).toMatch(/background: var\(--awe-segmented-selected\);/);
    expect(selected).toMatch(/border-color: var\(--awe-segmented-selected-border\);/);
    expect(selected).toMatch(/color: var\(--awe-segmented-selected-text\);/);
    expect(rule(criterion, '.field .p-selectbutton.p-buttonset.p-invalid')).toMatch(/border-color: var\(--awe-color-field-border-invalid\);/);
  });

  it('shows numbers with tabular figures and the date range shortcuts as pills', () => {
    expect(rule(read('AweInputNumeric.less'), '.p-inputnumber-input')).toMatch(/font-variant-numeric: var\(--awe-font-variant-numeric\);/);
    expect(read('AweInputDateRange.less')).toMatch(/\.p-button \{\s*border-radius: var\(--awe-radius-full\);/);
  });
});
