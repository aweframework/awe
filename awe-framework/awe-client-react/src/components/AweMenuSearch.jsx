import React, { useCallback, useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import PropTypes from "prop-types";

import { searchOptions } from "../utilities/menuSearch";
import { getIconNode, translateLabel } from "../utilities";
import { ANY_MODIFIERS, createKeyHandler } from "../utilities/keyboard";
import "./AweMenuSearch.css";

/**
 * Menu option search (command palette).
 *
 * Renders a search toggle and, when open, a text input plus a flat list of
 * matching leaf options with their breadcrumb path. Selecting a result launches
 * that option's actions (via the injected onSelect) exactly as clicking it in
 * the menu would. Matching, translation and allowance are delegated to the pure
 * searchOptions helper so this component only owns presentation and interaction.
 *
 * @param {object} props Component properties
 * @param {Array} props.options Menu option tree (as served for the menu)
 * @param {string} props.menuType Menu orientation ("vertical" | "horizontal")
 * @param {function} props.isAllowed Allowance predicate mirroring the menu filter
 * @param {function} props.onSelect Callback launching the selected option
 */
function AweMenuSearch(props) {
  const { options = [], menuType = "vertical", isAllowed = () => true, onSelect } = props;
  const { t } = useTranslation();

  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [active, setActive] = useState(-1);

  const containerRef = useRef(null);
  const inputRef = useRef(null);

  const searchTitle = translateLabel("BUTTON_SEARCH", t);

  /**
   * Close the option search panel and reset its state.
   */
  const closeSearch = useCallback(() => {
    setOpen(false);
    setQuery("");
    setResults([]);
    setActive(-1);
  }, []);

  /**
   * Toggle the option search panel, resetting its state on close.
   */
  const toggleSearch = () => (open ? closeSearch() : setOpen(true));

  /**
   * Recompute search results from the current query.
   * @param {string} value Current query text
   */
  const onSearchChange = (value) => {
    setQuery(value);
    const found = searchOptions(options, value, {
      translate: (key) => translateLabel(key, t),
      isAllowed
    }).map((entry) => ({
      option: entry.option,
      label: entry.path[entry.path.length - 1],
      breadcrumb: entry.path.slice(0, -1).join(" › ")
    }));
    setResults(found);
    setActive(found.length ? 0 : -1);
  };

  /**
   * Launch the selected option and close the search panel.
   * @param {object} entry Search result entry
   */
  const selectResult = useCallback((entry) => {
    const option = entry && entry.option;
    if (option && option.actions) {
      onSelect(option);
    }
    closeSearch();
  }, [onSelect, closeSearch]);

  /**
   * Keyboard navigation inside the search panel.
   * @param {object} event Keydown event
   */
  const handleSearchKey = createKeyHandler([
    {
      shortcut: {key: "ArrowDown", ...ANY_MODIFIERS},
      when: () => results.length > 0,
      handler: (event) => {
        setActive(prev => (prev + 1) % results.length);
        event.preventDefault();
      }
    },
    {
      shortcut: {key: "ArrowUp", ...ANY_MODIFIERS},
      when: () => results.length > 0,
      handler: (event) => {
        setActive(prev => (prev - 1 + results.length) % results.length);
        event.preventDefault();
      }
    },
    {
      shortcut: {key: "Enter", ...ANY_MODIFIERS},
      when: () => results.length > 0,
      handler: (event) => {
        selectResult(results[Math.max(active, 0)]);
        event.preventDefault();
      }
    },
    {
      shortcut: {key: "Escape", ...ANY_MODIFIERS},
      handler: (event) => {
        closeSearch();
        event.preventDefault();
      }
    }
  ]);
  // The keys of an input method composition belong to it (keyCode 229 is Safari's composition end with key "Enter").
  // React does not copy isComposing to its keyboard event: it is read from the native one
  const onSearchKeydown = (event) => event.nativeEvent?.isComposing || event.keyCode === 229 || handleSearchKey(event);

  // Focus the input when the panel opens
  useEffect(() => {
    if (open && inputRef.current) {
      inputRef.current.focus();
    }
  }, [open]);

  // Close the search panel when clicking outside of it
  useEffect(() => {
    if (!open) {
      return undefined;
    }
    const onDocumentClick = (event) => {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        closeSearch();
      }
    };
    document.addEventListener("click", onDocumentClick);
    return () => document.removeEventListener("click", onDocumentClick);
  }, [open, closeSearch]);

  return (
    <div ref={containerRef} className={`awe-menu-search awe-menu-search-${menuType}${open ? " open" : ""}`}>
      <button type="button" className="awe-menu-search-toggle" onClick={toggleSearch}
        title={searchTitle} aria-label={searchTitle} aria-expanded={open}>
        {getIconNode("pi-search", { "aria-hidden": "true" })}
        {menuType === "vertical" && (
          <span className="awe-menu-search-toggle-label">{searchTitle}</span>
        )}
      </button>
      {open && (
        <div className="awe-menu-search-panel">
          <input ref={inputRef} type="text" className="awe-menu-search-input p-inputtext"
            value={query} onChange={(event) => onSearchChange(event.target.value)}
            onKeyDown={onSearchKeydown} placeholder={searchTitle} aria-label={searchTitle} />
          {results.length > 0 && (
            <ul className="awe-menu-search-results" role="listbox">
              {results.map((entry, index) => (
                <li key={`${entry.option.name}-${index}`} role="presentation">
                  <button type="button" role="option" aria-selected={index === active}
                    className={`awe-menu-search-result${index === active ? " active" : ""}`}
                    onClick={() => selectResult(entry)}
                    onMouseEnter={() => setActive(index)}
                    tabIndex={-1}>
                    <span className="awe-menu-search-label">{entry.label}</span>
                    {entry.breadcrumb && (
                      <span className="awe-menu-search-breadcrumb">{entry.breadcrumb}</span>
                    )}
                  </button>
                </li>
              ))}
            </ul>
          )}
          {query && results.length === 0 && (
            <div className="awe-menu-search-empty">{translateLabel("MENU_SEARCH_EMPTY", t)}</div>
          )}
        </div>
      )}
    </div>
  );
}

AweMenuSearch.propTypes = {
  options: PropTypes.array,
  menuType: PropTypes.oneOf(["vertical", "horizontal"]),
  isAllowed: PropTypes.func,
  onSelect: PropTypes.func.isRequired
};

export default AweMenuSearch;
