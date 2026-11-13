const fs = require('fs');
const path = require('path');

const CLIENTS = path.join(__dirname, '../../../..');
const REACT_SRC = path.join(CLIENTS, 'awe-client-react', 'src');
const ANGULAR_TEMPLATES = path.join(CLIENTS, 'awe-client-angular', 'src', 'main', 'resources', 'templates', 'awe');
const lucideNames = JSON.parse(fs.readFileSync(
  path.join(CLIENTS, 'awe-design-tokens', 'icons', 'lucide-names.json'), 'utf8'));

const read = file => fs.readFileSync(file, 'utf8');
const spriteReferences = files => files.flatMap(file => [...read(file).matchAll(/"([^"#]*lucide-sprite\.svg)#([a-z0-9-]+)"/g)])
  .map(([, sprite, name]) => ({sprite, name}));
const constantOf = (file, pattern) => read(file).match(pattern)?.[1];

const REACT_TEMPLATES = ['help.stg', 'templates.stg'].map(file => path.join(REACT_SRC, file));

// The server templates of the help, breadcrumb and error page draw the Lucide icons of the sprite (#885)
describe('Lucide icons of the server templates', () => {
  it('references only Lucide icons of the sprite in the React templates', () => {
    const names = spriteReferences(REACT_TEMPLATES).map(({name}) => name);
    expect(names).toEqual(expect.arrayContaining(['house', 'triangle-alert', 'square-pen', 'check', 'x']));
    expect(names.filter(name => !lucideNames.icons.includes(name))).toEqual([]);
  });

  it('draws the same icons in the React and AngularJS templates', () => {
    const iconsOf = files => spriteReferences(files).map(({name}) => name).sort();
    const angular = ['help.stg', 'templates.stg'].map(file => path.join(ANGULAR_TEMPLATES, file));
    expect(iconsOf(REACT_TEMPLATES)).toEqual(iconsOf(angular));
  });

  it('keeps the brand icons without Lucide equivalence in Font Awesome', () => {
    expect(read(REACT_TEMPLATES[0])).toContain('<i class="fa fa-youtube-play fa-fw"></i>');
    expect(REACT_TEMPLATES.map(read).join('\n')).not.toMatch(/fa-(home|exclamation-triangle|bar-chart|table|check|times)\b/);
  });

  it('reads the sprite from the same place in the templates, both clients and the server', () => {
    // Relative to <base href> (the context path): the React client prefixes the context path to its absolute path
    const angularIcon = constantOf(path.join(CLIENTS, 'awe-client-angular', 'src', 'main', 'resources', 'js', 'awe',
      'services', 'icon.js'), /export const ICON_SPRITE = "([^"]+)"/);
    const reactUtilities = constantOf(path.join(REACT_SRC, 'utilities', 'index.tsx'),
      /export const ICON_SPRITE = "([^"]+)"/);
    const server = constantOf(path.join(CLIENTS, 'awe-model', 'src', 'main', 'java', 'com', 'almis', 'awe', 'model',
      'util', 'icon', 'IconResolver.java'), /public static final String SPRITE = "([^"]+)"/);
    const templates = [...new Set(spriteReferences([...REACT_TEMPLATES,
      ...['help.stg', 'templates.stg'].map(file => path.join(ANGULAR_TEMPLATES, file))]).map(({sprite}) => sprite))];

    expect(angularIcon).toBe('icons/lucide-sprite.svg');
    expect(reactUtilities).toBe(`/${angularIcon}`);
    expect(server).toBe(angularIcon);
    expect(templates).toEqual([angularIcon]);
  });
});
