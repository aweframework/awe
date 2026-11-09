import React from 'react';
import { renderWithProviders } from '../test-utils';
import WindowTemplate from '../../../src/templates/WindowTemplate';

describe('awe-react-client/test/js/templates/WindowTemplateTest.jsx', () => {

  it('renders WindowTemplate with all sources', () => {
    // Setup preloaded state for Redux
    const preloadedState = {
      screen: {
        breadcrumbs: {
          items: [
            { label: 'Home', url: '/' },
            { label: 'Test', url: '/test' }
          ]
        }
      }
    };

    // Create mock element list with all required sources
    const elementList = [
      { source: 'center', style: 'center-style', elementList: [] },
      { source: 'modal', style: 'modal-style', elementList: [] },
      { source: 'hidden', style: 'hidden-style', elementList: [] },
      { source: 'buttons', style: 'buttons-style', elementList: [] }
    ];

    // Render the component with providers
    const { container } = renderWithProviders(
      <WindowTemplate elementList={elementList} />,
      { preloadedState }
    );

    // Verify the rendered output contains the expected elements
    expect(container.querySelector('.window-buttons')).not.toBeNull();
    expect(container.querySelector('.p-breadcrumb')).not.toBeNull();
    expect(container.querySelector('[data-testid="breadcrumb"]')).toBe(container.querySelector('.p-breadcrumb'));
    expect(container.querySelector('.expand.expandible-vertical.center-style')).not.toBeNull();
  });

  it('renders WindowTemplate with custom styles', () => {
    // Create mock element list with custom styles
    const elementList = [
      { source: 'center', style: 'custom-center-style', elementList: [] },
      { source: 'modal', style: 'custom-modal-style', elementList: [] },
      { source: 'hidden', style: 'custom-hidden-style', elementList: [] },
      { source: 'buttons', style: 'custom-buttons-style', elementList: [] }
    ];

    // Setup preloaded state for Redux
    const preloadedState = {
      screen: {
        breadcrumbs: {
          items: []
        }
      }
    };

    // Render the component with providers
    const { container } = renderWithProviders(
      <WindowTemplate elementList={elementList} />,
      { preloadedState }
    );

    // Verify the rendered output contains the custom styles
    expect(container.querySelector('.window-buttons.custom-buttons-style')).not.toBeNull();
    expect(container.querySelector('.expand.expandible-vertical.custom-center-style')).not.toBeNull();
  });

  it('renders WindowTemplate with breadcrumbs', () => {
    // Setup custom breadcrumbs
    const customBreadcrumbs = {
      items: [
        { label: 'Home', url: '/' },
        { label: 'Products', url: '/products' },
        { label: 'Details', url: '/products/details' }
      ]
    };

    // Create mock element list
    const elementList = [
      { source: 'center', style: 'center-style', elementList: [] },
      { source: 'modal', style: 'modal-style', elementList: [] },
      { source: 'hidden', style: 'hidden-style', elementList: [] },
      { source: 'buttons', style: 'buttons-style', elementList: [] }
    ];

    // Setup preloaded state for Redux
    const preloadedState = {
      screen: {
        breadcrumbs: customBreadcrumbs
      }
    };

    // Render the component with providers
    const { container } = renderWithProviders(
      <WindowTemplate elementList={elementList} />,
      { preloadedState }
    );

    // Verify the breadcrumb is rendered
    expect(container.querySelector('.p-breadcrumb')).not.toBeNull();
  });

  describe('source tags with type and expandible, as in the AngularJS engine', () => {
    const sources = (center) => [
      center,
      { source: 'modal', elementList: [] },
      { source: 'hidden', elementList: [] },
      { source: 'buttons', elementList: [] }
    ];
    const child = {elementType: 'Tag', type: 'span', id: 'inner', label: 'Inner', elementList: []};
    const preloadedState = {screen: {breadcrumbs: {items: []}}};

    it('generates the element of a source tag with its style and expandible direction', () => {
      const { container } = renderWithProviders(
        <WindowTemplate elementList={sources({source: 'center', type: 'section', id: 'centre', style: 'expand wide', expand: 'horizontal', elementList: [child]})} />,
        { preloadedState }
      );

      const element = container.querySelector('section#centre');
      expect(element).not.toBeNull();
      expect(element.className).toBe('expandible-horizontal expand wide');
      expect(element.querySelector('span#inner')).not.toBeNull();
      expect(container.querySelector('.expand.expandible-vertical.wide')).toBeNull();
    });

    it('keeps the children directly in the template when the source tag has no type', () => {
      const { container } = renderWithProviders(
        <WindowTemplate elementList={sources({source: 'center', expand: 'horizontal', elementList: [child]})} />,
        { preloadedState }
      );

      expect(container.querySelector('.expand.expandible-vertical > span#inner')).not.toBeNull();
      expect(container.querySelector('.expandible-horizontal')).toBeNull();
    });
  });

  describe('window header mode (#925)', () => {
    const elementList = [
      { source: 'center', elementList: [] },
      { source: 'modal', elementList: [] },
      { source: 'hidden', elementList: [] },
      { source: 'buttons', elementList: [{elementType: 'Tag', type: 'span', id: 'save', label: 'Save', elementList: []}] }
    ];
    const render = (props, windowHeader) => renderWithProviders(
      <WindowTemplate elementList={elementList} {...props} />,
      { preloadedState: {screen: {breadcrumbs: {items: []}}, settings: windowHeader ? {windowHeader} : {}} }
    );
    const breadcrumb = (container) => container.querySelector('[data-testid="breadcrumb"]');

    it('shows the breadcrumb and the buttons by default', () => {
      const { container } = render({});

      expect(breadcrumb(container)).not.toBeNull();
      expect(container.querySelector('.window-buttons span#save')).not.toBeNull();
    });

    it('shows no breadcrumb and no title, only the buttons, in none mode', () => {
      const { container } = render({}, 'none');

      expect(breadcrumb(container)).toBeNull();
      expect(container.querySelector('h1')).toBeNull();
      expect(container.querySelector('.window-header-none .window-buttons span#save')).not.toBeNull();
    });

    it('lets the screen attribute override the application setting in both directions', () => {
      expect(breadcrumb(render({windowHeader: 'breadcrumb'}, 'none').container)).not.toBeNull();
      expect(breadcrumb(render({windowHeader: 'none'}, 'breadcrumb').container)).toBeNull();
    });

    it('resolves the screen value as the server does: trimmed, case insensitive, unknown falls back to the application', () => {
      expect(breadcrumb(render({windowHeader: ' None '}, 'breadcrumb').container)).toBeNull();
      expect(breadcrumb(render({windowHeader: 'sideways'}, 'none').container)).toBeNull();
      expect(breadcrumb(render({windowHeader: ' '}, 'none').container)).toBeNull();
      expect(breadcrumb(render({windowHeader: 'none'}, 'unknown').container)).toBeNull();
      expect(breadcrumb(render({}, 'unknown').container)).not.toBeNull();
    });
  });
});
