---
id: print-engine
title: Motor de impresión
sidebar_label: Motor de impresión
---

AWE incluye un motor de impresión genérico que permite generar el contenido de una pantalla en formato PDF, Excel y DOC.

## Imprimir una pantalla {#printing-a-screen}

Para habilitar la impresión en una pantalla, incluye el diálogo de impresión genérico de AWE:

```xml
<include target-screen="PrnOpt" target-source="center" />
```

A continuación, añade un botón que abra el diálogo incluido:

<img alt="Botón de impresión" src={require('@docusaurus/useBaseUrl').default('img/Boton.png')} />

Una vez completados estos dos pasos, el usuario puede abrir el diálogo de impresión y elegir el formato de salida y las opciones:

<img alt="Diálogo de impresión" src={require('@docusaurus/useBaseUrl').default('img/DialogImagePrint.png')} />

## Configurar el título {#configure-title}

Por defecto, el título de un informe impreso se genera con el patrón:

```text
report title : report subtitle
```

Los valores usados para el título y el subtítulo se toman de las etiquetas definidas en la pantalla, como la etiqueta de la pantalla, del tabcontainer, de la ventana o del *grid*.

El siguiente ejemplo muestra una pantalla antes de imprimir:

<img alt="Pantalla de ejemplo antes de imprimir" src={require('@docusaurus/useBaseUrl').default('img/print-01-screen-default.png')} />

Si no se aplica ninguna configuración adicional, AWE usa las etiquetas existentes de la pantalla para construir el título impreso.

El resultado impreso de la pantalla anterior es el siguiente:

<img alt="Salida impresa con el título por defecto" src={require('@docusaurus/useBaseUrl').default('img/print-02-output-default-title.png')} />

Puedes personalizar este comportamiento de las siguientes maneras:

- **Comportamiento por defecto**: si no añades ninguna configuración adicional, AWE usa las etiquetas ya definidas en la pantalla.
- **Subtítulo personalizado**: define el atributo `label` en el `tabcontainer` correspondiente y usa una entrada de locale si es necesario.
- **Eliminar el título principal**: establece la `label` de la pantalla a un valor vacío si quieres que el informe impreso muestre solo el subtítulo.

Por ejemplo, tras definir un subtítulo personalizado en la etiqueta del tabcontainer, la configuración de la pantalla queda así:

<img alt="Pantalla de ejemplo con subtítulo personalizado" src={require('@docusaurus/useBaseUrl').default('img/print-03-screen-with-subtitle.png')} />

En resumen, el título impreso se controla con las mismas etiquetas que se usan en la estructura de la pantalla, por lo que la personalización del título se realiza mediante la configuración normal de la pantalla.

## Configurar los datos de impresión {#configure-printing-data}

Desde AWE 3.2, el motor de impresión utiliza los datos que están disponibles actualmente en el cliente y los envía al motor de impresión.

Esto es importante cuando un grid usa paginación en el servidor (`load-all="false"`). En ese caso, el cliente solo tiene las filas cargadas actualmente en la página, por lo que el documento impreso solo incluirá esos datos.

Si necesitas imprimir el conjunto de resultados completo, la opción más sencilla es usar paginación local:

```xml
load-all="true"
```

Esto hace que todas las filas estén disponibles en el cliente, de modo que el motor de impresión pueda incluir el conjunto de datos completo.

:::warning
Este enfoque puede causar graves problemas de rendimiento si la consulta devuelve un gran número de filas.
:::

Si necesitas mantener la paginación en el servidor y aun así imprimir el conjunto de datos completo, debes implementar un flujo de impresión específico del proyecto que recargue los datos necesarios antes de generar el documento. Ese escenario depende de la aplicación y queda fuera del alcance de esta guía genérica.

## Elegir qué columnas del grid se imprimen {#choose-which-grid-columns-are-printed}

Las columnas del grid se imprimen según su atributo `printable`:

- **Sin definir**: se imprime en todos los formatos, pero solo mientras la columna esté visible en pantalla.
- **`true`**: se imprime en todos los formatos, incluso cuando la columna es `hidden="true"`.
- **`excel`**: se imprime solo en las salidas de hoja de cálculo (XLSX y CSV), incluso cuando está oculta.
- **`false`**: no se imprime nunca.

Esta es la forma recomendada de imprimir un valor sin formato cuando la columna mostrada en pantalla lleva marcado de estilos:

```xml
<column label="AMOUNT" name="Amount" charlength="20" printable="false"/>
<column label="AMOUNT" name="AmountRaw" charlength="20" hidden="true" type="float" printable="true"/>
```

