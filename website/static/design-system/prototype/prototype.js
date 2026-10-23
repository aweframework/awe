/*
 * AWE 5 visual reference prototype - behaviour.
 * The theme panel only changes <html>: the class `light` / `dark` and the attributes data-awe-primary,
 * data-awe-surface and data-awe-density. Everything else is presentation-only demo behaviour.
 */
(() => {
  'use strict';

  const root = document.documentElement;
  const SPRITE = 'assets/icons/sprite.svg';
  const STORAGE_KEY = 'awe-prototype-theme';
  const icon = (name, cls = '') => `<svg class="i ${cls}" aria-hidden="true"><use href="${SPRITE}#${name}"/></svg>`;
  const $ = (sel, ctx = document) => ctx.querySelector(sel);
  const $$ = (sel, ctx = document) => Array.from(ctx.querySelectorAll(sel));

  // Every data value interpolated into markup goes through esc(); render() parses the prototype's own templates in
  // the context of the target element (so <tr> rows parse inside a <tbody>) and replaces its children.
  const ESCAPES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
  const esc = (value) => String(value).replace(/[&<>"']/g, (c) => ESCAPES[c]);
  const render = (el, markup) => {
    const range = document.createRange();
    range.selectNodeContents(el);
    el.replaceChildren(range.createContextualFragment(markup));
  };

  /* ======================================================================
     Palette generator: a hex colour -> the --awe-primary-* or --awe-surface-* scale (no dependencies)
     The hex is converted to OKLCH; its hue and chroma are kept and every step gets a fixed lightness, so the contrast
     pairs of the semantic tokens hold for any hue. Chroma is clamped into the sRGB gamut per step; the surface chroma
     is capped so a saturated hex only tints the neutrals. The primary fill is chosen by measured contrast.
     ====================================================================== */
  const STEPS = ['50', '100', '200', '300', '400', '500', '600', '700', '800', '900', '950'];
  const PRIMARY_LIGHTNESS = { 50: 0.97, 100: 0.935, 200: 0.885, 300: 0.81, 400: 0.72, 500: 0.64, 600: 0.56,
    700: 0.49, 800: 0.43, 900: 0.38, 950: 0.28 };
  // Very light steps keep a pastel chroma, as the named palettes do
  const PRIMARY_CHROMA_CAP = { 50: 0.035, 100: 0.07 };
  const SURFACE_LIGHTNESS = { 50: 0.985, 100: 0.968, 200: 0.929, 300: 0.869, 400: 0.711, 500: 0.554, 600: 0.446,
    700: 0.372, 800: 0.279, 900: 0.208, 950: 0.129 };
  const SURFACE_CHROMA_MAX = 0.03;
  const SURFACE_CHROMA_PROFILE = { 50: 0.35, 100: 0.45, 200: 0.6, 300: 0.75, 400: 0.9, 500: 1, 600: 1, 700: 1,
    800: 0.95, 900: 0.9, 950: 0.8 };
  // Lightest dark-mode background a primary text or fill sits on (surface 900 with the highlight mix)
  const DARK_REFERENCE_LIGHTNESS = 0.3;
  const AA = 4.5;
  const HEX_RE = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i;

  const normalizeHex = (value) => {
    const v = String(value || '').trim();
    if (!HEX_RE.test(v)) return null;
    const h = v.slice(1).toLowerCase();
    return `#${h.length === 3 ? h.replace(/./g, (c) => c + c) : h}`;
  };
  const toLinear = (c) => (c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4);
  const fromLinear = (c) => (c <= 0.0031308 ? 12.92 * c : 1.055 * c ** (1 / 2.4) - 0.055);
  const hexToRgb = (hex) => [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255);
  const rgbToHex = (rgb) => `#${rgb.map((c) => Math.round(Math.min(1, Math.max(0, c)) * 255)
    .toString(16).padStart(2, '0')).join('')}`;

  const hexToOklch = (hex) => {
    const [r, g, b] = hexToRgb(hex).map(toLinear);
    const l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
    const m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
    const s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
    const L = 0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s;
    const A = 1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s;
    const B = 0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s;
    return { L, C: Math.hypot(A, B), H: Math.atan2(B, A) };
  };
  // Linear sRGB of an OKLCH colour (may be out of gamut)
  const oklchToLinear = ({ L, C, H }) => {
    const A = C * Math.cos(H);
    const B = C * Math.sin(H);
    const l = (L + 0.3963377774 * A + 0.2158037573 * B) ** 3;
    const m = (L - 0.1055613458 * A - 0.0638541728 * B) ** 3;
    const s = (L - 0.0894841775 * A - 1.291485548 * B) ** 3;
    return [4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
      -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
      -0.0041960863 * l - 0.7034186147 * m + 1.7076949378 * s];
  };
  const inGamut = (lin) => lin.every((c) => c >= -1e-4 && c <= 1 + 1e-4);
  // Keep L and H, reduce the chroma until the colour fits in sRGB (binary search)
  const oklchToHex = ({ L, C, H }) => {
    let lo = 0;
    let hi = C;
    if (!inGamut(oklchToLinear({ L, C, H }))) {
      for (let i = 0; i < 24; i++) {
        const mid = (lo + hi) / 2;
        if (inGamut(oklchToLinear({ L, C: mid, H }))) lo = mid;
        else hi = mid;
      }
      C = lo;
    }
    return rgbToHex(oklchToLinear({ L, C, H }).map(fromLinear));
  };
  const luminance = (hex) => {
    const [r, g, b] = hexToRgb(hex).map(toLinear);
    return 0.2126 * r + 0.7152 * g + 0.0722 * b;
  };
  const contrast = (a, b) => {
    const [x, y] = [luminance(a), luminance(b)].sort((p, q) => q - p);
    return (x + 0.05) / (y + 0.05);
  };

  const buildScale = (base, lightness, chromaAt) => {
    const scale = {};
    STEPS.forEach((s) => {
      scale[s] = { L: lightness[s], C: chromaAt(s), H: base.H };
    });
    return scale;
  };
  // Move one step's lightness (by 0.005) until the check passes; keeps the hue and chroma
  const ensure = (scale, step, check, direction) => {
    for (let i = 0; i < 60 && !check(oklchToHex(scale[step])); i++) {
      scale[step] = { ...scale[step], L: scale[step].L + 0.005 * direction };
    }
  };

  const generatePrimary = (hex) => {
    const base = hexToOklch(hex);
    const scale = buildScale(base, PRIMARY_LIGHTNESS, (s) => Math.min(base.C, PRIMARY_CHROMA_CAP[s] ?? Infinity));
    const darkRef = oklchToHex({ L: DARK_REFERENCE_LIGHTNESS, C: Math.min(base.C, 0.04), H: base.H });
    const at = (s) => oklchToHex(scale[s]);
    // Light mode: primary text (700) on white, on the highlight (50) and on the subtle background (100)
    ensure(scale, '700', (c) => contrast(c, '#ffffff') >= AA && contrast(c, at('50')) >= AA
      && contrast(c, at('100')) >= AA, -1);
    ensure(scale, '800', (c) => contrast(c, at('50')) >= AA && contrast(c, at('100')) >= AA, -1);
    // Dark mode: primary text and fill (400) and the subtle text (300) on the dark surfaces
    ensure(scale, '400', (c) => contrast(c, darkRef) >= AA, 1);
    ensure(scale, '300', (c) => contrast(c, darkRef) >= AA, 1);
    const hexes = Object.fromEntries(STEPS.map((s) => [s, at(s)]));

    // Fill: the step closest to the brand lightness whose label (white or the 950 step) reaches AA
    const white = '#ffffff';
    const dark = hexes['950'];
    const idx = (s) => STEPS.indexOf(s);
    const candidates = [];
    ['500', '600', '700', '800'].forEach((s) => {
      const hover = STEPS[idx(s) + 1];
      const active = STEPS[Math.min(idx(s) + 2, STEPS.length - 1)];
      if ([s, hover, active].every((x) => contrast(hexes[x], white) >= AA)) {
        candidates.push({ s, hover, active, on: white, d: Math.abs(scale[s].L - base.L) });
      }
    });
    ['300', '400', '500', '600'].forEach((s) => {
      const hover = STEPS[idx(s) - 1];
      const active = [STEPS[idx(s) + 1], STEPS[idx(s) - 2]].find((x) => contrast(hexes[x], dark) >= AA);
      if (active && [s, hover].every((x) => contrast(hexes[x], dark) >= AA)) {
        candidates.push({ s, hover, active, on: dark, d: Math.abs(scale[s].L - base.L) + 0.001 });
      }
    });
    candidates.sort((p, q) => p.d - q.d);
    const fill = candidates[0] || { s: '800', hover: '900', active: '950', on: white };
    const vars = {};
    STEPS.forEach((s) => {
      vars[`--awe-primary-${s}`] = hexes[s];
    });
    vars['--awe-primary-fill'] = hexes[fill.s];
    vars['--awe-primary-fill-hover'] = hexes[fill.hover];
    vars['--awe-primary-fill-active'] = hexes[fill.active];
    vars['--awe-primary-on-fill'] = fill.on;
    return vars;
  };

  const generateSurface = (hex) => {
    const base = hexToOklch(hex);
    const chroma = Math.min(base.C, SURFACE_CHROMA_MAX);
    const scale = buildScale(base, SURFACE_LIGHTNESS, (s) => chroma * SURFACE_CHROMA_PROFILE[s]);
    const at = (s) => oklchToHex(scale[s]);
    // Light: muted text (500) on white and on the page (50). Dark: muted text (400) on the dark surfaces
    ensure(scale, '500', (c) => contrast(c, '#ffffff') >= AA && contrast(c, at('50')) >= AA
      && contrast(c, at('100')) >= AA, -1);
    ensure(scale, '400', (c) => contrast(c, at('800')) >= AA && contrast(c, at('900')) >= AA, 1);
    const vars = { '--awe-surface-0': '#ffffff' };
    STEPS.forEach((s) => {
      vars[`--awe-surface-${s}`] = at(s);
    });
    return vars;
  };

  const CUSTOM_PROPS = [...STEPS.map((s) => `--awe-primary-${s}`), '--awe-primary-fill', '--awe-primary-fill-hover',
    '--awe-primary-fill-active', '--awe-primary-on-fill', '--awe-surface-0', ...STEPS.map((s) => `--awe-surface-${s}`)];
  // Inline custom properties on <html> win over the palette rules (and over the light-dark() surface defaults)
  const customVars = (theme) => ({
    ...(theme.primary === 'custom' && normalizeHex(theme.primaryHex) ? generatePrimary(normalizeHex(theme.primaryHex)) : {}),
    ...(theme.surface === 'custom' && normalizeHex(theme.surfaceHex) ? generateSurface(normalizeHex(theme.surfaceHex)) : {})
  });
  const applyCustomVars = (vars) => {
    CUSTOM_PROPS.forEach((p) => root.style.removeProperty(p));
    Object.entries(vars).forEach(([p, v]) => root.style.setProperty(p, v));
    if (!root.getAttribute('style')) root.removeAttribute('style');
  };

  /* ======================================================================
     Theme panel (prototype-only control)
     ====================================================================== */
  const PRIMARIES = ['emerald', 'green', 'lime', 'teal', 'cyan', 'sky', 'blue', 'indigo', 'violet', 'purple',
    'fuchsia', 'pink', 'rose', 'red', 'orange', 'amber', 'yellow'];
  const SURFACES = ['slate', 'gray', 'zinc', 'neutral', 'stone'];

  const readTheme = () => ({
    mode: root.classList.contains('dark') ? 'dark' : root.classList.contains('light') ? 'light' : 'system',
    primary: root.dataset.awePrimary || 'emerald',
    surface: root.dataset.aweSurface || 'auto',
    density: root.dataset.aweDensity || 'comfortable',
    brand: root.dataset.protoBrand || 'wide',
    loginImage: root.dataset.protoLoginImage || 'brand',
    primaryHex: root.dataset.protoPrimaryHex || '',
    surfaceHex: root.dataset.protoSurfaceHex || ''
  });
  const isLoginPage = document.body.dataset.page === 'login';

  const applyTheme = (theme) => {
    root.classList.remove('light', 'dark');
    if (theme.mode !== 'system') root.classList.add(theme.mode);
    root.dataset.awePrimary = theme.primary;
    if (theme.surface === 'auto') delete root.dataset.aweSurface;
    else root.dataset.aweSurface = theme.surface;
    root.dataset.aweDensity = theme.density;
    root.dataset.protoBrand = theme.brand;
    ['primaryHex', 'surfaceHex'].forEach((k) => {
      const key = `proto${k[0].toUpperCase()}${k.slice(1)}`;
      if (theme[k]) root.dataset[key] = theme[k];
      else delete root.dataset[key];
    });
    const vars = customVars(theme);
    applyCustomVars(vars);
    if (isLoginPage) root.dataset.protoLoginImage = theme.loginImage;
    try {
      // The generated custom properties are stored too, so each page applies them before its first paint
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...theme, vars }));
    } catch { /* storage unavailable (private mode) */ }
    renderThemeReadout();
    // Keep every control bound to the theme (panel radios, the login footer) in sync
    const values = { 'awe-mode': theme.mode, 'awe-primary': theme.primary, 'awe-surface': theme.surface,
      'awe-density': theme.density, 'proto-brand': theme.brand, 'proto-login-image': theme.loginImage };
    Object.entries(values).forEach(([name, value]) => $$(`.theme-panel input[name="${name}"]`).forEach((r) => {
      r.checked = r.value === value;
    }));
    document.dispatchEvent(new CustomEvent('awe-theme-change', { detail: theme }));
  };

  const renderThemeReadout = () => {
    const out = $('#theme-readout');
    if (!out) return;
    const attrs = [];
    if (root.className) attrs.push(`class="${root.className}"`);
    ['awePrimary', 'aweSurface', 'aweDensity', 'protoBrand', 'protoLoginImage', 'protoPrimaryHex', 'protoSurfaceHex'].forEach((k) => {
      if (root.dataset[k]) attrs.push(`data-${k.replace(/[A-Z]/g, (c) => '-' + c.toLowerCase())}="${root.dataset[k]}"`);
    });
    if (root.getAttribute('style')) attrs.push('style="--awe-primary-*… (generated)"');
    out.textContent = `<html ${attrs.join(' ')}>`;
  };

  const buildThemePanel = () => {
    const t = readTheme();
    const radio = (name, value, checked) => `<input type="radio" name="${name}" value="${value}"${checked ? ' checked' : ''}>`;
    // Custom colour: a swatch radio, the native colour picker and a hex field (validated), plus quick examples
    const customRow = (kind, checked, hex, examples) => `<div class="custom-color" data-custom="${kind}">
        <label class="swatch swatch--custom" title="Custom ${kind} colour">${radio(`awe-${kind}`, 'custom', checked)}
          <span style="--sw: var(--awe-${kind}-500)"></span><span class="sr-only">Custom</span></label>
        <span class="control control--sm custom-color__field">
          <input class="custom-color__picker" type="color" value="${esc(normalizeHex(hex) || examples[0][1].toLowerCase())}"
            aria-label="Pick a custom ${kind} colour">
          <input class="custom-color__hex mono" type="text" value="${esc(hex ? hex.toUpperCase() : '')}"
            placeholder="#RRGGBB" spellcheck="false" autocomplete="off" aria-label="Custom ${kind} hex"
            aria-describedby="custom-${kind}-err">
        </span>
        ${examples.map(([l, h]) => `<button class="btn btn--text btn--sm" type="button" data-hex="${h}"
          title="${h}">${l}</button>`).join('')}
      </div>
      <span class="error-msg" id="custom-${kind}-err" hidden>${icon('circle-alert')}Use #RGB or #RRGGBB</span>`;
    const panel = document.createElement('section');
    panel.className = 'theme-panel';
    panel.id = 'theme-panel';
    panel.setAttribute('aria-label', 'Theme settings');
    render(panel, `
      <div class="theme-panel__head">
        <strong>Theme</strong>
        <span class="tag tag--primary tag--plain" style="margin-right:auto;margin-left:8px">Prototype</span>
        <button class="btn btn--ghost btn--icon btn--sm" type="button" data-theme-close aria-label="Close theme panel">${icon('x')}</button>
      </div>
      <fieldset>
        <legend>Mode</legend>
        <div class="segmented segmented--block">
          ${[['light', 'sun', 'Light'], ['dark', 'moon', 'Dark'], ['system', 'monitor', 'System']].map(([v, ic, l]) =>
            `<label>${radio('awe-mode', v, t.mode === v)}<span>${icon(ic, 'i--sm')}${l}</span></label>`).join('')}
        </div>
      </fieldset>
      <fieldset>
        <legend>Primary</legend>
        <div class="swatches">
          ${PRIMARIES.map((p) => `<label class="swatch" title="${p}">${radio('awe-primary', p, t.primary === p)}
            <span style="--sw: var(--awe-palette-${p}-500)"></span><span class="sr-only">${p}</span></label>`).join('')}
        </div>
        ${customRow('primary', t.primary === 'custom', t.primaryHex, [['FIT', '#575C9C']])}
      </fieldset>
      <fieldset>
        <legend>Surface</legend>
        <div class="swatches swatches--surface">
          <label class="swatch swatch--auto" title="auto: slate in light, zinc in dark">${radio('awe-surface', 'auto', t.surface === 'auto')}
            <span></span><span class="sr-only">Auto</span></label>
          ${SURFACES.map((s) => `<label class="swatch" title="${s}">${radio('awe-surface', s, t.surface === s)}
            <span style="--sw: var(--awe-palette-${s}-500)"></span><span class="sr-only">${s}</span></label>`).join('')}
        </div>
        ${customRow('surface', t.surface === 'custom', t.surfaceHex, [['Warm', '#F4F1EA']])}
      </fieldset>
      <fieldset>
        <legend>Density</legend>
        <div class="segmented segmented--block">
          ${[['comfortable', 'Comfortable · 14px'], ['compact', 'Compact · 13px']].map(([v, l]) =>
            `<label>${radio('awe-density', v, t.density === v)}<span>${l}</span></label>`).join('')}
        </div>
      </fieldset>
      <fieldset>
        <legend>Customer logo <span class="muted">(prototype comparison)</span></legend>
        <div class="segmented segmented--block">
          ${[['wide', 'Wide logo'], ['mark', 'Mark + name'], ['none', 'AWE logo']].map(([v, l]) =>
            `<label>${radio('proto-brand', v, t.brand === v)}<span>${l}</span></label>`).join('')}
        </div>
      </fieldset>
      ${isLoginPage ? `<fieldset>
        <legend>Login image <span class="muted">(prototype comparison)</span></legend>
        <div class="segmented segmented--block">
          ${[['brand', 'AWE brand'], ['gradient', 'Gradient'], ['none', 'None']].map(([v, l]) =>
            `<label>${radio('proto-login-image', v, t.loginImage === v)}<span>${l}</span></label>`).join('')}
        </div>
      </fieldset>` : ''}
      <p class="theme-panel__note">Only &lt;html&gt; changes:<br><code id="theme-readout"></code></p>`);
    document.body.appendChild(panel);

    const fab = document.createElement('button');
    fab.className = 'btn btn--primary btn--rounded theme-fab';
    fab.type = 'button';
    fab.setAttribute('aria-controls', 'theme-panel');
    fab.setAttribute('aria-expanded', 'false');
    render(fab, `${icon('palette')}Theme`);
    document.body.appendChild(fab);

    const setOpen = (open) => {
      panel.classList.toggle('is-open', open);
      fab.setAttribute('aria-expanded', String(open));
    };
    fab.addEventListener('click', () => setOpen(!panel.classList.contains('is-open')));
    $('[data-theme-close]', panel).addEventListener('click', () => {
      setOpen(false);
      fab.focus();
    });
    const applyCustom = (kind, value) => {
      const row = $(`[data-custom="${kind}"]`, panel);
      const hex = normalizeHex(value);
      const field = $('.custom-color__hex', row);
      field.closest('.control').classList.toggle('is-invalid', !hex);
      field.setAttribute('aria-invalid', String(!hex));
      document.getElementById(`custom-${kind}-err`).hidden = !!hex;
      if (!hex) return;
      field.value = hex.toUpperCase();
      $('.custom-color__picker', row).value = hex;
      applyTheme({ ...readTheme(), [kind]: 'custom', [`${kind}Hex`]: hex });
    };
    $$('[data-custom]', panel).forEach((row) => {
      const kind = row.dataset.custom;
      $('.custom-color__picker', row).addEventListener('input', (e) => applyCustom(kind, e.target.value));
      $('.custom-color__hex', row).addEventListener('keydown', (e) => {
        if (e.key === 'Enter') applyCustom(kind, e.target.value);
      });
      $$('[data-hex]', row).forEach((b) => b.addEventListener('click', () => applyCustom(kind, b.dataset.hex)));
    });
    panel.addEventListener('change', (e) => {
      const row = e.target.closest('[data-custom]');
      if (row && e.target.type !== 'radio') {
        if (e.target.classList.contains('custom-color__hex')) applyCustom(row.dataset.custom, e.target.value);
        return;
      }
      if (e.target.value === 'custom' && row) {
        applyCustom(row.dataset.custom, $('.custom-color__hex', row).value || $('.custom-color__picker', row).value);
        return;
      }
      const t2 = readTheme();
      const { name, value } = e.target;
      if (name === 'awe-mode') t2.mode = value;
      if (name === 'awe-primary') t2.primary = value;
      if (name === 'awe-surface') t2.surface = value;
      if (name === 'awe-density') t2.density = value;
      if (name === 'proto-brand') t2.brand = value;
      if (name === 'proto-login-image') t2.loginImage = value;
      applyTheme(t2);
    });
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && panel.classList.contains('is-open')) setOpen(false);
    });
    renderThemeReadout();
  };

  buildThemePanel();
  applyTheme(readTheme());
  window.AweProto = Object.assign(window.AweProto || {}, {
    setThemeMode: (mode) => applyTheme({ ...readTheme(), mode }),
    palette: { normalizeHex, hexToOklch, oklchToHex, contrast, generatePrimary, generateSurface, customVars, applyCustomVars }
  });

  /* ======================================================================
     Listbox popup (Aura list style) for selects, multi-selects and autocompletes.
     The native <select> stays in the DOM (hidden) as the value holder; the visible control is a combobox button with
     aria-activedescendant on a role="listbox" popover, so the popup is in the top layer and never clipped.
     ====================================================================== */
  let listboxSeq = 0;
  const TYPEAHEAD_MS = 600;

  // items: [{ value, label, meta?, group?, disabled? }]; host: element the popup aligns to
  const createListbox = ({ host, trigger, items, multiple = false, filter = false, label, isSelected, onPick }) => {
    const id = `lb-${++listboxSeq}`;
    const pop = document.createElement('div');
    pop.className = 'listbox';
    pop.id = id;
    pop.setAttribute('popover', 'manual');
    (host.closest('dialog') || document.body).appendChild(pop);

    let active = -1;
    let query = '';
    let buffer = '';
    let bufferTimer = null;
    const visible = () => items.filter((it) => !query || it.label.toLowerCase().includes(query));

    const draw = () => {
      const list = visible();
      let lastGroup = null;
      let groupSeq = 0;
      const parts = [];
      list.forEach((it, i) => {
        if (it.group && it.group !== lastGroup) {
          if (lastGroup !== null) parts.push('</div>');
          lastGroup = it.group;
          groupSeq++;
          parts.push(`<div role="group" aria-labelledby="${id}-g${groupSeq}">
            <div class="overlay__head" id="${id}-g${groupSeq}">${esc(it.group)}</div>`);
        }
        const sel = isSelected(it);
        const box = multiple ? `<span class="check__box">${icon('check', 'i--check')}</span>` : '';
        const meta = it.meta ? `<span class="option__meta">${esc(it.meta)}</span>` : '';
        parts.push(`<div class="option${i === active ? ' is-focused' : ''}" role="option" id="${id}-o${i}"
          aria-selected="${sel}"${it.disabled ? ' aria-disabled="true"' : ''} data-i="${i}">${box}
          <span class="option__text"><span>${esc(it.label)}</span>${meta}</span>
          ${multiple ? '' : icon('check', 'check-mark')}</div>`);
      });
      if (lastGroup !== null) parts.push('</div>');
      if (!list.length) parts.push('<div class="listbox__empty">No results</div>');
      render($('.listbox__options', pop), parts.join(''));
      const focusTarget = filter ? $('.listbox__filter input', pop) : trigger;
      if (active >= 0 && pop.matches(':popover-open')) {
        focusTarget.setAttribute('aria-activedescendant', `${id}-o${active}`);
        $(`#${id}-o${active}`, pop)?.scrollIntoView({ block: 'nearest' });
      } else {
        focusTarget.removeAttribute('aria-activedescendant');
      }
    };

    render(pop, `${filter ? `<div class="listbox__filter"><label class="control control--sm">${icon('search', 'i--sm')}
        <input type="text" role="combobox" aria-expanded="true" aria-controls="${id}-list" aria-autocomplete="list"
          aria-label="Filter ${esc(label)}" placeholder="Filter…" autocomplete="off"></label></div>` : ''}
      <div class="listbox__options" role="listbox" id="${id}-list" aria-label="${esc(label)}"
        ${multiple ? 'aria-multiselectable="true"' : ''}></div>`);
    trigger.setAttribute('aria-controls', `${id}-list`);
    trigger.setAttribute('aria-haspopup', 'listbox');
    trigger.setAttribute('aria-expanded', 'false');

    const place = () => {
      const r = host.getBoundingClientRect();
      pop.style.minWidth = `${r.width}px`;
      pop.style.left = `${Math.min(r.left, window.innerWidth - pop.offsetWidth - 8)}px`;
      const below = window.innerHeight - r.bottom;
      pop.style.top = below < pop.offsetHeight + 12 && r.top > below
        ? `${r.top - pop.offsetHeight - 4}px` : `${r.bottom + 4}px`;
    };

    const isOpen = () => pop.matches(':popover-open');
    const open = () => {
      if (isOpen()) return;
      document.dispatchEvent(new CustomEvent('awe-listbox-open', { detail: pop }));
      query = '';
      const list = visible();
      active = Math.max(0, list.findIndex((it) => isSelected(it)));
      if (filter) $('.listbox__filter input', pop).value = '';
      pop.showPopover();
      draw();
      place();
      trigger.setAttribute('aria-expanded', 'true');
      host.classList.add('is-listbox-open');
      if (filter) $('.listbox__filter input', pop).focus();
    };
    const close = (refocus) => {
      if (!isOpen()) return;
      pop.hidePopover();
      trigger.setAttribute('aria-expanded', 'false');
      trigger.removeAttribute('aria-activedescendant');
      host.classList.remove('is-listbox-open');
      if (refocus) trigger.focus();
    };
    const pick = (i) => {
      const it = visible()[i];
      if (!it || it.disabled) return;
      onPick(it);
      if (multiple) {
        active = i;
        draw();
      } else {
        close(true);
      }
    };
    const move = (delta) => {
      const list = visible();
      if (!list.length) return;
      let i = active;
      do {
        i = Math.min(list.length - 1, Math.max(0, i + delta));
      } while (list[i].disabled && i > 0 && i < list.length - 1);
      active = i;
      draw();
    };
    const typeahead = (ch) => {
      clearTimeout(bufferTimer);
      buffer += ch.toLowerCase();
      bufferTimer = setTimeout(() => {
        buffer = '';
      }, TYPEAHEAD_MS);
      const list = visible();
      const start = buffer.length === 1 ? active + 1 : Math.max(active, 0);
      const order = [...list.keys()].map((k) => (k + start) % list.length);
      const hit = order.find((k) => list[k].label.toLowerCase().startsWith(buffer) && !list[k].disabled);
      if (hit === undefined) return;
      if (!isOpen() && !multiple) {
        active = hit;
        onPick(list[hit]);
      } else {
        active = hit;
        draw();
      }
    };

    const onKey = (e) => {
      const fromFilter = e.target.closest('.listbox__filter');
      switch (e.key) {
        case 'ArrowDown':
        case 'ArrowUp':
          e.preventDefault();
          if (!isOpen()) open();
          else move(e.key === 'ArrowDown' ? 1 : -1);
          break;
        case 'Home':
        case 'End':
          if (isOpen() && !fromFilter) {
            e.preventDefault();
            active = e.key === 'Home' ? 0 : visible().length - 1;
            draw();
          }
          break;
        case 'Enter':
          e.preventDefault();
          if (isOpen()) pick(active);
          else open();
          break;
        case ' ':
          if (fromFilter) break;
          e.preventDefault();
          if (isOpen()) pick(active);
          else open();
          break;
        case 'Escape':
          if (isOpen()) {
            e.preventDefault();
            e.stopPropagation();
            close(true);
          }
          break;
        case 'Tab':
          close(false);
          break;
        default:
          if (!fromFilter && e.key.length === 1 && !e.ctrlKey && !e.metaKey && !e.altKey) typeahead(e.key);
      }
    };

    trigger.addEventListener('click', (e) => {
      if (e.target.closest('.chip button')) return;
      if (isOpen()) close(false);
      else open();
    });
    trigger.addEventListener('keydown', onKey);
    pop.addEventListener('keydown', onKey);
    pop.addEventListener('mousedown', (e) => {
      if (!e.target.closest('input')) e.preventDefault(); // keep the focus on the combobox
    });
    pop.addEventListener('click', (e) => {
      const opt = e.target.closest('.option');
      if (opt) pick(Number(opt.dataset.i));
    });
    pop.addEventListener('mousemove', (e) => {
      const opt = e.target.closest('.option');
      if (opt && Number(opt.dataset.i) !== active) {
        active = Number(opt.dataset.i);
        $$('.option', pop).forEach((o) => o.classList.toggle('is-focused', o === opt));
      }
    });
    if (filter) {
      $('.listbox__filter input', pop).addEventListener('input', (e) => {
        query = e.target.value.trim().toLowerCase();
        active = 0;
        draw();
        place();
      });
    }
    document.addEventListener('mousedown', (e) => {
      if (isOpen() && !pop.contains(e.target) && !host.contains(e.target)) close(false);
    });
    document.addEventListener('awe-listbox-open', (e) => {
      if (e.detail !== pop) close(false);
    });
    window.addEventListener('resize', () => close(false));
    document.addEventListener('scroll', (e) => {
      if (isOpen() && !pop.contains(e.target)) place();
    }, true);
    return { open, close, redraw: draw };
  };

  // Native <select> -> combobox + listbox
  const enhanceSelects = (scope = document) => {
    $$('.control > select', scope).forEach((select) => {
      if (select.dataset.enhanced) return;
      select.dataset.enhanced = 'true';
      const control = select.parentElement;
      const trigger = document.createElement('button');
      trigger.type = 'button';
      trigger.className = 'select-trigger';
      trigger.setAttribute('role', 'combobox');
      if (select.id) {
        trigger.id = select.id;
        select.id = `${select.id}-native`;
      }
      const labelEl = trigger.id ? document.querySelector(`label[for="${trigger.id}"]`) : null;
      const label = select.getAttribute('aria-label') || labelEl?.textContent.replace('*', '').trim() || 'Options';
      if (!labelEl) trigger.setAttribute('aria-label', label);
      select.hidden = true;
      select.tabIndex = -1;
      select.before(trigger);

      const items = Array.from(select.options).map((o) => ({
        value: o.value, label: o.textContent, group: o.parentElement.tagName === 'OPTGROUP' ? o.parentElement.label : null,
        meta: o.dataset.meta, disabled: o.disabled
      }));
      const sync = () => {
        const cur = items.find((it) => it.value === select.value);
        render(trigger, `<span class="select-value${cur && cur.value === '' ? ' is-placeholder' : ''}">${esc(cur ? cur.label : '')}</span>`);
      };
      sync();
      createListbox({
        host: control, trigger, items, label,
        filter: select.dataset.filter !== undefined || items.length > 10,
        isSelected: (it) => it.value === select.value,
        onPick: (it) => {
          select.value = it.value;
          select.dispatchEvent(new Event('change', { bubbles: true }));
          sync();
        }
      });
    });
  };
  // Multi-select with chips: the trigger keeps the chips, the popup lists every option with a checkbox
  const enhanceMultiSelects = (scope = document) => {
    $$('[data-multiselect]', scope).forEach((control) => {
      if (control.dataset.enhanced) return;
      control.dataset.enhanced = 'true';
      const items = control.dataset.multiselect.split(',').map((v) => ({ value: v, label: v }));
      const selected = new Set((control.dataset.selected || '').split(',').filter(Boolean));
      const labelId = control.dataset.labelId;
      const label = document.getElementById(labelId)?.textContent || 'Options';
      control.setAttribute('role', 'group');
      control.setAttribute('aria-labelledby', labelId);
      render(control, `<span class="chips"></span>
        <button type="button" class="select-trigger chips-trigger" role="combobox" aria-labelledby="${esc(labelId)}">
          <span class="select-value is-placeholder">Add…</span></button>
        ${icon('chevron-down', 'control__icon')}`);
      const trigger = $('.chips-trigger', control);
      const drawChips = () => {
        render($('.chips', control), [...selected].map((v) => `<span class="chip">${esc(v)}<button type="button"
          data-remove="${esc(v)}" aria-label="Remove ${esc(v)}">${icon('x')}</button></span>`).join(''));
        render($('.select-value', trigger), selected.size ? `${selected.size} of ${items.length}` : `Select ${esc(label.toLowerCase())}…`);
      };
      const lb = createListbox({
        host: control, trigger, items, label, multiple: true, filter: true,
        isSelected: (it) => selected.has(it.value),
        onPick: (it) => {
          if (selected.has(it.value)) selected.delete(it.value);
          else selected.add(it.value);
          drawChips();
        }
      });
      control.addEventListener('click', (e) => {
        const rm = e.target.closest('[data-remove]');
        if (rm) {
          selected.delete(rm.dataset.remove);
          drawChips();
          lb.redraw();
          trigger.focus();
        } else if (!e.target.closest('.chips-trigger')) {
          trigger.click();
        }
      });
      drawChips();
    });
  };
  window.AweProto = Object.assign(window.AweProto || {}, { createListbox, enhanceSelects, enhanceMultiSelects });

  /* ======================================================================
     Dropdowns, autocomplete, date picker
     ====================================================================== */
  const closeAllDropdowns = (except) => {
    $$('.dropdown.is-open').forEach((d) => {
      if (d !== except) {
        d.classList.remove('is-open');
        $('[data-dropdown], [aria-expanded]', d)?.setAttribute('aria-expanded', 'false');
      }
    });
  };

  $$('[data-dropdown]').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const dd = btn.closest('.dropdown');
      const open = !dd.classList.contains('is-open');
      closeAllDropdowns(dd);
      dd.classList.toggle('is-open', open);
      btn.setAttribute('aria-expanded', String(open));
    });
  });
  document.addEventListener('click', (e) => {
    if (!e.target.closest('.dropdown')) closeAllDropdowns();
  });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeAllDropdowns();
  });

  const combo = $('#combo-manager');
  if (combo) {
    const input = $('input', combo);
    input.addEventListener('focus', () => {
      closeAllDropdowns(combo);
      combo.classList.add('is-open');
      input.setAttribute('aria-expanded', 'true');
    });
    $$('.option', combo).forEach((opt) => {
      opt.addEventListener('click', () => {
        input.value = $('.option__text > span', opt).textContent;
        $$('.option', combo).forEach((o) => o.setAttribute('aria-selected', String(o === opt)));
        combo.classList.remove('is-open');
        input.setAttribute('aria-expanded', 'false');
      });
    });
  }

  const dpGrid = $('#dp-grid');
  if (dpGrid) {
    // October 2026 starts on a Thursday; weeks start on Monday
    const days = ['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'Su'].map((d) => `<span class="datepicker__dow">${d}</span>`);
    const cells = [];
    for (let d = 28; d <= 30; d++) cells.push(`<button type="button" class="datepicker__day is-outside" tabindex="-1">${d}</button>`);
    for (let d = 1; d <= 31; d++) {
      const cls = d === 23 ? ' is-today' : '';
      const sel = d === 14 ? ' aria-selected="true"' : '';
      cells.push(`<button type="button" class="datepicker__day${cls}"${sel}>${d}</button>`);
    }
    for (let d = 1; cells.length < 42; d++) cells.push(`<button type="button" class="datepicker__day is-outside" tabindex="-1">${d}</button>`);
    render(dpGrid, days.join('') + cells.join(''));
    const dp = $('#dp');
    const toggle = $('#dp-toggle');
    toggle.addEventListener('click', (e) => {
      e.stopPropagation();
      const open = !dp.classList.contains('is-open');
      closeAllDropdowns(dp);
      dp.classList.toggle('is-open', open);
      toggle.setAttribute('aria-expanded', String(open));
    });
    dpGrid.addEventListener('click', (e) => {
      const day = e.target.closest('.datepicker__day:not(.is-outside)');
      if (!day) return;
      $$('.datepicker__day', dpGrid).forEach((b) => b.removeAttribute('aria-selected'));
      day.setAttribute('aria-selected', 'true');
      $('#f-created').value = `${day.textContent.padStart(2, '0')}/10/2026`;
      dp.classList.remove('is-open');
    });
  }

  $$('[data-toggle-password]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const input = document.getElementById(btn.dataset.togglePassword);
      const show = input.type === 'password';
      input.type = show ? 'text' : 'password';
      btn.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
      $('use', btn).setAttribute('href', `${SPRITE}#${show ? 'eye-off' : 'eye'}`);
    });
  });

  $$('.chip button').forEach((btn) => btn.addEventListener('click', () => btn.closest('.chip').remove()));
  $$('.filter-chip button').forEach((btn) => btn.addEventListener('click', () => btn.closest('.filter-chip').remove()));

  $$('.collapse-toggle').forEach((btn) => {
    btn.addEventListener('click', () => {
      const body = document.getElementById(btn.getAttribute('aria-controls'));
      const open = btn.getAttribute('aria-expanded') !== 'true';
      btn.setAttribute('aria-expanded', String(open));
      btn.setAttribute('aria-label', open ? 'Collapse search criteria' : 'Expand search criteria');
      body.hidden = !open;
    });
  });

  const dropzone = $('#dropzone');
  if (dropzone) {
    ['dragenter', 'dragover'].forEach((ev) => dropzone.addEventListener(ev, (e) => {
      e.preventDefault();
      dropzone.classList.add('is-dragover');
    }));
    ['dragleave', 'drop'].forEach((ev) => dropzone.addEventListener(ev, (e) => {
      e.preventDefault();
      dropzone.classList.remove('is-dragover');
    }));
  }

  /* ======================================================================
     Tabs
     ====================================================================== */
  $$('[data-tabs]').forEach((tabs) => {
    const list = $$('[role="tab"]', tabs);
    const select = (tab) => {
      list.forEach((t) => {
        const on = t === tab;
        t.setAttribute('aria-selected', String(on));
        t.tabIndex = on ? 0 : -1;
        document.getElementById(t.getAttribute('aria-controls')).hidden = !on;
      });
      tab.focus();
    };
    list.forEach((tab, i) => {
      tab.addEventListener('click', () => select(tab));
      tab.addEventListener('keydown', (e) => {
        if (e.key === 'ArrowRight') select(list[(i + 1) % list.length]);
        if (e.key === 'ArrowLeft') select(list[(i - 1 + list.length) % list.length]);
      });
    });
  });

  /* ======================================================================
     Data grid
     ====================================================================== */
  const CUSTOMERS = [
    ['CUS-10482', 'Acme Industrial Supplies', 'buy@acme.example', 'AI', '', 'Germany', 'Enterprise', 'success', 'Active', '148', '82,450.00', '18 Oct 2026'],
    ['CUS-10477', 'Brightline Logistics SL', 'ap@brightline.example', 'BL', 'info', 'Spain', 'Mid-market', 'success', 'Active', '64', '12,980.50', '21 Oct 2026', 'selected'],
    ['CUS-10471', 'Nordvik Marine AS', 'ap@nordvik.example', 'NM', 'warning', 'Norway', 'Enterprise', 'warning', 'On hold', '37', '45,120.00', '02 Oct 2026'],
    ['CUS-10466', 'Cobalt Health Partners', 'ap@cobalt.example', 'CH', 'neutral', 'United Kingdom', 'Mid-market', 'success', 'Active', '29', '7,340.25', '20 Oct 2026', 'editing'],
    ['CUS-10459', 'Lumen Retail Group', 'ap@lumen.example', 'LR', 'danger', 'France', 'SMB', 'success', 'Active', '212', '3,215.90', '22 Oct 2026', 'selected'],
    ['CUS-10452', 'Ferro & Sons Construction', 'admin@ferro.example', 'FS', 'warning', 'Italy', 'SMB', 'danger', 'Blocked', '12', '−1,250.00', '14 Aug 2026'],
    ['CUS-10448', 'Helios Energy BV', 'ap@helios.example', 'HE', 'success', 'Netherlands', 'Enterprise', 'success', 'Active', '96', '128,900.00', '19 Oct 2026'],
    ['CUS-10441', 'Quantum Analytics Inc.', 'ops@quantum.example', 'QA', 'info', 'United States', 'Mid-market', 'info', 'Prospect', '0', '0.00', '—'],
    ['CUS-10437', 'Sakura Foods KK', 'keiri@sakura.example', 'SF', 'danger', 'Japan', 'SMB', 'success', 'Active', '41', '9,870.40', '11 Oct 2026'],
    ['CUS-10430', 'Atlas Pharma GmbH', 'ap@atlas.example', 'AP', 'neutral', 'Switzerland', 'Enterprise', '', 'In review', '58', '61,005.75', '07 Oct 2026']
  ];

  const checkbox = (label, checked) => `<label class="check"><input type="checkbox" aria-label="${label}"${checked ? ' checked' : ''}>
    <span class="check__box">${icon('check', 'i--check')}${icon('minus', 'i--minus')}</span></label>`;

  const rowHtml = (row) => {
    const [code, name, email, initials, avatar, country, segment, tone, status, orders, balance, last, state] = row.map(esc);
    if (state === 'editing') {
      return `<tr class="is-editing">
        <td class="col-check">${checkbox(`Select ${name}`, false)}</td>
        <td class="mono muted-cell">${code}</td>
        <td><span class="control control--sm"><input type="text" value="${name}" aria-label="Customer"></span></td>
        <td><span class="control control--sm" style="min-width: 150px"><select aria-label="Country"><option selected>${country}</option><option>Ireland</option></select>${icon('chevron-down', 'i--sm select-chev')}</span></td>
        <td><span class="tag tag--${tone}">${status}</span></td>
        <td class="num">${orders}</td>
        <td class="num"><span class="control control--sm is-invalid" style="width: 120px; margin-left: auto"><input class="num" type="text" value="${balance}" aria-label="Balance" aria-invalid="true"></span></td>
        <td class="muted-cell">${last}</td>
        <td class="col-actions"><span class="row-actions">
          <button class="btn btn--primary btn--icon btn--sm" type="button" aria-label="Save row" data-tooltip="Save">${icon('check')}</button>
          <button class="btn btn--ghost btn--icon btn--sm" type="button" aria-label="Cancel editing" data-tooltip="Cancel">${icon('x')}</button>
        </span></td></tr>`;
    }
    const selected = state === 'selected';
    const negative = balance.startsWith('−') ? ' negative' : '';
    return `<tr${selected ? ' class="is-selected"' : ''}>
      <td class="col-check">${checkbox(`Select ${name}`, selected)}</td>
      <td class="mono muted-cell">${code}</td>
      <td><div class="cell-entity"><span class="avatar avatar--sm ${avatar ? 'avatar--' + avatar : ''}">${initials}</span>
        <div class="cell-entity__text"><span class="cell-entity__name">${name}</span><span class="cell-entity__meta">${email}</span></div></div></td>
      <td>${country}</td>
      <td><span class="tag ${tone ? 'tag--' + tone : ''}">${status}</span></td>
      <td class="num">${orders}</td>
      <td class="num${negative}">${balance}</td>
      <td class="muted-cell">${last}</td>
      <td class="col-actions"><span class="row-actions">
        <button class="btn btn--ghost btn--icon btn--sm" type="button" aria-label="Edit ${name}" data-tooltip="Edit">${icon('pencil')}</button>
        <button class="btn btn--ghost btn--icon btn--sm" type="button" aria-label="More actions for ${name}" data-tooltip="More">${icon('ellipsis')}</button>
      </span></td></tr>`;
  };

  const gridBody = $('#grid-body-data');
  if (gridBody) {
    render(gridBody, CUSTOMERS.map(rowHtml).join(''));
    const skeletonRow = (w) => `<tr>
      <td class="col-check"><span class="skeleton skeleton--box"></span></td>
      <td><span class="skeleton" style="width: 72px"></span></td>
      <td><div class="cell-entity"><span class="skeleton skeleton--circle"></span><div class="skeleton-stack">
        <span class="skeleton" style="width: ${w}px"></span><span class="skeleton" style="width: ${w + 40}px; height: 8px"></span></div></div></td>
      <td><span class="skeleton" style="width: 80px"></span></td>
      <td><span class="skeleton" style="width: 56px; height: 20px"></span></td>
      <td class="num"><span class="skeleton" style="width: 28px; margin-left: auto"></span></td>
      <td class="num"><span class="skeleton" style="width: 80px; margin-left: auto"></span></td>
      <td><span class="skeleton" style="width: 76px"></span></td>
      <td class="col-actions"><span class="skeleton" style="width: 56px"></span></td></tr>`;
    render($('#grid-body-loading'), [140, 110, 160, 120, 150, 100].map(skeletonRow).join(''));

    const updateSelection = () => {
      const boxes = $$('td.col-check input', gridBody);
      const count = boxes.filter((b) => b.checked).length;
      boxes.forEach((b) => {
        const tr = b.closest('tr');
        if (!tr.classList.contains('is-editing')) tr.classList.toggle('is-selected', b.checked);
      });
      const all = $('#select-all');
      all.checked = count === boxes.length;
      all.indeterminate = count > 0 && count < boxes.length;
      $('#selection-bar').classList.toggle('is-visible', count > 0);
      $('#selection-count').textContent = `${count} selected`;
    };
    gridBody.addEventListener('change', updateSelection);
    $('#select-all').addEventListener('change', (e) => {
      $$('td.col-check input', gridBody).forEach((b) => {
        b.checked = e.target.checked;
      });
      updateSelection();
    });
    updateSelection();

    // Sortable headers (visual only)
    $$('#customers-grid th .sort').forEach((btn) => {
      btn.addEventListener('click', () => {
        const th = btn.closest('th');
        const next = th.getAttribute('aria-sort') === 'ascending' ? 'descending' : 'ascending';
        $$('#customers-grid th[aria-sort]').forEach((h) => {
          h.removeAttribute('aria-sort');
          $('use', h).setAttribute('href', `${SPRITE}#arrow-up-down`);
        });
        th.setAttribute('aria-sort', next);
        $('use', btn).setAttribute('href', `${SPRITE}#${next === 'ascending' ? 'arrow-up' : 'arrow-down'}`);
      });
    });

    // Preview state switcher: data / loading / empty
    $$('input[name="grid-state"]').forEach((r) => r.addEventListener('change', () => {
      const state = $('input[name="grid-state"]:checked').value;
      $('#grid-body-data').hidden = state !== 'data';
      $('#grid-body-loading').hidden = state !== 'loading';
      $('#grid-body-empty').hidden = state !== 'empty';
      $('#grid-loading-bar').hidden = state !== 'loading';
    }));
  }

  /* ======================================================================
     Tree grid
     ====================================================================== */
  const tree = $('#tree-grid');
  if (tree) {
    const descendants = (id) => $$(`tr[data-parent="${id}"]`, tree)
      .flatMap((tr) => [tr, ...(tr.dataset.id ? descendants(tr.dataset.id) : [])]);
    const setExpanded = (btn, open) => {
      const tr = btn.closest('tr');
      btn.setAttribute('aria-expanded', String(open));
      const label = btn.getAttribute('aria-label').replace(/^(Expand|Collapse)/, open ? 'Collapse' : 'Expand');
      btn.setAttribute('aria-label', label);
      $('.tree-icon use', tr).setAttribute('href', `${SPRITE}#${open ? 'folder-open' : 'folder'}`);
      $$(`tr[data-parent="${tr.dataset.id}"]`, tree).forEach((child) => {
        child.hidden = !open;
        const childBtn = $('.tree-toggle', child);
        if (!open) descendants(tr.dataset.id).forEach((d) => { d.hidden = true; });
        else if (childBtn && childBtn.getAttribute('aria-expanded') === 'true') {
          descendants(child.dataset.id).forEach((d) => { d.hidden = false; });
        }
      });
    };
    $$('.tree-toggle', tree).forEach((btn) => btn.addEventListener('click', () =>
      setExpanded(btn, btn.getAttribute('aria-expanded') !== 'true')));
    $('#tree-expand-all')?.addEventListener('click', () => $$('.tree-toggle', tree).forEach((b) => setExpanded(b, true)));
  }

  /* ======================================================================
     Dialogs and toasts
     ====================================================================== */
  const TOASTS = {
    success: ['circle-check', 'Customer saved', 'Brightline Logistics SL was updated.'],
    info: ['info', 'Export started', 'We will notify you when customers-2026-10.xlsx is ready.'],
    warning: ['triangle-alert', 'Credit limit almost reached', 'Nordvik Marine AS is at 92% of its credit limit.'],
    danger: ['circle-x', 'Could not send statement', 'The mail server rejected 1 recipient. Try again later.'],
    'danger-delete': ['trash-2', '2 customers deleted', 'Orders and invoices were kept.']
  };

  const showToast = (kind) => {
    const [ic, title, detail] = TOASTS[kind];
    const tone = kind === 'danger-delete' ? 'success' : kind;
    const el = document.createElement('div');
    el.className = `toast toast--${tone}`;
    el.setAttribute('role', kind.startsWith('danger') && tone === 'danger' ? 'alert' : 'status');
    render(el, `${icon(ic)}<div class="toast__body"><p class="toast__title">${esc(title)}</p>
      <p class="toast__detail">${esc(detail)}</p></div>
      <button class="btn btn--ghost btn--icon btn--sm" type="button" aria-label="Dismiss">${icon('x')}</button>`);
    const remove = () => {
      el.classList.add('is-leaving');
      setTimeout(() => el.remove(), 220);
    };
    $('button', el).addEventListener('click', remove);
    const stack = $('#toasts');
    // The stack is a manual popover: it lives in the top layer, above modal dialogs and their backdrop
    if (stack.showPopover && !stack.matches(':popover-open')) stack.showPopover();
    stack.appendChild(el);
    setTimeout(remove, 6000);
  };

  $$('[data-toast]').forEach((b) => b.addEventListener('click', () => showToast(b.dataset.toast)));

  $$('[data-open-dialog]').forEach((b) => b.addEventListener('click', () => {
    closeAllDropdowns();
    document.getElementById(b.dataset.openDialog).showModal();
  }));
  $$('dialog.modal').forEach((dlg) => {
    $$('[data-close]', dlg).forEach((b) => b.addEventListener('click', () => {
      dlg.close();
      if (b.dataset.toastAfter) showToast(b.dataset.toastAfter);
    }));
    dlg.addEventListener('click', (e) => {
      if (e.target === dlg) dlg.close();
    });
  });

  /* ======================================================================
     tokens.html: everything is read from the rules of tokens.css
     ====================================================================== */
  const tokensPage = $('#tokens-page');
  if (tokensPage) {
    const sheet = Array.from(document.styleSheets).find((s) => (s.href || '').endsWith('tokens.css'));
    const base = new Map();
    const compact = new Map();
    Array.from(sheet.cssRules).forEach((rule) => {
      if (!(rule instanceof CSSStyleRule)) return;
      const target = rule.selectorText.includes('data-awe-density="compact"') ? compact
        : rule.selectorText === ':root' || rule.selectorText.startsWith(':root, :root[data-awe-primary="emerald"]') ? base
          : null;
      if (!target) return;
      for (let i = 0; i < rule.style.length; i++) {
        const name = rule.style[i];
        if (name.startsWith('--awe-') && !target.has(name)) target.set(name, rule.style.getPropertyValue(name).trim());
      }
    });
    const tierOf = (name) => {
      if (name.startsWith('--awe-palette-')) return 'Tier 1 · primitive';
      if (/^--awe-(primary|surface)-/.test(name)) return 'Tier 1 · configurable';
      if (name.startsWith('--awe-customer-logo-')) return 'Prototype only';
      if (/^--awe-(grid|menu|header|segmented|login)-/.test(name)) return 'Tier 3 · component';
      return 'Tier 2 · semantic';
    };
    const names = Array.from(base.keys());
    const counts = names.reduce((acc, n) => ({ ...acc, [tierOf(n)]: (acc[tierOf(n)] || 0) + 1 }), {});
    const semanticColors = names.filter((n) => n.startsWith('--awe-color-'));
    counts['of which colour (light + dark)'] = semanticColors.length;
    render($('#tier-counts'), Object.entries(counts).map(([k, v]) => `<div class="card kpi">
      <span class="kpi__label">${esc(k)}</span><span class="kpi__value">${v}</span></div>`).join(''));

    const SHADES = ['50', '100', '200', '300', '400', '500', '600', '700', '800', '900', '950'];
    const chip = (token, label, dark) => `<span class="scale__chip" style="background: var(${token}); color: var(${dark})">
      <b>${esc(label)}</b></span>`;
    const renderScales = () => {
      const p = root.dataset.awePrimary || 'emerald';
      $('#primary-name').textContent = `(${p === 'custom' ? `generated from ${root.dataset.protoPrimaryHex}` : p})`;
      const surfaceName = $('#surface-name');
      if (surfaceName) {
        surfaceName.textContent = root.dataset.aweSurface === 'custom'
          ? `(generated from ${root.dataset.protoSurfaceHex}: one scale for both modes)`
          : '(slate in light and zinc in dark unless data-awe-surface is set)';
      }
      render($('#scale-primary'), SHADES.map((s) => chip(`--awe-primary-${s}`, s,
        Number(s) >= 500 ? '--awe-primary-50' : '--awe-primary-900')).join(''));
      const surface = ['0', ...SHADES].map((s) => chip(`--awe-surface-${s}`, s,
        Number(s) >= 500 ? '--awe-surface-50' : '--awe-surface-900')).join('');
      render($('#scale-surface-light'), surface);
      render($('#scale-surface-dark'), surface);
    };
    renderScales();
    new MutationObserver(renderScales).observe(root, { attributes: true });

    render($('#semantic-colors'), semanticColors.map((n) => `<tr>
      <td class="mono">${esc(n)}</td>
      <td class="sw-col light"><span class="sw" style="background: var(${n})"></span></td>
      <td class="sw-col dark"><span class="sw" style="background: var(${n})"></span></td>
      <td class="token-def">${esc(base.get(n))}</td></tr>`).join(''));

    const textPane = `<p style="color: var(--awe-color-text-emphasis); font-weight: 600">Text emphasis · headings</p>
      <p style="color: var(--awe-color-text)">Text · body copy and cell values</p>
      <p style="color: var(--awe-color-text-muted)">Text muted · captions and help</p>
      <p style="color: var(--awe-color-primary-text)">Primary text · links and active tab</p>
      <p><span class="tag tag--success">Success</span> <span class="tag tag--warning">Warning</span>
        <span class="tag tag--danger">Danger</span> <span class="tag tag--info">Info</span></p>
      <p class="demo-row"><button class="btn btn--primary btn--sm" type="button">Primary</button>
        <button class="btn btn--secondary btn--sm" type="button">Secondary</button>
        <button class="btn btn--danger btn--sm" type="button">Danger</button></p>`;
    render($('#text-light'), textPane);
    render($('#text-dark'), textPane);
    const elev = ['xs', 'sm', 'md', 'lg'].map((s) => `<div class="elev" style="box-shadow: var(--awe-shadow-${s})">shadow-${s}</div>`).join('');
    render($('#elev-light'), elev);
    render($('#elev-dark'), elev);

    const cs = getComputedStyle(root);
    const sizes = names.filter((n) => /^--awe-font-size-(xs|sm|base|md|lg|xl|2xl|3xl)$/.test(n));
    render($('#type-scale'), `${sizes.map((n) => `<div class="type-row">
        <span class="type-meta mono">${esc(n.replace('--awe-font-size-', ''))} · ${esc(cs.getPropertyValue(n).trim())}</span>
        <span style="font-size: var(${n}); font-weight: ${n.match(/xl|lg/) ? 600 : 400};
          letter-spacing: ${n.match(/xl/) ? 'var(--awe-letter-spacing-display)' : 'normal'}">Customer list · Balance €82,450.00</span></div>`).join('')}
      <div class="type-row"><span class="type-meta mono">weights</span><span>
        <span style="font-weight: 400">Regular 400</span> · <span style="font-weight: 500">Medium 500</span> ·
        <span style="font-weight: 600">Semibold 600</span> · <span style="font-weight: 700">Bold 700</span></span></div>
      <div class="type-row"><span class="type-meta mono">numbers</span><span class="num-demo">
        <span>Proportional<br>1,111.11<br>9,999.99</span>
        <span class="num">Tabular (cells)<br>1,111.11<br>9,999.99</span></span></div>`);

    const spaces = names.filter((n) => /^--awe-space-\d/.test(n));
    render($('#spacing'), spaces.map((n) => `<div class="space-row"><span class="type-meta mono">${esc(n.replace('--awe-', ''))}
      · ${esc(cs.getPropertyValue(n).trim())}</span><span class="space-bar" style="width: var(${n})"></span></div>`).join(''));
    const radii = names.filter((n) => /^--awe-radius-(none|xs|sm|md|lg|xl|full)$/.test(n));
    render($('#radii'), radii.map((n) => `<div class="radius-box"><span style="border-radius: var(${n})"></span>
      <span class="type-meta mono">${esc(n.replace('--awe-radius-', ''))}</span></div>`).join(''));

    const others = names.filter((n) => !n.startsWith('--awe-palette-') && !/^--awe-(primary|surface)-/.test(n)
      && !n.startsWith('--awe-color-'));
    render($('#other-tokens'), others.map((n) => `<tr><td class="mono">${esc(n)}</td><td class="muted-cell">${esc(tierOf(n))}</td>
      <td class="token-def">${esc(base.get(n))}</td><td class="token-def">${esc(compact.get(n) || '')}</td></tr>`).join(''));

    const palettes = [...new Set(names.filter((n) => n.startsWith('--awe-palette-'))
      .map((n) => n.replace(/^--awe-palette-(.+)-\d+$/, '$1')))];
    render($('#primitives'), palettes.map((p) => `<div class="primitive-row"><span class="type-meta mono">${esc(p)}</span>
      <div class="scale">${SHADES.map((s) => chip(`--awe-palette-${p}-${s}`, s,
        Number(s) >= 500 ? `--awe-palette-${p}-50` : `--awe-palette-${p}-900`)).join('')}</div></div>`).join(''));
  }

  enhanceSelects();
  enhanceMultiSelects();
})();
