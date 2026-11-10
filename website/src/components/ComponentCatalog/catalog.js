/**
 * Data of the component catalog. Adding a component is adding an entry here (and its page in docs/components):
 *
 *   id          page name: docs/components/<id>.mdx (route /docs/components/<id>)
 *   family      one of FAMILIES
 *   element     XML element of the component
 *   status      'ready' (it has its page, `to`) or 'soon' (a card without a link)
 *   to          route of the page (only when ready)
 *   title, description  English text; the UI translates it with the ids catalog.<id>.title / .description
 *
 * Pure data (CommonJS) so that node:test can check it.
 */
const FAMILIES = [
  {id: 'forms', title: 'Forms'},
  {id: 'data', title: 'Data'},
  {id: 'layout', title: 'Layout'},
  {id: 'charts', title: 'Charts & maps'},
  {id: 'media', title: 'Media'},
];

const COMPONENTS = [
  {
    id: 'button', family: 'forms', element: 'button', status: 'ready', to: '/docs/components/button',
    title: 'Button', description: 'Launches actions: search, save, confirm. Normal, submit, reset, icon-only.',
  },
  {
    id: 'criteria', family: 'forms', element: 'criteria', status: 'ready', to: '/docs/components/criteria',
    title: 'Criteria', description: 'The inputs of a form: the attributes they share and a guide to every type.',
  },
  {
    id: 'criteria-text', family: 'forms', element: 'criteria', status: 'ready', to: '/docs/components/criteria-text',
    title: 'Text inputs', description: 'Text, password, text area, Markdown editor, text view and color.',
  },
  {
    id: 'criteria-numbers', family: 'forms', element: 'criteria', status: 'ready', to: '/docs/components/criteria-numbers',
    title: 'Numbers and dates', description: 'Numeric with a slider, date, date range, time and filtered calendar.',
  },
  {
    id: 'criteria-choices', family: 'forms', element: 'criteria', status: 'ready', to: '/docs/components/criteria-choices',
    title: 'Choices', description: 'Select, suggest, picklist, radio, checkbox and their multiple versions.',
  },
  {
    id: 'criteria-upload', family: 'forms', element: 'criteria', status: 'ready', to: '/docs/components/criteria-upload',
    title: 'File upload', description: 'Upload a file with a drop zone, a preview and camera capture.',
  },
  {
    id: 'grid', family: 'data', element: 'grid', status: 'ready', to: '/docs/components/grid',
    title: 'Grid', description: 'Editable, sortable, paginated tables with column filters and selection.',
  },
  {
    id: 'tree-grid', family: 'data', element: 'grid', status: 'ready', to: '/docs/components/tree-grid',
    title: 'Tree grid', description: 'A grid with expandable parent and child rows for hierarchical data.',
  },
  {
    id: 'pivot-table', family: 'data', element: 'pivot-table', status: 'ready', to: '/docs/components/pivot-table',
    title: 'Pivot table', description: 'Cross-tab matrices with rows, columns and aggregations.',
  },
  {
    id: 'chart', family: 'charts', element: 'chart', status: 'ready', to: '/docs/components/chart',
    title: 'Chart', description: 'Bar, line, pie and more, fed by the same queries as the grids.',
  },
  {
    id: 'wizard', family: 'layout', element: 'wizard', status: 'ready', to: '/docs/components/wizard',
    title: 'Wizard', description: 'Step-by-step flows with a progress indicator and validation per step.',
  },
  {
    id: 'tabs-accordion', family: 'layout', element: 'tabcontainer', status: 'ready', to: '/docs/components/tabs-accordion',
    title: 'Tabs and accordion', description: 'Organise dense screens into tabs or collapsible sections.',
  },
  {
    id: 'map', family: 'charts', element: 'map', status: 'ready', to: '/docs/components/map',
    title: 'Map', description: 'Interactive maps with selectable markers and routes.',
  },
  {
    id: 'file-viewer', family: 'media', element: 'widget', status: 'ready', to: '/docs/components/file-viewer',
    title: 'File viewer', description: 'Show text files and PDF documents next to your data.',
  },
  {
    id: 'card', family: 'layout', element: 'card', status: 'ready', to: '/docs/components/card',
    title: 'Card', description: 'Group related content in a titled, optionally collapsible surface.',
  },
  {
    id: 'dialog', family: 'layout', element: 'dialog', status: 'ready', to: '/docs/components/dialog',
    title: 'Dialog', description: 'Modal windows for confirmations and focused tasks.',
  },
  {
    id: 'side-panel-popover', family: 'layout', element: 'side-panel', status: 'ready', to: '/docs/components/side-panel-popover',
    title: 'Side panel and popover', description: 'Panels on the edge of the screen and popovers next to a control, without leaving the screen.',
  },
  {
    id: 'context-menu', family: 'layout', element: 'context-button', status: 'ready', to: '/docs/components/context-menu',
    title: 'Context menu', description: 'Right-click menus with icons, separators, submenus and any action.',
  },
  {
    id: 'messages-info', family: 'layout', element: 'message', status: 'ready', to: '/docs/components/messages-info',
    title: 'Messages and info', description: 'Confirm dialogs, toasts and messages over a field, plus the info controls of the header.',
  },
  {
    id: 'window-layout', family: 'layout', element: 'window', status: 'ready', to: '/docs/components/window-layout',
    title: 'Window and layout', description: 'Windows, resizable panels and the screen template with its breadcrumb.',
  },
  {
    id: 'tags-lists', family: 'layout', element: 'tag', status: 'ready', to: '/docs/components/tags-lists',
    title: 'Tags and lists', description: 'Section titles, plain text and a tag generated for each row of a query.',
  },
  {
    id: 'media-content', family: 'media', element: 'avatar', status: 'ready', to: '/docs/components/media-content',
    title: 'Media and content', description: 'Avatars, videos with a poster, images and links.',
  },
  {
    id: 'widgets', family: 'media', element: 'widget', status: 'ready', to: '/docs/components/widgets',
    title: 'Widgets', description: 'A carousel of images and videos, and the help viewer of the application.',
  },
];

/** Components of one family, or all of them (family 'all'), in the order of the catalog. */
function filterComponents(components, family) {
  return family === 'all' ? components : components.filter(component => component.family === family);
}

/** The families that have at least one of the given components, in the order of the given families. */
function familiesInUse(components, families) {
  return families.filter(family => components.some(component => component.family === family.id));
}

module.exports = {FAMILIES, COMPONENTS, filterComponents, familiesInUse};
