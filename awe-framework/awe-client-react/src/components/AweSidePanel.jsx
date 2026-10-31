import React, { useCallback, useRef } from "react";
import { Sidebar } from "primereact/sidebar";
import { Components } from "../utilities/structure";
import { classNames } from "../utilities/components";
import { getIconNode, translateLabel } from "../utilities";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { useComponentState } from "../hooks/useComponentState";
import { useDeferredLoad } from "../hooks/useDeferredLoad";
import { useFocusTrap } from "../hooks/useFocusTrap";
import { addActionsTop } from "../redux/actions/actions";
import PropTypes from "prop-types";
import { sidePanelPassThrough } from "../utilities/testPassThrough";
import { TestIds, testHook } from "../utilities/testIds";
import { getInitials } from "../utilities/initials";
import { AVATAR_TONES, STATUS_TONES, cellIcon, rowSwatch, rowText, rowTone } from "./AweGridCells";
import "./AweSidePanel.less";

const DEFAULT_MAX_WIDTH = "40rem";

/**
 * Identity of the header of a side panel (#793): the avatar (or the colour swatch), title, subtitle and tags of the
 * selected row of its header grid, or null when the panel has no header grid or not exactly one row of it is selected
 * @param {Object} attributes Side panel attributes (headerGrid, header fields and headerTags)
 * @param {Object} [grid] State of the header grid
 * @returns {Object|null} Identity
 */
export function getPanelIdentity(attributes, grid) {
  const { headerGrid, headerTitleField, headerSubtitleField, headerAvatarField, headerAvatarToneField, headerSwatchField,
    headerTags = [] } = attributes || {};
  const selected = headerGrid ? (grid?.model?.values || []).filter(row => row.selected) : [];
  if (selected.length !== 1) {
    return null;
  }
  const [row] = selected;
  const text = field => field ? rowText(row, field) : "";
  return {
    row,
    title: text(headerTitleField),
    subtitle: text(headerSubtitleField),
    initials: getInitials(text(headerAvatarField)),
    tone: headerAvatarToneField ? rowTone(row, headerAvatarToneField, AVATAR_TONES) : "neutral",
    swatch: rowSwatch(row, headerSwatchField),
    tags: headerTags
      .map(tag => ({ ...tag, text: text(tag.field), tone: tag.toneField ? rowTone(row, tag.toneField, STATUS_TONES) : "neutral" }))
      .filter(tag => tag.text.trim() !== "")
  };
}

/**
 * Identity header (#793): a large decorative avatar (or colour swatch), the title (the accessible name of the panel), a muted subtitle and
 * the tags, with the look of the avatar and the status tags of the grid cells
 * @param {Object} identity Identity of the selected row
 * @param {string} titleId Identifier of the title
 * @returns {React.ReactElement} Header
 */
function identityHeader(identity, titleId) {
  const { row, title, subtitle, initials, tone, swatch, tags } = identity;
  return (
    <div className="panel-identity">
      {swatch && <span className="panel-identity-avatar awe-cell-swatch" aria-hidden="true" style={{ "--awe-swatch-color": swatch }}/>}
      {!swatch && initials && <span className={`panel-identity-avatar awe-cell-avatar awe-cell-avatar-${tone}`} data-tone={tone} aria-hidden="true">{initials}</span>}
      <div className="panel-identity-text">
        <h2 className="panel-identity-title" id={titleId} {...testHook(TestIds.sidePanelTitle)}>{title}</h2>
        {subtitle && <p className="panel-identity-subtitle" {...testHook(TestIds.sidePanelSubtitle)}>{subtitle}</p>}
        {tags.length > 0 && <div className="panel-identity-tags">
          {tags.map((tag, index) => (
            <span key={`${tag.field}-${index}`} className={classNames("awe-cell-status", `awe-cell-status-${tag.tone}`, { "awe-cell-status-plain": !!tag.iconField })} data-tone={tag.tone}
              {...testHook(TestIds.sidePanelTag)}>
              {tag.iconField && cellIcon(row, tag.iconField)}{tag.text}
            </span>
          ))}
        </div>}
      </div>
    </div>
  );
}

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
  // The header grid, read only by a panel with an identity header (#793)
  const headerGrid = useComponentState(attributes?.headerGrid);

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

  const { style, label, icon, position = "right", maxWidth = DEFAULT_MAX_WIDTH } = attributes;
  // The icon of the panel goes before its title, as in the AngularJS engine; it is decorative (the title names the panel)
  const titleIcon = icon ? getIconNode(icon, { tag: "span", className: "panel-title-icon", "aria-hidden": "true" }) : null;
  // With a header grid the title (the identity of the selected row, or the label) is the accessible name of the panel
  const titleId = attributes.headerGrid ? `${id}-title` : undefined;
  const identity = getPanelIdentity(attributes, headerGrid);
  const passThrough = sidePanelPassThrough(id, shown);
  const pt = titleId ? { ...passThrough, root: { ...passThrough.root, "aria-labelledby": titleId } } : passThrough;
  return (
    <Sidebar
      ref={sidebar}
      id={id}
      className={classNames("side-panel", style)}
      style={{ width: "100%", maxWidth }}
      position={position}
      header={identity ? identityHeader(identity, titleId)
        : <span className="side-panel-title" id={titleId} {...testHook(TestIds.sidePanelTitle)}>{titleIcon}{translateLabel(label, t)}</span>}
      visible={shown}
      onHide={hide}
      pt={pt}
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
