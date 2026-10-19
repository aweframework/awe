---
id: grids
title: Grids
---

Los *grids* son el componente principal de AWE para mostrar las listas de datos. Se definen con *grid* y *columns*:

* El **grid** es el contenedor de la lista de datos. Tiene la consulta a cargar y un conjunto de atributos para definir el comportamiento del grid.
* Cada **columna** contiene la fuente de los datos de la lista de datos, la etiqueta de la columna y otros atributos para administrar cada celda de columna.

También hay una etiqueta **group-header** que se utiliza para mostrar una cabecera de segundo nivel sobre un grupo de encabezados de columnas.

<img alt="Grid" src={require('@docusaurus/useBaseUrl').default('img/Grid.png')} />

## Esqueleto de XML {#xml-skeleton}

Para definir un **grid** o un **treegrid** en AWE debe seguir la siguiente estructura:

```xml
<grid id="[grid-identifier]" ...>
  <column ... ></column>
  ...
  <group-header ...>
    <column ...></column>
    ...
  </group-header>
  ...
  <button ... > ... </button>
  ...
  <context-button ...> ... </context button>
  ...
</grid>
```
## Estructura de grid {#grid-structure}

```xml
<grid id="[grid-id]" style="[grid-style]" 
      multiselect="[grid-is-multiselect]" checkbox-multiselect="[checkbox-multiselectable]" editable="[grid-is-editable]" send-operations="[grid-is-multioperation]" initial-load="[initial-load]" server-action="[server-action]" target-action="[target-action]" max="[elements-per-page] pagination-disabled="[pagination-disabled]"">
...
</grid>
```

### Atributos generales {#general-attributes}

