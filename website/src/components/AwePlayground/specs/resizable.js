/**
 * Playground of the AWE resizable: a container whose sides the user drags to change its size. Every attribute is one the
 * XSD declares for `resizable` and the React client reads (`directions`, `style`); the client reads them from the node of
 * the structure. The container needs an initial size, which a real screen gives with a grid class in `style`; the stage
 * gives it with a docs-only class that is not in the XML. Drag the handles in the preview.
 * A playground spec is described in playgroundModel.js.
 */
const SIDES = ['right', 'left', 'bottom', 'top'];

const directionsOf = values => SIDES.filter(side => values[side]);

const resizable = {
  name: 'resizable',
  element: 'Resizable',
  tag: 'resizable',
  id: 'demo',
  reference: {element: 'resizable', page: 'screen'},
  api: '/docs/api/resizable',
  stage: 'wide',
  controls: [
    {id: 'right', type: 'switch', label: 'Right handle', xml: 'directions', client: '', token: 'right', default: false, initial: true},
    {id: 'left', type: 'switch', label: 'Left handle', xml: 'directions', client: '', token: 'left', default: false},
    {id: 'bottom', type: 'switch', label: 'Bottom handle', xml: 'directions', client: '', token: 'bottom', default: false, initial: true},
    {id: 'top', type: 'switch', label: 'Top handle', xml: 'directions', client: '', token: 'top', default: false},
  ],
  children: [{tag: 'tag', attrs: {type: 'p', label: 'Drag a handle to change the size of this panel.'}}],
  scene: values => ({
    node: {
      directions: directionsOf(values).join(' '),
      style: 'live-resizable',
      elementList: [{elementType: 'Tag', type: 'p', label: 'Drag a handle to change the size of this panel.'}],
    },
  }),
  events: [],
  actions: [],
};

module.exports = {resizable};
