---
id: card
title: Card
---

Una card es una tarjeta de métrica: un título, un valor grande con su unidad, un icono y una variación (delta) con la
flecha de su tendencia y un texto, como **Clientes activos** · 1.284 · ↗ 4,2% frente al mes pasado. También puede
mostrar el progreso hacia un objetivo, una descripción, un pie y, en su cuerpo, cualquier otro componente (un gráfico,
una rejilla, botones...). Sus atributos son valores fijos, y se rellena como un criterio: con `initial-load="query"` la
primera fila de su consulta la rellena. Lo soportan los dos motores web (AngularJS y React).

<img alt="Cards con una barra de progreso, un sparkline y un gauge" src={require('@docusaurus/useBaseUrl').default('img/card.png')} />

---

## Esqueleto de XML {#xml-skeleton}

```xml
<card id="[card-identifier]" title="[card-title]" subtitle="[card-subtitle]" chip="[status-text]"
      chip-tone="[success|warning|danger|info|neutral]" icon="[card-icon]"
      icon-tone="[success|warning|danger|info|neutral]" value="[value]" unit="[unit]" number-decimals="[decimals]"
      delta="[delta]" delta-unit="[delta-unit]" trend="[up|down|flat]" good-trend="[up|down]"
      tone="[success|warning|danger|info|neutral]" caption="[caption]" target="[target]"
      description="[description]" footer="[footer]" style="[card-style]"
      initial-load="query" target-action="[query-identifier]" autorefresh="[seconds]">
  [optional body: any component, for instance a chart or a grid]
</card>
```

## Estructura de la card {#card-structure}

