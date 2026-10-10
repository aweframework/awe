---
id: pivot-table
title: Tabla dinámica
---

La tabla dinámica es un componente analítico. Carga las filas de una consulta y permite al usuario construir un informe a partir de ellas: el
usuario arrastra los campos a las filas y a las columnas, elige una agregación (recuento, suma, media...) y un renderizador
(tabla, mapa de calor...), y la tabla se calcula en el navegador.

<img alt="Tabla dinámica" src={require('@docusaurus/useBaseUrl').default('img/PivotTable.png')} />

## Cuándo usarla {#when-to-use-it}

Usa una tabla dinámica cuando el usuario deba **explorar** un conjunto de datos (agruparlo, totalizarlo, cruzarlo) y no sepas de
antemano qué grupos necesitará. Si el informe es fijo, una [rejilla](grids.md) con la consulta adecuada es más sencilla y
admite paginación, edición y selección.

La tabla dinámica trabaja sobre **todas las filas que devuelve la consulta**: la agrupación y los totales los calcula el
cliente, no el servidor. Por ello:

* Indica `max="0"` para cargar todas las filas. Cuando no se indica `max`, la carga inicial pide al servidor una página de filas, como hace una
  rejilla (30 filas por defecto, consulta `awe.application.component.grid-rows-per-page` en [propiedades](../properties.md)),
  y la tabla dinámica solo usaría esas filas.
* Limita la consulta a los campos que necesita el usuario. Cada campo de la consulta se convierte en un campo de la tabla dinámica.

## Esqueleto XML {#xml-skeleton}

```xml
<pivot-table id="[pivot-id]" initial-load="query" target-action="[query-id]" max="0"
             rows="[fields]" cols="[fields]" aggregator="[aggregator]" aggregation-field="[field]"
             style="expand"/>
```

Una tabla dinámica puede contener estos elementos hijos: `context-button`, `context-separator` (consulta el
[menú contextual](context-menu.md)) y `dependency` (consulta [dependencias](dependencies.md)).

## Atributos {#attributes}

Los atributos son los del esquema `screen.xsd` (consulta las [herramientas XSD](../guides/xsd-tooling.md)).

### Identificación y carga de datos {#identification-and-data-loading}

