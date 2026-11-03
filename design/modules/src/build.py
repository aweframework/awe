#!/usr/bin/env python3
"""Builds the Modules window artboards (design/modules/*.html).

head.html is the <head> of design/sites/02-panel-ancho.html up to its artboard CSS (tokens.css embedded plus the
shell, grid, field and side panel rules), so the three directions share the visual language of the admin windows
that are already redesigned. menu.html is the side menu of that artboard. Run: python3 design/modules/src/build.py
"""
import pathlib
import re

SRC = pathlib.Path(__file__).parent
OUT = SRC.parent

HEAD = (SRC / "head.html").read_text(encoding="utf-8")
MENU = (SRC / "menu.html").read_text(encoding="utf-8")
# The current option is Modules, not Sites; the Sites spike option of that study is not part of the menu
MENU = MENU.replace(' aria-current="page"', "")
MENU = re.sub(r'<a class="menu-item" href="#"><svg(?:(?!</a>).)*?</svg>Sites \(spike\)</a>', "", MENU, flags=re.S)
MENU = re.sub(r'<a class="menu-item" href="#">(<svg(?:(?!</a>).)*?</svg>Modules</a>)',
              r'<a class="menu-item" href="#" aria-current="page">\1', MENU, count=1, flags=re.S)

EXTRA_CSS = """
/* ==== Modules window ==== */
.tag--warning { background: var(--awe-color-warning-subtle); color: var(--awe-color-warning-text);
  border-color: var(--awe-color-warning-border); }
.tag--danger { background: var(--awe-color-danger-subtle); color: var(--awe-color-danger-text);
  border-color: var(--awe-color-danger-border); }
.mono { font-family: var(--awe-font-family-monospace); font-size: var(--awe-font-size-xs);
  color: var(--awe-color-text-muted); white-space: nowrap; }
.summary { display: flex; flex-wrap: wrap; gap: var(--awe-space-1); }
.pill { display: inline-flex; align-items: center; gap: var(--awe-space-1); padding: var(--awe-space-0-5) var(--awe-space-2);
  border-radius: var(--awe-radius-full); font-size: var(--awe-font-size-xs); background: var(--awe-color-surface-hover);
  color: var(--awe-color-text); white-space: nowrap; }
.pill .icon { color: var(--awe-color-text-muted); }
.muted { color: var(--awe-color-text-muted); }

/* Tabs inside the side panel */
.tabs { display: flex; gap: var(--awe-space-1); padding: 0 var(--awe-space-5);
  border-bottom: var(--awe-border-width) solid var(--awe-color-border); }
.tab { display: inline-flex; align-items: center; gap: var(--awe-space-2); padding: var(--awe-space-3) var(--awe-space-3);
  border: 0; background: transparent; color: var(--awe-color-text-muted); font-weight: var(--awe-font-weight-medium);
  font-size: var(--awe-font-size-sm); cursor: pointer; position: relative; }
.tab:hover { color: var(--awe-color-text); }
.tab:focus-visible { outline: var(--awe-focus-ring); outline-offset: calc(var(--awe-focus-ring-offset) * -1); }
.tab[aria-selected="true"] { color: var(--awe-color-primary-text); }
.tab[aria-selected="true"]::after { content: ""; position: absolute; left: 0; right: 0; bottom: calc(var(--awe-border-width) * -1);
  height: var(--awe-space-0-5); background: var(--awe-color-primary); border-radius: var(--awe-radius-full); }
.tab .n { font-size: var(--awe-font-size-xs); background: var(--awe-color-surface-hover); color: var(--awe-color-text-muted);
  border-radius: var(--awe-radius-full); padding: 0 var(--awe-space-1-5); }
.tab .err { width: var(--awe-space-1-5); height: var(--awe-space-1-5); border-radius: var(--awe-radius-full);
  background: var(--awe-color-danger); }

/* Sub grids inside a section */
.subgrid { border: var(--awe-border-width) solid var(--awe-color-border); border-radius: var(--awe-radius-lg);
  overflow: hidden; }
.subgrid .grid-head-bar { padding: var(--awe-space-2) var(--awe-space-3); background: var(--awe-color-surface-subtle);
  border-bottom: var(--awe-border-width) solid var(--awe-color-border-subtle); }
.subgrid .grid-head-bar strong { font-size: var(--awe-font-size-sm); }
.subgrid table.grid th { position: static; }
.hint { display: flex; gap: var(--awe-space-2); align-items: flex-start; padding: var(--awe-space-3);
  border-radius: var(--awe-radius-lg); background: var(--awe-color-info-subtle); color: var(--awe-color-info-text);
  border: var(--awe-border-width) solid var(--awe-color-info-border); font-size: var(--awe-font-size-sm); }
.hint .icon { margin-top: var(--awe-space-0-5); }
.stack { display: flex; flex-direction: column; gap: var(--awe-space-4); }
.field input[aria-invalid="true"] { border-color: var(--awe-color-field-border-invalid); box-shadow: var(--awe-shadow-field-invalid); }
.field .error { font-size: var(--awe-font-size-xs); color: var(--awe-color-danger-text); }
.cell-select.invalid { border-color: var(--awe-color-field-border-invalid); }

/* Detail page (direction 03) */
.detail-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: var(--awe-layout-gap); }
.save-bar { position: sticky; bottom: 0; display: flex; align-items: center; gap: var(--awe-space-3);
  padding: var(--awe-space-3) var(--awe-page-padding); background: var(--awe-color-surface);
  border-top: var(--awe-border-width) solid var(--awe-color-border); box-shadow: var(--awe-shadow-md); }
.save-bar .spacer { flex: 1; }
.dirty { width: var(--awe-space-2); height: var(--awe-space-2); border-radius: var(--awe-radius-full);
  background: var(--awe-color-warning); }
"""

