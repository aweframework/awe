import { columnValuesAction } from '../../../../src/redux/thunks/columnFilters';
import { ADD_ACTIONS_TOP } from '../../../../src/redux/actions/actions';
import { DEFAULT_SETTINGS } from '../../../../src/redux/actions/settings';

describe('awe-react-client/test/js/redux/thunks/columnFiltersThunkTest.js', () => {
  const address = { view: 'report', component: 'GrdUsr' };
  const columnFilters = [
    { id: 'Nam', type: 'text', conditions: [{ operator: 'contains', value: 'ana' }] },
    { id: 'ProTxt', fields: ['Pro'], type: 'values', values: ['ADM'] }
  ];
  let dispatch;
  let getState;

  beforeEach(() => {
    dispatch = jest.fn();
    const state = {
      settings: { ...DEFAULT_SETTINGS, token: 'TOKEN' },
      components: {
        GrdUsr: {
          address,
          attributes: { targetAction: 'UsrLst', columnModel: [] },
          model: { values: [] },
          specificAttributes: { page: 3, max: 30, sort: [], columnFilters }
        }
      }
    };
    getState = jest.fn(() => state);
    global.fetch = jest.fn();
    window.fetch = global.fetch;
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  const answer = (response) => jest.spyOn(window, 'fetch').mockReturnValue(Promise.resolve({
    ok: true,
    json: () => Promise.resolve(response)
  }));

  it('asks the column-values action of the grid query with the grid filters and the column, its fields, type and search', async () => {
    const fetchSpy = answer([{ type: 'column-values', parameters: { datalist: { rows: [] }, more: false } }]);
    const signal = new AbortController().signal;

    await columnValuesAction(address, { name: 'ProTxt', filter: 'values', filterFields: 'Pro' }, ' adm ', signal)(dispatch, getState);

    const [url, options] = fetchSpy.mock.calls[0];
    expect(url).toContain('/action/column-values/UsrLst');
    expect(options.signal).toBe(signal);
    const body = JSON.parse(options.body);
    // Every column filter goes: the server leaves out the filter of the column itself
    expect(body.columnFilters).toEqual(columnFilters);
    expect(body.columnValues).toEqual({ id: 'ProTxt', fields: ['Pro'], type: 'values', search: 'adm' });
  });

  it('sends no search when the search is blank', async () => {
    const fetchSpy = answer([{ type: 'column-values', parameters: { datalist: { rows: [] }, more: false } }]);

    await columnValuesAction(address, { name: 'Nam', filter: 'text', sortField: 'NamSrt' }, '  ')(dispatch, getState);

    expect(JSON.parse(fetchSpy.mock.calls[0][1].body).columnValues).toEqual({ id: 'Nam', fields: ['NamSrt'], type: 'text' });
  });

  it('answers the values and whether there are more, and launches the other actions of the answer', async () => {
    const rows = [{ value: 'ADM', label: 'Administrator', count: 2 }, { value: null, label: null, count: 1 }];
    const message = { type: 'message', parameters: { type: 'warning', message: 'Too many' } };
    answer([{ type: 'column-values', parameters: { datalist: { rows }, more: 'true' } }, message, { type: 'end-load' }]);

    const result = await columnValuesAction(address, { name: 'ProTxt', filter: 'values' })(dispatch, getState);

    expect(result).toEqual({ values: rows, more: true });
    const launched = dispatch.mock.calls.map(([action]) => action).filter(action => action.type === ADD_ACTIONS_TOP);
    expect(launched.map(action => action.payload)).toEqual([[message]]);
  });

  it('answers no values when the request fails', async () => {
    jest.spyOn(window, 'fetch').mockReturnValue(Promise.resolve({ ok: false, status: 500, text: () => Promise.resolve('Error') }));

    const result = await columnValuesAction(address, { name: 'ProTxt', filter: 'values' })(dispatch, getState);

    expect(result).toEqual({ values: [], more: false });
  });
});
