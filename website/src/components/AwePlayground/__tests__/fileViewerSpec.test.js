const test = require('node:test');
const assert = require('node:assert/strict');
const {resolveValues, toScene, toXml} = require('../playgroundModel');
const {fileViewer, fileViewerExample} = require('../specs/file-viewer');

// A file viewer scrolls inside its space (style scrollable, as the screens of the application): without it a long file
// grows out of the expanded space and over the page
test('a file viewer expands and scrolls, in the XML, the stage and the examples; a pdf viewer only expands', () => {
  const file = resolveValues(fileViewer, {type: 'file-viewer'});
  const pdf = resolveValues(fileViewer, {type: 'pdf-viewer'});

  assert.match(toXml(fileViewer, file), /style="expand scrollable"/);
  assert.equal(toScene(fileViewer, file).components.demo.attributes.style, 'expand scrollable');
  assert.equal(fileViewerExample('file-viewer').components.viewer.attributes.style, 'expand scrollable');
  assert.match(toXml(fileViewer, pdf), /style="expand"/);
  assert.equal(fileViewerExample('pdf-viewer').components.viewer.attributes.style, 'expand');
});
