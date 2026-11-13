---
id: include
title: Include
---

Un `include` toma un fragmento de otra pantalla y lo coloca dentro de la actual. Úsalo para reutilizar una parte de una pantalla
(un diálogo, una barra de navegación, un grupo de botones) en lugar de copiarla en cada pantalla que la necesite.

## Esqueleto de XML {#xml-skeleton}

```xml
<include target-screen="[screen-to-include]" target-source="[source-to-pick]" />
```

Un `include` no tiene elementos hijos. Puede colocarse en cualquier sitio donde el esquema admita un elemento de pantalla: dentro de un
[`tag`](tags.md), una [ventana](window.md), un [botón](button.md), un elemento [info](info.md)...

## Atributos {#attributes}

| Atributo      | Uso                  | Tipo   | Descripción                                                                          |
| ------------- | -------------------- | ------ | ------------------------------------------------------------------------------------ |
| target-screen | **Obligatorio**      | String | Identificador de la [pantalla](screen.md) de la que se toma el código (el nombre de su archivo XML, sin `.xml`) |
| target-source | **Obligatorio** (ver más abajo) | String | `source` del [tag](tags.md) de esa pantalla que contiene el código a tomar     |

El esquema marca `target-source` como opcional, pero el servidor rechaza un `include` sin él. El esquema `screen.xsd`
también declara un atributo `name` para el `include`, pero no tiene ningún efecto.

## Cómo funciona {#how-it-works}

El servidor resuelve los includes al leer la pantalla, por lo que **funciona igual en los motores AngularJS y React**: el cliente recibe la pantalla con los elementos incluidos ya en su sitio.

* El servidor busca en `target-screen` el `tag` **hijo directo** cuyo `source` coincide con `target-source` (la
  comparación ignora las mayúsculas y minúsculas).
* Coloca los **hijos** de ese tag donde está el `include`. El propio tag y la plantilla de la pantalla incluida
  no se copian.
* El código se copia tal cual, por lo que los identificadores de los elementos incluidos son los mismos en todas las pantallas que los incluyen.
  No deben chocar con los identificadores de la pantalla que los incluye.
* Una pantalla incluida puede incluir otras pantallas, pero una cadena que vuelve al mismo `target-screen` y
  `target-source` se rechaza con el error `ERROR_TITLE_NESTED_INCLUDE`.
* Si el source no existe en la pantalla, la pantalla falla al cargar con el error *Bad 'include' definition*, que
  indica la pantalla, el `target-screen` y el `target-source`. Si el `target-screen` no existe, el error es el
  de una pantalla no definida.

> **Nota:** Para que un fragmento sea reutilizable, ponlo en un `tag` con un `source` en una pantalla propia. Pantallas como
> `info-buttons` solo contienen fragmentos para incluir. Recuerda que cada source de una pantalla debe ser único (ver
> [template](template.md)).

## Ejemplos {#examples}

### Insertar el diálogo de opciones de impresión y el diálogo de ayuda de la pantalla {#insert-the-print-options-dialog-and-the-screen-help-dialog}

El source `modal` de la pantalla `ScrCnf` incluye dos diálogos definidos en otras pantallas:

```xml
<tag source="modal">
  <include target-screen="PrnOpt" target-source="center" />
  <include target-screen="screen-help" target-source="center"/>
</tag>
```

`PrnOpt` define `<tag source="center">` con el `dialog` de opciones de impresión, y `screen-help` el `dialog` de ayuda. Después de que el
servidor resuelva los includes, `ScrCnf` tiene ambos diálogos en su source `modal`.

### Insertar fragmentos de una barra de navegación {#insert-pieces-of-a-navigation-bar}

La pantalla `home_navbar` toma varios sources de la pantalla `info-buttons` y los coloca dentro de un `tag`:

```xml
<tag type="ul" style="nav navbar-nav pull-right right-navbar-nav">
  <include target-screen="info-buttons" target-source="favourites"/>
  <include target-screen="info-buttons" target-source="siteModuleDatabase"/>
  <include target-screen="info-buttons" target-source="logout"/>
</tag>
```

El primer ejemplo es el final de la pantalla `ScrCnf` y el segundo una parte recortada de `home_navbar`, ambos en las
pantallas genéricas de AWE (`awe-framework/awe-generic-screens`). Las pantallas genéricas de React usan el mismo patrón
(`home-sidebar`, `home-topbar`).

## Páginas relacionadas {#related-pages}

* [Template](template.md) y [tags](tags.md): el atributo `source` que hace reutilizable un fragmento.
* [Pantalla](screen.md): el elemento que contiene los sources.
* [Diálogo](dialog.md): el fragmento más habitual para incluir.

_Revisado para AWE 5._
