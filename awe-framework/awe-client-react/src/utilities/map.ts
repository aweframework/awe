/**
 * Default names of the columns that hold the coordinates of a map row
 */
export const DEFAULT_LATITUDE = "latitude";
export const DEFAULT_LONGITUDE = "longitude";

/**
 * Retrieve a coordinate as a number
 * @param {unknown} value Coordinate
 * @returns {number|null} Number, null when the value is empty or not numeric
 */
export function toCoordinate(value: unknown): number | null {
  if (value === null || value === undefined || value === "") {
    return null;
  }
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

/**
 * Retrieve the [latitude, longitude] pair of a row
 * @param {object} row Row
 * @param {string} latitude Name of the latitude column
 * @param {string} longitude Name of the longitude column
 * @returns {number[]|null} Pair, null when a coordinate is empty or not numeric
 */
export function toPosition(
  row: Record<string, unknown> | null | undefined,
  latitude: string,
  longitude: string
): [number, number] | null {
  const lat = toCoordinate(row?.[latitude]);
  const lng = toCoordinate(row?.[longitude]);
  return lat === null || lng === null ? null : [lat, lng];
}
