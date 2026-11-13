const fs = require('fs');
const path = require('path');

const SRC = path.join(__dirname, '../../../src');
const TEMPLATES = ['help.stg', 'templates.stg'];

// Every {t{'KEY'}} placeholder is translated by the client, so an unclosed one leaks raw markup into the page.
describe.each(TEMPLATES)('%s translation placeholders', (fileName) => {
  const lines = fs.readFileSync(path.join(SRC, fileName), 'utf8').split('\n');
  const placeholders = lines
    .map((text, index) => ({text, line: index + 1}))
    .filter(({text}) => text.includes('{t{'));

  it('closes every {t{ placeholder with }}', () => {
    const unclosed = placeholders
      .filter(({text}) => (text.match(/\{t\{/g) || []).length !== (text.match(/\{t\{[^{}]*\}\}/g) || []).length)
      .map(({line}) => `${fileName}:${line}`);

    expect(unclosed).toEqual([]);
  });
});
