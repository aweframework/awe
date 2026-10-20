import React from "react";
import { cleanup, fireEvent, screen } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";
import { Columns } from "../../../src/utilities/structure";
import { getColumnWrap } from "../../../src/utilities/grid";

const LONG = "A long description that does not fit in the width of the column and needs several lines";

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

const address = { view: "report", component: "Grd", row: "1", column: "desc" };

describe("awe-react-client/test/js/components/AweGridWrapTest.jsx", () => {
  afterEach(cleanup);

  describe("getColumnWrap", () => {
    it("does not wrap a column without the attributes", () => {
      expect(getColumnWrap({})).toEqual({ wrap: false, maxLines: null });
    });

    it("wraps without a limit when the column declares wrap", () => {
      expect(getColumnWrap({ wrap: true })).toEqual({ wrap: true, maxLines: null });
    });

    it("wraps with a limit when the column declares a maximum number of lines", () => {
      expect(getColumnWrap({ maxLines: 3 })).toEqual({ wrap: true, maxLines: 3 });
      expect(getColumnWrap({ wrap: true, maxLines: 2 })).toEqual({ wrap: true, maxLines: 2 });
    });

    it("keeps one line when wrap is explicitly false", () => {
      expect(getColumnWrap({ wrap: false, maxLines: 3 })).toEqual({ wrap: false, maxLines: null });
    });

    it("ignores a maximum that is not a positive number", () => {
      expect(getColumnWrap({ maxLines: 0 })).toEqual({ wrap: false, maxLines: null });
      expect(getColumnWrap({ wrap: true, maxLines: "x" })).toEqual({ wrap: true, maxLines: null });
    });
  });

  describe("text cell", () => {
    it("renders as today without the attributes: one line cut with an ellipsis and its value as tooltip", () => {
      renderWithProviders(Columns({ address }, { value: LONG, label: LONG }));

      const cell = screen.getByText(LONG);
      expect(cell).toHaveClass("p-cell-text", "white-space-nowrap", "p-text-truncate");
      expect(cell).not.toHaveClass("p-cell-wrap");
      expect(cell).toHaveAttribute("title", LONG);
    });

    it("wraps over several lines without a limit", () => {
      renderWithProviders(Columns({ address, wrap: true }, { value: LONG, label: LONG }));

      const cell = screen.getByText(LONG);
      expect(cell).toHaveClass("p-cell-text", "p-cell-wrap");
      expect(cell).not.toHaveClass("white-space-nowrap");
      expect(cell).not.toHaveClass("p-cell-clamp");
    });

    it("limits the lines of a cell with a maximum and exposes the full value", () => {
      renderWithProviders(Columns({ address, maxLines: 2 }, { value: LONG, label: LONG }));

      const cell = screen.getByText(LONG);
      expect(cell).toHaveClass("p-cell-wrap", "p-cell-clamp");
      expect(cell.style.getPropertyValue("--cell-max-lines")).toBe("2");
      expect(cell).toHaveAttribute("data-full-text", LONG);
    });

    it("wraps the list of a multiple cell like a text", () => {
      renderWithProviders(Columns({ address, wrap: true, component: "select-multiple" }, [{ value: "a", label: "Alpha", selected: true }, { value: "b", label: "Beta", selected: true }]));

      expect(screen.getByText("Alpha, Beta")).toHaveClass("p-cell-wrap");
    });
  });

  describe("grid", () => {
    it("gives the cell of a wrapping column the wrap class and leaves the others untouched", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState(
          [{ name: "desc", label: "Description", wrap: true, charlength: 10 }, { name: "code", label: "Code", charlength: 10 }],
          [{ id: "r1", desc: LONG, code: "X1" }])
      });

      expect(container.querySelector("td.desc")).toHaveClass("p-cell-wrapping");
      expect(container.querySelector("td.code")).not.toHaveClass("p-cell-wrapping");
    });

    it("shows the full value of a header as tooltip", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState([{ name: "desc", label: "A really long column title", charlength: 4 }], [])
      });

      expect(container.querySelector("th [title='A really long column title']")).not.toBeNull();
    });

    describe("cut text", () => {
      const clamped = (container) => container.querySelector(".p-cell-clamp");
      const cut = (element, isCut) => {
        Object.defineProperty(element, "scrollHeight", { configurable: true, value: isCut ? 80 : 40 });
        Object.defineProperty(element, "clientHeight", { configurable: true, value: 40 });
      };

      const renderClamped = () => renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState([{ name: "desc", label: "Description", maxLines: 2, charlength: 10 }], [{ id: "r1", desc: LONG }])
      });

      it("does not measure the cells while it renders, because every measure forces a layout", () => {
        const measured = [];
        const measure = (property) => jest.spyOn(Element.prototype, property, "get").mockImplementation(function () {
          if (this.classList.contains("p-cell-clamp")) {
            measured.push(property);
          }
          return 0;
        });
        const spies = [measure("scrollHeight"), measure("clientHeight")];
        try {
          const { rerender } = renderClamped();
          rerender(<AweGrid id="Grd" />);

          expect(measured).toEqual([]);
        } finally {
          spies.forEach(spy => spy.mockRestore());
        }
      });

      it("makes the clamped cells reachable with the keyboard without measuring them", () => {
        const { container } = renderClamped();

        expect(clamped(container)).toHaveAttribute("tabindex", "0");
      });

      it("gives every clamped cell the maximum of lines of its column", () => {
        const { container } = renderWithProviders(<AweGrid id="Grd" />, {
          preloadedState: gridState([{ name: "desc", label: "Description", maxLines: 2, charlength: 10 }],
            [{ id: "r1", desc: LONG }, { id: "r2", desc: LONG }])
        });
        const [first, second] = container.querySelectorAll(".p-cell-clamp");

        expect(first.style.getPropertyValue("--cell-max-lines")).toBe("2");
        expect(second.style.getPropertyValue("--cell-max-lines")).toBe("2");
      });

      it("shows the full value on hover only when the text is cut", () => {
        const { container } = renderClamped();
        const cell = clamped(container);
        cut(cell, true);

        fireEvent.mouseOver(cell);
        expect(screen.getByRole("tooltip")).toHaveTextContent(LONG);

        fireEvent.mouseOut(cell);
        expect(screen.queryByRole("tooltip")).toBeNull();
      });

      it("shows no tooltip when the text fits", () => {
        const { container } = renderClamped();
        const cell = clamped(container);
        cut(cell, false);

        fireEvent.mouseOver(cell);
        expect(screen.queryByRole("tooltip")).toBeNull();
      });

      it("shows the full value on keyboard focus and hides it on blur", () => {
        const { container } = renderClamped();
        const cell = clamped(container);
        cut(cell, true);

        fireEvent.focusIn(cell);
        expect(screen.getByRole("tooltip")).toHaveTextContent(LONG);

        fireEvent.focusOut(cell);
        expect(screen.queryByRole("tooltip")).toBeNull();
      });

      it("hides the tooltip with Escape", () => {
        const { container } = renderClamped();
        const cell = clamped(container);
        cut(cell, true);

        fireEvent.focusIn(cell);
        fireEvent.keyDown(cell, { key: "Escape" });
        expect(screen.queryByRole("tooltip")).toBeNull();
      });

      it("hides the tooltip with Escape even with a modifier", () => {
        const { container } = renderClamped();
        const cell = clamped(container);
        cut(cell, true);

        fireEvent.focusIn(cell);
        fireEvent.keyDown(cell, { key: "Escape", shiftKey: true });
        expect(screen.queryByRole("tooltip")).toBeNull();
      });

      it("keeps the tooltip with other keys", () => {
        const { container } = renderClamped();
        const cell = clamped(container);
        cut(cell, true);

        fireEvent.focusIn(cell);
        fireEvent.keyDown(cell, { key: "Enter" });
        expect(screen.getByRole("tooltip")).toBeInTheDocument();
      });
    });
  });
});
