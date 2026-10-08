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

  it('keeps the icons, the emphasis and the line breaks that a label may use', () => {
    const { container } = renderWithProviders(
      <Tag type="div" id="markup" label={'<i class="fa fa-check"></i> <b>Done</b><br>Next <span class="text-danger">line</span>'} elementList={[]} />,
      { preloadedState: { settings: DEFAULT_SETTINGS } }
    );

    const tag = container.querySelector('div#markup');
    expect(tag.querySelector('i.fa.fa-check')).not.toBeNull();
    expect(tag.querySelector('b').textContent).toBe('Done');
    expect(tag.querySelector('br')).not.toBeNull();
    expect(tag.querySelector('span.text-danger').textContent).toBe('line');
  });

  it('shows the text of a label that is not markup as it is', () => {
    const { container } = renderWithProviders(
      <Tag type="div" id="text" label="Date <= and Rock & roll" elementList={[]} />,
      { preloadedState: { settings: DEFAULT_SETTINGS } }
    );

    expect(container.querySelector('div#text').textContent).toBe('Date <= and Rock & roll');
  });

  it('keeps the safe markup of a label and removes the markup that runs code', () => {
    const { container } = renderWithProviders(
      <Tag type="div" id="formatted" label={'<b>bold</b><img src=x onerror=alert(1)><a href="javascript:alert(1)">go</a>'} elementList={[]} />,
      { preloadedState: { settings: DEFAULT_SETTINGS } }
    );

    const tag = container.querySelector('div#formatted');
    expect(tag.querySelector('b').textContent).toBe('bold');
    expect(tag.querySelector('img')).toBeNull();
    expect(tag.querySelector('a').getAttribute('href')).toBeNull();
    expect(tag.innerHTML).not.toContain('onerror');
  });
});
