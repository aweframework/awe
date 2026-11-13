import {lazy} from "react";

/**
 * Heavy third-party components, loaded on demand: the package is fetched the first time the component renders, so a
 * component that uses one needs a Suspense with its own fallback. The library build keeps these imports as they are
 * (see webpack.common.js) and the application splits each package into an async chunk.
 */
export const ReactPlayer = lazy(() => import("react-player"));
export const PDFObject = lazy(() => import("react-pdfobject").then(({PDFObject: component}) => ({default: component})));
export const LazyLog = lazy(() => import("react-lazylog").then(({LazyLog: component}) => ({default: component})));
export const Editor = lazy(() => import("primereact/editor").then(({Editor: component}) => ({default: component})));
