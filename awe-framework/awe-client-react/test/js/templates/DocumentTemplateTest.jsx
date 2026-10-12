import React from 'react';
import { renderWithProviders } from '../test-utils';
import DocumentTemplate from '../../../src/templates/DocumentTemplate';

describe('awe-react-client/test/js/templates/DocumentTemplateTest.jsx', () => {
  it('renders DocumentTemplate with all sources', () => {
    // Setup preloaded state for Redux
    const preloadedState = {
      screen: {
        breadcrumbs: {
          items: []
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
      <DocumentTemplate elementList={elementList} />,
      { preloadedState }
    );

    // Verify the rendered output contains the expected elements
    expect(container.querySelector('.window-buttons')).not.toBeNull();
    expect(container.querySelector('.expand.expandible-vertical')).not.toBeNull();
    expect(container.querySelector('.expand.expandible-vertical.center-style')).not.toBeNull();
  });

  it('renders DocumentTemplate with custom styles', () => {
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
      <DocumentTemplate elementList={elementList} />,
      { preloadedState }
    );

    // Verify the rendered output contains the custom styles
    expect(container.querySelector('.window-buttons.custom-buttons-style')).not.toBeNull();
    expect(container.querySelector('.expand.expandible-vertical.custom-center-style')).not.toBeNull();
  });

  it('renders DocumentTemplate with modal and hidden sections', () => {
    // Create mock element list with specific styles for modal and hidden
    const elementList = [
      { source: 'center', style: 'center-style', elementList: [] },
      { source: 'modal', style: 'modal-specific-style', elementList: [] },
      { source: 'hidden', style: 'hidden-specific-style', elementList: [] },
      { source: 'buttons', style: 'buttons-specific-style', elementList: [] }
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
      <DocumentTemplate elementList={elementList} />,
      { preloadedState }
    );

    // Verify the modal and hidden sections are rendered with correct styles
    expect(container.querySelector('[style*="position: absolute"]')).not.toBeNull();
    expect(container.querySelector('[style*="position: absolute"]').className).toBe('modal-specific-style');

    expect(container.querySelector('[style*="display: none"]')).not.toBeNull();

    // Verify the buttons section is rendered with correct style
    expect(container.querySelector('.window-buttons.buttons-specific-style')).not.toBeNull();
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
        <DocumentTemplate elementList={sources({source: 'center', type: 'section', id: 'centre', style: 'expand wide', expand: 'horizontal', elementList: [child]})} />,
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
        <DocumentTemplate elementList={sources({source: 'center', expand: 'horizontal', elementList: [child]})} />,
        { preloadedState }
      );

      expect(container.querySelector('.expand.expandible-vertical > span#inner')).not.toBeNull();
      expect(container.querySelector('.expandible-horizontal')).toBeNull();
    });
  });
});
