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

const {BUTTON_RESET, BUTTON_NORMAL} = ButtonTypes;

function ColumnButton(props) {
  const {data, attrs, address, style, disabled, icon, actions, buttonType = BUTTON_NORMAL} = props;
  const {style: cellStyle, label, icon: cellIcon} = data;
  const {visible = true, readonly: cellReadonly} = attrs;
  const classes = classNames('p-button-sm', 'p-1', style, cellStyle, {"hidden": !visible});
  const dispatch = useDispatch();
  const { t} = useTranslation();

  const onClick = useCallback(() => {
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
    id={address.component}
    type="button"
    className={classes}
    icon={getIconCode(getFirstDefinedValue(cellIcon, icon), "p-button-icon p-c")}
    disabled={getFirstDefinedValue(cellReadonly, disabled, false)}
    label={label ? translateLabel(label, t) : null}
    iconPos={"left"}
    onClick={onClick}
  />;
}

export default ColumnButton;