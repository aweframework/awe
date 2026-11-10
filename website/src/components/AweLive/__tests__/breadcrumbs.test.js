const test = require('node:test');
const assert = require('node:assert/strict');
const { breadcrumbItems, releaseBreadcrumbs } = require('../breadcrumbs');

test('each label becomes an item of the breadcrumb, in order', () => {
  assert.deepEqual(breadcrumbItems(['Customers', 'List']), [{ label: 'Customers' }, { label: 'List' }]);
});

test('blank labels are left out and the others are trimmed', () => {
  assert.deepEqual(breadcrumbItems([' Customers ', '', '   ', 'List']), [{ label: 'Customers' }, { label: 'List' }]);
});

test('anything that is not a list of labels is an empty breadcrumb', () => {
  assert.deepEqual(breadcrumbItems(undefined), []);
  assert.deepEqual(breadcrumbItems('Customers'), []);
  assert.deepEqual(breadcrumbItems([null, 3, 'List']), [{ label: 'List' }]);
});

test('an example releases the breadcrumb only if it is still the one the registry holds', () => {
  const calls = [];
  const mine = [{ label: 'A' }];
  const registry = (items) => ({ getBreadcrumbs: () => ({ items }), clear: () => calls.push('clear') });

  releaseBreadcrumbs(registry(mine), mine);
  releaseBreadcrumbs(registry([{ label: 'B' }]), mine);

  assert.deepEqual(calls, ['clear']);
});
