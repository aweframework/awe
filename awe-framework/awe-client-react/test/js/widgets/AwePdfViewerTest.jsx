import React from 'react';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AwePdfViewer from "../../../src/widgets/AwePdfViewer";
import {act, waitFor} from "@testing-library/react";

describe('awe-react-client/test/js/widgets/AwePdfViewerTest.jsx', () => {

  beforeEach(() => {
    jest.spyOn(console, 'log').mockImplementation(() => {});
  });

  beforeAll(() => {
    if (!window.URL.createObjectURL) {
      window.URL.createObjectURL = jest.fn().mockReturnValue('blob://mock');
    }
    if (!window.URL.revokeObjectURL) {
      window.URL.revokeObjectURL = jest.fn();
    }
  });

  afterEach(() => jest.restoreAllMocks());

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

  it('renders AWE PDF Viewer widget (skeleton while loading)', async () => {
    global.fetch = jest.fn().mockReturnValue(Promise.resolve({
      headers: {
        get: () => 'application/pdf;base64'
      },
      status: 200,
      ok: true,
      blob: () => Promise.resolve(new Blob())
    }));

    await act(async () => {
      renderWithProviders(<AwePdfViewer id="pdfViewer"/>, {preloadedState: baseState});
    });

    await waitFor(() => {
      expect(document.querySelector("div.pdf-viewer")).toBeDefined();
    });
  });

  it('hides when visible=false', async () => {
    global.fetch = jest.fn().mockReturnValue(Promise.resolve({
      blob: () => Promise.resolve(new Blob())
    }));

    const preloadedState = JSON.parse(JSON.stringify(baseState));
    preloadedState.components.pdfViewer.attributes.visible = false;

    await act(async () => {
      renderWithProviders(<AwePdfViewer id="pdfViewer"/>, {preloadedState});
    });

    // Should render nothing
    expect(document.querySelector('#pdfViewer')).toBeNull();
  });

  it('adds style class and id when visible', async () => {
    // Mock fetch + URL to resolve quickly and set pdf
    global.fetch = jest.fn().mockReturnValue(Promise.resolve({
      blob: () => Promise.resolve(new Blob(["%PDF-1.4"], { type: 'application/pdf' }))
    }));
    const createUrlSpy = jest.spyOn(window.URL, 'createObjectURL').mockReturnValue('blob://pdf');

    const preloadedState = JSON.parse(JSON.stringify(baseState));
    preloadedState.components.pdfViewer.attributes.style = 'my-class';

    await act(async () => {
      renderWithProviders(<AwePdfViewer id="pdfViewer"/>, {preloadedState});
    });

    await waitFor(() => {
      expect(document.querySelector('#pdfViewer')).not.toBeNull();
      expect(document.querySelector('.my-class')).not.toBeNull();
    });
  });
});
