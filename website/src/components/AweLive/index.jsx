import React from 'react';
import BrowserOnly from '@docusaurus/BrowserOnly';
import './AweLive.css';

// Same height as the frame of a one-line example: the page does not jump when the real component arrives
const Skeleton = () => <div className="awe-live-loading" role="status" aria-busy="true" aria-live="polite" />;

/**
 * Renders real AWE React client components inside the docs, fed by a store preloaded from `components`:
 *
 *   <AweLive components={{save: {attributes: {label: 'Save', buttonType: 'submit'}}}}>
 *     {({AweButton}) => <AweButton id="save" />}
 *   </AweLive>
 *
 * Children are a function of the client components (clientComponents.js): they are loaded in the browser only.
 * Instead of children, `nodes` lists elements of the structure of a screen to render, as the client renders a screen
 * ({elementType: 'Card', id: 'kpi', elementList: [...]}): a container with its children, a criterion with its component.
 * Browser only for now: the client touches window/document (getContextPath, tooltips).
 *
 * Props: `components`, `view`, `settings` (see buildPreloadedState), `files` ({target action: url of a static file}: the file and
 * PDF viewers read it instead of asking a server, see simulateFiles.js), `options` ({target action: [{value, label}]}: the
 * suggests are answered from it while the user types, see optionFixtures.js), `uploads` (true: an uploader never sends its
 * file, see simulateUploads.js), `messages` ({id: {title, message}}: the `message` elements of the screen, which a `confirm`
 * action names in its target), `answers` ({target action: [actions]}: what the server answers to a server action of that
 * target action, for instance a `message` that shows a toast; the server action is not sent), `breadcrumbs` (labels of the menu path that the breadcrumb of the window template draws), `height` (a CSS length: data components such as
 * grids and charts fill their container, so they need one), `bare` (no frame), `colorScheme` ('light' or 'dark'
 * to force the mode of the example).
 */
export default function AweLive(props) {
  return (
    <BrowserOnly fallback={<Skeleton />}>
      {() => {
        const AweLiveRoot = require('./AweLiveRoot').default;
        return <AweLiveRoot {...props} />;
      }}
    </BrowserOnly>
  );
}
