import React from 'react';
import {act, cleanup, fireEvent, screen, within} from '@testing-library/react';
import {renderWithProviders} from '../test-utils';
import WindowTemplate from '../../../src/templates/WindowTemplate';
import MenuRegistry from '../../../src/redux/registry/MenuRegistry';
import ViewRegistry from '../../../src/redux/registry/ViewRegistry';
import {DEFAULT_SETTINGS} from '../../../src/redux/actions/settings';
import {navigationActions} from '../../../src/redux/actions/navigation';

const elementList = [
  {source: 'center', elementList: []},
  {source: 'modal', elementList: []},
  {source: 'hidden', elementList: []},
  {source: 'buttons', elementList: []}
];
const launchActions = [{type: 'screen', target: 'users'}];
// Menu path of the open screen: a group, a nested group and the option
const crumbs = [
  {label: 'Settings', name: 'settings', group: true, actions: []},
  {label: 'Security', name: 'security', group: true, actions: []},
  {label: 'Users', name: 'users', group: false, actions: launchActions}
];

describe('awe-react-client/test/js/templates/WindowTemplateBreadcrumbTest.jsx (#938)', () => {
  beforeEach(() => {
    jest.spyOn(navigationActions, 'navigateTo').mockImplementation(target => ({type: 'NAVIGATE_TO', payload: target}));
  });

  afterEach(() => {
    // Unmount first: the registries are global and reset after the component stops listening
    cleanup();
    detach?.();
    jest.restoreAllMocks();
    MenuRegistry.clear();
    ViewRegistry.setView('report', {loading: true});
  });

  // The menu the levels come from: a group of groups and the options the tests use
  const menuOption = (name, options = []) => ({name, id: name, label: name, visible: true, restricted: false, options, actions: []});
  const menu = [menuOption('settings', [menuOption('security', [menuOption('users')])]), menuOption('reports')];
  let detach;

  const renderHeader = (levels = crumbs, {origin = [], title = 'Users', location = null, vertical = true} = {}) => {
    MenuRegistry.setOptions(menu);
    detach = vertical ? MenuRegistry.attachExpander() : null;
    MenuRegistry.setLocation(location);
    ViewRegistry.setView('report', {option: 'users', title});
    MenuRegistry.setBreadcrumbs('users', levels);
    MenuRegistry.setOrigin(origin);
    return renderWithProviders(<WindowTemplate elementList={elementList}/>, {
      spyDispatch: true,
      preloadedState: {settings: {...DEFAULT_SETTINGS, initialURL: '/start'}}
    });
  };
  const nav = () => screen.getByRole('navigation', {name: 'SCREEN_TEXT_BREADCRUMB'});
  const dispatched = spy => spy.mock.calls.map(([call]) => call);

  describe('navigable levels', () => {
    it('wraps the breadcrumb in a labelled navigation landmark', () => {
      renderHeader();

      expect(nav()).toHaveAttribute('data-testid', 'breadcrumb');
    });

    it('shows the current page as plain text marked with aria-current, not as a link', () => {
      renderHeader();

      const current = within(nav()).getByText('Users');
      expect(current).toHaveAttribute('aria-current', 'page');
      expect(current).toHaveAttribute('data-testid', 'breadcrumb-level');
      expect(current).toHaveAttribute('data-active', 'true');
      expect(current.closest('a, button')).toBeNull();
    });

    it('goes to the start screen from the home level', () => {
      const {dispatchSpy} = renderHeader();

      fireEvent.click(within(nav()).getByRole('link', {name: 'BUTTON_HOME'}));

      expect(dispatched(dispatchSpy)).toContainEqual({type: 'NAVIGATE_TO', payload: '/start'});
    });

    it('launches an option level as the menu does, marking its actions as coming from the menu', () => {
      const {dispatchSpy} = renderHeader([
        {label: 'Settings', name: 'settings', group: true, actions: []},
        {label: 'Reports', name: 'reports', group: false, actions: launchActions},
        {label: 'Detail', name: 'detail', group: false, actions: []}
      ]);

      fireEvent.click(within(nav()).getByRole('link', {name: 'Reports'}));

      const added = dispatched(dispatchSpy).find(call => call.type === 'ADD_ACTIONS_TOP');
      expect(added.payload).toEqual([{...launchActions[0], origin: 'menu'}]);
      expect(dispatched(dispatchSpy).map(call => call.type)).toContain('DELETE_STACK');
    });

    it('asks the menu to open expanded at a group level', () => {
      renderHeader();

      fireEvent.click(within(nav()).getByRole('link', {name: 'Security'}));

      expect(MenuRegistry.getExpandRequest().name).toBe('security');
    });

    it('makes every level but the current page reachable with the keyboard', () => {
      renderHeader();

      const links = within(nav()).getAllByRole('link').map(link => link.textContent || link.getAttribute('aria-label'));
      expect(links).toEqual(['BUTTON_HOME', 'Settings', 'Security']);
    });
  });

  describe('collapse of long paths', () => {
    const level = (name, group = true) => ({label: name, name, group, actions: group ? [] : launchActions});
    const longPath = [level('One'), level('Two'), level('Three'), level('Four'), level('Five', false)];
    const labels = () => within(nav()).getAllByRole('listitem')
      .filter(item => item.getAttribute('role') !== 'separator').map(item => item.textContent || item.firstChild.getAttribute('aria-label'));

    it('shows every level of a path of up to four levels', () => {
      renderHeader(longPath.slice(0, 4));

      expect(screen.queryByRole('button', {name: 'SCREEN_TEXT_BREADCRUMB_EXPAND'})).toBeNull();
      expect(within(nav()).getByText('Two')).toBeInTheDocument();
    });

    it('collapses a longer path to its first level, an ellipsis and its last two levels', () => {
      renderHeader(longPath);

      expect(labels()).toEqual(['BUTTON_HOME', 'One', '…', 'Four', 'Five']);
      expect(screen.getByRole('button', {name: 'SCREEN_TEXT_BREADCRUMB_EXPAND'})).toHaveAttribute('aria-expanded', 'false');
    });

    it('expands the hidden levels in place from the ellipsis and collapses them again', () => {
      renderHeader(longPath);
      const ellipsis = screen.getByRole('button', {name: 'SCREEN_TEXT_BREADCRUMB_EXPAND'});

      fireEvent.click(ellipsis);
      expect(ellipsis).toHaveAttribute('aria-expanded', 'true');
      expect(labels()).toEqual(['BUTTON_HOME', 'One', '…', 'Two', 'Three', 'Four', 'Five']);

      fireEvent.click(ellipsis);
      expect(labels()).toEqual(['BUTTON_HOME', 'One', '…', 'Four', 'Five']);
    });
  });

  describe('origin screen', () => {
    const origin = [{label: 'Settings', name: 'settings', group: true, actions: []}, {label: 'Users', path: '/users'}];

    it('shows the screens the screen was opened from before it, and the screen itself as the page', () => {
      renderHeader([], {origin, title: 'User detail'});

      expect(within(nav()).getByRole('link', {name: 'Settings'})).toBeInTheDocument();
      expect(within(nav()).getByRole('link', {name: 'Users'})).toBeInTheDocument();
      expect(within(nav()).getByText('User detail')).toHaveAttribute('aria-current', 'page');
    });

    it('goes back to an origin screen and forgets the levels after it', () => {
      const {dispatchSpy} = renderHeader([], {origin, title: 'User detail'});

      fireEvent.click(within(nav()).getByRole('link', {name: 'Users'}));

      expect(dispatched(dispatchSpy)).toContainEqual({type: 'NAVIGATE_TO', payload: '/users'});
      expect(MenuRegistry.getOrigin()).toEqual([origin[0]]);
    });

    it('forgets the origin when a level launches an option from the menu', () => {
      renderHeader(crumbs.slice(0, 2).concat({label: 'Reports', name: 'reports', group: false, actions: launchActions},
        {label: 'Detail', name: 'detail', group: false, actions: []}), {origin});

      fireEvent.click(within(nav()).getByRole('link', {name: 'Reports'}));

      expect(MenuRegistry.getOrigin()).toEqual([]);
    });
  });

  describe('group levels in every layout of the menu', () => {
    it('shows a group as text when no side menu can open at it, as the horizontal menu', () => {
      renderHeader(crumbs, {vertical: false});

      expect(within(nav()).queryByRole('link', {name: 'Security'})).toBeNull();
      expect(within(nav()).getByText('Security')).toBeInTheDocument();
    });

    it('shows a group the menu does not have as text, not as a link that does nothing', () => {
      renderHeader([{label: 'Gone', name: 'gone', group: true, actions: []}, ...crumbs.slice(2)]);

      expect(within(nav()).queryByRole('link', {name: 'Gone'})).toBeNull();
      expect(within(nav()).getByText('Gone')).toBeInTheDocument();
    });
  });

  describe('levels that follow the screen', () => {
    const level = name => ({label: name, name, group: true, actions: []});

    it('collapses the levels again when the screen changes', () => {
      renderHeader([level('One'), level('Two'), level('Three'), level('Four'), {...level('Five'), group: false}]);
      fireEvent.click(screen.getByRole('button', {name: 'SCREEN_TEXT_BREADCRUMB_EXPAND'}));
      expect(screen.getByRole('button', {name: 'SCREEN_TEXT_BREADCRUMB_EXPAND'})).toHaveAttribute('aria-expanded', 'true');

      act(() => MenuRegistry.setBreadcrumbs('users', [level('Six'), level('Seven'), level('Eight'), level('Nine'), level('Ten')]));

      expect(screen.getByRole('button', {name: 'SCREEN_TEXT_BREADCRUMB_EXPAND'})).toHaveAttribute('aria-expanded', 'false');
    });

    it('does not repeat the menu path when browser back returns to the screen the trail came from', () => {
      const origin = [crumbs[0], crumbs[1], {label: 'Users', path: '/users'}];
      renderHeader(crumbs, {origin, location: '/users'});

      expect(within(nav()).getAllByText('Security')).toHaveLength(1);
      expect(within(nav()).getByText('Users')).toHaveAttribute('aria-current', 'page');
    });
  });
});
