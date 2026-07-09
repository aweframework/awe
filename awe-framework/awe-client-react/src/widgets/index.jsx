import React from "react";
import AweFileManager from "./AweFileManager";
import AweLogViewer from "./AweLogViewer";
import AweHelpViewer from "./AweHelpViewer";
import AwePdfViewer from "./AwePdfViewer";
import AweCarousel from "./AweCarousel";

const Widget = {};

/**
 * Register a widget component for a screen widget type
 * @param {string} type Widget type (matches the `type` attribute of the `widget` tag)
 * @param {React.ComponentType} component Component to render for that type
 */
export const registerWidget = (type, component) => {
  if (Widget[type]) {
    throw new Error(`Widget with type "${type}" already exists.`);
  }
  Widget[type] = component;
};

/**
 * Retrieve a registered widget component
 * @param {string} type Widget type
 * @returns {React.ComponentType|undefined} Registered component or undefined
 */
export const getWidget = (type) => Widget[type];

// Built-in widgets register through the same extension mechanism
registerWidget("file-manager", AweFileManager);
registerWidget("log-viewer", AweLogViewer);
registerWidget("help-viewer", AweHelpViewer);
registerWidget("pdf-viewer", AwePdfViewer);
registerWidget("carousel", AweCarousel);

export default (node, index) => {
  if (node.type in Widget) {
    return React.createElement(Widget[node.type], {
      ...node, key: node.id || `widget-${index}`
    });
  }
  return React.createElement(
    () => <div>The widget {node.type} has not been created yet.</div>,
    {key: index}
  );
};
