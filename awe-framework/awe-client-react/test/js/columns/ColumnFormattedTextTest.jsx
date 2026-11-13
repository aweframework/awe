import {fireEvent} from "@testing-library/react";
import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

const address = {component: "GrdFmt", view: "report", column: "FmtTxt", row: "1"};

function renderFormattedText(data, node = {}) {
  return renderWithProviders(Columns({
    component: "formatted-text",
    model: {values: []},
    numberFormat: {},
    address,
    settings: {},
    ...node
  }, data), {spyDispatch: true});
}

function text() {
  return document.querySelector("button.text-view .text-view-text");
}

describe("awe-react-client/test/js/columns/ColumnFormattedTextTest.jsx", () => {

  it("renders the label as html inside a text view", () => {
    renderFormattedText({value: "1", label: "<strong>Bold</strong> and <em>italic</em><br>line"});

    expect(text().querySelector("strong").textContent).toBe("Bold");
    expect(text().querySelector("em").textContent).toBe("italic");
    expect(text().querySelector("br")).not.toBeNull();
    expect(text().textContent).toBe("Bold and italicline");
  });

  it("uses the value when there is no label", () => {
    renderFormattedText({value: "<b>Value</b>"});

    expect(text().querySelector("b").textContent).toBe("Value");
  });

  it("removes scripts, images, frames and event handlers", () => {
    renderFormattedText({
      value: "1",
      label: "A<script>window.aweInjected=true</script><img src=\"x\" onerror=\"window.aweInjected=true\">" +
        "<b onclick=\"window.aweInjected=true\">B</b><iframe src=\"https://example.com\"></iframe>C"
    });

    expect(text().querySelector("script, img, iframe, [onclick], [onerror]")).toBeNull();
    expect(text().querySelector("b").textContent).toBe("B");
    expect(text().textContent).toBe("ABC");
  });

  it("drops script links and keeps safe links", () => {
    renderFormattedText({
      value: "1",
      label: "<a href=\"javascript:window.aweInjected=true\">Bad</a><a href=\"https://example.com\" target=\"_blank\">Good</a>"
    });

    const links = text().querySelectorAll("a");
    expect(links[0].getAttribute("href")).toBeNull();
    expect(links[1].getAttribute("href")).toBe("https://example.com");
    expect(links[1].getAttribute("rel")).toBe("noopener noreferrer");
  });

  it("does not use the html of the label as tooltip", () => {
    renderFormattedText({value: "1", label: "<b>Bold</b>"});

    expect(document.querySelector("button.text-view").getAttribute("title")).toBeNull();
  });

  it("shows the icon, unit and style of the cell", () => {
    renderFormattedText({value: "1", label: "<b>Bold</b>", icon: "download", style: "text-danger"}, {unit: "kg"});

    const button = document.querySelector("button.text-view");
    expect(button.classList).toContain("text-danger");
    expect(button.querySelector(".text-view-icon i, .text-view-icon span")).not.toBeNull();
    expect(button.textContent).toContain("kg");
  });

  it("stores the click event", () => {
    const {dispatchSpy} = renderFormattedText({value: "1", label: "<b>Bold</b>"});

    fireEvent.click(document.querySelector("button.text-view"));

    expect(dispatchSpy).toHaveBeenCalled();
  });
});
