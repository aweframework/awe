// Classes of the frame of a live example. Pure on purpose, so that node:test can run it.

/**
 * The frame is the `.maximize-target` of the client (the container a maximized window covers, the center area of the
 * template in an application), so a maximized window covers the example and not the page of the docs.
 * @param {{bare?: boolean, height?: string}} props `bare` drops the frame; `height` makes the example a sized column
 * @returns {string} Class names of the frame
 */
function liveClassName({bare, height}) {
  return ['awe-live', 'maximize-target', bare && 'awe-live--bare', height && 'awe-live--sized'].filter(Boolean).join(' ');
}

module.exports = {liveClassName};
