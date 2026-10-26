import React, {useCallback, useEffect, useLayoutEffect, useRef, useState} from "react";
import {useDispatch} from "react-redux";
import PropTypes from "prop-types";
import {useComponentState} from "../hooks/useComponentState";
import {fetchFileContentAction} from "../redux/thunks/files";
import {TestIds, testHook} from "../utilities/testIds";

/** Time after which a pending read is given up */
const HUNG_READ_MS = 30000;

/**
 * AWE File Viewer widget: shows the text of a file read from a server action
 * @category Widgets
 */
function AweFileViewer(props) {

  const {id} = props;
  const {attributes = {}} = useComponentState(id);
  const {autoload, autorefresh, serverAction, targetAction, style, visible = true} = attributes;
  const parameters = props.parameters || attributes.parameters || {};
  const stickBottom = parameters.stickBottom === true || parameters.stickBottom === "true";
  const dispatch = useDispatch();
  const [content, setContent] = useState("");
  const container = useRef(null);
  // A user at the bottom (or the first content) follows the new content when stickBottom is on
  const follow = useRef(true);

  // Like AngularJS, which refreshes after each answer: one read at a time, and no answer applied once the widget is gone.
  // A read that hangs for longer than HUNG_READ_MS is given up: a new one starts and the late answer is ignored
  const pending = useRef(null);
  const mounted = useRef(false);
  const shown = useRef("");
  useEffect(() => {
    // Set on every mount: React StrictMode mounts, unmounts and mounts again in development
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const update = useCallback(text => {
    // An unchanged text changes nothing, not even whether the next content is followed
    if (!mounted.current || text === shown.current) {
      return;
    }
    shown.current = text;
    const element = container.current;
    follow.current = follow.current || (element && element.scrollTop + element.clientHeight >= element.scrollHeight);
    setContent(text);
  }, []);

  const load = useCallback(() => {
    if (pending.current && Date.now() - pending.current.since < HUNG_READ_MS) {
      return;
    }
    const read = { since: Date.now() };
    pending.current = read;
    const current = () => pending.current === read;
    // The thunk handles its own errors: the promise only frees the read, nothing waits for it
    void Promise.resolve(dispatch(fetchFileContentAction(serverAction, targetAction, text => current() && update(text))))
      .finally(() => {
        if (current()) {
          pending.current = null;
        }
      });
  }, [dispatch, serverAction, targetAction, update]);

  // A widget shown again has a new container at the top: like a first load, it goes to the bottom
  useLayoutEffect(() => {
    if (visible) {
      follow.current = true;
    }
  }, [visible]);

  useLayoutEffect(() => {
    if (stickBottom && follow.current && container.current) {
      container.current.scrollTop = container.current.scrollHeight;
      follow.current = false;
    }
  }, [content, stickBottom, visible]);

  useEffect(() => {
    if (visible && autoload) {
      load();
    }
  }, [visible, autoload, load]);

  useEffect(() => {
    if (visible && autorefresh > 0) {
      const interval = setInterval(load, autorefresh * 1000);
      return () => clearInterval(interval);
    }
  }, [visible, autorefresh, load]);

  return visible ? <div className={style} id={id} ref={container}>
    <pre {...testHook(TestIds.fileViewer)}>{content}</pre>
  </div> : <></>;
}

AweFileViewer.propTypes = {
  id: PropTypes.string,
  parameters: PropTypes.object
};

export default AweFileViewer;
