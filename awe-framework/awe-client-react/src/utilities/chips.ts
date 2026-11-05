/**
 * How many chips of a row are shown (#922). They go in order and all of them when they fit; when some do not, the
 * counter of the hidden ones keeps its room. At least one chip stays: the layout cuts its text with an ellipsis.
 *
 * @param widths Natural width of every chip
 * @param available Width of the row
 * @param counterWidth Width of the "+K" counter
 * @param gap Space between two items of the row
 * @returns Number of chips to show
 */
export function visibleChipCount(widths: number[], available: number, counterWidth: number, gap: number): number {
  const rowWidth = (count: number) => widths.slice(0, count).reduce((sum, width) => sum + width, 0) + gap * Math.max(count - 1, 0);
  if (rowWidth(widths.length) <= available) {
    return widths.length;
  }
  let count = widths.length - 1;
  while (count > 1 && rowWidth(count) + gap + counterWidth > available) {
    count--;
  }
  return Math.max(count, 1);
}