# Lucide paths (stroke icons, 24x24)
ICONS = {
    "plus": '<circle cx="12" cy="12" r="9"/><path d="M12 8v8M8 12h8"/>',
    "pencil": '<path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"/>',
    "more": '<circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/>',
    "trash": '<path d="M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14"/>',
    "x": '<path d="M18 6 6 18M6 6l12 12"/>',
    "check": '<path d="m5 13 4 4L19 7"/>',
    "chev": '<path d="m6 9 6 6 6-6"/>',
    "search": '<circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/>',
    "help": '<circle cx="12" cy="12" r="9"/><path d="M9.5 9a2.5 2.5 0 1 1 3.5 2.3c-.7.4-1 .9-1 1.7"/><path d="M12 17h.01"/>',
    "print": '<path d="M6 9V3h12v6"/><rect x="3" y="9" width="18" height="8" rx="2"/><path d="M6 17h12v4H6z"/>',
    "columns": '<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M12 4v16"/>',
    "export": '<path d="M12 3v12"/><path d="m7 11 5 5 5-5"/><path d="M4 20h16"/>',
    "box": '<path d="M21 8 12 3 3 8v8l9 5 9-5Z"/><path d="m3 8 9 5 9-5"/><path d="M12 13v8"/>',
    "shield": '<path d="M12 3 4 6v6c0 5 3.5 8 8 9 4.5-1 8-4 8-9V6Z"/>',
    "users": '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20a6.5 6.5 0 0 1 13 0"/><path d="M16 4.5a3.5 3.5 0 0 1 0 7"/><path d="M18 14.5a6.5 6.5 0 0 1 3.5 5.5"/>',
    "user": '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
    "db": '<ellipse cx="12" cy="6" rx="8" ry="3"/><path d="M4 6v6c0 1.7 3.6 3 8 3s8-1.3 8-3V6"/><path d="M4 12v6c0 1.7 3.6 3 8 3s8-1.3 8-3v-6"/>',
    "network": '<rect x="9" y="3" width="6" height="5" rx="1"/><rect x="2" y="16" width="6" height="5" rx="1"/><rect x="16" y="16" width="6" height="5" rx="1"/><path d="M12 8v4M5 16v-2h14v2"/>',
    "info": '<circle cx="12" cy="12" r="9"/><path d="M12 11v6"/><path d="M12 7h.01"/>',
    "grip": '<circle cx="9" cy="6" r="1"/><circle cx="9" cy="12" r="1"/><circle cx="9" cy="18" r="1"/><circle cx="15" cy="6" r="1"/><circle cx="15" cy="12" r="1"/><circle cx="15" cy="18" r="1"/>',
    "back": '<path d="m12 19-7-7 7-7"/><path d="M19 12H5"/>',
    "link": '<path d="M7 17 17 7"/><path d="M8 7h9v9"/>',
    "palette": '<circle cx="13.5" cy="6.5" r="1"/><circle cx="17.5" cy="10.5" r="1"/><circle cx="8.5" cy="7.5" r="1"/><circle cx="6.5" cy="12.5" r="1"/><path d="M12 2a10 10 0 0 0 0 20c1 0 1.7-.8 1.7-1.7 0-.4-.2-.8-.4-1.1-.3-.3-.4-.7-.4-1.1 0-.9.8-1.7 1.7-1.7H17a5 5 0 0 0 5-5c0-5-4.5-9-10-9Z"/>',
    "monitor": '<rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4"/>',
    "save": '<path d="M5 3h11l5 5v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z"/><path d="M7 3v5h8"/><path d="M7 21v-7h10v7"/>',
    "filter": '<path d="M3 5h18l-7 8v6l-4 2v-8Z"/>',
}


def ic(name, size="sm"):
    cls = "icon" if size == "md" else "icon icon--" + size
    return f'<svg class="{cls}" viewBox="0 0 24 24" aria-hidden="true">{ICONS[name]}</svg>'


def btn(label, icon=None, kind="", aria=None):
    cls = "btn" + "".join(" btn--" + k for k in kind.split() if k)
    a = f' aria-label="{aria}"' if aria else ""
    inner = (ic(icon, "md" if "sm" not in kind else "sm") if icon else "") + (label or "")
    return f'<button class="{cls}"{a}>{inner}</button>'


def tag(text, tone):
    return f'<span class="tag tag--{tone}"><span class="dot"></span>{text}</span>'


def cell_select(text, invalid=False):
    cls = "cell-select invalid" if invalid else "cell-select"
    return f'<div class="{cls}" role="button" tabindex="0">{text} {ic("chev")}</div>'


def row_actions(label):
    return (f'<div class="row-actions">{btn("", "pencil", "ghost sm", "Editar " + label)}'
            f'{btn("", "trash", "ghost sm", "Quitar " + label)}</div>')


def editing_actions():
    return ('<div class="row-actions">'
            '<button class="btn btn--ghost btn--sm" aria-label="Guardar la fila" style="color:var(--awe-color-primary-text)">'
            f'{ic("check")}</button>{btn("", "x", "ghost sm", "Cancelar la fila")}</div>')


# ---------------------------------------------------------------------------------------------------------------
# Data
# ---------------------------------------------------------------------------------------------------------------
MODULES = [
    # name, initials, screen, theme, order, active, profiles, users, sites
    ("Base", "BA", "home", "Sunset", 1, True, 3, 1, "Madrid · Oñate"),
    ("Informes", "IN", "report-home", "Clean", 2, True, 2, 0, "Madrid"),
    ("Planificador", "PL", "scheduler-tasks", "Sunset", 3, True, 1, 2, "Madrid · Oñate"),
    ("Test", "TE", "test-home", "Grass", 4, False, 0, 1, "Bilbao"),
]


