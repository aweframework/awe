import React from 'react';

// 24x24 line icons, drawn like the ones of the homepage (stroke only, they take the colour of the text)
const PATHS = {
  button: <><rect x="3" y="8" width="18" height="8" rx="3" /><path d="M8 12h8" /></>,
  criteria: <><rect x="3" y="7" width="18" height="10" rx="2" /><path d="M7 11v2" /></>,
  'criteria-text': <><rect x="3" y="7" width="18" height="10" rx="2" /><path d="M7 11h6" /></>,
  'criteria-numbers': <><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M3 10h18M8 3v4M16 3v4" /></>,
  'criteria-selects': <><rect x="3" y="4" width="18" height="6" rx="2" /><path d="M7 14h10M7 18h6" /></>,
  'criteria-checkbox-radio': <><rect x="3" y="4" width="7" height="7" rx="1.5" /><circle cx="6.5" cy="17.5" r="3.5" /><path d="M14 7h7M14 18h7" /></>,
  'criteria-upload': <path d="M12 16V4m0 0L8 8m4-4 4 4M4 16v3a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-3" />,
  grid: <><rect x="3" y="3" width="18" height="18" rx="2" /><path d="M3 9h18M3 15h18M9 3v18" /></>,
  'tree-grid': <path d="M4 5h5M8 12h5M12 19h8M6 5v7M10 12v7" />,
  'pivot-table': <><rect x="3" y="3" width="18" height="18" rx="2" /><path d="M3 9h18M9 3v18M13 13h4M13 17h4" /></>,
  chart: <path d="M3 3v18h18M8 17v-5M13 17V8M18 17v-9" />,
  wizard: <><circle cx="5" cy="12" r="2" /><circle cx="12" cy="12" r="2" /><circle cx="19" cy="12" r="2" /><path d="M7 12h3M14 12h3" /></>,
  'tabs-accordion': <><rect x="3" y="8" width="18" height="12" rx="2" /><path d="M3 8V5h7v3" /></>,
  map: <path d="M9 4 3 6v14l6-2 6 2 6-2V4l-6 2zM9 4v14M15 6v14" />,
  'file-viewer': <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8zM14 3v5h5" />,
  card: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="M3 10h18" /></>,
  dialog: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="M3 9h18M8 15h8" /></>,
};

export default function ComponentIcon({name}) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {PATHS[name] ?? PATHS.card}
    </svg>
  );
}
