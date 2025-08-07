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
});