Consulta [columnas de impresión](../api/grids.md#printing-columns) para ver las reglas completas.

## Qué imprime un grid {#what-a-printed-grid-shows}

Un grid impreso reproduce lo que el grid muestra en pantalla. Los atributos de columna que lo deciden se leen en el
servidor de la definición de la pantalla, de modo que el informe sigue el XML de la pantalla en los dos motores de
cliente. El PDF (y los demás formatos de documento: DOCX, RTF, HTML, TEXT) los dibuja; XLSX, XLS, ODS y CSV los escriben
como texto.

| Característica (atributo) | PDF y documentos | XLSX y CSV |
|---------------------------|------------------|------------|
| Segunda línea (`secondary-field`) | Bajo el valor, más pequeña y atenuada, en la misma celda, cuando la columna es texto (sin `type` o con `type="string"`). En una columna numérica, de fecha o de chips va a su propia columna, tras la principal | Una columna tras la principal solo con `export-secondary="true"` (titulada con `secondary-label`) |
| Estado (`status-field`) | Un chip con el texto de la celda, coloreado con el tono de la fila (`success`, `warning`, `danger`, `info`; cualquier otro tono es `neutral`) | El texto de la celda |
| Chips (`chips="true"`, `chip-separator`) | Un chip por elemento (todos: `max-chips` solo limita la pantalla), `neutral`, o con el tono de `status-field` cuando la columna también lo tiene | Los elementos unidos con una coma y un espacio |
| Progreso (`component="progress"`, `bar-min`, `bar-max`) | Una barra rellena con el valor dentro de su escala (de 0 a 100 por defecto) y el texto de la celda. El segundo valor y los segmentos apilados de las opciones de barra no se dibujan | El texto de la celda |
| Sparkline (`component="sparkline"`) | La serie como línea, barras o área (`sparkline-type`), con `sparkline-color` y `sparkline-negative-color`. El último punto y las marcas de mínimo y máximo no se dibujan | Los números de la serie |
| Texto con formato (`component="formatted-text"`) | Texto plano: sin etiquetas, con las entidades decodificadas y los saltos de línea como espacios. Nunca marcado | Texto plano |
| Agrupación | Filas de grupo, subtotales y total, véase abajo | Las mismas filas, con estilo, **sin niveles de esquema** |
| Detalle de fila (`<row-detail print="true">`) | Bajo cada fila, véase abajo | Una celda a lo ancho del grid bajo cada fila |

Notas:

- Los colores de un sparkline son hexadecimales `#RRGGBB` (o `#RGB`) o el nombre de una paleta del sistema de diseño
  (`green`, `red`, `sky`...), que imprime su tono `500`. Cualquier otro valor (por ejemplo una variable CSS) se ignora y
  ADE usa su color por defecto; un color incorrecto nunca hace fallar la impresión. Los colores de estado y de grupo son
  valores de impresión fijos tomados de los tokens de diseño de AWE 5, porque un informe no tiene variables CSS.
- El campo secundario y el campo de estado viajan con los datos de impresión: los dos motores los envían aunque no sean
  columnas del grid, siempre que una columna impresa los use.
- Un grid sin estos atributos se imprime exactamente igual que antes.

### Grids agrupados {#grouped-grids}

El grid se imprime con la agrupación que el usuario había elegido al imprimir: los dos motores envían las columnas que
agrupan las filas, también ninguna cuando el usuario eligió *Sin agrupar*, y el servidor usa el `group-by` del grid solo
cuando el cliente no envía nada. Los grids que no pueden agrupar en pantalla (de árbol, editables, `send-operations`,
`move-rows` o `row-detail`) tampoco agrupan en el informe.

- Cada grupo tiene una **fila de grupo**, `GROUP1`, `GROUP2`... según el nivel (en negrita, sobre los colores de superficie
  del sistema de diseño, más claros en cada nivel), con el valor del grupo y su número de filas, y los agregados (`sum`,
  `avg`, `max`, `min`) de las columnas con `summary-type`. La etiqueta va en la primera columna impresa que es texto
  plano (ninguna si no hay columnas de texto, para que nunca caiga en una celda de número o fecha); un valor vacío se
  etiqueta *(vacío)*.
- A continuación van sus filas; cuando `group-subtotals` no es `false`, una **fila de subtotal** cierra cada grupo de todos
  los niveles.
- El total general va al final cuando `show-totals` está activo: el pie que envía la pantalla.
- Todos los grupos se imprimen expandidos. Los grupos van por su valor, ascendente: primero los números, luego los
  textos y el vacío al final. Las filas conservan el orden que envía el cliente. La ordenación que el usuario eligió en
  pantalla no se aplica al orden de los grupos.
- Una columna que agrupa no tiene que imprimirse. Una columna con `summary-type` que es la columna de la etiqueta muestra
  la etiqueta.
- Las filas de grupo, los subtotales y el total llevan el estilo de su tipo de fila, sin ninguna columna adicional.

### Detalle de fila {#row-detail}

Un grid cuyo `<row-detail>` declara `print="true"` imprime su detalle bajo cada fila; sin él, nada cambia. Una plantilla de
texto la renderiza el servidor para cada fila con las reglas de la pantalla; un grid anidado ejecuta su consulta una vez
por fila impresa (limitada por su `max`, 100 si no declara ninguno) e imprime sus columnas como tabla. Véase
[imprimir el detalle](../api/grids.md#printing-the-detail) para lo que necesita y los límites. Un detalle hecho con un
`include` no se imprime. En XLSX el detalle es una celda combinada a lo ancho del grid, y una tabla anidada no se alinea
con las columnas del grid: la hoja tiene columnas extra y la cabecera anidada se repite bajo cada fila.
