import {useLayoutEffect} from "react";
import {frozenLeftProperty, getFrozenOffsets} from "../utilities/grid";

// Frozen header cells on the left (the row edit column is frozen at the right)
const FROZEN_HEADER_CELLS = ":scope > .p-datatable-wrapper > table > thead > tr:first-child > th.p-frozen-column:not(.grid-row-editor-column)";

/**
 * Pin the frozen cells of the rows of a grid (#943). PrimeReact measures each frozen body cell to find its offset, a
 * forced layout per row; instead the grid measures its frozen header cells once, and again only when they resize, and
 * shares their offsets with the rows through CSS custom properties.
 * @param {Object} tableRef Reference to the DataTable
 * @param {string} frozenKey Identity of the frozen columns (empty without frozen columns)
 */
export function useFrozenOffsets(tableRef, frozenKey) {
  useLayoutEffect(() => {
    const element = tableRef.current?.getElement?.();
    if (!frozenKey || !element) {
      return undefined;
    }
    const cells = [...element.querySelectorAll(FROZEN_HEADER_CELLS)];
    const update = () => {
      const offsets = getFrozenOffsets(cells.map(cell => cell.getBoundingClientRect().width));
      offsets.forEach((offset, index) => element.style.setProperty(frozenLeftProperty(index), `${offset}px`));
    };
    update();
    if (typeof ResizeObserver === "undefined") {
      return undefined;
    }
    const observer = new ResizeObserver(update);
    cells.forEach(cell => observer.observe(cell));
    return () => observer.disconnect();
  }, [tableRef, frozenKey]);
}
