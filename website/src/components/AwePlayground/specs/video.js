/**
 * Playground of the AWE video: the `video` element plays the file of its `src`. Every attribute is one the XSD declares
 * for `video` and the React client reads. The clip is a three seconds animation generated for the docs, and the poster a
 * static image of the site: with a poster the player waits for a click before it loads the clip.
 * A playground spec is described in playgroundModel.js.
 */
const video = {
  name: 'video',
  element: 'Video',
  tag: 'video',
  id: 'demo',
  reference: {element: 'video', page: 'screen'},
  api: '/docs/api/media',
  stage: 'wide',
  controls: [
    {
      id: 'poster', type: 'segmented', label: 'Poster', xml: 'poster', default: '',
      options: [
        {value: '', label: 'None'},
        {value: '/live/media/poster.svg', label: 'Poster'},
      ],
    },
    {id: 'controls', type: 'switch', label: 'Show the controls', xml: 'controls', default: true},
    {id: 'loop', type: 'switch', label: 'Loop', xml: 'loop', default: false},
    {
      id: 'preload', type: 'segmented', label: 'Preload', xml: 'preload', default: '',
      options: [
        {value: '', label: 'Default'},
        {value: 'none', label: 'None'},
        {value: 'metadata', label: 'Metadata'},
        {value: 'full-video', label: 'Full video'},
      ],
    },
  ],
  xmlAttributes: () => [['src', '/live/media/clip.webm']],
  scene: () => ({attributes: {src: '/live/media/clip.webm'}}),
  events: [],
  actions: [],
};

module.exports = {video};
