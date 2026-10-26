// Type-level assertions for utilities/map, checked by `npm run typecheck` (not run by Jest)
import {DEFAULT_LATITUDE, DEFAULT_LONGITUDE, toCoordinate, toPosition} from "../../../../src/utilities/map";

// The default column names are literals
const latitude: "latitude" = DEFAULT_LATITUDE;
const longitude: "longitude" = DEFAULT_LONGITUDE;
// @ts-expect-error the default column name is a literal, not any text
const wrongName: "lat" = DEFAULT_LATITUDE;

// A coordinate is a number, or null when the value is empty or not numeric
const coordinate: number | null = toCoordinate("40.4");
const fromNothing: number | null = toCoordinate(undefined);
// @ts-expect-error a coordinate can be null: it is not always a number
const alwaysNumber: number = toCoordinate("40.4");

// A position is a [latitude, longitude] pair, or null
const position: [number, number] | null = toPosition({lat: 1, lng: 2}, "lat", "lng");
const noRow: [number, number] | null = toPosition(null, "lat", "lng");
const missingRow: [number, number] | null = toPosition(undefined, DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
// @ts-expect-error a position can be null
const alwaysPair: [number, number] = toPosition({}, "lat", "lng");
// @ts-expect-error the names of the columns are text
toPosition({}, 1, 2);

export {latitude, longitude, wrongName, coordinate, fromNothing, alwaysNumber, position, noRow, missingRow, alwaysPair};
