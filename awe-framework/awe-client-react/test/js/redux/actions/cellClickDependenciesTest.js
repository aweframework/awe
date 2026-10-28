import {checkDependencies, initializeDependencies} from '../../../../src/redux/actions/dependencies';
import {serverDownloadAction} from '../../../../src/redux/thunks/server';
import {ACCEPT_ACTION, ADD_ACTIONS, ADD_ACTIONS_TOP} from '../../../../src/redux/actions/actions';

const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0));

/**
 * #880: a click in a grid cell only launches the dependencies of the column clicked, in its row, as AngularJS
 * (the launcher of an element with a column and a row is the cell). A server-download launched from the cell resolves
 * the grid and downloads the file.
 */
describe('awe-react-client/test/js/redux/actions/cellClickDependenciesTest.js', () => {
  const view = 'base';
  const grid = {view, component: 'GrdSta'};
  let dispatch;

  const downloadDependency = {
    elements: [{id: 'GrdSta', column1: 'DwnFil', attribute1: 'selectedRowValue', event: 'click'}],
    actions: [{type: 'server-download', parameters: {serverAction: 'get-file-maintain', targetAction: 'downloadFileGrid'}}]
  };

  const cellClickDependency = {
    elements: [{id: 'GrdSta', column1: 'Btn', attribute1: 'currentRow', event: 'click'}],
    actions: [{type: 'message', parameters: {}}]
  };

  const buildState = (lastEvent) => ({
    settings: {activeDependencies: true, token: 'token', targetActionKey: 'targetAction'},
    runtime: {lastEvent},
    components: {
      GrdSta: {
        address: grid,
        attributes: {
          component: 'grid',
          gridId: 'id',
          columnModel: [
            {id: 'Btn', name: 'Btn', dependencies: [cellClickDependency]},
            {id: 'TxtViw', name: 'TxtViw'},
            {id: 'DwnFil', name: 'DwnFil', dependencies: [downloadDependency]}
          ]
        },
        specificAttributes: {},
        model: {
          values: [
            {id: '1', Btn: 'b1', TxtViw: 't1', DwnFil: 'file1.txt'},
            {id: '2', Btn: 'b2', TxtViw: 't2', DwnFil: 'file2.txt', selected: true}
          ]
        },
        dependencies: []
      }
    }
  });

  const launchedActions = () => dispatch.mock.calls
    .map(([action]) => action)
    .filter(action => [ADD_ACTIONS, ADD_ACTIONS_TOP].includes(action.type))
    .flatMap(action => action.payload.flat(2));

  const click = (column, row) => {
    initializeDependencies(view, buildState(undefined), jest.fn());
    checkDependencies(buildState({event: 'click', address: {...grid, column, row}}), dispatch);
  };

  beforeEach(() => {
    dispatch = jest.fn();
    jest.spyOn(console, 'info').mockImplementation(() => {});
    jest.spyOn(console, 'log').mockImplementation(() => {});
    jest.spyOn(console, 'warn').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('does not launch the dependency of another column when a cell is clicked', () => {
    click('TxtViw', '2');

    expect(launchedActions()).toEqual([]);
  });

  it('only launches the dependency of the column of the button clicked', () => {
    click('Btn', '2');

    const types = launchedActions().map(action => action.type);
    expect(types).not.toContain('server-download');
    expect(types).toEqual(['message']);
  });

  it('launches the dependency of a column once, for the row of the cell clicked', () => {
    click('Btn', '1');

    const launched = launchedActions();
    expect(launched).toHaveLength(1);
    expect(launched[0].address).toEqual({...grid, column: 'Btn', row: '1'});
  });

  it('does not launch the download from a row that is not the selected one', () => {
    // As AngularJS, the launcher of a "selectedRowValue" element is the cell of the selected row: the download of
    // another row would send the file of the selected one
    click('DwnFil', '1');

    expect(launchedActions()).toEqual([]);
  });

  it('launches the download of the column clicked and downloads the file', async () => {
    click('DwnFil', '2');

    const downloads = launchedActions().filter(action => action.type === 'server-download');
    expect(downloads).toHaveLength(1);

    global.fetch = jest.fn().mockResolvedValue({
      headers: {get: () => 'file2.txt'},
      blob: () => Promise.resolve(new Blob(['content']))
    });
    window.fetch = global.fetch;
    window.URL.createObjectURL = jest.fn().mockReturnValue('blob:file');
    jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
    const download = jest.fn();

    serverDownloadAction(downloads[0])(download, () => buildState(undefined));
    await flushPromises();

    expect(global.fetch).toHaveBeenCalledWith(expect.stringContaining('downloadFileGrid'), expect.anything());
    expect(download).toHaveBeenCalledWith(expect.objectContaining({type: ACCEPT_ACTION}));
  });
});
