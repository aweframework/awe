import React from "react";
import PropTypes from "prop-types";
import {ProgressSpinner} from "primereact/progressspinner";
import "./AweLoader.less";

const SPINNER = "spinner";
const withCubeClass = (items) => items.map(item => `sk-cube ${item}`);

/**
 * Loading icons of the icon-loading screen attribute, the same ones the AngularJS engine offers.
 * The element tree of every icon is styled by AweLoader.less
 */
const CSS_LOADERS = {
  carpet: withCubeClass(["sk-cube1", "sk-cube2", "sk-cube3", "sk-cube4", "sk-cube5", "sk-cube6", "sk-cube7", "sk-cube8", "sk-cube9"]),
  circlebar: ["bounce1", "bounce2", "bounce3"],
  circles: ["double-bounce1", "double-bounce2"],
  cubes: ["cube1", "cube2"],
  dots: ["dot1", "dot2"],
  folding: withCubeClass(["sk-cube1", "sk-cube2", "sk-cube4", "sk-cube3"]),
  squarebar: ["rect1", "rect2", "rect3", "rect4", "rect5"],
  square: [],
  custom: []
};

export const LOADER_ICONS = [SPINNER, "none", "icon", ...Object.keys(CSS_LOADERS)];

/**
 * Component loading icon
 * @category Components
 * @param {string} [icon] Loading icon name (icon-loading attribute). Unknown or empty names show the spinner
 * @param {object} [rootProps] Attributes added to the root element (test hooks)
 * @returns {JSX.Element} Loading icon
 */
function AweLoader({icon, rootProps = {}}) {
  if (!LOADER_ICONS.includes(icon) || icon === SPINNER) {
    return <ProgressSpinner pt={{root: rootProps}} />;
  }

  let content = null;
  if (icon === "icon") {
    content = <i className="pi pi-spinner pi-spin" />;
  } else if (icon in CSS_LOADERS) {
    content = <div className={`${icon}-loader`}>
      {CSS_LOADERS[icon].map(item => <div key={item} className={item} />)}
    </div>;
  }
  return <div className="awe-loader" {...rootProps}>{content}</div>;
}

AweLoader.propTypes = {
  icon: PropTypes.string,
  rootProps: PropTypes.object
};

export default AweLoader;
