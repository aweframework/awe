const fs = require('fs');
const path = require('path');
const postcss = require('postcss');

const SRC = path.join(__dirname, '..', '..', '..', 'src');
const CSS_FILE = path.join(SRC, 'assets', 'css', 'bootstrap-grid-compat.css');
const TIERS = ['xs', 'sm', 'md', 'lg', 'xl'];
const HOLDS_COLUMNS = `:has(${TIERS.map((tier) => `> [class*="col-${tier}-"]`).join(', ')})`;
const PLAIN_WRAPPER = ':is(div:not([class]), div[class=""])';

const normalize = (selector) => selector.replace(/\s+/g, ' ').trim();

describe('awe-react-client/test/js/packaging/bootstrapGridCompatTest.js', () => {
  describe('main.jsx', () => {
    it('imports the Bootstrap container stylesheet right after layout.css', () => {
      const main = fs.readFileSync(path.join(SRC, 'main.jsx'), 'utf8');
      const layout = main.indexOf("import './assets/css/layout.css';");
      const grid = main.indexOf("import './assets/css/bootstrap-grid-compat.css';");

      expect(layout).toBeGreaterThanOrEqual(0);
      expect(grid).toBeGreaterThan(layout);
      expect(main.slice(layout, grid).split('\n').filter((line) => line.includes('import'))).toHaveLength(1);
    });
  });

  describe('bootstrap-grid-compat.css', () => {
    let rules;

    const declarationsOf = (selector) => {
      const rule = rules.find((item) => normalize(item.selector) === normalize(selector));
      expect(rule).toBeDefined();
      const declarations = {};
      rule.walkDecls((declaration) => {
        declarations[declaration.prop] = declaration.value;
      });
      return declarations;
    };

    beforeAll(() => {
      expect(fs.existsSync(CSS_FILE)).toBe(true);
      rules = [];
      postcss.parse(fs.readFileSync(CSS_FILE, 'utf8')).walkRules((rule) => rules.push(rule));
    });

    it('lets plain wrapper divs holding grid columns pass their columns to the parent row', () => {
      expect(declarationsOf(`${PLAIN_WRAPPER}${HOLDS_COLUMNS}`)).toEqual({display: 'contents'});
    });

    it('turns plain wrapper divs into wrapping rows inside vertical containers', () => {
      expect(declarationsOf(`.expandible-vertical > ${PLAIN_WRAPPER}${HOLDS_COLUMNS}`)).toEqual({
        display: 'flex',
        'flex-wrap': 'wrap',
        'align-content': 'flex-start'
      });
    });

    it('starts a new full-width wrapping line for every row', () => {
      expect(declarationsOf('.row:not(.panel-body)')).toEqual({
        display: 'flex',
        'flex-wrap': 'wrap',
        flex: '0 0 100%',
        width: '100%',
        'box-sizing': 'border-box'
      });
    });

    it('keeps a full line for row children that are not grid columns', () => {
      expect(declarationsOf('.row:not(.panel-body) > :not([class*="col-"])')).toEqual({flex: '0 0 100%'});
    });

    it('leaves column widths to the criterion grid', () => {
      expect(rules.filter((rule) => /\.col-(xs|sm|md|lg|xl)-\d/.test(rule.selector))).toEqual([]);
    });
  });
});
