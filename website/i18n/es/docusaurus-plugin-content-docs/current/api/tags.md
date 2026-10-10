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

Un tag puede contener cualquier elemento de pantalla: otros tags, [ventanas](window.md), [rejillas](grids.md), [criterios](criteria.md),
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

| Caso                                           | Motor AngularJS                                                               | Motor React                                                                     |
| ---------------------------------------------- | ----------------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| Tag sin `type` (y sin `source`)                | Ningún elemento HTML: los hijos se colocan en el padre                        | Se genera un `div`                                                              |
| Tag con `label` **y** con hijos                | Solo se dibuja la etiqueta (dentro de un `span`); los hijos se ignoran        | Se dibujan la etiqueta, el `<text>` y los hijos, en este orden                  |
| `type` y `expandible` de un tag con `source`   | Se aplican: el tag de origen genera su propio elemento dentro de la plantilla | No se aplican: la plantilla usa el `style` y los hijos del tag de origen        |

Por las dos primeras filas, escribe tags que se comporten igual en ambos motores:

* Da un `type` a cada tag que tenga un `style` o un `expandible`, para que sea un elemento real en ambos motores.
* Usa un tag con `label` solo para texto. Si necesitas texto junto a otros elementos, pon cada uno en su propio tag.
* Usa `<tag source="...">` solo para rellenar un punto de origen. Coloca el `div` con `expandible` **dentro** de él.

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
