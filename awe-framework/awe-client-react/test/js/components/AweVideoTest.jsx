import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweVideo from "../../../src/components/AweVideo";

const mockPlayer = jest.fn((props) => <div id={props.id}><video/></div>);
jest.mock("react-player", () => ({__esModule: true, default: (props) => mockPlayer(props)}));

describe('awe-react-client/test/js/criteria/AweVideoTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      video: {
        address: {component: 'video', view: 'report'},
        model: {values: [{label: 'test', value: 'test', selected: true}]},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          label: "url",
          title: "link",
          url: "https://www.youtube.com/watch?v=LXb3EKWsInQ"
        },
        validationRules: {
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders video component', () => {
    renderWithProviders(<AweVideo id={"video"}/>, {preloadedState});

    // check
    expect(document.querySelector("div#video")).not.toBeNull();
  });

  const withAttributes = (attributes) => ({
    settings: DEFAULT_SETTINGS,
    components: {
      video: {
        address: {component: 'video', view: 'report'},
        model: {values: []},
        attributes: {src: "https://example.com/video.mp4", ...attributes},
        specificAttributes: {sort: []}
      }
    }
  });

  const lastPlayerProps = () => mockPlayer.mock.calls[mockPlayer.mock.calls.length - 1][0];

  beforeEach(() => mockPlayer.mockClear());

  it('passes the preload attribute to the player', () => {
    renderWithProviders(<AweVideo id={"video"}/>, {preloadedState: withAttributes({preload: "none"})});

    expect(lastPlayerProps().config.file.attributes.preload).toBe("none");
  });

  it('leaves the preload attribute to the browser when it is not defined', () => {
    renderWithProviders(<AweVideo id={"video"}/>, {preloadedState: withAttributes({})});

    expect(lastPlayerProps().config.file.attributes.preload).toBeUndefined();
  });
});
