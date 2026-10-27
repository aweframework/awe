const fs = require('fs');
const path = require('path');

// The colour mode of the React engine is the light/dark class on <html> (#885): the screens no longer toggle the
// layout-dark/layout-light classes of the body, and the styles read tokens instead of selecting a mode
const REPOSITORY = path.join(__dirname, '../../../../..');
const REACT_SRC = path.join(REPOSITORY, 'awe-framework', 'awe-client-react', 'src');
const INFO_BUTTONS = [
  path.join(REPOSITORY, 'awe-framework', 'awe-generic-screens', 'src', 'main', 'resources', 'application', 'awe-react',
    'screen', 'home', 'info-buttons.xml'),
  path.join(REPOSITORY, 'awe-tests', 'awe-boot-react', 'src', 'main', 'resources', 'application', 'app', 'screen',
    'info-buttons.xml')
];
const LAYOUT_MODE = /layout-(dark|light)/;

const listFiles = (folder, extensions) => fs.readdirSync(folder, {withFileTypes: true}).flatMap(entry => {
  const file = path.join(folder, entry.name);
  if (entry.isDirectory()) {
    return listFiles(file, extensions);
  }
  return extensions.includes(path.extname(entry.name)) ? [file] : [];
});

describe('Colour mode classes (#885)', () => {
  it('keeps the mode selector of the info buttons without body layout classes', () => {
    INFO_BUTTONS.forEach(file => {
      const xml = fs.readFileSync(file, 'utf8');
      // The hidden criterion stays: the React engine refreshes the <html> appearance when it changes
      expect(xml).toContain('id="currentMode" session="themeMode"');
      expect(xml).not.toMatch(LAYOUT_MODE);
    });
  });

  it('has no style of the engine that selects the body layout mode', () => {
    const offenders = listFiles(REACT_SRC, ['.css', '.less'])
      .filter(file => LAYOUT_MODE.test(fs.readFileSync(file, 'utf8')))
      .map(file => path.relative(REACT_SRC, file));
    expect(offenders).toEqual([]);
  });

  it('dims the grids while they load with the surface token, in light and dark mode', () => {
    ['components/AweGrid.less', 'components/AweTreeGrid.less'].forEach(file => {
      const less = fs.readFileSync(path.join(REACT_SRC, file), 'utf8');
      expect(less).toMatch(/component-overlay\s*\{\s*background-color: var\(--awe-grid-loading-overlay\);/);
      expect(less).not.toMatch(/rgba\(/);
    });
  });
});
