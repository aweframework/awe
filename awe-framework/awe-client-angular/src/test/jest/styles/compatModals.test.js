import {compatFamilyCases, describeLessOnly} from "./support/compatFamily";
import {modals} from "./support/compatFamilies";

// Compiling the LESS build and Tailwind takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

describe(`awe-framework/awe-client-angular/src/test/jest/styles/compat ${modals.title}`, () => {
  it.each(compatFamilyCases(modals))("%s", (title, check) => check());
});

describeLessOnly("modals", "modals.css", [
  ".modal.side-panel .modal-dialog",
  ".modal.grid-columns-side-panel .grid-column-item",
  ".bootbox .modal-footer",
  ".note-editor .note-dialog .note-help-dialog .modal-content",
  ".alert .close",
  ".awe-popover .modal-footer",
  ".modal-dialog .close.fa-times",
  ".modal-content > .wizard .wizard-wrapper",
  ".modal .bootstrap-timepicker-widget.dropdown-menu"
]);