| Elemento                 | Uso             | Múltiples instancias | Descripción                                                  |
|--------------------------|-----------------|----------------------|--------------------------------------------------------------|
| [card](#card-attributes) | **Obligatorio** | No                   | Nodo global de la card. Define los atributos de la card      |
| Cualquier componente     | Opcional        | Sí                   | Cuerpo de la card, que se muestra debajo del valor           |
| [dependency](dependencies.md) | Opcional   | Sí                   | Dependencias de la card, como en cualquier componente        |

## Anatomía {#anatomy}

De arriba abajo, una card muestra solo las partes que tienen valor:

| Parte       | Contenido                                                                                                      |
|-------------|----------------------------------------------------------------------------------------------------------------|
| Cabecera    | `title` y `subtitle` a la izquierda. A la derecha, el `chip` de estado seguido del recuadro del `icon` o, si no hay icono, la variación      |
| Valor       | `value` en cifras grandes, seguido de su `unit`                                                                |
| Cuerpo      | Los hijos de la card (un gráfico, una rejilla, botones...)                                                     |
| Progreso    | Una barra con el valor frente al `target`, y su texto (`82 % de 500.000 €`)                                    |
| Descripción | El párrafo de `description`                                                                                    |
| Pie         | El texto de `footer` a la izquierda. A la derecha, la variación (si hay icono) seguida del `caption`           |

- Con `icon`, la variación va al pie; sin él, la variación va a la cabecera.
- Los hijos van en el cuerpo debajo del valor, no lo sustituyen. Una card sin `value` muestra solo su cuerpo.

## Atributos de la card {#card-attributes}

| Atributo        | Uso             | Tipo    | Descripción                                                                                                    | Valores                                                                                    |
|-----------------|-----------------|---------|----------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| id              | **Obligatorio** | String  | Identificador de la card. Es el destino de las acciones y las dependencias                                     |                                                                                            |
| title           | Opcional        | String  | Título de la card. Si no hay título, se usa `label`                                                            | **Nota:** Puede usar ficheros de [i18n](i18n-internationalization.md) (locales)            |
| label           | Opcional        | String  | Título de la card cuando no se indica `title`                                                                  | **Nota:** Puede usar ficheros de [i18n](i18n-internationalization.md) (locales)            |
| subtitle        | Opcional        | String  | Texto secundario debajo del título                                                                             | **Nota:** Puede usar ficheros de [i18n](i18n-internationalization.md) (locales)            |
| chip            | Opcional        | String  | Estado que se muestra como una etiqueta con un punto arriba a la derecha. Consulte [chip de estado](#status-chip)  | **Nota:** Puede usar ficheros de [i18n](i18n-internationalization.md) (locales)            |
| chip-tone       | Opcional        | String  | Color del chip de estado                                                                                       | `success`, `warning`, `danger`, `info`, `neutral` (por defecto)                            |
| icon            | Opcional        | String  | Icono que se muestra en un recuadro arriba a la derecha. Con icono la variación va al pie; sin él, a la cabecera | **Nota:** Puede consultar todos los juegos de iconos en la pantalla de [iconos](icons.md) (`users`, `mdi:name`, `lucide:name`) |
| icon-tone       | Opcional        | String  | Color del recuadro del icono y de la barra de progreso                                                         | `success`, `warning`, `danger`, `info`, `neutral`. Por defecto: el color primario          |
| value           | Opcional        | String  | Valor principal. Un número se formatea en el idioma del usuario; cualquier otro texto se muestra tal cual      |                                                                                            |
| unit            | Opcional        | String  | Unidad o sufijo del valor y del objetivo                                                                       | `%`, `€`, `ms`...                                                                          |
| number-decimals | Opcional        | Integer | Decimales del valor y del objetivo cuando son números. La variación mantiene siempre hasta 2 decimales         | Por defecto: los decimales que tengan, hasta 2                                             |
| delta           | Opcional        | String  | Variación del valor (por ejemplo `4.2`), que se muestra como una etiqueta con la flecha de la tendencia y su texto. Sin una variación numérica no hay etiqueta |                                     |
| delta-unit      | Opcional        | String  | Unidad de la variación. `%` va pegado al número (`+12,4%`), cualquier otra unidad va tras un espacio (`2,4 pt`) | Por defecto: `%`                                                                           |
| trend           | Opcional        | String  | Dirección de la variación. Cuando se indica, la variación se muestra sin signo                                 | `up`, `down`, `flat`. Por defecto: el signo de la variación                                |
| good-trend      | Opcional        | String  | Dirección que es una buena noticia. Ver [tendencia y tono](#trend-and-tone)                                    | `up` (por defecto), `down` (tasa de errores, costes, tiempo de respuesta)                  |
| tone            | Opcional        | String  | Tono de la variación, que se usa tal cual en lugar del que se deriva de la tendencia                           | `success`, `warning`, `danger`, `info`, `neutral`                                          |
| caption         | Opcional        | String  | Texto junto a la variación (por ejemplo *frente al mes pasado*)                                                | **Nota:** Puede usar ficheros de [i18n](i18n-internationalization.md) (locales)            |
| target          | Opcional        | String  | Objetivo del valor. Cuando el valor y el objetivo son números (y el objetivo no es cero), una barra de progreso muestra el valor frente a él |                                                  |
| progress-style  | Opcional        | String  | Cómo se dibuja el progreso hacia el objetivo                                                                   | `bar` (por defecto), `gauge`                                                     |
| sparkline       | Opcional        | String  | Serie de números dibujada como un gráfico pequeño debajo del valor, como array JSON                            | `[93.1,93.8,92.6]`. La consulta puede devolverla como un campo                   |
| sparkline-type  | Opcional        | String  | Tipo del sparkline                                                                                             | `line`, `area` (por defecto), `bar`                                              |
| description     | Opcional        | String  | Párrafo debajo del valor y del cuerpo                                                                          | **Nota:** Puede usar ficheros de [i18n](i18n-internationalization.md) (locales)            |
| footer          | Opcional        | String  | Texto atenuado abajo a la izquierda de la card                                                                 | **Nota:** Puede usar ficheros de [i18n](i18n-internationalization.md) (locales)            |
| style           | Opcional        | String  | Estilo de la card (clases css)                                                                                 | Clases CSS separadas por espacio (`' '`)                                                   |
| initial-load    | Opcional        | String  | Cómo obtiene la card sus datos al construir la pantalla. Con `query` la primera fila de la consulta destino rellena la card | `query`                                                                        |
| target-action   | Opcional        | String  | Consulta (o servicio) que rellena la card                                                                      |                                                                                            |
| server-action   | Opcional        | String  | Acción de servidor que se usa para refrescar los datos de la card                                              | `data`                                                                                     |
| autorefresh     | Opcional        | Integer | Recarga los datos de la card desde el servidor cada este número de segundos                                    | `0` o vacío: sin refresco automático                                                       |
| autoload        | Opcional        | Boolean | Recarga la card desde el servidor en cuanto se crea, después de mostrar la pantalla                            | `true`, `false` (por defecto)                                                              |

> **Nota:** El esquema también acepta `max`, común a los componentes que cargan datos, pero la card solo lee la primera
> fila.

## Atributos fijos y campos de la consulta {#static-attributes-and-query-fields}

Los atributos de la card son valores fijos. Cuando la card tiene una consulta (`initial-load="query"` y
`target-action`), la primera fila de la consulta la rellena: cada campo se llama como el atributo en camelCase
(`title`, `subtitle`, `chip`, `chipTone`, `icon`, `iconTone`, `value`, `unit`, `numberDecimals`, `delta`, `deltaUnit`, `trend`,
`goodTrend`, `tone`, `caption`, `target`, `description`, `footer`).

- Un campo de la fila prevalece sobre el atributo cuando está presente y no está vacío; si no, se usa el atributo. Así
  el XML puede contener las partes fijas (`icon`, `unit`, `caption`) y la consulta solo las cifras.
- El título es `title` o, si no hay, `label`.
- Los textos (`title`, `subtitle`, `chip`, `caption`, `description`, `footer`) pueden ser claves de locale o texto plano.

> **Nota:** El tono de la variación llega en el campo `tone`. El campo `style` sigue conteniendo las clases CSS de la
> card.

## Chip de estado {#status-chip}

El `chip` muestra un estado arriba a la derecha de la card, delante del icono (o de la variación), como una etiqueta con un punto: *Operativo*, *En riesgo*, *Caído*... Su
color es `chip-tone` (`neutral` si falta o no es válido). Hay un chip por card, y una card sin texto en `chip` no tiene
chip.

Como cualquier campo, la consulta puede devolver `chip` y `chipTone`, de modo que el estado cambia al refrescar la card.
Los campos `icon` e `iconTone` pueden cambiar con él:

```xml
<card id="CrdStatus" title="SCREEN_TEXT_SERVICE_STATUS" initial-load="query" target-action="ServiceStatus"
      autorefresh="60"/>
```

```java
DataListUtil.addColumn(dataList, "chip", List.of("SCREEN_TEXT_DEGRADED"));
DataListUtil.addColumn(dataList, "chipTone", List.of("warning"));
DataListUtil.addColumn(dataList, "icon", List.of("lucide:triangle-alert"));
DataListUtil.addColumn(dataList, "iconTone", List.of("warning"));
DataListUtil.addColumn(dataList, "value", List.of(97.4));
DataListUtil.addColumn(dataList, "unit", List.of("%"));
```

## Tendencia y tono {#trend-and-tone}

La etiqueta de la variación tiene una dirección (la tendencia) y un color (el tono). Se deciden en este orden:

1. **`tone`** prevalece tal cual (`success`, `warning`, `danger`, `info` o `neutral`).
2. Si no, el tono sale de la tendencia. Un **`trend` explícito** (`up`, `down` o `flat`) prevalece, incluso frente al
   signo de la variación.
3. Si no, la tendencia es el **signo de la variación**: negativa `down`, cero `flat`, positiva `up`.

Después `good-trend` convierte la tendencia en el tono:

| Tendencia            | `good-trend="up"` (por defecto) | `good-trend="down"` |
|----------------------|---------------------------------|---------------------|
| `up`                 | `success`                       | `danger`            |
| `down`               | `danger`                        | `success`           |
| `flat`               | `neutral`                       | `neutral`           |

- El signo de `value` nunca importa: un valor negativo con una variación positiva sigue subiendo.
- Sin una tendencia explícita la variación lleva signo (`+4,2%`, `−4,2%`, `0%`). Con una tendencia explícita la flecha
  ya indica la dirección, así que la variación se muestra sin signo (nunca `↗ −4,2%`).

## Formato de los números {#number-formatting}

- `value`, `delta` y `target` son números cuando son finitos (un número o un texto numérico). Se formatean en el idioma
  del usuario (`1,284.5` en inglés, `1.284,5` en español).
- `value` y `target` usan exactamente `number-decimals` decimales cuando se indica; si no, los decimales que tengan,
  hasta 2.
- La variación mantiene siempre hasta 2 decimales, diga lo que diga `number-decimals`.
- El progreso es `round(value / target * 100)` %. La barra se limita entre 0 y 100 %, el texto muestra el porcentaje
  real. La barra toma el color de `icon-tone` (el color primario por defecto).

## Sparkline y gauge {#sparkline-and-gauge}

Una card puede dibujar gráficos pequeños propios, sin un `chart` en su cuerpo:

- **Sparkline**: `sparkline` es una serie de números (un array JSON, desde el XML o desde un campo de la consulta) que
  se dibuja debajo del valor a todo el ancho de la card, como área (por defecto), línea o barras (`sparkline-type`).
  Toma el color de `icon-tone`. Una serie que no es una lista de números no dibuja nada.
- **Gauge**: `progress-style="gauge"` dibuja el progreso hacia el `target` como un semicírculo con el porcentaje en el
  centro, en lugar de la barra. Las reglas son las mismas que las de la barra (porcentaje, límites, color de
  `icon-tone`); como el porcentaje está en el arco, el texto de debajo solo indica el objetivo (`de 1.000 GB`).

```xml
<card id="CrdOrders" title="SCREEN_TEXT_ORDERS" value="1284" delta="4.2"
      sparkline="[980,1010,995,1060,1040,1105,1090,1150,1175,1160,1230,1284]"/>
<card id="CrdDisk" title="SCREEN_TEXT_DISK" icon="lucide:hard-drive" icon-tone="warning" value="720" unit="GB"
      target="1000" progress-style="gauge"/>
```

## Rejilla de cards {#card-grid}

Ponga las cards en un contenedor con la clase `awe-card-grid` para colocarlas en una rejilla adaptable: tantas columnas
de al menos `16rem` como quepan, con un espacio entre ellas, y todas las cards de una fila estiradas a la misma altura.
Es una clase sobre cualquier contenedor, no una etiqueta nueva:

```xml
<tag type="div" style="awe-card-grid">
  <card id="CrdCustomers" ... />
  <card id="CrdRevenue" ... />
  <card id="CrdErrors" ... />
</tag>
```

Una card con un gráfico o un grid en su cuerpo necesita más espacio: el estilo `awe-card-wide` hace que ocupe dos
columnas de la rejilla (`<card id="CrdSales" style="awe-card-wide">`). En pantallas estrechas la rejilla tiene una sola
columna y la card ancha ocupa la fila entera como las demás.

## Refresco {#refresh}

Una card se refresca como cualquier componente que carga datos:

- una acción `filter` cuyo destino es la card vuelve a ejecutar su consulta;
- una [dependencia](dependencies.md) con una acción `server` y la card como `target` la recarga cuando se cumplen sus
  condiciones (por ejemplo, cuando cambia un criterio);
- `autorefresh` la recarga cada cierto número de segundos, y la acción `reload` recarga toda la pantalla.

```xml
<button id="ButCrdReload" label="PARAMETER_RELOAD" icon="refresh">
  <button-action type="filter" target="CrdTstThemes"/>
</button>
```

## Accesibilidad {#accessibility}

- La card es una `section` etiquetada por su título.
- El tono no se indica solo con el color: la etiqueta de la variación muestra una flecha (`↗`, `↘` o `→`), el texto de
  la variación y una etiqueta oculta visualmente con la tendencia (*Sube*, *Baja*, *Sin cambios*), que leen los lectores
  de pantalla.
- El chip de estado indica el estado con su texto; el punto y el color solo lo repiten.
- La barra de progreso es un `progressbar` con su porcentaje.
- La card usa los colores del tema, en modo claro y oscuro.

## Ejemplos {#examples}

### Métrica rellena desde una consulta {#metric-filled-from-a-query}

El XML contiene el icono y el texto de la variación; la consulta devuelve el valor y la variación. La variación es
positiva y no hay tendencia explícita, así que sube y se muestra como éxito (`↗ +12,5%`):

```xml
<card id="CrdTstThemes" title="CARD_TST_THEMES" icon="lucide:palette" caption="CARD_TST_VS_LAST_MONTH"
      initial-load="query" target-action="CrdTstThemes"/>
```

```xml
<query id="CrdTstThemes">
  <table id="AweThm"/>
  <field id="IdeThm" function="CNT" alias="value"/>
  <constant value="12.5" alias="delta"/>
</query>
```

Un servicio Java también puede devolver la card completa, con los campos en camelCase:

```java
DataList dataList = new DataList();
DataListUtil.addColumn(dataList, "title", List.of("DASHBOARD_CARD_REVENUE"));
DataListUtil.addColumn(dataList, "value", List.of(412600));
DataListUtil.addColumn(dataList, "unit", List.of("€"));
DataListUtil.addColumn(dataList, "target", List.of(500000));
DataListUtil.addColumn(dataList, "delta", List.of(6.1));
DataListUtil.addColumn(dataList, "caption", List.of("DASHBOARD_CARD_VS_LAST_MONTH"));
return new ServiceData().setDataList(dataList);
```

### Card con un gráfico en el cuerpo {#card-with-a-chart-in-the-body}

La consulta de la card rellena la cabecera, el valor y el pie; el gráfico debajo del valor carga sus propios datos. No
hay icono, así que la variación va a la cabecera:

```xml
<card id="CrdDshDeliveries" initial-load="query" target-action="DshCardDeliveries" number-decimals="1">
  <chart id="ChrDshDeliveries" type="areaspline" initial-load="query" target-action="DshCardDeliveriesTrend"
         label="DASHBOARD_CARD_DELIVERIES_TREND">
    <chart-legend enabled="false"/>
    <x-axis type="category"/>
    <y-axis/>
    <chart-tooltip suffix=" %" number-decimals="1"/>
    <chart-serie id="serie1" x-value="week" y-value="onTime" label="DASHBOARD_CARD_DELIVERIES_TREND"/>
  </chart>
</card>
```

### Card cuya buena tendencia es bajar {#card-whose-good-trend-is-down}

En una tasa de errores, bajar es una buena noticia. Con `good-trend="down"` la variación negativa se muestra como éxito
(`↘ −0,18 pt`):

```xml
<card id="CrdTstErrors" title="CARD_TST_ERRORS" value="0.42" unit="%" delta="-0.18" delta-unit="pt"
      good-trend="down" footer="CARD_TST_STATIC"/>
```
