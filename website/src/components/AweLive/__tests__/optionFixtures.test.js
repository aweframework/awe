const test = require('node:test');
const assert = require('node:assert/strict');
const { findOptionAnswer } = require('../optionFixtures');

const COUNTRIES = [
  { value: 'es', label: 'Spain' },
  { value: 'fr', label: 'France' },
  { value: 'de', label: 'Germany' },
  { value: 'cl', label: 'Chile' },
];
const options = { CountryList: COUNTRIES };
const rowsOf = answer => answer[0].parameters.datalist.rows;
const ask = (suggest, url = '/awe/action/data/CountryList') => findOptionAnswer(url, JSON.stringify({ suggest, max: 0 }), options);

test('the answer is a fill action carrying the datalist, as the server sends it', () => {
  const answer = ask('');

  assert.equal(answer.length, 1);
  assert.equal(answer[0].type, 'fill');
  assert.deepEqual(rowsOf(answer), COUNTRIES);
});

test('the typed text keeps the options whose label contains it, in any case', () => {
  assert.deepEqual(rowsOf(ask('an')).map(row => row.value), ['fr', 'de']);
  assert.deepEqual(rowsOf(ask('CHI')).map(row => row.value), ['cl']);
});

test('accents do not matter when searching', () => {
  const answer = findOptionAnswer('/x/action/data/Pais', JSON.stringify({ suggest: 'espana' }), { Pais: [{ value: 'es', label: 'España' }] });

  assert.deepEqual(rowsOf(answer).map(row => row.value), ['es']);
});

test('a text that matches nothing answers an empty list, not no answer', () => {
  assert.deepEqual(rowsOf(ask('zzz')), []);
});

test('the check of the values of a multiple suggest (a list of values) answers those options', () => {
  const answer = findOptionAnswer('/awe/action/data/CountryList', JSON.stringify({ suggest: ['es', 'cl'] }), options);

  assert.deepEqual(rowsOf(answer).map(row => row.value), ['es', 'cl']);
});

test('a request of another target action is not simulated', () => {
  assert.equal(ask('a', '/awe/action/data/Other'), undefined);
  assert.equal(findOptionAnswer(undefined, '{}', options), undefined);
});

test('no options, or a body that is not JSON, simulates nothing but the empty text', () => {
  assert.equal(findOptionAnswer('/awe/action/data/CountryList', '{}', undefined), undefined);
  assert.deepEqual(rowsOf(findOptionAnswer('/awe/action/data/CountryList', 'not json', options)), COUNTRIES);
  assert.deepEqual(rowsOf(findOptionAnswer('/awe/action/data/CountryList', undefined, options)), COUNTRIES);
});

test('the query string of the url does not hide the target action', () => {
  assert.deepEqual(rowsOf(ask('spa', '/awe/action/data/CountryList?x=1')).map(row => row.value), ['es']);
});
