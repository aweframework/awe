import React, {useCallback, useLayoutEffect, useRef, useState} from "react";
import {createPortal} from "react-dom";
import PropTypes from "prop-types";
import {classNames} from "../utilities/components";
import {Components} from "../utilities/structure";
import {
  AdvancedHolder,
  AdvancedPanel,
  AdvancedReveal,
  AdvancedToggle,
  ADVANCED_MODE_INLINE,
  FilterChips,
  getAdvancedRegionId,
  useAdvancedWindow,
  useSaveAdvancedState
} from "./AweWindowAdvanced";
import {SavedSearches} from "./AweWindowSavedSearches";

/**
 * Hosts of the children of the advanced block of a window that keeps saved searches (#915): each child renders in its
 * own element (laid out as if it were not there), so a saved search can move it to the main row, after the essential
 * criteria, while it stays mounted (dependencies, validation and keep-criteria do not change).
 * @param {boolean} enabled The window keeps saved searches
 * @return {function(object, number): HTMLElement|null} Host of a child
 */
function useChildHosts(enabled) {
  const [hosts] = useState(() => new Map());
  return useCallback((node, index) => {
    if (!enabled) {
      return null;
    }
    const key = node.id || `#${index}`;
    if (!hosts.has(key)) {
      const element = document.createElement("div");
      element.className = "window-advanced-item";
      hosts.set(key, element);
    }
    return hosts.get(key);
  }, [enabled, hosts]);
}

/**
 * Advanced block of a window with its advanced search (#790): the disclosure row (the toggle with its count badge, the
 * saved searches of a window that keeps them, then the filters in effect) and its children, which stay mounted. Inline,
 * they open in a region below the row (the row never moves); in a side-panel window, they live in a hidden holder of the
 * block and move into the side panel while it is open. In a window that keeps saved searches, the criteria of the layout
 * of the search in effect move to the main row, before the block, and count as essential ones (#915).
 */
function AdvancedBlock({id, style, advanced, elementList}) {
  const {id: windowId, label, mode, open, setOpen, advancedCriteria, searchCriteria, savedSearches, mainCriteria,
    blockCriteria} = advanced;
  const inline = mode === ADVANCED_MODE_INLINE;
  const holderRef = useRef(null);
  const regionRef = useRef(null);
  const mainRef = useRef(null);
  // The container of the children of a side-panel block, moved between its holder and the side panel
  const [container] = useState(() => {
    if (inline) {
      return null;
    }
    const element = document.createElement("div");
    element.className = "window-advanced-criteria";
    return element;
  });
  const getHost = useChildHosts(savedSearches);
  const saveState = useSaveAdvancedState(windowId, searchCriteria.length ? searchCriteria : advancedCriteria);
  // Only the toggles of the user in an inline window are remembered: not the side panel, nor the reveal of an error
  const toggle = useCallback(() => {
    setOpen(!open);
    if (inline) {
      saveState(!open);
    }
  }, [open, setOpen, inline, saveState]);
  const reveal = useCallback(() => setOpen(true), [setOpen]);
  const close = useCallback(() => setOpen(false), [setOpen]);

  // Each host goes to the main row or to the block, in the order of the block. A place that already holds its hosts in
  // order is not touched, so a criterion that does not move keeps the focus
  useLayoutEffect(() => {
    if (!savedSearches) {
      return;
    }
    const hosts = elementList.map((node, index) => ({main: mainCriteria.includes(node.id), host: getHost(node, index)}));
    [[mainRef.current, true], [inline ? regionRef.current : container, false]].forEach(([target, main]) => {
      const placed = hosts.filter(item => item.main === main).map(item => item.host);
      const current = [...target.children];
      if (current.length !== placed.length || placed.some((host, index) => current[index] !== host)) {
        placed.forEach(host => target.appendChild(host));
      }
    });
  }, [savedSearches, inline, container, elementList, mainCriteria, getHost]);

  const children = elementList.map((node, index) => {
    const child = Components(node, index);
    return savedSearches ? createPortal(child, getHost(node, index), node.id || `#${index}`) : child;
  });
  const blockChildren = savedSearches ? null : children;

  return <>
    {savedSearches && <div ref={mainRef} className="window-advanced-main">{children}</div>}
    <div id={id} className={classNames("window-advanced", style)}>
      <div className="window-advanced-row">
        <AdvancedToggle id={windowId} mode={mode} open={open} advancedCriteria={blockCriteria} onToggle={toggle}/>
        {savedSearches && <SavedSearches id={windowId} label={label} advancedCriteria={advancedCriteria}
                                         searchCriteria={searchCriteria}/>}
        <FilterChips id={windowId} searchCriteria={searchCriteria} advancedCriteria={blockCriteria}
                     advancedHidden={!open}/>
      </div>
      {inline ?
        <div ref={regionRef} id={getAdvancedRegionId(windowId)} className="window-advanced-region" hidden={!open}>
          {blockChildren}
        </div> :
        <>
          <AdvancedHolder container={container} holderRef={holderRef}/>
          {blockChildren && createPortal(blockChildren, container)}
          <AdvancedPanel id={windowId} label={label} open={open} advancedCriteria={advancedCriteria}
                         searchCriteria={searchCriteria} container={container} holderRef={holderRef} onClose={close}/>
        </>}
      <AdvancedReveal advancedCriteria={blockCriteria} hidden={!open} onReveal={reveal}/>
    </div>
  </>;
}

AdvancedBlock.propTypes = {
  id: PropTypes.string,
  style: PropTypes.string,
  advanced: PropTypes.object,
  elementList: PropTypes.array
};

/**
 * Advanced block (<advanced>) of a window. Outside a window with advanced search its children render as they are.
 */
function AweAdvanced({id, style, elementList = []}) {
  const advanced = useAdvancedWindow();
  if (!advanced) {
    return <>{elementList.map((node, index) => Components(node, index))}</>;
  }
  return <AdvancedBlock id={id} style={style} advanced={advanced} elementList={elementList}/>;
}

AweAdvanced.propTypes = {
  id: PropTypes.string,
  style: PropTypes.string,
  elementList: PropTypes.array
};

export default AweAdvanced;
