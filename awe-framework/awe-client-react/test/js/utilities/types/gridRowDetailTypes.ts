// Type-level assertions for utilities/gridRowDetail, checked by `npm run typecheck` (not run by Jest)
import {
  getRowDetailInstanceId,
  getRowDetailParameters,
  isRowDetailComponent,
  renderRowDetail
} from "../../../../src/utilities/gridRowDetail";

const row = {name: "Ada", city: {value: "M", label: "Madrid"}, selected: true};

// The detail is plain text
const detail: string = renderRowDetail("{name} lives in {city}", row);
const emptyDetail: string = renderRowDetail(undefined, null);
const numberTemplate: string = renderRowDetail(null, undefined);
// @ts-expect-error the detail is text, not a number
const wrongDetail: number = renderRowDetail("{name}", row);
// @ts-expect-error the row is an object
renderRowDetail("{name}", "Ada");

// The instance identifier is text, built from a text or a number row identifier
const instance: string = getRowDetailInstanceId("Btn", 7);
const textInstance: string = getRowDetailInstanceId("Btn", "r7");
// @ts-expect-error the identifier of the row is text or a number
getRowDetailInstanceId("Btn", {});

// The request parameters are indexed by text; the row is optional
const parameters: Record<string, unknown> = getRowDetailParameters("Grd", row);
const noRowParameters: Record<string, unknown> = getRowDetailParameters("Grd");
// @ts-expect-error the parameters are not a list
const wrongParameters: unknown[] = getRowDetailParameters("Grd", row);

// The check answers with a boolean for a component state, or for nothing
const belongs: boolean = isRowDetailComponent({rowDetailInstance: "Grd-7"});
const none: boolean = isRowDetailComponent(undefined);
// @ts-expect-error the answer is a boolean
const asText: string = isRowDetailComponent(null);

export {detail, emptyDetail, numberTemplate, wrongDetail, instance, textInstance, parameters, noRowParameters, wrongParameters, belongs, none, asText};
