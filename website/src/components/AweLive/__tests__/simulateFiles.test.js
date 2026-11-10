const test = require('node:test');
const assert = require('node:assert/strict');
const { createFileSimulator, withBaseUrls } = require('../simulateFiles');

// A window whose fetch answers by url: the static file with its content, anything else as the real server would
function fakeWindow(answers = {}) {
  const calls = [];
  const fetch = async input => {
    const url = typeof input === 'string' ? input : input.url;
    calls.push(url);
    const answer = answers[url];
    return answer ?? new Response('real', { status: 200 });
  };
  return { win: { fetch }, calls, original: fetch };
}

test('a request of a simulated target action is answered with the static file as a 201', async () => {
  const { win, calls } = fakeWindow({ '/live/a.log': new Response('log text', { status: 200 }) });
  const { simulate } = createFileSimulator(win, { warn: () => {} });
  simulate({ ServerLog: '/live/a.log' });

  const response = await win.fetch('/awe/action/file/ServerLog', { method: 'POST' });

  assert.equal(response.status, 201);
  assert.equal(await response.text(), 'log text');
  assert.deepEqual(calls, ['/live/a.log']);
});

test('any other request goes to the real fetch', async () => {
  const { win } = fakeWindow();
  const { simulate } = createFileSimulator(win, { warn: () => {} });
  simulate({ ServerLog: '/live/a.log' });

  assert.equal(await (await win.fetch('/awe/action/data/Other')).text(), 'real');
});

test('a fixture that cannot be read surfaces: the failure status goes to the viewer and a warning is logged', async () => {
  const warnings = [];
  const { win } = fakeWindow({ '/live/missing.log': new Response('Not found', { status: 404 }) });
  const { simulate } = createFileSimulator(win, { warn: (...args) => warnings.push(args.join(' ')) });
  simulate({ ServerLog: '/live/missing.log' });

  const response = await win.fetch('/x/ServerLog');

  assert.equal(response.status, 404);
  assert.equal(warnings.length, 1);
  assert.match(warnings[0], /missing\.log/);
});

test('a fixture request that fails rejects and logs a warning', async () => {
  const warnings = [];
  const { win } = fakeWindow();
  win.fetch = async () => { throw new TypeError('offline'); };
  const { simulate } = createFileSimulator(win, { warn: message => warnings.push(message) });
  simulate({ ServerLog: '/live/a.log' });

  await assert.rejects(() => win.fetch('/x/ServerLog'), /offline/);
  assert.equal(warnings.length, 1);
});

test('the real fetch comes back when the last example stops, and not before', () => {
  const { win, original } = fakeWindow();
  const { simulate } = createFileSimulator(win, { warn: () => {} });
  const stopA = simulate({ A: '/a' });
  const stopB = simulate({ B: '/b' });

  assert.notEqual(win.fetch, original);
  stopA();
  assert.notEqual(win.fetch, original);
  stopB();
  assert.equal(win.fetch, original);
});

test('two examples with the same files object stop independently', () => {
  const { win, original } = fakeWindow();
  const { simulate } = createFileSimulator(win, { warn: () => {} });
  const files = { A: '/a' };
  const stopFirst = simulate(files);
  const stopSecond = simulate(files);

  stopFirst();
  assert.notEqual(win.fetch, original);
  stopSecond();
  assert.equal(win.fetch, original);
});

test('a fetch that somebody else patched on top is not overwritten when the examples stop', () => {
  const { win, original } = fakeWindow();
  const { simulate } = createFileSimulator(win, { warn: () => {} });
  const stop = simulate({ A: '/a' });
  const other = async () => new Response('other');
  win.fetch = other;

  stop();

  assert.equal(win.fetch, other);
  assert.notEqual(win.fetch, original);
});

test('a patch on top that still calls ours reaches the real fetch after the examples stop, and examples mounted later work', async () => {
  const { win } = fakeWindow({ '/live/b.log': new Response('b text', { status: 200 }) });
  const { simulate } = createFileSimulator(win, { warn: () => {} });
  const stop = simulate({ A: '/a' });
  const ours = win.fetch;
  win.fetch = (input, init) => ours(input, init);

  stop();
  const passthrough = await win.fetch('/api/data');
  simulate({ B: '/live/b.log' });
  const simulated = await win.fetch('/awe/action/file/B');

  assert.equal(await passthrough.text(), 'real');
  assert.equal(simulated.status, 201);
  assert.equal(await simulated.text(), 'b text');
});

test('no files, or no window, simulates nothing', () => {
  const { win, original } = fakeWindow();
  const { simulate } = createFileSimulator(win, { warn: () => {} });
  simulate({})();
  simulate(undefined)();
  assert.equal(win.fetch, original);
  assert.doesNotThrow(() => createFileSimulator(undefined).simulate({ A: '/a' })());
});

test('withBaseUrls puts every file url under the base url of the site and keeps no files as no files', () => {
  assert.deepEqual(withBaseUrls({ A: '/live/a.log' }, url => `/docs${url}`), { A: '/docs/live/a.log' });
  assert.equal(withBaseUrls(undefined, url => url), undefined);
});

test('a search of the options of a target action is answered with the datalist filtered by the typed text', async () => {
  const { win, calls } = fakeWindow();
  const { simulateOptions } = createFileSimulator(win, { warn: () => {} });
  simulateOptions({ CountryList: [{ value: 'es', label: 'Spain' }, { value: 'fr', label: 'France' }] });

  const response = await win.fetch('/awe/action/data/CountryList', { method: 'POST', body: JSON.stringify({ suggest: 'fra' }) });
  const answer = await response.json();

  assert.equal(response.status, 200);
  assert.deepEqual(answer[0].parameters.datalist.rows, [{ value: 'fr', label: 'France' }]);
  assert.deepEqual(calls, []);
});

test('the requests of other target actions go to the real fetch while options are simulated', async () => {
  const { win } = fakeWindow();
  const { simulateOptions } = createFileSimulator(win, { warn: () => {} });
  simulateOptions({ CountryList: [] });

  assert.equal(await (await win.fetch('/awe/action/data/Other', { body: '{}' })).text(), 'real');
});

test('the real fetch comes back when the last simulated options stop, files and options sharing the patch', () => {
  const { win, original } = fakeWindow();
  const { simulate, simulateOptions } = createFileSimulator(win, { warn: () => {} });
  const stopFiles = simulate({ A: '/a' });
  const stopOptions = simulateOptions({ B: [] });

  stopFiles();
  assert.notEqual(win.fetch, original);
  stopOptions();
  assert.equal(win.fetch, original);
  assert.doesNotThrow(() => createFileSimulator(win).simulateOptions({})());
});
