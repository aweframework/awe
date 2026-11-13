/**
 * Soft blobs of the sign-in image panel: a sample canvas effect of the awe-boot-react test application (S23).
 *
 * AWE ships no effect: the framework only hosts a canvas over the image panel and starts the effect the application
 * registered under the name of `awe.application.login.background-effect`, so every application writes its own style.
 * This one is deliberately different from the ascii sample of awe-boot: large soft lights in the AWE brand colours
 * drifting over a dark base, without any character grid.
 */

// #020617: the dark base the blobs float over
const BASE = [2, 6, 23];

// AWE logo colours as soft lights: green #40AC5C, blue #257EC0, purple #8A4793 and the highlight #f6f8ff. Position,
// radius and drift are fractions of the canvas box, speed is in radians per second
const BLOBS = [
  {color: [64, 172, 92], x: 0.28, y: 0.34, radius: 0.62, driftX: 0.14, driftY: 0.09, speed: 0.13, alpha: 0.55},
  {color: [37, 126, 192], x: 0.68, y: 0.58, radius: 0.7, driftX: 0.11, driftY: 0.12, speed: 0.09, alpha: 0.5},
  {color: [138, 71, 147], x: 0.82, y: 0.24, radius: 0.44, driftX: 0.08, driftY: 0.1, speed: 0.07, alpha: 0.4},
  {color: [246, 248, 255], x: 0.45, y: 0.12, radius: 0.34, driftX: 0.16, driftY: 0.06, speed: 0.05, alpha: 0.3}
];

const REDUCED_MOTION_QUERY = "(prefers-reduced-motion: reduce)";
// Higher screen densities draw more pixels with no visible gain
const MAX_PIXEL_RATIO = 2;

/**
 * Draw the blobs on a canvas: it fills the box of the canvas and follows its size. With reduced motion it draws a
 * single still frame, again only when the canvas is resized; while the page is hidden it draws nothing. Without a 2D
 * context (an old browser) it does nothing
 * @param {HTMLCanvasElement} canvas Canvas the framework placed over the image panel
 * @return {Function} Stop function: no more frames, observers or listeners
 */
export const startSoftBlobs = canvas => {
  const context = canvas.getContext("2d");
  if (!context) {
    return () => undefined;
  }

  const view = canvas.ownerDocument.defaultView || window;
  const doc = canvas.ownerDocument;
  const motionQuery = view.matchMedia ? view.matchMedia(REDUCED_MOTION_QUERY) : null;
  const startTime = performance.now();
  let width = 0;
  let height = 0;
  let frame = 0;
  let stopped = false;

  const reducedMotion = () => Boolean(motionQuery && motionQuery.matches);

  const resize = () => {
    const box = canvas.getBoundingClientRect();
    const ratio = Math.min(view.devicePixelRatio || 1, MAX_PIXEL_RATIO);
    width = box.width;
    height = box.height;
    canvas.width = Math.round(width * ratio);
    canvas.height = Math.round(height * ratio);
    context.setTransform(ratio, 0, 0, ratio, 0, 0);
  };

  const draw = time => {
    if (!width || !height) {
      return;
    }
    const seconds = (time - startTime) / 1000;
    context.globalCompositeOperation = "source-over";
    context.fillStyle = `rgb(${BASE[0]}, ${BASE[1]}, ${BASE[2]})`;
    context.fillRect(0, 0, width, height);
    // The lights add up over the base, so the overlaps glow
    context.globalCompositeOperation = "lighter";
    const longest = Math.max(width, height);
    BLOBS.forEach(blob => {
      const turn = seconds * blob.speed;
      const x = (blob.x + blob.driftX * Math.sin(turn)) * width;
      const y = (blob.y + blob.driftY * Math.cos(turn * 0.8)) * height;
      const radius = Math.max(blob.radius * longest, 1);
      const [red, green, blue] = blob.color;
      const gradient = context.createRadialGradient(x, y, 0, x, y, radius);
      gradient.addColorStop(0, `rgba(${red}, ${green}, ${blue}, ${blob.alpha})`);
      gradient.addColorStop(0.55, `rgba(${red}, ${green}, ${blue}, ${blob.alpha * 0.35})`);
      gradient.addColorStop(1, `rgba(${red}, ${green}, ${blue}, 0)`);
      context.fillStyle = gradient;
      context.beginPath();
      context.arc(x, y, radius, 0, Math.PI * 2);
      context.fill();
    });
    context.globalCompositeOperation = "source-over";
  };

  const cancel = () => {
    if (frame) {
      view.cancelAnimationFrame(frame);
      frame = 0;
    }
  };

  // A new frame unless stopped, still (reduced motion) or hidden
  const schedule = () => {
    if (!stopped && !frame && !reducedMotion() && !doc.hidden) {
      frame = view.requestAnimationFrame(tick);
    }
  };

  function tick() {
    frame = 0;
    draw(performance.now());
    schedule();
  }

  const onResize = () => {
    resize();
    draw(reducedMotion() ? startTime : performance.now());
  };
  const onVisibility = () => (doc.hidden ? cancel() : schedule());
  const onMotion = () => {
    cancel();
    draw(reducedMotion() ? startTime : performance.now());
    schedule();
  };

  const ResizeObserverClass = view.ResizeObserver;
  const observer = ResizeObserverClass ? new ResizeObserverClass(onResize) : null;
  if (observer) {
    observer.observe(canvas);
  } else {
    view.addEventListener("resize", onResize);
  }
  doc.addEventListener("visibilitychange", onVisibility);
  if (motionQuery && motionQuery.addEventListener) {
    motionQuery.addEventListener("change", onMotion);
  }

  resize();
  draw(reducedMotion() ? startTime : performance.now());
  schedule();

  return () => {
    stopped = true;
    cancel();
    if (observer) {
      observer.disconnect();
    }
    view.removeEventListener("resize", onResize);
    doc.removeEventListener("visibilitychange", onVisibility);
    if (motionQuery && motionQuery.removeEventListener) {
      motionQuery.removeEventListener("change", onMotion);
    }
  };
};
