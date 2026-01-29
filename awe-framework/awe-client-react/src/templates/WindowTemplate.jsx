import React from "react";
import {getSource, getSourceChildren} from "../utilities/structure";
import {BreadCrumb} from "primereact/breadcrumb";

import "./WindowTemplate.css";
import { useMenuBreadcrumbs } from "../hooks/useMenuRegistry";

function WindowTemplate(props) {

  const { elementList = [] } = props;
  const breadcrumbs = useMenuBreadcrumbs();
  const home = {icon: 'pi pi-home'};
  const sourceCenter = getSource("center", elementList);
  const sourceModal = getSource("modal", elementList);
  const sourceHidden = getSource("hidden", elementList);
  const sourceButtons = getSource("buttons", elementList);

  return (
    <div className={"expand expandible-vertical animate__animated animate__fadeIn"} style={{position: "relative"}}>
      <div className="breadcrumb-buttons p-breadcrumb-container">
        <BreadCrumb model={breadcrumbs.items} home={home}/>
        <div className={`window-buttons ${sourceButtons?.style ?? ""}`}>{getSourceChildren(sourceButtons)}</div>
      </div>
      <div className={`expand expandible-vertical ${sourceCenter?.style ?? ""}`}>{getSourceChildren(sourceCenter)}</div>
      <div style={{position: 'absolute'}} className={sourceModal?.style ?? ""}>{getSourceChildren(sourceModal)}</div>
      <div style={{display: 'none'}}>{getSourceChildren(sourceHidden)}</div>
    </div>
  );
}

// Connect redux store updates
export default WindowTemplate;