def module_rows(selected=None, access_col=True, themes=True):
    rows = []
    for name, ini, scr, thm, ordr, act, pro, usr, sites in MODULES:
        sel = ' aria-selected="true"' if name == selected else ""
        access = []
        if pro:
            access.append(f'<span class="pill">{ic("shield")}{pro} {"perfil" if pro == 1 else "perfiles"}</span>')
        if usr:
            access.append(f'<span class="pill">{ic("user")}{usr} {"usuario" if usr == 1 else "usuarios"}</span>')
        if not access:
            access.append('<span class="muted">Sin acceso</span>')
        status = tag("Activo", "success") if act else tag("Inactivo", "neutral")
        access_td = f'<td><div class="summary">{"".join(access)}</div></td>' if access_col else ""
        rows.append(
            f'<tr{sel}><td><div class="cell-main"><span class="avatar">{ini}</span>'
            f'<span><span class="cell-strong">{name}</span><span class="cell-sub">Pantalla inicial · '
            f'<span class="mono">{scr}</span></span></span></div></td>'
            f'{f"<td>{thm}</td>" if themes else ""}{access_td}<td><span class="pill">{ic("network")}{sites}</span></td>'
            f'<td class="num">{ordr}</td><td>{status}</td>'
            f'<td class="actions">{btn("", "pencil", "ghost sm", "Editar " + name)}'
            f'{btn("", "more", "ghost sm", "Más acciones de " + name)}</td></tr>')
    return "\n".join(rows)


def list_card(selected=None, new_label="Nuevo módulo", themes=True):
    theme_th = '<th style="width:8rem">Tema</th>' if themes else ""
    return f'''
      <div class="page-head">
        <div><h1>Módulos</h1><p>Los módulos de la aplicación, quién puede entrar en cada uno y qué base de datos usa en cada site.</p></div>
        <div class="actions">
          {btn("", "help", "ghost", "Ayuda")}
          {btn("", "print", "ghost", "Imprimir")}
          {btn(new_label, "plus", "primary")}
        </div>
      </div>
      <section class="card">
        <div class="card-head"><h2>Módulos</h2><span class="count">4</span>
          <div class="actions"><span class="col-count">Ordenado por orden</span>{ic("filter")}</div></div>
        <div class="grid-toolbar">
          <div class="search">{ic("search")}<span>Buscar por nombre o pantalla inicial</span></div>
          <div class="segmented" role="group" aria-label="Filtros rápidos">
            <button aria-pressed="true">Todos <span class="n">4</span></button>
            <button aria-pressed="false">Activos <span class="n">3</span></button>
            <button aria-pressed="false">Inactivos <span class="n">1</span></button>
          </div>
          <div class="spacer"></div>
          {btn("", "columns", "ghost", "Columnas")}
          {btn("", "export", "ghost", "Exportar")}
        </div>
        <table class="grid">
          <thead><tr><th>Módulo</th>{theme_th}<th style="width:15rem">Acceso</th>
            <th style="width:12rem">Sites</th><th style="width:6rem">Orden</th><th style="width:8rem">Estado</th>
            <th style="width:6rem"><span class="sr-only">Acciones</span></th></tr></thead>
          <tbody>
{module_rows(selected, themes=themes)}
          </tbody>
        </table>
        <div class="card-foot"><span>Mostrando 1–4 de 4</span><div class="actions">
          <button class="btn btn--sm">Anterior</button><button class="btn btn--sm">Siguiente</button></div></div>
      </section>'''


def shell(page, crumb="Modules", overlay=""):
    return f'''
<div class="shell">
  {MENU}
  <div class="main">
    <header class="app-header">
      {btn("", "columns", "ghost", "Mostrar u ocultar el menú")}
      <div class="spacer"></div>
      <button class="btn btn--ghost" aria-label="Menú de usuario"><span class="avatar">PV</span></button>
    </header>
    <nav class="crumbs" aria-label="Migas de pan">Home<span>/</span>Tools<span>/</span><strong>{crumb}</strong></nav>
    <div class="page">
{page}
    </div>
  </div>
</div>
{overlay}'''


# ---------------------------------------------------------------------------------------------------------------
# Panel sections
# ---------------------------------------------------------------------------------------------------------------
def identity_section(invalid_name=False, themes=True):
    theme_field = ('<div class="field"><label for="thm">Tema</label>' + cell_select("Sunset") +
                   '<span class="help">Vacío: el tema de cada usuario.</span></div>') if themes else ""
    name_field = ('<input id="nam" value="" aria-invalid="true" placeholder="Por ejemplo Informes">'
                  '<span class="error">El nombre es obligatorio.</span>') if invalid_name else \
        '<input id="nam" value="Planificador" placeholder="Por ejemplo Informes">' \
        '<span class="help">Es el nombre que eligen los usuarios en el selector de módulo.</span>'
    return f'''
      <div class="form-section">{ic("box")}Identidad</div>
      <div class="form-row">
        <div class="field"><label for="nam">Nombre <span class="req">*</span></label>{name_field}</div>
        <div class="field"><label for="scr">Pantalla inicial</label>{cell_select('<span class="mono">scheduler-tasks</span>')}
          <span class="help">La pantalla que se abre al entrar en el módulo.</span></div>
        {theme_field}
        <div class="field"><label for="ord">Orden</label><input id="ord" value="3">
          <span class="help">Posición en el selector de módulo.</span></div>
        <div class="field field--inline"><span class="switch" role="switch" aria-checked="true" aria-label="Activo"></span><label>Activo</label></div>
      </div>'''


