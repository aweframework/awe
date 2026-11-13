/*
 * AWE 5 visual reference prototype - behaviour of components.html (static demos of the new AWE 5 elements).
 */
(() => {
  'use strict';

  const { $, $$, esc, icon, render, SPRITE } = window.AweProto;
  const fmt = (n, d = 2) => n.toLocaleString('en-GB', { minimumFractionDigits: d, maximumFractionDigits: d });

  /* ======================================================================
     #855 Sparkline column
     ====================================================================== */
  const W = 120;
  const H = 28;
  const PAD = 3;
  const scaleY = (values) => {
    const min = Math.min(0, ...values);
    const max = Math.max(0, ...values);
    const span = max - min || 1;
    return (v) => PAD + (H - 2 * PAD) * (1 - (v - min) / span);
  };
  const xs = (n) => (i) => PAD + ((W - 2 * PAD) * i) / Math.max(1, n - 1);

  const lineSpark = (values, marks) => {
    const y = scaleY(values);
    const x = xs(values.length);
    const d = values.map((v, i) => `${i ? 'L' : 'M'}${x(i).toFixed(1)} ${y(v).toFixed(1)}`).join(' ');
    const iMin = values.indexOf(Math.min(...values));
    const iMax = values.indexOf(Math.max(...values));
    const last = values.length - 1;
    const dots = marks ? `<circle class="spark-min" cx="${x(iMin)}" cy="${y(values[iMin])}" r="2.4"/>
      <circle class="spark-max" cx="${x(iMax)}" cy="${y(values[iMax])}" r="2.4"/>
      <circle class="spark-last" cx="${x(last)}" cy="${y(values[last])}" r="2.6"/>` : '';
    const zero = Math.min(...values) < 0 ? `<line class="spark-zero" x1="0" x2="${W}" y1="${y(0)}" y2="${y(0)}"/>` : '';
    return `<svg class="spark" viewBox="0 0 ${W} ${H}" aria-hidden="true">${zero}<path class="spark-line" d="${d}"/>${dots}</svg>`;
  };
  const barSpark = (values) => {
    const y = scaleY(values);
    const bw = (W - 2 * PAD) / values.length - 2;
    const bars = values.map((v, i) => {
      const x = PAD + i * (bw + 2);
      const top = Math.min(y(v), y(0));
      return `<rect class="${v < 0 ? 'spark-bar spark-bar--neg' : 'spark-bar'}" x="${x.toFixed(1)}" y="${top.toFixed(1)}"
        width="${bw.toFixed(1)}" height="${Math.max(1, Math.abs(y(v) - y(0))).toFixed(1)}" rx="1"/>`;
    }).join('');
    return `<svg class="spark" viewBox="0 0 ${W} ${H}" aria-hidden="true">${bars}</svg>`;
  };
  const areaSpark = (values) => {
    const y = scaleY(values);
    const x = xs(values.length);
    const pts = values.map((v, i) => `${x(i).toFixed(1)} ${y(v).toFixed(1)}`);
    const z = y(0).toFixed(1);
    const clipId = `sp-${Math.random().toString(36).slice(2, 8)}`;
    return `<svg class="spark" viewBox="0 0 ${W} ${H}" aria-hidden="true">
      <defs><clipPath id="${clipId}-pos"><rect x="0" y="0" width="${W}" height="${z}"/></clipPath>
        <clipPath id="${clipId}-neg"><rect x="0" y="${z}" width="${W}" height="${H}"/></clipPath></defs>
      <path class="spark-area" clip-path="url(#${clipId}-pos)" d="M${x(0)} ${z} L${pts.join(' L')} L${x(values.length - 1)} ${z} Z"/>
      <path class="spark-area spark-area--neg" clip-path="url(#${clipId}-neg)" d="M${x(0)} ${z} L${pts.join(' L')} L${x(values.length - 1)} ${z} Z"/>
      <path class="spark-line" clip-path="url(#${clipId}-pos)" d="M${pts.join(' L')}"/>
      <path class="spark-line spark-line--neg" clip-path="url(#${clipId}-neg)" d="M${pts.join(' L')}"/>
      <line class="spark-zero" x1="0" x2="${W}" y1="${z}" y2="${z}"/></svg>`;
  };

  const SPARKS = [
    ['Steel shelving 180 cm', [12, 14, 13, 17, 19, 18, 22, 24, 23, 27, 29, 31], [42, 38, 51, 47, 55, 61, 58], [8, 9, 7, 10, 11, 9, 12]],
    ['Pallet wrap 23 µm', [40, 37, 35, 36, 31, 29, 30, 26, 24, 25, 21, 19], [120, 110, 96, 102, 88, 80, 74], [5, 3, 1, -2, -1, 2, 3]],
    ['Corner protectors', [8, 11, 7, 12, 9, 14, 10, 15, 11, 13, 12, 16], [300, 280, 340, 310, 290, 360, 330], [-3, -4, -2, -5, -3, -1, -2]],
    ['Forklift battery 48V', [3, 3, 4, 3, 5, 4, 6, 5, 7, 6, 8, 9], [2, 3, 1, 4, 2, 5, 3], [14, 15, 13, 16, 18, 17, 19]]
  ];
  const sparkBody = $('#spark-grid tbody');
  if (sparkBody) {
    render(sparkBody, SPARKS.map(([name, trend, units, margin]) => `<tr>
      <td>${esc(name)}</td>
      <td>${lineSpark(trend, true)}</td>
      <td>${barSpark(units)}</td>
      <td>${areaSpark(margin)}</td>
      <td class="num${margin[margin.length - 1] < 0 ? ' negative' : ''}">${margin[margin.length - 1]}%</td></tr>`).join(''));
  }

  /* ======================================================================
     #831 Data bars
     ====================================================================== */
  const BARS = [['Centre', 1700680, 6.2], ['East', 1496470, 3.1], ['North', 911820, -2.4], ['South', 792130, -6.8],
    ['Islands', 312400, 1.4]];
  const barBody = $('#databar-grid tbody');
  if (barBody) {
    const maxRev = Math.max(...BARS.map((b) => b[1]));
    const maxAbs = Math.max(...BARS.map((b) => Math.abs(b[2])));
    render(barBody, BARS.map(([region, rev, margin]) => `<tr><td>${esc(region)}</td>
      <td class="num"><span class="databar" style="--w: ${((rev / maxRev) * 100).toFixed(1)}%"><span>${fmt(rev, 0)}</span></span></td>
      <td class="num"><span class="databar databar--diverging ${margin < 0 ? 'is-negative' : ''}"
        style="--w: ${((Math.abs(margin) / maxAbs) * 50).toFixed(1)}%"><span class="${margin < 0 ? 'negative' : ''}">${margin > 0 ? '+' : ''}${fmt(margin, 1)} pt</span></span></td></tr>`).join(''));
  }

  /* ======================================================================
     #830 Colour scale cells
     ====================================================================== */
  const heat = $('#heatmap');
  if (heat) {
    const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const rows = [['Madrid', [82, 91, 88, 95, 97, 54, 12]], ['Barcelona', [64, 70, 76, 81, 89, 41, 8]],
      ['Valencia', [45, 52, 61, 58, 72, 33, 5]], ['Sevilla', [38, 41, 36, 49, 55, 22, 3]]];
    const step = (v) => Math.min(5, 1 + Math.floor(v / 20));
    render($('thead', heat), `<tr><th>Warehouse</th>${days.map((d) => `<th class="num">${d}</th>`).join('')}</tr>`);
    render($('tbody', heat), rows.map(([w, vals]) => `<tr><th scope="row">${esc(w)}</th>${vals.map((v) =>
      `<td class="num heat scale-${step(v)}">${v}</td>`).join('')}</tr>`).join(''));
  }

  /* ======================================================================
     #820 Month picker
     ====================================================================== */
  const monthGrid = $('#month-grid');
  if (monthGrid) {
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    render(monthGrid, months.map((m, i) => `<button type="button" class="monthpicker__month${i === 9 ? ' is-current' : ''}"
      ${i === 9 ? 'aria-selected="true"' : ''}>${m}</button>`).join(''));
    monthGrid.addEventListener('click', (e) => {
      const b = e.target.closest('button');
      if (!b) return;
      $$('button', monthGrid).forEach((x) => x.removeAttribute('aria-selected'));
      b.setAttribute('aria-selected', 'true');
      $('#c-month').value = `${b.textContent} 2026`;
    });
  }

  /* ======================================================================
     #839 Date range shortcuts
     ====================================================================== */
  const shortcuts = $('#dr-shortcuts');
  if (shortcuts) {
    const today = new Date(2026, 9, 23);
    const d2s = (d) => `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
    shortcuts.addEventListener('click', (e) => {
      const b = e.target.closest('.shortcut');
      if (!b) return;
      $$('.shortcut', shortcuts).forEach((x) => x.setAttribute('aria-pressed', String(x === b)));
      const from = new Date(today);
      if (b.dataset.days) from.setDate(from.getDate() - Number(b.dataset.days));
      if (b.dataset.months) from.setMonth(from.getMonth() - Number(b.dataset.months));
      if (b.dataset.ytd) from.setMonth(0, 1);
      $('#dr-from').value = b.dataset.all ? '' : d2s(from);
      $('#dr-from').placeholder = 'Any';
      $('#dr-to').value = d2s(today);
    });
  }

  /* ======================================================================
     Grid demos: expandable rows, grouping, move rows, quick search
     ====================================================================== */
  $$('#expand-grid .tree-toggle').forEach((btn) => btn.addEventListener('click', () => {
    const open = btn.getAttribute('aria-expanded') !== 'true';
    btn.setAttribute('aria-expanded', String(open));
    btn.setAttribute('aria-label', btn.getAttribute('aria-label').replace(/^(Expand|Collapse)/, open ? 'Collapse' : 'Expand'));
    document.getElementById(btn.getAttribute('aria-controls')).hidden = !open;
  }));

  $$('#group-grid [data-group]').forEach((btn) => btn.addEventListener('click', () => {
    const open = btn.getAttribute('aria-expanded') !== 'true';
    btn.setAttribute('aria-expanded', String(open));
    btn.setAttribute('aria-label', btn.getAttribute('aria-label').replace(/^(Expand|Collapse)/, open ? 'Collapse' : 'Expand'));
    $$(`#group-grid tr[data-in="${btn.dataset.group}"]`).forEach((tr) => {
      tr.hidden = !open;
    });
  }));

  const moveBody = $('#move-grid tbody');
  if (moveBody) {
    const renumber = () => $$('tr', moveBody).forEach((tr, i) => {
      tr.cells[0].textContent = String(i + 1);
    });
    moveBody.addEventListener('click', (e) => {
      const tr = e.target.closest('tr');
      if (!tr) return;
      $$('tr', moveBody).forEach((r) => {
        r.classList.toggle('is-selected', r === tr);
        r.setAttribute('aria-selected', String(r === tr));
      });
    });
    $$('[data-move]').forEach((b) => b.addEventListener('click', () => {
      const sel = $('tr.is-selected', moveBody);
      if (!sel) return;
      const where = b.dataset.move;
      if (where === 'top') moveBody.prepend(sel);
      if (where === 'bottom') moveBody.append(sel);
      if (where === 'up' && sel.previousElementSibling) sel.previousElementSibling.before(sel);
      if (where === 'down' && sel.nextElementSibling) sel.nextElementSibling.after(sel);
      renumber();
    }));
  }

  const qs = $('#qs-input');
  if (qs) {
    const rows = $$('#qs-grid tbody tr');
    qs.addEventListener('input', () => {
      const q = qs.value.trim().toLowerCase();
      let n = 0;
      let total = 0;
      rows.forEach((tr) => {
        const hit = !q || tr.textContent.toLowerCase().includes(q);
        tr.hidden = !hit;
        if (hit) {
          n++;
          total += Number(tr.cells[3].textContent.replace(/,/g, ''));
        }
      });
      $('#qs-total').textContent = fmt(total);
      $('#qs-total-label').textContent = `Filtered total · ${n} of 8 rows`;
      $('#qs-count').textContent = `${n} of 8 rows`;
    });
  }

  /* ======================================================================
     #827 Reveal the first invalid field across tabs
     ====================================================================== */
  const validate = $('#reveal-validate');
  if (validate) {
    const reveal = () => {
      $('#rv-tab-billing').click();
      const input = $('#rv-iban');
      input.focus();
      input.scrollIntoView({ block: 'center', behavior: 'smooth' });
    };
    validate.addEventListener('click', () => {
      const invalid = !$('#rv-iban').value.trim();
      $('#rv-iban-control').classList.toggle('is-invalid', invalid);
      $('#rv-iban').setAttribute('aria-invalid', String(invalid));
      $('#rv-iban-err').hidden = !invalid;
      $('.tab-error', $('#rv-tab-billing')).hidden = !invalid;
      $('#reveal-summary').hidden = !invalid;
      if (invalid) reveal();
    });
    $('#reveal-go').addEventListener('click', reveal);
  }

  /* ======================================================================
     Overlays: context menu, maximize, popover, dialog sizes
     ====================================================================== */
  const ctxField = $('#ctx-field');
  if (ctxField) {
    const menu = $('#ctx-menu');
    ctxField.closest('.control').addEventListener('contextmenu', (e) => {
      e.preventDefault();
      menu.classList.remove('is-hidden');
      $('.option', menu).focus();
    });
    ctxField.addEventListener('keydown', (e) => {
      if (e.key === 'F10' && e.shiftKey) {
        e.preventDefault();
        menu.classList.remove('is-hidden');
        $('.option', menu).focus();
      }
    });
    menu.addEventListener('click', () => menu.classList.add('is-hidden'));
  }

  const maxToggle = $('#max-toggle');
  if (maxToggle) {
    const card = $('#max-card');
    const setMax = (on) => {
      card.classList.toggle('is-maximized', on);
      maxToggle.setAttribute('aria-label', on ? 'Restore' : 'Maximize');
      maxToggle.dataset.tooltip = on ? 'Restore' : 'Maximize';
      $('use', maxToggle).setAttribute('href', `${SPRITE}#${on ? 'minimize-2' : 'maximize-2'}`);
    };
    maxToggle.addEventListener('click', () => setMax(!card.classList.contains('is-maximized')));
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && card.classList.contains('is-maximized')) setMax(false);
    });
  }

  $$('[data-close-popover]').forEach((b) => b.addEventListener('click', () => {
    const dd = b.closest('.dropdown');
    dd.classList.remove('is-open');
    $('[data-dropdown]', dd).setAttribute('aria-expanded', 'false');
    $('[data-dropdown]', dd).focus();
  }));

  const SIZES = { sm: 'Small · 360 px', md: 'Medium · 560 px', lg: 'Large · 800 px', xl: 'Extra large · 1140 px', full: 'Full screen' };
  $$('[data-dialog-size]').forEach((b) => b.addEventListener('click', () => {
    const dlg = $('#dlg-size');
    dlg.className = `modal modal--size-${b.dataset.dialogSize}`;
    $('#dlg-size-title').textContent = `${b.textContent} dialog`;
    $('#dlg-size-sub').textContent = SIZES[b.dataset.dialogSize];
    dlg.showModal();
  }));

  /* ======================================================================
     #828 Launcher pages (three levels)
     ====================================================================== */
  const LAUNCHERS = {
    root: { title: 'Reports', cards: [
      { icon: 'chart-column', title: 'Sales reports', desc: 'Revenue, orders and margins by period and customer', go: 'sales', count: '3 reports' },
      { icon: 'landmark', title: 'Finance reports', desc: 'Balance sheet, profit and loss and receivables', count: '2 reports' },
      { icon: 'truck', title: 'Logistics reports', desc: 'Deliveries, carriers and on-time performance', count: '4 reports' },
      { icon: 'boxes', title: 'Inventory reports', desc: 'Stock levels, rotation and valuation', count: '3 reports' }
    ] },
    sales: { title: 'Sales reports', parent: 'root', cards: [
      { icon: 'calendar-days', title: 'Monthly report', desc: 'Sales grouped by month with comparison to last year', go: 'monthly' },
      { icon: 'calendar', title: 'Yearly report', desc: 'Year totals and growth by segment' },
      { icon: 'users', title: 'By customer', desc: 'Ranking of customers by revenue and margin' }
    ] },
    monthly: { title: 'Monthly report', parent: 'sales', cards: [
      { icon: 'file-spreadsheet', title: 'October 2026', desc: 'Closed on 2 Nov · 1,284 customers' },
      { icon: 'file-spreadsheet', title: 'September 2026', desc: 'Closed on 3 Oct · 1,271 customers' },
      { icon: 'file-text', title: 'Q3 2026 summary', desc: 'Quarter summary for the board' }
    ] }
  };
  const launcher = $('#launcher');
  if (launcher) {
    const show = (key) => {
      const page = LAUNCHERS[key];
      const path = [];
      for (let k = key; k; k = LAUNCHERS[k].parent) path.unshift(k);
      render($('#launcher-crumbs'), path.map((k, i) => {
        const sep = i ? icon('chevron-right', 'i--sm sep') : '';
        return i === path.length - 1
          ? `<li>${sep}<span aria-current="page">${esc(LAUNCHERS[k].title)}</span></li>`
          : `<li>${sep}<a href="#" data-launch="${k}">${esc(LAUNCHERS[k].title)}</a></li>`;
      }).join(''));
      render(launcher, page.cards.map((c) => `<a class="launcher-card" href="#" ${c.go ? `data-launch="${c.go}"` : ''}>
        <span class="launcher-card__icon">${icon(c.icon, 'i--lg')}</span>
        <span class="launcher-card__text"><span class="launcher-card__title">${esc(c.title)}</span>
          <span class="launcher-card__desc">${esc(c.desc)}</span>
          ${c.count ? `<span class="launcher-card__meta">${esc(c.count)}</span>` : ''}</span>
        ${icon('chevron-right', 'launcher-card__chev')}</a>`).join(''));
    };
    $('#c-828').addEventListener('click', (e) => {
      const a = e.target.closest('[data-launch]');
      if (!a) return;
      e.preventDefault();
      show(a.dataset.launch);
      $('.launcher-card', launcher)?.focus();
    });
    show('monthly');
  }
})();
