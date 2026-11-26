import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweSelect from "../../../src/criteria/AweSelect";
import {cleanup, fireEvent, render, screen} from "@testing-library/react";
import {getIconNode} from "../../../src/utilities";

describe('awe-react-client/test/js/criteria/AweSelectTest.jsx', () => {

  afterAll(cleanup);

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      select: {
        address: {component: 'select', view: 'report'},
        model: {values: [{label: 'textToCheck', value: 'test', selected: true}]},
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

  it('renders Awe Select component', async () => {
    renderWithProviders(<AweSelect id="select"/>, {preloadedState});

    // check
    expect(await screen.findByRole("button")).not.toBeNull();
  });

  it('lists the options under the header of their group', () => {
    const {container} = renderWithProviders(<AweSelect id="select"/>, {
      preloadedState: {
        ...preloadedState,
        components: {
          select: {
            ...preloadedState.components.select,
            model: {values: [
              {label: 'Everything', value: '*'},
              {label: 'Users · Read', value: 'users.read', group: 'Users', selected: true},
              {label: 'Profiles · Read', value: 'profiles.read', group: 'Profiles'},
              {label: 'Users · Update', value: 'users.update', group: 'Users'}
            ]}
          }
        }
      }
    });

    expect(container.querySelector("[data-testid='select-value']").textContent).toBe('Users · Read');
    fireEvent.click(container.querySelector("[data-testid='select-trigger']"));
    const panel = document.querySelector("[data-testid='select-dropdown']");
    const rows = Array.from(panel.querySelectorAll("[data-pc-section='itemgroup'], [data-testid='select-option']"))
      .map(row => row.getAttribute("data-pc-section") === "itemgroup" ? `# ${row.textContent}`.trim() : row.textContent);
    // An option without group keeps its place, without header; an option does not repeat the header of its group
    expect(rows).toEqual(['#', 'Everything', '# Users', 'Read', 'Update', '# Profiles', 'Read']);
  });


  // The clear icon is the Lucide x of the suggest (#997), and keeps the keyboard access and the label of PrimeReact
  const expectLucideClearIcon = (clearIcon) => {
    const lucideX = render(getIconNode("lucide:x", {"aria-hidden": "true"})).container.innerHTML;
    expect(clearIcon).not.toBeNull();
    expect(clearIcon.innerHTML).toBe(lucideX);
    expect(clearIcon.getAttribute("tabindex")).toBe("0");
    expect(clearIcon.getAttribute("aria-label")).toBeTruthy();
  };

  it('clears an optional select with the Lucide x', () => {
    const {container} = renderWithProviders(<AweSelect id="select"/>, {preloadedState: {
      ...preloadedState,
      components: {select: {...preloadedState.components.select,
        attributes: {...preloadedState.components.select.attributes, optional: true}}}
    }});

    expectLucideClearIcon(container.querySelector(".p-dropdown-clear-icon"));
  });
});
