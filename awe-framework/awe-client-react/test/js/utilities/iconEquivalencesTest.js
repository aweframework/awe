const fs = require('fs');
const path = require('path');
import {resolveIcon} from '../../../src/utilities/iconResolver';

const REACT = path.join(__dirname, '../../..');
const FRAMEWORK = path.join(REACT, '..');
const REPOSITORY = path.join(FRAMEWORK, '..');
const ICONS = path.join(FRAMEWORK, 'awe-design-tokens', 'icons');
const LUCIDE = path.join(REACT, 'node_modules', 'lucide-static');

const readJson = file => JSON.parse(fs.readFileSync(file, 'utf8'));
const equivalences = readJson(path.join(ICONS, 'icon-equivalences.json'));
const lucideNames = readJson(path.join(ICONS, 'lucide-names.json'));
const lucideIcons = new Set(Object.keys(readJson(path.join(LUCIDE, 'icon-nodes.json'))));

// Every file of the given extensions under a folder, without build output nor dependencies
const SKIPPED_FOLDERS = new Set(['node_modules', 'target', 'dist', '.git', 'build']);
const listFiles = (folder, extensions) => fs.readdirSync(folder, {withFileTypes: true}).flatMap(entry => {
  const file = path.join(folder, entry.name);
  if (entry.isDirectory()) {
    return SKIPPED_FOLDERS.has(entry.name) ? [] : listFiles(file, extensions);
  }
  return extensions.some(extension => entry.name.endsWith(extension)) ? [file] : [];
});

// The icon values of the screen XML, found as the inventory of #885 found them:
// rg -o '\bicon(-loading)?="[^"]+"' -g '*.xml' awe-framework awe-tests awe-samples
// Values with [wildcards] come from data (the notifier enumerated gives times / check) and are left out
const xmlIconValues = () => [...new Set(['awe-framework', 'awe-tests', 'awe-samples']
  .flatMap(folder => listFiles(path.join(REPOSITORY, folder), ['.xml']))
  .flatMap(file => [...fs.readFileSync(file, 'utf8').matchAll(/\bicon(?:-loading)?="([^"]+)"/g)].map(match => match[1])))]
  .filter(value => !value.includes('['));

// Modifier classes of the icon fonts, not icons
const PRIME_MODIFIERS = new Set(['pi-fw', 'pi-spin']);

// The PrimeIcons and the icon names the React components write in their code (the resolver documents its syntax)
const RESOLVER = path.join(REACT, 'src', 'utilities', 'iconResolver.js');
const reactHardCodedIcons = () => {
  const sources = listFiles(path.join(REACT, 'src'), ['.js', '.jsx']).filter(file => file !== RESOLVER)
    .map(file => fs.readFileSync(file, 'utf8'));
  const primeIcons = sources.flatMap(source => [...source.matchAll(/\bpi-[a-z0-9-]+/g)].map(match => match[0]))
    .filter(name => !PRIME_MODIFIERS.has(name));
  const iconCodes = sources.flatMap(source => [...source.matchAll(/getIconCode\(\s*["']([^"']+)["']/g)].map(match => match[1]));
  return [...new Set([...primeIcons, ...iconCodes])];
};

const fallbackOf = family => new Set(equivalences.fallback[family]);
const isFallback = value => {
  const icon = resolveIcon(value);
  return icon !== null && icon.family !== 'lucide' && fallbackOf(icon.family).has(icon.name);
};
const unresolved = values => values.filter(value => resolveIcon(value)?.family !== 'lucide' && !isFallback(value));

describe('Icon equivalence table (#885)', () => {
  it('maps every icon of the screen XML to a Lucide icon or lists it as a fallback', () => {
    const values = xmlIconValues();
    // The inventory of #885 found 185 distinct values: the walk must find the screens
    expect(values.length).toBeGreaterThan(150);
    expect(unresolved(values)).toEqual([]);
  });

  it('maps every icon written in the React components to a Lucide icon or lists it as a fallback', () => {
    const icons = reactHardCodedIcons();
    expect(icons).toEqual(expect.arrayContaining(['pi-question-circle', 'pi-times', 'refresh', 'circle']));
    expect(unresolved(icons)).toEqual([]);
  });

  it('only points to icons of the pinned lucide-static', () => {
    const targets = ['fa', 'material', 'pi'].flatMap(family => Object.values(equivalences[family]));
    expect(targets.filter(name => !lucideIcons.has(name))).toEqual([]);
  });

  it('never maps and falls back the same name', () => {
    ['fa', 'material', 'pi'].forEach(family => {
      expect(equivalences.fallback[family].filter(name => name in equivalences[family])).toEqual([]);
    });
  });

  it('is built for the pinned lucide-static version', () => {
    const pinned = readJson(path.join(REACT, 'package.json')).devDependencies['lucide-static'];
    expect(pinned).toMatch(/^\d+\.\d+\.\d+$/);
    expect(readJson(path.join(LUCIDE, 'package.json')).version).toBe(pinned);
    expect(equivalences.lucideVersion).toBe(pinned);
    expect(lucideNames.version).toBe(pinned);
  });

  it('keeps the list of Lucide names in step with the pinned lucide-static', () => {
    const {readLucide, buildNames} = require('../../../../awe-design-tokens/icons/lucide-sprite.cjs');
    expect(lucideNames).toEqual(buildNames(readLucide(LUCIDE)));
    expect(lucideNames.icons).toEqual([...lucideIcons]);
  });
});