| Atributo               | Uso             | Tipo    | Descripción                                                                                                                                     | Valores                                                                                                                                                    |
| ---------------------- | --------------- | ------- | ----------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| id                     | **Obligatorio** | String  | Grid identifier. Con fines de referencia                                                                                                        |                                                                                                                                                            |
| label                  | Opcional        | String  | Título de grid (sólo para imprimir)                                                                                                       | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                                                                                 |
| style                  | Opcional        | String  | Clases CSS de Grid                                                                                                                              |                                                                                                                                                            |
| initial-load          | Opcional        | String  | Llamada de acción del servidor para cargar los datos del grid. Sólo soporta el valor de `query`                                      |                                                                                                                                                            |
| server-action          | Opcional        | String  | Llamada de acción del servidor                                                                                                                  | Ver [lista de acciones del servidor](actions.md#server-actions)                                                                                            |
| target-action          | Opcional        | String  | Destino para llamar al servidor                                                                                                                 |                                                                                                                                                            |
| max                    | Opcional        | Entero  | Número máximo de elementos a recuperar **por página**                                                                                           |                                                                                                                                                            |
| pagination-disabled | Opcional        | Boolean | Deshabilitado el elemento de paginación del grid                                                                                        | **Nota:** Valor predeterminado `false`                                                                                                                     |
| pager-values      | Opcional        | String  | Activar paginador de grid. Establecer la lista de valores con el número de filas por página                                               | **Ejemplo:** `pager-value="5,25,50,100"`                                                                                                                   |
| load-all            | Opcional        | Boolean | Cargar todos los valores del grid y ordenar y paginar localmente                                                                        | **Nota:** El valor predeterminado es `false`                                                                                                               |
| send-all            | Opcional        | Boolean | Enviar todos los valores del grid al servidor en lugar de a los seleccionados. (Las seleccionadas se enviarán el parámetro `.selected`) | **Nota:** El valor predeterminado es `false`                                                                                                               |
| help                  | Opcional        | String  | Texto de ayuda para el grid                                                                                                               | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                                                                                 |
| help-image        | Opcional        | String  | Imagen de ayuda para el grid                                                                                                              | Esta **debe** ser una ruta de imagen                                                                                                                       |
| icon-loading        | Opcional        | String  | Establecer el icono de carga                                                                                                                    | `skeleton`, `skeleton-dim`, `spinner`, `square`, `circles`, `carpet`, `dots`, `folding`, `squarebar`, `circlebar`, `cubes`, `icon`, `custom`, `none`. Por defecto: la propiedad [`awe.application.component.grid-icon-loading`](../properties#awe.application.component.grid-icon-loading) (`skeleton`). `skeleton` muestra filas de relleno con las columnas del grid en la primera carga y en cada recarga; indica otro valor para desactivarlo en este grid. `skeleton-dim` las muestra en la primera carga, y una recarga con filas visibles las mantiene, atenuadas y no clicables (`aria-busy="true"`), en lugar de sustituirlas; con cualquier otro icono el grid conserva su icono en la primera carga y en las recargas |
| row-numbers        | Opcional        | Boolean | Indica si se muestran los números de fila                                                                                                              | El valor predeterminado es `true`                                                                                                                     |
| row-height             | Opcional        | Entero  | Establece la altura de la fila en píxeles (mínima si hay columnas con varias líneas)                                                                                                                    |                                                                                                                                                            |
| validate-on-save       | Opcional        | Boolean | Establezca `false` para evitar validar la fila seleccionada al pulsar el botón `save`                                                                  | El valor predeterminado es `true`                                                                                                                     |
| empty-icon             | Opcional        | String  | Icono del [estado vacío](#empty-state) del grid                                                                                                        | El valor predeterminado es `inbox`. Consulte [iconos](icons.md)                                                                                       |
| empty-title            | Opcional        | String  | Título del [estado vacío](#empty-state) del grid                                                                                                       | **Nota:** Puede usar archivos [i18n](i18n-internationalization.md) (locales). Por defecto: un título traducido "No se han encontrado registros"      |
| empty-text             | Opcional        | String  | Texto del [estado vacío](#empty-state) del grid                                                                                                        | **Nota:** Puede usar archivos [i18n](i18n-internationalization.md) (locales). Por defecto: un texto explicativo traducido                             |
| empty-action           | Opcional        | String  | Identificador de un botón del grid que se ofrece en el [estado vacío](#empty-state)                                                                    | El estado vacío muestra un botón con la etiqueta y el icono de ese botón, que ejecuta sus acciones                                                    |
| show-toolbar            | Opcional        | Boolean | Muestra la [barra de herramientas](#toolbar) del grid, con el diálogo de columnas y el botón de maximizar                                              | Por defecto `true`. Use `false` para desactivarla                                                                                                      |

### Barra de herramientas {#toolbar}

Todos los grids muestran una barra de herramientas, tanto en el motor AngularJS como en el de React, con dos botones:

- **Columnas** abre un diálogo con la lista de columnas del grid. El usuario muestra u oculta cada columna, la sube o la
  baja (con las flechas o arrastrando su asa) y ve cuántas columnas están visibles. *Mostrar todas* muestra todas las
  columnas, *Restablecer* vuelve a la definición del XML de la pantalla y *Guardar como predeterminado* guarda la
  disposición para el usuario.
- **Maximizar** hace que el grid ocupe el área de contenido de la pantalla, como el botón de maximizar de una ventana o una pestaña: el grid crece con una animación, la cabecera y el menú de la aplicación siguen visibles y el resto de la pantalla se desvanece hasta que se restaura el grid.

Los cambios del diálogo se aplican al grid de inmediato, pero solo se conservan cuando el usuario elige *Guardar como
predeterminado*. La disposición guardada pertenece al usuario, así que le acompaña en otros navegadores y dispositivos, y
se almacena en la tabla `AweUsrGrdCnf` (una fila por usuario, pantalla y grid; la crea una migración de base de datos).
*Restablecer* la elimina. El usuario siempre se toma de la sesión. La disposición se aplica al generar la pantalla: el
grid se envía con su `columnModel` original más un `columnLayout` (el orden de las columnas y las ocultas), de modo que
el primer pintado ya está personalizado.

Algunas reglas mantienen válida una disposición guardada cuando cambia la pantalla:

- Las columnas ocultas en el XML (`hidden="true"`, por ejemplo las claves) nunca aparecen en el diálogo.
- Los identificadores de columna que ya no existen se ignoran, y una columna añadida al XML aparece visible en su
  posición del XML.
- Una columna solo se puede reordenar dentro de su propio `group-header`, y las columnas sin grupo entre sí; una columna
  nunca sale de su grupo, aunque la disposición guardada lo indique. Los grupos no se reordenan como bloques.
- Las cabeceras de grupo se calculan a partir de las columnas visibles de cada grupo, de modo que ocultar cualquier
  columna de un grupo (también la primera) mantiene las cabeceras alineadas, y un grupo sin columnas visibles no se
  muestra.

Use `show-toolbar="false"` para que un grid no tenga barra de herramientas; entonces siempre usa la definición del XML.

```xml
<grid id="UserGrid" show-toolbar="false" server-action="data" target-action="UserList">
  <column label="USER" name="user" />
</grid>
```

Las exportaciones a Excel y PDF y la impresión usan las columnas del XML de la pantalla, no la disposición del usuario.

### Estado vacío {#empty-state}

Un grid sin filas muestra un estado vacío en lugar de una tabla vacía, tanto en el motor AngularJS como en el de React. Se
compone de un icono, un título y un texto. Los textos por defecto están traducidos; utilice `empty-icon`, `empty-title` y
`empty-text` para cambiarlos. Mientras el grid se está cargando, no se muestra el estado vacío.

El atributo opcional `empty-action` apunta a un botón del grid (un `button` declarado dentro del grid). El estado vacío
ofrece un botón con la etiqueta y el icono del botón referenciado, y al hacer clic en él se ejecutan las acciones de ese
botón, por lo que puede usarse, por ejemplo, para limpiar los criterios de búsqueda.

```xml
<grid id="UserGrid" server-action="data" target-action="UserList"
      empty-icon="search" empty-title="GRID_NO_USERS" empty-text="GRID_NO_USERS_HELP" empty-action="ClearSearch">
  <column label="USER" name="user" />
  <button id="ClearSearch" label="BUTTON_CLEAR" icon="eraser" button-type="reset" />
</grid>
```


## Estructura de columna {#column-structure}

```xml
<column label="[column-label]" name="[column-name]" sort-field="[sort-field]" align="[text-align]" 
        charlength="[column-width-chars]" component="[column-component]" >
  <dependency ...> ... </dependency>
  ...
</column>
```

### Atributos de columna {#column-attributes}

| Atributo   | Uso             | Tipo    | Descripción                                                                                                                                            | Valores                                                                          |
| ---------- | --------------- | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------- |
| name       | **Obligatorio** | String  | Identificador de columna. Con fines de referencia                                                                                                      |                                                                                  |
| sort-field | Opcional        | String  | Ordenar por campo (si no treegrid)                                                                                                                     |                                                                                  |
| type       | Opcional        | String  | Tipo de campo (para fines de impresión)                                                                                                                | `string`, `integer`, `float` o `date`                                            |
| hidden     | Opcional        | Boolean | La columna **no** es visible en pantalla. Aun así puede imprimirse, véase [columnas de impresión](#printing-columns)                                                | El valor predeterminado es `false`                                               |
| printable | Opcional        | String  | Indica si la columna se incluye en el informe impreso. Véase [columnas de impresión](#printing-columns)                                                        | `true` (por defecto) o `false`                                                      |
| align      | Opcional        | String  | La columna **no es** visible                                                                                                                           | `left`, `center` o `right`                                                |
| width      | Opcional        | Entero  | Ancho de la columna en píxeles o en porcentaje.                                                                                                                     | Ej.: `width = "10"` o `width = "20%"` **Nota:** Puede usar el valor '*' para ajuste automático. |
| sortable   | Opcional        | Boolean | El campo es ordenable (si no es treegrid)                                                                                                              | El valor predeterminado es `true`                                           |
| movable    | Opcional        | Boolean | Permite mover la posición de la columna en el grid                                                                                               | El valor predeterminado es `true`                                           |
| sendable   | Opcional        | Boolean | Los datos de columna deben ser enviados al servidor                                                                                                    | El valor predeterminado es `true`                                           |
| charlength | Opcional        | Entero  | Ancho de columna en caracteres                                                                                                                         |                                                                                  |
| label      | Opcional        | String  | Etiqueta de columna                                                                                                                                    | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)       |
| style      | Opcional        | String  | Columna clase css                                                                                                                                      | Clase bruta a aplicar a la columna                                               |
| component  | Opcional        | String  | Tipo de columna (si editable)                                                                                                                          | Ver [componentes](criteria.md#components)                                        |
| max        | Opcional        | Entero  | Número máximo de registros a recuperar cuando la columna se inicializa con acción de destino                                                           | El valor predeterminado es `30`                                                  |
| visibility | Opcional        | Boolean | Visibilidad inicial del componente de columna                                                                                                          | El valor predeterminado es `true`                                           |
| frozen     | Opcional        | Boolean | Mantenga la columna fijada fuera del desplazamiento horizontal. **ADVERTENCIA**: No uses este atributo si el encabezado tiene más de una línea de alto | El valor predeterminado es `false`                                               |
| wrap       | Opcional        | Booleano | El texto de las celdas se divide en varias líneas y la fila crece para ajustarse. Ver [texto en varias líneas](#wrapping-text) | El valor por defecto es `false`                                                  |
| max-lines  | Opcional        | Entero  | Número máximo de líneas de una celda con texto en varias líneas; implica `wrap`. Ver [texto en varias líneas](#wrapping-text)   | Un número mayor que `0`                                                          |

> **Nota:** Cuando una columna es editable (tiene un *componente*) todos los atributos de criterios pueden ser usados en la columna. Ver **[criterios atributos](criteria.md#criteria-structure)** para más referencias.

### Texto en varias líneas {#wrapping-text}

Por defecto el texto de una celda ocupa una sola línea y se corta con puntos suspensivos cuando no cabe en el ancho de la
columna. Una columna puede declarar que su texto se divide en varias líneas, y la fila crece para ajustarse a la celda más
alta. Funciona en **ambos motores de cliente** y un grid sin estos atributos se muestra exactamente igual que antes.

```xml
<grid id="GrdComments" style="expand" initial-load="query" server-action="data" target-action="CommentList">
  <column label="Code" name="code" align="left" charlength="8"/>
  <column label="Title" name="title" align="left" charlength="16" wrap="true"/>
  <column label="Description" name="description" align="left" charlength="30" max-lines="3"/>
</grid>
```

- `wrap="true"` muestra el texto completo en tantas líneas como necesite.
- `max-lines="N"` limita el texto a `N` líneas. Si el texto es más largo, la línea `N` termina en puntos suspensivos y el
  texto completo se muestra en un tooltip al pasar el ratón y al recibir el foco del teclado (se puede llegar a las celdas
  cortadas con la tecla `Tab` y `Esc` cierra el tooltip). `max-lines` implica `wrap`, salvo que la columna declare
  `wrap="false"`.
- El resto de columnas mantienen su única línea. La selección, la ordenación, la edición en línea y los atajos de teclado del
  grid funcionan como siempre con filas más altas.
- En el motor AngularJS `row-height` es la altura **mínima** de las filas de un grid con columnas de varias líneas. El motor
  React ignora `row-height`.

Un texto que no se divide y no cabe en el ancho de su columna siempre expone su valor completo: en el motor React, las
celdas de texto y las cabeceras lo muestran como tooltip (el motor AngularJS ya lo hacía en las celdas).

:::note Límites
- Solo se divide el texto de las celdas estándar. Las celdas de columnas con `component` mantienen su propia disposición.
- El motor AngularJS coloca las filas de un grid con una altura fija, así que un grid con columnas de varias líneas dibuja
  **todas las filas de su página** en lugar de virtualizarlas (el tamaño de página del grid, ver `max`, acota el coste). Un
  grid con columnas congeladas (`frozen`) o un grid de árbol mantiene sus filas de una línea en ese motor.
:::

Las aplicaciones de prueba incluyen las pantallas *Wrapping cells* y *Single-line cells* en el menú de pruebas de la matriz,
con las mismas 100 filas con y sin varias líneas.

### Columnas de impresión {#printing-columns}

Cuando se imprime una pantalla (véase la [guía del motor de impresión](../guides/print-guide.md)), cada grid decide qué columnas van al informe mediante el atributo `printable` de la columna:

| `printable` | Impreso en PDF, DOCX y TEXT         | Impreso en XLSX y CSV               |
| ----------- | ----------------------------------- | ----------------------------------- |
| sin definir | Solo cuando la columna está en pantalla | Solo cuando la columna está en pantalla |
| `true`      | Siempre, incluso si `hidden="true"` | Siempre, incluso si `hidden="true"` |
| `excel`     | Nunca                               | Siempre, incluso si `hidden="true"` |
| `false`     | Nunca                               | Nunca                               |

«En pantalla» significa que la columna no es `hidden="true"` y que no se ha ocultado en tiempo de ejecución mediante una acción de dependencia `hide-column`. Declarar `printable="true"`, `printable="excel"` o `printable="false"` tiene prioridad sobre el estado de la pantalla.

Esto permite mostrar un valor con estilo en pantalla y exportar el valor sin formato:

```xml
<grid id="GrdAmounts" server-action="data" target-action="QryAmounts" load-all="true">
  <!-- Shown on screen with HTML styling, excluded from the report -->
  <column label="AMOUNT" name="Amount" align="right" charlength="20" printable="false"/>
  <!-- Hidden on screen, printed as a numeric value in every format -->
  <column label="AMOUNT" name="AmountRaw" align="right" charlength="20" hidden="true" type="float" printable="true"/>
  <!-- Hidden on screen, exported to the spreadsheet only -->
  <column label="INTERNAL_CODE" name="Code" charlength="10" hidden="true" printable="excel"/>
</grid>
```

Tenga en cuenta lo siguiente:

- Una columna impresa necesita sus datos en el cliente, por lo que debe ser `sendable` (el valor por defecto). Una columna `sendable="false"` aparece en la cabecera del informe con celdas vacías.
- Use `type` (`string`, `integer`, `float`, `date`) en las columnas impresas para que las celdas de la hoja de cálculo tengan el formato correcto.
- Las columnas dentro de un `group-header` mantienen su agrupación en el informe. Las columnas del grupo que no se imprimen en un formato se omiten, y la cabecera del grupo desaparece cuando ninguna de sus columnas se imprime.
- Cuando una petición de impresión mezcla formatos de hoja de cálculo y de documento y alguna columna es `printable="excel"`, AWE diseña el informe dos veces, una por cada tipo de salida. Los grids que se imprimen desde una consulta ejecutan esa consulta una vez por diseño.
- Los valores `all` y `tab`, admitidos por el esquema en versiones anteriores, se han eliminado. Sustituya `all` por `true`.

### Componentes de columna {#column-components}

| Componente           | Descripción                                                                                                           |
| -------------------- | --------------------------------------------------------------------------------------------------------------------- |
| text                | Componente de columna de texto. Ver el [criterio de texto](criteria.md#text-criterion)                               |
| password           | Componente de columna de contraseña. Ver el criterio de [contraseña](criteria.md#password-criterion)                  |
| textarea             | Textarea column component. See [textarea criterion](criteria.md#textarea-criterion)                                   |
| numeric              | Componente numérico de columna. Ver el [criterio numérico](criteria.md#numeric-criterion)                            |
| date                | Componente de columna de fecha. Ver el [criterio de fecha](criteria.md#date-criterion)                               |
| time               | Componente de columna de tiempo. Ver el [criterio de tiempo](criteria.md#time-criterion)                             |
| filtered-date       | Componente de columna de fecha filtrado. Ver el criterio de fecha [filtrado](criteria.md#filtered-date-criterion)     |
| select          | Seleccionar componente de columna. Ver [seleccionar el criterio](criteria.md#select-criterion)                        |
| suggest              | Sugerir componente de columna. Ver [criterio de sugerencia](criteria.md#suggest-criterion)                            |
| select-multiple | Seleccione un componente de columna múltiple. See [select multiple criterion](criteria.md#multiple-select-criterion)  |
| suggest-multiple     | Sugerir el componente de múltiples columnas. See [suggest multiple criterion](criteria.md#multiple-suggest-criterion) |
| checkbox              | Componente de columna Checkbox. See [checkbox criterion](criteria.md#checkbox-criterion)                              |
| radio                 | Componente de columna Radio: las opciones de una lista como una fila de botones de opción. Ver [componente de columna radio](#radio-column-component) |
| button-radio          | Componente de columna Radio: las opciones de una lista como una barra de botones unidos. Ver [componente de columna radio](#radio-column-component) |
| color                | Componente de columna de color. Ver el [criterio de color](criteria.md#color-criterion)                              |
| uploader             | Componente de columna del cargador. Ver el [criterio de carga de archivos](criteria.md#uploader-criterion)                   |
| text-view       | Componente de columna de vista de texto. Ver el [criterio de vista de texto](criteria.md#text-view-criterion)        |
| formatted-text     | Componente de columna de texto HTML. Ver [componente de texto formateado](#formatted-text-column-component)           |
| icon                 | Componente de columna de icono. Úselo para mostrar iconos impresionantes de fuentes en una columna.                   |
| image               | Componente de columna de imagen. Utilícelo para mostrar imágenes en una columna.                                      |
| button               | Componente que permite al usuario hacer clic en un botón que lanza un conjunto de `button-action`                      |
| progress             | Componente de columna de progreso. Útil para mostrar una barra de progreso en una columna. Ver [progreso](#progress-column-component) |
| sparkline            | Componente de columna sparkline: un pequeño gráfico de línea, barras o área de una serie de números. Ver [componente de columna sparkline](#sparkline-column-component) |
| link                 | Componente de enlace HTML. Abre una URL en una página nueva                                                                       |


### Estilo específico de fila {#specific-row-style}

Cuando necesite establecer un estilo específico para todas las celdas de una fila (p. ej.: cambiar el color de fondo), puede añadir un nuevo campo llamado `_style_` con el nombre del estilo CSS.

```xml
<query id="MyQuery" >
    <field id="_style_" alias="_style_" variable="styleClass"/>
    <variable id="styleClass" type="STRING" value="red"/>
</query>
```

### Clases de tema {#theme-classes}

AWE proporciona clases CSS listas para usar para dos aspectos habituales de un grid. Están disponibles en **ambos motores
de cliente y en todos los temas**, claros u oscuros, por lo que una aplicación no necesita escribir su propio CSS.

#### Grid sin fila de cabecera {#grid-without-header-row}

Añada la clase `grid-no-header` al `style` del grid. La fila de cabecera se oculta pero sigue participando en la
disposición de las columnas, de modo que estas mantienen sus anchos (`charlength` y `width`). Es útil para listas compactas.

```xml
<grid id="GrdCompactList" style="expand grid-no-header" initial-load="query" server-action="data" target-action="CompactList">
  <column label="Name" name="name" align="left" charlength="30"/>
  <column label="Description" name="description" align="left" charlength="40"/>
</grid>
```

:::note
La cabecera no está disponible para los lectores de pantalla mientras está oculta, ni tampoco los controles de
ordenación, filtro y menú de columna de la cabecera. Utilice la clase en grids que no los necesiten.
:::

#### Celdas coloreadas por escala (mapas de calor) {#colour-scaled-cells-heat-maps}

La consulta asigna una de las siguientes clases a cada celda con el atributo `style` de una
[celda compuesta](#standard-cell-without-components). Cada clase pinta toda la celda y establece su propio color de texto,
de modo que el texto mantiene un contraste de al menos 4,5:1 (WCAG AA) en todos los temas.

| Escala                        | Clases                                              | Úsela para                                  |
|-------------------------------|-----------------------------------------------------|---------------------------------------------|
| Secuencial (azul)             | `scale-seq-1` (la más baja) a `scale-seq-5` (la más alta) | Valores que van de bajos a altos      |
| Divergente, positiva (verde)  | `scale-pos-1` (la más baja) a `scale-pos-5` (la más alta) | Valores por encima de la referencia (p. ej. ganancias) |
| Divergente, negativa (roja)   | `scale-neg-1` (la más baja) a `scale-neg-5` (la más alta) | Valores por debajo de la referencia (p. ej. pérdidas)  |

Por ejemplo, una consulta que colorea el importe en la escala positiva o negativa según su valor:

```xml
<query id="Results">
  <table id="Results"/>
  <field id="Name" alias="name"/>
  <field id="Amount" alias="amount" noprint="true"/>
  <compound alias="result">
    <computed alias="value" format="[amount]"/>
    <computed alias="label" format="[amount]"/>
    <computed alias="style" format="[amount] &lt; -50 ? 'scale-neg-5' : [amount] &lt; 0 ? 'scale-neg-2' : [amount] &lt; 50 ? 'scale-pos-2' : 'scale-pos-5'" eval="true"/>
  </compound>
</query>
```

Las aplicaciones de prueba incluyen las pantallas *Grid without header* y *Colour scales* en el menú de la matriz de
pruebas, con un ejemplo de cada clase.

### Componentes de columna específicos {#specific-column-components}

Existen varios componentes desarrollados específicamente para ser utilizados en células de la red. Estos componentes necesitan una estructura especial en la consulta que los llene:

```xml
<compound alias="specialCell">
  <computed alias="value" format="xxx"/>  
  <computed alias="label" format="xxx" translate="EnumTranslate"/>
</compound>
```

Estas son las células que utilizan la estructura especial:

#### Celda estándar sin componentes {#standard-cell-without-components}

Cuando se envía una **estructura compuesta** a una celda estándar, se pueden utilizar los siguientes atributos:

| Atributo | Descripción                                                                                                               |
| -------- | ------------------------------------------------------------------------------------------------------------------------- |
| value    | Valor que se enviará al servidor                                                                                          |
| label    | Texto que se mostrará. Puedes usar [i18n](i18n-internationalization.md) archivos (locales)                                |
| title    | Texto a mostrar cuando mueve el ratón sobre el icono. Puedes usar [i18n](i18n-internationalization.md) archivos (locales) |
| style    | Clase CSS para formatear el contenido                                                                                     |

#### Componente de columna de vista de texto {#text-view-column-component}

Este componente es muy útil para mostrar un texto con estilo dentro de un grid con un icono. Los **atributos compuestos** para rellenar este componente son los siguientes:

| Atributo | Descripción                                                                                                                   |
| -------- | ----------------------------------------------------------------------------------------------------------------------------- |
| value    | Valor que se enviará al servidor                                                                                              |
| label    | Texto que se mostrará. Puedes usar [i18n](i18n-internationalization.md) archivos (locales)                                    |
| title    | Texto a mostrar cuando mueve el ratón sobre el icono. Puedes usar [i18n](i18n-internationalization.md) archivos (locales)     |
| icon     | Clase de icono. Puedes consultar todos los conjuntos de iconos en la pantalla [icons](icons.md)                                                         |
| unit     | Etiqueta unitaria (se muestra en la derecha de la celda). Puedes usar [i18n](i18n-internationalization.md) archivos (locales) |
| style    | Clase CSS para formatear el contenido                                                                                         |

#### Componente de columna de icono {#icon-column-component}

<img alt="IconColumn" src={require('@docusaurus/useBaseUrl').default('img/IconColumn.png')} />

Este componente es muy útil para mostrar un icono dentro de un grid. Los **atributos compuestos** para rellenar este componente son los siguientes:

| Atributo | Descripción                                                                                                               |
| -------- | ------------------------------------------------------------------------------------------------------------------------- |
| value    | Valor que se enviará al servidor                                                                                          |
| label    | Texto a mostrar cuando mueve el ratón sobre el icono. Puedes usar [i18n](i18n-internationalization.md) archivos (locales) |
| title    | Texto a mostrar cuando mueve el ratón sobre el icono. Puedes usar [i18n](i18n-internationalization.md) archivos (locales) |
| icon     | Clase de icono. Puedes consultar todos los conjuntos de iconos en la pantalla [icons](icons.md)                                                     |
| style    | Clase CSS para formatear el contenido                                                                                     |

#### Componente de columna de texto con formato {#formatted-text-column-component}

Este componente se utiliza para mostrar código HTML dentro de un grid. Se permiten dos tipos de etiquetas HTML:

* `<format style="[text class]">`: Texto formateado.
* `<br>`: Nueva línea

#### Componente de columna de imagen {#image-column-component}

<img alt="Columna" src={require('@docusaurus/useBaseUrl').default('img/ImageColumn.png')} />

Este componente se utiliza para mostrar una imagen dentro de un grid. Los **atributos compuestos** para rellenar este componente son los siguientes:

| Atributo | Descripción                                                                                                               |
| -------- | ------------------------------------------------------------------------------------------------------------------------- |
| value    | Valor que se enviará al servidor                                                                                          |
| label    | Texto alternativo. Puedes usar [i18n](i18n-internationalization.md) archivos (locales)                                    |
| title    | Texto a mostrar cuando mueve el ratón sobre el icono. Puedes usar [i18n](i18n-internationalization.md) archivos (locales) |
| image   | Ruta de imagen                                                                                                            |
| style    | Clase CSS para formatear el contenido                                                                                     |

#### Componente de columna del botón {#button-column-component}

<img alt="Columna del botón" src={require('@docusaurus/useBaseUrl').default('img/DialogColumn.png')} />

Una columna de botón es un componente que permite lanzar un conjunto de `button-action` definido en columna.

| Atributo | Descripción                                                                                                               |
| -------- | ------------------------------------------------------------------------------------------------------------------------- |
| value    | Valor que se enviará al servidor                                                                                          |
| label    | Texto a mostrar en el botón. Puedes usar [i18n](i18n-internationalization.md) archivos (locales)                          |
| title    | Texto a mostrar cuando mueve el ratón sobre el botón. Puedes usar [i18n](i18n-internationalization.md) archivos (locales) |
| icon     | Clase de icono para mostrar en el botón. Puedes consultar todos los conjuntos de iconos en la pantalla [icons](icons.md)                           |
| style    | Clase CSS para formatear el contenido                                                                                     |

#### Componente de columna radio {#radio-column-component}

Las columnas `radio` y `button-radio` muestran **todas las opciones de una lista en cada celda** y el usuario elige una
de ellas. Las opciones salen de la columna igual que en una columna `select`: con `initial-load` y `target-action` (por
ejemplo un enumerado, `enum`, o una consulta) y cada opción tiene un `value` y un `label`. El valor de la opción elegida
es el valor de la celda.

```xml
<enumerated>
  <group id="LevelOptions">
    <option label="Low" value="L"/>
    <option label="Medium" value="M"/>
    <option label="High" value="H"/>
  </group>
</enumerated>
```

```xml
<grid id="GrdTheme" style="expand" initial-load="query" server-action="data" target-action="ThemeList" send-all="true">
  <column label="Theme" name="name" align="left" charlength="30"/>
  <column label="Level" name="level" component="radio" initial-load="enum" target-action="LevelOptions" align="center" charlength="30"/>
  <column label="Mode" name="mode" component="button-radio" initial-load="enum" target-action="ModeOptions" align="center" charlength="30"/>
</grid>
```

| Atributo      | Descripción                                                                                                                  |
|---------------|------------------------------------------------------------------------------------------------------------------------------|
| initial-load  | Cómo se cargan las opciones: `enum` para un enumerado, `query` para una consulta                                              |
| target-action | Grupo del enumerado o consulta que proporciona las opciones (`value` y `label` de cada una)                                   |
| label         | Cabecera de la columna. Es también el nombre accesible del grupo de opciones de cada celda                                    |
| validation    | `required` exige una elección en cada fila                                                                                    |
| readonly      | Una columna o una celda de solo lectura no se puede cambiar                                                                   |

Para empezar con una elección, la consulta que rellena el grid devuelve el valor de la opción en un campo con el nombre de
la columna (aquí `level`, con `L`, `M` o `H`). Una fila en la que ese campo está vacío o es nulo empieza sin elección.

La columna se comporta como cualquier otra columna del grid: se puede ordenar, validar, editar e imprimir.

##### Aspecto de `radio` {#radio-look}

Cada opción es un botón de opción seguido de su etiqueta, y las opciones de una celda van en una línea. Cuando la
columna es estrecha, el motor React las reparte en varias líneas, mientras que el motor AngularJS las mantiene en una y
oculta lo que no cabe, así que da a la columna anchura suficiente (por ejemplo con `charlength`). El botón marcado tiene el
color primario del tema.

##### Aspecto de `button-radio` {#button-radio-look}

Las opciones de una celda son **una barra de botones unidos** (un control segmentado), como el
[criterio botón radio](criteria.md#radio-button-criterion-line): los botones contiguos comparten sus bordes, solo los dos
extremos de la barra son redondeados y la opción elegida aparece resaltada.

##### Teclado, validación y solo lectura {#keyboard-validation-and-read-only}

| Comportamiento | Descripción                                                                                                                                       |
|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| Teclado        | Las flechas izquierda y derecha mueven la elección entre las opciones de la celda (también las de arriba y abajo) y dan la vuelta en los extremos; `Tab` abandona la celda |
| Accesibilidad  | Las opciones de una celda son un `radiogroup` etiquetado con la etiqueta de la columna, y los radios de una fila comparten un nombre             |
| Validación     | Con `validation="required"` una fila sin elección no es válida                                                                                    |
| Solo lectura   | Una columna o una celda de solo lectura no se puede cambiar                                                                                       |

:::note
La opción elegida no se desmarca por sí sola, solo al elegir otra.
:::

##### Datos enviados al servidor {#data-sent-to-the-server}

El grid envía la columna igual que una columna `select`: una lista con un valor por fila (en el mismo orden que las demás
columnas), la opción elegida de cada fila y `null` para una fila sin elección:

```json
{
  "id": [12, 3, 7],
  "level": ["M", null, "H"],
  "level.selected": "H"
}
```

* `<columna>` contiene el valor elegido de cada fila enviada (todas las filas cuando el grid tiene `send-all`, las
  seleccionadas en caso contrario).
* `<columna>.selected` contiene el valor de las filas seleccionadas.
* `<columna>.editing` contiene el valor de las filas que se están editando, en los grids editables y de multioperación.

El objetivo de mantenimiento lee la lista como una variable con el nombre de la columna
(`<variable id="level" type="STRING" name="level"/>`).

#### Componente de columna de progreso {#progress-column-component}

<img alt="Progreso" src={require('@docusaurus/useBaseUrl').default('img/Progress.png')} />

El componente de progreso es muy útil para mostrar el estado de un trabajo, o una tarea.

| Atributo | Descripción                                                                                                               |
| -------- | ------------------------------------------------------------------------------------------------------------------------- |
| value    | Valor (en porcentaje (0-100) de la barra de progreso                                                                      |
| label    | Texto a mostrar en la barra de progreso (es decir, porcentaje)                                                            |
| title    | Texto a mostrar cuando mueve el ratón sobre el botón. Puedes usar [i18n](i18n-internationalization.md) archivos (locales) |
| style    | Clase CSS para formatear el contenido                                                                                     |

#### Componente de columna sparkline {#sparkline-column-component}

Un sparkline dibuja un pequeño gráfico de una serie de números dentro de la celda, de modo que se puede ver una
tendencia por fila (ventas por mes, carga por día) sin abrir un gráfico. El valor de la celda es un **array JSON de
números**, por ejemplo `[3, 5, -1, 8]`. La consulta puede devolverlo como texto o como una lista ya interpretada.

```xml
<column label="LOAD" name="load" component="sparkline" sparkline-type="bar" sparkline-negative-color="#d64550" charlength="14"/>
```

| Atributo                 | Descripción                                                                                                                  | Valores                                   |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------- |
| sparkline-type           | Dibujo de la serie: una línea continua, una barra por valor, o una línea con el área inferior rellena                          | `line` (por defecto), `bar` o `area`      |
| sparkline-color          | Color de la serie. Un valor que no sea un color simple se ignora                                                              | Cualquier color CSS, p. ej.: `#3b82f6`    |
| sparkline-negative-color | Color de los valores negativos: las barras por debajo de cero, o los tramos de la línea y del área que pasan por debajo de cero. Sin él, los valores negativos usan el color de la serie | Cualquier color CSS                       |
| sparkline-last-point     | Resalta el último punto de la serie: un punto en líneas y áreas, un contorno oscuro en barras                                  | `true` o `false` (por defecto)            |
| sparkline-min-max        | Marca con un punto el valor más bajo (rojo) y el más alto (verde) de la serie                                                  | `true` o `false` (por defecto)            |

Cómo se dibuja la serie:

* El gráfico es un SVG de 100 por 24 píxeles, dibujado igual por **los dos motores de cliente**. Da a la columna
  anchura suficiente (`charlength="14"` o más); la alineación de la celda (`align`) coloca el gráfico dentro de ella.
* **Barras** y **valores negativos**: las barras parten de la línea de cero, así que las negativas cuelgan por debajo.
  Las líneas y las áreas se escalan entre el valor más bajo y el más alto; se dibuja una fina línea gris de cero siempre
  que la serie tenga algún valor negativo.
* **Tooltip**: al pasar el ratón sobre un punto se ve su valor. Cada punto tiene una zona sensible tan ancha como su
  parte del gráfico.
* **Un solo valor**: las líneas y las áreas dibujan un punto, las barras dibujan una barra.
* **Serie vacía o no válida**: la celda queda vacía, sin error. Es el caso de un valor vacío, `[]`, un texto que no es
  JSON, un valor que no es una lista y una lista con algún elemento que no es un número.
* La celda es un componente de visualización: no es editable y no es una parada del tabulador (el gráfico no es
  enfocable), así que no interfiere con la navegación por teclado del grid.
* Se ha eliminado el antiguo plugin jQuery sparkline del cliente AngularJS; las aplicaciones que usaran sus clases CSS
  (`jqstooltip`, `stats-sparklines`) deben aportar las suyas.

## Estructura de cabecera de grupo {#group-header-structure}

```xml
<group-header label="[group-header-label]" name="[group-header-name]">
  <column...> ... </column>
  ...
</group-header>
```

<img alt="Encabezado de grupo" src={require('@docusaurus/useBaseUrl').default('img/GroupHeader.png')} />

### Atributos de cabecera de grupo {#group-header-attributes}

| Atributo | Uso             | Tipo   | Descripción                                                 | Valores                                                                    |
| -------- | --------------- | ------ | ----------------------------------------------------------- | -------------------------------------------------------------------------- |
| name     | **Obligatorio** | String | Identificador de cabecera de grupo. Con fines de referencia |                                                                            |
| label    | Opcional        | String | Etiqueta de cabecera de grupo                               | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md) |

## Grid básico {#basic-grid}

El grid básico es el grid estándar sin una estructura de árbol. Se puede definir como grid de una sola selección, grid multiselectivo, grid editable o grid multiopcional y combinaciones entre ellas.

<img alt="BasicGrid" src={require('@docusaurus/useBaseUrl').default('img/BasicGrid.png')} />

### Atributos básicos específicos del grid {#basic-grid-specific-attributes}

| Atributo        | Tipo    | Descripción                         | Valores                            |
| --------------- | ------- | ----------------------------------- | ---------------------------------- |
| show-totals | Boolean | Mostrar una línea con totalizadores | El valor predeterminado es `false` |
| row-numbers | Boolean | Mostrar los números de fila         | El valor predeterminado es `false` |


## Multiselección de grid {#multiselect-grid}

<img alt="MultiselectGrid" src={require('@docusaurus/useBaseUrl').default('img/MultiselectGrid.png')} />

### Atributos específicos de grid de selección múltiple {#multiselect-grid-specific-attributes}

| Atributo                | Tipo    | Descripción                                                                                     | Valores                            |
| ----------------------- | ------- | ----------------------------------------------------------------------------------------------- | ---------------------------------- |
| multiselect          | Boolean | Permitir seleccionar más de una línea                                                           | El valor predeterminado es `false` |
| checkbox-multiselect | Boolean | Permitir seleccionar más de una línea, pero haciendo clic en **solo** en las casillas laterales | El valor predeterminado es `false` |

## Grid de árbol {#tree-grid}

El grid de árbol es un grid que se puede expandir como árbol. Se puede definir como *árbol estándar*, *cargando árbol*, *árbol editable* o *árbol multiopción*, y combinaciones entre ellos.

La estructura de datos **necesita** un identificador **por fila**, y también un **identificador padre**, sin valor si la fila es una fila raíz (sin padre). El campo identificador debe definirse en el atributo tree-id del grid y el padre debe definirse en el atributo tree-parent.

<img alt="TreeGrid" src={require('@docusaurus/useBaseUrl').default('img/TreeGrid.png')} />

### Atributos específicos del grid del árbol {#tree-grid-specific-attributes}

| Atributo            | Tipo    | Descripción                         | Valores                                                          |
| ------------------- | ------- | ----------------------------------- | ---------------------------------------------------------------- |
| treegrid           | Boolean | Establecer grid como treegrid | El valor predeterminado es `false`                               |
| tree-id            | String  | Definir campo identificador         | El valor predeterminado es `id`                                  |
| tree-parent         | String  | Definir campo padre                 | El valor predeterminado es `parent`                               |
| tree-leaf       | String  | Definir el campo 'is leaf'          | El valor predeterminado es `isLeaf`                              |
| expand-column     | String  | Nombre de la columna a expandir     | Debe ser un identificador de columna                             |
| initial-level       | Entero  | Nivel inicial para expandir         | El valor predeterminado es `1`                                   |
| icon-expand | String  | Icono de una rama sin expandir      | **Nota:** Puedes consultar todos los conjuntos de iconos en la pantalla [icons](icons.md) |
| icon-collapse     | String  | Icono de una rama expandida         | **Nota:** Puedes consultar todos los conjuntos de iconos en la pantalla [icons](icons.md) |
| icon-leaf       | String  | Icono de una rama de hoja           | **Nota:** Puedes consultar todos los conjuntos de iconos en la pantalla [icons](icons.md) |

## Grid editable {#editable-grid}

<img alt="Grilla editable" src={require('@docusaurus/useBaseUrl').default('img/EditableGrid.png')} />

### Atributos específicos del grid editable {#editable-grid-specific-attributes}

| Atributo | Tipo    | Descripción                            | Valores                            |
| -------- | ------- | -------------------------------------- | ---------------------------------- |
| editable | Boolean | Permite editar las filas de grid | El valor predeterminado es `false` |

### Mover filas {#moving-rows}

Un grid editable puede reordenar sus filas con cuatro acciones de cliente que mueven la fila seleccionada: `move-row-top`,
`move-row-up`, `move-row-down` y `move-row-bottom`. Consulta la página de [acciones](actions.md). Las filas conservan el nuevo
orden en el modelo del grid, así que ese es el orden que el grid envía al servidor cuando se guarda (usa
`send-all="true"` para enviar todas las filas y no solo las seleccionadas). El orden de las filas se guarda en la posición de los
valores de cada lista de columna, y una aplicación que quiera conservarlo debe guardar esa posición (por ejemplo en una columna
`order`).

```xml
<grid id="GrdSteps" editable="true" send-all="true" server-action="data" target-action="StepList">
  <column name="name" label="COLUMN_NAME" component="text"/>
  <button label="BUTTON_MOVE_ROW_UP" icon="angle-up" id="ButStepUp">
    <button-action type="move-row-up" target="GrdSteps" silent="true"/>
    <dependency target-type="enable" initial="true">
      <dependency-element id="GrdSteps" attribute="selectedRows" condition="eq" value="1"/>
    </dependency>
  </button>
</grid>
```

Con una fila seleccionada, ambos motores web también la mueven con el teclado mientras el grid tiene el foco:

| Teclas           | Acción            |
|------------------|-------------------|
| `Alt+ArrowUp`    | `move-row-up`     |
| `Alt+ArrowDown`  | `move-row-down`   |
| `Alt+Home`       | `move-row-top`    |
| `Alt+End`        | `move-row-bottom` |

Las acciones se ignoran cuando la fila ya está en esa posición, y los grids en árbol no las soportan.

## Multiopción de grid {#multioption-grid}

<img alt="Multiopción" src={require('@docusaurus/useBaseUrl').default('img/MultioptionGrid.png')} />

> **Nota:** El grid multiopción enviará una variable llamada `[GridId]-RowTyp` (donde `[GridId]` es el identificador del grid) que contiene la acción realizada en cada fila (`INSERT`, `UPDATE` o `DELETE` acciones)

### Atributos específicos de grid de múltiples opciones {#multioption-grid-specific-attributes}

| Atributo           | Tipo    | Descripción                                                                       | Valores                            |
| ------------------ | ------- | --------------------------------------------------------------------------------- | ---------------------------------- |
| send-operations | Boolean | Permite editar las filas de grid, pero enviando sólo las líneas modificadas | El valor predeterminado es `false` |

## Botones de grid {#grid-buttons}


Puede añadir elementos de botón para realizar acciones sobre los grids. Por ejemplo, un par de botones para insertar y borrar filas.

<img alt="Botones de grid" src={require('@docusaurus/useBaseUrl').default('img/GridButtons.png')} />

```xml
<grid id="GrdKeyLst" style="expand" initial-load="query" server-action="data" target-action="AweKeyLst" send-operations="true" editable="true" max="40">
  <column label="COLUMN_NAM" name="KeyNam" sort-field="KeyNam" align="left" charlength="20" component="text" validation="required">
    <dependency source-type="query" server-action="unique" target-action="AweKeyUni">
      <dependency-element id="GrdKeyLst" column="KeyNam" attribute="currentRowValue" />
    </dependency>
  </column>
  ...
  <button label="BUTTON_NEW" icon="plus-circle" id="ButGrdKeyLstAdd">
    <button-action type="add-row" target="GrdKeyLst" silent="true" />
  </button>
  <button label="BUTTON_DELETE" icon="trash" id="ButGrdKeyLstDel">
    <button-action type="delete-row" target="GrdKeyLst" silent="true" />
    <dependency target-type="enable" initial="true">
      <dependency-element id="GrdKeyLst" attribute="selectedRows" condition="==" value="1" />
    </dependency>
  </button>
</grid>
```

## Menú contextual {#context-menu}

Puede definir un menú de contexto dentro del grid para ayudar al usuario a realizar acciones con las filas seleccionadas. Ver [menú contextual](context-menu.md).

<img alt="Menú contextual de grid" src={require('@docusaurus/useBaseUrl').default('img/GridContextMenu.png')} />

## Variables {#variables}

El componente grid envía algunas variables específicas al servidor, dependiendo de su estado y atributos. Aquí está la lista de variables que pueden ser enviadas por el componente de grid:

| Variable                        | Tipo         | Condición                              | Descripción                                                                                                                                              |
| ------------------------------- | ------------ | -------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| _[GridName]_                    | Matriz       | Siempre                                | Lista de identificadores de líneas seleccionadas                                                                                                         |
| _[GridName]_-id                 | Matriz       | Siempre                                | Lista de identificadores de fila para registros con operaciones                                                                                          |
| _[ColumnName]_                  | Matriz       | `send-all="false"`                   | Lista de valores de columnas de líneas seleccionadas (para cada columna)                                                                                 |
| _[ColumnName]_                  | Matriz       | `send-all="true"`               | Lista de todos los valores de columna (para cada columna)                                                                                                |
| _[ColumnName]_.selected         | Valor/Matriz | Siempre                                | Lista de valores de columna de líneas seleccionadas (para cada columna). Si sólo hay una fila seleccionada, sólo enviará el valor en lugar de una matriz |
| _[GridName]_.selectedRowAddress | JsonNode     | Cuando sólo hay una línea seleccionada | Nodo Json con la dirección `address` de la fila seleccionada: vista, componente e id de fila                                                                    |
| sort                            | Matriz       | Al ordenar                             | Lista de JsonNodes con la información de ordenamiento de grid (`id`: id de columna, `direction`: dirección de ordenación)                          |
| _[GridName]_.data               | JsonNode     | Siempre                                | Lista de información extra del grid (página, máximos registros, información de ordenación, etc.)                                                 |

El grid multioperación envía las variables de una manera diferente a los otros grids:

| Variable                        | Tipo         | Condición                              | Descripción                                                                                                                                              |
| ------------------------------- | ------------ | -------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| _[GridName]_                    | Entero       | `multioperation="true"`           | Número de operaciones enviadas                                                                                                                           |
| _[GridName]_.RowTyp             | Matriz       | `multioperation="true"`           | Lista de operaciones requeridas para cada columna (sólo para las filas con operaciones)                                                                  |
| _[GridName]_-id                 | Matriz       | Siempre                                | Lista de identificadores de fila para registros con operaciones                                                                                          |
| _[ColumnName]_                  | Matriz       | `multioperation="true"`           | Lista de valores de columna para filas con operaciones (para cada columna)                                                                               |
| _[ColumnName]_.selected         | Valor/Matriz | Siempre                                | Lista de valores de columna de líneas seleccionadas (para cada columna). Si sólo hay una fila seleccionada, sólo enviará el valor en lugar de una matriz |
| _[GridName]_.selectedRowAddress | JsonNode     | Cuando sólo hay una línea seleccionada | Nodo Json con la dirección `address` de la fila seleccionada: vista, componente e id de fila                                                                    |
| sort                            | Matriz       | Al ordenar                             | Lista de JsonNodes con la información de ordenamiento de grid (`id`: id de columna, `direction`: dirección de ordenación)                          |
| _[GridName]_.data               | JsonNode     | Siempre                                | Lista de información extra del grid (página, máximos registros, información de ordenación, etc.)                                                 |

> **Nota:** Ver muestras de uso en [Definición de servicio](service-definition.md#load-beans-from-parameters)