import ComponentRegistry from '../../../../../src/redux/registry/ComponentRegistry';
import {components} from '../../../../../src/redux/reducers/components';
import {getMergedComponent, getKeepModelComponent} from '../../../../../src/redux/reducers/components/stateHelpers';
import {validateComponent, validateRow} from '../../../../../src/redux/reducers/validation';
import {
  VALIDATE_COMPONENTS,
  VALIDATE_ROW,
  keepAttribute,
  registerComponents,
  restoreMultipleAttributes,
  updateAttributes,
  updateMultipleValidation
} from '../../../../../src/redux/actions/components';

const address = {view: 'base', component: 'name'};
const settings = {passwordPattern: '.*'};

const criterion = (rules = {}) => ({
  address,
  attributes: {component: 'text', size: 'md'},
  model: {values: [{value: 'abc', selected: true}]},
  validationRules: rules
});

describe('awe-react-client/test/js/redux/reducers/components/componentsHandlersTest.js', () => {
  beforeEach(() => {
    ComponentRegistry.clearAll();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  describe('multiple actions through the reducer', () => {
    it('restores several kept attributes', () => {
      ComponentRegistry.register('name', criterion());
      let state = components({}, updateAttributes(address, {style: {color: 'red'}}));
      state = components(state, keepAttribute(address, 'style'));
      state = components(state, updateAttributes(address, {style: {color: 'green'}}));
      expect(getMergedComponent('name', state).attributes.style).toEqual({color: 'green'});

      state = components(state, restoreMultipleAttributes([{address, data: 'style'}]));

      expect(getMergedComponent('name', state).attributes.style).toEqual({color: 'red'});
    });

    it('updates the validation rules of several components with the settings of the action', () => {
      ComponentRegistry.register('name', criterion({required: true}));

      const state = components({}, {...updateMultipleValidation([{address, data: {text: true}}]), settings});

      expect(getMergedComponent('name', state).validationRules).toEqual({required: true, text: true});
    });

    it('validates a list of components and sets their error', () => {
      const component = {...criterion({required: {value: true}}), model: {values: []}};
      ComponentRegistry.register('name', component);

      const state = components({}, {type: VALIDATE_COMPONENTS, settings, componentList: [component]});

      expect(getMergedComponent('name', state).attributes.error).toMatchObject({message: 'VALIDATOR_MESSAGE_REQUIRED'});
    });

    it('validates a row of a grid', () => {
      const grid = {
        address: {view: 'base', component: 'grid'},
        attributes: {columnModel: [{name: 'col', component: 'text', validationRules: {required: true}}]},
        model: {values: [{id: 1, col: null}]}
      };
      ComponentRegistry.register('grid', grid);

      const state = components({}, {type: VALIDATE_ROW, settings, address: {...grid.address, row: 1}});

      expect(getMergedComponent('grid', state).model.values[0].$attrs.col).toHaveProperty('error');
    });

    it('registers components that have no address and keeps them', () => {
      const warn = jest.spyOn(console, 'warn').mockImplementation(() => {});

      const state = components({}, registerComponents({orphan: {attributes: {component: 'text'}}}));

      expect(Object.keys(state)).toEqual(['orphan']);
      expect(warn).toHaveBeenCalledWith(expect.stringContaining('[AWE] Malformed component detected'), expect.anything());
    });
  });

  describe('validation edge cases', () => {
    it('reports a rule method that does not exist and goes on', () => {
      const error = jest.spyOn(console, 'error').mockImplementation(() => {});
      const state = {name: {...criterion({unknownRule: true})}};

      const result = validateComponent(state, state.name, settings);

      expect(error).toHaveBeenCalled();
      expect(result.name.attributes.error).toBeUndefined();
    });

    it('leaves the state as it is when the row is not in the grid', () => {
      ComponentRegistry.register('grid', {
        address: {view: 'base', component: 'grid'},
        attributes: {columnModel: []},
        model: {values: [{id: 1}]}
      });
      const state = {};

      expect(validateRow(state, {view: 'base', component: 'grid', row: 9}, settings)).toBe(state);
    });

    it('leaves the state as it is when the grid has no model', () => {
      const state = {};

      expect(validateRow(state, {view: 'base', component: 'missing', row: 1}, settings)).toBe(state);
    });
  });

  describe('snapshots of unregistered components', () => {
    it('keeps an initial model with the default one given', () => {
      const state = {name: {model: {values: [{value: 'a'}]}}};

      const update = getKeepModelComponent(state, 'name', settings, true, {values: [{value: 'd'}]});

      expect(update.name.defaultModel.values).toEqual([{value: 'd'}]);
      expect(update.name.storedModel.values).toEqual([{value: 'a'}]);
    });

    it('keeps an initial model of a registered component with the default one given', () => {
      ComponentRegistry.register('name', criterion());

      const update = getKeepModelComponent({}, 'name', settings, true, {values: [{value: 'd'}]});

      expect(update.name.defaultModel.values).toEqual([{value: 'd'}]);
    });
  });
});
