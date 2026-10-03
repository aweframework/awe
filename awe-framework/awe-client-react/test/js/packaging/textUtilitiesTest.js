const fs = require('fs');
const path = require('path');
const postcss = require('postcss');

const MAIN_CSS = path.join(__dirname, '..', '..', '..', 'src', 'main.css');

describe('awe-react-client/test/js/packaging/textUtilitiesTest.js', () => {
  let rules;

  beforeAll(() => {
    rules = postcss.parse(fs.readFileSync(MAIN_CSS, 'utf8')).nodes.filter((node) => node.type === 'rule');
  });

  const declarationsOf = (selector) => {
    const rule = rules.find((item) => item.selector.trim() === selector);
    expect(rule).toBeDefined();
    const declarations = {};
    rule.walkDecls((declaration) => {
      declarations[declaration.prop] = declaration.value;
    });
    return declarations;
  };

  it('upper cases the text of the screens that use the AngularJS "text-uppercase" class', () => {
    expect(declarationsOf('.text-uppercase')).toEqual({ 'text-transform': 'uppercase' });
  });
});