def profiles_grid(editing=False):
    first = (f'<tr class="editing"><td>{cell_select("GEN · General")}</td><td>{editing_actions()}</td></tr>'
             if editing else
             f'<tr><td><span class="cell-strong">ADM</span> · Administrador</td><td>{row_actions("ADM")}</td></tr>')
    return f'''
      <div class="subgrid">
        <div class="grid-head-bar">{ic("shield")}<strong>Perfiles</strong><span class="col-count">Todos sus usuarios entran</span>
          <div class="spacer"></div>{btn("Añadir perfil", "plus", "sm")}</div>
        <table class="grid editable">
          <thead><tr><th>Perfil</th><th style="width:5rem"><span class="sr-only">Acciones</span></th></tr></thead>
          <tbody>{first}
            <tr><td><span class="cell-strong">OPE</span> · Operador de planificación</td><td>{row_actions("OPE")}</td></tr>
          </tbody>
        </table>
      </div>'''


def users_grid(editing=False, themes=True):
    def thm(value):
        return f"<td>{value}</td>" if themes else ""
    first = (f'<tr class="editing"><td>{cell_select("jmartin · Julia Martín")}</td>{thm(cell_select("Sunset"))}'
             f'<td>{editing_actions()}</td></tr>' if editing else
             f'<tr><td><span class="cell-strong">jmartin</span><span class="cell-sub">Julia Martín</span></td>{thm("Sunset")}'
             f'<td>{row_actions("jmartin")}</td></tr>')
    theme_th = '<th style="width:10rem">Tema en el módulo</th>' if themes else ""
    return f'''
      <div class="subgrid">
        <div class="grid-head-bar">{ic("user")}<strong>Usuarios</strong><span class="col-count">Además de los de sus perfiles</span>
          <div class="spacer"></div>{btn("Añadir usuario", "plus", "sm")}</div>
        <table class="grid editable">
          <thead><tr><th>Usuario</th>{theme_th}<th style="width:5rem"><span class="sr-only">Acciones</span></th></tr></thead>
          <tbody>{first}
            <tr><td><span class="cell-strong">lgarcia</span><span class="cell-sub">Luis García</span></td>{thm("Clean")}<td>{row_actions("lgarcia")}</td></tr>
          </tbody>
        </table>
      </div>'''


def sites_grid(editing=False, invalid=False):
    first = (f'<tr class="editing"><td>{ic("grip")}</td><td>{cell_select("Bilbao")}</td>'
             f'<td>{cell_select("Elige una base de datos", invalid)}</td><td><input class="cell-input" value="3" aria-label="Orden"></td>'
             f'<td>{editing_actions()}</td></tr>' if editing else "")
    return f'''
      <div class="subgrid">
        <div class="grid-head-bar">{ic("network")}<strong>Sites y bases de datos</strong>
          <div class="spacer"></div>{btn("Añadir site", "plus", "sm")}</div>
        <table class="grid editable">
          <thead><tr><th style="width:2rem"><span class="sr-only">Mover</span></th><th>Site</th><th>Base de datos</th>
            <th style="width:5rem">Orden</th><th style="width:5rem"><span class="sr-only">Acciones</span></th></tr></thead>
          <tbody>
            <tr><td><span class="grip">{ic("grip")}</span></td><td class="cell-strong">Madrid</td><td>AWE DB 1 <span class="mono">awedb1</span></td>
              <td style="text-align:center">1</td><td>{row_actions("Madrid")}</td></tr>
            <tr><td><span class="grip">{ic("grip")}</span></td><td class="cell-strong">Oñate</td><td>AWE DB 2 <span class="mono">awedb2</span></td>
              <td style="text-align:center">2</td><td>{row_actions("Oñate")}</td></tr>
            {first}
          </tbody>
        </table>
      </div>'''


def panel(body, title="Planificador", sub="Módulo 3 · 2 perfiles, 2 usuarios · 2 sites", avatar="PL",
          status=True, tabs="", foot_primary="Guardar cambios", foot_icon="save", top="0"):
    st = tag("Activo", "success") if status else ""
    return f'''
  <div class="panel-mask"></div>
  <div class="panel panel--wide" role="dialog" aria-modal="true" aria-labelledby="panel-title" style="top:{top}">
    <div class="panel-head">
      <span class="avatar">{avatar}</span>
      <div class="titles"><h2 id="panel-title">{title}</h2><div class="sub">{sub}</div></div>
      {st}
      {btn("", "x", "ghost close", "Cerrar el panel")}
    </div>
    {tabs}
    <div class="panel-body">
{body}
    </div>
    <div class="panel-foot">
      <button class="btn">Cancelar</button>
      {btn(foot_primary, foot_icon, "primary")}
    </div>
  </div>'''


def state(letter, title, text, inner, height, badge=""):
    return f'''
<div class="state">
  <div class="state-label"><span class="letter">{letter}</span><h3>{title}</h3>{badge}<p>{text}</p></div>
  <div class="frame" style="height:{height}px">
{inner}
  </div>
</div>'''


def findings(title, items):
    lis = "\n".join(f"    <li>{i}</li>" for i in items)
    return f'''
<div class="findings">
  <h3>{title}</h3>
  <ol>
{lis}
  </ol>
</div>'''


TOOLS = '''
<div class="demo-tools">
  <span class="note">AWE 5 · Módulos · {label}</span>
  <div class="spacer"></div>
  <div class="segmented" role="group" aria-label="Modo de color">
    <button data-mode="light" aria-pressed="false" onclick="setMode('light')">Claro</button>
    <button data-mode="dark" aria-pressed="false" onclick="setMode('dark')">Oscuro</button>
  </div>
  <div class="segmented" role="group" aria-label="Densidad">
    <button data-density="comfortable" aria-pressed="true" onclick="setDensity('comfortable')">Cómoda</button>
    <button data-density="compact" aria-pressed="false" onclick="setDensity('compact')">Compacta</button>
  </div>
</div>'''

