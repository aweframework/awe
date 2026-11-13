/*
 * AWE 5 visual reference prototype - application shell (D6), shared by index.html and components.html.
 * Renders the brand slot, the three-level side menu (with search and the collapsed-rail flyout) and the header.
 * The menu is data: in AWE it comes from the same menu XML as today, only the presentation changes.
 */
(() => {
  'use strict';

  const SPRITE = 'assets/icons/sprite.svg';
  const MENU_STATE_KEY = 'awe-prototype-menu';
  const $ = (sel, ctx = document) => ctx.querySelector(sel);
  const $$ = (sel, ctx = document) => Array.from(ctx.querySelectorAll(sel));
  const ESCAPES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
  const esc = (value) => String(value).replace(/[&<>"']/g, (c) => ESCAPES[c]);
  const icon = (name, cls = '') => `<svg class="i ${cls}" aria-hidden="true"><use href="${SPRITE}#${name}"/></svg>`;
  const render = (el, markup) => {
    const range = document.createRange();
    range.selectNodeContents(el);
    el.replaceChildren(range.createContextualFragment(markup));
  };
  window.AweProto = { $, $$, esc, icon, render, SPRITE };

  const app = $('#app');
  const sidebar = $('#sidebar');
  if (!app || !sidebar) return;

  /* ======================================================================
     Menu data: section > option > sub-option > sub-sub-option
     ====================================================================== */
  const leaf = (label, extra = {}) => ({ label, ...extra });
  const MENU = [
    { section: 'Overview', items: [
      leaf('Dashboard', { icon: 'layout-dashboard' }),
      leaf('Inbox', { icon: 'inbox', badge: '5' })
    ] },
    { section: 'Sales', items: [
      { label: 'Customers', icon: 'users', children: [
        { label: 'Directory', children: [
          leaf('Customer list', { id: 'customer-list', href: './' }),
          leaf('Contacts'),
          leaf('Delivery addresses')
        ] },
        { label: 'Segments', children: [leaf('Industry segments'), leaf('Regional segments'), leaf('Segment rules')] },
        { label: 'Price agreements', children: [leaf('Active agreements'), leaf('Pending approval', { badge: '4' }),
          leaf('Expired')] },
        leaf('Credit control')
      ] },
      { label: 'Orders', icon: 'shopping-cart', children: [
        leaf('Sales orders'),
        { label: 'Quotations', children: [leaf('Open quotations'), leaf('Won'), leaf('Lost')] },
        leaf('Returns'),
        leaf('Back orders')
      ] },
      leaf('Invoices', { icon: 'receipt', badge: '12', badgeTone: 'danger' })
    ] },
    { section: 'Operations', items: [
      { label: 'Inventory', icon: 'boxes', children: [
        { label: 'Products', children: [leaf('Catalogue'), leaf('Variants'), leaf('Units of measure')] },
        { label: 'Warehouses', children: [leaf('Locations'), leaf('Bins'), leaf('Transfers')] },
        leaf('Stock movements'),
        leaf('Stocktaking')
      ] },
      { label: 'Purchasing', icon: 'package', children: [leaf('Purchase orders'), leaf('Suppliers'),
        leaf('Supplier invoices')] },
      { label: 'Logistics', icon: 'truck', children: [
        leaf('Shipments'),
        { label: 'Carriers', children: [leaf('Rates'), leaf('Contracts'), leaf('Performance')] },
        leaf('Routes')
      ] }
    ] },
    { section: 'Finance', items: [
      { label: 'Accounting', icon: 'landmark', children: [
        { label: 'General ledger', children: [leaf('Journal entries'), leaf('Chart of accounts'), leaf('Period closing')] },
        leaf('Receivables'),
        leaf('Payables'),
        leaf('Fixed assets')
      ] },
      { label: 'Reports', icon: 'chart-column', children: [
        { label: 'Sales reports', children: [leaf('Monthly report'), leaf('Yearly report'), leaf('By customer')] },
        { label: 'Finance reports', children: [leaf('Balance sheet'), leaf('Profit and loss')] }
      ] }
    ] },
    { section: 'Administration', items: [
      leaf('Users and roles', { icon: 'shield' }),
      leaf('Settings', { icon: 'settings' }),
      { label: 'Design system', icon: 'swatch-book', children: [
        leaf('AWE 5 components', { id: 'components', href: 'components.html', badge: 'New' }),
        leaf('Design tokens', { id: 'tokens', href: 'tokens.html' }),
        leaf('Login', { id: 'login', href: 'login.html' })
      ] }
    ] }
  ];

  // Ids, parents and levels
  const slug = (s) => s.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
  const nodes = new Map();
  const index = (items, parent, level) => items.forEach((n) => {
    n.id = n.id || `${parent ? parent.id + '--' : ''}${slug(n.label)}`;
    n.parent = parent;
    n.level = level;
    nodes.set(n.id, n);
    if (n.children) index(n.children, n, level + 1);
  });
  MENU.forEach((s) => index(s.items, null, 1));

  const activeId = document.body.dataset.page;
  const trail = new Set();
  for (let n = nodes.get(activeId); n; n = n.parent) trail.add(n.id);

  const stored = (() => {
    try {
      return JSON.parse(localStorage.getItem(MENU_STATE_KEY) || '{}');
    } catch {
      return {};
    }
  })();
  const saveState = (patch) => {
    Object.assign(stored, patch);
    try {
      localStorage.setItem(MENU_STATE_KEY, JSON.stringify(stored));
    } catch { /* storage unavailable */ }
  };
  // Expanded options: the active trail is always open on load
  const open = new Set([...trail]);

  /* ======================================================================
     Brand slot: customer logo (wide / square mark + product name) or AWE mark
     ====================================================================== */
  const CUSTOMER_MARK = `<svg class="customer-mark" viewBox="0 0 36 36" aria-hidden="true">
      <rect class="cl-primary" width="36" height="36" rx="9"/>
      <path class="cl-on-primary" d="M9.5 11 17 25.5 24.8 10.6" fill="none" stroke-width="3.4" stroke-linecap="round"
        stroke-linejoin="round"/>
      <circle class="cl-accent" cx="27.2" cy="25.2" r="3.2"/>
    </svg>`;
  const CUSTOMER_WIDE = `<svg class="customer-wide" viewBox="0 0 176 36" aria-hidden="true">
      <g>${CUSTOMER_MARK.replace(/<\/?svg[^>]*>/g, '')}</g>
      <text class="cl-ink" x="45" y="22.5" font-size="21" font-weight="700" letter-spacing="-0.6">veloria</text>
      <text class="cl-muted" x="46" y="33" font-size="7.4" font-weight="600" letter-spacing="2.6">LOGISTICS</text>
    </svg>`;
  // AWE artwork from the repository (assets/logos): wordmark for light / dark surfaces and the "w" mark
  const AWE_WORDMARK = `<img class="awe-logo awe-logo--on-light" src="assets/logos/awe-logo-dark-text.svg" alt="">
      <img class="awe-logo awe-logo--on-dark" src="assets/logos/awe-logo-light-text.svg" alt="">`;
  const AWE_MARK = '<img class="awe-mark-img" src="assets/logos/awe-mark.svg" alt="">';
  const AWE_MINI = '<img class="awe-mini-img" src="assets/logos/awe-mark.svg" alt="">';

  const brandMarkup = `
    <div class="sidebar__brand">
      <a class="brand brand--wide" href="./" aria-label="Veloria Logistics · Operations Hub, home">${CUSTOMER_WIDE}</a>
      <a class="brand brand--mark" href="./" aria-label="Operations Hub, home">${CUSTOMER_MARK}
        <span class="brand-text"><span class="brand-name">Operations Hub</span><span class="brand-sub">Veloria Logistics</span></span></a>
      <a class="brand brand--awe" href="./" aria-label="Operations Hub on AWE, home">${AWE_WORDMARK}</a>
      <a class="brand brand--rail" href="./" aria-label="Operations Hub, home">
        <span class="rail-customer">${CUSTOMER_MARK}</span><span class="rail-awe">${AWE_MARK}</span></a>
    </div>`;

  /* ======================================================================
     Side menu
     ====================================================================== */
  const highlight = (label, q) => {
    if (!q) return esc(label);
    const i = label.toLowerCase().indexOf(q);
    if (i < 0) return esc(label);
    return `${esc(label.slice(0, i))}<mark>${esc(label.slice(i, i + q.length))}</mark>${esc(label.slice(i + q.length))}`;
  };

  const badge = (n) => (n.badge ? `<span class="badge${n.badgeTone ? ' badge--' + n.badgeTone : ''}">${esc(n.badge)}</span>` : '');

  // filter: null (no search) or { q, visible: Set, expanded: Set }
  const renderItems = (items, level, filter) => {
    const lis = items.filter((n) => !filter || filter.visible.has(n.id)).map((n) => {
      const isOpen = filter ? filter.expanded.has(n.id) : open.has(n.id);
      const cls = ['nav-item', `nav-item--l${level}`];
      if (n.id === activeId) cls.push('is-active');
      else if (trail.has(n.id)) cls.push('is-trail');
      const label = `<span class="nav-label">${highlight(n.label, filter && filter.q)}</span>`;
      const ic = n.icon && level === 1 ? icon(n.icon) : '';
      if (n.children) {
        return `<li><button class="${cls.join(' ')}" type="button" data-node="${esc(n.id)}" aria-expanded="${isOpen}"
            aria-controls="sub-${esc(n.id)}" title="${esc(n.label)}">${ic}${label}${badge(n)}${icon('chevron-right', 'chev')}</button>
          <div class="nav-collapse" data-open="${isOpen}"><ul class="nav-sub nav-sub--l${level + 1}" id="sub-${esc(n.id)}">
            ${renderItems(n.children, level + 1, filter)}</ul></div></li>`;
      }
      const current = n.id === activeId ? ' aria-current="page"' : '';
      return `<li><a class="${cls.join(' ')}" href="${esc(n.href || '#')}" data-node="${esc(n.id)}" title="${esc(n.label)}"${current}>
          ${ic}${label}${badge(n)}</a></li>`;
    });
    return lis.join('');
  };

  const renderNav = (filter) => {
    const sections = MENU.filter((s) => !filter || s.items.some((n) => filter.visible.has(n.id)));
    const markup = sections.map((s) => `<div class="nav-section">
        <div class="nav-section__label">${esc(s.section)}</div>
        <ul class="nav-list">${renderItems(s.items, 1, filter)}</ul></div>`).join('');
    render($('#nav'), markup || '<p class="nav-empty">No options match your search.</p>');
  };

  render(sidebar, `${brandMarkup}
    <div class="sidebar__search">
      <label class="control control--sm">
        ${icon('search', 'i--sm')}
        <span class="sr-only">Filter menu options</span>
        <input type="search" id="menu-filter" placeholder="Find an option…" autocomplete="off" aria-keyshortcuts="Control+K Meta+K">
        <span class="kbd" aria-hidden="true">Ctrl K</span>
      </label>
    </div>
    <nav class="nav" id="nav" aria-label="Main menu"></nav>
    <div class="sidebar__footer">
      <span class="powered">${AWE_MINI}<span>Powered by <b>AWE</b> 5</span></span>
      <button class="btn btn--ghost btn--icon btn--sm collapse-btn" type="button" id="menu-collapse" aria-label="Collapse menu"
        title="Collapse menu">${icon('chevrons-left', 'collapse-icon')}</button>
    </div>`);
  renderNav(null);
  // Transitions only after the first paint, so the restored menu state does not animate on load
  requestAnimationFrame(() => requestAnimationFrame(() => $('#nav').classList.add('is-animated')));

  // Expand / collapse per level (event delegation: the nav is re-rendered by the search)
  $('#nav').addEventListener('click', (e) => {
    const btn = e.target.closest('button.nav-item');
    if (!btn) return;
    if (app.dataset.menu === 'collapsed' && !isDrawer()) return; // the rail opens the flyout instead
    const id = btn.dataset.node;
    const expanded = btn.getAttribute('aria-expanded') !== 'true';
    btn.setAttribute('aria-expanded', String(expanded));
    // The wrapper animates its grid row from 0fr to 1fr: the submenu grows to its real height (nested levels included)
    document.getElementById(`sub-${id}`).parentElement.dataset.open = String(expanded);
    if (!$('#menu-filter').value) {
      if (expanded) open.add(id);
      else open.delete(id);
    }
  });

  // Search: matches at any depth, shows their path opened and highlights the text
  $('#menu-filter').addEventListener('input', (e) => {
    const q = e.target.value.trim().toLowerCase();
    if (!q) {
      renderNav(null);
      return;
    }
    const visible = new Set();
    const expanded = new Set();
    nodes.forEach((n) => {
      if (!n.label.toLowerCase().includes(q)) return;
      visible.add(n.id);
      for (let p = n.parent; p; p = p.parent) {
        visible.add(p.id);
        expanded.add(p.id);
      }
      // A matching option keeps its sub-options reachable (collapsed)
      const addDesc = (m) => (m.children || []).forEach((c) => {
        visible.add(c.id);
        addDesc(c);
      });
      addDesc(n);
    });
    renderNav({ q, visible, expanded });
  });

  /* ======================================================================
     Collapsed rail: flyout with the nested levels
     ====================================================================== */
  const isDrawer = () => window.matchMedia('(max-width: 1024px)').matches;
  const isRail = () => app.dataset.menu === 'collapsed' && !isDrawer();
  const flyout = document.createElement('div');
  flyout.className = 'nav-flyout';
  flyout.id = 'nav-flyout';
  flyout.setAttribute('popover', 'manual');
  document.body.appendChild(flyout);
  let flyoutFor = null;
  let closeTimer = null;

  const renderFlyoutItems = (items, level) => items.map((n) => {
    const cls = ['nav-item', `nav-item--l${level}`];
    if (n.id === activeId) cls.push('is-active');
    else if (trail.has(n.id)) cls.push('is-trail');
    if (n.children) {
      return `<li><span class="${cls.join(' ')} nav-item--group">${esc(n.label)}${badge(n)}</span>
        <ul class="nav-sub nav-sub--l${level + 1}">${renderFlyoutItems(n.children, level + 1)}</ul></li>`;
    }
    const current = n.id === activeId ? ' aria-current="page"' : '';
    return `<li><a class="${cls.join(' ')}" href="${esc(n.href || '#')}"${current}><span class="nav-label">${esc(n.label)}</span>
      ${badge(n)}</a></li>`;
  }).join('');

  const closeFlyout = (restoreFocus) => {
    clearTimeout(closeTimer);
    if (!flyoutFor) return;
    const trigger = flyoutFor;
    flyoutFor = null;
    trigger.classList.remove('is-flyout-open');
    if (trigger.hasAttribute('aria-expanded')) trigger.setAttribute('aria-expanded', 'false');
    if (flyout.matches(':popover-open')) flyout.hidePopover();
    if (restoreFocus) trigger.focus();
  };

  const openFlyout = (trigger, focusFirst) => {
    clearTimeout(closeTimer);
    if (flyoutFor === trigger) {
      if (focusFirst) $('a', flyout)?.focus();
      return;
    }
    closeFlyout(false);
    const n = nodes.get(trigger.dataset.node);
    flyoutFor = trigger;
    trigger.classList.add('is-flyout-open');
    if (n.children) trigger.setAttribute('aria-expanded', 'true');
    render(flyout, n.children
      ? `<div class="nav-flyout__head">${esc(n.label)}</div><nav aria-label="${esc(n.label)}">
          <ul class="nav-list">${renderFlyoutItems(n.children, 2)}</ul></nav>`
      : `<a class="nav-flyout__single" href="${esc(n.href || '#')}">${esc(n.label)}${badge(n)}</a>`);
    flyout.classList.toggle('nav-flyout--single', !n.children);
    flyout.showPopover();
    const r = trigger.getBoundingClientRect();
    const top = Math.max(8, Math.min(r.top - (n.children ? 4 : 0), window.innerHeight - flyout.offsetHeight - 8));
    flyout.style.left = `${r.right + 10}px`;
    flyout.style.top = `${n.children ? top : r.top + (r.height - flyout.offsetHeight) / 2}px`;
    if (focusFirst) $('a', flyout)?.focus();
  };

  const scheduleClose = () => {
    clearTimeout(closeTimer);
    closeTimer = setTimeout(() => closeFlyout(false), 220);
  };

  $('#nav').addEventListener('mouseover', (e) => {
    const item = e.target.closest('.nav-item--l1');
    if (item && isRail()) openFlyout(item, false);
  });
  $('#nav').addEventListener('mouseout', (e) => {
    if (isRail() && e.target.closest('.nav-item--l1')) scheduleClose();
  });
  $('#nav').addEventListener('click', (e) => {
    const item = e.target.closest('button.nav-item--l1');
    if (item && isRail()) {
      e.preventDefault();
      if (flyoutFor === item && !e.detail) closeFlyout(true);
      else openFlyout(item, e.detail === 0);
    }
  });
  $('#nav').addEventListener('keydown', (e) => {
    const item = e.target.closest('.nav-item--l1');
    if (item && isRail() && e.key === 'ArrowRight') {
      e.preventDefault();
      openFlyout(item, true);
    }
  });
  $('#nav').addEventListener('focusin', (e) => {
    const item = e.target.closest('.nav-item--l1');
    if (item && isRail() && !item.matches('button')) openFlyout(item, false);
  });
  flyout.addEventListener('mouseenter', () => clearTimeout(closeTimer));
  flyout.addEventListener('mouseleave', scheduleClose);
  flyout.addEventListener('keydown', (e) => {
    const links = $$('a', flyout);
    const i = links.indexOf(document.activeElement);
    if (e.key === 'Escape' || e.key === 'ArrowLeft') {
      e.preventDefault();
      closeFlyout(true);
    } else if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault();
      links[(i + (e.key === 'ArrowDown' ? 1 : -1) + links.length) % links.length]?.focus();
    }
  });
  flyout.addEventListener('focusout', (e) => {
    if (!flyout.contains(e.relatedTarget) && e.relatedTarget !== flyoutFor) scheduleClose();
  });
  document.addEventListener('click', (e) => {
    if (flyoutFor && !flyout.contains(e.target) && !flyoutFor.contains(e.target)) closeFlyout(false);
  });
  window.addEventListener('resize', () => closeFlyout(false));
  $('#nav').addEventListener('scroll', () => closeFlyout(false));

  /* ======================================================================
     Rail / drawer toggles
     ====================================================================== */
  let railTimer = null;
  const setMenu = (state) => {
    app.dataset.menu = state;
    // Labels fade in while the rail widens (class removed when the width transition ends)
    clearTimeout(railTimer);
    app.classList.add('is-menu-animating');
    railTimer = setTimeout(() => app.classList.remove('is-menu-animating'), 400);
    $('#menu-collapse').setAttribute('aria-label', state === 'collapsed' ? 'Expand menu' : 'Collapse menu');
    $('#menu-collapse').title = state === 'collapsed' ? 'Expand menu' : 'Collapse menu';
    closeFlyout(false);
    saveState({ menu: state });
  };
  if (stored.menu === 'collapsed') app.dataset.menu = 'collapsed';
  $('#menu-collapse').addEventListener('click', () => setMenu(app.dataset.menu === 'collapsed' ? 'expanded' : 'collapsed'));

  const mask = document.createElement('div');
  mask.className = 'sidebar-mask';
  sidebar.after(mask);
  mask.addEventListener('click', () => {
    app.dataset.drawer = 'closed';
  });

  /* ======================================================================
     Header
     ====================================================================== */
  const topbar = $('#topbar');
  if (topbar) {
    const onComponents = activeId === 'components';
    render(topbar, `
      <button class="btn btn--ghost btn--icon" type="button" id="menu-toggle" aria-label="Toggle menu" aria-controls="sidebar"
        data-tooltip="Menu" data-tooltip-pos="bottom">${icon('panel-left')}</button>
      <div class="topbar__actions">
        <a class="btn ${onComponents ? 'btn--secondary' : 'btn--ghost'} btn--sm hide-sm" href="components.html"
          ${onComponents ? 'aria-current="page"' : ''}>${icon('blocks', 'i--sm')}AWE 5 components</a>
        <button class="btn btn--ghost btn--icon hide-sm" type="button" aria-label="Help" data-tooltip="Help center"
          data-tooltip-pos="bottom">${icon('circle-help')}</button>
        <div class="dropdown">
          <button class="btn btn--ghost btn--icon has-dot" type="button" data-dropdown aria-expanded="false" aria-haspopup="true"
            aria-label="Notifications, 3 unread">${icon('bell')}</button>
          <div class="overlay overlay--right notif" role="dialog" aria-label="Notifications">
            <div class="notif__head"><strong>Notifications</strong><button class="btn btn--text btn--sm" type="button">Mark all as read</button></div>
            <ul class="notif__list">
              <li class="notif__item is-unread"><span class="avatar avatar--sm avatar--danger">${icon('receipt', 'i--sm')}</span>
                <div><p class="notif__text"><b>3 invoices</b> are overdue for Ferro &amp; Sons Construction</p><p class="notif__time">12 minutes ago</p></div></li>
              <li class="notif__item is-unread"><span class="avatar avatar--sm">LM</span>
                <div><p class="notif__text"><b>Laura Méndez</b> assigned you the account <b>Helios Energy BV</b></p><p class="notif__time">1 hour ago</p></div></li>
              <li class="notif__item is-unread"><span class="avatar avatar--sm avatar--success">${icon('file-spreadsheet', 'i--sm')}</span>
                <div><p class="notif__text">Your export <b>customers-2026-10.xlsx</b> is ready to download</p><p class="notif__time">Yesterday, 18:42</p></div></li>
              <li class="notif__item"><span class="avatar avatar--sm avatar--info">${icon('refresh-cw', 'i--sm')}</span>
                <div><p class="notif__text">Nightly price list synchronisation finished with <b>0 errors</b></p><p class="notif__time">Yesterday, 02:10</p></div></li>
            </ul>
            <div class="notif__foot"><a href="#">View all notifications</a></div>
          </div>
        </div>
        <span class="vr hide-sm" aria-hidden="true"></span>
        <div class="dropdown">
          <button class="user-btn" type="button" data-dropdown aria-expanded="false" aria-haspopup="menu">
            <span class="avatar">PV</span>
            <span class="user-btn__text"><span class="user-btn__name">Pat Vega</span><span class="user-btn__role">Sales operations</span></span>
            ${icon('chevron-down', 'i--sm muted')}
          </button>
          <div class="overlay overlay--right user-pop" role="menu">
            <div class="user-pop__head"><span class="avatar avatar--lg">PV</span><div><b>Pat Vega</b><span>pat.vega@veloria.example</span></div></div>
            <div class="separator"></div>
            <a class="option" role="menuitem" href="#">${icon('user')}My profile</a>
            <a class="option" role="menuitem" href="#">${icon('sliders-horizontal')}Preferences</a>
            <a class="option" role="menuitem" href="#">${icon('keyboard')}Keyboard shortcuts<span class="trail">?</span></a>
            <a class="option" role="menuitem" href="#">${icon('building-2')}Switch company<span class="trail">Veloria ES</span></a>
            <div class="separator"></div>
            <a class="option option--danger" role="menuitem" href="login.html">${icon('log-out')}Sign out</a>
          </div>
        </div>
      </div>`);

    $('#menu-toggle').addEventListener('click', () => {
      if (isDrawer()) app.dataset.drawer = app.dataset.drawer === 'open' ? 'closed' : 'open';
      else setMenu(app.dataset.menu === 'collapsed' ? 'expanded' : 'collapsed');
    });
  }

  document.addEventListener('keydown', (e) => {
    // Ctrl K focuses the menu search, as both engines; on the collapsed rail the search is hidden and the browser keeps it
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k' && !isRail()) {
      e.preventDefault();
      if (isDrawer()) app.dataset.drawer = 'open';
      $('#menu-filter')?.focus();
    }
    if (e.key === 'Escape') {
      if (flyoutFor) closeFlyout(true);
      if (app.dataset.drawer === 'open') app.dataset.drawer = 'closed';
    }
  });
})();
