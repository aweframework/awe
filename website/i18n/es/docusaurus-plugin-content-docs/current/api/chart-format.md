---
id: chart-format
title: Formatos de gráficas
---

Los textos que dibuja una gráfica (etiquetas de datos, tooltips y etiquetas de los ejes) se describen con **cadenas de formato**: un texto con
etiquetas `{...}` que se sustituyen por valores. El lenguaje es el lenguaje de plantillas de Highcharts 11. El motor React
interpreta en el navegador el subconjunto descrito en esta página, por lo que un formato que solo use ese subconjunto produce el mismo
texto con ECharts. Cualquier otra especificación de número (salvo `.Nf` y `,.Nf`) escribe el valor sin tratar: revísalas cuando
migres una gráfica al motor React.

```text
<b>{point.name}</b>: {point.percentage:.1f} %
```

## Dónde se usan los formatos {#where-formats-are-used}

| Lugar                | Cómo configurarlo                                                              | Variables                         |
| -------------------- | ------------------------------------------------------------------------------ | --------------------------------- |
| Etiquetas de datos   | Atributo `format-data-labels` de `chart`, o `dataLabels.format` de una serie   | El punto (ver más abajo)          |
| Etiquetas de los ejes | Atributo `label-format` de `x-axis` e `y-axis`, o `labels.format`             | `value`                           |
| Tooltip, línea de punto | Atributo `point-format` de `chart-tooltip`, o el parámetro `pointFormat`    | El punto                          |
| Tooltip, cabecera    | Parámetro `headerFormat` de `chart-tooltip`                                    | El punto                          |
| Tooltip, pie         | Parámetro `footerFormat` de `chart-tooltip`                                    | El punto                          |
| Fecha del tooltip    | Atributo `date-format` de `chart-tooltip`                                      | La fecha del eje                  |

Un formato que no se define usa el texto por defecto de la gráfica: el nombre de la serie y el valor en el tooltip, y
el valor en los ejes.

## Sintaxis {#syntax}

### Expresiones {#expressions}

Una expresión es una variable entre llaves, con una especificación opcional tras dos puntos:

| Expresión                 | Resultado                                               |
| ------------------------- | ------------------------------------------------------- |
| `{point.y}`               | El valor tal como se recibe, sin redondear              |
| `{point.y:.2f}`           | El valor con dos decimales                              |
| `{point.y:,.0f}`          | El valor sin decimales y con separadores de miles       |
| `{point.x:%Y-%m-%d}`      | El valor x (una marca de tiempo) como fecha             |
| `{point.name}`            | El nombre del punto                                     |

Un texto entre llaves que no es una expresión válida se mantiene como texto.

### Especificaciones de número {#number-specifications}

`.Nf` escribe el número con `N` decimales. `,.Nf` añade el separador de miles. El punto decimal y el separador
provienen del idioma de la aplicación (las entradas `decimalPoint` y `thousandsSep` de
`src/i18n/charts/charts-<language>.json` en el cliente React). Cualquier otra especificación de número escribe el valor tal cual.

> **Nota:** Los valores nunca se redondean salvo que el formato (o `number-decimals` en el tooltip) lo pida. Un valor
> de `3.14159265` se dibuja como `3.14159265` cuando no hay especificación.

### Especificaciones de fecha {#date-specifications}

Una especificación que empieza por `%` da formato a una marca de tiempo en milisegundos con la hora local del navegador:

| Código | Significado            | Código | Significado            |
| ------ | ---------------------- | ------ | ---------------------- |
| `%Y`   | Año (2026)             | `%H`   | Horas (00-23)          |
| `%y`   | Año (26)               | `%M`   | Minutos                |
| `%m`   | Número de mes (01-12)  | `%S`   | Segundos               |
| `%d`   | Día del mes (01)       | `%L`   | Milisegundos           |
| `%e`   | Día del mes (1)        | `%b`   | Nombre corto del mes   |
| `%B`   | Nombre del mes         | `%A`   | Nombre del día de la semana |
| `%a`   | Nombre corto del día de la semana | `%%` | Un signo de porcentaje |

Los nombres de los meses y de los días de la semana provienen del archivo de idioma de los textos de las gráficas.

### Condiciones {#conditions}

`{#if <condition>}...{else}...{/if}` escribe un texto u otro. La parte `{else}` es opcional.

```text
{#if (gt y 0)}{y:,.0f}€{else}{(multiply y -1):,.0f}€{/if}
```

### Helpers {#helpers}

Una etiqueta cuya primera palabra es un helper lo invoca con el resto de palabras como argumentos. Los argumentos son números,
variables u otras llamadas entre paréntesis:

| Helper                          | Resultado                   |
| ------------------------------- | --------------------------- |
| `multiply`, `divide`            | Producto, cociente          |
| `add`, `subtract`               | Suma, resta                 |
| `gt`, `lt`, `ge`, `le`          | `>`, `<`, `>=`, `<=`        |
| `eq`, `ne`                      | Igual, distinto             |

```text
{multiply value 0.001}k
{(subtract y 10):.1f}
```

El resultado de un cálculo se redondea para eliminar el ruido de la coma flotante (`0.1 + 0.2` es `0.3`).

## Variables {#variables}

Para las etiquetas de datos y las líneas del tooltip, las variables describen el punto que se dibuja:

