import MenuRegistry from '../../../../src/redux/registry/MenuRegistry';
import SizeRegistry from '../../../../src/redux/registry/SizeRegistry';
import ViewRegistry from '../../../../src/redux/registry/ViewRegistry';

describe('awe-react-client/test/js/redux/registry/registriesTest.js', () => {
  describe('MenuRegistry', () => {
    afterEach(() => MenuRegistry.clear());

    it('marks every option, nested ones included, as allowed when it is visible and not restricted', () => {
      MenuRegistry.setOptions([
        {name: 'home', visible: true, options: [{name: 'child', visible: true, restricted: true}, {name: 'hidden'}]},
        {name: 'locked', visible: true, restricted: true}
      ]);
      const [home, locked] = MenuRegistry.getOptions();
      expect(home.allowed).toBe(true);
      expect(home.options.map(option => option.allowed)).toEqual([false, undefined]);
      expect(home.options[0].options).toEqual([]);
      expect(locked.allowed).toBe(false);
    });

    it('clears the options when none is given', () => {
      MenuRegistry.setOptions([{name: 'home', visible: true}]);
      MenuRegistry.setOptions();
      expect(MenuRegistry.getOptions()).toEqual([]);
    });

    it('keeps the breadcrumbs with the key of the current option and resets them on clear', () => {
      expect(MenuRegistry.getBreadcrumbs()).toEqual({items: [], option: null});
      MenuRegistry.setBreadcrumbs('home', [{name: 'home'}]);
      expect(MenuRegistry.getBreadcrumbs()).toEqual({items: [{name: 'home'}], option: 'home'});
      MenuRegistry.clear();
      expect(MenuRegistry.getBreadcrumbs()).toEqual({items: [], option: null});
    });

    it('notifies the subscribers until they unsubscribe', () => {
      const listener = jest.fn();
      const unsubscribe = MenuRegistry.subscribe(listener);
      MenuRegistry.setOptions([]);
      MenuRegistry.setBreadcrumbs('home', []);
      MenuRegistry.clear();
      expect(listener).toHaveBeenCalledTimes(3);
      unsubscribe();
      MenuRegistry.clear();
      expect(listener).toHaveBeenCalledTimes(3);
    });
  });

  describe('SizeRegistry', () => {
    it('merges the measures and notifies the subscribers until they unsubscribe', () => {
      const listener = jest.fn();
      const unsubscribe = SizeRegistry.subscribe(listener);
      SizeRegistry.setSize({width: 10});
      SizeRegistry.setSize({height: 20});
      expect(SizeRegistry.getSize()).toMatchObject({width: 10, height: 20});
      expect(listener).toHaveBeenCalledTimes(2);
      unsubscribe();
      SizeRegistry.setSize({width: 30});
      expect(listener).toHaveBeenCalledTimes(2);
    });
  });

  describe('ViewRegistry', () => {
    afterEach(() => {
      ViewRegistry.clearAll();
      ViewRegistry.setCurrentView('base');
    });

    it('starts with the base and report views loading and base as the current view', () => {
      expect(ViewRegistry.get('base')).toEqual({loading: true});
      expect(ViewRegistry.get('report')).toEqual({loading: true});
      expect(ViewRegistry.get('other')).toBeUndefined();
      expect(ViewRegistry.getCurrentView()).toBe('base');
    });

    it('replaces, updates and clears a view', () => {
      ViewRegistry.setView('report', {title: 'Report', loading: false});
      ViewRegistry.updateView('report', {loading: true});
      expect(ViewRegistry.get('report')).toEqual({title: 'Report', loading: true});
      ViewRegistry.updateView('new', {title: 'New'});
      expect(ViewRegistry.get('new')).toEqual({title: 'New'});
      ViewRegistry.clearView('report');
      expect(ViewRegistry.get('report')).toEqual({loading: true});
    });

    it('changes the current view and clears every view', () => {
      ViewRegistry.setCurrentView('report');
      ViewRegistry.setView('extra', {loading: false});
      ViewRegistry.clearAll();
      expect(ViewRegistry.getCurrentView()).toBe('report');
      expect(ViewRegistry.get('extra')).toBeUndefined();
      expect(ViewRegistry.get('base')).toEqual({loading: true});
    });

    it('notifies the subscribers until they unsubscribe', () => {
      const listener = jest.fn();
      const unsubscribe = ViewRegistry.subscribe(listener);
      ViewRegistry.setCurrentView('report');
      ViewRegistry.clearAll();
      expect(listener).toHaveBeenCalledTimes(2);
      unsubscribe();
      ViewRegistry.clearAll();
      expect(listener).toHaveBeenCalledTimes(2);
    });
  });
});
