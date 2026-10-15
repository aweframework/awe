import React from "react";
import {Skeleton} from "primereact/skeleton";
import {TestIds, testHook} from "../utilities/testIds";
import "./AweScreenSkeleton.less";

const CRITERIA = 4;

/**
 * Generic screen placeholder shown while a screen is loading: a title bar, a row of criteria and a grid
 * @category Components
 * @returns {JSX.Element} Screen skeleton
 */
function AweScreenSkeleton() {
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
