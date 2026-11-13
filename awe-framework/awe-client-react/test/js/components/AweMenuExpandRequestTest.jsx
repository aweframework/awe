import React from 'react';
import {act, cleanup} from '@testing-library/react';
import {DEFAULT_SETTINGS} from '../../../src/redux/actions/settings';
import {renderWithProviders} from '../test-utils';
import AweMenu from '../../../src/components/AweMenu';
import MenuRegistry from '../../../src/redux/registry/MenuRegistry';
import ViewRegistry from '../../../src/redux/registry/ViewRegistry';

const option = (name, options = [], actions = []) => ({
  name, id: name, label: name, visible: true, restricted: false, separator: false, menuScreen: false, actions, options
});
// admin > security > users, and a sibling group
const options = [
  option('admin', [option('security', [option('users', [], [{type: 'screen', target: 'users'}])])]),
  option('tools', [option('sites', [], [{type: 'screen', target: 'sites'}])])
];

describe('awe-react-client/test/js/components/AweMenuExpandRequestTest.jsx (#938)', () => {
  let matchMedia;

  beforeEach(() => {
    matchMedia = window.matchMedia;
    window.matchMedia = jest.fn(query => ({matches: query === '(min-width: 1025px)', media: query}));
    MenuRegistry.setOptions(options);
    // The open screen is not under the group the breadcrumb asks for
    ViewRegistry.setView('report', {option: 'outside-the-menu', title: 'Outside'});
  });

  afterEach(() => {
    // Unmount first: the registries are global and reset after the menu stops listening
    cleanup();
    window.matchMedia = matchMedia;
    MenuRegistry.clear();
    ViewRegistry.setView('report', {loading: true});
  });

  const desktop = matches => {
    window.matchMedia = jest.fn(query => ({matches: matches && query === '(min-width: 1025px)', media: query}));
  };
  const layout = () => document.querySelector('.layout-container');

  const renderMenu = (collapsed = false, style = 'vertical') => renderWithProviders(
    <div className={collapsed ? 'layout-container layout-static-inactive' : 'layout-container'}>
      <AweMenu id="menu" style={style}/>
    </div>, {
      preloadedState: {
        settings: {...DEFAULT_SETTINGS, menuSearchEnabled: false},
        screen: {size: {width: 100, height: 100}},
        components: {menu: {address: {component: 'menu', view: 'base'}, model: {values: []}, attributes: {}}}
      }
    });
  const header = name => document.querySelector(`.p-panelmenu-panel.${name} > .p-panelmenu-header`);
  const nested = name => document.querySelector(`.p-panelmenu-panel.admin li.${name}`);

  it('expands the group the breadcrumb asks for, with the groups above it', () => {
    renderMenu();
    expect(header('admin')).not.toHaveAttribute('aria-expanded', 'true');

    act(() => MenuRegistry.requestExpand('security'));

    expect(header('admin')).toHaveAttribute('aria-expanded', 'true');
    expect(nested('security')).not.toBeNull();
    expect(header('tools')).not.toHaveAttribute('aria-expanded', 'true');
  });

  it('expands the menu again when the same group is asked for after the user collapsed it', () => {
    renderMenu();
    act(() => MenuRegistry.requestExpand('admin'));
    act(() => header('admin').click());
    expect(header('admin')).not.toHaveAttribute('aria-expanded', 'true');

    act(() => MenuRegistry.requestExpand('admin'));

    expect(header('admin')).toHaveAttribute('aria-expanded', 'true');
  });

  it('opens the collapsed rail so the group shows', () => {
    renderMenu(true);

    act(() => MenuRegistry.requestExpand('security'));

    expect(document.querySelector('.layout-container')).not.toHaveClass('layout-static-inactive');
  });

  it('opens the drawer on a narrow viewport, where the class opens the menu instead of collapsing it', () => {
    desktop(false);
    renderMenu(false);

    act(() => MenuRegistry.requestExpand('security'));

    expect(layout()).toHaveClass('layout-static-inactive');
    expect(header('admin')).toHaveAttribute('aria-expanded', 'true');
  });

  it('keeps the drawer open on a narrow viewport when it already is', () => {
    desktop(false);
    renderMenu(true);

    act(() => MenuRegistry.requestExpand('security'));

    expect(layout()).toHaveClass('layout-static-inactive');
  });

  it('leaves an expanded menu on a desktop as it is', () => {
    renderMenu(false);

    act(() => MenuRegistry.requestExpand('security'));

    expect(layout()).not.toHaveClass('layout-static-inactive');
  });

  it('is a menu a group can be opened in only while the side menu is mounted, not for the horizontal menu', () => {
    const {unmount} = renderMenu(false, 'horizontal');
    expect(MenuRegistry.hasExpander()).toBe(false);
    unmount();

    const vertical = renderMenu(false);
    expect(MenuRegistry.hasExpander()).toBe(true);
    vertical.unmount();
    expect(MenuRegistry.hasExpander()).toBe(false);
  });

  it('makes a menu screen with options a level that launches it, as the menu shows it as a leaf', () => {
    const launcher = {...option('reports', [option('monthly', [], [{type: 'screen', target: 'monthly'}])],
      [{type: 'screen', target: 'reports'}]), menuScreen: true};
    MenuRegistry.setOptions([option('sales', [launcher])]);
    ViewRegistry.setView('report', {option: 'monthly', title: 'Monthly'});

    renderMenu();

    const levels = MenuRegistry.getBreadcrumbs().items;
    expect(levels.map(level => [level.name, level.group])).toEqual([['sales', true], ['reports', false], ['monthly', false]]);
    expect(levels[1].actions).toEqual([{type: 'screen', target: 'reports'}]);
  });
});
