import React, {useCallback} from "react";
import {useDispatch} from "react-redux";
import {useComponentState} from "../hooks/useComponentState";
import {useTranslation} from "react-i18next";
import {getIconCode, getVisibleTextData, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import {updateModelWithDependencies as updateThunk} from "../redux/thunks/components";
import {addActionsTop} from "../redux/actions/actions";
import AweCriterion from "./AweCriterion";
import useComponent from "../hooks/useComponent";
import PropTypes from "prop-types";

function AweInputTextView(props) {
  const { id } = props;
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const { address } = useComponent(id);
  const { model = { values: [] }, attributes = {}, validationRules = {}, actions = [] } = useComponentState(id);

  const onAction = useCallback((event) => {
    dispatch(updateThunk(address, { event }));
    if (actions.length > 0) {
      dispatch(addActionsTop(actions.map(action => ({ ...action, address: { ...address } }))));
    }
  }, [dispatch, address, actions]);

  const onClick = useCallback(() => onAction("click"), [onAction]);
  const onKeyDown = useCallback(() => onAction("keyDown"), [onAction]);

  const { placeholder, size, icon, unit } = attributes;
  const { values = [] } = model;
  const textToShow = (values[0] || {}).label || (values[0] || {}).value || placeholder;
  const classes = classNames("text-view", { [`text-${size}`]: size });
  const unitNode = unit ? <span className="p-tag ml-auto">{translateLabel(unit, t)}</span> : null;
  const iconNode = icon ? getIconCode(icon, classNames("fa-fw", { [`text-${size}`]: size })) : null;

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules}
      groupClass="flex p-2" generateIcon={false} generateUnit={false}>
      {iconNode}
      <button className={classes} tabIndex={0} onClick={onClick} onKeyDown={onKeyDown}>{getVisibleTextData(textToShow, t)}</button>
      {unitNode}
    </AweCriterion>
  );
}

AweInputTextView.propTypes = {
  id: PropTypes.string,
};

export default AweInputTextView;