SCRIPT = '''
<script>
  function sync(attr, value) {
    document.querySelectorAll('[data-' + attr + ']').forEach(function (b) {
      b.setAttribute('aria-pressed', String(b.dataset[attr] === value));
    });
  }
  function setMode(mode) {
    document.documentElement.classList.toggle('dark', mode === 'dark');
    document.documentElement.classList.toggle('light', mode === 'light');
    sync('mode', mode);
  }
  function setDensity(density) {
    document.documentElement.setAttribute('data-awe-density', density);
    sync('density', density);
  }
  if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) { setMode('dark'); }
  else { setMode('light'); }
  sync('density', 'comfortable');
</script>'''


def page(title, label, content):
    head = HEAD.replace("<title>Sites · panel ancho con el grid de módulos</title>", f"<title>{title}</title>")
    return (f'{head}\n{EXTRA_CSS}\n</style>\n</head>\n<body>\n<main class="board" aria-label="{title}">'
            f'{TOOLS.format(label=label)}\n{content}\n</main>\n{SCRIPT}\n</body>\n</html>\n')


# ---------------------------------------------------------------------------------------------------------------
# 01 · Stacked panel (the Sites pattern, three sections)
# ---------------------------------------------------------------------------------------------------------------
def artboard_01():
    body = f'''{identity_section()}
      <div class="form-section">{ic("shield")}Acceso</div>
      <div class="stack">
{profiles_grid()}
{users_grid(editing=True)}
      </div>
      <div class="form-section">{ic("network")}Sites y bases de datos</div>
{sites_grid()}'''
    a = state("A", "La lista: qué es cada módulo y quién entra",
              "Cuatro columnas nuevas de resumen (acceso y sites) sustituyen al antiguo filtro por pantalla y tema.",
              shell(list_card(selected=None)), 760)
    b = state("B", "Editar: un panel con las tres secciones una debajo de otra",
              "El patrón de Sites, sin piezas nuevas. Las tres rejillas caben en 640px; el panel hace scroll.",
              shell(list_card(selected="Planificador"), overlay=panel(body)), 1260)
    f = findings("Qué cuesta y qué queda abierto", [
        "<strong>Ninguna pieza nueva en el framework.</strong> Es <code>site-admin.xml</code> con tres grids editables "
        "en vez de uno, y <code>ModUpd</code>/<code>ModNew</code> ya reciben las tres rejillas en una sola acción "
        "(<code>MdlUsrLst</code>, <code>MdlPrfLst</code>, <code>MdlSitDbsLst</code>).",
        "<strong>El panel es largo.</strong> Con un módulo real (10 perfiles, 30 usuarios) los sites quedan muy abajo; "
        "cada grid necesita su propia altura con scroll interno (<code>height-lg</code>) para que el panel no crezca sin fin.",
        "<strong>Acceso agrupa perfiles y usuarios</strong> porque eso es lo que decide el selector de módulo: "
        "<code>ModNamByOpeSel</code> une la asignación directa con la del perfil.",
        "<strong>Las columnas de resumen de la lista</strong> (perfiles, usuarios, sites) piden una consulta nueva "
        "con recuentos; <code>ModLst</code> no los trae.",
    ])
    return page("Módulos · panel apilado", "alternativa 01", a + b + f)


# ---------------------------------------------------------------------------------------------------------------
# 02 · Panel with tabs
# ---------------------------------------------------------------------------------------------------------------
def tabs(active, error_on=None):
    items = [("general", "General", None), ("access", "Acceso", "4"), ("sites", "Sites y BBDD", "2")]
    out = []
    for key, label, n in items:
        sel = "true" if key == active else "false"
        badge = f'<span class="n">{n}</span>' if n else ""
        err = '<span class="err" aria-label="Tiene errores"></span>' if key == error_on else ""
        out.append(f'<button class="tab" role="tab" aria-selected="{sel}">{label}{badge}{err}</button>')
    return f'<div class="tabs" role="tablist" aria-label="Secciones del módulo">{"".join(out)}</div>'


