/**
 * Route grid of the sign-in image panel: a map-like grid with dashed routes and small trucks (the truck mark of the
 * application) driving along them, in the blue of the application.
 *
 * The colours come from the design tokens (the primary colour of the theme, read from a probe element), the drawing is
 * on a canvas that fills the panel. With reduced motion it draws a single still frame; while the page is hidden it draws
 * nothing; it uses requestAnimationFrame and a ResizeObserver, and stops cleanly. Without a 2D context it does nothing
 */

const REDUCED_MOTION_QUERY = "(prefers-reduced-motion: reduce)";
const MAX_PIXEL_RATIO = 2;

// The geometry of static/images/illustrations/truck-mark.svg (64 x 64): the body, the cab and the two wheels
const TRUCK_BODY = new Path2D("M9 14h25a5 5 0 0 1 5 5v19H4V19a5 5 0 0 1 5-5zM43 22h8.6a4 4 0 0 1 3.2 1.6l5.4 7.2a4 4 0 0 1 .8 2.4V36a2 2 0 0 1-2 2H43zM47 26v6h8.2l-4.5-6zM16 39a6 6 0 1 0 0 12a6 6 0 1 0 0-12zM16 43a2 2 0 1 1 0 4a2 2 0 1 1 0-4zM51 39a6 6 0 1 0 0 12a6 6 0 1 0 0-12zM51 43a2 2 0 1 1 0 4a2 2 0 1 1 0-4z");
const TRUCK_SIZE = 64;

// Routes as fractions of the panel: each one is a polyline the trucks follow, at its own speed (segments per second)
const ROUTES = [
  {points: [[-0.05, 0.78], [0.25, 0.7], [0.45, 0.82], [0.72, 0.62], [1.05, 0.55]], speed: 0.05, offset: 0.1},
  {points: [[-0.05, 0.4], [0.2, 0.3], [0.5, 0.44], [0.78, 0.22], [1.05, 0.12]], speed: 0.04, offset: 0.55},
  {points: [[1.05, 0.92], [0.7, 0.88], [0.5, 0.95], [0.2, 0.9], [-0.05, 0.96]], speed: 0.035, offset: 0.3}
];

/**
 * Colour of a token as the numbers of its computed rgb
 * @param {Document} doc Document with the theme
 * @param {string} token CSS variable
 * @return {number[]} Red, green and blue
 */
const readToken = (doc, token) => {
  const probe = doc.createElement("span");
  probe.style.color = `var(${token})`;
  probe.style.display = "none";
  doc.body.appendChild(probe);
  const color = (doc.defaultView || window).getComputedStyle(probe).color;
  probe.remove();
  const numbers = color.match(/[\d.]+/g);
  return numbers && numbers.length >= 3 ? numbers.slice(0, 3).map(Number) : [37, 99, 235];
};

const mix = (color, other, ratio) => color.map((value, index) => Math.round(value * (1 - ratio) + other[index] * ratio));
const rgb = (color, alpha = 1) => `rgba(${color[0]}, ${color[1]}, ${color[2]}, ${alpha})`;

/**
 * Point of a polyline at a fraction of its length, with the angle of its segment
 * @param {number[][]} points Polyline in pixels
 * @param {number} fraction From 0 to 1
 * @return {{x: number, y: number, angle: number}} Position
 */
const pointAt = (points, fraction) => {
  const lengths = points.slice(1).map((point, index) => Math.hypot(point[0] - points[index][0], point[1] - points[index][1]));
  let remaining = fraction * lengths.reduce((sum, length) => sum + length, 0);
  for (let index = 0; index < lengths.length; index++) {
    if (remaining <= lengths[index] || index === lengths.length - 1) {
      const ratio = Math.min(remaining / lengths[index], 1);
      const [x0, y0] = points[index];
      const [x1, y1] = points[index + 1];
      return {x: x0 + (x1 - x0) * ratio, y: y0 + (y1 - y0) * ratio, angle: Math.atan2(y1 - y0, x1 - x0)};
    }
    remaining -= lengths[index];
  }
  return {x: 0, y: 0, angle: 0};
};

