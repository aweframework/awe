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

Los valores usados para el título y el subtítulo se toman de las etiquetas definidas en la pantalla, como la etiqueta de la pantalla, del tabcontainer, de la ventana o de la tabla.

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

Esto es importante cuando una tabla usa paginación en el servidor (`load-all="false"`). En ese caso, el cliente solo tiene las filas cargadas actualmente en la página, por lo que el documento impreso solo incluirá esos datos.

Si necesitas imprimir el conjunto de resultados completo, la opción más sencilla es usar paginación local:

```xml
load-all="true"
```

Esto hace que todas las filas estén disponibles en el cliente, de modo que el motor de impresión pueda incluir el conjunto de datos completo.

:::warning
Este enfoque puede causar graves problemas de rendimiento si la consulta devuelve un gran número de filas.
:::

Si necesitas mantener la paginación en el servidor y aun así imprimir el conjunto de datos completo, debes implementar un flujo de impresión específico del proyecto que recargue los datos necesarios antes de generar el documento. Ese escenario depende de la aplicación y queda fuera del alcance de esta guía genérica.

## Elegir qué columnas de la tabla se imprimen {#choose-which-grid-columns-are-printed}

Las columnas de la tabla se imprimen según su atributo `printable`:

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
