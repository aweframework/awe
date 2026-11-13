import DOMPurify from "dompurify";
import {sanitizeHtml} from "../../../src/utilities/sanitizeHtml";

describe("awe-react-client/test/js/utilities/sanitizeHtmlTest.js", () => {
  it.each([
    ["an empty value", "", ""],
    ["null", null, ""],
    ["undefined", undefined, ""],
    ["a number", 12.5, "12.5"],
    ["plain text", "plain text", "plain text"],
    ["escaped markup", "&lt;a href='x'&gt;aaa&lt;/a&gt;", "&lt;a href='x'&gt;aaa&lt;/a&gt;"]
  ])("keeps %s", (_name, value, expected) => {
    expect(sanitizeHtml(value)).toBe(expected);
  });

  it.each([
    "<strong>bold</strong>",
    "<em>italic</em>",
    "line<br>break",
    "<p>paragraph</p>",
    "<span class=\"text-danger\">danger</span>",
    "<i class=\"fa fa-check\"></i>",
    "<ul><li>one</li></ul>",
    "<a href=\"https://example.com\" title=\"link\">link</a>"
  ])("keeps the safe markup %s", (html) => {
    expect(sanitizeHtml(html)).toBe(html);
  });

  it.each([
    ["an event handler of an image", "<img src=x onerror=alert(1)>", ""],
    ["a javascript url", "<a href=\"javascript:alert(1)\">go</a>", "<a>go</a>"],
    ["an obfuscated javascript url", "<a href=\"java&#115;cript:alert(1)\">go</a>", "<a>go</a>"],
    ["a script tag", "a<script>alert(1)</script>b", "ab"],
    ["an event handler of a safe tag", "<b onclick=\"alert(1)\">b</b>", "<b>b</b>"],
    ["an iframe", "<iframe src=\"https://example.com\"></iframe>", ""],
    ["an svg", "<svg onload=alert(1)></svg>", ""],
    ["a style tag", "<style>body{display:none}</style>text", "text"],
    ["an inline style", "<span style=\"position:fixed\">x</span>", "<span>x</span>"],
    ["a data attribute", "<span data-x=\"1\">x</span>", "<span>x</span>"],
    ["a form", "<form action=\"https://evil.example\"><input name=a></form>", ""]
  ])("removes %s", (_name, html, expected) => {
    expect(sanitizeHtml(html)).toBe(expected);
  });

  it("protects the opener of the links that open a new window", () => {
    const html = sanitizeHtml("<a href=\"https://example.com\" target=\"_blank\">link</a>");
    expect(html).toContain("target=\"_blank\"");
    expect(html).toContain("rel=\"noopener noreferrer\"");
  });

  it("does not change the behaviour of the shared DOMPurify of the page", () => {
    const html = DOMPurify.sanitize("<a href=\"https://example.com\" target=\"_blank\">link</a>", {ADD_ATTR: ["target"]});

    expect(sanitizeHtml("<a href=\"https://example.com\" target=\"_blank\">link</a>")).toContain("rel=\"noopener noreferrer\"");
    expect(html).not.toContain("rel=");
  });

  it.each([
    ["an icon", "<i class=\"fa fa-check\"></i> Done", "<i class=\"fa fa-check\"></i> Done"],
    ["a bold text", "Total: <b>12</b>", "Total: <b>12</b>"],
    ["a styled span", "<span class=\"text-danger\">Error</span>", "<span class=\"text-danger\">Error</span>"],
    ["a line break", "First line<br>Second line", "First line<br>Second line"],
    ["a line break with a slash", "First line<br/>Second line", "First line<br>Second line"],
    ["a paragraph with a link", "<p>See <a href=\"https://example.com\">the docs</a></p>", "<p>See <a href=\"https://example.com\">the docs</a></p>"]
  ])("keeps the markup of a label with %s", (_name, html, expected) => {
    expect(sanitizeHtml(html)).toBe(expected);
  });

  it.each([
    ["a comparison operator", "Date <=", "Date &lt;="],
    ["a text with new lines", "First line\n\nSecond line", "First line\n\nSecond line"],
    ["a markdown text", "A *separated node* with [databases](#databases)", "A *separated node* with [databases](#databases)"],
    ["an ampersand", "Rock & roll", "Rock & roll"]
  ])("keeps the text of a label with %s", (_name, text, expected) => {
    expect(sanitizeHtml(text)).toBe(expected);
  });

  it("removes the inline style of a label, which is the only markup of a label that the allow-list does not keep", () => {
    expect(sanitizeHtml("<span style=\"color:red\" class=\"text-danger\">Error</span>"))
      .toBe("<span class=\"text-danger\">Error</span>");
  });
});
