const test = require('node:test');
const assert = require('node:assert/strict');
const { createUploadSimulator, acceptsUpload } = require('../simulateUploads');

// A window whose XMLHttpRequest records what it would really send
function fakeWindow() {
  const sent = [];
  class FakeXhr {
    constructor() {
      this.upload = new EventTarget();
      this.readyState = 0;
      this.status = 0;
    }
    open(method, url) { this.url = url; }
    setRequestHeader() {}
    send(body) { sent.push({ url: this.url, body }); }
  }
  return { win: { XMLHttpRequest: FakeXhr }, sent, FakeXhr };
}

const form = (address = { component: 'doc', view: 'live' }, file = { name: 'report.pdf', size: 2048 }) => ({
  get: key => (key === 'address' ? JSON.stringify(address) : key === 'file' ? file : null),
});
const manual = () => {
  const queue = [];
  return { schedule: fn => queue.push(fn), run: () => { while (queue.length) queue.shift()(); }, queue };
};
const makeEvent = (type, init) => Object.assign(new Event(type), init);

function upload(win, url = '/awe/file/upload', body = form()) {
  const xhr = new win.XMLHttpRequest();
  const events = [];
  xhr.upload.addEventListener('progress', event => events.push(event.loaded / event.total));
  const states = [];
  xhr.onreadystatechange = () => states.push([xhr.readyState, xhr.status]);
  xhr.open('POST', url, true);
  xhr.send(body);
  return { xhr, events, states };
}

test('an upload is not sent: it reports its progress, hands over the file and ends with status 200', () => {
  const { win, sent } = fakeWindow();
  const timer = manual();
  const files = [];
  const { simulate } = createUploadSimulator(win, { schedule: timer.schedule, createEvent: makeEvent });
  simulate(file => files.push(file));

  const { events, states } = upload(win);
  assert.deepEqual(sent, []);
  timer.run();

  assert.deepEqual(events, [0.25, 0.5, 0.75, 1]);
  assert.deepEqual(states, [[4, 200]]);
  assert.deepEqual(files, [{ address: { component: 'doc', view: 'live' }, file: { name: 'report.pdf', size: 2048 } }]);
});

test('the file is handed over once the progress is complete, before the request ends', () => {
  const { win } = fakeWindow();
  const timer = manual();
  const order = [];
  const { simulate } = createUploadSimulator(win, { schedule: timer.schedule, createEvent: makeEvent });
  simulate(() => order.push('file'));
  const xhr = new win.XMLHttpRequest();
  xhr.upload.addEventListener('progress', () => order.push('progress'));
  xhr.onreadystatechange = () => order.push('done');
  xhr.open('POST', '/awe/file/upload');
  xhr.send(form());
  timer.run();

  assert.deepEqual(order, ['progress', 'progress', 'progress', 'progress', 'file', 'done']);
});

test('any other request is sent as it is', () => {
  const { win, sent } = fakeWindow();
  const timer = manual();
  const { simulate } = createUploadSimulator(win, { schedule: timer.schedule, createEvent: makeEvent });
  simulate(() => {});

  upload(win, '/awe/action/data/Other', { plain: true });

  assert.equal(sent.length, 1);
  assert.equal(timer.queue.length, 0);
});

test('the requests go back to the network when the last example stops', () => {
  const { win, sent } = fakeWindow();
  const timer = manual();
  const { simulate } = createUploadSimulator(win, { schedule: timer.schedule, createEvent: makeEvent });
  const stopA = simulate(() => {});
  const stopB = simulate(() => {});

  stopA();
  upload(win);
  assert.equal(sent.length, 0);
  stopB();
  upload(win);
  assert.equal(sent.length, 1);
});

test('no handler, or no window, simulates nothing', () => {
  const { win, sent } = fakeWindow();
  const { simulate } = createUploadSimulator(win, { schedule: () => {}, createEvent: makeEvent });
  simulate(undefined)();
  upload(win);
  assert.equal(sent.length, 1);
  assert.doesNotThrow(() => createUploadSimulator(undefined).simulate(() => {})());
});

test('the end of the request is also reported with the load and loadend events of the request and of its upload', () => {
  const { win } = fakeWindow();
  win.XMLHttpRequest.prototype.dispatchEvent = function dispatchEvent(event) { this.events = [...(this.events ?? []), event.type]; return true; };
  const timer = manual();
  const { simulate } = createUploadSimulator(win, { schedule: timer.schedule, createEvent: makeEvent });
  simulate(() => {});
  const { xhr } = upload(win);
  const uploadEvents = [];
  xhr.upload.addEventListener('load', () => uploadEvents.push('load'));
  timer.run();

  assert.deepEqual(xhr.events, ['load', 'loadend']);
  assert.deepEqual(uploadEvents, ['load']);
});

test('a body whose address is not JSON still ends the upload: the file goes without address', () => {
  const { win } = fakeWindow();
  const timer = manual();
  const files = [];
  const { simulate } = createUploadSimulator(win, { schedule: timer.schedule, createEvent: makeEvent });
  simulate(item => files.push(item));
  const body = { get: key => (key === 'address' ? 'not json' : key === 'file' ? { name: 'a.txt', size: 1 } : null) };

  upload(win, '/awe/file/upload', body);
  timer.run();

  assert.deepEqual(files, [{ address: null, file: { name: 'a.txt', size: 1 } }]);
});

test('an example takes an upload only when it is the one that owns the criterion: same view and a component of its own', () => {
  const components = { doc: {} };

  assert.equal(acceptsUpload({ view: 'live-2', component: 'doc' }, 'live-2', components), true);
  assert.equal(acceptsUpload({ view: 'live-3', component: 'doc' }, 'live-2', components), false);
  assert.equal(acceptsUpload({ view: 'live-2', component: 'other' }, 'live-2', components), false);
  assert.equal(acceptsUpload(null, 'live-2', components), false);
});
