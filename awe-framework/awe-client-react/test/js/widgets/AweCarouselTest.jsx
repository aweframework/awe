import React from 'react';
import { act, waitFor } from '@testing-library/react';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweCarousel from "../../../src/widgets/AweCarousel";

jest.mock('react-player', () => function MockReactPlayer(props) {
  return <div data-testid="mock-react-player" data-url={props.url} />;
});

describe('awe-react-client/test/js/widgets/AweCarouselTest.jsx', () => {

  beforeEach(() => {
    jest.spyOn(console, 'info').mockImplementation(() => {});
    jest.spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    screen: {
      breadcrumbs: [],
      report: {name: "opcion", option: "opcion"}
    },
    components: {
      carousel: {
        address: {component: 'carousel', view: 'report'},
        model: {values: [
            {
              "id": 1,
              "title": "SCREEN_TEXT_STEP 1",
              "type": "video",
              "url": "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            },
            {
              "id": 2,
              "title": "SCREEN_TEXT_STEP 2",
              "type": "pdf",
              "url": "https://www.orimi.com/pdf-test.pdf"
            },
            {
              "id": 3,
              "title": "SCREEN_TEXT_STEP 3",
              "type": "video",
              "url": "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
            },
            {
              "id": 4,
              "title": "SCREEN_TEXT_STEP 4",
              "type": "image",
              "url": "https://picsum.photos/640/480"
            }
          ]},
        attributes: {}
      }
    }
  };

  it('renders Carousel widget', async () => {
    await act(async () => {
      renderWithProviders(<AweCarousel id="carousel"/>, {preloadedState});
    });

    await waitFor(() => {
      expect(document.querySelector("div.carousel")).toBeDefined();
      expect(document.querySelector("div.p-carousel")).toBeDefined();
    });
  });
});
