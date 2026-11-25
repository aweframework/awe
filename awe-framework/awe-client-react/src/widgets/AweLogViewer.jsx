import React, {Suspense, useCallback, useEffect, useId, useRef, useState} from "react";
import {createPortal} from "react-dom";
import {Skeleton} from "primereact/skeleton";
import {LazyLog} from "../utilities/lazyPackages";
import {getIconCode} from "../utilities";
import {isEmpty} from "../utilities/general";
import {InputSwitch} from "primereact/inputswitch";
import "./AweLogViewer.less";
import {useDispatch} from "react-redux";
import {useComponentState} from "../hooks/useComponentState";
import {updateAttributes} from "../redux/actions/components";
import {fetchLogAction} from "../redux/thunks/files";
import PropTypes from "prop-types";
import {TestIds, testHook} from "../utilities/testIds";
import {useTranslation} from "react-i18next";

/**
 * AWE Log Viewer component
 * @category Widgets
 */
function AweLogViewer(props) {

  const { id } = props;
  const { address = {}, attributes = {} } = useComponentState(id);
  const { autorefresh, loading, serverAction, targetAction, visible = true } = attributes;
  const dispatch = useDispatch();
  const [offset, setOffset] = useState(1);
  const [logText, setLogText] = useState("");
  const [showLoadingDots, setShowLoadingDots] = useState(true);
  const lastAutorefresh = useRef(autorefresh);
  // Line to read from, kept out of the render so a read always continues where the last one ended
  const nextOffset = useRef(1);
  // One read at a time: the reads of the autorefresh, of its start and of a filter never append the same lines twice
  const reading = useRef(false);
  const containerRef = useRef(null);
  const viewerAddress = useRef(address);
  viewerAddress.current = address;
  // Toolbar of react-lazylog: the follow switch goes at its right end
  const [toolbar, setToolbar] = useState(null);
  const followId = useId();
  const { t } = useTranslation();
  const filterLabel = t("LOG_VIEWER_FILTER_MATCHES");
  const searchLabel = t("LOG_VIEWER_SEARCH");

  // react-lazylog draws its filter toggle without a name and its search field with an English placeholder: give them a
  // tooltip and an accessible name in the language of the user once they are rendered (a missing text keeps the
  // placeholder of the library), and find its toolbar for the follow switch
  useEffect(() => {
    const container = containerRef.current;
    if (!container) {
      return undefined;
    }
    const label = () => {
      container.querySelectorAll(".react-lazylog-searchbar-filter").forEach(button => {
        button.setAttribute("title", filterLabel);
        button.setAttribute("aria-label", filterLabel);
      });
      if (searchLabel !== "LOG_VIEWER_SEARCH") {
        container.querySelectorAll(".react-lazylog-searchbar-input").forEach(input => {
          input.setAttribute("placeholder", searchLabel);
          input.setAttribute("aria-label", searchLabel);
        });
      }
      setToolbar(container.querySelector(".react-lazylog-searchbar"));
    };
    label();
    const observer = new MutationObserver(label);
    observer.observe(container, { childList: true, subtree: true });
    return () => observer.disconnect();
  }, [filterLabel, searchLabel, visible]);

  /**
   * Move the line to read from
   * @param {function} update From the previous line to the next one
   */
  const updateOffset = useCallback(update => {
    nextOffset.current = update(nextOffset.current);
    setOffset(nextOffset.current);
  }, []);

  /**
   * Read the next lines of the log, unless a read is still running
   */
  const readLog = useCallback(() => {
    if (reading.current) {
      return;
    }
    reading.current = true;
    // The other actions of the answer run on the viewer (the server turns its autorefresh off once the execution ended)
    void Promise.resolve(dispatch(fetchLogAction(serverAction, targetAction, nextOffset.current, setLogText, updateOffset,
      viewerAddress.current)))
      .finally(() => {
        reading.current = false;
      });
  }, [serverAction, targetAction]);

  // Autorefresh: the log is read at once when it is enabled (or its target changes), then once per period
  useEffect(() => {
    if (!autorefresh) {
      setShowLoadingDots(false);
      return undefined;
    }
    lastAutorefresh.current = autorefresh;
    setShowLoadingDots(true);
    readLog();
    const interval = setInterval(readLog, autorefresh * 1000);
    return () => clearInterval(interval);
  }, [autorefresh, readLog]);

  // A filter of the viewer (the detail of an ended execution) reads the log once, as AngularJS does
  useEffect(() => {
    if (loading) {
      readLog();
      dispatch(updateAttributes(address, { loading: false }));
    }
  }, [loading]);

  /**
   * Turn on/off autorefresh on log viewer
   */
  const toggleAutoRefresh = () => {
    dispatch(updateAttributes(address, { autorefresh: showLoadingDots ? 0 : lastAutorefresh.current }));
  };

  // react-lazylog keeps the text after the last line break as a typed array and concatenates it into
  // its line list, which then holds the bytes as numbers and crashes the whole screen when rendered.
  // Closing the text with a line break leaves nothing after the last line break.
  // A missing text is an empty one. A blank text is kept as a single blank line: an empty text made the screen
  // crash in the real application once the first lines arrived, a blank line does not.
  const safeText = isEmpty(logText) ? " " : logText;
  const lazyLogText = safeText.endsWith("\n") ? safeText : safeText + "\n";

  // Follow the end of the log (autorefresh): a labelled switch at the right end of the toolbar
  const followSwitch = <div className="log-follow">
    <InputSwitch inputId={followId} className="awe-switch" checked={showLoadingDots} onChange={toggleAutoRefresh} />
    <label htmlFor={followId}>{t("LOG_VIEWER_FOLLOW")}</label>
  </div>;

  return visible ? <div ref={containerRef} className={"expand expandible-vertical panel-body p-0 log-container"} id={id} {...testHook(TestIds.logViewer)}>
    {toolbar && createPortal(followSwitch, toolbar)}
    <Suspense fallback={<Skeleton width="100%" height="100%" />}>
      <LazyLog text={lazyLogText} scrollToLine={Number.isFinite(offset) ? offset : 1} enableSearch caseInsensitive selectableLines extraLines={1} />
    </Suspense>
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
