const test = require('node:test');
const assert = require('node:assert/strict');
const { findFileFixture } = require('../fileFixtures');

const files = { ServerLog: '/files/server.log', Manual: '/files/manual.pdf' };

test('a request whose last path segment is a simulated target action is answered with its file', () => {
  assert.equal(findFileFixture('/awe/rest/file/stream/maintain/Manual', files), '/files/manual.pdf');
  assert.equal(findFileFixture('http://localhost:3786/awe/action/file/ServerLog', files), '/files/server.log');
});

test('a query string or a hash does not hide the target action', () => {
  assert.equal(findFileFixture('/awe/action/file/ServerLog?x=1#top', files), '/files/server.log');
});

test('any other request is not simulated', () => {
  assert.equal(findFileFixture('/awe/action/data/Other', files), undefined);
  assert.equal(findFileFixture('/awe/action/file/ServerLogs', files), undefined);
});

test('a missing url or files is not simulated', () => {
  assert.equal(findFileFixture(undefined, files), undefined);
  assert.equal(findFileFixture('/x/ServerLog', undefined), undefined);
});
