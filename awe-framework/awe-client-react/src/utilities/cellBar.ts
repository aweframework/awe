/** Scale of a bar of a progress column */
export interface BarScale {
  min: number;
  max: number;
}

/** Fill of a bar, in percentage of the track */
export interface BarFill {
  left: number;
  width: number;
  negative: boolean;
}

/** A segment of a stacked bar, in percentage of the track. The index tells its colour */
export interface BarSegment {
  index: number;
  left: number;
  width: number;
}

/** Attributes of a progress column that ask for a bar with a scale, a second value or segments (#831) */
export interface BarOptions {
  barMin?: number | string;
  barMax?: number | string;
  barSecondField?: string;
  barStackFields?: string;
  barLeafOnly?: boolean;
}

const DEFAULT_SCALE: BarScale = {min: 0, max: 100};

/**
 * Reads a finite number from a number or a numeric string
 * @param value Number, numeric string or anything else
 * @returns The number, or null when the value is empty or not numeric
 */
export function toBarNumber(value: unknown): number | null {
  const text = typeof value === "string" ? value.trim() : value;
  if (text === "" || (typeof text !== "number" && typeof text !== "string")) {
    return null;
  }
  const number = Number(text);
  return Number.isFinite(number) ? number : null;
}

/**
 * Scale of a bar: 0..100 by default and when the given one is not valid (max not above min, not numbers)
 * @param min Lower end
 * @param max Upper end
 */
export function barScale(min: unknown, max: unknown): BarScale {
  const low = toBarNumber(min ?? 0);
  const high = toBarNumber(max ?? 100);
  return low !== null && high !== null && high > low ? {min: low, max: high} : {...DEFAULT_SCALE};
}

/** Position of a value in the track, in percentage, clamped to the scale */
function position(scale: BarScale, value: number): number {
  const clamped = Math.min(Math.max(value, scale.min), scale.max);
  return (clamped - scale.min) / (scale.max - scale.min) * 100;
}

/** Position of the zero in the track: the zero clamped to the scale */
export function barZero(scale: BarScale): number {
  return position(scale, 0);
}

/** Whether the zero is inside the scale, so the bar draws its mark */
export function hasBarZeroMark(scale: BarScale): boolean {
  return scale.min < 0 && scale.max > 0;
}

/**
 * Fill from the zero to the value
 * @param scale Scale of the bar
 * @param value Value, clamped to the scale
 */
export function barFill(scale: BarScale, value: number): BarFill {
  const zero = barZero(scale);
  const end = position(scale, value);
  return {left: Math.min(end, zero), width: Math.abs(end - zero), negative: value < 0};
}

/**
 * Consecutive segments from the zero, one per value. Values that are not positive numbers draw nothing, and what goes
 * past the end of the scale is cut
 * @param scale Scale of the bar
 * @param values Value of each segment
 */
export function barSegments(scale: BarScale, values: unknown[]): BarSegment[] {
  const segments: BarSegment[] = [];
  let total = 0;
  values.forEach((value, index) => {
    const number = toBarNumber(value);
    if (number === null || number <= 0) {
      return;
    }
    const start = position(scale, total);
    total += number;
    const width = position(scale, total) - start;
    if (width > 0) {
      segments.push({index, left: start, width});
    }
  });
  return segments;
}

/**
 * Whether a column asks for the bar with options instead of the plain progress bar
 * @param node Column attributes
 */
export function hasBarOptions(node: BarOptions): boolean {
  const defined = (value: unknown) => value !== undefined && value !== null && value !== "";
  return defined(node.barMin) || defined(node.barMax) || defined(node.barSecondField) || defined(node.barStackFields)
    || node.barLeafOnly === true;
}
