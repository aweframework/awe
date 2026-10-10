---
id: layout
title: Layout
---

El layout de AWE ayuda al desarrollador a posicionar las partes de una pantalla y aprovechar todo el tamaño de la pantalla. Se
construye sobre el flexible box layout de CSS: un **contenedor** indica en qué dirección dispone a sus hijos, y los
hijos indican si ocupan el espacio libre o mantienen su tamaño.

Hay dos formas de "expandir" un contenedor, que se definen con el atributo `expandible`:

* **[Verticalmente](#vertical-layout):** Con el atributo `expandible="vertical"`, los **hijos directos** con un estilo `expand` aumentarán su altura para ajustarse al tamaño del contenedor. Todos los hijos sin la clase `expand` mantendrán su altura.
* **[Horizontalmente](#horizontal-layout):** Con el atributo `expandible="horizontal"`, los **hijos directos** con un estilo `expand` aumentarán su anchura para ajustarse al tamaño del contenedor. Todos los hijos sin la clase `expand` mantendrán su anchura.

El atributo `expandible` genera la clase CSS `expandible-vertical` o `expandible-horizontal` en el elemento, y
los estilos `expand` son clases CSS que escribes en el atributo `style` de los hijos.

## Dónde usarlo {#where-to-use-it}

Los contenedores habituales de un layout son estos (el esquema también declara `expandible` para otros elementos de tipo `tag`, como
`dialog`, `tabcontainer` y `wizard-panel`):

| Elemento                         | Notas                                                                                                         |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| [`tag`](tags.md)                 | El contenedor habitual. Dale un `type` (`div`) para que sea un elemento real en ambos motores                 |
| [`window`](window.md)            | Motor AngularJS: el contenido de la ventana es vertical por defecto. Motor React: se ignora, siempre es vertical |
| [`resizable`](resizable.md)      | Declarado en el esquema, pero ningún cliente lo usa. Pon un `tag` con `expandible` dentro del resizable       |

En las [plantillas](template.md) `window` y `document` el source `center` ya es un contenedor vertical, por lo que un
`window` o un `tag` con el estilo `expand` colocado directamente en él ocupa la altura que queda.

> **Nota:** El layout funciona con **hijos directos**. Si un elemento `expand` está dentro de otro elemento que no es
> expandible, no crece: expande cada nivel desde la pantalla hasta el elemento.

## Estilos de expansión {#expansion-styles}

Estos estilos van en el atributo `style` de los hijos de un contenedor expandible:

| Estilo                | Efecto                                                                                                              |
| --------------------- | ------------------------------------------------------------------------------------------------------------------- |
| `expand`              | El hijo ocupa su parte del espacio libre                                                                            |
| `expand-2x`           | El hijo ocupa el doble de espacio que un único `expand`                                                             |
| `expand-3x` ... `expand-12x` | El hijo ocupa tres, cuatro... hasta doce veces el espacio de un único `expand`                               |
| `expand-maximize`     | El hijo se expande **solo** cuando la [ventana](window.md) *padre* está maximizada. Solo en el motor AngularJS      |

Los hijos sin ninguno de estos estilos mantienen su propio tamaño.

## Layout vertical {#vertical-layout}

En los siguientes ejemplos puedes ver un elemento con el atributo `expandible="vertical"`. Las cajas rojas son hijos sin el estilo `expand`, y las cajas azules son hijos con el estilo `expand`:

### Dos hijos expandibles y un hijo estático {#two-expandible-children-and-one-static-child}
<img alt="Layout vertical 1" src={require('@docusaurus/useBaseUrl').default('img/Layout_vertical_1.png')} />

### Un hijo expandible y varios hijos estáticos {#one-expandible-child-and-some-static-children}
<img alt="Layout vertical 2" src={require('@docusaurus/useBaseUrl').default('img/Layout_vertical_2_1.png')} />

La ventana de datos de la pantalla de pruebas de la tabla pivote es un layout vertical: la ventana y su `tag` se expanden, por lo que la tabla
pivote (con `style="expand"`) usa toda la altura que la ventana de criterios no necesita:

```xml
<window label="SCREEN_TEXT_DATA" style="expand" expandible="vertical">
  <tag type="div" style="panel-body expand scrollable-both" expandible="vertical">
    <pivot-table id="listaDatos" initial-load="query" target-action="QryUniTstId" style="expand"/>
  </tag>
</window>
```

## Layout horizontal {#horizontal-layout}

En los siguientes ejemplos puedes ver un elemento con el atributo `expandible="horizontal"`. Las cajas rojas son hijos sin el estilo `expand`, y las cajas azules son hijos con el estilo `expand`:

### Dos hijos expandibles y un hijo estático {#two-expandible-children-and-one-static-child-1}
<img alt="Layout horizontal" src={require('@docusaurus/useBaseUrl').default('img/Layout_horizontal.png')} />

#### Un hijo expandible y varios hijos estáticos {#one-expandible-child-and-some-static-children-1}
<img alt="Layout horizontal 2" src={require('@docusaurus/useBaseUrl').default('img/Layout_horizontal_2.png')} />

Esta parte de la pantalla de pruebas `layout` tiene un contenedor con dos hijos que se reparten la anchura y un hijo que mantiene
su tamaño (`staticWidthSample` es una clase CSS que fija una anchura):

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

## Layout combinado {#combined-layout}

Para diseñar una pantalla de aplicación puedes combinar el uso de layouts verticales y horizontales con hijos expandibles y no expandibles:

<img alt="Layout combinado" src={require('@docusaurus/useBaseUrl').default('img/Combined_layout.png')} />

<img alt="Layout combinado 2" src={require('@docusaurus/useBaseUrl').default('img/Combined_layout_2.png')} />

<img alt="Layout combinado 3" src={require('@docusaurus/useBaseUrl').default('img/Combined_layout_3.png')} />

La pantalla `MatTst` de las aplicaciones de prueba combina ambos (el ejemplo está recortado): un contenedor vertical cuyo primer
hijo es un contenedor [resizable](resizable.md) (dispuesto horizontalmente en su interior) y cuyo segundo hijo es un grid que
se expande:

```xml
<tag type="div" style="expand expandible-vertical">
  <resizable directions="bottom" style="no-resizable-xs height-lg">
    <tag type="div" style="expand expandible-horizontal">
      <resizable directions="right" style="col-xs-12 col-sm-4 col-lg-2 no-padding no-resizable-xs">
        <grid id="MatSel" style="expand grid-bordered" initial-load="query" server-action="data" target-action="QryUniTst" max="10">
          <column label="COLUMN_SIT" sort-field="Als" name="Als5" charlength="25" />
        </grid>
      </resizable>
      <grid id="MatPrn" style="expand grid-bordered" initial-load="query" server-action="data" target-action="QryUniTst" max="10">
        <column label="COLUMN_SIT" sort-field="Als" name="Als6" charlength="25" />
      </grid>
    </tag>
  </resizable>
  <grid id="MatCol" style="expand grid-bordered" initial-load="query" server-action="data" target-action="QryUniTst" max="10">
    <column label="COLUMN_SIT" sort-field="Als" name="Als17" charlength="25" />
  </grid>
</tag>
```

## Motores AngularJS y React {#angularjs-and-react-engines}

El atributo `expandible` y los estilos `expand` funcionan en ambos motores, con estas diferencias:

| Característica                           | Motor AngularJS                                       | Motor React                                        |
| ---------------------------------------- | ----------------------------------------------------- | -------------------------------------------------- |
| `expand`, `expand-2x` a `expand-12x`     | Se aplica                                             | Se aplica                                          |
| `expandible` de un `tag` con `type`      | Se aplica                                             | Se aplica                                          |
| `expandible` de un `tag` sin `type`      | Se pierde: un tag sin `type` no genera ningún elemento | Se aplica al `div` que genera el tag de React     |
| `expandible` de un `window`              | Se aplica (vertical por defecto)                      | Se ignora: la ventana es siempre vertical          |
| `expandible` de un tag con `source`      | Se aplica (si el tag tiene un `type`)                 | Se ignora: la plantilla solo usa el `style`        |
| `expand-maximize`                        | Se aplica                                             | Se ignora: la ventana no tiene maximizar           |

Para que una pantalla siga funcionando en ambos motores, da un `type` a los tags con `expandible`, y pon los layouts horizontales
dentro de un `tag` en lugar de directamente en una ventana o en un tag con source.

## Páginas relacionadas {#related-pages}

* [Tags](tags.md): el atributo `expandible` del tag.
* [Ventana](window.md) y [resizable](resizable.md): los contenedores que suelen formar parte de un layout.
* [Plantilla](template.md): los sources donde empieza el layout.

_Revisado para AWE 5._
