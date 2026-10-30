import React from "react";
import { cleanup, render } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";
import { Columns } from "../../../src/utilities/structure";

const address = { view: "report", component: "Grd", row: "1", column: "name" };

// Inner HTML of a cell rendered alone, to compare it with the DOM of S8
const cellHtml = (node, data, row) => render(<div>{Columns({ address, ...node }, data, {}, false, row)}</div>).container.firstChild.innerHTML;

function gridState(columnModel, values) {
  return {
    settings: DEFAULT_SETTINGS,
    screen: { size: { width: 100, height: 100 } },
    components: {
      Grd: {
        address: { component: "Grd", view: "report" },
        model: { values, records: values.length },
        attributes: { columnModel, headerModel: [], buttonModel: [], visible: true, max: 30 },
        specificAttributes: { sort: [] }
      }
    }
  };
}

describe("awe-react-client/test/js/components/AweGridCellsTest.jsx", () => {
  afterEach(cleanup);

  describe("secondary line", () => {
    it("shows the secondary field of the row under the main value", () => {
      expect(cellHtml({ secondaryField: "email" }, { value: "Ana" }, { name: "Ana", email: "ana@almis.com" }))
        .toBe('<div class="awe-cell-lines"><span class="awe-cell-main">Ana</span><span class="awe-cell-secondary">ana@almis.com</span></div>');
    });

    it("reads the label of a secondary cell model", () => {
      expect(cellHtml({ secondaryField: "city" }, { value: "Ana" }, { city: { value: "M", label: "Madrid" } }))
        .toContain('<span class="awe-cell-secondary">Madrid</span>');
    });

    it("shows only the main line when the secondary value is empty or missing", () => {
      const plain = cellHtml({}, { value: "Ana" });
      expect(cellHtml({ secondaryField: "email" }, { value: "Ana" }, { email: "" })).toBe(plain);
      cleanup();
      expect(cellHtml({ secondaryField: "email" }, { value: "Ana" }, {})).toBe(plain);
      cleanup();
      expect(cellHtml({ secondaryField: "email" }, { value: "Ana" })).toBe(plain);
    });

    it("combines with a status", () => {
      expect(cellHtml({ secondaryField: "since", statusField: "tone" }, { value: "Active" }, { tone: "success", since: "2024" }))
        .toBe('<div class="awe-cell-lines"><span class="awe-cell-main"><span class="awe-cell-status awe-cell-status-success" data-tone="success" title="Active">Active</span></span><span class="awe-cell-secondary">2024</span></div>');
    });
  });

  describe("status", () => {
    it.each(["success", "warning", "danger", "info", "neutral"])("shows the %s tone as a dot and the text", (tone) => {
      expect(cellHtml({ statusField: "tone" }, { value: "State" }, { tone }))
        .toBe(`<span class="awe-cell-status awe-cell-status-${tone}" data-tone="${tone}" title="State">State</span>`);
    });

    it("reads the value of a tone cell model", () => {
      expect(cellHtml({ statusField: "tone" }, { value: "State" }, { tone: { value: "danger", label: "Danger" } }))
        .toContain('data-tone="danger"');
    });

    it.each([["an unknown tone", "purple"], ["an empty tone", ""], ["a missing tone", undefined], ["a markup tone", "<b>x</b>"]])
    ("falls back to neutral with %s", (_name, tone) => {
      expect(cellHtml({ statusField: "tone" }, { value: "State" }, { tone }))
        .toBe('<span class="awe-cell-status awe-cell-status-neutral" data-tone="neutral" title="State">State</span>');
    });

    it("shows the label of the main cell model", () => {
      expect(cellHtml({ statusField: "tone" }, { value: "A", label: "Active" }, { tone: "info" })).toContain('title="Active">Active</span>');
    });
  });

  describe("chips", () => {
    it("splits the main value by commas, trims the items and drops the empty ones", () => {
      expect(cellHtml({ chips: true }, { value: " red, green ,, blue ," }))
        .toBe('<span class="awe-cell-chips"><span class="awe-cell-chip">red</span><span class="awe-cell-chip">green</span><span class="awe-cell-chip">blue</span></span>');
    });

    it("splits by the separator of the column", () => {
      expect(cellHtml({ chips: true, chipSeparator: "|" }, { value: "a, b|c" }))
        .toBe('<span class="awe-cell-chips"><span class="awe-cell-chip">a, b</span><span class="awe-cell-chip">c</span></span>');
    });

    it("collapses the chips over the maximum into a +n chip that lists the hidden ones", () => {
      expect(cellHtml({ chips: true, maxChips: 2 }, { value: "a,b,c,d" }))
        .toBe('<span class="awe-cell-chips"><span class="awe-cell-chip">a</span><span class="awe-cell-chip">b</span>'
          + '<span class="awe-cell-chip awe-cell-chip-more" title="c, d" aria-label="2 more: c, d">+2</span></span>');
    });

    it("shows every chip when they do not exceed the maximum", () => {
      expect(cellHtml({ chips: true, maxChips: 3 }, { value: "a,b,c" })).not.toContain("awe-cell-chip-more");
    });

    it("ignores a maximum that is not a positive number", () => {
      expect(cellHtml({ chips: true, maxChips: 0 }, { value: "a,b" })).not.toContain("awe-cell-chip-more");
    });

    it("ignores the status", () => {
      const html = cellHtml({ chips: true, statusField: "tone" }, { value: "a,b" }, { tone: "danger" });
      expect(html).toContain("awe-cell-chips");
      expect(html).not.toContain("awe-cell-status");
    });
  });

  describe("avatar (#793)", () => {
    const avatar = (initials, tone) =>
      `<span class="awe-cell-avatar awe-cell-avatar-${tone}" data-tone="${tone}" aria-hidden="true">${initials}</span>`;
    const SPACER = '<span class="awe-cell-avatar-spacer" aria-hidden="true"></span>';

    it("shows the initials of the avatar field before the main value, with the tone of the row", () => {
      expect(cellHtml({ avatarField: "ini", avatarToneField: "ton" }, { value: "Carmen Fernández" }, { ini: "Carmen Fernández", ton: "info" }))
        .toBe(`<div class="awe-cell-entity">${avatar("CF", "info")}<div class="awe-cell-entity-text">`
          + '<span class="awe-cell-main" title="Carmen Fernández">Carmen Fernández</span></div></div>');
    });

    it.each([
      ["two words", "Carmen Fernández", "CF"],
      ["one word", "admin", "A"],
      ["particles", "Pat de la Vega", "PV"],
      ["leading words that do not start with a letter", "123 ann (test) lee", "AL"],
      ["words that do not start with a letter at the end", "Manager (test)", "M"],
      ["lower case and accents", "álvaro ruiz", "ÁR"],
      ["initials", "jg", "J"],
      ["spaces around", "  Diego   Moreno  ", "DM"]
    ])("builds the initials of %s", (_name, text, initials) => {
      const { container } = render(<div>{Columns({ address, avatarField: "ini" }, { value: "x" }, {}, false, { ini: text })}</div>);
      expect(container.querySelector(".awe-cell-avatar").textContent).toBe(initials);
    });

    it("reads the label of an avatar cell model", () => {
      expect(cellHtml({ avatarField: "ini" }, { value: "x" }, { ini: { value: "1", label: "Diego Moreno" } })).toContain(avatar("DM", "neutral"));
    });

    it.each(["primary", "info", "success", "warning", "danger", "neutral"])("shows the %s tone", (tone) => {
      expect(cellHtml({ avatarField: "ini", avatarToneField: "ton" }, { value: "Ann" }, { ini: "Ann", ton: tone })).toContain(avatar("A", tone));
    });

    it.each([["an unknown tone", "purple", "neutral"], ["an empty tone", "", "neutral"], ["a missing tone", undefined, "neutral"],
      ["a markup tone", "<b>x</b>", "neutral"], ["a tone to normalise", " Danger ", "danger"], ["a tone cell model", { value: "warning", label: "W" }, "warning"]])
    ("reads %s", (_name, ton, expected) => {
      expect(cellHtml({ avatarField: "ini", avatarToneField: "ton" }, { value: "Ann" }, { ini: "Ann", ton })).toContain(avatar("A", expected));
    });

    it("is neutral without a tone field", () => {
      expect(cellHtml({ avatarField: "ini" }, { value: "Ann" }, { ini: "Ann" })).toContain(avatar("A", "neutral"));
    });

    it.each([["empty", ""], ["missing", undefined], ["without letters", "(1)"]])
    ("shows no avatar, only an aligned space, when the value is %s", (_name, ini) => {
      expect(cellHtml({ avatarField: "ini" }, { value: "Ann" }, { ini }))
        .toBe(`<div class="awe-cell-entity">${SPACER}<div class="awe-cell-entity-text"><span class="awe-cell-main" title="Ann">Ann</span></div></div>`);
    });

    it("combines with a secondary line and a status", () => {
      const node = { avatarField: "ini", avatarToneField: "ton", secondaryField: "login", statusField: "state" };
      expect(cellHtml(node, { value: "Ann Lee" }, { ini: "Ann Lee", ton: "warning", login: "alee", state: "success" }))
        .toBe(`<div class="awe-cell-entity">${avatar("AL", "warning")}<div class="awe-cell-entity-text"><div class="awe-cell-lines">`
          + '<span class="awe-cell-main"><span class="awe-cell-status awe-cell-status-success" data-tone="success" title="Ann Lee">Ann Lee</span></span>'
          + '<span class="awe-cell-secondary">alee</span></div></div></div>');
    });

    it("renders the markup of the avatar and the main value as text", () => {
      const { container } = render(<div>{Columns({ address, avatarField: "ini" }, { value: "<b>x</b>" }, {}, false, { ini: "<i>y</i> z" })}</div>);
      expect(container.querySelector("b, i")).toBeNull();
      expect(container.querySelector(".awe-cell-main").textContent).toBe("<b>x</b>");
      expect(container.querySelector(".awe-cell-avatar").textContent).toBe("Z");
    });

    it("is ignored in a column with a component", () => {
      const { container } = render(<div>{Columns({ address, component: "text", avatarField: "ini" }, { value: "Ann" }, {}, false, { ini: "Ann" })}</div>);
      expect(container.querySelector(".awe-cell-entity, .awe-cell-avatar, .awe-cell-avatar-spacer")).toBeNull();
    });

    it("ignores a tone field without an avatar field", () => {
      expect(cellHtml({ avatarToneField: "ton" }, { value: "Ann" }, { ton: "info" })).toBe(cellHtml({}, { value: "Ann" }));
    });

    it("reads the avatar from the row of the grid", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState(
          [{ name: "name", label: "Name", avatarField: "ini", avatarToneField: "ton", secondaryField: "email", sortable: true }],
          [{ id: "r1", name: "Ana Torres", ini: "Ana Torres", ton: "success", email: "ana@almis.com" }])
      });

      const cell = container.querySelector("td.name");
      expect(cell.querySelector(".awe-cell-avatar.awe-cell-avatar-success")).toHaveTextContent("AT");
      expect(cell.querySelector(".awe-cell-avatar")).toHaveAttribute("aria-hidden", "true");
      expect(cell.querySelector(".awe-cell-entity-text .awe-cell-main")).toHaveTextContent("Ana Torres");
      expect(cell.querySelector(".awe-cell-entity-text .awe-cell-secondary")).toHaveTextContent("ana@almis.com");
    });
  });

  describe("icon (#793)", () => {
    const ICON = '<span class="awe-cell-icon" aria-hidden="true"><svg class="awe-icon" aria-hidden="true" focusable="false">'
      + '<use href="/icons/lucide-sprite.svg#shield-check"></use></svg></span>';

    it("shows the icon of the row inside the status, in place of its dot", () => {
      expect(cellHtml({ statusField: "ton", iconField: "icn" }, { value: "On" }, { ton: "neutral", icn: "lucide:shield-check" }))
        .toBe(`<span class="awe-cell-status awe-cell-status-neutral awe-cell-status-plain" data-tone="neutral" title="On">${ICON}On</span>`);
    });

    it.each([["empty", ""], ["missing", undefined], ["an unknown Lucide name", "lucide:not-an-icon"]])
    ("shows a plain status, without dot nor icon, when the icon of the row is %s", (_name, icn) => {
      expect(cellHtml({ statusField: "ton", iconField: "icn" }, { value: "Off" }, { ton: "neutral", icn }))
        .toBe('<span class="awe-cell-status awe-cell-status-neutral awe-cell-status-plain" data-tone="neutral" title="Off">Off</span>');
    });

    it("shows the icon before the text of a column without status", () => {
      expect(cellHtml({ iconField: "icn" }, { value: "On" }, { icn: "lucide:shield-check" }))
        .toBe(`<span class="awe-cell-icon-text" title="On">${ICON}On</span>`);
    });

    it("reads the value of an icon cell model and resolves the icon names of the screens", () => {
      const { container } = render(<div>{Columns({ address, iconField: "icn" }, { value: "On" }, {}, false, { icn: { value: "fa-check", label: "x" } })}</div>);
      expect(container.querySelector(".awe-cell-icon use").getAttribute("href")).toBe("/icons/lucide-sprite.svg#check");
    });

    it("combines with a secondary line", () => {
      expect(cellHtml({ statusField: "ton", iconField: "icn", secondaryField: "since" }, { value: "On" }, { ton: "success", icn: "lucide:shield-check", since: "2026" }))
        .toBe('<div class="awe-cell-lines"><span class="awe-cell-main">'
          + `<span class="awe-cell-status awe-cell-status-success awe-cell-status-plain" data-tone="success" title="On">${ICON}On</span></span>`
          + '<span class="awe-cell-secondary">2026</span></div>');
    });

    it("renders the value as text and the icon as decorative", () => {
      const { container } = render(<div>{Columns({ address, statusField: "ton", iconField: "icn" }, { value: "<b>x</b>" }, {}, false, { icn: "lucide:shield-check" })}</div>);
      expect(container.querySelector("b")).toBeNull();
      expect(container.querySelector(".awe-cell-status").textContent).toBe("<b>x</b>");
      expect(container.querySelector(".awe-cell-icon")).toHaveAttribute("aria-hidden", "true");
    });

    it("is ignored in a column with a component and in a chips column", () => {
      const { container } = render(<div>
        {Columns({ address, component: "text", iconField: "icn" }, { value: "On" }, {}, false, { icn: "lucide:shield-check" })}
        {Columns({ address, chips: true, iconField: "icn" }, { value: "a,b" }, {}, false, { icn: "lucide:shield-check" })}
      </div>);
      expect(container.querySelector(".awe-cell-icon, .awe-cell-icon-text")).toBeNull();
    });

    it("keeps the status with its dot in a column without icon field", () => {
      expect(cellHtml({ statusField: "ton" }, { value: "Active" }, { ton: "success", icn: "lucide:shield-check" }))
        .toBe('<span class="awe-cell-status awe-cell-status-success" data-tone="success" title="Active">Active</span>');
    });

    it("reads the icon from the row of the grid", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState(
          [{ name: "twoFa", label: "2FA", statusField: "ton", iconField: "icn", sortable: true }],
          [{ id: "r1", twoFa: "On", ton: "neutral", icn: "lucide:shield-check" }, { id: "r2", twoFa: "Off", ton: "neutral", icn: "" }])
      });

      const cells = container.querySelectorAll("td.twoFa");
      expect(cells[0].querySelector(".awe-cell-status-plain .awe-cell-icon use").getAttribute("href")).toBe("/icons/lucide-sprite.svg#shield-check");
      expect(cells[0].querySelector(".awe-cell-status")).toHaveTextContent("On");
      expect(cells[1].querySelector(".awe-cell-icon")).toBeNull();
      expect(cells[1].querySelector(".awe-cell-status-plain")).toHaveTextContent("Off");
    });
  });

  describe("values are text", () => {
    const MARKUP = "<img src=x onerror=alert(1)><b>bold</b>";

    it.each([
      ["secondary", { secondaryField: "extra" }, { extra: MARKUP }, { value: MARKUP }],
      ["status", { statusField: "tone" }, { tone: "info" }, { value: MARKUP }],
      ["chips", { chips: true, maxChips: 1 }, {}, { value: `${MARKUP},x` }]
    ])("renders the markup of a %s cell as text", (_name, node, row, data) => {
      const { container } = render(<div>{Columns({ address, ...node }, data, {}, false, row)}</div>);
      expect(container.querySelector("img, b")).toBeNull();
      expect(container.textContent).toContain(MARKUP);
      expect(container.innerHTML).toContain("&lt;img src=x onerror=alert(1)&gt;");
    });

    it("gives the status its main value as a text title", () => {
      const { container } = render(<div>{Columns({ address, statusField: "tone" }, { value: MARKUP }, {}, false, { tone: "info" })}</div>);
      expect(container.querySelector(".awe-cell-status").getAttribute("title")).toBe(MARKUP);
    });
  });

  describe("columns without the attributes or with a component", () => {
    it("renders a column without the attributes as before", () => {
      expect(cellHtml({}, { value: "Ana" }, { email: "x" }))
        .toBe('<span class="p-cell-text white-space-nowrap p-text-truncate" title="Ana">Ana</span>');
    });

    it("ignores the three features in a column with a component", () => {
      const node = { component: "text", secondaryField: "email", statusField: "tone", chips: true };
      const { container } = render(<div>{Columns({ address, ...node }, { value: "a,b" }, {}, false, { email: "x", tone: "danger" })}</div>);
      expect(container.querySelector(".awe-cell-lines, .awe-cell-status, .awe-cell-chips")).toBeNull();
    });
  });

  describe("grid", () => {
    it("reads the secondary and the status from the row of the grid", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState(
          [{ name: "name", label: "Name", secondaryField: "email", statusField: "tone", sortable: true }],
          [{ id: "r1", name: "Ana", email: "ana@almis.com", tone: "warning" }])
      });

      const cell = container.querySelector("td.name");
      expect(cell.querySelector(".awe-cell-main .awe-cell-status-warning")).toHaveTextContent("Ana");
      expect(cell.querySelector(".awe-cell-secondary")).toHaveTextContent("ana@almis.com");
    });

    it("draws the bar of a progress column in every row of a flat grid, with its second value (#831)", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState(
          [{ name: "done", label: "Done", component: "progress", barMax: 40, barSecondField: "goal", barLeafOnly: true }],
          [{ id: "r1", done: 4.13, goal: 20 }, { id: "r2", done: 10, goal: 5 }])
      });

      const meters = container.querySelectorAll("td.done [role='meter']");
      expect(meters).toHaveLength(2);
      expect(meters[0].querySelector(".awe-data-bar-fill").style.right).toBe("89.675%");
      expect(meters[0].querySelector(".awe-data-bar-lane-second .awe-data-bar-fill").style.right).toBe("50%");
      expect(meters[0]).toHaveAttribute("aria-valuetext", "4.13 / 20");
    });

    it("gives the rest of the width to a column with width * (#793)", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState([{ name: "name", label: "Name", charlength: 30, width: "*" }, { name: "tags", label: "Tags", charlength: 10 }],
          [{ id: "r1", name: "Ana", tags: "a" }])
      });

      // The header cell of the column (the first one) and its body cells
      [container.querySelector("th"), container.querySelector("td.name")].forEach(cell => {
        expect(cell.style.width).toBe("100%");
        expect(cell.style.maxWidth).toBe("0");
        expect(cell.style.minWidth).toBe("");
      });
      expect(container.querySelector("td.tags").style.width).toBe("112px");
    });

    it("shows chips in a grid cell", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState([{ name: "tags", label: "Tags", chips: true, maxChips: 1 }], [{ id: "r1", tags: "a,b,c" }])
      });

      expect(container.querySelectorAll("td.tags .awe-cell-chip")).toHaveLength(2);
      expect(container.querySelector("td.tags .awe-cell-chip-more")).toHaveAttribute("aria-label", "2 more: b, c");
    });
  });
});