def artboard_02():
    general = identity_section(themes=False)
    access = f'''
      <div class="hint">{ic("info")}<span>Entran en <strong>Planificador</strong> los usuarios de estos perfiles y,
        además, los usuarios de la lista.</span></div>
      <div class="stack" style="margin-top:var(--awe-space-4)">
{profiles_grid(editing=True)}
{users_grid(themes=False)}
      </div>'''
    sites = f'''
      <div class="hint">{ic("info")}<span>La base de datos que usa el módulo en cada site. Es la misma relación que se
        edita desde <strong>Sites</strong>: un cambio aquí se ve allí.</span></div>
      <div style="margin-top:var(--awe-space-4)">
{sites_grid(editing=True, invalid=True)}
      </div>'''
    lst = shell(list_card(selected="Planificador", themes=False), overlay="")
    a = state("A", "General: los cuatro campos del módulo",
              "El panel abre siempre en General. Cada pestaña muestra cuántas filas tiene. Sin tema: nunca se aplicaba.",
              shell(list_card(selected="Planificador", themes=False), overlay=panel(general, tabs=tabs("general"))), 900,
              '<span class="badge badge--chosen">Elegida · medida en los dos motores</span>')
    b = state("B", "Acceso: perfiles y usuarios en la misma pestaña",
              "Perfiles primero (el caso normal), usuarios después como excepción. Una columna por rejilla.",
              shell(list_card(selected="Planificador", themes=False), overlay=panel(access, tabs=tabs("access"))), 900)
    c = state("C", "Sites y BBDD: editable, una fila sin guardar marca su pestaña",
              "Se edita aquí y en Sites. Un campo obligatorio vacío ya salta a su pestaña; una fila sin guardar solo avisa (el punto rojo pide una mejora del engine).",
              shell(list_card(selected="Planificador", themes=False), overlay=panel(sites, tabs=tabs("sites", error_on="sites"))), 900)
    f = findings("Lo que midió la pantalla de prueba (awe-boot y awe-boot-react, sin tocar el engine)", [
        "<strong>Las pestañas funcionan dentro del side panel de 640px</strong> en los dos motores, en claro y oscuro.",
        "<strong>Las rejillas de una pestaña que nunca se abre se envían igual:</strong> editar solo General conserva "
        "perfiles, usuarios y sites (comprobado en la BD). Añadir, editar y quitar filas dentro de una pestaña y crear "
        "un módulo nuevo con filas también funcionan.",
        "<strong>Errores en una pestaña oculta:</strong> un campo obligatorio vacío salta a su pestaña y la marca en los "
        "dos motores. Una fila sin guardar bloquea con <code>check-records-saved</code>, pero no salta ni marca la "
        "pestaña: el punto rojo de C es una mejora del engine, no algo que ya exista.",
        "<strong>Contador en la pestaña:</strong> React pinta <code>unit</code> como badge (rojo, de alerta) y AngularJS "
        "lo ignora (<code>tab.js:15-19</code>). Lo común es el número dentro de la etiqueta y solo se actualiza al reabrir.",
        "<strong>Detalles de XML:</strong> <code>IdeMod</code> como criterio oculto relleno desde la fila; volver a "
        "General al abrir el panel (AngularJS recuerda la última pestaña); rejillas de Acceso con <code>height-lg</code>.",
        "<strong>Sin tema hay que tocar dos cosas:</strong> <code>ModUpd</code>/<code>ModNew</code> dejan de escribir "
        "<code>IdeThm</code> (si no, lo ponen a NULL), y <code>QryLstUsrByMod</code> deja de cruzar con "
        "<code>AweThm</code> en INNER JOIN, o los usuarios nuevos (tema NULL) desaparecen de la lista.",
        "<strong>Defectos menores del engine vistos:</strong> en AngularJS las rejillas cargadas con la pestaña oculta "
        "salen más estrechas que su cabecera; en React la fila en edición de Sites hace scroll horizontal a 640px.",
        "<strong>Decidido: sin tema y con Sites y BBDD editable aquí</strong> (la misma relación "
        "<code>AweSitModDbs</code> que edita <code>site-admin.xml</code>).",
    ])
    return page("Módulos · panel con pestañas", "alternativa 02", a + b + c + f)


# ---------------------------------------------------------------------------------------------------------------
# 03 · List + detail page
# ---------------------------------------------------------------------------------------------------------------
def artboard_03():
    detail = f'''
      <div class="page-head">
        <div style="display:flex;gap:var(--awe-space-3);align-items:center">
          {btn("", "back", "ghost", "Volver a Módulos")}
          <span class="avatar">PL</span>
          <div><h1>Planificador</h1><p>Módulo 3 · pantalla inicial <span class="mono">scheduler-tasks</span></p></div>
          {tag("Activo", "success")}
        </div>
        <div class="actions">{btn("", "help", "ghost", "Ayuda")}{btn("", "trash", "ghost", "Borrar el módulo")}</div>
      </div>
      <section class="card">
        <div class="card-head"><h2>General</h2></div>
        <div class="card-body">
          <div class="form-row">
            <div class="field"><label>Nombre <span class="req">*</span></label><input value="Planificador"></div>
            <div class="field"><label>Pantalla inicial</label>{cell_select('<span class="mono">scheduler-tasks</span>')}</div>
            <div class="field"><label>Tema</label>{cell_select("Sunset")}</div>
            <div class="field"><label>Orden</label><input value="3"></div>
            <div class="field field--inline"><span class="switch" role="switch" aria-checked="true" aria-label="Activo"></span><label>Activo</label></div>
          </div>
        </div>
      </section>
      <div class="detail-grid">
        <section class="card"><div class="card-head"><h2>Perfiles</h2><span class="count">2</span>
          <div class="actions">{btn("Añadir perfil", "plus", "sm")}</div></div>
          <div class="card-body card-body--flush">
          <table class="grid editable"><thead><tr><th>Perfil</th><th style="width:5rem"><span class="sr-only">Acciones</span></th></tr></thead>
            <tbody><tr><td><span class="cell-strong">ADM</span> · Administrador</td><td>{row_actions("ADM")}</td></tr>
            <tr><td><span class="cell-strong">OPE</span> · Operador de planificación</td><td>{row_actions("OPE")}</td></tr></tbody></table></div></section>
        <section class="card"><div class="card-head"><h2>Usuarios</h2><span class="count">2</span>
          <div class="actions">{btn("Añadir usuario", "plus", "sm")}</div></div>
          <div class="card-body card-body--flush">
          <table class="grid editable"><thead><tr><th>Usuario</th><th style="width:10rem">Tema en el módulo</th><th style="width:5rem"><span class="sr-only">Acciones</span></th></tr></thead>
            <tbody><tr class="editing"><td>{cell_select("jmartin · Julia Martín")}</td><td>{cell_select("Sunset")}</td><td>{editing_actions()}</td></tr>
            <tr><td><span class="cell-strong">lgarcia</span><span class="cell-sub">Luis García</span></td><td>Clean</td><td>{row_actions("lgarcia")}</td></tr></tbody></table></div></section>
      </div>
      <section class="card"><div class="card-head"><h2>Sites y bases de datos</h2><span class="count">2</span>
        <div class="actions">{btn("Añadir site", "plus", "sm")}</div></div>
        <div class="card-body card-body--flush">
        <table class="grid editable"><thead><tr><th style="width:2rem"><span class="sr-only">Mover</span></th><th>Site</th><th>Base de datos</th><th style="width:6rem">Orden</th><th style="width:5rem"><span class="sr-only">Acciones</span></th></tr></thead>
          <tbody><tr><td><span class="grip">{ic("grip")}</span></td><td class="cell-strong">Madrid</td><td>AWE DB 1 <span class="mono">awedb1</span></td><td style="text-align:center">1</td><td>{row_actions("Madrid")}</td></tr>
          <tr><td><span class="grip">{ic("grip")}</span></td><td class="cell-strong">Oñate</td><td>AWE DB 2 <span class="mono">awedb2</span></td><td style="text-align:center">2</td><td>{row_actions("Oñate")}</td></tr></tbody></table></div></section>'''
    bar = f'''
    <div class="save-bar"><span class="dirty" aria-hidden="true"></span><span>1 fila sin guardar en Usuarios</span>
      <div class="spacer"></div><button class="btn">Descartar</button>{btn("Guardar cambios", "save", "primary")}</div>'''
    a = state("A", "La lista, igual que en las otras alternativas",
              "El lápiz y la fila no abren un panel: navegan a la página del módulo.",
              shell(list_card()), 760)
    b = state("B", "La página del módulo: las tres rejillas a lo ancho",
              "Perfiles y usuarios lado a lado; sites a todo el ancho. Una barra de guardado fija abajo.",
              shell(detail, crumb="Modules / Planificador") + bar, 1180)
    f = findings("Qué cuesta y qué queda abierto", [
        "<strong>La mejor superficie para las rejillas</strong>: nada de scroll dentro del panel, y Acceso se lee de un vistazo.",
        "<strong>Rompe el patrón de las demás ventanas admin</strong> (una ventana con panel lateral): son dos pantallas "
        "y se pierde la lista de contexto mientras se edita.",
        "<strong>La barra de guardado fija</strong> no existe en el framework; sin ella vuelven los botones de la "
        "cabecera de <code>ModUpd</code>.",
        "<strong>Crear un módulo</strong> abre la misma página vacía: es la única alternativa donde nuevo y editar no "
        "comparten superficie con la lista.",
    ])
    return page("Módulos · lista y página de detalle", "alternativa 03", a + b + f)


