/**
 * Animated ascii background of the sign-in image panel: a sample canvas effect of the awe-boot test application.
 *
 * AWE ships no effect (S23): the framework only hosts a canvas over the image panel and starts the effect the
 * application registered under the name of `awe.application.login.background-effect` (the global registry
 * `aweLoginEffects`). Every application writes its own style, so this one lives here and not in the framework.
 *
 * The picture: a soft gradient lit from the top that fades to a dark arch at the bottom, in the AWE logo colours,
 * under a grid of characters that slowly flows upwards and flickers. Plain canvas 2D, no dependency.
 */

/** A colour as its red, green and blue channels (0-255) */
const rgb = (red, green, blue) => [red, green, blue];

// AWE logo colours, hard-coded because they are the style of this sample: green #40AC5C, blue #257EC0,
// purple #8A4793 (accent), highlight #f6f8ff and the dark base #020617
const PALETTE = {
  base: rgb(2, 6, 23),
  colors: [rgb(64, 172, 92), rgb(37, 126, 192), rgb(138, 71, 147), rgb(246, 248, 255)]
};

/** Settings of the effect */
const SETTINGS = {
  /** Characters of the grid; a space is a blank cell */
  characters: " 01",
  /** Size of a cell of the grid in CSS pixels */
  cellSize: 15,
  /** Share of the cells that show a character, where the field is brightest (0-1) */
  density: 0.52,
  /** Probability of a cell changing its character on each tick (0-1) */
  flicker: 0.19,
  /** Softness of the edge of the dark arch (0 is a hard edge) */
  softness: 1,
  /** Speed of the drift of the colours and of the flow of the characters */
  flow: 0.45
};

const REDUCED_MOTION_QUERY = "(prefers-reduced-motion: reduce)";
// About 24 ticks per second
const TICK = 1000 / 24;
// Higher screen densities draw more pixels with no visible gain
const MAX_PIXEL_RATIO = 2;
// The characters are drawn with the monospace stack of the page
const FONT_FAMILY = "monospace";

// The dark arch: an ellipse centred under the bottom edge (in the unit square of the canvas, y going down)
const ARCH = {x: 0.5, y: 1.12, radiusX: 0.62, radiusY: 0.52};
// Width of the edge of the arch for a softness of 1
const ARCH_EDGE = 0.22;
// Height above the arch where the green turns into blue
const GREEN_TO_BLUE = [0.15, 0.6];
// Highlight glow at the top centre, drifting sideways
const GLOW = {radiusX: 0.28, radiusY: 0.3, drift: 0.18, strength: 0.9};
// Purple accent on the right side
const ACCENT = {x: 0.85, y: 0.5, radiusX: 0.22, radiusY: 0.2, strength: 0.45};

const clamp = (value, min = 0, max = 1) => Math.min(max, Math.max(min, value));

const smoothstep = (edge0, edge1, value) => {
  const t = clamp((value - edge0) / (edge1 - edge0));
  return t * t * (3 - 2 * t);
};

const glow = (dx, dy) => Math.exp(-(dx * dx + dy * dy));

/**
 * Mix two colours
 * @param {number[]} from First colour
 * @param {number[]} to Second colour
 * @param {number} amount Share of the second colour (0-1)
 * @return {number[]} Mixed colour (fractional channels)
 */
const mix = (from, to, amount) => {
  const share = clamp(amount);
  return [0, 1, 2].map(channel => from[channel] + (to[channel] - from[channel]) * share);
};

/**
 * Brightness of a colour (its relative luminance, without the gamma)
 * @param {number[]} color Colour
 * @return {number} Brightness between 0 and 1
 */
const brightness = color => (0.2126 * color[0] + 0.7152 * color[1] + 0.0722 * color[2]) / 255;

/**
 * Colour of the gradient at a point: the dark base inside the arch under the bottom edge, green right above it, blue
 * higher up, the highlight glowing at the top centre and the purple accent on the right side. The phase drifts the
 * glow and waves the edge of the arch
 * @param {number} x Horizontal position (0 left, 1 right)
 * @param {number} y Vertical position (0 top, 1 bottom)
 * @param {number} phase Time in seconds multiplied by the flow
 * @return {number[]} Colour with whole channels
 */
const fieldColor = (x, y, phase) => {
  const [green, blue, purple, highlight] = PALETTE.colors;
  const archX = (x - ARCH.x) / ARCH.radiusX;
  const archY = (y - ARCH.y) / ARCH.radiusY;
  // Height above the arch, with a slow wave
  const height = Math.hypot(archX, archY) - 1 + 0.035 * Math.sin(x * 5 + phase * 1.3);
  const lit = height <= 0 ? 0 : smoothstep(0, Math.max(ARCH_EDGE * SETTINGS.softness, 1e-4), height);

  let color = mix(green, blue, smoothstep(GREEN_TO_BLUE[0], GREEN_TO_BLUE[1], height));
  const accent = glow((x - ACCENT.x) / ACCENT.radiusX, (y - ACCENT.y) / ACCENT.radiusY) * ACCENT.strength;
  color = mix(color, purple, accent);
  const glowX = 0.5 + GLOW.drift * Math.sin(phase * 0.5);
  color = mix(color, highlight, glow((x - glowX) / GLOW.radiusX, y / GLOW.radiusY) * GLOW.strength);
  return mix(PALETTE.base, color, lit).map(channel => clamp(Math.round(channel), 0, 255));
};

/**
 * Character of a cell: shown when the seed of the cell is under a threshold that grows with the density and the
 * brightness of the field, chosen from the set by the seed
 * @param {number} light Brightness of the field at the cell (0-1)
 * @param {number} seed Seed of the cell (0-1)
 * @return {string} Character, a space for a blank cell
 */