| Atributo      | Uso             | Tipo    | Descripción                                                         | Valores / por defecto                                                             |
| ------------- | --------------- | ------- | ------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| id            | **Obligatorio** | String  | Identificador de la tabla dinámica. Debe ser único en la pantalla   |                                                                                   |
| style         | Opcional        | String  | Clases CSS de la tabla dinámica                                     | Usa `expand` para ocupar el espacio disponible (consulta [layout](layout.md))    |
| initial-load  | Opcional        | String  | De dónde proceden los datos en la carga inicial                     | `query` (el habitual), `enum`, `value`                                            |
| server-action | Opcional        | String  | Acción de servidor que recarga los datos                            | Consulta la [lista de acciones de servidor](actions.md#server-actions)            |
| target-action | Opcional        | String  | [Consulta](query-definition.md) que devuelve los datos              | Identificador de la consulta                                                      |
| max           | Opcional        | Integer | Número máximo de filas a cargar                                     | `0` carga todas las filas. Por defecto: el tamaño de página de la rejilla (30)    |
| autoload      | Opcional        | Boolean | Lanza la `target-action` cuando se inicializa la pantalla           | Solo motor AngularJS                                                              |
| autorefresh   | Opcional        | Integer | Lanza la `target-action` cada X segundos                            | Solo motor AngularJS                                                              |

### Disposición inicial de la tabla {#initial-layout-of-the-table}

El usuario puede cambiar todo esto en el navegador. Los atributos solo establecen lo que el usuario ve cuando la tabla se
dibuja por primera vez.

| Atributo               | Uso      | Tipo    | Descripción                                          | Valores / por defecto                                                         |
| ---------------------- | -------- | ------- | ---------------------------------------------------- | ----------------------------------------------------------------------------- |
| rows                   | Opcional | String  | Campos que empiezan en las filas                     | Nombres de campo separados por comas                                          |
| cols                   | Opcional | String  | Campos que empiezan en las columnas                  | Nombres de campo separados por comas                                          |
| renderer               | Opcional | String  | Renderizador seleccionado al inicio                  | `Table` (por defecto), `Table Barchart`, `Heatmap`, `Row Heatmap`, `Col Heatmap` |
| aggregator             | Opcional | String  | Agregador seleccionado al inicio                     | Consulta los [agregadores](#aggregators). Por defecto: `Count`                |
| aggregation-field      | Opcional | String  | Campo sobre el que trabaja el agregador              | Un campo de la consulta. No es necesario para `Count`                         |
| sort-method            | Opcional | String  | Cómo se ordenan los valores de un campo              | `natural` (por defecto), `absolute` (por valor numérico absoluto)             |
| total-row-placement    | Opcional | String  | Dónde se dibuja la fila con los totales              | `top`, `bottom` (por defecto)                                                 |
| total-column-placement | Opcional | String  | Dónde se dibuja la columna con los totales           | `left`, `right` (por defecto)                                                 |

### Formato numérico {#number-format}

| Atributo           | Uso      | Tipo    | Descripción                          | Valores / por defecto                                                                  |
| ------------------ | -------- | ------- | ------------------------------------ | -------------------------------------------------------------------------------------- |
| decimal-numbers    | Opcional | Integer | Número de decimales de los resultados | Por defecto: los decimales de las opciones numéricas de la aplicación, o `0`          |
| thousand-separator | Opcional | String  | Separador de millares                | Por defecto: el de las opciones numéricas de la aplicación                             |
| decimal-separator  | Opcional | String  | Separador decimal                    | Por defecto: el de las opciones numéricas de la aplicación                             |

Los tres atributos solo los aplican los **agregadores personalizados** (`Custom Sum`, `Custom Average`...) del
motor AngularJS. Los agregadores estándar formatean los números con el formato por defecto de la librería, y los
agregadores personalizados solo existen cuando se indica el atributo `aggregator`.

## Agregadores {#aggregators}

El atributo `aggregator` acepta estos valores:

| Grupo                          | Valores                                                                                                                        |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------ |
| Recuento                       | `Count`, `Count Unique Values`, `List Unique Values`                                                                           |
| Suma y estadísticas            | `Sum`, `Integer Sum`, `Average`, `Minimum`, `Maximum`, `Sum over Sum`, `80% Upper Bound`, `80% Lower Bound`                    |
| Fracciones                     | `Sum as Fraction of Total`, `Sum as Fraction of Rows`, `Sum as Fraction of Columns`, `Count as Fraction of Total`, `Count as Fraction of Rows`, `Count as Fraction of Columns` |
| Personalizados (formato numérico) | `Custom Sum`, `Custom Average`, `Custom Minimum`, `Custom Maximum`, `Custom Sum over Sum`, `Custom 80% Upper Bound`, `Custom 80% Lower Bound` |

Los agregadores personalizados son iguales que los estándar, pero usan el formato definido por `decimal-numbers`,
`thousand-separator` y `decimal-separator`.

> **Nota:** `Custom 80% Upper Bound` y `Custom 80% Lower Bound` no aplican el formato numérico personalizado, y ambos
> calculan el límite superior. Usa `80% Upper Bound` y `80% Lower Bound` en su lugar.

## Ejemplo {#example}

La tabla dinámica de la pantalla de pruebas `pivot-test` agrupa las filas de una consulta por el campo `Als` y suma el campo
`Prg1`. El usuario puede cambiar los grupos, el agregador y el renderizador en la pantalla:

```xml
<window label="SCREEN_TEXT_DATA" style="expand" expandible="vertical">
  <tag type="div" style="panel-body expand scrollable-both" expandible="vertical">
    <pivot-table id="listaDatos" initial-load="query" target-action="QryUniTstId" max="0"
                 cols="Als" aggregation-field="Prg1" style="expand"/>
  </tag>
</window>
```

Para recargar los datos (por ejemplo desde un botón de búsqueda, como hace la pantalla de pruebas) usa la acción `filter` sobre la tabla
dinámica:

```xml
<button button-type="submit" label="BUTTON_SEARCH" icon="search" id="ButSch">
  <button-action type="filter" target="listaDatos"/>
</button>
```

Este ejemplo es la tabla dinámica de la pantalla de pruebas `pivot-test` (`awe-tests/awe-boot` y `awe-tests/awe-boot-react`),
con `max="0"` añadido.

### Cambiar los grupos desde el servidor {#changing-the-groups-from-the-server}

Tres [acciones de cliente](actions.md#client-actions) cambian la tabla desde un servicio o una dependencia:

| Acción                 | Parámetro | Descripción                                                                           |
| ---------------------- | --------- | ------------------------------------------------------------------------------------- |
| `set-pivot-group-rows` | `rows`    | Establece los campos de las filas, como una lista separada por comas                  |
| `set-pivot-group-cols` | `cols`    | Establece los campos de las columnas, como una lista separada por comas               |
| `set-pivot-sorters`    | `sorters` | Establece el orden de los valores de cada campo                                       |

## Motores AngularJS y React {#angularjs-and-react-engines}

La tabla dinámica se dibuja con una librería distinta en cada motor, y los atributos XML no son equivalentes:

| Característica                                                       | Motor AngularJS                       | Motor React                                                   |
| -------------------------------------------------------------------- | ------------------------------------- | ------------------------------------------------------------- |
| Librería                                                             | PivotTable.js (jQuery), incluida en AWE      | [react-pivottable](https://github.com/plotly/react-pivottable) 0.11 |
| `rows`, `cols`                                                       | Aplicados                             | Aplicados                                                     |
| `renderer`, `aggregator`, `aggregation-field`                        | Aplicados                             | **No aplicados**: la tabla empieza con los valores por defecto de la librería |
| `sort-method`, `total-row-placement`, `total-column-placement`       | Aplicados                             | **No aplicados**                                              |
| `decimal-numbers`, `thousand-separator`, `decimal-separator`, agregadores personalizados | Aplicados         | **No aplicados**: los agregadores `Custom ...` no existen     |
| Acciones `set-pivot-group-rows`, `set-pivot-group-cols`, `set-pivot-sorters` | Aplicadas                     | Aplicadas                                                     |
| Menú contextual (`context-button`)                                   | Disponible                            | No disponible                                                 |
| `autoload`, `autorefresh`                                            | Aplicados                             | Ignorados                                                     |
| Textos de la tabla                                                   | Traducidos (inglés, español, euskera, francés) | Siempre en inglés                                    |
| Número máximo de valores listados en el filtro de un campo           | `awe.application.component.pivot-num-group` (5000) | 500 (el valor por defecto de la librería)        |

En el motor React el usuario todavía puede elegir el renderizador y el agregador en la tabla, y las elecciones se
mantienen mientras la pantalla está abierta. Si una pantalla debe empezar con un renderizador o agregador determinado en ambos motores, hoy necesita el
motor AngularJS.

> **Nota:** Las diferencias del motor React provienen de cómo el componente pasa los atributos XML a la librería
> (`AwePivotTable.jsx` los reenvía tal cual, y la librería espera otros nombres). Se listan aquí para que puedas
> planificar con antelación; no rompen la pantalla.

## Páginas relacionadas {#related-pages}

* [Grids](grids.md): cuando el informe es fijo o las filas deben editarse o paginarse.
* [Query definition](query-definition.md): la consulta que alimenta la tabla.
* [Layout](layout.md): cómo `style="expand"` hace que la tabla ocupe su contenedor.
* [Actions](actions.md): la acción `filter` y las acciones de tabla dinámica.
* [Dependencies](dependencies.md) y [context menu](context-menu.md): los elementos hijos.

_Revisado para AWE 5._
