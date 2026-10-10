---
id: chart-parameters
title: Parámetros de gráficas
---

El elemento `chart-parameter` añade o sobrescribe opciones de Highcharts de una gráfica. Es el punto de extensión del XML de
gráficas: todo lo que los atributos XML no cubren (colores, estilos de las etiquetas de datos, marcadores, rellenos con degradado, nombres de apilado,
separación de barras, formatos de tooltip...) se define con él.

El mismo elemento lo usan ambos motores, pero cada uno lo lee a su manera:

* **Motor AngularJS (Highcharts).** Las opciones se fusionan tal cual en la configuración de Highcharts, por lo que cualquier
  opción de la [API de Highcharts](https://api.highcharts.com/highcharts) es válida.
* **Motor React (Apache ECharts).** El servidor traduce las opciones que tienen equivalente en ECharts al
  `echartsModel` de la gráfica. Cualquier otra opción se **ignora** y se notifica una vez, con un aviso en el log del servidor.
  Las opciones siguen llegando sin cambios al `chartModel` de Highcharts, por lo que la misma pantalla sigue funcionando en el motor
  AngularJS.

Esta página lista qué opciones se traducen, cuáles se aproximan y cuáles se ignoran. La tabla se
deriva de la tabla de traducción del servidor (`HighchartsParameterTable` y `SeriesParameterRules` en
`awe-model`).

## El elemento {#the-element}

| Atributo  | Uso          | Tipo   | Descripción                                                                                            |
| --------- | ------------ | ------ | ------------------------------------------------------------------------------------------------------ |
| type      | **Obligatorio** | String | `string`, `integer`, `long`, `float`, `double`, `boolean`, `array`, `object` o `null`               |
| name      | **Obligatorio** | String | Nombre de la opción de Highcharts. Usa un nombre vacío para los elementos de un `array`             |
| value     | Opcional     | String | Valor de la opción. Los parámetros `object` y `array` contienen elementos `chart-parameter` anidados en su lugar |

Los parámetros `object` anidados construyen la ruta de una opción: la opción `dataLabels.style.fontSize` se escribe como tres
parámetros anidados.

```xml
<chart-parameter type="object" name="dataLabels">
  <chart-parameter type="boolean" name="enabled" value="true"/>
  <chart-parameter type="object" name="style">
    <chart-parameter type="string" name="fontSize" value="11px"/>
  </chart-parameter>
</chart-parameter>
```

## Dónde poner un parámetro {#where-to-put-a-parameter}

El elemento que contiene el `chart-parameter` decide el ámbito de la opción, que es la raíz de su ruta:

| Elemento padre                    | Ámbito  | Raíz de la ruta de la opción                             |
| --------------------------------- | ------- | -------------------------------------------------------- |
| `chart`                           | gráfica | La configuración de Highcharts (`title`, `colors`, `plotOptions`, `chart`...) |
| `x-axis`, `y-axis`                | eje     | El eje (`labels`, `dateTimeLabelFormats`, `gridLineWidth`...) |
| `chart-tooltip`                   | tooltip | El tooltip (`headerFormat`, `useHTML`...)                 |
| `chart-legend`                    | leyenda | La leyenda                                                |
| `chart-serie`                     | serie   | La serie (`color`, `dataLabels`, `marker`, `stack`...)    |

Las opciones de una serie pueden escribirse en la propia serie o, como valores por defecto para todas las series o para las series de un
tipo, en `plotOptions.series.*` y `plotOptions.<type>.*` del ámbito de la gráfica (por ejemplo `plotOptions.pie.size`).
Las opciones de la serie prevalecen sobre las de `plotOptions.<type>`, que prevalecen sobre las de `plotOptions.series`.

Las opciones sin tratar de cada serie también están disponibles para el [lenguaje de formato](chart-format.md) como
`series.userOptions.<name>`, incluidos nombres personalizados como `fullname`.

## Tabla de traducción (motor React) {#translation-table-react-engine}

Las tablas usan estas marcas:

* **Traducida**: la opción tiene equivalente en ECharts y se aplica.
* **Aproximada**: se usa el comportamiento más cercano de ECharts. El servidor registra un aviso una vez.
* **Ignorada**: la opción se descarta. El servidor registra un aviso una vez.

### Ámbito de gráfica {#chart-scope}

| Opción                                   | Resultado       | Notas                                                                                         |
| ---------------------------------------- | --------------- | --------------------------------------------------------------------------------------------- |
| `title` (string) y `title.text`          | Traducida       | Texto del título                                                                              |
| `title.align`                            | Traducida       | Posición horizontal del título (`left`, `center`, `right`)                                    |
| `title.verticalAlign`                    | Traducida       | Posición vertical del título (`top`, `middle`, `bottom`)                                      |
| `title.y`                                | Traducida       | Desplazamiento vertical en píxeles. ECharts no puede desplazar un título alineado al centro, por lo que lo aplica el cliente |
| `colors`                                 | Traducida       | Paleta de la gráfica. Sustituye a la paleta por defecto                                       |
| `plotOptions.pie.size`                   | Traducida       | Radio exterior del gráfico circular (porcentaje o píxeles)                                    |
| `plotOptions.pie.innerSize`              | Traducida       | Radio interior de un donut. Es relativo a `size`, como en Highcharts                          |
| `chart.alignTicks`                       | Traducida       | Las marcas de los ejes de valores secundarios se alinean con las del primer eje               |
| `chart.alignThresholds`                  | Aproximada      | Aproximada mediante marcas alineadas (`alignTicks`)                                           |
| `tickAmount` (raíz de la gráfica)        | Ignorada        | Se descarta sin aviso: tampoco tiene efecto en Highcharts. Defínela en el eje                 |

### Ámbito de serie y `plotOptions` {#series-scope-and-plotoptions}

Cada una de estas opciones es válida en un `chart-serie` y en `plotOptions.series` y `plotOptions.<type>`.

| Opción                                  | Resultado    | Notas                                                                                       |
| --------------------------------------- | ------------ | ------------------------------------------------------------------------------------------- |
| `dataLabels.enabled`                    | Traducida    | Muestra las etiquetas de datos de la serie                                                  |
| `dataLabels.format`                     | Traducida    | [Formato](chart-format.md) de las etiquetas. Se eliminan las etiquetas HTML                 |
| `dataLabels.distance`                   | Traducida    | En un gráfico circular, un valor negativo coloca la etiqueta dentro del sector; en caso contrario es la longitud de la línea guía |
| `dataLabels.style.fontSize`             | Traducida    |                                                                                             |
| `dataLabels.style.fontWeight`           | Traducida    |                                                                                             |
| `dataLabels.style.color`                | Traducida    |                                                                                             |
| `dataLabels.style.textShadow`           | Traducida    | Forma CSS `<x> <y> [<blur>] <color>`. Las palabras clave `contrast` y `none` se descartan   |
| `dataLabels.connectorColor`             | Traducida    | Color de la línea guía de un gráfico circular                                               |
| `stack`                                 | Traducida    | Nombre del apilado de la serie. Las series con el mismo nombre se apilan juntas             |
| `stacking`                              | Traducida    | `normal` o `percent`. Otros valores se ignoran                                              |
| `groupPadding`                          | Traducida    | Espacio entre grupos de columnas. Se convierte en un porcentaje del ancho de la barra       |
| `pointPadding`                          | Traducida    | Espacio entre las columnas de un grupo. Se convierte en un porcentaje del ancho de la barra |
| `pointWidth`                            | Traducida    | Ancho de las columnas en píxeles                                                            |
| `borderRadius`                          | Traducida    | Píxeles o porcentaje. Se redondea el extremo de la barra que está más lejos de cero, solo en la serie más externa de un apilado |
| `color`                                 | Traducida    | Color de la serie                                                                           |
| `borderColor`                           | Traducida    |                                                                                             |
| `colorByPoint`                          | Traducida    | Cada punto toma un color de la paleta                                                       |
| `allowPointSelect`                      | Traducida    | Los puntos se pueden seleccionar con un clic                                                |
| `lineWidth`                             | Traducida    |                                                                                             |
| `dashStyle`                             | Traducida    | Los nombres de Highcharts se reducen a `solid`, `dashed` o `dotted`                         |
| `fillColor`                             | Traducida    | Color del área                                                                              |
| `fillColor.linearGradient.x1`, `y1`, `x2`, `y2` | Traducida | Dirección del degradado. Vertical, de arriba abajo, por defecto                     |
| `fillColor.stops`                       | Traducida    | Colores del degradado: array de pares `[offset, color]`. Cada parada lleva su propio alfa   |
| `threshold`                             | Aproximada   | `0` y `-Infinity` se traducen. Cualquier otro umbral rellena el área desde cero             |
| `marker.enabled`                        | Traducida    | `false` oculta los símbolos de una línea y hace invisibles los puntos de un scatter         |
| `marker.radius`                         | Traducida    |                                                                                             |
| `marker.fillColor`                      | Traducida    |                                                                                             |
| `marker.lineColor`                      | Traducida    |                                                                                             |
| `marker.lineWidth`                      | Traducida    |                                                                                             |
| `enableMouseTracking`                   | Traducida    | `false` elimina la serie del tooltip y de la interacción con el ratón                       |
| `showInLegend`                          | Traducida    | `false` deja la serie fuera de la leyenda                                                   |
| `tooltip.valueSuffix`                   | Traducida    | Sufijo de los valores de la serie en el tooltip. Sustituye al de `chart-tooltip`            |
| `minSize`, `maxSize`                    | Traducida    | Rango de los tamaños de burbuja, en píxeles o como porcentaje                               |

Estas opciones solo son válidas en un `chart-serie`:

| Opción          | Resultado  | Notas                                                                                                  |
| --------------- | ---------- | ------------------------------------------------------------------------------------------------------ |
| `linkedTo`      | Traducida  | `:previous` o el id de una serie anterior. Una serie enlazada se oculta y se muestra con la serie a la que está enlazada, y no tiene entrada en la leyenda |
| `legendIndex`   | Traducida  | Posición de la serie en la leyenda                                                                     |
| `keys`          | Traducida  | Asocia los valores de un punto a `name`, `y` y `drilldown`, para gráficas con [drilldown](chart.md#drilldown) |
| `fullname`      | Traducida  | Clave personalizada de las pantallas. No cambia la gráfica, los formatos la leen como `series.userOptions.fullname` |

### Ámbito de eje {#axis-scope}

| Opción                       | Resultado  | Notas                                                                                           |
| ---------------------------- | ---------- | ----------------------------------------------------------------------------------------------- |
| `dateTimeLabelFormats.<unit>` | Traducida | Patrón de fecha de las etiquetas de un eje de tiempo para una unidad: `year`, `month`, `day`, `hour`, `minute`, `second`, `millisecond` |
| `labels.format`              | Traducida  | [Formato](chart-format.md) de las etiquetas. El valor es `{value}`                              |
| `gridLineWidth`              | Traducida  | `0` oculta las líneas de la cuadrícula                                                          |
| `tickAmount`                 | Traducida  | Número aproximado de marcas del eje                                                             |

### Ámbito de tooltip {#tooltip-scope}

| Opción          | Resultado  | Notas                                                                                               |
| --------------- | ---------- | --------------------------------------------------------------------------------------------------- |
| `useHTML`       | Traducida  | Las piezas del tooltip son bloques html (filas de tabla) que se unen tal cual                       |
| `headerFormat`  | Traducida  | [Formato](chart-format.md) de la cabecera, mostrada una vez                                         |
| `pointFormat`   | Traducida  | Igual que el atributo `point-format`                                                                |
| `footerFormat`  | Traducida  | [Formato](chart-format.md) del pie, mostrado una vez                                                |
| `style.fontSize` | Traducida | Tamaño de fuente del tooltip                                                                        |
| `distance`      | Ignorada   | El tooltip de ECharts sigue al puntero con un desplazamiento fijo. El servidor registra un aviso    |

### Ámbito de leyenda {#legend-scope}

Todas las opciones de un `chart-parameter` dentro de `chart-legend` se **ignoran**. Usa los atributos de
[`chart-legend`](chart.md#legend-element).

### Todo lo demás {#everything-else}

Cualquier opción que no esté en las tablas se **ignora**. La primera vez que el servidor la encuentra, escribe un aviso:

```text
Highcharts chart-parameter 'plotOptions.series.shadow' has no ECharts translation yet (scope: chart); it is ignored by the ECharts model
```

Cada opción se notifica una vez mientras el servidor está en ejecución (el servidor recuerda hasta 500 opciones distintas). Revisa el log
de la aplicación después de migrar una pantalla, y elimina las opciones que se notifiquen o cambia la
gráfica para usar los atributos del XML.

### Aproximaciones que el servidor también notifica {#approximations-that-the-server-also-reports}

| Caso                                                                     | Resultado en el motor React                                   |
| ------------------------------------------------------------------------ | ------------------------------------------------------------- |
| Series de tipo `arearange` y `areasplinerange`                           | Se dibujan como una línea rellena (aviso)                     |
| Un `type` de serie que no está en la tabla de tipos de gráfica            | Se dibuja como una línea (aviso)                              |
| `crosshairs="yAxis"` en `chart-tooltip`                                  | Se dibuja como el par de líneas guía de ambos ejes (sin aviso) |

## Ejemplos {#examples}

**Cambiar el formato de las etiquetas de un eje de fechas.**

```xml
<x-axis label="Dates" type="datetime">
  <chart-parameter type="object" name="dateTimeLabelFormats">
    <chart-parameter type="string" name="day" value="%Y-%m-%d"/>
  </chart-parameter>
</x-axis>
```

**Cambiar la posición del título de una gráfica.**

```xml
<chart-parameter type="object" name="title">
  <chart-parameter type="string" name="align" value="center"/>
  <chart-parameter type="string" name="verticalAlign" value="middle"/>
  <chart-parameter type="integer" name="y" value="50"/>
</chart-parameter>
```

**Cambiar las opciones de trazado de los gráficos circulares de una gráfica.**

```xml
<chart-parameter type="object" name="plotOptions">
  <chart-parameter type="object" name="pie">
    <chart-parameter type="string" name="size" value="75%"/>
    <chart-parameter type="string" name="innerSize" value="40%"/>
    <chart-parameter type="object" name="dataLabels">
      <chart-parameter type="boolean" name="enabled" value="true"/>
      <chart-parameter type="integer" name="distance" value="-20"/>
      <chart-parameter type="object" name="style">
        <chart-parameter type="string" name="fontWeight" value="bold"/>
        <chart-parameter type="string" name="color" value="white"/>
        <chart-parameter type="string" name="textShadow" value="0px 1px 2px black"/>
      </chart-parameter>
    </chart-parameter>
  </chart-parameter>
</chart-parameter>
```

**Definir la paleta de una gráfica.** Los elementos de un array tienen un nombre vacío.

```xml
<chart-parameter type="array" name="colors">
  <chart-parameter type="string" name="" value="rgba(201,229,134,0.9)"/>
  <chart-parameter type="string" name="" value="rgba(140,172,65,0.9)"/>
  <chart-parameter type="string" name="" value="rgba(47,111,143,0.9)"/>
</chart-parameter>
```
