import React from "react";
import AweFileManager from "./AweFileManager";
import AweLogViewer from "./AweLogViewer";
import AweHelpViewer from "./AweHelpViewer";
import AwePdfViewer from "./AwePdfViewer";

const Widget = {
  "file-manager": AweFileManager,
  "log-viewer": AweLogViewer,
  "help-viewer": AweHelpViewer,
  "pdf-viewer": AwePdfViewer
};

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
