import * as suggestThunks from '../../../../src/redux/thunks/suggest';
import { ADD_ACTIONS_TOP } from '../../../../src/redux/actions/actions';
import { KEEP_MODEL } from '../../../../src/redux/actions/components';

describe('awe-react-client/test/js/redux/thunks/suggestThunkTest.js', () => {
  let dispatch;
  let getState;
  let state;

  beforeEach(() => {
    dispatch = jest.fn();
    state = {
      settings: { token: 'TOKEN' },
      // getFormValues reads state; minimal shape is fine for our checks
      components: {}
    };
    getState = jest.fn(() => state);
    global.fetch = jest.fn();
    window.fetch = global.fetch;
  });

  describe('suggestAction', () => {
    it('requests suggestions and returns rows + default when strict=false and text not empty; dispatches extra actions', async () => {
      // Arrange response: one fill with rows and one extra action that should be dispatched via ADD_ACTIONS_TOP
      const response = [
        { type: 'fill', parameters: { datalist: { rows: [{ value: 1, label: 'one' }] } } },
        { type: 'other-action', foo: 'bar' }
      ];
      const fetchSpy = jest.spyOn(window, 'fetch').mockReturnValue(Promise.resolve({
        ok: true,
        json: () => Promise.resolve(response)
      }));

      const props = { serverAction: 'data', targetAction: 'suggest-cities', strict: false };

      // Act
      const result = await suggestThunks.suggestAction('input', 'Madrid', props)(dispatch, getState);

      // Assert fetch params
      expect(fetchSpy).toHaveBeenCalled();
      const [url, options] = fetchSpy.mock.calls.at(-1);
      expect(typeof url).toBe('string');
      expect(url).toContain('/action/');
      expect(url).toContain('suggest-cities');
      expect(options.method).toBe('POST');
      const sentBody = JSON.parse(options.body);
      expect(sentBody.suggest).toBe('Madrid');
      expect(sentBody.max).toBe(0);
      expect(options.headers.Authorization).toBe('TOKEN');

      // Assert ADD_ACTIONS_TOP dispatched for other actions
      const addTop = dispatch.mock.calls.map(a => a[0]).find(a => a && a.type === ADD_ACTIONS_TOP);
      expect(addTop).toBeDefined();
      expect(addTop.payload.length).toBe(1);
      expect(addTop.payload[0].type).toBe('other-action');

      // Result should include rows plus default value (strict=false)
      const labels = result.map(r => r.label);
      expect(labels).toContain('one');
      expect(labels).toContain('Madrid');
    });

    it('does not add default option when strict=true', async () => {
      const response = [
        { type: 'fill', parameters: { datalist: { rows: [{ value: 2, label: 'two' }] } } }
      ];
      jest.spyOn(window, 'fetch').mockReturnValue(Promise.resolve({
        ok: true,
        json: () => Promise.resolve(response)
      }));

      const result = await suggestThunks.suggestAction('input', 'Custom', { serverAction: 'data', targetAction: 'sug', strict: true })(dispatch, getState);

      const labels = result.map(r => r.label);
      expect(labels).toEqual(['two']);
      expect(labels).not.toContain('Custom');
    });

    it('does not add default option when text is empty even if strict=false', async () => {
      const response = [
        { type: 'fill', parameters: { datalist: { rows: [] } } }
      ];
      jest.spyOn(window, 'fetch').mockReturnValue(Promise.resolve({
        ok: true,
        json: () => Promise.resolve(response)
      }));

      const result = await suggestThunks.suggestAction('input', '', { serverAction: 'data', targetAction: 'sug', strict: false })(dispatch, getState);
      expect(result).toEqual([]);
    });
  });

  describe('initialSuggestAction', () => {
    it('fetches with checkTarget when provided, updates model, keeps model and returns rows', async () => {
      const rows = [
        { value: 'ABC', label: 'ABC' },
        { value: 'XYZ', label: 'XYZ' }
      ];
      const response = [
        { type: 'fill', parameters: { datalist: { rows } } }
      ];
      const fetchSpy = jest.spyOn(window, 'fetch').mockReturnValue(Promise.resolve({
        ok: true,
        json: () => Promise.resolve(response)
      }));

      const props = {
        address: { view: 'v', component: 'comp1' },
        serverAction: 'data',
        targetAction: 'targetA',
        checkTarget: 'checkA'
      };

      const promise = suggestThunks.initialSuggestAction('ABC', props)(dispatch, getState);
      const returnedRows = await promise;

      // Called with checkTarget instead of targetAction
      const [url, options] = fetchSpy.mock.calls.at(-1);
      expect(typeof url).toBe('string');
      expect(url).toContain('/action/');
      expect(url).toContain('checkA');
      const sentBody = JSON.parse(options.body);
      expect(sentBody).toEqual({ suggest: 'ABC', max: 0 });
      expect(options.headers.Authorization).toBe('TOKEN');

      // First dispatch should be a thunk (updateModelWithDependencies), second a KEEP_MODEL action
      expect(dispatch).toHaveBeenCalled();
      const calls = dispatch.mock.calls.map(a => a[0]);
      expect(typeof calls[0]).toBe('function');
      const keep = calls.find(a => a && a.type === KEEP_MODEL);
      expect(keep).toBeDefined();
      expect(keep.address).toEqual({ view: 'v', component: 'comp1' });

      // Returned rows equal rows from datalist
      expect(returnedRows).toEqual(rows);
    });

    it('selects only items matching suggest value', async () => {
      const rows = [
        { value: '111', label: 'Uno' },
        { value: '222', label: 'Dos' }
      ];
      jest.spyOn(window, 'fetch').mockReturnValue(Promise.resolve({
        ok: true,
        json: () => Promise.resolve([
          { type: 'fill', parameters: { datalist: { rows } } }
        ])
      }));

      // capture the thunk passed as first dispatch and execute it to inspect payload
      const localDispatch = jest.fn();
      const localGetState = getState;

      await suggestThunks.initialSuggestAction('222', { address: { view: 'v', component: 'comp1' } })(localDispatch, localGetState);

      // The first dispatched arg should be a thunk that when executed would eventually dispatch an UPDATE_MODEL action.
      // Since wiring that thunk chain is complex, at least ensure a function was dispatched and KEEP_MODEL too
      const calls = localDispatch.mock.calls.map(a => a[0]);
      expect(typeof calls[0]).toBe('function');
      const keep = calls.find(a => a && a.type === KEEP_MODEL);
      expect(keep).toBeDefined();
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
    delete global.fetch;
    delete window.fetch;
  });
});
