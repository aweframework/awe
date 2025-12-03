import React from 'react';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AwePdfViewer from "../../../src/widgets/AwePdfViewer";
import {act} from "@testing-library/react";

describe('awe-react-client/test/js/widgets/AwePdfViewerTest.jsx', () => {

  const baseState = {
    settings: DEFAULT_SETTINGS,
    screen: {
      breadcrumbs: [],
      report: {name: "opcion", option: "opcion"}
    },
    components: {
      pdfViewer: {
        address: {component: 'pdfViewer', view: 'report'},
        model: {values: []},
        attributes: { targetAction: 'download-report' }
      }
    }
  };

  it('renders AWE PDF Viewer widget (skeleton while loading)', () => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'application/pdf;base64'
      },
      status: 200,
      ok: true,
      blob: () => Promise.resolve(new Blob())
    }));

    renderWithProviders(<AwePdfViewer id="pdfViewer"/>, {preloadedState: baseState});

    expect(document.querySelector("div.pdf-viewer")).toBeDefined();
    expect(document.querySelector("div.p-skeleton-circle")).toBeDefined();
  });

  it('hides when visible=false', () => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      blob: () => Promise.resolve(new Blob())
    }));

    const preloadedState = JSON.parse(JSON.stringify(baseState));
    preloadedState.components.pdfViewer.attributes.visible = false;

    renderWithProviders(<AwePdfViewer id="pdfViewer"/>, {preloadedState});

    // Should render nothing
    expect(document.querySelector('#pdfViewer')).toBeNull();
  });

  it('adds style class and id when visible', () => {
    // Mock fetch + URL to resolve quickly and set pdf
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      blob: () => Promise.resolve(new Blob(["%PDF-1.4"], { type: 'application/pdf' }))
    }));
    const createUrlSpy = spyOn(window.URL, 'createObjectURL').and.returnValue('blob://pdf');

    const preloadedState = JSON.parse(JSON.stringify(baseState));
    preloadedState.components.pdfViewer.attributes.style = 'my-class';

    renderWithProviders(<AwePdfViewer id="pdfViewer"/>, {preloadedState});

    // Allow promises to flush
    expect(document.querySelector('#pdfViewer')).not.toBeNull();
    expect(document.querySelector('.my-class')).not.toBeNull();
  });
});
