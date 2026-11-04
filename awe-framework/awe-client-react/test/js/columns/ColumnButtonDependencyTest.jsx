import {initializeDependencies} from '../../../src/redux/actions/dependencies';
import {UPDATE_MULTIPLE_ATTRIBUTES} from '../../../src/redux/actions/components';
import {Columns} from '../../../src/utilities/structure';
import {renderWithProviders} from '../test-utils';
import {createStore} from '../../../src/redux/store';
import {updateViewComponentsWithDependencies} from '../../../src/redux/thunks/components';
import {fillAction} from '../../../src/redux/thunks/form';
import {getComponent} from '../../../src/utilities';
import {getAllComponents} from '../../../src/redux/selectors/componentSelectors';

/**
 * As AngularJS, a button is rendered in every row of its column, and each one evaluates the dependencies of its column
 * with its own row: a "currentRowValue" element reads the cell of the row of the button (#791, S14)
 */
describe('awe-react-client/test/js/columns/ColumnButtonDependencyTest.jsx', () => {
  const view = 'base';
  const grid = {view, component: 'GrdAcc'};

  const enableUnlessAllowed = {
    initial: true,
    target: 'enable',
    elements: [{id: 'GrdAcc', column1: 'access', attribute1: 'currentRowValue', condition: 'ne', value: 'allowed'}]
  };

  const buildState = (column) => ({
    settings: {activeDependencies: true},
    runtime: {},
    components: {
      GrdAcc: {
        address: grid,
        attributes: {
          component: 'treegrid',
          gridId: 'id',
          columnModel: [{id: 'access', name: 'access'}, {id: 'AlwBtn', name: 'AlwBtn', dependencies: [enableUnlessAllowed], ...column}]
        },
        validationRules: {},
        model: {
          values: [
            {id: '1', access: 'allowed', $row: {}},
            {id: '2', access: 'denied', $row: {editing: true}},
            {id: '3', access: 'allowed', $row: {}}
          ]
        },
        dependencies: []
      }
    }
  });

  // The cell attributes the dependencies give, by row
  const cellAttributes = (column) => {
    const dispatch = jest.fn();
    initializeDependencies(view, buildState(column), dispatch);
    return dispatch.mock.calls
      .map(([action]) => action)
      .filter(action => action.type === UPDATE_MULTIPLE_ATTRIBUTES)
      .flatMap(action => action.componentList)
      .reduce((cells, {address, data}) => ({...cells, [address.row]: {...cells[address.row], ...data}}), {});
  };

  const isButtonDisabled = (attrs, row) => {
    const {container, unmount} = renderWithProviders(Columns({
      component: 'button', name: 'AlwBtn', icon: 'lucide:check', title: 'Allow', actions: [],
      address: {...grid, column: 'AlwBtn', row}
    }, {}, attrs || {}, false));
    const disabled = container.querySelector('button').disabled;
    unmount();
    return disabled;
  };

  beforeEach(() => {
    jest.spyOn(console, 'warn').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('disables the button of a column only in the rows whose cell matches its currentRowValue dependency', () => {
    const cells = cellAttributes({component: 'button'});

    expect(['1', '2', '3'].map(row => isButtonDisabled(cells[row], row))).toEqual([true, false, true]);
  });

  it('keeps the currentRowValue dependency of a column without a cell component on the editing row only', () => {
    expect(cellAttributes({})).toEqual({'2': {disabled: false}});
  });

  describe('when the rows are loaded after the screen', () => {
    let store;
    const fill = (rows) => ({type: 'fill', address: grid, parameters: {datalist: {rows}}});
    // The rows of a tree are identified by its tree id, and they may hold an "id" field with another value
    const disabledByRow = () => Object.fromEntries(getComponent(getAllComponents(store.getState()), grid).model.values
      .map(row => [row['option-id'], row.$attrs?.AlwBtn?.disabled]));

    beforeEach(() => {
      store = createStore();
      store.dispatch(updateViewComponentsWithDependencies(view, {
        GrdAcc: {
          address: grid,
          context: {view, source: [view, 'GrdAcc']},
          attributes: {
            component: 'treegrid',
            treegrid: true,
            treeId: 'option-id',
            columnModel: [{id: 'access', name: 'access'}, {id: 'AlwBtn', name: 'AlwBtn', component: 'button', dependencies: [enableUnlessAllowed]}]
          },
          model: {values: []}
        }
      }));
    });

    it('disables the button of the matching rows once they are loaded, and again when the same rows are reloaded', () => {
      const rows = () => [{id: 1, 'option-id': 'dashboard', access: 'allowed'}, {id: 2, 'option-id': 'tools', access: 'denied'}];

      store.dispatch(fillAction(fill(rows())));
      expect(disabledByRow()).toEqual({dashboard: true, tools: false});

      store.dispatch(fillAction(fill(rows())));
      expect(disabledByRow()).toEqual({dashboard: true, tools: false});
    });
  });
});
