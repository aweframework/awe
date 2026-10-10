---
id: resizable
title: Redimensionable
---

Un redimensionable es un contenedor cuyo tamaño puede cambiar el usuario arrastrando uno de sus lados. Úsalo para dejar que el usuario decida cuánto espacio ocupa una parte de la pantalla, por ejemplo una lista de elementos a la izquierda del detalle que controla.

## Esqueleto XML {#xml-skeleton}

```xml
<resizable directions="[directions]" style="[style]">
  ...
</resizable>
```

Un redimensionable puede contener cualquier elemento de pantalla: [tags](tags.md), [ventanas](window.md), [rejillas](grids.md), [gráficos](chart.md),
[criterios](criteria.md), otros redimensionables...

## Atributos {#attributes}

| Atributo   | Uso          | Tipo   | Descripción                                              | Valores / por defecto                                                                              |
| ---------- | ------------ | ------ | -------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| directions | Opcional (defínelo) | String | Lados del contenedor que se pueden arrastrar      | `right`, `left`, `top`, `bottom`. En el motor AngularJS puedes indicar varios, separados por espacios |
| style      | Opcional     | String | Clases CSS del contenedor                                | Ver [estilos de tamaño y responsive](#size-and-responsive-styles)                                  |
| id         | Opcional     | String | Identificador del redimensionable. Con fines de referencia |                                                                                                  |

El esquema no obliga a indicar `directions`, pero sin él el motor AngularJS no dibuja ningún tirador, por lo que el redimensionable no se puede redimensionar. Indícalo siempre.

El esquema `screen.xsd` también declara `icon`, `label` y `expandible` para el redimensionable, pero ningún cliente los utiliza.

> **Nota:** Un redimensionable necesita un **ancho o alto inicial**, definido con una clase CSS en `style` (por ejemplo las clases de rejilla `col-xs-12 col-sm-4 col-lg-2`). El redimensionable cambia ese tamaño cuando el usuario arrastra el lado.

## Ejemplos {#examples}

### Redimensionable con un tirador a la derecha {#resizable-with-a-grabber-to-the-right}

El redimensionable de la pantalla de pruebas `MatTst`: una rejilla a la izquierda que el usuario puede ensanchar o estrechar, junto a otra rejilla que ocupa el resto del espacio.

```xml
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
```

<img alt="redimensionable" src={require('@docusaurus/useBaseUrl').default('img/resizable.png')} />

El `tag` padre tiene `expandible-horizontal` para que el redimensionable y la segunda rejilla compartan el ancho (consulta
[layout](layout.md)).

### Redimensionable con un tirador abajo {#resizable-with-a-grabber-at-the-bottom}

El estilo `height-lg` da al redimensionable su alto inicial (consulta la nota anterior).

```xml
<tag type="div" style="expand expandible-vertical">
  <resizable directions="bottom" style="no-resizable-xs height-lg">
    <grid id="MatSel" style="expand grid-bordered" initial-load="query" server-action="data" target-action="QryUniTst" max="10">
      <column label="COLUMN_SIT" sort-field="Als" name="Als5" charlength="25" />
    </grid>
  </resizable>
  <grid id="MatCol" style="expand grid-bordered" initial-load="query" server-action="data" target-action="QryUniTst" max="10">
    <column label="COLUMN_SIT" sort-field="Als" name="Als17" charlength="25" />
  </grid>
</tag>
```

Ambos ejemplos están adaptados de la pantalla `MatTst` de las aplicaciones de pruebas (`awe-tests/awe-boot` y
`awe-tests/awe-boot-react`): el primero conserva sus atributos, el segundo solo cambia el contenido del redimensionable.

## Estilos de tamaño y responsive {#size-and-responsive-styles}

En el motor AngularJS el redimensionable tiene un tamaño mínimo de 160 px y no puede crecer por encima del 99 % de su padre. Estos estilos desactivan el tirador cuando la pantalla es estrecha, de modo que en un teléfono el contenedor usa su tamaño normal:

| Estilo            | El redimensionable deja de ser redimensionable cuando la pantalla es |
| ----------------- | -------------------------------------------------------------------- |
| `no-resizable-xs` | más estrecha de 768 px                                               |
| `no-resizable-sm` | más estrecha de 992 px                                               |
| `no-resizable-md` | más estrecha de 1200 px                                              |
| `no-resizable-lg` | de 1200 px de ancho o más                                            |

Cuando el usuario suelta el tirador, el cliente lanza un redimensionado, de modo que las rejillas y los gráficos de la pantalla se adaptan a su nuevo tamaño.

## Motores AngularJS y React {#angularjs-and-react-engines}

Los dos motores no dibujan el redimensionable de la misma forma:

| Característica               | Motor AngularJS                                                                 | Motor React                                                                                      |
| ---------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| Qué se arrastra              | Los lados listados en `directions` del propio contenedor                        | El borde **entre** los hijos: cada hijo directo del redimensionable se convierte en un panel     |
| Varias direcciones           | Permitido (`directions="right bottom"`)                                         | No se usa: `top` y `bottom` dividen los hijos verticalmente, cualquier otro valor los divide horizontalmente |
| Un solo hijo                 | El contenedor es redimensionable                                                | No hay nada que arrastrar: el borde solo existe entre dos o más hijos                            |
| Estilos `no-resizable-*`     | Aplicados                                                                       | Ignorados                                                                                        |
| Librería                     | Directiva propia                                                                | [PrimeReact Splitter](https://primereact.org/splitter/)                                          |

Si una pantalla debe redimensionarse en ambos motores, coloca en el redimensionable **dos o más hijos** que compartan el espacio: el motor React los divide, y en el motor AngularJS se apilan dentro del contenedor que el tirador redimensiona. Comprueba el resultado en ambos motores antes de depender de él.

## Páginas relacionadas {#related-pages}

* [Layout](layout.md): cómo el contenedor y sus hermanos comparten el espacio.
* [Tags](tags.md) y [ventana](window.md): el contenido habitual de un redimensionable.
* [Screens](screens.md): la lista de elementos de pantalla.

_Revisado para AWE 5._
