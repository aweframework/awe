import React from 'react';
import { renderWithProviders } from '../test-utils';
import Tag from '../../../src/components/Tag';
import { DEFAULT_SETTINGS } from '../../../src/redux/actions/settings';

describe('awe-react-client/test/js/components/TagTest.jsx', () => {
  it('renders a void element tag (hr) without children', () => {
    const { container } = renderWithProviders(
      <Tag type="hr" id="separator" style="col-xs-12 no-padding" elementList={[]} />,
      { preloadedState: { settings: DEFAULT_SETTINGS } }
    );

    const hr = container.querySelector('hr#separator');
    expect(hr).toBeTruthy();
    expect(hr.className).toContain('no-padding');
    expect(hr.childNodes).toHaveLength(0);
  });

  it('renders a void element tag (br) even when a label and a value are provided', () => {
    const { container } = renderWithProviders(
      <Tag type="br" id="break" label="ignored" value="ignored too" elementList={[]} />,
      { preloadedState: { settings: DEFAULT_SETTINGS } }
    );

    expect(container.querySelector('br#break')).toBeTruthy();
    expect(container.textContent).toBe('');
  });

  it('keeps rendering the label and the value of a regular tag', () => {
    const { container } = renderWithProviders(
      <Tag type="span" id="regular" label="Some label" value=" and value" elementList={[]} />,
      { preloadedState: { settings: DEFAULT_SETTINGS } }
    );

    expect(container.querySelector('span#regular').textContent).toBe('Some label and value');
  });
});
