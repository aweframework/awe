import React from 'react';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AwePdfViewer from "../../../src/widgets/AwePdfViewer";

describe('awe-react-client/test/js/widgets/AwePdfViewerTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    screen: {
      breadcrumbs: [],
      report: {name: "opcion", option: "opcion"}
    },
    components: {
      pdfViewer: {
        address: {component: 'pdfViewer', view: 'report'},
        model: {values: []},
        attributes: {}
      }
    }
  };

  it('renders AWE PDF Viewer widget', () => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'application/pdf;base64'
      },
      status: 200,
      ok: true,
      blob: () => Promise.resolve(new Blob())
    }));

    renderWithProviders(<AwePdfViewer id="pdfViewer"/>, {preloadedState});

    expect(document.querySelector("div.pdfViewer")).toBeDefined();
    expect(document.querySelector("div.p-skeleton-circle")).toBeDefined();
  });
});