| Variable                               | Contenido                                                           |
| -------------------------------------- | ------------------------------------------------------------------- |
| `point.y`, `y`, `value`                | El valor del punto                                                  |
| `point.x`, `x`                         | El valor x (una marca de tiempo en un eje de fechas)                |
| `point.z`, `z`                         | El valor z de una burbuja                                           |
| `point.name`, `name`                   | El nombre del punto (la categoría, o el sector de un gráfico circular) |
| `point.key`, `key`                     | El nombre del punto, o su valor x cuando no tiene nombre            |
| `point.percentage`, `percentage`       | El porcentaje de un sector de un gráfico circular                   |
| `point.color`                          | El color del punto                                                  |
| `series.name`                          | El nombre de la serie                                               |
| `series.color`                         | El color de la serie                                                |
| `series.userOptions.<name>`            | Una opción de la serie escrita como [`chart-parameter`](chart-parameters.md) |

En una gráfica invertida, `x` e `y` mantienen su significado en AWE: `x` es el `x-value` de la serie e `y` su `y-value`.

Las etiquetas de un eje solo tienen la variable `value`.

## Etiquetas de datos y etiquetas de ejes {#data-labels-and-axis-labels}

Las etiquetas de datos y las etiquetas de ejes son texto plano: las etiquetas HTML del formato se eliminan y los saltos de línea se convierten en espacios.

```xml
<chart id="ChrAdvPie" type="pie" enable-data-labels="true"
       format-data-labels="&lt;b&gt;{point.name}&lt;/b&gt;: {point.percentage:.2f} %" ...>
```

> **Nota:** El formato va en un atributo XML, por lo que los caracteres `<`, `>` y `&` hay que escribirlos como `&lt;`,
> `&gt;` y `&amp;`.

<img alt="Gráfico circular con etiquetas de datos que muestran el nombre y el porcentaje de cada sector" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-pie-labels.png')} />

### Formateadores con nombre {#named-formatters}

El atributo `formatter-function` de un eje aplica una función por su nombre. Hay una:

* `formatCurrencyMagnitude`: escribe un número con el símbolo de su magnitud.

| Valor      | Etiqueta |
| ---------- | ------ |
| `300`      | `300`  |
| `1234`     | `1.23K` |
| `100000`   | `100K` |
| `12000000` | `12M`  |

```xml
<y-axis formatter-function="formatCurrencyMagnitude"/>
```

### Ejes de fechas {#date-axes}

Las etiquetas de un eje de fechas se eligen según el nivel de zoom. Define el patrón de fecha de cada unidad con el
parámetro `dateTimeLabelFormats` del eje:

```xml
<x-axis type="datetime">
  <chart-parameter type="object" name="dateTimeLabelFormats">
    <chart-parameter type="string" name="month" value="%B %Y"/>
  </chart-parameter>
</x-axis>
```

## Tooltips {#tooltips}

El tooltip de una gráfica muestra una **cabecera** (una vez), una **línea por cada punto** y un **pie** (una vez). Sin formatos,
la línea es el nombre de la serie y el valor, y la cabecera es el valor del eje.

Los atributos de `chart-tooltip` dan los formatos sencillos:

| Atributo          | Efecto                                                                 |
| ----------------- | ---------------------------------------------------------------------- |
| `number-decimals` | Decimales de los valores                                               |
| `prefix`          | Texto antes del valor                                                  |
| `suffix`          | Texto después del valor                                                |
| `date-format`     | Patrón de fecha de la cabecera de un eje de fechas                     |
| `shared`          | Un único tooltip para todas las series con el mismo valor x            |
| `crosshairs`      | Línea guía de los ejes                                                 |

El `suffix` de una serie (el parámetro `tooltip.valueSuffix`) sustituye al del tooltip.

<img alt="Tooltip compartido de una gráfica mixta, con una línea guía y el valor de cada serie" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-tooltip-shared.png')} />

### Tooltips HTML {#html-tooltips}

Usa `point-format` (o los parámetros `pointFormat`, `headerFormat` y `footerFormat`) para escribir tu propio tooltip. Las
etiquetas de la plantilla se mantienen, incluso sin `useHTML`, como hace Highcharts con su HTML básico (`b`, `i`, `br` y
`span`). Los valores que sustituyen a las
expresiones se escapan, por lo que un valor que contiene `<` se muestra como texto y nunca se convierte en marcado.

Con `useHTML` a `true` las piezas del tooltip se unen tal cual, por lo que se puede construir una tabla con los
tres formatos. Cuando se define un formato de punto, el marcador de color de la serie no se añade antes de su línea, porque un
marcador fuera de una celda de tabla rompería la tabla:

```xml
<chart-tooltip shared="false">
  <chart-parameter type="boolean" name="useHTML" value="true"/>
  <chart-parameter type="string" name="headerFormat"
                   value="&lt;table&gt;&lt;tr&gt;&lt;th colspan='2'&gt;{point.key}&lt;/th&gt;&lt;/tr&gt;"/>
  <chart-parameter type="string" name="pointFormat"
                   value="&lt;tr&gt;&lt;td&gt;{series.userOptions.fullname}: &lt;/td&gt;&lt;td&gt;&lt;b&gt;{#if (gt y 0)}{y:,.0f}€{else}{(multiply y -1):,.0f}€{/if}&lt;/b&gt;&lt;/td&gt;&lt;/tr&gt;"/>
  <chart-parameter type="string" name="footerFormat" value="&lt;/table&gt;"/>
</chart-tooltip>
```

<img alt="Tooltip HTML de una gráfica de pirámide con una cabecera, el nombre de la serie y un valor formateado" src={require('@docusaurus/useBaseUrl').default('img/charts/echarts-tooltip-html.png')} />

Las series de este ejemplo guardan el texto de la línea en una opción personalizada, que el formato lee como
`series.userOptions.fullname`:

```xml
<chart-serie id="men" type="column" x-value="age" y-value="men" label="Men">
  <chart-parameter type="string" name="fullname" value="Average gross salary of men"/>
</chart-serie>
```
