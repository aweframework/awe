import React from 'react';
import {cleanup, screen, waitFor} from '@testing-library/react';
import AweResizable from "../../../src/components/AweResizable";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/components/AweResizableTest.jsx', () => {

  afterAll(cleanup);

  const preloadedState = {};

  it('renders Awe Resizable component', async () => {
    renderWithProviders(<AweResizable directions="top" style="estilo-especifico" elementList={[{
      elementType: "Tag",
      type: "div",
      elementList: [{elementType: "Tag", type: "div", elementList: [], t: (key) => key}],
      t: (key) => key
    },
      {elementType: "Tag", type: "div", label: "prueba", elementList: [], t: (key) => key},
    ]}/>, {preloadedState});

    expect(document.querySelector(".p-splitter.estilo-especifico")).not.toBeNull();
    expect(document.querySelector(".p-splitter-panel")).not.toBeNull();
    await waitFor(() => expect(screen.findByText("prueba")).toBeDefined());
  });

  it('renders Awe Resizable component horizontal', async () => {
    renderWithProviders(<AweResizable directions="left" style="estilo-especifico" elementList={[{
      elementType: "Tag",
      type: "div",
      elementList: [{elementType: "Tag", type: "div", elementList: [], t: (key) => key}],
      t: (key) => key
    },
      {elementType: "Tag", type: "div", label: "prueba", elementList: [], t: (key) => key},
    ]}/>, {preloadedState});

    expect(document.querySelector(".p-splitter-horizontal.estilo-especifico")).not.toBeNull();
    expect(document.querySelector(".p-splitter-panel")).not.toBeNull();
    await waitFor(() => expect(screen.findByText("prueba")).toBeDefined());
  });
});