export const startRouteGrid = canvas => {
  const context = canvas.getContext("2d");
  if (!context) {
    return () => undefined;
  }
  const doc = canvas.ownerDocument;
  const view = doc.defaultView || window;
  const motionQuery = view.matchMedia ? view.matchMedia(REDUCED_MOTION_QUERY) : null;
  const startTime = performance.now();
  // The still frame of reduced motion: always the same moment, so a resize does not move the trucks
  const stillTime = startTime + 3000;
  let width = 0;
  let height = 0;
  let frame = 0;
  let stopped = false;
  let primary = [37, 99, 235];

  const reducedMotion = () => Boolean(motionQuery && motionQuery.matches);

  const resize = () => {
    const box = canvas.getBoundingClientRect();
    const ratio = Math.min(view.devicePixelRatio || 1, MAX_PIXEL_RATIO);
    width = box.width;
    height = box.height;
    canvas.width = Math.round(width * ratio);
    canvas.height = Math.round(height * ratio);
    context.setTransform(ratio, 0, 0, ratio, 0, 0);
    primary = readToken(doc, "--awe-color-primary");
  };

  const drawTruck = (x, y, angle, size, color) => {
    context.save();
    context.translate(x, y);
    context.rotate(Math.abs(angle) > Math.PI / 2 ? angle + Math.PI : angle);
    const scale = size / TRUCK_SIZE;
    context.scale(scale * (Math.abs(angle) > Math.PI / 2 ? -1 : 1), scale);
    context.translate(-TRUCK_SIZE / 2, -TRUCK_SIZE / 2);
    context.fillStyle = color;
    context.fill(TRUCK_BODY, "evenodd");
    context.restore();
  };

  const draw = time => {
    if (!width || !height) {
      return;
    }
    const seconds = (time - startTime) / 1000;
    const dark = mix(primary, [0, 0, 0], 0.82);
    const mid = mix(primary, [0, 0, 0], 0.62);
    const light = mix(primary, [255, 255, 255], 0.55);
    const gradient = context.createLinearGradient(0, 0, width, height);
    gradient.addColorStop(0, rgb(mid));
    gradient.addColorStop(1, rgb(dark));
    context.fillStyle = gradient;
    context.fillRect(0, 0, width, height);

    // The map grid
    const cell = Math.max(48, Math.round(Math.min(width, height) / 9));
    context.strokeStyle = rgb(light, 0.07);
    context.lineWidth = 1;
    context.beginPath();
    for (let x = 0; x <= width; x += cell) {
      context.moveTo(x, 0);
      context.lineTo(x, height);
    }
    for (let y = 0; y <= height; y += cell) {
      context.moveTo(0, y);
      context.lineTo(width, y);
    }
    context.stroke();

    ROUTES.forEach(route => {
      const points = route.points.map(([x, y]) => [x * width, y * height]);
      context.setLineDash([14, 10]);
      context.lineDashOffset = -seconds * 6;
      context.strokeStyle = rgb(light, 0.28);
      context.lineWidth = 3;
      context.lineCap = "round";
      context.lineJoin = "round";
      context.beginPath();
      points.forEach(([x, y], index) => (index ? context.lineTo(x, y) : context.moveTo(x, y)));
      context.stroke();
      context.setLineDash([]);
      // The stops of the route
      context.fillStyle = rgb(light, 0.35);
      points.slice(1, -1).forEach(([x, y]) => {
        context.beginPath();
        context.arc(x, y, 6, 0, Math.PI * 2);
        context.fill();
      });
      const fraction = (route.offset + seconds * route.speed) % 1;
      const truck = pointAt(points, fraction);
      drawTruck(truck.x, truck.y - 20, truck.angle, 56, rgb(light, 0.85));
    });
  };

  const cancel = () => {
    if (frame) {
      view.cancelAnimationFrame(frame);
      frame = 0;
    }
  };

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
    draw(reducedMotion() ? stillTime : performance.now());
  };
  const onVisibility = () => (doc.hidden ? cancel() : schedule());
  const onMotion = () => {
    cancel();
    draw(reducedMotion() ? stillTime : performance.now());
    schedule();
  };

  const observer = view.ResizeObserver ? new view.ResizeObserver(onResize) : null;
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
  draw(reducedMotion() ? stillTime : performance.now());
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
