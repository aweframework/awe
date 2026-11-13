const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {translationTexts} = require('../playgroundModel');
const specs = require('../specs');

const SITE = path.join(__dirname, '..', '..', '..', '..');
const spanish = JSON.parse(fs.readFileSync(path.join(SITE, 'i18n', 'es', 'code.json'), 'utf8'));

const spec = {
  name: 'demo',
  controls: [
    {id: 'look', label: 'Look', options: [{value: '', label: 'Default'}, {value: 'btn-primary', label: 'Primary'}, 'plain']},
    {id: 'size', label: 'Size', options: ['sm']},
    {id: 'label', label: 'Label'},
    {id: 'icon', type: 'icon', label: 'Icon', options: ['', 'save']},
  ],
  events: [{name: 'click', description: 'On click'}],
  actions: [{name: 'close', description: 'Closes it'}],
};

test('translationTexts lists the id and English text of every string (icon names are not text) a spec shows', () => {
  assert.deepEqual(translationTexts(spec), {
    'playground.demo.control.look': 'Look',
    'playground.demo.option.look.none': 'Default',
    'playground.demo.option.look.btn-primary': 'Primary',
    'playground.demo.option.look.plain': 'plain',
    'playground.demo.control.size': 'Size',
    'playground.demo.option.size.sm': 'sm',
    'playground.demo.control.label': 'Label',
    'playground.demo.control.icon': 'Icon',
    'playground.demo.event.click': 'On click',
    'playground.demo.action.close': 'Closes it',
  });
});

test('every catalog spec has a Spanish translation for every string it shows', () => {
  const missing = [];
  for (const spec of Object.values(specs)) {
    for (const id of Object.keys(translationTexts(spec))) {
      if (!spanish[id]?.message) missing.push(id);
    }
  }
  assert.deepEqual(missing, []);
});

test('two options of a control never share the id of their translation: an empty value and "none" are different texts', () => {
  const texts = translationTexts({
    name: 'media',
    controls: [{id: 'preload', label: 'Preload', options: [{value: '', label: 'Default'}, {value: 'none', label: 'None'}]}],
  });

  assert.equal(Object.values(texts).filter(text => text === 'Default').length, 1);
  assert.equal(Object.values(texts).filter(text => text === 'None').length, 1);
});

test('every catalog spec gives each string its own translation id', () => {
  const clashes = [];
  for (const spec of Object.values(specs)) {
    const count = spec.controls.reduce((total, control) => total + 1 + (control.type === 'icon' ? 0 : (control.options ?? []).length), 0);
    if (Object.keys(translationTexts(spec)).length < count) clashes.push(spec.name);
  }
  assert.deepEqual(clashes, []);
});
