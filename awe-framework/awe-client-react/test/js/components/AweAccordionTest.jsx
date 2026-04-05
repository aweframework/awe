import React from 'react';
import {cleanup, fireEvent, screen, waitFor} from '@testing-library/react';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweAccordion from "../../../src/components/AweAccordion";

describe('awe-react-client/test/js/components/AweAccordionTest.jsx', () => {

  afterAll(cleanup);
  afterEach(() => jest.restoreAllMocks());

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      accordion: {
        address: {component: 'accordion', view: 'report'},
        model: {values: []},
        attributes: {
          style: "estilo-especifico"
        },
        specificAttributes: {sort: []}
      },
      acc1: {
        address: {component: 'acc1', view: 'report'},
        model: {values: []},
        attributes: {},
        specificAttributes: {sort: []}
      },
      acc2: {
        address: {component: 'acc2', view: 'report'},
        model: {values: []},
        attributes: {},
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders Awe Accordion component', async () => {
    renderWithProviders(<AweAccordion id="accordion" style="estilo-especifico" elementList={[{
      elementType: "AccordionItem",
      id: "acc1",
      updateModel: jest.fn(),
      updateAttributes: jest.fn(),
      addActionsTop: jest.fn(),
      address: {component: 'acc1', view: 'report'},
      elementList: [{elementType: "Tag", type: "div", elementList: [], t: (key) => key}],
      t: (key) => key
    }, {
      elementType: "AccordionItem",
      id: "acc2",
      label: "prueba",
      address: {component: 'acc2', view: 'report'},
      elementList: [{elementType: "Tag", type: "div", label: "lo de dentro", elementList: [], t: (key) => key}], t: (key) => key
    }]}/>, {preloadedState});

    expect(document.querySelector(".p-accordion")).toHaveClass("estilo-especifico");
    expect(document.querySelector(".p-accordion-tab")).toBeDefined();
    await waitFor(() => expect(screen.findByText("prueba")).toBeDefined());

    fireEvent.click(screen.getByText("prueba"));

    await waitFor(() => expect(screen.findByText("lo de dentro")).toBeDefined());
  });

  it('renders Awe Accordion component multiple', async () => {
    renderWithProviders(<AweAccordion id="accordion" style="estilo-especifico" autocollapse={false} elementList={[{
      elementType: "AccordionItem",
      id: "acc1",
      updateModel: jest.fn(),
      updateAttributes: jest.fn(),
      addActionsTop: jest.fn(),
      address: {component: 'acc1', view: 'report'},
      t: (key) => key,
      elementList: [
        {elementType: "Tag", type: "div", elementList: [], t: (key) => key},
        {elementType: "Dependency", elementList: [], t: (key) => key},
        {elementType: "Other", elementList: [], t: (key) => key}]
    }, {
      elementType: "AccordionItem",
      label: "prueba",
      address: {component: 'acc2', view: 'report'},
      elementList: [{elementType: "Tag", type: "div", label: "lo de dentro", elementList: [], t: (key) => key}], t: (key) => key
    }]}/>, {preloadedState});

    expect(document.querySelector(".p-accordion")).toHaveClass("estilo-especifico");
    expect(document.querySelector(".p-accordion-tab")).toBeDefined();
    await waitFor(() => expect(screen.findByText("prueba")).toBeDefined());

    fireEvent.click(screen.getByText("prueba"));

    await waitFor(() => expect(screen.findByText("lo de dentro")).toBeDefined());
  });
});
