import React from "react";
import { Steps } from "primereact/steps";
import { Components } from "../utilities/structure";
import './AweSteps.less';
import { getIconCode, translateLabel } from "../utilities";
import { useTranslation } from "react-i18next";
import { usePanelable } from "../hooks/usePanelable";
import { useSelector } from "react-redux";
import { useComponentState } from "../hooks/useComponentState";
import PropTypes from "prop-types";

function AweSteps(props) {
  const { id, elementList = [] } = props;
  const { model = { values: [] }, address, attributes } = useComponentState(id);
  const globalDisabled = useSelector(state => state.actions.running);
  const { t } = useTranslation();
  const { values, activeIndex, selectIndex } = usePanelable(model, address);

  const titleRenderer = (title) => title ? <span className={`p-steps-supertitle`}>{translateLabel(title, t)}</span> : null;
  const labelRenderer = (label) => label ? <span className={`p-steps-title`}>{translateLabel(label, t)}</span> : null;
  const iconRenderer = (icon, number) => icon ? getIconCode(icon) : number;

  const onChange = (e) => {
    const { index } = e;
    selectIndex(index);
  };

  const itemRenderer = (item, itemIndex) => {
    let icon = item.icon;
    let stepClass = "p-step-pending";

    if (activeIndex === itemIndex) {
      stepClass = "p-step-current";
    } else if (activeIndex > itemIndex) {
      stepClass = "p-step-completed";
      icon = "pi:check";
    }

    return (
      <button className={`p-menuitem-link ${stepClass}`} onClick={() => onChange({ index: itemIndex })} tabIndex={-1}>
        <span className="p-steps-number">{iconRenderer(icon, itemIndex + 1)}</span>
        <div className={`p-steps-text`}>
          {titleRenderer(item.title)}
          {labelRenderer(item.label)}
        </div>
      </button>
    );
  };

  const { disabled, orientation = "horizontal" } = attributes;
  const expandible = orientation === "horizontal" ? "vertical" : "horizontal";
  return <div className={`p-steps-container expand expandible-${expandible} orientation-${orientation}`}>
    <Steps model={values.map((item, index) => ({
      ...item,
      disabled: globalDisabled || disabled || index > activeIndex,
      template: (item) => itemRenderer(item, index)
    }))}
      activeIndex={activeIndex}
      onSelect={onChange}
      readOnly={false}
    />
    {elementList
      .filter(item => item.elementType === "WizardPanel")
      .filter((item, index) => index === activeIndex).map((node, index) => Components(node, index))}
  </div>;
}

AweSteps.propTypes = {
  id: PropTypes.string,
  elementList: PropTypes.array
};

export default AweSteps;
