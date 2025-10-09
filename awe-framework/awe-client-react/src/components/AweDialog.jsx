import React, {useCallback} from "react";
import {Dialog} from 'primereact/dialog';
import {Components} from "../utilities/structure";
import {translateLabel} from "../utilities";
import {useTranslation} from "react-i18next";
import {useDispatch, useSelector} from "react-redux";
import {addActionsTop} from "../redux/actions/actions";
import PropTypes from "prop-types";

function AweDialog(props) {
  const { id, elementList = [] } = props;
  const { address, attributes } = useSelector(state => ({
    address: state.components[id]?.address,
    attributes: state.components[id]?.attributes
  }));
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { style, label } = attributes;

  const hide = useCallback(() => {
    if (attributes.isShowing) {
      dispatch(addActionsTop([{ address, type: "close" }]));
    }
  }, [dispatch, attributes?.isShowing, address]);

  return (
    <Dialog
      id={id}
      className={style}
      style={{ minWidth: "40vw", maxWidth: "90vw" }}
      header={translateLabel(label, t)}
      visible={attributes?.isShowing}
      onHide={hide}
      focusOnShow={false}
    >
      {elementList.map((node, index) => Components(node, index))}
    </Dialog>
  );
}

AweDialog.propTypes = {
  id: PropTypes.string,
  elementList: PropTypes.array
};

export default AweDialog;
