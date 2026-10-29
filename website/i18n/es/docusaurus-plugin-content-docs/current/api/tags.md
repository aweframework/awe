---
id: tags
title: Tag
---

Un `tag` es el bloque básico de construcción de una pantalla. Hace dos trabajos:

* **Estructura.** Con un `type` genera un elemento HTML (`div`, `span`, `h3`...) que agrupa otros elementos, aplica
  clases CSS y, con `expandible`, [distribuye](layout.md) a sus hijos.
* **Punto de origen.** Con un `source` rellena uno de los lugares de la [plantilla](template.md) de la pantalla (`center`,
  `buttons`, `modal`, `hidden`), o marca una pieza que otras pantallas pueden recoger con un [`include`](include.md).

## Esqueleto XML {#xml-skeleton}

```xml
<tag type="[type]" label="[label]" style="[style]" id="[id]" source="[source]" expandible="[expand-direction]">
  ...
</tag>
```

Un tag puede contener cualquier elemento de pantalla: otros tags, [ventanas](window.md), [grids](grids.md), [criterios](criteria.md),
[botones](button.md), [includes](include.md)...

Existe una forma especial que solo contiene texto:

```xml
<tag type="p"><text>[text]</text></tag>
```

## Atributos del tag {#tag-attributes}

| Atributo   | Uso      | Tipo   | Descripción                                              | Valores                                                                     |
| ---------- | -------- | ------ | -------------------------------------------------------- | --------------------------------------------------------------------------- |
| source     | Opcional | String | Punto de origen de la plantilla que rellena el tag       | `center`, `buttons`, `modal`, `hidden`... consulta [plantilla](template.md) |
| type       | Opcional | String | Elemento HTML a generar                                  | `div`, `span`, `p`, `ul`, `h3`... cualquier nombre de elemento HTML         |
| label      | Opcional | String | Texto del tag                                            | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales) |
| style      | Opcional | String | Clase o clases CSS a aplicar al elemento                 |                                                                             |
| id         | Opcional | String | Identificador del tag. Útil para referencias externas    |                                                                             |
| expandible | Opcional | String | Cómo [expandir](layout.md) los hijos del tag             | `vertical`, `horizontal`                                                    |
| icon       | Opcional | String | Icono decorativo dibujado antes de la `label`. Ver [títulos de sección](#section-titles) | **Nota:** Puedes consultar todos los iconos en la pantalla de [iconos](icons.md) |

Un `source` se puede usar **una vez** por pantalla, y los tags que tienen un `source` son hijos directos de `screen`.

## Qué genera el tag {#what-the-tag-generates}

* **Con `type`** el tag genera `<type id="..." class="...">...</type>`. Las clases son el `style` más
  `expandible-vertical` o `expandible-horizontal` cuando se indica `expandible`.
* **Sin `type`** el tag no necesita generar ningún elemento propio. Así se define un punto de origen
  (`<tag source="center">...</tag>`) o se agrupan elementos que no deben envolverse (consulta las diferencias más abajo).
* **El texto** del tag es su `label` (traducido con los locales de la aplicación) o, cuando no hay `label`,
  el elemento `<text>`.
* **Los hijos** son los elementos dentro del tag.

## Motores AngularJS y React {#angularjs-and-react-engines}

Ambos motores generan lo mismo para un tag:

| Caso                                           | Resultado en ambos motores                                                                      |
| ---------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| Tag sin `type` (y sin `source`)                | Ningún elemento HTML: su contenido se coloca en el padre. Su `style` y su `expandible` se ignoran |
| Tag con `label`                                | Solo se dibuja la etiqueta. El `<text>` y los hijos del tag se ignoran                          |
| Tag con `<text>` y sin `label`                 | Solo se dibuja el texto. Los hijos del tag se ignoran                                           |
| `type` y `expandible` de un tag con `source`   | Se aplican: el tag de origen genera su propio elemento dentro de la plantilla                   |

Las únicas diferencias que quedan son menores:

* El motor AngularJS dibuja el `label` dentro de un `span`; el motor React lo dibuja directamente en el elemento del tag.
* Un tag sin `type` que tiene un `style` lo ignora en ambos motores. Si necesitas el estilo, da un `type` al tag.
* En el motor React el `style` de un tag de origen **sin** `type` sigue aplicándose al contenedor que la plantilla dibuja
  para el origen. El motor AngularJS lo ignora. No dependas de ello: da un `type` al tag.

Escribe los tags de modo que el resultado sea claro en ambos motores:

* Da un `type` a cada tag que tenga un `style` o un `expandible`, para que sea un elemento real.
* Usa un tag con `label` solo para texto. Si necesitas texto junto a otros elementos, pon cada uno en su propio tag.
* Usa `<tag source="...">` solo para rellenar un punto de origen. Añade un `type` si necesita un `expandible`.

## Títulos de sección {#section-titles}

Dos estilos de tag convierten un tag de encabezado (`h2` a `h6`, para que los lectores de pantalla anuncien la sección)
con `label` en un título de sección. Los dos se ven igual en los dos motores cliente, en modo claro y oscuro:

* `awe-section-title` (#910): el título de sección del sistema de diseño, una etiqueta pequeña en mayúsculas y
  atenuada, seguida de una línea que ocupa el resto de la fila. Úsalo para dividir un formulario largo o un cuadro de
  mando en secciones; el estilo quita los márgenes por defecto del encabezado.
* `awe-form-section-title` (#793): el título de una sección de un formulario (un panel lateral, un diálogo, una
  ventana), como en el panel lateral de la pantalla `user-admin`: su `icon` y su etiqueta en una línea, con el tamaño
  de título de sección, y una línea de separación encima de cada título de sección de formulario salvo el primero de
  su contenedor.

```xml
<tag type="h3" style="awe-section-title" label="SCREEN_TEXT_RECORD_DETAIL"/>
```

```xml
<tag type="h3" style="col-xs-12 awe-form-section-title" label="SCREEN_TEXT_USER_IDENTITY" icon="user" />
<criteria label="PARAMETER_FULL_NAME" id="Nam" component="text" style="col-xs-12" />
<tag type="h3" style="col-xs-12 awe-form-section-title" label="SCREEN_TEXT_USER_SIGN_IN_SECURITY" icon="lucide:key-round" />
<criteria label="PARAMETER_PASSWD" id="Pas" component="password" style="col-xs-12" />
```

El `icon` de un tag es decorativo (oculto para los lectores de pantalla) y solo se dibuja en un tag con `label`, con
cualquiera de los dos estilos. El separador de `awe-form-section-title` va encima de un título de sección de formulario
que sigue a otro dentro del mismo contenedor, así que mantén los títulos y los criterios de un formulario como hermanos
(en el mismo `tag`).

## Ejemplos {#examples}

### Estructura de una ventana {#structure-of-a-window}

Los estilos `panel-body` y `panel-footer` dan a una [ventana](window.md) su contenido y su pie. Esta es la pantalla de la tabla dinámica
de las aplicaciones de pruebas:

```xml
<window label="SCREEN_TEXT_CRITERIA">
  <tag type="div" style="panel-body">
    <criteria component="date" id="fecha" label="PARAMETER_DATE" style="col-xs-6 col-sm-3 col-md-2"/>
  </tag>
  <tag type="div" style="panel-footer">
    <tag type="div" style="pull-right">
      <button button-type="submit" label="BUTTON_SEARCH" icon="search" id="ButSch">
        <button-action type="filter" target="listaDatos" />
      </button>
    </tag>
  </tag>
</window>
```

### Tags expandibles {#expandible-tags}

Un tag con `expandible` reparte el espacio entre sus hijos: los que tienen el estilo `expand` ocupan el espacio libre, los
demás mantienen su tamaño (consulta [layout](layout.md)):

```xml
<tag type="div" style="panel-body expand" expandible="horizontal">
  <tag type="div" style="p-2 expand">
    <criteria label="SCREEN_TEXT_USER" component="text" id="gegtr" validation="required" style="col-xs-12" />
    <criteria label="SCREEN_TEXT_PASS" component="password" id="ertge" validation="required" style="col-xs-12" />
  </tag>
  <tag type="div" style="p-2 staticWidthSample">
    <criteria label="SCREEN_TEXT_USER" icon="user" component="text" id="etertg" validation="required" style="col-xs-12" />
  </tag>
</tag>
```

### Tags de texto {#text-tags}

Un tag con `label` escribe texto traducido, y la forma `<text>` escribe el texto tal cual:

```xml
<tag type="h3" label="SCREEN_TEXT_USER_DATA"/>
<tag type="p">
  <text>Lorem ipsum dolor sit amet.</text>
</tag>
```

### Puntos de origen {#source-points}

```xml
<screen template="window" label="SCREEN_TITLE_QUE">
  <tag source="hidden"></tag>
  <tag source="buttons">
    <button label="BUTTON_SEND" icon="exchange" id="ButSndSyn">
      <button-action type="server" server-action="maintain" target-action="TstQueSndSyn" />
    </button>
  </tag>
  <tag source="center">
    ...
  </tag>
</screen>
```

Los ejemplos son copias de partes de las pantallas de pruebas `pivot-test` (estructura de una ventana), `layout` (tags expandibles),
`settings` (el tag `h3` con una etiqueta), `accordion` (la forma `<text>`, allí sin `type`) y `QueTst` (puntos de
origen) de `awe-tests/awe-boot-react`. `pivot-test` y `layout` también están en `awe-tests/awe-boot`.

## Páginas relacionadas {#related-pages}

* [Template](template.md): los puntos de origen de cada plantilla.
* [Layout](layout.md): el atributo `expandible` y los estilos `expand`.
* [Include](include.md): recoge un origen de otra pantalla.
* [Window](window.md): el contenedor habitual de tags con `panel-body` y `panel-footer`.
* [Screen](screen.md): el elemento que contiene los tags de origen.

_Revisado para AWE 5._
