import {
  buildStatusStrip,
  parseStatusStrip,
  statusStripSummary,
  statusStripTooltip
} from "../../../main/resources/js/awe/services/grid/statusStrip";

/*
 * Rules of the shared status strip module. The React client keeps an identical copy (src/utilities/statusStrip.js,
 * checked by statusStripParity.jest.spec.js), so these rules hold in both engines.
 */
describe("awe-client-angular/src/test/jest/services/statusStrip.test.js", () => {
  const items = [
    {tone: "success", label: "OK"},
    {tone: "danger", label: "Error"},
    {tone: "warning", label: "Aviso"}
  ];

  it("reads a JSON text or an already parsed list of {tone, label} items in their order, and nothing else", () => {
    expect(parseStatusStrip(JSON.stringify(items))).toEqual(items);
    expect(parseStatusStrip(items)).toEqual(items);
    [null, undefined, "", "not json", "[]", "{}", "[1, 2]", "[\"OK\"]", 7, {tone: "success", label: "OK"}, [],
      [{tone: "success"}], [{tone: "success", label: ""}], [{tone: "success", label: 3}], [items[0], null],
      [items[0], [items[1]]]
    ].forEach(value => expect(parseStatusStrip(value)).toEqual([]));
    expect(buildStatusStrip("oops")).toBeNull();
  });

  it("draws an unknown or missing tone as neutral", () => {
    expect(parseStatusStrip([{tone: "purple", label: "Skipped"}, {label: "Queued"}, {tone: "info", label: "Info"}]))
      .toEqual([{tone: "neutral", label: "Skipped"}, {tone: "neutral", label: "Queued"}, {tone: "info", label: "Info"}]);
  });

  it("summarises the items as the count of every label, in the order each label first appears", () => {
    const strip = [
      {tone: "success", label: "OK"}, {tone: "warning", label: "Aviso"}, {tone: "success", label: "OK"},
      {tone: "danger", label: "Error"}, {tone: "success", label: "OK"}
    ];
    expect(statusStripSummary(strip)).toBe("3 OK, 1 Aviso, 1 Error");
    expect(buildStatusStrip(JSON.stringify(strip))).toEqual({items: strip, label: "3 OK, 1 Aviso, 1 Error"});
  });

  it("keeps the optional text title of an item, ignores any other title and still summarises by label", () => {
    const strip = [
      {tone: "success", label: "OK", title: "#4807 · 17/11/2026 08:05 · 1 min 12 s"},
      {tone: "success", label: "OK", title: "#4806 · 17/11/2026 07:05 · 58 s"},
      {tone: "danger", label: "Error", title: ""},
      {tone: "warning", label: "Aviso", title: 12}
    ];
    expect(buildStatusStrip(strip)).toEqual({
      items: [strip[0], strip[1], {tone: "danger", label: "Error"}, {tone: "warning", label: "Aviso"}],
      label: "2 OK, 1 Error, 1 Aviso"
    });
  });

  it("builds the tooltip of a bar as the label and the title on two lines, or the label alone when it has no title", () => {
    expect(statusStripTooltip({tone: "success", label: "OK", title: "#4807 · 17/11/2026 08:05"}))
      .toBe("OK\n#4807 · 17/11/2026 08:05");
    expect(statusStripTooltip({tone: "danger", label: "Error"})).toBe("Error");
  });
});
