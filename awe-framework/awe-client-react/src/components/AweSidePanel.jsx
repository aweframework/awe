import React, { useCallback, useRef } from "react";
import { Sidebar } from "primereact/sidebar";
import { Components } from "../utilities/structure";
import { classNames } from "../utilities/components";
import { translateLabel } from "../utilities";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { useComponentState } from "../hooks/useComponentState";
import { useDeferredLoad } from "../hooks/useDeferredLoad";
import { useFocusTrap } from "../hooks/useFocusTrap";
import { addActionsTop } from "../redux/actions/actions";
import PropTypes from "prop-types";
import { sidePanelPassThrough } from "../utilities/testPassThrough";
import "./AweSidePanel.less";

const DEFAULT_MAX_WIDTH = "40rem";

/**
 * Side panel: a dialog anchored to the left or right edge of the screen (#843). It is opened and closed as a dialog,
 * closes with its close button, a click outside or Esc, and keeps the keyboard focus inside while it is shown.
 */
function AweSidePanel(props) {
  const { id, elementList = [] } = props;
  const { address, attributes } = useComponentState(id);
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const sidebar = useRef(null);
  const shown = !!attributes?.isShowing;

  const hide = useCallback(() => {
    if (attributes.isShowing) {
      // on-close="reject" (acceptOnClose false) rejects the action that opened the panel, as a dialog does
      const type = attributes.acceptOnClose === false ? "close-cancel" : "close";
      dispatch(addActionsTop([{ address, type }]));
    }
  }, [dispatch, attributes?.isShowing, attributes?.acceptOnClose, address]);

  // A deferred panel loads the data of its components when it is shown for the first time
  useDeferredLoad(address, attributes, shown);

  useFocusTrap(() => sidebar.current?.getElement(), shown);

  // If address is undefined, return skeleton
  if (!address) {
    return <></>;
  }

  const { style, label, position = "right", maxWidth = DEFAULT_MAX_WIDTH } = attributes;
  return (
    <Sidebar
      ref={sidebar}
      id={id}
      className={classNames("side-panel", style)}
      style={{ width: "100%", maxWidth }}
      position={position}
      header={<span className="side-panel-title">{translateLabel(label, t)}</span>}
      visible={shown}
      onHide={hide}
      pt={sidePanelPassThrough(id, shown)}
    >
      {elementList.map((node, index) => Components(node, index))}
    </Sidebar>
  );
}

AweSidePanel.propTypes = {
  id: PropTypes.string,
  elementList: PropTypes.array
};

export default AweSidePanel;
