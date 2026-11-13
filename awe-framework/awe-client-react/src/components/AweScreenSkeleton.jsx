import React, {useLayoutEffect, useRef, useState, useSyncExternalStore} from "react";
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

// Time the splash takes to fill the logo up and fade out (css/splash.css)
const SPLASH_OUTRO_MS = 400;

/**
 * Fill the logo of the splash up and fade it out over the first screen: a copy of the splash, out of the React tree,
 * plays it once React has removed the splash
 * @param {HTMLElement} splash Splash element
 */
function playSplashOutro(splash) {
  const outro = splash.cloneNode(true);
  // Resume the fill where the splash was
  outro.style.setProperty("--awe-splash-elapsed", String(Math.round(performance.now())));
  queueMicrotask(() => {
    // Still in the page: the effect was only re-run (strict mode), the splash has not left
    if (splash.isConnected) {
      return;
    }
    delete outro.dataset.testid;
    outro.removeAttribute("role");
    outro.removeAttribute("aria-busy");
    outro.setAttribute("aria-hidden", "true");
    document.body.appendChild(outro);
    // Commit the current state, so the fill and the fade transition from it
    outro.getBoundingClientRect();
    outro.classList.add("splash-done", "splash-leaving");
    setTimeout(() => outro.remove(), SPLASH_OUTRO_MS);
  });
}

/**
 * Generic screen placeholder shown while a screen is loading: a title bar, a row of criteria and a grid. The first
 * screen of the page shows the splash animation instead
 * @category Components
 * @returns {JSX.Element} Screen skeleton
 */
function AweScreenSkeleton() {
  const splash = useSyncExternalStore(subscribe, isInitialLoad, isInitialLoad);
  // The splash of the page template is replaced when React renders: resume its fill where it was
  const [elapsed] = useState(() => Math.round(performance.now()));
  const splashRef = useRef(null);
  // When the splash leaves (the first screen is shown, or it gives way to the skeleton) the logo fills up and fades out
  useLayoutEffect(() => {
    const node = splashRef.current;
    return node ? () => playSplashOutro(node) : undefined;
  }, [splash]);
  if (splash) {
    // Same markup as the splash of the page template (styled by css/splash.css)
    return <div id="splash" ref={splashRef} className="screen-splash" role="status" aria-busy="true"
                style={{"--awe-splash-elapsed": elapsed}} {...testHook(TestIds.loadingSpinner)}>
      <div id="splashBackground"></div>
      <div className="splash-logo" aria-hidden="true">
        <div className="splash-fill"><div className="splash-level"><div className="splash-cover"></div></div></div>
      </div>
      <span className="splash-label">Loading</span>
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
