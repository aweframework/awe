import React, {useCallback, useRef, useState} from "react";
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

/**
 * Advanced block of a window with its advanced search (#790): the disclosure row (the toggle with its count badge, then
 * the filters in effect) and its children, which stay mounted. Inline, they open in a region below the row (the row
 * never moves); in a side-panel window, they live in a hidden holder of the block and move into the side panel while it
 * is open.
 */
function AdvancedBlock({id, style, advanced, children}) {
  const {id: windowId, label, mode, open, setOpen, advancedCriteria, searchCriteria} = advanced;
  const inline = mode === ADVANCED_MODE_INLINE;
  const holderRef = useRef(null);
  // The container of the children of a side-panel block, moved between its holder and the side panel
  const [container] = useState(() => {
    if (inline) {
      return null;
    }
    const element = document.createElement("div");
    element.className = "window-advanced-criteria";
    return element;
  });
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

  return <div id={id} className={classNames("window-advanced", style)}>
    <div className="window-advanced-row">
      <AdvancedToggle id={windowId} mode={mode} open={open} advancedCriteria={advancedCriteria} onToggle={toggle}/>
      <FilterChips id={windowId} searchCriteria={searchCriteria} advancedCriteria={advancedCriteria}
                   advancedHidden={!open}/>
    </div>
    {inline ?
      <div id={getAdvancedRegionId(windowId)} className="window-advanced-region" hidden={!open}>{children}</div> :
      <>
        <AdvancedHolder container={container} holderRef={holderRef}/>
        {createPortal(children, container)}
        <AdvancedPanel id={windowId} label={label} open={open} advancedCriteria={advancedCriteria}
                       searchCriteria={searchCriteria} container={container} holderRef={holderRef} onClose={close}/>
      </>}
    <AdvancedReveal advancedCriteria={advancedCriteria} hidden={!open} onReveal={reveal}/>
  </div>;
}

AdvancedBlock.propTypes = {
  id: PropTypes.string,
  style: PropTypes.string,
  advanced: PropTypes.object,
  children: PropTypes.node
};

/**
 * Advanced block (<advanced>) of a window. Outside a window with advanced search its children render as they are.
 */
function AweAdvanced({id, style, elementList = []}) {
  const advanced = useAdvancedWindow();
  const children = elementList.map((node, index) => Components(node, index));
  if (!advanced) {
    return <>{children}</>;
  }
  return <AdvancedBlock id={id} style={style} advanced={advanced}>{children}</AdvancedBlock>;
}

AweAdvanced.propTypes = {
  id: PropTypes.string,
  style: PropTypes.string,
  elementList: PropTypes.array
};

export default AweAdvanced;
