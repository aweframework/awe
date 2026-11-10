import {getBreadcrumbLevels, originOf} from '../../../src/utilities/breadcrumbs';

describe('awe-react-client/test/js/utilities/breadcrumbsTest.js (#938)', () => {
  const settings = {label: 'Settings', name: 'settings', group: true};
  const users = {label: 'Users', name: 'users', actions: [{type: 'screen'}]};
  const report = {option: 'users', title: 'Users'};

  it('shows the origin screens before the screen opened from them', () => {
    const origin = [settings, {label: 'Users', path: '/users'}];

    const levels = getBreadcrumbLevels(origin, {items: [], option: null}, {option: 'detail', title: 'Detail'}, '/detail');

    expect(levels).toEqual([...origin, {label: 'Detail'}]);
  });

  it('drops an origin that comes back to the screen it left, as browser back does, instead of repeating its menu path', () => {
    const origin = [settings, {label: 'Users', path: '/users'}];

    const levels = getBreadcrumbLevels(origin, {items: [settings, users], option: 'users'}, report, '/users');

    expect(levels).toEqual([settings, users]);
  });

  it('cuts the origin at the screen the user came back to, keeping the screens before it', () => {
    const origin = [{label: 'Home page', path: '/home'}, {label: 'Users', path: '/users'}];

    const levels = getBreadcrumbLevels(origin, {items: [], option: null}, {option: 'x', title: 'Users'}, '/users');

    expect(levels).toEqual([{label: 'Home page', path: '/home'}, {label: 'Users'}]);
  });

  describe('screen opened from a launcher that is on its own menu path', () => {
    const launcher = {label: 'Launcher', name: 'launcher', actions: [{type: 'screen'}]};
    const sales = {label: 'Sales', name: 'sales', actions: [{type: 'screen'}]};
    const reports = {label: 'Reports', name: 'reports', actions: [{type: 'screen'}]};

    it('shows only the menu path when the screen it was opened from is on that path', () => {
      const origin = [settings, launcher, {label: 'Sales', option: 'sales', path: '/sales'}];

      const levels = getBreadcrumbLevels(origin, {items: [settings, launcher, sales, reports], option: 'reports'}, {option: 'reports', title: 'Reports'}, '/reports');

      expect(levels).toEqual([settings, launcher, sales, reports]);
    });

    it('keeps the trail when the screen it was opened from is not on the menu path', () => {
      const origin = [{label: 'Home page', option: 'home', path: '/home'}];

      const levels = getBreadcrumbLevels(origin, {items: [settings, users], option: 'users'}, report, '/users');

      expect(levels).toEqual([...origin, settings, users]);
    });

    it('keeps the option of the screen that is left in the level that goes back to it', () => {
      expect(originOf([settings, {label: 'Users'}], '/users', 'users')).toEqual([settings, {label: 'Users', path: '/users', option: 'users'}]);
    });
  });
});
