import {useEffect, useRef} from "react";

// Mounted titles: the highest level wins (a sub view over its view), ties go to the most recently set one.
// Child effects run before parent effects, so the order of the effects alone cannot decide it
const titles = [];
let sequence = 0;

const wins = (entry, winner) => !winner || entry.level > winner.level ||
  (entry.level === winner.level && entry.order > winner.order);

const applyTitle = () => {
  const current = titles.reduce((winner, entry) => wins(entry, winner) ? entry : winner, null);
  if (current) {
    document.title = current.title;
  }
};

/**
 * Set the document title while the component is mounted
 * @param {string|null} title Title (null or undefined does not set any title)
 * @param {number} level Nesting level (higher levels win)
 */
export function useDocumentTitle(title, level = 0) {
  useEffect(() => {
    if (title == null) {
      return undefined;
    }
    const entry = {title, level, order: ++sequence};
    titles.push(entry);
    applyTitle();
    return () => {
      titles.splice(titles.indexOf(entry), 1);
      applyTitle();
    };
  }, [title, level]);
}

/**
 * Keep a stylesheet link in the document head while the component is mounted
 * @param {string} href Stylesheet URL
 */
export function useHeadStylesheet(href) {
  const link = useRef(null);

  useEffect(() => {
    link.current = document.createElement("link");
    link.current.rel = "stylesheet";
    document.head.append(link.current);
    return () => {
      link.current.remove();
      link.current = null;
    };
  }, []);

  useEffect(() => {
    link.current.setAttribute("href", href);
  }, [href]);
}
