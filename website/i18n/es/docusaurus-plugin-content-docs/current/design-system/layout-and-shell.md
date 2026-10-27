---
id: layout-and-shell
title: Estructura de la aplicación
sidebar_label: Estructura de la aplicación
---

El marco de la aplicación en AWE 5: menú lateral plegable, cabecera clara, breadcrumb y contenido en tarjetas. Las
opciones de menú y el XML de las pantallas no cambian; solo cambia la presentación. Puede verlo en la
[pantalla de ejemplo](pathname:///design-system/prototype/).

```text
┌──────────────┬──────────────────────────────────────────────────┐
│ Zona de marca│ Cabecera: menú · búsqueda global · ayuda ·       │
│              │           notificaciones · menú de usuario       │
├──────────────┼──────────────────────────────────────────────────┤
│ Buscar opción│ Breadcrumb                                       │
│ Sección      │ Título de página + acciones                      │
│  Opción      │ ┌ Tarjeta ────────────┐ ┌ Tarjeta ────────────┐  │
│   Subopción  │ │                     │ │                     │  │
│    Sub-sub   │ └─────────────────────┘ └─────────────────────┘  │
│ Powered by   │                                                  │
└──────────────┴──────────────────────────────────────────────────┘
```

## Zona de marca {#brand-slot}

Arriba a la izquierda del menú lateral. Contiene el logo **del cliente**, como hacen hoy los productos de AWE.

| Variante | Contenido | Cuándo |
|----------|-----------|--------|
| Logo horizontal | El logo horizontal del cliente | Por defecto |
| Marca + nombre | Marca cuadrada más el nombre del producto y del cliente | El logo es cuadrado o no se lee en pequeño |
| Logo de AWE | El logo de AWE | No hay logo de cliente configurado |
| Rail | Solo la marca cuadrada | El menú está plegado |

- El logo tiene una variante clara y otra oscura, o una que funcione sobre los dos fondos.
- La atribución a AWE es pequeña y secundaria: "Powered by AWE 5" con la marca de AWE, en el pie del menú.
- Altura del logo: `--awe-header-logo-height` (34px en cómoda, 30px en compacta).

## Menú lateral {#side-menu}

Hasta **tres niveles** por debajo de una sección: sección › opción › subopción › sub-subopción.

| Elemento | Regla |
|----------|-------|
| Etiqueta de sección | Texto pequeño en mayúsculas y atenuado. No es clicable. |
| Opción de nivel 1 | Icono + etiqueta (+ badge). Las opciones con hijos muestran un chevron. |
| Niveles 2 y 3 | Sin icono. Con sangría y una línea guía vertical por nivel. Más pequeños y atenuados. |
| Rastro activo | Cada antecesor de la página actual queda marcado (peso, barra de acento en la guía); la propia página tiene el fondo de resaltado. |
| Expandir / plegar | Por nivel y animado (`--awe-motion-duration-expand`). Los niveles abiertos se restauran sin animación. |
| Búsqueda | Filtra a cualquier profundidad, abre la ruta de cada coincidencia y resalta el texto encontrado. |
| Badges | Contadores (neutros o de peligro) y "New". |

**Rail.** El menú se pliega a una barra de iconos (`--awe-menu-width-collapsed`, 68px) y el cambio de ancho se anima.
Al pasar el ratón, enfocar o pulsar una opción de nivel 1 se abre un **flyout** con sus niveles 2 y 3. Teclado: Enter o
→ lo abre y enfoca el primer enlace, ↑/↓ se mueven, Esc o ← lo cierran y devuelven el foco.

**Tableta y menor (≤1024px).** El menú pasa a ser un cajón sobre el contenido con máscara. El rail solo existe en
escritorio.

## Cabecera {#header}

- Clara, ligeramente translúcida, fija al desplazar, de altura `--awe-header-height`.
- Izquierda: botón de menú y búsqueda global (`Ctrl K` la enfoca).
- Derecha: ayuda, notificaciones (punto cuando hay sin leer; lista en un popover) y menú de usuario (perfil,
  preferencias, atajos, cambiar de empresa, cerrar sesión).

## Página {#page}

| Elemento | Regla |
|----------|-------|
| Breadcrumb | Icono de inicio y cada nivel del menú hasta la página. La página actual no es un enlace. |
| Título de página | `--awe-font-size-page-title` y una línea opcional de descripción en texto atenuado. |
| Acciones de página | A la derecha del título. Como mucho una acción primaria; el resto secundarias, con borde, de texto o solo icono. |
| Tarjetas | Cada bloque (criterios, grid, pestañas) es una tarjeta: superficie, borde, `--awe-radius-card`, `--awe-shadow-card`. |
| Cabecera de tarjeta | Título (+ subtítulo) y acciones de la tarjeta. Un separador cuando el cuerpo es un formulario o un grid. |
| Rejilla de tarjetas | `--awe-layout-gap` entre tarjetas. Las dos columnas pasan a una por debajo de 1280px. |
| Maximizar | Una tarjeta puede ocupar el área de contenido; el menú y la cabecera siguen visibles. |

## Página de login {#login-page}

Puede verla en el [prototipo del login](pathname:///design-system/prototype/login.html).

| Zona | Contenido |
|------|-----------|
| Columna izquierda | Logo del cliente, formulario centrado verticalmente (máximo 420px) y pie con copyright, "Powered by AWE 5", selector de idioma y de modo |
| Columna derecha | Panel de imagen con un pie (título y una línea) sobre una capa oscura |
| Por debajo de 960px | El panel de imagen se oculta; el formulario queda centrado, máximo 400px |

Formulario: título "Sign in to the platform" en `--awe-font-size-display`, usuario, contraseña con mostrar/ocultar,
"Forgot password?", "Sign in" primario a todo el ancho, separador "or continue with" y un botón con borde a todo el ancho
para el inicio de sesión único.

**Hueco de imagen.** Un producto fija su propia imagen con `--awe-login-image` (un `url()`) o con el hueco
`<img class="login__img">` (`object-fit: cover`). Por defecto: el logo de AWE sobre un degradado oscuro de marca
(`--awe-login-brand-background`). Otras opciones: un degradado simple del primario (`--awe-login-gradient`) o sin
imagen (formulario centrado y solo). La capa del pie mantiene su texto por encima de 10:1 con cualquier primario;
compruébelo con su propia imagen. En el motor AngularJS las propiedades
`awe.application.paths.image-startup-background` y `awe.application.paths.image-startup-logo` siguen vigentes: cuando
un producto cambia sus valores por defecto, la imagen llena el panel y el logo sustituye al logo de AWE del formulario.

**Inicio de sesión único.** El botón es genérico ("Continue with your organization", icono de llave). No use logos ni
marcas de terceros.

| Estado | Comportamiento |
|--------|----------------|
| Errores de validación | Campos obligatorios marcados, mensaje bajo cada uno, foco en el primero |
| Credenciales incorrectas | Mensaje de peligro arriba del formulario, contraseña vaciada y enfocada |
| Bloq Mayús activado | Aviso bajo la contraseña mientras se escribe |
| Cargando | Spinner en el botón, "Signing in…", campos deshabilitados, `aria-busy` |
| Contraseña caducada | Paso "Change your password": campos nueva y confirmación, lista de requisitos en vivo (#849) |
| SSO en curso | Vista de progreso con Cancelar |

Los campos usan `autocomplete="username"`, `current-password` y `new-password`. Enter envía el formulario.