const cellCharacter = (light, seed) => {
  const {characters, density} = SETTINGS;
  const threshold = clamp(density) * (0.15 + 0.85 * clamp(light));
  if (!characters || seed >= threshold) {
    return " ";
  }
  return characters[Math.min(characters.length - 1, Math.floor((seed / threshold) * characters.length))];
};

/**
 * Colour of a character: a brighter tint of the colour of its cell, towards the highlight
 * @param {number[]} color Colour of the cell
 * @return {number[]} Colour with whole channels
 */
const characterColor = color => mix(color, PALETTE.colors[3], 0.45).map(Math.round);

/**
 * Flicker: each cell takes a new seed with the probability of the flicker
 * @param {Float32Array} seeds Seeds of the cells, changed in place
 * @return {number} Number of cells changed
 */
const flickerSeeds = seeds => {
  let changed = 0;
  for (let index = 0; index < seeds.length; index++) {
    if (Math.random() < SETTINGS.flicker) {
      seeds[index] = Math.random();
      changed++;
    }
  }
  return changed;
};

/**
 * Draw the effect on a canvas: it fills the box of the canvas and follows its size. With reduced motion it draws a
 * single still frame, again only when the canvas is resized; while the page is hidden it draws nothing. Without a 2D
 * context (an old browser) it does nothing
 * @param {HTMLCanvasElement} canvas Canvas the framework placed over the image panel
 * @return {Function} Stop function: no more frames, observers or listeners
 */
export const startAsciiEffect = canvas => {
  const context = canvas.getContext("2d");
  const field = context ? canvas.ownerDocument.createElement("canvas") : null;
  const fieldContext = field ? field.getContext("2d") : null;
  if (!context || !field || !fieldContext) {
    return () => undefined;
  }

  const cell = Math.max(4, SETTINGS.cellSize);
  const view = canvas.ownerDocument.defaultView || window;
  const doc = canvas.ownerDocument;
  const motionQuery = view.matchMedia ? view.matchMedia(REDUCED_MOTION_QUERY) : null;
  const startTime = performance.now();

  let width = 0;
  let height = 0;
  let columns = 0;
  let rows = 0;
  let seeds = new Float32Array(0);
  let image = null;
  let font = "";
  let frame = 0;
  let lastTick = -Infinity;
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
    // A spare column, and the rows that flow in from the bottom
    columns = Math.ceil(width / cell) + 1;
    rows = Math.ceil(height / cell) + 2;
    field.width = columns;
    field.height = rows;
    image = width > 0 && height > 0 ? fieldContext.createImageData(columns, rows) : null;
    seeds = Float32Array.from({length: columns * rows}, () => Math.random());
    font = `${Math.round(cell * 0.8)}px ${FONT_FAMILY}`;
  };

  const draw = time => {
    if (!image) {
      return;
    }
    const seconds = (time - startTime) / 1000;
    const phase = seconds * SETTINGS.flow;
    // The gradient: one pixel per cell, scaled up smoothly
    const {data} = image;
    for (let row = 0; row < rows; row++) {
      for (let column = 0; column < columns; column++) {
        const color = fieldColor(((column + 0.5) * cell) / width, ((row + 0.5) * cell) / height, phase);
        const index = (row * columns + column) * 4;
        data[index] = color[0];
        data[index + 1] = color[1];
        data[index + 2] = color[2];
        data[index + 3] = 255;
      }
    }
    fieldContext.putImageData(image, 0, 0);
    context.clearRect(0, 0, width, height);
    context.imageSmoothingEnabled = true;
    context.imageSmoothingQuality = "high";
    context.drawImage(field, 0, 0, columns, rows, 0, 0, columns * cell, rows * cell);

    // The characters flow upwards through the gradient
    const risen = seconds * SETTINGS.flow * cell;
    const rowShift = Math.floor(risen / cell);
    const offset = risen - rowShift * cell;
    context.font = font;
    context.textAlign = "center";
    context.textBaseline = "middle";
    for (let row = 0; row < rows; row++) {
      const seedRow = (row + rowShift) % rows;
      for (let column = 0; column < columns; column++) {
        const index = (row * columns + column) * 4;
        const color = [data[index], data[index + 1], data[index + 2]];
        const light = brightness(color);
        const character = cellCharacter(light, seeds[seedRow * columns + column]);
        if (character.trim()) {
          const [red, green, blue] = characterColor(color);
          context.fillStyle = `rgba(${red}, ${green}, ${blue}, ${(0.35 + 0.55 * light).toFixed(2)})`;
          context.fillText(character, (column + 0.5) * cell, row * cell - offset + cell / 2);
        }
      }
    }
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

  const tick = () => {
    frame = 0;
    const time = performance.now();
    if (time - lastTick >= TICK) {
      lastTick = time;
      flickerSeeds(seeds);
      draw(time);
    }
    schedule();
  };

  // The still frame of reduced motion is the start of the animation
  const drawNow = () => {
    lastTick = reducedMotion() ? startTime : performance.now();
    draw(lastTick);
  };

  const onResize = () => {
    resize();
    drawNow();
  };
  const onVisibility = () => (doc.hidden ? cancel() : schedule());
  const onMotion = () => {
    cancel();
    drawNow();
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
  drawNow();
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

// Register the effect under the name selected with awe.application.login.background-effect. The AngularJS engine
// exposes its registry on globalThis (aweLoginEffects) and its bundle loads before this application bundle
if (globalThis.aweLoginEffects) {
  globalThis.aweLoginEffects.register("ascii", startAsciiEffect);
} else {
  console.error("The aweLoginEffects registry of the framework is not available: the ascii login effect is not registered");
}
