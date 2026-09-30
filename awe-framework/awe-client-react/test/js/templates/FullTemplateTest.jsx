import React from 'react';
import { render } from '@testing-library/react';
import FullTemplate from '../../../src/templates/FullTemplate';

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
});
