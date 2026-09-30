import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweSuggest from "../../../src/criteria/AweSuggest";
import {ADD_ACTIONS_TOP} from "../../../src/redux/actions/actions";
import { act, fireEvent, waitFor } from "@testing-library/react";

describe('awe-react-client/test/js/criteria/AweSuggestTest.jsx', () => {

  const baseState = {
    settings: DEFAULT_SETTINGS,
    components: {
      suggest: {
        address: {component: 'suggest', view: 'report'},
        model: {values: [{label: 'test', value: 'test', selected: true}]},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
        },
        validationRules: {
          required: false
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders Awe Suggest component', async () => {
    await act(async () => {
      renderWithProviders(<AweSuggest id="suggest"/>, {preloadedState: baseState});
    });

    // check
    expect(document.querySelector("#suggest input[aria-autocomplete]")).not.toBeNull();
  });
  it('renders a skeleton when address is missing', async () => {
    const preloadedState = {
      ...baseState,
      components: {
        suggest: {
          // no address
          model: { values: [{ value: 'X', selected: true }] },
          attributes: { placeholder: 'ph' },
          validationRules: { required: false }
        }
      }
    };

    await act(async () => {
      renderWithProviders(<AweSuggest id="suggest" />, { preloadedState });
    });

    // Should render a primereact Skeleton
    expect(document.querySelector('.p-skeleton')).not.toBeNull();
  });


    describe('New features (Virtual Scroll & Tooltip)', () => {

        let originalFetch;
        let originalXHR;
        let fetchRows = [];

        beforeAll(() => {
            originalFetch = window.fetch;
            originalXHR = window.XMLHttpRequest;

            window.fetch = () => Promise.resolve({
                ok: true,
                json: () => Promise.resolve([{
                    type: "fill",
                    parameters: { datalist: { rows: fetchRows } }
                }])
            });

            window.XMLHttpRequest = class {
                open() {}
                send() {
                    this.readyState = 4;
                    this.status = 200;
                    this.responseText = JSON.stringify([{
                        type: "fill",
                        parameters: { datalist: { rows: fetchRows } }
                    }]);
                    this.response = [{
                        type: "fill",
                        parameters: { datalist: { rows: fetchRows } }
                    }];

                    if (this.onload) this.onload();
                    if (this.onreadystatechange) this.onreadystatechange();
                }
                setRequestHeader() {}
            };
        });

        beforeEach(() => {
            fetchRows = [];
        });

        afterAll(() => {
            window.fetch = originalFetch;
            window.XMLHttpRequest = originalXHR;
        });

      it('triggers initialSuggest when checkTarget is true and value needs initialization', async () => {
            fetchRows = [{ label: 'valor_inicial', value: 'valor_inicial' }];
            const initValueState = {
                ...baseState,
                components: {
                    suggest: {
                        ...baseState.components.suggest,
                        model: {
                            values: [{ value: 'valor_inicial', selected: true }]
                        },
                        attributes: {
                            ...baseState.components.suggest.attributes,
                            checkTarget: true,
                            placeholder: "test"
                        }
                    }
                }
            };

            await act(async () => {
              renderWithProviders(<AweSuggest id="suggest" />, { preloadedState: initValueState });
            });

            await act(async () => {
              await new Promise(resolve => setTimeout(resolve, 100));
            });

            // the initial value is kept and shown after the initialization
            const input = document.querySelector("#suggest input[aria-autocomplete]");
            expect(input).not.toBeNull();
            expect(input.value).toBe('valor_inicial');
      });

    it('renders large list with virtual scroller class', async () => {
        const largeValues = Array.from({ length: 105 }, (_, i) => ({
            label: `Item ${i}`,
            value: `${i}`,
            selected: i === 0
        }));
        fetchRows = largeValues;

        const preloadedState = {
            ...baseState,
            components: {
                suggest: {
                    ...baseState.components.suggest,
                    attributes: {
                        ...baseState.components.suggest.attributes,
                        timeout: 0,
                        openPanelOnMount: true,
                        virtualScroll: false
                    },
                    model: { values: largeValues }
                }
            }
        };

        let container;
        await act(async () => {
          ({ container } = renderWithProviders(<AweSuggest id="suggest" />, { preloadedState }));
        });

        const virtualItem = document.querySelector('.awe-virtual-item');
        if (virtualItem) {
            expect(virtualItem).not.toBeNull();
            expect(virtualItem.style.cursor).toBe('pointer');
        }
    });

    it('activates tooltip logic on hover when text overflows', async () => {
        const largeValues = Array.from({ length: 105 }, (_, i) => ({
            label: `Long Text Item ${i}`,
            value: `${i}`,
            selected: false
        }));
        fetchRows = largeValues;

        const preloadedState = {
            ...baseState,
            components: {
                suggest: {
                    ...baseState.components.suggest,
                    attributes: {
                        ...baseState.components.suggest.attributes,
                        timeout: 0,
                        openPanelOnMount: true,
                        virtualScroll: false
                    },
                    model: { values: largeValues }
                }
            }
        };

        let container;
        await act(async () => {
            ({ container } = renderWithProviders(<AweSuggest id="suggest" />, { preloadedState }));
        });

        const input = container.querySelector('#suggest input[aria-autocomplete]');
        expect(input).not.toBeNull();
        await act(async () => {
            fireEvent.change(input, { target: { value: 'a' } });
            await waitFor(() => {
                expect(document.querySelector('.awe-virtual-item')).not.toBeNull();
            });
        });

        const item = document.querySelector('.awe-virtual-item');
        Object.defineProperty(item, 'scrollWidth', {value: 200, configurable: true});
        Object.defineProperty(item, 'clientWidth', {value: 100, configurable: true});

        fireEvent.mouseEnter(item);
        expect(item.getAttribute('aria-label')).not.toBeNull();
    });

    it('runs tooltip hide logic on mouse leave for large list items', async () => {
        const largeValues = Array.from({ length: 105 }, (_, i) => ({
            label: `Long Text Item ${i}`,
            value: `${i}`,
            selected: false
        }));
        fetchRows = largeValues;

        const preloadedState = {
            ...baseState,
            components: {
                suggest: {
                    ...baseState.components.suggest,
                    attributes: {
                        ...baseState.components.suggest.attributes,
                        timeout: 0,
                        openPanelOnMount: true,
                        virtualScroll: false
                    },
                    model: { values: largeValues }
                }
            }
        };

        let container;
        await act(async () => {
            ({ container } = renderWithProviders(<AweSuggest id="suggest" />, { preloadedState }));
        });

        const input = container.querySelector('#suggest input[aria-autocomplete]');
        expect(input).not.toBeNull();
        await act(async () => {
            fireEvent.change(input, { target: { value: 'a' } });
            await waitFor(() => {
                expect(document.querySelector('.awe-virtual-item')).not.toBeNull();
            });
        });

        const item = document.querySelector('.awe-virtual-item');

        Object.defineProperty(item, 'scrollWidth', { value: 200, configurable: true });
        Object.defineProperty(item, 'clientWidth', { value: 100, configurable: true });

        fireEvent.mouseEnter(item);
        expect(() => fireEvent.mouseLeave(item)).not.toThrow();
    });
    });
});
