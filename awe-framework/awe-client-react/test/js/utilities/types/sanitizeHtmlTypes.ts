// Type-level assertions for utilities/sanitizeHtml, checked by `npm run typecheck` (not run by Jest)
import type {ReactNode} from "react";
import {ALLOWED_ATTRIBUTES, ALLOWED_TAGS, renderSafeHtml, sanitizeHtml} from "../../../../src/utilities/sanitizeHtml";

const tags: string[] = ALLOWED_TAGS;
const attributes: string[] = ALLOWED_ATTRIBUTES;
const html: string = sanitizeHtml(12.5);
const node: ReactNode = renderSafeHtml(null);

// @ts-expect-error the sanitized html is text
const wrong: number = sanitizeHtml("<b>a</b>");
// @ts-expect-error a value to sanitize is required
sanitizeHtml();
export {tags, attributes, html, node, wrong};
