const fs = require('fs');
const path = require('path');

const REACT = path.join(__dirname, '../../..');
const LUCIDE = path.join(REACT, 'node_modules', 'lucide-static');
const {readLucide, buildSprite} = require('../../../../awe-design-tokens/icons/lucide-sprite.cjs');

describe('Lucide sprite (#885)', () => {
  const lucide = readLucide(LUCIDE);
  const sprite = buildSprite(lucide);
  const symbols = [...sprite.matchAll(/<symbol\b([^>]*)>([\s\S]*?)<\/symbol>/g)];

  it('has one symbol per icon of the pinned lucide-static, with the Lucide name as id', () => {
    const names = Object.keys(JSON.parse(fs.readFileSync(path.join(LUCIDE, 'icon-nodes.json'), 'utf8')));
    expect(symbols.map(symbol => symbol[1].match(/id="([^"]+)"/)[1])).toEqual(names);
  });

  it('leaves the stroke and the fill to CSS', () => {
    // A symbol has its id and the 24px grid, nothing else
    expect(symbols.filter(symbol => !/^ id="[a-z0-9-]+" viewBox="0 0 24 24"$/.test(symbol[1]))).toEqual([]);
    expect(sprite).not.toMatch(/\sstroke[a-z-]*=/);
    // The only fill is the solid dot some icons draw in the colour of the text (as in the prototype sprite)
    expect([...sprite.matchAll(/fill="([^"]*)"/g)].map(match => match[1]).filter(fill => fill !== 'currentColor')).toEqual([]);
    expect(sprite).not.toMatch(/\bclass=|\bstyle=|\bwidth="24"/);
  });

  it('draws each icon with the shapes of lucide-static', () => {
    const tag = symbols.find(symbol => symbol[1].includes('id="tag"'))[2];
    expect(tag).toBe('<path d="M12.586 2.586A2 2 0 0 0 11.172 2H4a2 2 0 0 0-2 2v7.172a2 2 0 0 0 .586 1.414l8.704 8.704a2.426 2.426 0 0 0 3.42 0l6.58-6.58a2.426 2.426 0 0 0 0-3.42z"/>'
      + '<circle cx="7.5" cy="7.5" r=".5" fill="currentColor"/>');
    // The size of a rect is its geometry, not a presentation attribute of the icon
    const square = symbols.find(symbol => symbol[1].includes('id="square"'))[2];
    expect(square).toBe('<rect width="18" height="18" x="3" y="3" rx="2"/>');
    expect(sprite).not.toMatch(/<rect(?![^>]*\bwidth=)[^>]*>/);
  });

  it('carries the licence of lucide-static, as the ISC licence asks of every copy', () => {
    expect(sprite.startsWith(`<!-- Lucide icons (lucide-static ${lucide.version}`)).toBe(true);
    const comment = sprite.slice('<!--'.length, sprite.indexOf('-->'));
    expect(comment).toContain('ISC License');
    expect(comment).toContain('Copyright (c) 2026 Lucide Icons and Contributors');
    expect(comment).toContain('Copyright (c) 2013-present Cole Bemis');
    // An XML comment cannot hold a double hyphen
    expect(comment).not.toContain('--');
    expect(sprite).toMatch(/<svg xmlns="http:\/\/www.w3.org\/2000\/svg">/);
  });
});
