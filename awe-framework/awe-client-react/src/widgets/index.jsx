import React from "react";
import AweFileManager from "./AweFileManager";
import AweLogViewer from "./AweLogViewer";
import AweHelpViewer from "./AweHelpViewer";
import AwePdfViewer from "./AwePdfViewer";
import AweCarousel from "./AweCarousel";

const Widget = {
  "file-manager": AweFileManager,
  "log-viewer": AweLogViewer,
  "help-viewer": AweHelpViewer,
  "pdf-viewer": AwePdfViewer,
  "carousel": AweCarousel
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
