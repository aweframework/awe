import React from "react";
import FullTemplate from "./FullTemplate";
import WindowTemplate from "./WindowTemplate";
import DocumentTemplate from "./DocumentTemplate";

const Templates = {
  "full": FullTemplate,
  "window": WindowTemplate,
  "document": DocumentTemplate
};

export const registerTemplate = (name, component) => {
  if (Templates[name]) {
    throw new Error(`Template with name "${name}" already exists.`);
  }
  Templates[name] = component;
};

export const getTemplate = (name) => Templates[name];


export default (node) => {
  if (node.screenTemplate in Templates) {
    return React.createElement(Templates[node.screenTemplate], {
      ...node
    });
  }
  return React.createElement(
    () => <div>The template {node.screenTemplate} has not been created yet.</div>
  );
};
