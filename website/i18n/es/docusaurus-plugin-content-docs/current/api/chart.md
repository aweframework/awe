---
id: chart
title: Gráficas
---

Un gráfico muestra el resultado de una consulta de forma gráfica: líneas, columnas, áreas, sectores, diagramas de dispersión... Se describe en el XML de la pantalla con un elemento `chart`, sus ejes, sus series y, opcionalmente, una leyenda, un tooltip y parámetros adicionales. El gráfico carga sus datos de una consulta, como cualquier otro componente de AWE.

Dos motores dibujan los gráficos y **el XML del gráfico es el mismo en ambos**:

| Motor    | Biblioteca                                             | Notas                                                                                             |
| --------- | --------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| React     | [Apache ECharts](https://echarts.apache.org/) 6.1.0 | Cliente por defecto de AWE 5. Los gráficos se dibujan como SVG                                                  |
| AngularJS | [Highcharts](https://www.highcharts.com/)           | Sigue dibujando los gráficos con Highcharts hasta que el motor AngularJS pase a ECharts (issue #775) |

La traducción la hace el servidor. Para cada gráfico construye el `chartModel` de Highcharts (usado por el motor AngularJS) y, junto a él, un `echartsModel` (usado por el motor React). El `echartsModel` es una opción de ECharts sin datos y sin funciones: el cliente añade los valores de la consulta e interpreta los [formatos](chart-format.md) y el resto de indicaciones que envía el servidor. Las opciones que se establecen con [`chart-parameter`](chart-parameters.md) también se traducen; las que no tienen equivalente en ECharts se ignoran y se registran en el log del servidor.

> **Nota:** Apache ECharts es de código abierto (Apache License 2.0). Highcharts es de código abierto para **aplicaciones no comerciales**. Para fines comerciales, debe *adquirir* una licencia para usarlo en el motor AngularJS.

Las páginas sobre gráficos son:

* **Gráficas** (esta página): estructura XML, ejemplos de cada tipo de gráfico, acciones y diferencias entre los motores.
* [Formatos de gráficos](chart-format.md): el lenguaje de formato de etiquetas, tooltips y ejes.
* [Parámetros de gráficos](chart-parameters.md): las opciones de Highcharts que se pueden establecer y cómo se traduce cada una.

## Conceptos del gráfico {#chart-concepts}

Para entender cómo funciona un gráfico es importante conocer las distintas partes o conceptos que lo componen. A continuación se muestra una imagen y una descripción de los conceptos principales de un gráfico. La imagen se dibujó con Highcharts, pero las partes son las mismas en ambos motores.

<img alt="Sobre Highcharts" src={require('@docusaurus/useBaseUrl').default('img/understanding_highcharts.png')} />

* **Título:** Es el texto que se presentará en la parte superior de un gráfico. También puede añadir un elemento de subtítulo para describir con más detalle el gráfico. Más información sobre la estructura XML del elemento [gráfico](#chart-element).

* **Series:** Conjunto de datos relacionados que se presentan en un gráfico. Un gráfico puede contener una o varias series de datos. See [series](#serie-element) for more information in XML structure.

* **Tooltip:** Al pasar el ratón sobre una serie o un punto del gráfico se puede obtener un tooltip que describe los valores de esa parte concreta del gráfico. Más información sobre la estructura XML del elemento [tooltip](#tooltip-element).

* **Leyenda:** La leyenda muestra las series de datos del gráfico y permite activar y desactivar una o varias series. Más información sobre la estructura XML del elemento [leyenda](#legend-element).

* **Eje:** Los ejes x e y del gráfico; también se pueden usar varios ejes para distintas series de datos. La mayoría de los tipos de gráfico, como los típicos cartesianos de línea y columna, tienen ejes. Más información sobre la estructura XML del elemento [eje](#axis-element).

### Series temporales con zoom (gráficos bursátiles) {#time-series-with-zoom-stock-charts}

Un gráfico con `stock-chart="true"` está pensado para mostrar la evolución de muchos datos a lo largo del tiempo. En el motor React tiene un **deslizador** bajo el área del gráfico (el navegador) y el eje x se puede ampliar con la rueda del ratón. El motor AngularJS lo dibuja con Highstock, que añade un selector de rango, una barra de desplazamiento y un navegador.

<img alt="Sobre Highstock" src={require('@docusaurus/useBaseUrl').default('img/understanding_highstock.png')} />

* **Selector fino:** Te permite ajustar el rango del gráfico que se muestra.
* **Selector de rango:** Permite seleccionar rápidamente un rango para mostrar en el gráfico o especificar el intervalo exacto. Solo en el motor AngularJS.
* **Barra de desplazamiento:** Permite desplazarse en el gráfico. Solo en el motor AngularJS.
* **Cruceta:** Muestra una línea siguiendo la descripción de un gráfico para leer mejor los resultados del eje x. Esta funcionalidad se encuentra en las opciones del tooltip y se puede usar en cualquier gráfico (no está habilitada por defecto).

La imagen anterior muestra Highstock. Consulte el [ejemplo de gráfico bursátil](#stock-chart-with-zoom) para el motor React.

## Esqueleto de XML {#xml-skeleton}

La estructura básica del gráfico es la siguiente:

```xml
<chart id="[Chart Id]" label="[Chart title]" subtitle="[Chart subtitle]" type="[Type chart]"
       initial-load="[Initial load]" target-action="[Action]">
  <chart-legend layout="[Layout]" align="[Align]" verticalAlign="[Vertical align]" />
  <x-axis label="[Label X-Axis]" type="[Type axis]"/>
  <y-axis label="[Label Y-Axis]"/>
  <chart-tooltip suffix="[Suffix value]" number-decimals="[Decimal numbers]"/>
  <chart-serie id="[Serie ID]" x-value="[X-Values]" y-value="[Y-value]" label="[Serie label]" />
  <chart-parameter type="[Type parameter]" name="[Name parameter]">
    <chart-parameter type="[Type parameter]" name="[Name parameter]" value="[Parameter value]"/>
  </chart-parameter>
</chart>
```

### Estructura general del gráfico {#global-chart-structure}

Para facilitar el desarrollo, no todas las etiquetas son necesarias.

| Elemento                                    | Uso             | Varias instancias | Descripción                                                                                                                                                             |
| ------------------------------------------- | --------------- | ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [chart](#chart-element)                     | **Obligatorio** | No                | Nodo global del gráfico. Describe los atributos generales del gráfico. Título, tipo, qué consulta lo genera, ...                                                        |
| [chart-legend](#legend-element)             | Opcional        | No                | Describe la leyenda del gráfico                                                                                                                                         |
| [chart-tooltip](#tooltip-element)           | Opcional        | No                | El tooltip aparece al pasar sobre un punto de una serie. Por defecto, el tooltip muestra los valores del punto y el nombre de la serie                                  |
| [x-axis](#axis-element)                     | **Obligatorio** | Si                | Describe el eje X del gráfico                                                                                                                                           |
| [y-axis](#axis-element)                     | **Obligatorio** | Si                | Describe el eje Y del gráfico. Es posible tener múltiples ejes y enlazarlos con diferentes series de datos                                                              |
| [chart-serie](#serie-element)               | **Obligatorio** | Si                | Una serie es un conjunto de datos. Se representa como una lista de arrays con dos valores, _[[x1,y1], [x2,y2]]_. Cada array es un punto en la serie representado por el eje |
| [chart-parameter](#chart-parameter-element) | Opcional        | Si                | Parámetros adicionales para sobrescribir la estructura del gráfico. Véase [parámetros de gráficos](chart-parameters.md)                                                                          |

El elemento `chart` también puede contener elementos `dependency`, `context-button` y `context-separator`, como el resto de componentes. Un gráfico de sectores no tiene ejes: los ejes que declare el XML no se dibujan.

#### Elemento gráfico {#chart-element}

El elemento gráfico tiene los siguientes atributos:

| Atributo           | Uso             | Tipo    | Descripción                                                                                                                    | Valores                                                                                                                                                                                                          |
| ------------------ | --------------- | ------- | ------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| id                 | **Obligatorio** | String  | Identificador del gráfico                                                                                                      |                                                                                                                                                                                                                  |
| label              | Opcional        | String  | Es el título del gráfico                                                                                                       | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                                                                                                                                             |
| subtitle           | Opcional        | String  | Es el subtítulo del gráfico                                                                                                    | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                                                                                                                                             |
| type               | **Obligatorio** | String  | Tipo de gráfico                                                                                                                | `line`, `spline`, `column`, `column_3d`, `area`, `areaspline`, `arearange`, `areasplinerange`, `pie`, `pie_3d`, `donut`, `donut_3d`, `semicircle`, `mixed`, `bubble`, `scatter`. See [chart types](#chart-types) |
| stock-chart        | Opcional        | Boolean | Indicador de tipo de gráfico de stock                                                                                          | `true` o `false`                                                                                                                                                                                                |
| theme              | Opcional        | String  | Es el nombre del tema de Highcharts                                                                                            | Solo motor AngularJS. Véase [Highcharts themes](#highcharts-themes-angularjs-engine)                                                                                                                              |
| inverted           | Opcional        | Boolean | Invertir los ejes para que el eje x sea vertical y el eje y sea horizontal. Cuando es true, el eje x es revertido por defecto. | `true` o `false`                                                                                                                                                                                                |
| stacking           | Opcional        | String  | Si se deben apilar los valores de cada serie encima de la otra                                                                 | `normal` o `percent`                                                                                                                                                                                            |
| enable-data-labels | Opcional        | Boolean | Indica si se muestran las etiquetas de datos de los puntos                                                                                  | Por defecto es `false`                                                                                                                                                                                              |
| format-data-labels | Opcional        | String  | Cadena de formato de las etiquetas de datos. Véase [formatos de gráficos](chart-format.md)                                                        | Ej. Punto y con 3 decimales `format-data-labels="{y:.3f}"`                                                                                                                                                       |
| zoom-type          | Opcional        | String  | Decide en qué dimensiones puede ampliar el usuario                                                                                   | `xAxis`, `yAxis` o `all`                                                                                                                                                                                        |
| initial-load      | **Obligatorio** | String  | Para cargar el gráfico cuando se genera la pantalla                                                                            | **Nota:** Solo puede tener el valor 'query'                                                                                                                                                                      |
| target-action      | **Obligatorio** | String  | Es el nombre de la consulta que carga el gráfico                                                                               |                                                                                                                                                                                                                  |
| server-action      | Opcional        | String  | Tipo de acción de servidor que carga el gráfico                                                                                     |                                                                                                                                                                                                                  |
| max                | Opcional        | Number  | Número de puntos a mostrar                                                                                                     | **Nota:** 0 significa todos los elementos                                                                                                                                                                        |
| autorefresh      | Opcional        | Number  | Intervalo de la recarga automática del gráfico                                                                                  |                                                                                                                                                                                                                  |
| autoload   | Opcional        | Boolean | Indica si el gráfico carga sus datos automáticamente                                                                                 |                                                                                                                                                                                                                  |
| style              | Opcional        | String  | Clases CSS del gráfico                                                                                                       |                                                                                                                                                                                                                  |
| visible            | Opcional        | Boolean | Indica si el gráfico es visible                                                                                                   |                                                                                                                                                                                                                  |
| help              | Opcional        | String  | Texto de ayuda del gráfico                                                                                                         | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                                                                                                                                             |
| help-image    | Opcional        | String  | Imagen que se muestra en la ayuda del gráfico                                                                                           |                                                                                                                                                                                                                  |
| icon-loading    | Opcional        | String  | Define el icono de carga                                                                                                       | `spinner` (por defecto), `square`, `circles`, `carpet`, `dots`, `folding`, `squarebar`, `circlebar`, `cubes`, `icon`, `custom`, `none`                                                                           |

##### Tipos de gráfico {#chart-types}

| Tipo                           | Dibujado como (motor React)                                                                    |
| ------------------------------ | ------------------------------------------------------------------------------------------ |
| `line`, `spline`               | Líneas. `spline` las suaviza                                                               |
| `area`, `areaspline`           | Líneas rellenas. `areaspline` las suaviza                                                    |
| `arearange`, `areasplinerange` | Aproximado mediante una línea rellena (el servidor registra una advertencia)                                  |
| `column`                      | Barras verticales                                                                              |
| `column_3d`                    | Barras verticales, **planas** (el 3D no se dibuja)                                                  |
| `pie`                          | Sectores                                                                                        |
| `pie_3d`                       | Sectores, **plano**                                                                              |
| `donut`, `donut_3d`            | Sectores con radio interior (50% por defecto), plano                                            |
| `semicircle`                   | Anillo que solo dibuja su mitad superior                                                       |
| `scatter`, `bubble`            | Puntos. `bubble` usa el `z-value` de la serie como tamaño                              |
| `mixed`                        | Lo decide el tipo de cada serie (`type` de `chart-serie`). Una serie sin tipo es una línea |

#### Eje {#axis-element}
El elemento eje tiene los siguientes atributos:

| Atributo           | Uso      | Tipo    | Descripción                                                                                                                                                                                                               | Valores                                                                                                                                                                     |
| ------------------ | -------- | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| label              | Opcional | String  | Es el nombre del eje                                                                                                                                                                                                      | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                                                                                                        |
| label-format       | Opcional | String  | Cadena de formato de la etiqueta del eje. Véase [formatos de gráficos](chart-format.md)                                                                                                                                                  | Por defecto es `{value}`. Ej.: añadir una unidad al eje `label-format = "{value} ºC"`                                                                                                  |
| formatter-function | Opcional | String  | Función con nombre para formatear las etiquetas del eje                                                                                                                                                                                      | `formatCurrencyMagnitude`. See [named formatters](chart-format.md#named-formatters)                                                                                         |
| label-rotation     | Opcional | Number  | Rotación de las etiquetas en grados                                                                                                                                                                                       | Por defecto a 0                                                                                                                                                             |
| type               | Opcional | String  | El tipo de eje.                                                                                                                                                                                                           | `linear`, `logarithmic`, `datetime` o `category`. Por defecto es `linear`                                                                                                     |
| tick-interval      | Opcional | Number  | El intervalo de las marcas de separación en unidades de eje                                                                                                                                                               | En un eje `datetime` la unidad es el milisegundo, por lo que un intervalo de un día es `24 * 3600 * 1000` = `86400000`. En un eje `category` se muestra una etiqueta cada n categorías |
| allow-decimal      | Opcional | Boolean | Indica si se permiten decimales en las marcas de separación del eje. Cuando se cuentan números enteros, como personas o coincidencias en una página web, los decimales deben evitarse en las etiquetas de las marcas de separación del eje | Por defecto es `false`, por lo que las marcas son enteros salvo que se establezca `true`                                                                                                        |
| opposite           | Opcional | Boolean | Mostrar el eje en el lado opuesto al usual. La normal es en el lado izquierdo para los ejes verticales y el inferior para horizontal, así que los lados opuestos serán de derecha y superior respectivamente              | Por defecto es `false`                                                                                                                                                         |

Un elemento `x-axis` o `y-axis` puede contener elementos `chart-parameter`, con las opciones del [ámbito de eje](chart-parameters.md#axis-scope).

#### Elemento leyenda {#legend-element}
El elemento leyenda tiene los siguientes atributos:

| Atributo      | Uso      | Tipo    | Descripción                                                                                         | Valores                                                                                                                                                          |
| ------------- | -------- | ------- | --------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| label         | Opcional | String  | Es el título de la leyenda                                                                          | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                                                                                             |
| enabled       | Opcional | Boolean | Para activar o desactivar la leyenda en el gráfico                                                  | Por defecto es `false` cuando el elemento `chart-legend` está presente. Sin el elemento, la leyenda se muestra en los gráficos con ejes y se oculta en los de sectores y bursátiles |
| border-width  | Opcional | Number  | El ancho del borde dibujado alrededor de la leyenda                                                 | Default to `0`                                                                                                                                                   |
| layout        | Opcional | String  | El diseño de los elementos de la leyenda                                                            | `horizontal` o `vertical`. Por defecto es `horizontal`                                                                                                             |
| align         | Opcional | String  | La alineación horizontal de la caja de la leyenda dentro del área del gráfico                       | `left`, `center` o `right`. Por defecto es `center`                                                                                                                |
| verticalAlign | Opcional | String  | La alineación vertical de la caja de leyenda                                                        | `top`, `middle` o `bottom`. Por defecto es `bottom`                                                                                                                |
| floating      | Opcional | Boolean | Cuando la leyenda está flotando, el área de la parcela ignora y se permite colocarla debajo de ella | `true` o `false`. Por defecto es `false`                                                                                                                           |

En un gráfico de sectores la leyenda enumera los sectores. En los demás gráficos enumera las series y al hacer clic en una entrada se oculta o se muestra la serie.

#### Elemento tooltip {#tooltip-element}
El elemento tooltip tiene los siguientes atributos:

| Atributo        | Uso      | Tipo    | Descripción                                                                                   | Valores                                                                        |
| --------------- | -------- | ------- | --------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| enabled         | Opcional | Boolean | Para activar o desactivar la descripción en el gráfico                                        | Por defecto es `true`                                                             |
| crosshairs      | Opcional | String  | Muestra el cruce de caminos para conectar los puntos con sus correspondientes valores del eje | `xAxis`, `yAxis` o `all`                                                      |
| number-decimals | Opcional | Number  | Valor de la descripción de formato ajustando el número de decimales                           |                                                                                |
| suffix          | Opcional | String  | Formatear el valor del tooltip añadiendo una cadena de sufijo                                 | Ej.: `suffix = " ºC"`                                                          |
| prefix          | Opcional | String  | Formatear valor tooltip añadiendo una cadena de prefijo                                       | Ej.: `prefix = "Temp. "`                                                       |
| point-format    | Opcional | String  | El formato de la línea del punto en el tooltip. Véase [formatos de gráficos](chart-format.md#tooltips)  | Ej.: `point-format = '{series.name}: <b>{point.y}</b><br/>'` |
| date-format     | Opcional | String  | Para series en un eje datetime, el patrón de fecha de la cabecera del tooltip                  | Ej.: `date-format = "%Y-%m-%d"`                                                |
| shared          | Opcional | Boolean | Descripción compartida para múltiples series de gráficos                                      | Por defecto es `false`                                                            |

La cabecera y el pie del tooltip, así como el modo HTML, se establecen con parámetros del tooltip. Véase [tooltips HTML](chart-format.md#html-tooltips).

#### Elemento serie {#serie-element}
El elemento serie tiene los siguientes atributos:

| Atributo        | Uso             | Tipo    | Descripción                                                                                                                                       | Valores                                                                                                                                                                                                                                                                                                |
| --------------- | --------------- | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| id              | **Obligatorio** | String  | Identificador de serie                                                                                                                            |                                                                                                                                                                                                                                                                                                        |
| label           | Opcional        | String  | Es el nombre de la serie. Este valor se muestra en la leyenda                                                                                     | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                                                                                                                                                                                                                             |
| type            | Opcional        | String  | Tipo de serie                                                                                                                                     | `line`, `spline`, `column`, `bar`, `pie`, `area`, `areaspline`, `arearange`, `areasplinerange` o `scatter`. **Nota:** Si hay varias series de distinto tipo, debe establecer el atributo type del elemento chart en `mixed`. Una serie `bar` es una columna horizontal: invierte todo el gráfico |
| color           | Opcional        | String  | Se utiliza para establecer un color para la serie                                                                                                 | **Nota:** Puede usar el nombre de colores o código hexadecimal. Ej.: `color = "red"` o `color = "#BF0B23"`                                                                                                                                                                                            |
| x-value         | **Obligatorio** | String  | Define el valor del punto en el eje x. Corresponde al atributo `alias` del campo en la consulta                                                   |                                                                                                                                                                                                                                                                                                        |
| y-value         | **Obligatorio** | String  | Define el valor del punto en el eje y. Corresponde al atributo `alias` del campo en la consulta                                                   |                                                                                                                                                                                                                                                                                                        |
| z-value         | Opcional        | String  | Define el valor del punto en el eje z (el tamaño de una burbuja). Corresponde al atributo `alias` del campo en la consulta                     |                                                                                                                                                                                                                                                                                                        |
| x-axis          | Opcional        | Number  | Al usar ejes x dobles o múltiples, este número define a qué xAxis está conectada la serie en particular. Hace referencia a la posición del eje    | El valor por defecto es 0                                                                                                                                                                                                                                                                              |
| y-axis          | Opcional        | Number  | Al usar ejes dobles o múltiples, este número define a qué ejes yAxis está conectada la serie en particular. Hace referencia a la posición del eje | El valor por defecto es 0                                                                                                                                                                                                                                                                              |
| drilldown-serie | Opcional        | String  | Identificador de la serie de desglose                                                                                                             | **Nota:** Véase [este](#drilldown) ejemplo                                                                                                                                                                                                                                                              |
| drilldown       | Opcional        | Boolean | Marcar para indicar si la serie se utiliza en un subgráfico                                                                                       | `true` o `false`                                                                                                                                                                                                                                                                                      |

Un elemento `chart-serie` puede contener elementos `chart-parameter`, con las opciones del [ámbito de serie](chart-parameters.md#series-scope-and-plotoptions).

#### Elemento parámetro de gráfico {#chart-parameter-element}
Se utiliza para sobrescribir valores en los elementos del gráfico. Tiene los siguientes atributos:

| Atributo | Uso             | Tipo   | Descripción                  | Valores                                                                                               |
| -------- | --------------- | ------ | ---------------------------- | ----------------------------------------------------------------------------------------------------- |
| type     | **Obligatorio** | String | Tipo de parámetro de gráfico | Puede ser uno de `string`, `integer`, `long`, `float`, `double`, `boolean`, `array`, `object` o `null`. |
| name     | **Obligatorio** | String | Nombre del parámetro gráfico |                                                                                                       |
| value    | Opcional        | String | Valor del parámetro gráfico  |                                                                                                       |

El elemento chart, los ejes, la leyenda, el tooltip y las series admiten hijos `chart-parameter`. Véase [parámetros de gráficos](chart-parameters.md) para las opciones que aplica cada motor.

## Conceptos de series y consultas {#series-and-queries-concepts}

Esta sección explica cómo funciona la integración entre las series de los gráficos y el motor de consultas de AWE. Una serie es una lista de puntos, y un punto tiene un valor `x` y un valor `y` (y un valor `z` en un gráfico de burbujas). Cada punto es una fila de la consulta.

Por lo tanto, debe establecer los valores de x e y para el punto de una serie. Para ello existen los atributos de serie `x-value` y `y-value`.

Estos atributos corresponden al atributo `alias` del campo en la consulta. Veamos el siguiente ejemplo:

* **Código XML del elemento gráfico**

```xml
<chart id="ChrBarTst" label="CHART_2" type="column_3d" initial-load="query" target-action="TstChrThrDatSrc">
  <x-axis label="SCREEN_TEXT_CHART_AXIS_DATES" type="datetime"/>
  <y-axis label="Temperaturas (ºC)"/>
  <chart-serie id="serie2-1" x-value="dates" y-value="serie1" type="column" label="SCREEN_TEXT_CHART_SERIE_1" />
  <chart-serie id="serie2-2" x-value="dates" y-value="serie2" type="column" label="SCREEN_TEXT_CHART_SERIE_2" />
  <chart-serie id="serie2-3" x-value="dates" y-value="serie3" type="column" label="SCREEN_TEXT_CHART_SERIE_3" />
</chart>
```

* **Consulta para cargar los datos del gráfico**

``` xml
  <!-- GET THREE DATA SERIES WITH DATES -->
  <query id="TstChrThrDatSrc" distinct="true">
    <table id="HISAweDbs"/>
    <field id="HISdat" transform="DATE_MS" function="TRUNCDATE" alias="dates"/>
    <computed format="(1/(parseInt(Math.random()*10,10)+1))*10" eval="true" alias="serie1" transform="NUMBER"/>
    <computed format="(parseInt(Math.random()*10,10)+1)+2" eval="true" alias="serie2" transform="NUMBER"/>
    <computed format="((parseInt(Math.random()*10,10)+1)*2)-4" eval="true" alias="serie3" transform="NUMBER"/>
    <order-by field="dates" type="ASC"/>
  </query>
```

En este ejemplo, puede ver que el atributo `x-value = "dates"` de la serie del gráfico es igual al atributo `alias` del elemento field y que el atributo `y-value = "serie1"` es igual al alias de la consulta.

> **Nota:** Si el tipo de eje es **datetime**, el gráfico espera una fecha como valor long: una marca de tiempo de fecha de JavaScript (milisegundos desde el 1 de enero de 1970). AWE proporciona **transform = "DATE_MS"**.

Las series de un gráfico también se pueden definir en tiempo de ejecución, sin elementos `chart-serie`, con las [acciones de gráfico](#chart-actions).

> **Nota:** Los valores de los puntos se dibujan tal como los devuelve la consulta. El gráfico no los redondea: use `number-decimals` en el tooltip o un [formato](chart-format.md) para mostrar un número determinado de decimales.

## Ejemplos {#examples}

Los ejemplos muestran pantallas recortadas de la aplicación de pruebas de React, dibujadas con ECharts. Los valores de las series de las consultas de prueba son aleatorios, por lo que un gráfico de su aplicación no se verá exactamente igual. Las pantallas de prueba usan textos en español y en inglés para sus títulos y etiquetas.

Todos los ejemplos usan la misma estructura de consulta: un campo con el valor x (`DATE_MS` para fechas) y un campo calculado o seleccionado para cada serie, como se describe en [Conceptos de series y consultas](#series-and-queries-concepts).

### Gráfico de líneas {#line-chart}

Tipo `line`: las series se dibujan como líneas con un marcador en cada punto. El atributo `color` de una serie establece su color y `zoom-type="xAxis"` permite al usuario ampliar el eje x.

<img alt="Gráfico de líneas con dos series y un eje de fechas" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-line.png')} />

``` xml
<chart id="ChrLinTst" label="SCREEN_TEXT_LINE_CHART" type="line" initial-load="query"
       target-action="TstChrTwoSrc" zoom-type="xAxis" max="25">
  <chart-legend label="Leyenda" verticalAlign="middle" align="right" border-width="1" />
  <x-axis label="Fechas" type="datetime" />
  <y-axis label="Temperaturas (ºC)" />
  <chart-tooltip crosshairs="xAxis" suffix=" ºC" number-decimals="1" />
  <chart-serie id="serie-1" x-value="dates" y-value="serie1" label="Serie 1" color="#0080FF" />
  <chart-serie id="serie-2" x-value="dates" y-value="serie2" label="Serie 2" color="#81DAF5" />
</chart>
```

> **Nota:** Un elemento `chart-legend` está deshabilitado salvo que se establezca `enabled="true"`, por lo que la leyenda de este gráfico no se dibuja.

### Gráfico mixto: columnas, spline y dos ejes y {#mixed-chart-column-spline-and-two-y-axes}

Tipo `mixed`: cada serie establece su propio `type`. Este gráfico dibuja una serie de columnas y una serie `spline`, cada una en su propio eje y (`y-axis="0"` y `y-axis="1"`; el segundo eje es `opposite`).

<img alt="Gráfico mixto con columnas en el eje izquierdo y una línea suavizada en el eje derecho" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-mixed.png')} />

``` xml
<chart id="ChrLinTst" label="SCREEN_TEXT_CHART_TITLE_1" subtitle="Subtitulo grafico 1" type="mixed"
       initial-load="query" target-action="TstChrTwoSrc" zoom-type="xAxis" max="30">
  <chart-legend label="Leyenda" />
  <x-axis label="Fechas" type="datetime" />
  <y-axis label="Temperaturas (ºC)" formatter-function="formatCurrencyMagnitude"/>
  <y-axis opposite="true" label="Lluvias (mm)" />
  <chart-tooltip crosshairs="xAxis" suffix=" ºC" number-decimals="3" shared="true"/>
  <chart-serie id="serie-1" y-axis="0" x-value="dates" y-value="serie1" type="column" label="Serie 1" color="#A8E0A6" />
  <chart-serie id="serie-2" y-axis="1" x-value="dates" y-value="serie2" type="spline" label="Serie 2" />
</chart>
```

El tooltip de este gráfico es compartido: muestra los valores de todas las series para el mismo valor de x, con una cruceta. Véase el [ejemplo de tooltip](chart-format.md#tooltips).

### Gráfico de área {#area-chart}

Los tipos `area` y `areaspline` rellenan el área bajo la línea. Este ejemplo usa `areaspline` y cambia el patrón de fecha de las etiquetas del eje con un `chart-parameter`.

<img alt="Gráfico de áreas suavizado con dos series superpuestas" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-areaspline.png')} />

``` xml
<chart id="ChrAreTst" label="Grafico 5" type="areaspline" initial-load="query" target-action="TstChrTwoSrcLab" max="16">
  <chart-legend align="right" verticalAlign="top" floating="true" />
  <x-axis label="Fechas" type="datetime">
    <chart-parameter type="object" name="dateTimeLabelFormats">
      <chart-parameter type="string" name="day" value="%Y-%m-%d" />
    </chart-parameter>
  </x-axis>
  <y-axis label="Temperaturas (ºC)" />
  <chart-tooltip suffix=" ºC" number-decimals="3" />
  <chart-serie id="serie1" x-value="dates" y-value="serie1" label="Serie 1" />
  <chart-serie id="serie2" x-value="dates" y-value="serie2" label="Serie 2" />
</chart>
```

#### Área con relleno en degradado {#area-with-a-gradient-fill}

El relleno de un área es un degradado lineal definido en la serie (`fillColor`). El degradado va de la parte superior a la inferior del área del gráfico. El `threshold` de `-Infinity` rellena el área desde la base del eje, y los marcadores se estilizan con `plotOptions.area.marker`.

<img alt="Gráfico de áreas con relleno en degradado verde y marcadores blancos" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-gradient-area.png')} />

``` xml
<chart id="ChrAdvArea" label="Evolución de la plantilla" type="area" zoom-type="xAxis"
       initial-load="query" target-action="ChrAdvAreaSrc">
  <chart-legend enabled="false" />
  <chart-tooltip number-decimals="0" date-format="%B %Y" crosshairs="xAxis" suffix=" personas" />
  <x-axis type="datetime">
    <chart-parameter type="object" name="dateTimeLabelFormats">
      <chart-parameter type="string" name="month" value="%B %Y" />
    </chart-parameter>
    <chart-parameter type="string" name="gridLineWidth" value="0.5" />
  </x-axis>
  <y-axis>
    <chart-parameter type="string" name="gridLineWidth" value="0.5" />
  </y-axis>
  <chart-serie id="staff" x-value="month" y-value="staff" label="Plantilla" color="#8cac41">
    <chart-parameter type="object" name="fillColor">
      <chart-parameter type="object" name="linearGradient">
        <chart-parameter type="integer" name="x1" value="0" />
        <chart-parameter type="integer" name="y1" value="0" />
        <chart-parameter type="integer" name="x2" value="0" />
        <chart-parameter type="integer" name="y2" value="1" />
      </chart-parameter>
      <chart-parameter type="array" name="stops">
        <chart-parameter type="array" name="">
          <chart-parameter type="integer" name="" value="0" />
          <chart-parameter type="string" name="" value="rgba(140,172,65,0.7)" />
        </chart-parameter>
        <chart-parameter type="array" name="">
          <chart-parameter type="integer" name="" value="1" />
          <chart-parameter type="string" name="" value="rgba(140,172,65,0.05)" />
        </chart-parameter>
      </chart-parameter>
    </chart-parameter>
  </chart-serie>
  <chart-parameter type="object" name="plotOptions">
    <chart-parameter type="object" name="area">
      <chart-parameter type="integer" name="lineWidth" value="2" />
      <chart-parameter type="string" name="threshold" value="-Infinity" />
      <chart-parameter type="object" name="marker">
        <chart-parameter type="string" name="fillColor" value="#FFFFFF" />
        <chart-parameter type="integer" name="lineWidth" value="2" />
        <chart-parameter type="string" name="lineColor" value="#0088CC" />
        <chart-parameter type="integer" name="radius" value="4" />
      </chart-parameter>
    </chart-parameter>
  </chart-parameter>
</chart>
```

### Gráfico de columnas {#column-chart}

Tipo `column`. Con el atributo `stacking` (`normal` o `percent`) las series se apilan. El tipo `column_3d` se acepta en el XML, pero **el motor React lo dibuja plano**, como este gráfico apilado:

<img alt="Gráfico de columnas apiladas con tres series" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-stacked-column.png')} />

``` xml
<chart id="ChrBarTst" label="Grafico 2" subtitle="Subtitulo grafico 2" type="column_3d" stacking="normal"
       initial-load="query" target-action="TstChrThrDatSrc" max="21">
  <chart-legend layout="vertical" align="right" verticalAlign="middle" />
  <x-axis label="SCREEN_TEXT_CHART_AXIS_DATES" type="datetime" />
  <y-axis label="Temperaturas (ºC)" />
  <chart-tooltip crosshairs="xAxis" suffix=" ºC" number-decimals="3" />
  <chart-serie id="serie2-1" x-value="dates" y-value="serie1" type="column" label="SCREEN_TEXT_CHART_SERIE_1" />
  <chart-serie id="serie2-2" x-value="dates" y-value="serie2" type="column" label="SCREEN_TEXT_CHART_SERIE_2" />
  <chart-serie id="serie2-3" x-value="dates" y-value="serie3" type="column" label="SCREEN_TEXT_CHART_SERIE_3" />
</chart>
```

Las series de una pila son las que tienen el mismo parámetro `stack` (la pila por defecto es compartida por todas las series).

#### Columnas apiladas con esquinas redondeadas {#stacked-columns-with-rounded-corners}

El `borderRadius` de una serie (píxeles o porcentaje) redondea el extremo de la barra que queda más lejos del cero, y solo en la serie más externa de una pila. El espaciado entre columnas se establece con `pointPadding` y `groupPadding`, y el orden de las entradas de la leyenda con `legendIndex`. El eje y usa el formateador `formatCurrencyMagnitude`.

<img alt="Columnas apiladas con la parte superior redondeada y una leyenda en orden personalizado" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-rounded-columns.png')} />

``` xml
<chart id="ChrAdvColumns" label="Aportaciones por año" type="column" stacking="normal"
       initial-load="query" target-action="ChrAdvColumnsSrc">
  <chart-legend enabled="true" />
  <chart-tooltip number-decimals="2" suffix=" €" shared="true" />
  <x-axis type="category" tick-interval="1" />
  <y-axis formatter-function="formatCurrencyMagnitude" tick-interval="500" />
  <chart-serie id="company" type="column" x-value="year" y-value="company" label="Empresa" color="#2f6f8f">
    <chart-parameter type="string" name="borderRadius" value="30%" />
    <chart-parameter type="integer" name="legendIndex" value="2" />
  </chart-serie>
  <chart-serie id="employee" type="column" x-value="year" y-value="employee" label="Empleado" color="#7fb3c8">
    <chart-parameter type="string" name="borderRadius" value="30%" />
    <chart-parameter type="integer" name="legendIndex" value="1" />
  </chart-serie>
  <chart-parameter type="object" name="plotOptions">
    <chart-parameter type="object" name="column">
      <chart-parameter type="float" name="pointPadding" value="0.05" />
      <chart-parameter type="float" name="groupPadding" value="0.05" />
    </chart-parameter>
  </chart-parameter>
</chart>
```

### Gráfico de barras y pirámide {#bar-chart-and-pyramid}

Un gráfico de **barras** es un gráfico de columnas girado de lado. Hay dos maneras de obtenerlo: una serie de tipo `bar`, o `inverted="true"` en el gráfico. En ambos casos el eje x es vertical y el eje y es horizontal (la primera categoría o la fecha más antigua queda arriba).

La pirámide es un gráfico de barras con dos series que comparten una pila, un segundo eje x en el lado opuesto y valores negativos y positivos formateados con una [condición](chart-format.md#conditions). Dos series auxiliares (`dummyMax` y `dummyMin`, de tipo `scatter`) están ocultas en la leyenda y para el ratón: solo amplían el rango del eje.

<img alt="Pirámide salarial: barras horizontales para dos series a ambos lados de un eje central" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-pyramid.png')} />

``` xml
<chart id="ChrAdvPyramid" label="Pirámide salarial" type="column" inverted="true"
       initial-load="query" target-action="ChrAdvPyramidSrc">
  <chart-legend enabled="true" />
  <chart-tooltip shared="false">
    <!-- html tooltip: see Chart formats -->
  </chart-tooltip>
  <x-axis type="category" />
  <x-axis type="category" opposite="true" />
  <y-axis>
    <chart-parameter type="object" name="labels">
      <chart-parameter type="string" name="format"
                       value="{#if (gt value 0)}{multiply value 0.001}k{else}{multiply value -0.001}k{/if}" />
    </chart-parameter>
    <chart-parameter type="string" name="gridLineWidth" value="0" />
  </y-axis>
  <chart-serie id="men" type="column" x-value="age" y-value="men" label="Hombres" color="#4a7dbf">
    <chart-parameter type="string" name="stack" value="salary" />
    <chart-parameter type="float" name="pointPadding" value="0" />
    <chart-parameter type="float" name="groupPadding" value="0.1" />
    <chart-parameter type="integer" name="borderRadius" value="20" />
    <chart-parameter type="object" name="dataLabels">
      <chart-parameter type="boolean" name="enabled" value="true" />
      <chart-parameter type="string" name="format"
                       value="{#if (gt y 0)}{y:,.0f}€{else}{(multiply y -1):,.0f}€{/if}" />
    </chart-parameter>
  </chart-serie>
  <chart-serie id="women" type="column" x-value="age" y-value="women" label="Mujeres" color="#d9789b" x-axis="1">
    <chart-parameter type="string" name="stack" value="salary" />
    <!-- same parameters as the men series -->
  </chart-serie>
  <chart-serie id="dummyMax" type="scatter" x-value="age" y-value="dummyMax" label="max" color="transparent">
    <chart-parameter type="boolean" name="showInLegend" value="false" />
    <chart-parameter type="boolean" name="enableMouseTracking" value="false" />
    <chart-parameter type="object" name="marker">
      <chart-parameter type="boolean" name="enabled" value="false" />
    </chart-parameter>
  </chart-serie>
  <chart-serie id="dummyMin" type="scatter" x-value="age" y-value="dummyMin" label="min" color="transparent">
    <chart-parameter type="string" name="linkedTo" value=":previous" />
    <!-- same parameters as dummyMax -->
  </chart-serie>
</chart>
```

### Gráfico de sectores {#pie-chart}

Tipo `pie`. La leyenda enumera los sectores. `enable-data-labels` muestra la etiqueta de cada sector y su texto se establece con `format-data-labels` (véase [formatos de gráficos](chart-format.md#data-labels-and-axis-labels)). Un gráfico de sectores no tiene ejes, por lo que los ejes que declare el XML no se dibujan.

<img alt="Gráfico de sectores con una leyenda y una etiqueta de porcentaje para cada sector" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-pie.png')} />

``` xml
<chart id="ChrPieTst" label="Grafico 6" type="pie" initial-load="query" enable-data-labels="true"
       format-data-labels="&lt;b&gt;{point.name}&lt;/b&gt;: {point.percentage:.1f} %"
       target-action="TstChrPieDrillSrc" max="5">
  <chart-legend enabled="true" />
  <x-axis label="Themes" />
  <y-axis label="Percent (%)" />
  <chart-tooltip suffix=" %" number-decimals="2" />
  <chart-serie id="serie1" x-value="names" y-value="serie1" label="Themes" />
  <chart-parameter type="object" name="plotOptions">
    <chart-parameter type="object" name="pie">
      <chart-parameter type="string" name="size" value="75%" />
    </chart-parameter>
  </chart-parameter>
</chart>
```

#### Sectores con colores propios {#pie-with-its-own-colors}

El parámetro `colors` establece la paleta del gráfico. Con `colorByPoint` cada sector toma un color y `allowPointSelect` permite al usuario seleccionar un sector con un clic. Este gráfico es de tipo `pie_3d`, que se dibuja plano.

<img alt="Gráfico de sectores con una paleta personalizada en verde y azul" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-pie-colors.png')} />

``` xml
<chart id="ChrAdvPie3d" label="Plantilla por categoría" type="pie_3d" enable-data-labels="true"
       initial-load="query" target-action="ChrAdvPieSrc">
  <chart-legend enabled="true" />
  <chart-tooltip number-decimals="0" suffix=" personas" />
  <chart-serie id="cat" x-value="category" y-value="people" label="Personas" />
  <chart-parameter type="array" name="colors">
    <chart-parameter type="string" name="" value="rgba(201,229,134,0.9)" />
    <chart-parameter type="string" name="" value="rgba(140,172,65,0.9)" />
    <!-- ... -->
  </chart-parameter>
  <chart-parameter type="object" name="plotOptions">
    <chart-parameter type="object" name="series">
      <chart-parameter type="boolean" name="colorByPoint" value="true" />
      <chart-parameter type="boolean" name="allowPointSelect" value="true" />
    </chart-parameter>
    <chart-parameter type="object" name="pie">
      <chart-parameter type="object" name="dataLabels">
        <chart-parameter type="string" name="connectorColor" value="rgba(128,128,128,0.7)" />
      </chart-parameter>
    </chart-parameter>
  </chart-parameter>
</chart>
```

### Gráfico de anillo {#donut-chart}

Los tipos `donut` y `donut_3d` dibujan un gráfico de sectores con radio interior (50% por defecto). `plotOptions.pie.size` es el radio exterior y `plotOptions.pie.innerSize` el radio interior, relativo a `size`. El tipo 3D se dibuja plano.

<img alt="Gráfico de anillo con cinco sectores" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-donut.png')} />

``` xml
<chart id="ChrDonutTst" label="Grafico 7" type="donut_3d" initial-load="query" target-action="TstChrPieSrc" max="5">
  <chart-legend enabled="true" />
  <x-axis label="Themes" />
  <y-axis label="Percent (%)" />
  <chart-tooltip suffix=" %" number-decimals="2" />
  <chart-serie id="serie1" x-value="names" y-value="serie1" label="Themes" />
  <chart-parameter type="object" name="plotOptions">
    <chart-parameter type="object" name="pie">
      <chart-parameter type="string" name="size" value="75%" />
      <chart-parameter type="string" name="innerSize" value="40%" />
    </chart-parameter>
  </chart-parameter>
</chart>
```

### Gráfico de semicírculo {#semicircle-chart}

El tipo `semicircle` dibuja solo la mitad superior de un anillo. Las etiquetas se escriben dentro de los sectores (un `dataLabels.distance` negativo) y el título se desplaza al centro del gráfico con los parámetros de `title`. Como en Highcharts, el centro y el tamaño del semicírculo se miden en el área del gráfico, que excluye la leyenda, por lo que el título queda en el hueco.

<img alt="Gráfico de semicírculo con las etiquetas dentro de los sectores" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-semicircle.png')} />

``` xml
<chart id="ChrSemiCircleTst" label="Grafico 8" type="semicircle" enable-data-labels="true"
       initial-load="query" target-action="TstChrPieSrc" max="5">
  <chart-legend enabled="true" />
  <x-axis label="Themes" />
  <y-axis label="Percent (%)" />
  <chart-tooltip suffix=" %" number-decimals="2" />
  <chart-serie id="serie1" x-value="names" y-value="serie1" label="Themes" />
  <chart-parameter type="object" name="title">
    <chart-parameter type="string" name="align" value="center" />
    <chart-parameter type="string" name="verticalAlign" value="middle" />
    <chart-parameter type="integer" name="y" value="50" />
  </chart-parameter>
  <chart-parameter type="object" name="plotOptions">
    <chart-parameter type="object" name="pie">
      <chart-parameter type="string" name="size" value="75%" />
      <chart-parameter type="string" name="innerSize" value="40%" />
      <chart-parameter type="object" name="dataLabels">
        <chart-parameter type="boolean" name="enabled" value="true" />
        <chart-parameter type="integer" name="distance" value="-20" />
        <chart-parameter type="object" name="style">
          <chart-parameter type="string" name="fontWeight" value="bold" />
          <chart-parameter type="string" name="color" value="white" />
          <chart-parameter type="string" name="textShadow" value="0px 1px 2px black" />
        </chart-parameter>
      </chart-parameter>
    </chart-parameter>
  </chart-parameter>
</chart>
```

### Drilldown {#drilldown}

Una serie con `drilldown-serie` abre otra serie cuando el usuario hace clic en uno de sus puntos. La serie de destino establece `drilldown="true"` y no se dibuja hasta que se abre. Un enlace **Back to Themes** (el nombre de la primera serie) en la parte superior izquierda del gráfico devuelve al primer nivel (el texto procede del `drillUpText` de los archivos de textos del gráfico). El parámetro `keys` de la primera serie enlaza los valores de cada punto con el `name`, el valor `y` y el destino `drilldown`.

<img alt="Segundo nivel de un gráfico de sectores con drilldown y un enlace de retroceso arriba a la izquierda" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-drilldown.png')} />

``` xml
<chart id="ChrPieTst" label="Grafico 6" type="pie" initial-load="query" enable-data-labels="true"
       format-data-labels="&lt;b&gt;{point.name}&lt;/b&gt;: {point.percentage:.1f} %"
       target-action="TstChrPieDrillSrc" max="5">
  <chart-legend enabled="true" />
  <x-axis label="Themes" />
  <y-axis label="Percent (%)" />
  <chart-tooltip suffix=" %" number-decimals="2" />
  <chart-serie id="serie1" x-value="names" y-value="serie1" label="Themes" drilldown-serie="serie1_1">
    <chart-parameter type="array" name="keys">
      <chart-parameter type="string" value="name" name=""/>
      <chart-parameter type="string" value="y" name=""/>
      <chart-parameter type="string" value="drilldown" name=""/>
    </chart-parameter>
  </chart-serie>
  <chart-serie id="serie1_1" drilldown="true" x-value="names" y-value="subserie1" label="SubThemes" />
</chart>
```

La consulta devuelve los valores de ambos niveles en la misma fila:

``` xml
<query id="TstChrPieDrillSrc" distinct="true">
  <table id="AweThm"/>
  <field id="Nam" alias="names"/>
  <computed format="parseInt(Math.random()*100,10)" eval="true" alias="serie1" transform="NUMBER"/>
  <computed format="(parseInt(Math.random()*100,10))+2" eval="true" alias="subserie1" transform="NUMBER"/>
</query>
```

### Gráfico de dispersión {#scatter-chart}

Tipo `scatter`: un punto por cada fila, sin línea. Los valores x e y son números o fechas.

<img alt="Gráfico de dispersión con una serie y un eje de fechas" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-scatter.png')} />

``` xml
<chart id="ChrScaTst" label="Grafico 10" subtitle="Subtitulo grafico 10" type="scatter" initial-load="query"
       target-action="TstChrOneSrcThrAxs" max="21">
  <chart-legend layout="vertical" align="right" verticalAlign="middle" />
  <x-axis label="SCREEN_TEXT_CHART_AXIS_DATES" type="datetime" />
  <y-axis label="Temperaturas (ºC)" />
  <chart-tooltip suffix=" ºC" number-decimals="3" />
  <chart-serie id="serie2-1" x-value="Ord" y-value="serie1_1" z-value="serie1_2" label="SCREEN_TEXT_CHART_SERIE_1" />
</chart>
```

### Gráfico de burbujas {#bubble-chart}

Tipo `bubble`: un gráfico de dispersión en el que el `z-value` de la serie da el tamaño de cada burbuja. El rango de tamaños se establece con `minSize` y `maxSize`, y las etiquetas de datos pueden mostrar el valor z con `{point.z}`.

<img alt="Gráfico de burbujas donde el tamaño de cada burbuja es su valor z, etiquetado con el mismo valor" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-bubble-labels.png')} />

``` xml
<chart id="ChrAdvBubble" label="Rentabilidad frente a riesgo" type="bubble" initial-load="query"
       target-action="ChrAdvBubbleSrc">
  <chart-legend enabled="false" />
  <x-axis label="Riesgo (%)" />
  <y-axis label="Rentabilidad (%)" label-format="{value:.2f}" />
  <chart-serie id="funds" x-value="risk" y-value="return" z-value="weight" label="Fondos" />
  <chart-parameter type="object" name="plotOptions">
    <chart-parameter type="object" name="series">
      <chart-parameter type="object" name="dataLabels">
        <chart-parameter type="boolean" name="enabled" value="true" />
        <chart-parameter type="string" name="format" value="{point.z}" />
      </chart-parameter>
    </chart-parameter>
    <chart-parameter type="object" name="bubble">
      <chart-parameter type="integer" name="minSize" value="10" />
      <chart-parameter type="integer" name="maxSize" value="40" />
    </chart-parameter>
  </chart-parameter>
</chart>
```

Sin `minSize` ni `maxSize`, los tamaños van de 10 a 50 píxeles:

<img alt="Gráfico de burbujas con los tamaños por defecto" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-bubble.png')} />

### Gráfico bursátil con zoom {#stock-chart-with-zoom}

Con `stock-chart="true"` el gráfico está pensado para series temporales largas. El motor React añade un deslizador (el navegador) bajo el área del gráfico, un zoom del eje x con la rueda del ratón y no muestra leyenda por defecto.

<img alt="Gráfico de series temporales con un deslizador de navegación bajo el área del gráfico" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-stock.png')} />

``` xml
<chart style="expand-3x" id="ChrStockTst" label="Grafico 4" stock-chart="true" zoom-type="xAxis" type="line"
       initial-load="query" target-action="TstChrThrDatSrcHor" max="30">
  <x-axis label="Fechas" type="datetime" />
  <y-axis label="Temperaturas (ºC)" label-format="{value} ºC"/>
  <chart-tooltip suffix=" ºC" number-decimals="2" />
  <chart-serie id="serie1" x-value="dates" y-value="serie1" label="Serie 1" />
</chart>
```

`zoom-type` (`xAxis`, `yAxis` o `all`) añade el zoom de los ejes a cualquier gráfico, no solo a los bursátiles.

## Acciones de gráfico {#chart-actions}

El servidor puede modificar un gráfico desde un servicio, mediante acciones de cliente. Las acciones se construyen con las clases de `com.almis.awe.builder.client.chart` y se enumeran en la página de [acciones](actions.md):

| Action                 | Builder                           | Effect                                                   |
| ---------------------- | --------------------------------- | -------------------------------------------------------- |
| `add-chart-series`     | `AddChartSeriesActionBuilder`     | Añade series (una serie con el mismo id se sustituye)      |
| `remove-chart-series`  | `RemoveChartSeriesActionBuilder`  | Elimina las series con los ids indicados                    |
| `replace-chart-series` | `ReplaceChartSeriesActionBuilder` | Sustituye todas las series                                  |
| `add-points`           | `AddPointsActionBuilder`          | Añade las filas de un `DataList` a los valores del gráfico |

Las series que llevan las acciones son objetos `ChartSerie`, con el id, el nombre, los nombres `x-value` e `y-value` y los puntos. El servidor traduce cada una a ECharts, igual que hace con los elementos `chart-serie` del XML, y envía la traducción con la acción. El gráfico no necesita declarar series en su XML: la pantalla del ejemplo no tiene elementos `chart-serie`.

```java
public ServiceData replaceSeriesChart(List<String> userList) {
  List<ChartSerie> series = new ArrayList<>();
  for (String user : userList) {
    ChartSerie serie = new ChartSerie();
    serie.setId(user);
    serie.setName(user);
    serie.setXValue("month");
    serie.setYValue(user + "-y");
    serie.setData(months.stream()
      .map(month -> new ChartSeriePoint(factory.textNode(month), factory.numberNode(random.nextInt(11))))
      .toList());
    series.add(serie);
  }
  return new ServiceData().addClientAction(new ReplaceChartSeriesActionBuilder("ChrLinTst", series).build());
}
```

<img alt="Gráfico de líneas tras una acción de sustitución de series, con los meses en un eje de categorías" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-dynamic-series.png')} />

La pantalla `ChrTstDynamicSeries` de la aplicación de pruebas tiene tres botones que llaman a servicios como este (añadir, eliminar y sustituir series).

### Dependencias {#dependencies}

Una dependencia con `target-type="chart-options"` cambia las opciones de un gráfico en tiempo de ejecución. Por ejemplo, para sincronizar el zoom de dos gráficos:

```xml
<dependency source-type="formule" target-type="chart-options" formule="{zoom: {x: {min: [xMin],max: [xMax]}}}">
  <dependency-element id="ChrStockTst" attribute="xMin" alias="xMin" optional="true"/>
  <dependency-element id="ChrStockTst" attribute="xMax" alias="xMax" optional="true"/>
  <dependency-element id="ChrStockTst" event="zoom"/>
</dependency>
```

El resultado de la fórmula es un objeto de opciones de **Highcharts**, por lo que solo lo aplica el motor AngularJS. El motor React no lo aplica: el cliente registra una advertencia en la consola del navegador (`The 'chart-options' dependency is not applied to charts drawn with ECharts`) y deja el gráfico como está. Las demás dependencias de un gráfico (visibilidad, atributos, `reload`...) funcionan en ambos motores. Véase [dependencias](dependencies.md).

## Diferencias entre los motores {#differences-between-the-engines}

| Característica                                      | React (ECharts)                                                                     | AngularJS (Highcharts)                          |
| -------------------------------------------- | ----------------------------------------------------------------------------------- | ----------------------------------------------- |
| Tipos 3D (`column_3d`, `pie_3d`, `donut_3d`) | Dibujado **plano**                                                                      | Dibujado en 3D                                     |
| Opciones de `chart-parameter`                    | Solo las [opciones traducidas](chart-parameters.md); el resto se ignoran y se registran | Cualquier opción de la API de Highcharts                |
| Dependencia `chart-options`                   | No se aplica (advertencia en la consola)                                                | Se aplica                                         |
| Atributo `theme` (temas de Highcharts)        | Se ignora. El aspecto proviene del tema de la aplicación                           | Se aplica                                         |
| Paleta por defecto                              | La paleta de Highcharts 11, para las series sin color                             | La paleta de Highcharts, o la del tema |
| Modo oscuro                                    | Los gráficos siguen el tema de la aplicación (véase más abajo)                          | Temas de Highcharts (atributo `theme`)           |
| Gráficos bursátiles                                 | Deslizador y zoom con la rueda del ratón                                                | Highstock: navegador, selector de rango, barra de desplazamiento |
| Impresión                                     | Imagen dibujada por el renderizador SVG de ECharts                                          | Exportación de Highcharts                               |
| Textos del gráfico                                  | Archivos `src/i18n/charts/charts-<language>.json`                                | Textos del cliente AngularJS                   |

### Límites conocidos del motor React {#known-limits-of-the-react-engine}

* **Los gráficos 3D son planos.** Los tipos 3D se aceptan, de modo que las pantallas existentes siguen funcionando, pero la profundidad no se dibuja.
* **Colores.** Cuando el XML no indica colores, las series toman los colores de la paleta por defecto de Highcharts 11 (`#2caffe`, `#544fc5`, `#00e272`, `#fe6a35`, `#6b8abc`, `#d568fb`, `#2ee0ca`, `#fa4b42`, `#feb56a`, `#91e8e1`). Establezca el `color` de las series, o el parámetro `colors` del gráfico, para usar los suyos.
* **Tema oscuro.** El gráfico lee el tema de la aplicación cuando se crea y se dibuja con colores claros u oscuros. Si el usuario cambia el tema con la pantalla abierta, el gráfico adopta el nuevo tema cuando se vuelve a crear (por ejemplo, al abrir de nuevo la pantalla o al cambiar el idioma).
* **Drilldown e impresión.** Al imprimir un gráfico con drilldown se imprime el nivel superior.
* **Valores sin procesar.** Los valores se dibujan tal como los devuelve la consulta: nada se redondea salvo que lo indique un formato o `number-decimals`.
* **Opciones de Highcharts.** Las opciones sin equivalente en ECharts se ignoran. Consulte el log del servidor, que informa de cada una una sola vez (véase [parámetros de gráficos](chart-parameters.md#everything-else)).
* **Temas de Highcharts.** El atributo `theme` solo se aplica al motor AngularJS.
* **Tipos de rango.** `arearange` y `areasplinerange` se aproximan mediante una línea rellena.

## Temas de Highcharts (motor AngularJS) {#highcharts-themes-angularjs-engine}

> **Nota:** Esta sección solo se aplica al motor AngularJS. El motor React ignora el atributo `theme`.

El motor AngularJS y la biblioteca Highcharts permiten cambiar el estilo fácilmente. Hay algunos temas por defecto. Basta con establecer el atributo `theme` del elemento chart con el nombre del tema.

La lista de temas disponible es:

-   dark-unica
-   dark-green
-   dark-blue
-   gray
-   grid

### Creando tu propio tema {#creating-your-own-theme}

Aquí hay un ejemplo sencillo para mostrar el proceso:

```javascript
/**
 * Theme for Highcharts JS
 */

Highcharts.theme["your-theme"] = {
colors: ['#058DC7', '#50B432', '#ED561B', '#DDDF00', '#24CBE5', '#64E572', 
             '#FF9655', '#FFF263', '#6AF9C4'],
    chart: {
        backgroundColor: {
            linearGradient: [0, 0, 500, 500],
            stops: [
                [0, 'rgb(255, 255, 255)'],
                [1, 'rgb(240, 240, 255)']
            ]
        },
    },
    title: {
        style: {
            color: '#000',
            font: 'bold 16px "Trebuchet MS", Verdana, sans-serif'
        }
    },
    subtitle: {
        style: {
            color: '#666666',
            font: 'bold 12px "Trebuchet MS", Verdana, sans-serif'
        }
    },

    legend: {
        itemStyle: {
            font: '9pt Trebuchet MS, Verdana, sans-serif',
            color: 'black'
        },
        itemHoverStyle:{
            color: 'gray'
        }   
    }
};

```

> **Nota:** Debe añadir el nuevo nombre de archivo en **scripts.xml** en su proyecto.

## Impresión y renderizado en el servidor {#printing-and-server-rendering}

**Impresión en el motor React.** Cuando el usuario imprime una pantalla con un gráfico, el cliente dibuja una imagen del gráfico aparte de la pantalla, con el renderizador SVG de ECharts, en colores claros y con el tamaño de la página. La imagen se dibuja a partir del nivel superior del gráfico, incluso cuando el usuario ha hecho drilldown.

**Renderizado en el servidor.** El `ChartService` descrito a continuación es independiente del motor del navegador: construye el modelo de Highcharts del gráfico y lo envía a un servidor de exportación de Highcharts, que devuelve una imagen SVG. Sigue necesitando el servidor de exportación (`highcharts.server.url`).

## Servicio de exportación de gráficas {#chart-render-service}

Highcharts tiene un servidor de exportación que permite generar gráficos de alta calidad en el lado del servidor:

https://www.highcharts.com/docs/export-module/setting-up-the-server

Hemos desarrollado un nuevo servicio de renderizado que permite generar gráficos en formato `SVG` usando este servidor de exportación y los gráficos de AWE.

### Definiendo gráficos {#defining-charts}

Definir un gráfico que se generará en el servidor es similar a definir un gráfico que se mostrará en un navegador.

Usted define una pantalla y dentro del gráfico, como se muestra antes. Esta pantalla puede estar o no en la aplicación, no importa.

Los gráficos definidos para ser generados en el servidor no necesitan atributos `server-action` y `target-action`, ya que los datos se van a pasar como parámetros.

### Uso {#usage}

Hay un nuevo servicio diseñado para generar gráficas en el lado del servidor, llamado `ChartService`. Este servicio tiene dos métodos que permiten al desarrollador generar cartas usando una sola fuente de datos o múltiples fuentes de datos:

#### Renderizar un gráfico con un solo `DataList` {#render-a-chart-with-a-single-datalist}

El método de renderizado de gráficos (con un solo `DataList`) genera un gráfico usando únicamente un DataList como origen de datos.

```java
public String renderChart(String screenName, String chartName, DataList data) throws AWException
```

* `screenName` es el nombre del archivo de pantalla donde está el gráfico
* `chartName` es el `id` del gráfico que se generará
* `data` es el `DataList` de origen

Este método devolverá un gráfico en formato SVG+XML (o AWException si hay un error).

#### Renderizar un gráfico con varios `DataList` {#render-a-chart-with-multiple-datalist}

El método de renderizado de gráficos (con un mapa de `DataList`) genera un gráfico usando varios DataList.

```java
public String renderChart(String screenName, String chartName, Map<String, DataList> datasources) throws AWException
```

* `screenName` es el nombre del archivo de pantalla donde está el gráfico
* `chartName` es el `id` del gráfico que se generará
* `datasources` es un mapa de String y DataList que contendrá todas las fuentes de datos.

Este método devolverá un gráfico en formato SVG+XML (o AWException si hay un error).

Uno de los datos debe llamarse `main` y será el predeterminado; el resto coincidirá con un parámetro `datasource` definido en la serie que debería seleccionar los datos. Por ejemplo, esta serie seleccionaría el DataList que hay dentro de la clave de mapa `detail`:

```xml
<chart-serie id="data" x-value="name" y-value="value"/>
<chart-serie id="detail" x-value="name" y-value="value">
  <chart-parameter type="string" name="datasource" value="detail"/>
</chart-serie>
```

el mapa correspondiente para esta serie debe ser:

```java
Map<String, DataList> datasources = new HashMap<>();
datasources.put("main", mainDataList);
datasources.put("detail", detailDataList);
```

La primera serie (`data`) elegirá los datos definidos en la clave `main` y la segunda serie (`detail`) elegirá los datos definidos en la clave `detail`

### Procesar servidor {#render-server}

Puede configurar el servidor de renderizado en el archivo `application.properties` actualizando la propiedad `highcharts.server.url`:

```properties
################################################
# Chart properties
################################################
highcharts.server.url=http://export.highcharts.com
```

El servidor de exportación predeterminado apunta al servidor de exportación de Highcharts: `http://export.highcharts.com`

## Actualización a AWE 5 {#upgrading-to-awe-5}

En AWE 5 el motor React dibuja los gráficos con [Apache ECharts](https://echarts.apache.org/) 6.1.0 en lugar de Highcharts. El motor AngularJS sigue dibujándolos con Highcharts hasta que pase a ECharts (issue #775).

El XML del gráfico no cambia. El servidor traduce el gráfico, sus ejes, series y elementos `chart-parameter` a un modelo de ECharts y registra una advertencia (una vez por opción) por cada opción que no puede traducir. Revise estos puntos al actualizar una aplicación que usa el motor React:

* **Revise el log del servidor.** Abra cada pantalla con un gráfico y busque las advertencias que empiecen por `Highcharts chart-parameter`. Cada una es una opción que el motor React ignora. Véase [parámetros de gráficos](chart-parameters.md) para las opciones que se traducen.
* **Código propio que importa Highcharts.** El cliente React ya no depende de `highcharts` ni de `highcharts-react-official`. Un componente propio de su aplicación que los importe debe declararlos en su propio `package.json` (compruebe la licencia de Highcharts) o pasar a ECharts, que el cliente ya incluye.
* **Reglas CSS `.highcharts-*`.** Los gráficos ya no los dibuja Highcharts, por lo que estas reglas no coinciden con nada en el motor React. Los colores, las fuentes y los bordes provienen del XML (`chart-parameter`) y del tema. El modo oscuro de la aplicación se aplica automáticamente a los gráficos cuando se crea el gráfico.
* **Paleta.** Las series sin color propio mantienen la paleta por defecto de Highcharts 11. Establezca `color` en la serie o el parámetro `colors` en el gráfico para cambiarla.
* **Temas de Highcharts.** El motor React ignora el atributo `theme`. Si un tema daba colores o fuentes a sus gráficos, establézcalos en el XML.
* **Gráficos 3D** (`column_3d`, `pie_3d`, `donut_3d`): se dibujan planos.
* **Dependencia `chart-options`.** Su resultado es un objeto de opciones de Highcharts. No se aplica a los gráficos dibujados con ECharts y el cliente registra una advertencia en la consola del navegador.
* **Formatos.** Las cadenas de formato de Highcharts 11 (`{point.y:.2f}`, `{#if ...}`, los patrones de fecha como `%Y-%m-%d`...) funcionan como antes, véase [formatos de gráficos](chart-format.md). Las especificaciones numéricas distintas de `.Nf` y `,.Nf` se escriben como el valor sin procesar, y los valores nunca se redondean salvo que un formato lo pida.
* **Impresión.** Los gráficos se imprimen a partir de una imagen dibujada por ECharts. Un gráfico con drilldown imprime su nivel superior.
* **Textos del gráfico.** Los archivos de idioma de los textos del gráfico son `src/i18n/charts/charts-<language>.json` en el cliente React, y solo contienen los textos que usan los gráficos: `noData`, `drillUpText`, `decimalPoint`, `thousandsSep`, `months`, `shortMonths` y `weekdays`. Copie sus personalizaciones a esos archivos.
* **Renderizado en el servidor.** `ChartService` sigue usando el servidor de exportación de Highcharts y el modelo de Highcharts del gráfico, por lo que sigue siendo necesario `highcharts.server.url`.
