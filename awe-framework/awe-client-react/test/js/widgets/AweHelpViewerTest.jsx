import React from 'react';
import {cleanup, screen, waitFor} from '@testing-library/react';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import "../i18nForTests";
import AweHelpViewer from "../../../src/widgets/AweHelpViewer";

describe('awe-react-client/test/js/widgets/AweHelpViewerTest.jsx', () => {

  afterAll(cleanup);
  afterEach(() => jest.restoreAllMocks());

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    screen: {
      breadcrumbs: [],
      report: {name: "opcion", option: "opcion"}
    },
    components: {
      helpViewer: {
        address: {component: 'helpViewer', view: 'report'},
        model: {values: []},
        attributes: {
          autorefresh: 1
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders AWE Help Viewer widget', async () => {
    global.fetch = jest.fn().mockReturnValue(Promise.resolve({
      headers: {
        get: () => 'text/html;charset=UTF-8'
      },
      status: 200,
      ok: true,
      text: () => Promise.resolve("<div>{t{'palabras a traducir'}}</div>")
    }));

    renderWithProviders(<AweHelpViewer id="helpViewer"/>, {preloadedState});

    expect(await screen.findByText("palabras a traducir")).toBeDefined();

  });

  it('renders AWE Help Viewer widget with error on help retrieval', async () => {
    global.fetch = jest.fn().mockReturnValue(Promise.resolve({
      headers: {
        get: () => 'text/html;charset=UTF-8'
      },
      status: 401,
      ok: false,
      text: () => Promise.resolve("Error leyendo ayuda")
    }));

    renderWithProviders(<AweHelpViewer id="helpViewer"/>, {preloadedState});

    expect(await screen.findByRole("alert")).toBeDefined();

  });

  it('renders AWE Help Viewer widget without option', async () => {
    const preloadedState2 = {
      settings: DEFAULT_SETTINGS,
      screen: {},
      components: {
        helpViewer: {
          address: {component: 'helpViewer', view: 'report'}
        }
      }
    };

    global.fetch = jest.fn().mockReturnValue(Promise.resolve({
      headers: {
        get: () => 'text/html;charset=UTF-8'
      },
      status: 200,
      ok: true,
      text: () => Promise.resolve("<div>{t{'palabras a traducir'}}</div>")
    }));

    renderWithProviders(<AweHelpViewer id="helpViewer"/>, {preloadedState: preloadedState2});

    expect(await screen.findByText("palabras a traducir")).toBeDefined();
  });
});
