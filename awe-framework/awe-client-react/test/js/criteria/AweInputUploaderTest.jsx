import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {act, cleanup, fireEvent, screen} from "@testing-library/react";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputUploader from "../../../src/criteria/AweInputUploader";

describe('awe-react-client/test/js/criteria/AweInputUploaderTest.jsx', () => {

  afterAll(cleanup);
  afterEach(() => jest.restoreAllMocks());

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      uploader: {
        address: {component: 'uploader', view: 'report'},
        model: {values: [{label: 'test', value: 'test', selected: true}]},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          size: "sm"
        },
        validationRules: {
          required: false
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders Awe Input Uploader component', () => {
    const resultPromise = new Promise((resolve) => resolve({
      structure: [], components: [{
        uploader: {
          address: {component: 'uploader', view: 'report'},
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
      }]
    }));
    const promise = new Promise((resolve) => {
      resolve({
        headers: {
          get: () => 'application/json;charset=UTF-8'
        },
        status: 200,
        ok: true,
        json: () => resultPromise
      });
    });
    global.fetch = jest.fn().mockReturnValue(promise);
    promise.catch((e) => console.info(e));

    renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState});

    // check
    expect(screen.getByText("BUTTON_CHOOSE")).toBeDefined();
  });

  it('renders Awe Input Uploader component with size', () => {
    renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState});

    // check
    expect(screen.getByText("BUTTON_CHOOSE")).toBeDefined();
  });

  it('renders Awe Input Uploader component and deletes a file', () => {
    renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState});

    const clearButton = screen.getByText("BUTTON_CLEAR");

    // check
    expect(clearButton).toBeDefined();

    // click on delete file
    act(() => clearButton.click());

    // check button clear
    expect(clearButton.parentElement).toHaveClass("hidden");
  });


  describe('drop zone, preview and camera capture', () => {
    const stateWith = (attributes, settings = DEFAULT_SETTINGS) => ({
      settings,
      components: {
        uploader: {
          address: {component: 'uploader', view: 'report'},
          model: {values: []},
          attributes: {placeholder: "placeholder", readonly: false, size: "sm", ...attributes},
          validationRules: {required: false},
          specificAttributes: {sort: []}
        }
      }
    });
    const dropZone = () => screen.queryByTestId("upload-drop-zone");
    const makeFile = (name, type, size = 10) => {
      const file = new File(["x"], name, {type});
      Object.defineProperty(file, "size", {value: size});
      return file;
    };
    let createObjectURL, revokeObjectURL, send;

    beforeEach(() => {
      // PrimeReact also creates an object URL for every selected file, so the URL is derived from the file name
      createObjectURL = jest.fn((file) => `blob:${file.name}`);
      revokeObjectURL = jest.fn();
      global.URL.createObjectURL = createObjectURL;
      global.URL.revokeObjectURL = revokeObjectURL;
      // Upload finishes right away with a successful response
      send = jest.spyOn(XMLHttpRequest.prototype, "send").mockImplementation(function () {
        Object.defineProperty(this, "readyState", {value: 4, configurable: true});
        Object.defineProperty(this, "status", {value: 200, configurable: true});
        this.onreadystatechange && this.onreadystatechange();
      });
    });

    it('does not render the drop zone, the preview or the capture button by default', () => {
      renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState: stateWith({})});

      expect(dropZone()).toBeNull();
      expect(screen.queryByTestId("upload-capture")).toBeNull();
      expect(screen.queryByTestId("upload-preview")).toBeNull();
    });

    it('renders the drop zone with its hint when dropZone is set', () => {
      renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState: stateWith({dropZone: true})});

      expect(dropZone()).not.toBeNull();
      expect(screen.getByText("UPLOADER_DROP_HINT")).toBeDefined();
      expect(screen.getByText("BUTTON_CHOOSE")).toBeDefined();
    });

    it('highlights the drop zone while a file is dragged over it', () => {
      renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState: stateWith({dropZone: true})});

      fireEvent.dragEnter(dropZone(), {dataTransfer: {files: []}});
      expect(dropZone()).toHaveClass("upload-drop-zone-active");

      fireEvent.dragLeave(dropZone());
      expect(dropZone()).not.toHaveClass("upload-drop-zone-active");
    });

    it('uploads a dropped file and previews it when it is an image', () => {
      renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState: stateWith({dropZone: true})});
      const file = makeFile("<b>photo</b>.png", "image/png");

      fireEvent.drop(dropZone(), {dataTransfer: {files: [file]}});

      expect(send).toHaveBeenCalledTimes(1);
      const preview = screen.getByTestId("upload-preview");
      expect(preview.tagName).toBe("IMG");
      expect(preview.getAttribute("src")).toBe("blob:<b>photo</b>.png");
      // The file name is only ever used as text, never as markup
      expect(preview.getAttribute("alt")).toBe("<b>photo</b>.png");
      expect(dropZone().querySelector("b")).toBeNull();
    });

    it('does not preview files that are not safe raster images', () => {
      renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState: stateWith({dropZone: true})});

      fireEvent.drop(dropZone(), {dataTransfer: {files: [makeFile("doc.pdf", "application/pdf")]}});
      expect(send).toHaveBeenCalledTimes(1);
      expect(screen.queryByTestId("upload-preview")).toBeNull();

      fireEvent.drop(dropZone(), {dataTransfer: {files: [makeFile("pic.svg", "image/svg+xml")]}});
      expect(screen.queryByTestId("upload-preview")).toBeNull();
    });

    it('rejects a dropped file over the size limit', () => {
      renderWithProviders(<AweInputUploader id="uploader"/>,
        {preloadedState: stateWith({dropZone: true}, {...DEFAULT_SETTINGS, uploadMaxSize: 1})});

      fireEvent.drop(dropZone(), {dataTransfer: {files: [makeFile("big.png", "image/png", 2 * 1024 * 1024)]}});

      expect(send).not.toHaveBeenCalled();
      expect(screen.queryByTestId("upload-preview")).toBeNull();
    });

    it('ignores drops when the criterion is read only', () => {
      renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState: stateWith({dropZone: true, readonly: true})});

      fireEvent.drop(dropZone(), {dataTransfer: {files: [makeFile("photo.png", "image/png")]}});

      expect(send).not.toHaveBeenCalled();
    });

    it('revokes the preview when the file is cleared and when the component unmounts', () => {
      const {unmount} = renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState: stateWith({dropZone: true})});

      fireEvent.drop(dropZone(), {dataTransfer: {files: [makeFile("a.png", "image/png")]}});
      expect(screen.getByTestId("upload-preview")).toBeDefined();

      act(() => screen.getByText("BUTTON_CLEAR").click());
      expect(revokeObjectURL).toHaveBeenCalledWith("blob:a.png");
      expect(screen.queryByTestId("upload-preview")).toBeNull();

      fireEvent.drop(dropZone(), {dataTransfer: {files: [makeFile("b.png", "image/png")]}});
      unmount();
      expect(revokeObjectURL).toHaveBeenCalledWith("blob:b.png");
    });

    it('revokes the previous preview when another image replaces it', () => {
      renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState: stateWith({dropZone: true})});

      fireEvent.drop(dropZone(), {dataTransfer: {files: [makeFile("a.png", "image/png")]}});
      act(() => screen.getByText("BUTTON_CLEAR").click());
      fireEvent.drop(dropZone(), {dataTransfer: {files: [makeFile("b.png", "image/png")]}});

      expect(screen.getByTestId("upload-preview").getAttribute("src")).toBe("blob:b.png");
    });

    it('renders a capture button wired to the camera when capture is set', () => {
      renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState: stateWith({capture: "environment"})});

      expect(screen.getByTestId("upload-capture").tagName).toBe("BUTTON");
      expect(screen.getByText("BUTTON_CAPTURE")).toBeDefined();
      const input = document.querySelector("input[capture]");
      expect(input.getAttribute("capture")).toBe("environment");
      expect(input.getAttribute("accept")).toBe("image/*");
      expect(dropZone()).toBeNull();
    });

    it('uploads the photo taken with the camera and previews it', () => {
      renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState: stateWith({capture: "user"})});

      fireEvent.change(document.querySelector("input[capture]"), {target: {files: [makeFile("shot.jpg", "image/jpeg")]}});

      expect(send).toHaveBeenCalledTimes(1);
      expect(screen.getByTestId("upload-preview").getAttribute("src")).toBe("blob:shot.jpg");
    });

    it('opens the camera input when the capture button is clicked', () => {
      renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState: stateWith({capture: "environment"})});
      const click = jest.spyOn(document.querySelector("input[capture]"), "click").mockImplementation(() => null);

      fireEvent.click(screen.getByTestId("upload-capture"));

      expect(click).toHaveBeenCalledTimes(1);
    });

    it('disables the capture button when the criterion is read only', () => {
      renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState: stateWith({capture: "environment", readonly: true})});

      expect(screen.getByTestId("upload-capture")).toBeDisabled();
    });

    it('ignores an invalid capture value', () => {
      renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState: stateWith({capture: "bogus"})});

      expect(screen.queryByTestId("upload-capture")).toBeNull();
    });
  });
});
