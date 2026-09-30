import React, { useCallback } from "react";
import { getIconCode, translateLabel } from "../utilities";
import { classNames } from "../utilities/components";
import { PickList } from "primereact/picklist";
import "./AwePicklist.less";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { useComponentState } from "../hooks/useComponentState";
import { updateModelWithDependencies } from "../redux/thunks/components";
import { Skeleton } from "primereact/skeleton";
import PropTypes from "prop-types";

function AwePicklist(props) {

  const { id, style: propsStyle } = props;
  const { t } = useTranslation();
  const { address, attributes = {}, model = { values: [] } } = useComponentState(id);
  const dispatch = useDispatch();

  const onChange = useCallback((e) => {
    dispatch(updateModelWithDependencies(address, {
      values: [
        ...e.source.map(v => ({ ...v, selected: false })),
        ...e.target.map(v => ({ ...v, selected: true }))
      ]
    }));
  }, [address]);

  const itemTemplate = (item) => {
    const { t } = props;
    return (
      <div className="flex flex-wrap p-2 align-items-center gap-3">
        {item.image && <img className="picklist-image" src={item.image} alt={translateLabel(item.label, t)} />}
        <div className="flex-1 flex flex-column gap-2">
          <span className="picklist-title">{translateLabel(item.label, t)}</span>
          <div className="flex align-items-center gap-2">
            {item.icon && getIconCode(item.icon, "p-button-icon p-c p-button-icon-left")}
            {item.description && <span className="picklist-description">{translateLabel(item.description, t)}</span>}
          </div>
        </div>
        {item.unit && <span className="picklist-unit">{item.unit}</span>}
      </div>
    );
  };


  const { label, style, visible = true } = attributes;
  const classes = classNames(style, { "hidden": !visible });

  // If address is undefined, return skeleton
  if (!address) {
    return <Skeleton width="100%" height="8rem" style={propsStyle} />;
  }

  // Paint component
  return <div className={classes} criterion-id={id}>
    <PickList
      dataKey="value"
      source={model.values.filter(v => !v.selected)}
      target={model.values.filter(v => v.selected)}
      onChange={onChange}
      showSourceControls={false}
      showTargetControls={false}
      sourceHeader={translateLabel(label, t)}
      targetHeader={translateLabel("SCREEN_TEXT_SELECTED", t)}
      itemTemplate={itemTemplate}
    />
  </div>;
}

AwePicklist.propTypes = {
  id: PropTypes.string,
  style: PropTypes.string
};

export default AwePicklist;
