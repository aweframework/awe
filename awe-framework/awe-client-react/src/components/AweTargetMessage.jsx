import React, {useEffect, useRef} from "react";
import PropTypes from "prop-types";
import {useDispatch} from "react-redux";
import {OverlayPanel} from "primereact/overlaypanel";
import {acceptAction} from "../redux/actions/actions";
import {removePopover} from "../redux/actions/messages";
import {findMessageTarget} from "../utilities/messageTarget";
import {TestIds, testHook} from "../utilities/testIds";
import {alertType, popoverPassThrough} from "../utilities/testPassThrough";

import "./AweTargetMessage.less";

/**
 * Popover of a target message, anchored on its target component. It hides on a click outside, on escape, after the
 * timeout of its type or when its target leaves the page, and then finishes its action. A new message replaces the text of the popover shown; the action
 * of the replaced message is not finished again when the panel hides.
 * @category Components
 */
function AweTargetMessage({popover}) {
  const dispatch = useDispatch();
  const panelRef = useRef(null);
  // Message the panel is showing (the one to finish when it hides)
  const shownRef = useRef(null);

  const finish = (message) => {
    dispatch(acceptAction(message.action));
    dispatch(removePopover(message));
  };

  useEffect(() => {
    // A new message replaces the shown one, whose action was finished by whoever replaced it: hiding the panel must
    // not finish it again
    shownRef.current = null;
    const target = popover && findMessageTarget(popover.address);
    if (!target) {
      panelRef.current?.hide();
      if (popover) {
        finish(popover);
      }
      return undefined;
    }
    shownRef.current = popover;
    panelRef.current?.show(null, target);
    const timer = popover.life > 0 ? setTimeout(() => panelRef.current?.hide(), popover.life) : null;
    // The target can leave the page while the panel is shown (screen change, view reload, dialog close): the panel
    // hides then, so a message without timeout does not keep its action running
    const observer = new MutationObserver(() => {
      if (!target.isConnected) {
        observer.disconnect();
        panelRef.current?.hide();
      }
    });
    observer.observe(document.body, {childList: true, subtree: true});
    return () => {
      clearTimeout(timer);
      observer.disconnect();
    };
    // Each message is shown once: a new message has a new id
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [popover?.id]);

  const onHide = () => {
    const shown = shownRef.current;
    shownRef.current = null;
    if (shown) {
      finish(shown);
    }
  };

  const {severity, summary, detail, address} = popover || {};
  return <OverlayPanel ref={panelRef} dismissable onHide={onHide}
                       className={`target-message target-message-${alertType(severity)}`}
                       pt={popoverPassThrough(severity, address?.component)}>
    {summary && <div className="target-message-title" {...testHook(TestIds.popoverTitle)}>{summary}</div>}
    <div className="target-message-content" {...testHook(TestIds.popoverContent)}>{detail}</div>
  </OverlayPanel>;
}

AweTargetMessage.propTypes = {
  popover: PropTypes.shape({
    id: PropTypes.number,
    severity: PropTypes.string,
    summary: PropTypes.string,
    detail: PropTypes.string,
    life: PropTypes.number,
    address: PropTypes.object,
    action: PropTypes.object
  })
};

export default AweTargetMessage;
