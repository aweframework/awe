const test = require('node:test');
const assert = require('node:assert/strict');
const {attrsToClient, columnsXml, columnsClient, sumOf} = require('../specs/data/dataModel');

const COLUMNS = [
  {name: 'name', label: 'Customer', component: 'text', 'avatar-field': 'initials', sortable: 'true', charlength: '24'},
  {name: 'mrr', label: 'MRR', align: 'right', 'bar-max': '9000'},
];

test('attrsToClient writes the attributes as the client reads them: camelCase, booleans and numbers', () => {
  assert.deepEqual(attrsToClient(COLUMNS[0]), {
    name: 'name', label: 'Customer', component: 'text', avatarField: 'initials', sortable: true, charlength: 24,
  });
  assert.deepEqual(attrsToClient({hidden: 'false', 'bar-max': '9000', max: '5'}), {hidden: false, barMax: 9000, max: 5});
});

test('text that only looks like a number stays text, and a label is always text', () => {
  assert.deepEqual(attrsToClient({label: '2024', name: '007'}), {label: '2024', name: '007'});
});

test('columnsXml gives one column child by column and columnsClient the column model of the grid', () => {
  assert.deepEqual(columnsXml(COLUMNS).map(child => [child.tag, child.attrs.name]), [['column', 'name'], ['column', 'mrr']]);
  assert.deepEqual(columnsClient(COLUMNS).map(column => column.name), ['name', 'mrr']);
  assert.equal(columnsClient(COLUMNS)[1].barMax, 9000);
});

test('columnsXml and columnsClient do not share the objects of the columns', () => {
  columnsXml(COLUMNS)[0].attrs.label = 'changed';
  columnsClient(COLUMNS)[0].label = 'changed';

  assert.equal(COLUMNS[0].label, 'Customer');
});

test('sumOf adds a field of the rows and ignores what is not a number', () => {
  assert.equal(sumOf([{a: 1}, {a: 2.5}, {a: 'x'}, {}], 'a'), 3.5);
  assert.equal(sumOf([], 'a'), 0);
});