# ---------------------------------------------------------------------------------------------------------------
# 04 · Panel with tabs, Access as two picklists
# ---------------------------------------------------------------------------------------------------------------
PICKLIST_CSS = """
.picklist { display: grid; grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr); gap: var(--awe-space-3);
  align-items: stretch; }
.pl-list { border: var(--awe-border-width) solid var(--awe-color-border); border-radius: var(--awe-radius-lg);
  display: flex; flex-direction: column; min-width: 0; background: var(--awe-color-surface); }
.pl-head { display: flex; align-items: center; gap: var(--awe-space-2); padding: var(--awe-space-2) var(--awe-space-3);
  background: var(--awe-color-surface-subtle); border-bottom: var(--awe-border-width) solid var(--awe-color-border-subtle);
  font-size: var(--awe-font-size-xs); font-weight: var(--awe-font-weight-semibold); text-transform: uppercase;
  letter-spacing: var(--awe-letter-spacing-caps); color: var(--awe-color-text-muted);
  border-radius: var(--awe-radius-lg) var(--awe-radius-lg) 0 0; }
.pl-head .n { margin-left: auto; text-transform: none; letter-spacing: 0; font-weight: var(--awe-font-weight-normal); }
.pl-filter { margin: var(--awe-space-2); display: flex; align-items: center; gap: var(--awe-space-2);
  height: var(--awe-size-control-sm); padding: 0 var(--awe-space-2); border: var(--awe-border-width) solid var(--awe-color-field-border);
  border-radius: var(--awe-radius-field); background: var(--awe-color-field); font-size: var(--awe-font-size-sm);
  color: var(--awe-color-field-placeholder); }
.pl-items { list-style: none; margin: 0; padding: var(--awe-space-1); display: flex; flex-direction: column;
  gap: var(--awe-space-0-5); height: 13rem; overflow: auto; }
.pl-item { display: flex; align-items: center; gap: var(--awe-space-2); padding: var(--awe-list-option-padding);
  border-radius: var(--awe-radius-list-option); font-size: var(--awe-font-size-sm); cursor: pointer; min-width: 0; }
.pl-item:hover { background: var(--awe-color-list-option-focus); }
.pl-item[aria-selected="true"] { background: var(--awe-color-list-option-selected); color: var(--awe-color-primary-text); }
.pl-item .t { min-width: 0; }
.pl-item .t strong { font-weight: var(--awe-font-weight-medium); }
.pl-item .t span { display: block; font-size: var(--awe-font-size-xs); color: var(--awe-color-text-muted);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.pl-item .unit { margin-left: auto; font-size: var(--awe-font-size-xs); color: var(--awe-color-text-muted); white-space: nowrap; }
.pl-moves { display: flex; flex-direction: column; justify-content: center; gap: var(--awe-space-2); }
.pl-moves .btn { width: var(--awe-size-control-sm); height: var(--awe-size-control-sm); padding: 0; }
.pl-title { display: flex; align-items: center; gap: var(--awe-space-2); margin: 0 0 var(--awe-space-2);
  font-size: var(--awe-font-size-sm); font-weight: var(--awe-font-weight-semibold); }
.pl-title .muted { font-weight: var(--awe-font-weight-normal); font-size: var(--awe-font-size-xs); }
"""

ICONS.update({
    "right": '<path d="m9 18 6-6-6-6"/>',
    "left": '<path d="m15 18-6-6 6-6"/>',
    "rights": '<path d="m6 17 5-5-5-5"/><path d="m13 17 5-5-5-5"/>',
    "lefts": '<path d="m11 17-5-5 5-5"/><path d="m18 17-5-5 5-5"/>',
})


