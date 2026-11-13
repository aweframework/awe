const test = require('node:test');
const assert = require('node:assert/strict');
const { CLIENT_ACTIONS, runKind, answerActions, partitionActions, routeActions } = require('../clientActions');

test('the simulated client actions are the dialog, wizard and grid ones the examples need, and no server action', () => {
  const types = [...CLIENT_ACTIONS];

  for (const type of ['dialog', 'close', 'next-step', 'select-row', 'change-page', 'change-sort', 'tree-branch', 'edit-row', 'save-row']) {
    assert.ok(types.includes(type), type);
  }
  for (const type of ['server', 'filter', 'validate-row']) {
    assert.ok(!types.includes(type), type);
  }
});

test('partitionActions splits the actions the examples run from the ones nobody handles, keeping the order', () => {
  const items = [{ type: 'validate-row' }, { type: 'dialog', target: 'd' }, { type: 'server' }, { type: 'next-step', target: 'w' }];
  const { handled, rest } = partitionActions(items);

  assert.deepEqual(handled.map(item => item.type), ['dialog', 'next-step']);
  assert.deepEqual(rest.map(item => item.type), ['validate-row', 'server']);
});

test('partitionActions accepts a missing or empty list', () => {
  assert.deepEqual(partitionActions(undefined), { handled: [], rest: [] });
  assert.deepEqual(partitionActions([]), { handled: [], rest: [] });
});

const STACK = 'ADD_ACTIONS_TOP';
const add = (...types) => ({ type: STACK, actions: types.map(type => ({ type })), payload: types.map(type => ({ type })) });

test('routeActions lets any other redux action go on untouched', () => {
  const other = { type: 'UPDATE_MODEL', payload: [{ type: 'dialog' }] };
  const routed = routeActions(other, STACK);

  assert.deepEqual(routed.run, []);
  assert.equal(routed.forward, other);
});

test('routeActions forwards the very same action when the stack has nothing to simulate', () => {
  const action = add('server', 'validate-row');
  const routed = routeActions(action, STACK);

  assert.deepEqual(routed.run, []);
  assert.equal(routed.forward, action);
});

test('routeActions runs the simulated actions and forwards the rest in both fields of the stack action', () => {
  const routed = routeActions(add('dialog', 'server', 'select-row'), STACK);

  assert.deepEqual(routed.run.map(item => item.type), ['dialog', 'select-row']);
  assert.equal(routed.forward.type, STACK);
  assert.deepEqual(routed.forward.actions.map(item => item.type), ['server']);
  assert.deepEqual(routed.forward.payload.map(item => item.type), ['server']);
});

test('routeActions forwards nothing when every action of the stack is simulated', () => {
  const routed = routeActions(add('next-step', 'change-page'), STACK);

  assert.deepEqual(routed.run.map(item => item.type), ['next-step', 'change-page']);
  assert.equal(routed.forward, undefined);
});

test('routeActions accepts a missing action', () => {
  assert.deepEqual(routeActions(undefined, STACK), { run: [], forward: undefined });
});

const deleteFile = { type: 'server', address: { component: 'doc' }, parameters: { serverAction: 'delete-file', filename: 'a.pdf' } };

test('the delete of an uploaded file is run by the examples: it is the only server action they simulate', () => {
  const routed = routeActions({ type: STACK, actions: [deleteFile, { type: 'server', parameters: { serverAction: 'data' } }], payload: [deleteFile, { type: 'server', parameters: { serverAction: 'data' } }] }, STACK);

  assert.deepEqual(routed.run, [deleteFile]);
  assert.deepEqual(routed.forward.actions.map(item => item.parameters.serverAction), ['data']);
});

test('a server action of another kind, or without parameters, is not simulated', () => {
  assert.deepEqual(partitionActions([{ type: 'server' }, { type: 'server', parameters: { serverAction: 'maintain' } }]).handled, []);
});

test('the messages, the context menu of a button and the static value are run by the examples', () => {
  for (const type of ['confirm', 'message', 'target-message', 'show-context-menu', 'value']) {
    assert.ok(CLIENT_ACTIONS.has(type), type);
  }
});

const save = { type: 'server', address: { view: 'live', component: 'Save' }, parameters: { serverAction: 'maintain', targetAction: 'SaveCustomer' } };
const answers = { SaveCustomer: [{ type: 'message', parameters: { type: 'ok', title: 'Saved', message: 'Done' } }] };

test('a server action whose target action has an answer is run by the examples; the others are not', () => {
  const other = { type: 'server', parameters: { serverAction: 'maintain', targetAction: 'Other' } };

  assert.deepEqual(partitionActions([save, other], answers), { handled: [save], rest: [other] });
  assert.deepEqual(partitionActions([save]).handled, []);
});

test('routeActions takes the answers into account', () => {
  const routed = routeActions({ type: STACK, actions: [save], payload: [save] }, STACK, answers);

  assert.deepEqual(routed.run, [save]);
  assert.equal(routed.forward, undefined);
});

test('answerActions gives the actions of the answer the address of the action that asked, as the server does', () => {
  const [message] = answerActions(save, answers);

  assert.equal(message.type, 'message');
  assert.deepEqual(message.address, save.address);
  assert.equal(answers.SaveCustomer[0].address, undefined);
});

test('answerActions keeps the address an action of the answer carries, and is empty without an answer', () => {
  const targeted = { SaveCustomer: [{ type: 'target-message', address: { component: 'Name' }, parameters: {} }] };

  assert.deepEqual(answerActions(save, targeted)[0].address, { view: 'live', component: 'Name' });
  assert.deepEqual(answerActions(save, {}), []);
  assert.deepEqual(answerActions(save, undefined), []);
});

test('runKind says how the middleware runs each handled action: the answer, the delete of a file or the thunk of the client', () => {
  assert.equal(runKind(save, answers), 'answer');
  assert.equal(runKind(deleteFile, answers), 'delete-file');
  assert.equal(runKind(deleteFile), 'delete-file');
  assert.equal(runKind({ type: 'dialog' }, answers), 'thunk');
  // A server action nobody answers and that is not the delete of a file is never run
  assert.equal(runKind({ type: 'server', parameters: { serverAction: 'maintain', targetAction: 'Other' } }, answers), 'thunk');
});

test('an answer that answers itself would loop through the stack: its answered server actions are dropped', () => {
  const looping = { SaveCustomer: [{ type: 'server', parameters: { serverAction: 'maintain', targetAction: 'SaveCustomer' } }, { type: 'message', parameters: {} }] };

  assert.deepEqual(answerActions(save, looping).map(action => action.type), ['message']);
});
