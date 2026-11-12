import React from 'react';
import {renderWithProviders} from '../test-utils';
import ColumnIcon from '../../../src/columns/ColumnIcon';
import {DEFAULT_SETTINGS} from '../../../src/redux/actions/settings';

describe('awe-react-client/test/js/columns/ColumnIconTest.jsx', () => {
  const render = data => renderWithProviders(<ColumnIcon data={data} />, {preloadedState: {settings: DEFAULT_SETTINGS}});
  const legacy = style => ({
    icon: 'fa-exclamation-triangle fa-inverse fa-border icon-fixed-width-grid', style, title: 'Warning'
  });

  // The legacy icon cells (the notifications screen) paint the type with a Bootstrap bg-* class and a white glyph
  it.each(['default', 'primary', 'success', 'info', 'warning', 'danger'])(
    'draws a legacy bg-%s inverse icon as a badge of that tone, as Bootstrap is not there to paint it', tone => {
      const {container} = render(legacy(`bg-${tone}`));

      const icon = container.querySelector('.icon-container i');
      expect(icon.classList.contains('awe-icon-badge')).toBe(true);
      expect(icon.classList.contains(`awe-icon-badge-${tone}`)).toBe(true);
    });

  it('keeps an icon without a bg-* tone or without an inverse glyph as it is', () => {
    expect(render({icon: 'fa-bell', style: 'text-warning'}).container.querySelector('.awe-icon-badge')).toBeNull();
    expect(render({icon: 'fa-bell fa-inverse', style: ''}).container.querySelector('.awe-icon-badge')).toBeNull();
  });
});
