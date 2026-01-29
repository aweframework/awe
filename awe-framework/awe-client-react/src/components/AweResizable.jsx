import React from "react";
import {Splitter, SplitterPanel} from "primereact/splitter";
import SizeRegistry from "../redux/registry/SizeRegistry";
import {Components} from "../utilities/structure";

function Resizable(props) {

  const {elementList = [], directions, style} = props;
  const resize = (e) => {
    SizeRegistry.setSize({width: window.innerWidth, height: window.innerHeight, current: e.sizes[0]});
  };

  const getLayout = (directions) => {
    return ["top", "bottom"].includes(directions) ? "vertical" : "horizontal";
  };

  return <Splitter layout={getLayout(directions)} className={`expand ${style}`}
    onResizeEnd={resize}>
    {elementList.map((node, index) => <SplitterPanel key={`splitter-${index}`}
      className="expand expandible-vertical scrollable">{Components(node, index)}</SplitterPanel>)}
  </Splitter>;
}

export default Resizable;
