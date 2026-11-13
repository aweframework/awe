import React from "react";
import {render} from "@testing-library/react";
import {Columns} from "../../../src/utilities/structure";

const NODE = {component: "unknown-column", address: {view: "report", component: "grid", column: "col", row: "1"}};

const renderCell = (data) => render(<div data-testid="host">{Columns(NODE, data)}</div>).container;

const MALICIOUS_VALUES = {
  "an event handler on an image": "<img src=x onerror=alert(1)>",
  "a javascript url": "<a href=\"javascript:alert(1)\">click</a>",
  "a script tag": "before<script>window.__pwned = true;</script>after",
  "an event handler on an allowed tag": "<b onmouseover=\"alert(1)\">bold</b>",
  "an iframe": "<iframe src=\"https://example.com\"></iframe>",
  "an svg with a handler": "<svg onload=alert(1)><circle/></svg>"
};

const hasScriptCarrier = (container) => {
  const dangerousTags = container.querySelectorAll("script, iframe, img, svg, object, embed, style");
  const handlers = Array.from(container.querySelectorAll("*"))
    .flatMap(element => Array.from(element.attributes))
    .filter(attribute => attribute.name.startsWith("on"));
  const scriptUrls = Array.from(container.querySelectorAll("[href], [src]"))
    .filter(element => /^\s*javascript:/i.test(element.getAttribute("href") ?? element.getAttribute("src")));
  return dangerousTags.length + handlers.length + scriptUrls.length;
};

describe("awe-react-client/test/js/utilities/structureCellTest.jsx", () => {
  describe.each(Object.entries(MALICIOUS_VALUES))("a cell with %s", (_name, malicious) => {
    it("renders no script carrier from the label", () => {
      expect(hasScriptCarrier(renderCell({label: malicious, value: "v"}))).toBe(0);
    });

    it("renders no script carrier from the value", () => {
      expect(hasScriptCarrier(renderCell({value: malicious}))).toBe(0);
    });

    it("renders no script carrier from a list of selected items", () => {
      const container = render(<div>{Columns(NODE, [{label: malicious, selected: true}])}</div>).container;
      expect(hasScriptCarrier(container)).toBe(0);
    });
  });

  it("keeps the text around a stripped script tag", () => {
    const container = renderCell({value: MALICIOUS_VALUES["a script tag"]});
    expect(container.textContent).toContain("before");
    expect(container.textContent).toBe("beforeafter");
    expect(container.querySelector("script")).toBeNull();
  });

  it("keeps the text of a link and drops its javascript url", () => {
    const container = renderCell({value: MALICIOUS_VALUES["a javascript url"]});
    expect(container.textContent).toBe("click");
    expect(container.querySelector("a")?.getAttribute("href") ?? null).toBeNull();
  });

  it("keeps a safe subset of the markup that the server transforms generate", () => {
    const container = renderCell({value: "<strong>Name</strong> - <em>description</em><br>second line"});
    expect(container.querySelector("strong").textContent).toBe("Name");
    expect(container.querySelector("em").textContent).toBe("description");
    expect(container.querySelector("br")).not.toBeNull();
  });

  it("shows escaped markup as text", () => {
    const container = renderCell({value: "&lt;a href='tutu'&gt;aaa&lt;/a&gt;"});
    expect(container.querySelector("a")).toBeNull();
    expect(container.textContent).toBe("<a href='tutu'>aaa</a>");
  });

  it("keeps plain text, numbers and empty values", () => {
    expect(renderCell({value: "plain text"}).textContent).toBe("plain text");
    expect(renderCell({value: 12.5}).textContent).toBe("12.5");
    expect(renderCell({}).textContent).toBe("");
  });

  it("keeps the style and the title of the cell", () => {
    const span = renderCell({label: "L", value: "v", style: "text-danger"}).querySelector("span");
    expect(span.className).toContain("text-danger");
    expect(span.getAttribute("title")).toBe("L");
  });
});
