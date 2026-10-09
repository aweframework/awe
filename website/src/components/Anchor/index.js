import React from 'react';
import useBrokenLinks from '@docusaurus/useBrokenLinks';

/**
 * Link target inside a page, for places that cannot be a heading (a table cell, for example).
 *
 * Docusaurus checks `[text](#id)` links against the anchors it knows, and it only learns the ones it renders itself:
 * headings, list items and `<Link id>`. A raw `<a id="...">` or `<a name="...">` in a page is not registered, so every
 * link to it is reported as a broken anchor. This component renders the same `<a id>` and registers it.
 *
 * Usage (the page must be MDX: `format: mdx` in its front matter):
 *
 *   import Anchor from '@site/src/components/Anchor';
 *
 *   | <Anchor id="awe.application.name"/> [awe.application.name](#awe.application.name) | ... |
 */
export default function Anchor({id}) {
  useBrokenLinks().collectAnchor(id);
  return <a id={id}></a>;
}
