import React from 'react';
import { render } from '@testing-library/react';
import FullTemplate from '../../../src/templates/FullTemplate';
import { renderWithProviders } from '../test-utils';

describe('awe-react-client/test/js/templates/FullTemplateTest.jsx', () => {
  it('renders FullTemplate with all sources', () => {
    // Create mock element list with all required sources
    const elementList = [
      { source: 'center', style: 'center-style', elementList: [] },
      { source: 'modal', style: 'modal-style', elementList: [] },
      { source: 'hidden', style: 'hidden-style', elementList: [] }
    ];

    // Render the component
    const { container } = render(<FullTemplate elementList={elementList} />);

    // Verify the rendered output contains the expected elements
    expect(container.querySelector('.expand.expandible-vertical')).not.toBeNull();
    expect(container.querySelector('.expand.expandible-vertical.center-style')).not.toBeNull();
  });

  it('marks the centre as the container where a maximized window grows', () => {
    const { container } = render(<FullTemplate elementList={[{ source: 'center', elementList: [] }]} />);

    expect(container.querySelector('.maximize-target.expandible-vertical')).not.toBeNull();
  });

  it('renders FullTemplate with custom styles', () => {
    // Create mock element list with custom styles
    const elementList = [
      { source: 'center', style: 'custom-center-style', elementList: [] },
      { source: 'modal', style: 'custom-modal-style', elementList: [] },
      { source: 'hidden', style: 'custom-hidden-style', elementList: [] }
    ];

    // Render the component
    const { container } = render(<FullTemplate elementList={elementList} />);

    // Verify the rendered output contains the custom styles
    expect(container.querySelector('.expand.expandible-vertical.custom-center-style')).not.toBeNull();
  });

  it('renders FullTemplate with modal and hidden sections', () => {
    // Create mock element list with specific styles for modal and hidden
    const elementList = [
      { source: 'center', style: 'center-style', elementList: [] },
      { source: 'modal', style: 'modal-specific-style', elementList: [] },
      { source: 'hidden', style: 'hidden-specific-style', elementList: [] }
    ];

    // Render the component
    const { container } = render(<FullTemplate elementList={elementList} />);

    // Verify the modal and hidden sections are rendered with correct styles
    expect(container.querySelector('[style*="position: absolute"]')).not.toBeNull();
    expect(container.querySelector('[style*="position: absolute"]').className).toBe('modal-specific-style');

    expect(container.querySelector('[style*="display: none"]')).not.toBeNull();
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
        <FullTemplate elementList={sources({source: 'center', type: 'section', id: 'centre', style: 'expand wide', expand: 'horizontal', elementList: [child]})} />,
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
        <FullTemplate elementList={sources({source: 'center', expand: 'horizontal', elementList: [child]})} />,
        { preloadedState }
      );

      expect(container.querySelector('.expand.expandible-vertical > span#inner')).not.toBeNull();
      expect(container.querySelector('.expandible-horizontal')).toBeNull();
    });
  });
});
