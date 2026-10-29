import React, {useCallback} from "react";
import {useTranslation} from "react-i18next";
import {Button} from "primereact/button";
import {ButtonTypes} from "../redux/actions/components";
import {getIconCode, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import {useDispatch} from "react-redux";
import {updateModelWithDependencies} from "../redux/thunks/components";
import {addActionsTop} from "../redux/actions/actions";
import {getFirstDefinedValue} from "../utilities/general";
import PropTypes from "prop-types";
import {useContextMenuAnchor} from "../hooks/useContextMenu";
import {TestIds, testHook} from "../utilities/testIds";
import {getButtonVariant} from "../utilities/buttons";
import {selectButtonRow} from "../redux/thunks/grid";
import "../components/AweButton.less";

const {BUTTON_RESET, BUTTON_NORMAL} = ButtonTypes;

function ColumnButton(props) {
  const {data, attrs, address, style, disabled, icon, actions, buttonType = BUTTON_NORMAL, title: columnTitle} = props;
  const {style: cellStyle, label, icon: cellIcon, title: cellTitle} = data;
  const {visible = true, readonly: cellReadonly} = attrs;
  // A btn-* style of the column gives the look of its buttons (btn-ghost for the row actions)
  const classes = classNames('p-button-sm', 'p-1', getButtonVariant(classNames(style, cellStyle), ""), style, cellStyle, {"hidden": !visible});
  const dispatch = useDispatch();
  const { t} = useTranslation();
  const buttonRef = useContextMenuAnchor(address);
  // The title of the cell, else the one of the column, is the tooltip; a cell without label shows only its icon and the
  // tooltip is its accessible name too
  const tooltip = translateLabel(getFirstDefinedValue(cellTitle, columnTitle), t) || undefined;

  const onClick = useCallback(() => {
    // The button acts on its row: it is selected (only it) before the actions run, as their dependencies and the
    // parameters they send read the selected row
    dispatch(selectButtonRow(address));

    // Change click event
    dispatch(updateModelWithDependencies(address, {event: "click"}));

    // Send actions to action container
    if (actions.length > 0) {
      dispatch(addActionsTop(actions.map(action => ({...action, address: {...address}}))));
    } else if (BUTTON_RESET === buttonType) {
      dispatch(addActionsTop([{type: "restore", address: {...address}}]));
    }
  }, []);

  return <Button
    ref={buttonRef}
    id={address.component}
    type="button"
    className={classes}
    icon={getIconCode(getFirstDefinedValue(cellIcon, icon), "p-button-icon p-c")}
    disabled={getFirstDefinedValue(cellReadonly, disabled, false)}
    label={label ? translateLabel(label, t) : null}
    {...(tooltip ? {title: tooltip} : {})}
    {...(!label && tooltip ? {"aria-label": tooltip} : {})}
    iconPos={"left"}
    onClick={onClick}
    {...testHook(TestIds.button)}
  />;
}

ColumnButton.propTypes = {
  actions: PropTypes.array,
  address: PropTypes.object,
  attrs: PropTypes.object,
  buttonType: PropTypes.any,
  data: PropTypes.oneOfType([PropTypes.object, PropTypes.array]),
  disabled: PropTypes.bool,
  icon: PropTypes.any,
  style: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  title: PropTypes.string,
};

export default ColumnButton;