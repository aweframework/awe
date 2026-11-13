import React, {createContext, useContext} from "react";
import {useTranslation} from "react-i18next";
import {useComponentState} from "../hooks/useComponentState";
import {classNames} from "../utilities/components";
import "./ScreenInspect.less";

/**
 * Inspect mode of the screen personalization editor (#792): { configurable: Set of ids, selected: id, personalized: Set of ids,
 * onSelect: (id) => void }. Without a provider every element behaves as usual.
 */
export const InspectContext = createContext(null);

/**
 * Wraps a configurable element so a click selects it instead of acting. A hidden element is shown dimmed with a tag
 * @param {Object} props node (the element of the structure) and its rendered children
 * @returns {React.ReactElement} The element, wrapped when it is configurable
 */
export function InspectNode({node, children}) {
  const inspect = useContext(InspectContext);
  const {t} = useTranslation();
  const state = useComponentState(node.id);
  if (!inspect?.configurable.has(node.id)) {
    return children;
  }
  const hidden = state?.attributes?.visible === false;
  const select = event => {
    // Capture runs outermost first: a node lets the click through to the innermost node under it (a grid in a window)
    if (event.target.closest(".awe-inspect-node") !== event.currentTarget) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    inspect.onSelect(node.id);
  };
  return (
    <div className={classNames("awe-inspect-node", {"awe-inspect-selected": inspect.selected === node.id, "awe-inspect-hidden": hidden,
      "awe-inspect-personalized": inspect.personalized.has(node.id)})} onClickCapture={select}
      style={hidden ? {"--awe-inspect-hidden-text": JSON.stringify(t("SCR_PER_HIDDEN"))} : undefined}>
      {children}
      {hidden && <span className="awe-inspect-tag" title={state.attributes.label || node.id}>{t("SCR_PER_HIDDEN")}</span>}
    </div>
  );
}

/** Wrapper installed in the structure builder while the editor is open */
export const inspectWrapper = (node, element) => <InspectNode key={element.key} node={node}>{element}</InspectNode>;
