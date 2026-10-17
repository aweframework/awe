import React, {useSyncExternalStore} from "react";
import {Skeleton} from "primereact/skeleton";
import {TestIds, testHook} from "../utilities/testIds";
import "./AweSkeleton.less";
import "./AweScreenSkeleton.less";

const CRITERIA = 4;

// The first screen of the page session loads under the splash animation of the page, as the page itself
let initialLoad = true;
const listeners = new Set();
const subscribe = listener => {
  listeners.add(listener);
  return () => listeners.delete(listener);
};
const isInitialLoad = () => initialLoad;

/**
 * End the initial load of the page: the screens loaded from now on show the screen skeleton
 */
export function endInitialLoad() {
  if (initialLoad) {
    initialLoad = false;
    listeners.forEach(listener => listener());
  }
}

/**
 * Generic screen placeholder shown while a screen is loading: a title bar, a row of criteria and a grid. The first
 * screen of the page shows the splash animation instead
 * @category Components
 * @returns {JSX.Element} Screen skeleton
 */
function AweScreenSkeleton() {
  const splash = useSyncExternalStore(subscribe, isInitialLoad, isInitialLoad);
  if (splash) {
    // Same markup as the splash of the page template (styled by css/splash.css)
    return <div id="splash" className="screen-splash" aria-busy="true" {...testHook(TestIds.loadingSpinner)}>
      <div id="splashBackground"></div>
      <div className="spinner"><div className="dot1"></div><div className="dot2"></div></div>
    </div>;
  }
  return <div className="expand screen-skeleton animate__animated animate__fadeIn" aria-busy="true"
              {...testHook(TestIds.loadingSpinner)}>
    <div className="screen-skeleton-contents" aria-hidden="true">
      <div className="screen-skeleton-title"><Skeleton width="16rem" height="1.75rem" /></div>
      <div className="screen-skeleton-criteria">
        {Array.from({length: CRITERIA}, (_, index) => <Skeleton key={index} height="2.25rem" />)}
      </div>
      <div className="screen-skeleton-grid"><Skeleton height="100%" /></div>
    </div>
  </div>;
}

export default AweScreenSkeleton;
