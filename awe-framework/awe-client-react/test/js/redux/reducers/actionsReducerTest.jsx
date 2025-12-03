import { actions as actionsReducer } from '../../../../src/redux/reducers/actionsReducer';
import {
  ADD_ACTION,
  ADD_ACTIONS,
  ADD_ACTIONS_TOP,
  ADD_STACK,
  REMOVE_STACK,
  DELETE_STACK,
  RUN_ACTION,
  START_ACTION,
  ACCEPT_ACTION,
  REJECT_ACTION,
  ABORT_ACTION,
  TOGGLE_ACTIONS_RUNNING,
  CLOSE_ALL_ACTIONS,
  ActionStatus
} from '../../../../src/redux/actions/actions';

// Helper to get current stack
function currentStack(state) {
  const sync = state.sync || [[]];
  return sync[Math.max(sync.length - 1, 0)] || [];
}

describe('awe-react-client/test/js/redux/reducers/actionsReducerTest.jsx', () => {
  it('ADD_ACTIONS enqueues actions and runNext sets first to RUNNING', () => {
    const initial = undefined; // reducer will use InitialState
    const a1 = { type: 'filter' };
    const a2 = { type: 'refresh' };
    const state1 = actionsReducer(initial, { type: ADD_ACTIONS, payload: [a1, a2] });

    expect(state1.running).toBe(true);
    const stack = currentStack(state1);
    expect(stack.length).toBe(2);
    expect(stack[0].status).toBe(ActionStatus.STATUS_RUNNING);
    expect(stack[0].type).toBe('filter');
    expect(stack[1].status).toBe(ActionStatus.STATUS_INITIAL);
    // ids should be generated
    expect(typeof stack[0].id).toBe('string');
  });

  it('ADD_ACTIONS_TOP inserts at the front preserving order and runs first', () => {
    let state = actionsReducer(undefined, { type: ADD_ACTIONS, payload: [{ type: 'a' }] });
    const state2 = actionsReducer(state, { type: ADD_ACTIONS_TOP, payload: [{ type: 'top1' }, { type: 'top2' }] });

    const stack = currentStack(state2);
    expect(stack.map(a => a.type)).toEqual(['top1', 'top2', 'a']);
    expect(stack[0].status).toBe(ActionStatus.STATUS_RUNNING);
  });

  it('runNext async branch: first action async moves to async list and RUNNING there', () => {
    const state1 = actionsReducer(undefined, { type: ADD_ACTIONS, payload: [{ type: 'asyncA', async: true }, { type: 'syncB' }] });
    // After adding, runNext is invoked inside reducer
    const stack = currentStack(state1);
    // The async action should have been moved away from sync stack
    expect(stack[0].type).toBe('syncB');
    // Async list should contain the async action marked as RUNNING
    expect(state1.async.length).toBe(1);
    expect(state1.async[0].type).toBe('asyncA');
    expect(state1.async[0].status).toBe(ActionStatus.STATUS_RUNNING);
    expect(state1.running).toBe(true);
  });

  it('RUN_ACTION respects silent flag for running toggle', () => {
    let state = actionsReducer(undefined, { type: ADD_ACTIONS, payload: [{ type: 'x' }] });
    const id = currentStack(state)[0].id;
    // Mark running with silent true
    state = actionsReducer(state, { type: RUN_ACTION, payload: { id, silent: true } });
    expect(state.running).toBe(false);
    expect(currentStack(state)[0].status).toBe(ActionStatus.STATUS_RUNNING);
  });

  it('START_ACTION sets status STARTED and adds startDate', () => {
    let state = actionsReducer(undefined, { type: ADD_ACTIONS, payload: [{ type: 'x' }] });
    const id = currentStack(state)[0].id;
    state = actionsReducer(state, { type: START_ACTION, payload: { id } });
    const act = currentStack(state)[0];
    expect(act.status).toBe(ActionStatus.STATUS_STARTED);
    expect(typeof act.startDate).toBe('number');
  });

  it('ACCEPT_ACTION removes action and toggles running when none left', () => {
    let state = actionsReducer(undefined, { type: ADD_ACTIONS, payload: [{ type: 'one' }] });
    const id = currentStack(state)[0].id;
    state = actionsReducer(state, { type: ACCEPT_ACTION, payload: { id } });
    expect(currentStack(state).length).toBe(0);
    expect(state.running).toBe(false);
  });

  it('REJECT_ACTION clears current stack (DELETE_STACK) and stops running', () => {
    let state = actionsReducer(undefined, { type: ADD_ACTIONS, payload: [{ type: 'one' }, { type: 'two' }] });
    state = actionsReducer(state, { type: REJECT_ACTION });
    expect(currentStack(state).length).toBe(0);
    expect(state.running).toBe(false);
  });

  it('ADD_STACK creates new empty stack; REMOVE_STACK merges current into previous (current first)', () => {
    // First, put one action in initial stack
    let state = actionsReducer(undefined, { type: ADD_ACTIONS, payload: [{ type: 'prev' }] });
    // Add a new stack
    state = actionsReducer(state, { type: ADD_STACK });
    expect(state.sync.length).toBe(2);
    expect(currentStack(state).length).toBe(0);
    // Add one action to current (second) stack
    state = actionsReducer(state, { type: ADD_ACTIONS, payload: [{ type: 'curr' }] });
    const newCurrentId = currentStack(state)[0].id;
    // Now remove stack -> merge current into previous
    state = actionsReducer(state, { type: REMOVE_STACK });
    expect(state.sync.length).toBe(1); // merged into single stack due to reducer logic
    const merged = currentStack(state);
    expect(merged.map(a => a.type)).toEqual(['curr', 'prev']);
    // First should be set to RUNNING (already running), id preserved
    expect(merged[0].id).toBe(newCurrentId);
  });

  it('DELETE_STACK empties current stack', () => {
    let state = actionsReducer(undefined, { type: ADD_ACTIONS, payload: [{ type: 'a' }, { type: 'b' }] });
    state = actionsReducer(state, { type: DELETE_STACK });
    expect(currentStack(state).length).toBe(0);
  });

  it('TOGGLE_ACTIONS_RUNNING toggles and is no-op if same value', () => {
    let state = actionsReducer(undefined, { type: TOGGLE_ACTIONS_RUNNING, running: true });
    expect(state.running).toBe(true);
    const state2 = actionsReducer(state, { type: TOGGLE_ACTIONS_RUNNING, running: true });
    // unchanged object when no diff? reducer returns same reference
    expect(state2).toBe(state);
    const state3 = actionsReducer(state, { type: TOGGLE_ACTIONS_RUNNING, running: false });
    expect(state3.running).toBe(false);
  });

  it('ADD_ACTION adds sync or async without running next', () => {
    let state = actionsReducer(undefined, { type: ADD_ACTION, payload: { type: 'single' } });
    expect(currentStack(state).length).toBe(1);
    expect(currentStack(state)[0].status).toBe(ActionStatus.STATUS_INITIAL);
    expect(state.running).toBe(false);

    state = actionsReducer(state, { type: ADD_ACTION, payload: { type: 'bg', async: true } });
    expect(state.async.length).toBe(1);
    expect(state.async[0].type).toBe('bg');
    expect(state.async[0].status).toBe(ActionStatus.STATUS_INITIAL);
    expect(state.running).toBe(false);
  });

  it('ABORT_ACTION removes action by id', () => {
    let state = actionsReducer(undefined, { type: ADD_ACTIONS, payload: [{ type: 'toAbort' }] });
    const id = currentStack(state)[0].id;
    state = actionsReducer(state, { type: ABORT_ACTION, payload: { id } });
    expect(currentStack(state).length).toBe(0);
  });

  it('CLOSE_ALL_ACTIONS resets to initial state shape', () => {
    let state = actionsReducer(undefined, { type: ADD_ACTIONS, payload: [{ type: 'x' }] });
    state = actionsReducer(state, { type: CLOSE_ALL_ACTIONS });
    expect(state.running).toBe(false);
    expect(state.sync).toEqual([[]]);
    expect(state.async).toEqual([]);
  });
});