def pl_items(items, selected_label=None):
    out = []
    for label, desc, unit in items:
        sel = ' aria-selected="true"' if label == selected_label else ' aria-selected="false"'
        u = f'<span class="unit">{unit}</span>' if unit else ""
        out.append(f'<li class="pl-item" role="option"{sel}><span class="t"><strong>{label}</strong>'
                   f'<span>{desc}</span></span>{u}</li>')
    return "".join(out)


def picklist(title, sub, icon, source, target, source_head, filter_text, selected=None):
    return f'''
      <div>
        <p class="pl-title">{ic(icon)}{title}<span class="muted">{sub}</span></p>
        <div class="picklist">
          <div class="pl-list"><div class="pl-head">{source_head}<span class="n">{len(source)}</span></div>
            <div class="pl-filter">{ic("search")}{filter_text}</div>
            <ul class="pl-items" role="listbox" aria-label="{source_head}">{pl_items(source, selected)}</ul></div>
          <div class="pl-moves">{btn("", "right", "sm", "Dar acceso a lo seleccionado")}{btn("", "rights", "sm", "Dar acceso a todos")}
            {btn("", "left", "sm", "Quitar el acceso a lo seleccionado")}{btn("", "lefts", "sm", "Quitar el acceso a todos")}</div>
          <div class="pl-list"><div class="pl-head">Con acceso<span class="n">{len(target)}</span></div>
            <div class="pl-filter">{ic("search")}Filtrar</div>
            <ul class="pl-items" role="listbox" aria-label="Con acceso">{pl_items(target)}</ul></div>
        </div>
      </div>'''


def artboard_04():
    profiles = picklist("Perfiles", "Todos sus usuarios entran", "shield",
                        [("GEN", "General", "14 usuarios"), ("TST", "Tester", "3 usuarios"),
                         ("AUD", "Auditoría", "2 usuarios"), ("RRHH", "Recursos humanos", "6 usuarios")],
                        [("ADM", "Administrador", "2 usuarios"), ("OPE", "Operador de planificación", "9 usuarios")],
                        "Perfiles", "Buscar perfil", selected="GEN")
    users = picklist("Usuarios", "Además de los de sus perfiles", "user",
                     [("agomez", "Ana Gómez · GEN", ""), ("ctorres", "Carlos Torres · TST", ""),
                      ("dperez", "Diana Pérez · RRHH", ""), ("eruiz", "Elena Ruiz · GEN", ""),
                      ("fnavarro", "Fernando Navarro · AUD", "")],
                     [("jmartin", "Julia Martín · GEN", "Tema Sunset"), ("lgarcia", "Luis García · TST", "")],
                     "Usuarios", "Buscar por usuario o nombre")
    access = f'''
      <div class="hint">{ic("info")}<span>Entran en <strong>Planificador</strong> los usuarios de estos perfiles y,
        además, los usuarios de la lista. Pasa a la derecha lo que deba tener acceso.</span></div>
      <div class="stack" style="margin-top:var(--awe-space-4);gap:var(--awe-space-6)">
{profiles}
{users}
      </div>'''
    a = state("A", "Acceso con dos list pickers: elegir, no editar filas",
              "Sin añadir fila, select en celda, guardar fila. Se mueve de Disponibles a Con acceso y se guarda con el panel.",
              shell(list_card(selected="Planificador"), overlay=panel(access, tabs=tabs("access"))), 1000,
              '<span class="badge badge--dropped">Descartada por ahora · picklist solo en React</span>')
    f = findings("Qué cuesta y qué queda abierto", [
        "<strong><code>picklist</code> solo existe en React</strong> (<code>AwePicklist.jsx</code>; el XSD y "
        "<code>criteria.md</code> lo dicen). Para usarlo en una pantalla genérica hay que implementarlo en AngularJS.",
        "<strong>Al de React le falta el filtro</strong> de cada lista (con cientos de usuarios es imprescindible; "
        "PrimeReact lo trae con <code>filterBy</code>). Los botones de mover ya salen (solo oculta los de ordenar) "
        "y la cabecera de la lista elegida es fija (<code>SCREEN_TEXT_SELECTED</code>).",
        "<strong>El tema por usuario no cabe en un picklist</strong>: <code>AweModOpe.IdeThm</code> es un dato por fila. "
        "O se deja de usar desde esta ventana (el tema se queda en el usuario y en el módulo), o se muestra solo como "
        "<code>unit</code> de lectura, como <em>Tema Sunset</em> en jmartin.",
        "<strong>Cambia la forma de guardar:</strong> el picklist envía la lista de valores elegidos, no operaciones de fila. "
        "<code>ModUpd</code> pasaría a borrar e insertar los perfiles y usuarios del módulo en vez de usar "
        "<code>send-operations</code>; la auditoría registraría bajas y altas aunque no cambien.",
        "<strong>Sites y BBDD sigue siendo grid editable</strong>: cada fila tiene tres datos (site, base de datos, orden).",
    ])
    return page("Módulos · acceso con list picker", "alternativa 04", a + f).replace(
        "</style>\n</head>", PICKLIST_CSS + "</style>\n</head>", 1)


if __name__ == "__main__":
    (OUT / "04-acceso-picklist.html").write_text(artboard_04(), encoding="utf-8")
    (OUT / "01-panel-apilado.html").write_text(artboard_01(), encoding="utf-8")
    (OUT / "02-panel-pestanas.html").write_text(artboard_02(), encoding="utf-8")
    (OUT / "03-pagina-detalle.html").write_text(artboard_03(), encoding="utf-8")
    print("built", sorted(p.name for p in OUT.glob("*.html")))
