import React from "react";
import {Panel} from 'primereact/panel';
import {useTranslation} from 'react-i18next';
import "./AweWindow.less";
import {getHelpTooltipNode, getIconCode, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import {Components} from "../utilities/structure";
import PropTypes from "prop-types";

function AweWindow(props) {
  const {id, style, help, helpImage, label, icon, elementList = []} = props;
  const { t } = useTranslation();

  const getIcon = () => {
    if (icon) {
      return getIconCode(icon, "fa-fw");
    }
    return null;
  };

  const getHelpIcon = () => {
    if (help || helpImage) {
      return <i role="note" className={`help-icon pi pi-question-circle`} />;
    } else {
      return null;
    }
  };

  const getHeader = () => {
    return <div className={`help-target-${id}`}>{translateLabel(label, t)} {getHelpIcon()}</div>;
  };

  const classes = classNames("m-2", `expandible-vertical`, style);

  return <>
    {getHelpTooltipNode(help, helpImage, t,`.help-target-${id}`)}
    <Panel
      id={id}
      className={classes}
      header={getHeader()}
      icons={getIcon()}>
      {elementList.map((node, index) => Components(node, index))}
    </Panel>
  </>;
}

AweWindow.propTypes = {
  id: PropTypes.string,
  style: PropTypes.string,
  label: PropTypes.string,
  icon: PropTypes.string,
  help: PropTypes.string,
  helpImage: PropTypes.string,
  elementList: PropTypes.array
};

export default AweWindow;
