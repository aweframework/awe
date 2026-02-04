import React, {useCallback, useEffect, useRef, useState} from "react";
import {LazyLog} from 'react-lazylog';
import {getIconCode} from "../utilities";
import {Button} from "primereact/button";
import "./AweLogViewer.less";
import {useDispatch} from "react-redux";
import {useComponentState} from "../hooks/useComponentState";
import {updateAttributes} from "../redux/actions/components";
import {fetchLogAction} from "../redux/thunks/files";
import PropTypes from "prop-types";

/**
 * AWE Log Viewer component
 * @category Widgets
 */
function AweLogViewer(props) {

  const { id } = props;
  const { address = {}, attributes = {} } = useComponentState(id);
  const { autorefresh, serverAction, targetAction, visible = true } = attributes;
  const dispatch = useDispatch();
  const [offset, setOffset] = useState(1);
  const [logText, setLogText] = useState(" ");
  const [showLoadingDots, setShowLoadingDots] = useState(true);
  const loadingTimeout = useRef(null);
  const lastAutorefresh = useRef(autorefresh);

  /**
   * Check if autorefresh must be active or not
   */
  const checkAutoRefresh = useCallback(() => {
    if (autorefresh) {
      clearInterval(loadingTimeout.current);
      loadingTimeout.current = setInterval(() => dispatch(fetchLogAction(serverAction, targetAction, offset, setLogText, setOffset)), autorefresh * 1000);
      lastAutorefresh.current = autorefresh;
      setShowLoadingDots(true);
    } else {
      clearInterval(loadingTimeout.current);
      setShowLoadingDots(false);
    }
  }, [autorefresh, serverAction, targetAction, offset]);

  /**
   * Turn on/off autorefresh on log viewer
   */
  const toggleAutoRefresh = () => {
    dispatch(updateAttributes(address, { autorefresh: showLoadingDots ? 0 : lastAutorefresh.current }));
  };

  useEffect(() => {
    checkAutoRefresh();
  }, [autorefresh, offset]);

  return visible ? <div className={"expand expandible-vertical panel-body p-0 log-container"} id={id}>
    <Button data-testid="autoload-button"
      className={"p-button-text p-button-rounded log-button-autoload"}
      icon={getIconCode("refresh", showLoadingDots ? "fa-spin" : "")}
      onClick={toggleAutoRefresh} />
    <LazyLog text={logText} scrollToLine={Number.isFinite(offset) ? offset : 1} enableSearch caseInsensitive selectableLines extraLines={1} />
    <div className={"log-loading-dots " + (!showLoadingDots ? "hidden" : "")}>
      {getIconCode("circle", "fa-fw fade1 animation-dot")}
      {getIconCode("circle", "fa-fw fade2 animation-dot")}
      {getIconCode("circle", "fa-fw fade3 animation-dot")}
    </div>
  </div> : <></>;
}

AweLogViewer.propTypes = {
  id: PropTypes.string,
};

export default AweLogViewer;
