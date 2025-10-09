import React from 'react';
import {cleanup, screen, waitFor} from '@testing-library/react';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import "../i18nForTests";
import AweHelpViewer from "../../../src/widgets/AweHelpViewer";

describe('awe-react-client/test/js/widgets/AweHelpViewerTest.jsx', () => {

  afterAll(cleanup);

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
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'text/html;charset=UTF-8'
      },
      status: 200,
      ok: true,
      text: () => Promise.resolve("<div>{t{'palabras a traducir'}}</div>")
    }));

    renderWithProviders(<AweHelpViewer id="helpViewer"/>, {preloadedState});

    expect(await screen.findByText("palabras a traducir")).toBeDefined();
    console.info("1.-", await screen.findByText("palabras a traducir"));

  });

  it('renders AWE Help Viewer widget with error on help retrieval', async () => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'text/html;charset=UTF-8'
      },
      status: 401,
      ok: false,
      text: () => Promise.resolve("Error leyendo ayuda")
    }));

    renderWithProviders(<AweHelpViewer id="helpViewer"/>, {preloadedState});

    expect(await screen.findByRole("alert")).toBeDefined();
    console.info("2.-", await screen.findByText("Error leyendo ayuda"));

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

    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'text/html;charset=UTF-8'
      },
      status: 200,
      ok: true,
      text: () => Promise.resolve("<div>{t{'palabras a traducir'}}</div>")
    }));

    renderWithProviders(<AweHelpViewer id="helpViewer"/>, {preloadedState: preloadedState2});

    expect(await screen.findByText("palabras a traducir")).toBeDefined();
    console.info("3.-", await screen.findByText("palabras a traducir"));
  });
});
