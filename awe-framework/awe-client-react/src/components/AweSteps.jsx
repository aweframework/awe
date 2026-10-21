import React from "react";
import { Steps } from "primereact/steps";
import { Components } from "../utilities/structure";
import './AweSteps.less';
import { getIconCode, translateLabel } from "../utilities";
import { useTranslation } from "react-i18next";
import { useInvalidValues, usePanelable } from "../hooks/usePanelable";
import { useSelector } from "react-redux";
import { selectActionsRunning } from "../redux/selectors/stateSelectors";
import { useComponentState } from "../hooks/useComponentState";
import PropTypes from "prop-types";
import { TestAttributes, TestIds, testHook } from "../utilities/testIds";
import { useContextMenu } from "../hooks/useContextMenu";

/**
 * Steps disabled while actions run. It is the only part of the wizard that reads the running flag, so a running/idle
 * transition does not render the wizard panel again
 */
function AweStepList({ values, activeIndex, disabled, onChange, itemRenderer }) {
  const running = useSelector(selectActionsRunning);
  return <Steps model={values.map((item, index) => ({
    ...item,
    disabled: running || disabled || index > activeIndex,
    template: (item) => itemRenderer(item, index)
  }))}
    activeIndex={activeIndex}
    onSelect={onChange}
    readOnly={false}
  />;
}

AweStepList.propTypes = {
  values: PropTypes.array,
  activeIndex: PropTypes.number,
  disabled: PropTypes.bool,
  onChange: PropTypes.func,
  itemRenderer: PropTypes.func
};

function AweSteps(props) {
  const { id, elementList = [] } = props;
  const { model = { values: [] }, address, attributes } = useComponentState(id);
  const { t } = useTranslation();
  const { values, activeIndex, selectIndex } = usePanelable(model, address);
  const invalid = useInvalidValues(id);
  const { onContextMenu, contextMenuTemplate } = useContextMenu({ address, contextMenu: attributes.contextMenu });

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
    const isInvalid = invalid.includes(item.value);

    if (activeIndex === itemIndex) {
      stepClass = "p-step-current";
    } else if (activeIndex > itemIndex) {
      stepClass = "p-step-completed";
      icon = "pi:check";
    }

    return (
      <button className={`p-menuitem-link ${stepClass}${isInvalid ? " p-step-invalid" : ""}`} onClick={() => onChange({ index: itemIndex })} tabIndex={-1}
        {...testHook(TestIds.wizardStep, {
          active: activeIndex === itemIndex, completed: activeIndex > itemIndex, invalid: isInvalid, attributes: { "option-id": item.value, [TestAttributes.stepNumber]: String(itemIndex + 1) }
        })}>
        <span className="p-steps-number" {...testHook(TestIds.wizardStepNumber)}>{iconRenderer(icon, itemIndex + 1)}</span>
        <div className={`p-steps-text`}>
          {titleRenderer(item.title)}
          {labelRenderer(item.label)}
        </div>
      </button>
    );
  };

  const { disabled, orientation = "horizontal" } = attributes;
  const expandible = orientation === "horizontal" ? "vertical" : "horizontal";
  return <div className={`p-steps-container expand expandible-${expandible} orientation-${orientation}`} criterion-id={id} onContextMenu={onContextMenu}>
    <AweStepList values={values} activeIndex={activeIndex} disabled={disabled} onChange={onChange}
      itemRenderer={itemRenderer} />
    {elementList
      .filter(item => item.elementType === "WizardPanel")
      .filter((item, index) => index === activeIndex).map((node, index) => Components(node, index))}
    {contextMenuTemplate()}
  </div>;
}

AweSteps.propTypes = {
  id: PropTypes.string,
  elementList: PropTypes.array
};

export default AweSteps;
