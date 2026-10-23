import React from 'react';
import {render} from '@testing-library/react';
import {useDocumentTitle, useHeadStylesheet} from '../../../src/hooks/useDocumentHead';

function Title({title, level, children}) {
  useDocumentTitle(title, level);
  return <>{children}</>;
}

function Stylesheet({href}) {
  useHeadStylesheet(href);
  return null;
}

const stylesheets = () => Array.from(document.head.querySelectorAll('link[rel="stylesheet"]'));

describe('awe-react-client/test/js/hooks/useDocumentHeadTest.jsx', () => {
  beforeEach(() => {
    document.title = 'initial';
  });

  describe('useDocumentTitle', () => {
    it('sets the document title', () => {
      const {unmount} = render(<Title title="View" level={0}/>);

      expect(document.title).toBe('View');
      unmount();
    });

    it('keeps the deepest title when the sub view is nested inside the view', () => {
      const {unmount} = render(<Title title="View" level={0}><Title title="Sub view" level={1}/></Title>);

      expect(document.title).toBe('Sub view');
      unmount();
    });

    it('keeps the deepest title when the sub view mounts before the view', () => {
      const {rerender, unmount} = render(<><Title title="Sub view" level={1}/></>);
      rerender(<><Title title="Sub view" level={1}/><Title title="View" level={0}/></>);

      expect(document.title).toBe('Sub view');
      unmount();
    });

    it('restores the view title when the sub view unmounts', () => {
      const {rerender, unmount} = render(<Title title="View" level={0}><Title title="Sub view" level={1}/></Title>);
      rerender(<Title title="View" level={0}/>);

      expect(document.title).toBe('View');
      unmount();
    });

    it('ignores a missing title, as an unrendered title does not count (sub view still loading)', () => {
      const {unmount} = render(<Title title="View" level={0}><Title title={null} level={1}/></Title>);

      expect(document.title).toBe('View');
      unmount();
    });

    it('updates the title when it changes', () => {
      const {rerender, unmount} = render(<Title title="View" level={0}><Title title="Sub view" level={1}/></Title>);
      rerender(<Title title="Other view" level={0}><Title title="Other sub view" level={1}/></Title>);

      expect(document.title).toBe('Other sub view');
      rerender(<Title title="Other view" level={0}/>);
      expect(document.title).toBe('Other view');
      unmount();
    });
  });

  describe('useHeadStylesheet', () => {
    it('adds the stylesheet link to the head', () => {
      const {unmount} = render(<Stylesheet href="/css/themes/sky/theme.css"/>);

      expect(stylesheets().map(link => link.getAttribute('href'))).toEqual(['/css/themes/sky/theme.css']);
      unmount();
    });

    it('updates the href when it changes, keeping a single link', () => {
      const {rerender, unmount} = render(<Stylesheet href="/css/themes/sky/theme.css"/>);
      rerender(<Stylesheet href="/css/themes/grass/theme.css"/>);

      expect(stylesheets().map(link => link.getAttribute('href'))).toEqual(['/css/themes/grass/theme.css']);
      unmount();
    });

    it('removes the link on unmount', () => {
      const {unmount} = render(<Stylesheet href="/css/themes/sky/theme.css"/>);
      unmount();

      expect(stylesheets()).toEqual([]);
    });
  });
});
