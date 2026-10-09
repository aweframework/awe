---
id: grids
title: Tablas
---

Las tablas son el componente principal de AWE para mostrar las listas de datos. Está definido por *grid* y *columns*:

* La **tabla** es el contenedor de la lista de datos. Tiene la consulta a cargar y un conjunto de atributos para definir el comportamiento de la tabla.
* Cada **columna** contiene la fuente de los datos de la lista de datos, la etiqueta de la columna y otros atributos para administrar cada celda de columna.

También hay una etiqueta **group-header** que se utiliza para mostrar una cabecera de segundo nivel sobre un grupo de encabezados de columnas.

<img alt="Tabla" src={require('@docusaurus/useBaseUrl').default('img/Grid.png')} />

## Esqueleto de XML {#xml-skeleton}

Para definir una cuadrícula **** o un **treegrid** en AWE debe seguir la siguiente estructura:

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
## Estructura de cuadrícula {#grid-structure}

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
| label                  | Opcional        | String  | Título de cuadrícula (sólo para imprimir)                                                                                                       | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                                                                                 |
| style                  | Opcional        | String  | Clases CSS de Grid                                                                                                                              |                                                                                                                                                            |
| carga-inicial          | Opcional        | String  | Llamada de acción del servidor para cargar los datos de la cuadrícula. Sólo soporta el valor de `consulta`                                      |                                                                                                                                                            |
| server-action          | Opcional        | String  | Llamada de acción del servidor                                                                                                                  | Ver [lista de acciones del servidor](actions.md#server-actions)                                                                                            |
| target-action          | Opcional        | String  | Destino para llamar al servidor                                                                                                                 |                                                                                                                                                            |
| max                    | Opcional        | Entero  | Número máximo de elementos a recuperar **por página**                                                                                           |                                                                                                                                                            |
| paginación-desactivada | Opcional        | Boolean | Deshabilitado el elemento de paginación de la cuadrícula                                                                                        | **Nota:** Valor predeterminado `falso`                                                                                                                     |
| valores-paginador      | Opcional        | String  | Activar paginador de cuadrícula. Establecer la lista de valores con el número de filas por página                                               | **Ejemplo:** `pager-value="5,25,50,100"`                                                                                                                   |
| cargar-todo            | Opcional        | Boolean | Cargar todos los valores de la cuadrícula y ordenar y paginar localmente                                                                        | **Nota:** El valor predeterminado es `falso`                                                                                                               |
| enviar todo            | Opcional        | Boolean | Enviar todos los valores de la cuadrícula al servidor en lugar de a los seleccionados. (Las seleccionadas se enviarán el parámetro `.selected`) | **Nota:** El valor predeterminado es `falso`                                                                                                               |
| ayuda                  | Opcional        | String  | Texto de ayuda para la cuadrícula                                                                                                               | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                                                                                 |
| imagen de ayuda        | Opcional        | String  | Imagen de ayuda para la cuadrícula                                                                                                              | Esta **debe** ser una ruta de imagen                                                                                                                       |
| carga de íconos        | Opcional        | String  | Establecer el icono de carga                                                                                                                    | `spinner` (por defecto), `cuadrado`, `círculos`, `alfombra`, `puntos`, `plegando`, `cuadrados`, `círculos`, `cubos`, `iconos`, `personalizados`, `ninguno` |
| números de fila        | Opcional        | Boolean | Whether to show row numbers or not                                                                                                              | El valor predeterminado es `verdadero`                                                                                                                     |
| row-height             | Opcional        | Entero  | Set the row height in pixels                                                                                                                    |                                                                                                                                                            |
| validate-on-save       | Opcional        | Boolean | Set `false` to avoid validate the selected row when clicking the `save` button                                                                  | El valor predeterminado es `verdadero`                                                                                                                     |


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
| hidden     | Opcional        | Boolean | Column is **not** visible on screen. It can still be printed, see [printing columns](#printing-columns)                                                | El valor predeterminado es `falso`                                               |
| imprimible | Opcional        | String  | Whether the column is included in the printed report. See [printing columns](#printing-columns)                                                        | `true` (default) or `false`                                                      |
| align      | Opcional        | String  | La columna **no es** visible                                                                                                                           | `izquierda`, `centro` o `derecha`                                                |
| width      | Opcional        | Entero  | Column width in pixels or percent.                                                                                                                     | Ex.: `width = "10"` or `width = "20%"` **Note:** You can use '*' value for auto. |
| sortable   | Opcional        | Boolean | El campo es ordenable (si no es treegrid)                                                                                                              | El valor predeterminado es `verdadero`                                           |
| movable    | Opcional        | Boolean | Permite mover la posición de la columna en la cuadrícula                                                                                               | El valor predeterminado es `verdadero`                                           |
| sendable   | Opcional        | Boolean | Los datos de columna deben ser enviados al servidor                                                                                                    | El valor predeterminado es `verdadero`                                           |
| charlength | Opcional        | Entero  | Ancho de columna en caracteres                                                                                                                         |                                                                                  |
| label      | Opcional        | String  | Etiqueta de columna                                                                                                                                    | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)       |
| style      | Opcional        | String  | Columna clase css                                                                                                                                      | Clase bruta a aplicar a la columna                                               |
| component  | Opcional        | String  | Tipo de columna (si editable)                                                                                                                          | Ver [componentes](criteria.md#components)                                        |
| max        | Opcional        | Entero  | Número máximo de registros a recuperar cuando la columna se inicializa con acción de destino                                                           | El valor predeterminado es `30`                                                  |
| visibility | Opcional        | Boolean | Visibilidad inicial del componente de columna                                                                                                          | El valor predeterminado es `verdadero`                                           |
| frozen     | Opcional        | Boolean | Mantenga la columna fijada fuera del desplazamiento horizontal. **ADVERTENCIA**: No uses este atributo si el encabezado tiene más de una línea de alto | El valor predeterminado es `falso`                                               |

> **Nota:** Cuando una columna es editable (tiene un *componente*) todos los atributos de criterios pueden ser usados en la columna. Ver **[criterios atributos](criteria.md#criteria-structure)** para más referencias.

### Printing columns

When a screen is printed (see the [print engine guide](../guides/print-guide.md)), each grid decides which columns go to the report using the `printable` attribute of the column:

| `imprimible` | Printed in PDF, DOCX and TEXT     | Printed in XLSX and CSV           |
| ------------ | --------------------------------- | --------------------------------- |
| not set      | Only when the column is on screen | Only when the column is on screen |
| `true`       | Always, even if `hidden="true"`   | Always, even if `hidden="true"`   |
| `excel`      | Never                             | Always, even if `hidden="true"`   |
| `false`      | Never                             | Never                             |

"On screen" means the column is not `hidden="true"` and it has not been hidden at runtime by a `hide-column` dependency action. Declaring `printable="true"`, `printable="excel"` or `printable="false"` takes precedence over the screen state.

This makes it possible to show a styled value on screen while exporting the raw value:

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

Keep in mind:

- A printed column needs its data in the client, so it must be `sendable` (the default). A `sendable="false"` column appears in the report header with empty cells.
- Use `type` (`string`, `integer`, `float`, `date`) on printed columns so spreadsheet cells get the right format.
- Columns inside a `group-header` keep their grouping in the report. Columns of the group that are not printed in a format are left out, and the group header disappears when none of its columns is printed.
- When a print request mixes spreadsheet and document formats and some column is `printable="excel"`, AWE designs the report twice, once per kind of output. Grids that print from a query run that query once per design.
- The values `all` and `tab`, accepted by the schema in earlier versions, have been removed. Replace `all` with `true`.

### Componentes de columna {#column-components}

| Componente           | Descripción                                                                                                           |
| -------------------- | --------------------------------------------------------------------------------------------------------------------- |
| texto                | Componente de columna de texto. Ver el criterio de texto [](criteria.md#text-criterion)                               |
| contraseña           | Componente de columna de contraseña. Ver el criterio de [contraseña](criteria.md#password-criterion)                  |
| textarea             | Textarea column component. See [textarea criterion](criteria.md#textarea-criterion)                                   |
| numeric              | Componente numérico de columna. Ver el criterio numérico [](criteria.md#numeric-criterion)                            |
| fecha                | Componente de columna de fecha. Ver el criterio de fecha [](criteria.md#date-criterion)                               |
| tiempo               | Componente de columna de tiempo. Ver el criterio de tiempo [](criteria.md#time-criterion)                             |
| fecha filtrada       | Componente de columna de fecha filtrado. Ver el criterio de fecha [filtrado](criteria.md#filtered-date-criterion)     |
| seleccionar          | Seleccionar componente de columna. Ver [seleccionar el criterio](criteria.md#select-criterion)                        |
| sugerir              | Sugerir componente de columna. Ver [criterio de sugerencia](criteria.md#suggest-criterion)                            |
| seleccionar-múltiple | Seleccione un componente de columna múltiple. See [select multiple criterion](criteria.md#multiple-select-criterion)  |
| sugerir-múltiple     | Sugerir el componente de múltiples columnas. See [suggest multiple criterion](criteria.md#multiple-suggest-criterion) |
| casilla              | Componente de columna Checkbox. See [checkbox criterion](criteria.md#checkbox-criterion)                              |
| color                | Componente de columna de color. Ver el criterio de color [](criteria.md#color-criterion)                              |
| cargador             | Componente de columna del cargador. Ver el criterio de subida de [](criteria.md#uploader-criterion)                   |
| vista de texto       | Componente de columna de vista de texto. Ver el criterio de vista de texto [](criteria.md#text-view-criterion)        |
| texto-formateado     | Componente de columna de texto HTML. Ver [componente de texto formateado](#formatted-text-column-component)           |
| icon                 | Componente de columna de icono. Úselo para mostrar iconos impresionantes de fuentes en una columna.                   |
| imagen               | Componente de columna de imagen. Utilícelo para mostrar imágenes en una columna.                                      |
| button               | Componente que permite al usuario hacer clic en un botón que lanza un conjunto de `botón-acción`                      |
| progreso             | Componente de columna de progreso. Útil para mostrar una barra de progreso en una columna. (Trabajo en curso)         |
| sparkline            | Sparkline column component. Útil para mostrar un gráfico de sparkline en una columna. (Trabajo en curso)              |
| link                 | HTML Link component. Opens an URL in a new page                                                                       |


### Specific row style

When you need set specific style to all cells of one row (ex.: change background color), you can add a new field called `_style_` with CSS style name.

```xml
<query id="MyQuery" >
    <field id="_style_" alias="_style_" variable="styleClass"/>
    <variable id="styleClass" type="STRING" value="red"/>
</query>
```

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

Este componente es muy útil para mostrar un texto con estilo dentro de una cuadrícula con un icono. Los **atributos compuestos** para rellenar este componente son los siguientes:

| Atributo | Descripción                                                                                                                   |
| -------- | ----------------------------------------------------------------------------------------------------------------------------- |
| value    | Valor que se enviará al servidor                                                                                              |
| label    | Texto que se mostrará. Puedes usar [i18n](i18n-internationalization.md) archivos (locales)                                    |
| title    | Texto a mostrar cuando mueve el ratón sobre el icono. Puedes usar [i18n](i18n-internationalization.md) archivos (locales)     |
| icon     | Clase de icono. You can check all iconset at [icons](icons.md) screen                                                         |
| unit     | Etiqueta unitaria (se muestra en la derecha de la celda). Puedes usar [i18n](i18n-internationalization.md) archivos (locales) |
| style    | Clase CSS para formatear el contenido                                                                                         |

#### Componente de columna de icono {#icon-column-component}

<img alt="IconColumn" src={require('@docusaurus/useBaseUrl').default('img/IconColumn.png')} />

Este componente es muy útil para mostrar un icono dentro de una cuadrícula. Los **atributos compuestos** para rellenar este componente son los siguientes:

| Atributo | Descripción                                                                                                               |
| -------- | ------------------------------------------------------------------------------------------------------------------------- |
| value    | Valor que se enviará al servidor                                                                                          |
| label    | Texto a mostrar cuando mueve el ratón sobre el icono. Puedes usar [i18n](i18n-internationalization.md) archivos (locales) |
| title    | Texto a mostrar cuando mueve el ratón sobre el icono. Puedes usar [i18n](i18n-internationalization.md) archivos (locales) |
| icon     | Clase de icono. You can check all iconset at [icons](icons.md) screen                                                     |
| style    | Clase CSS para formatear el contenido                                                                                     |

#### Componente de columna de texto con formato {#formatted-text-column-component}

Este componente se utiliza para mostrar código HTML dentro de una cuadrícula. Se permiten dos tipos de etiquetas HTML:

* `<format style="[text class]">`: Texto formateado.
* `<br>`: Nueva línea

#### Componente de columna de imagen {#image-column-component}

<img alt="Columna" src={require('@docusaurus/useBaseUrl').default('img/ImageColumn.png')} />

Este componente se utiliza para mostrar una imagen dentro de una cuadrícula. Los **atributos compuestos** para rellenar este componente son los siguientes:

| Atributo | Descripción                                                                                                               |
| -------- | ------------------------------------------------------------------------------------------------------------------------- |
| value    | Valor que se enviará al servidor                                                                                          |
| label    | Texto alternativo. Puedes usar [i18n](i18n-internationalization.md) archivos (locales)                                    |
| title    | Texto a mostrar cuando mueve el ratón sobre el icono. Puedes usar [i18n](i18n-internationalization.md) archivos (locales) |
| imagen   | Ruta de imagen                                                                                                            |
| style    | Clase CSS para formatear el contenido                                                                                     |

#### Componente de columna del botón {#button-column-component}

<img alt="Columna del botón" src={require('@docusaurus/useBaseUrl').default('img/DialogColumn.png')} />

Una columna de botón es un componente que permite lanzar un conjunto de `botón-acción` definido en columna.

| Atributo | Descripción                                                                                                               |
| -------- | ------------------------------------------------------------------------------------------------------------------------- |
| value    | Valor que se enviará al servidor                                                                                          |
| label    | Texto a mostrar en el botón. Puedes usar [i18n](i18n-internationalization.md) archivos (locales)                          |
| title    | Texto a mostrar cuando mueve el ratón sobre el botón. Puedes usar [i18n](i18n-internationalization.md) archivos (locales) |
| icon     | Clase de icono para mostrar en el botón. You can check all iconsets at [icons](icons.md) screen                           |
| style    | Clase CSS para formatear el contenido                                                                                     |

#### Componente de columna de progreso {#progress-column-component}

<img alt="Progreso" src={require('@docusaurus/useBaseUrl').default('img/Progress.png')} />

El componente de progreso es muy útil para mostrar el estado de un trabajo, o una tarea.

| Atributo | Descripción                                                                                                               |
| -------- | ------------------------------------------------------------------------------------------------------------------------- |
| value    | Valor (en porcentaje (0-100) de la barra de progreso                                                                      |
| label    | Texto a mostrar en la barra de progreso (es decir, porcentaje)                                                            |
| title    | Texto a mostrar cuando mueve el ratón sobre el botón. Puedes usar [i18n](i18n-internationalization.md) archivos (locales) |
| style    | Clase CSS para formatear el contenido                                                                                     |

#### Sparkline column component

(Trabajo en curso)

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
| label    | Opcional        | String | Etiqueta de cabecera de grupo                               | **Note:** You can use [i18n](i18n-internationalization.md) files (locales) |

## Cuadrícula básica {#basic-grid}

La cuadrícula básica es la cuadrícula estándar sin una estructura de árbol. Se puede definir como cuadrícula de una sola selección, cuadrícula multiselectiva, cuadrícula editable o cuadrícula multiopcional y combinaciones entre ellas.

<img alt="BasicGrid" src={require('@docusaurus/useBaseUrl').default('img/BasicGrid.png')} />

### Atributos básicos específicos de la cuadrícula {#basic-grid-specific-attributes}

| Atributo        | Tipo    | Descripción                         | Valores                            |
| --------------- | ------- | ----------------------------------- | ---------------------------------- |
| mostrar totales | Boolean | Mostrar una línea con totalizadores | El valor predeterminado es `falso` |
| números de fila | Boolean | Mostrar los números de fila         | El valor predeterminado es `falso` |


## Multiselección de cuadrícula {#multiselect-grid}

<img alt="MultiselectGrid" src={require('@docusaurus/useBaseUrl').default('img/MultiselectGrid.png')} />

### Atributos específicos de cuadrícula de selección múltiple {#multiselect-grid-specific-attributes}

| Atributo                | Tipo    | Descripción                                                                                     | Valores                            |
| ----------------------- | ------- | ----------------------------------------------------------------------------------------------- | ---------------------------------- |
| multiselección          | Boolean | Permitir seleccionar más de una línea                                                           | El valor predeterminado es `falso` |
| checkbox-multiselección | Boolean | Permitir seleccionar más de una línea, pero haciendo clic en **solo** en las casillas laterales | El valor predeterminado es `falso` |

## Rejilla de árbol {#tree-grid}

La rejilla de árbol es una rejilla que se puede expandir como árbol. Se puede definir como *árbol estándar*, *cargando árbol*, *árbol editable* o *árbol multiopción*, y combinaciones entre ellos.

La estructura de datos **necesita** un identificador **por fila**, y también un **identificador padre**, sin valor si la fila es una fila raíz (sin padre). El campo identificador debe definirse en el atributo árbol-id de cuadrícula y el padre debe definirse en el atributo árbol-padre.

<img alt="Cuadrícula" src={require('@docusaurus/useBaseUrl').default('img/TreeGrid.png')} />

### Atributos específicos de la cuadrícula del árbol {#tree-grid-specific-attributes}

| Atributo            | Tipo    | Descripción                         | Valores                                                          |
| ------------------- | ------- | ----------------------------------- | ---------------------------------------------------------------- |
| zambullir           | Boolean | Establecer cuadrícula como treegrid | El valor predeterminado es `falso`                               |
| arbol-id            | String  | Definir campo identificador         | El valor predeterminado es `id`                                  |
| árbol-padre         | String  | Definir campo padre                 | El valor predeterminado es `padre`                               |
| hoja de árbol       | String  | Definir el campo 'is leaf'          | El valor predeterminado es `isLeaf`                              |
| ampliar-columna     | String  | Nombre de la columna a expandir     | Debe ser un identificador de columna                             |
| nivel inicial       | Entero  | Nivel inicial para expandir         | El valor predeterminado es `1`                                   |
| expansión de iconos | String  | Icono de una rama sin expandir      | **Note:** You can check all iconsets at [icons](icons.md) screen |
| icono contraído     | String  | Icono de una rama expandida         | **Note:** You can check all iconsets at [icons](icons.md) screen |
| hoja de icono       | String  | Icono de una rama de hoja           | **Note:** You can check all iconsets at [icons](icons.md) screen |

## Rejilla editable {#editable-grid}

<img alt="Grilla editable" src={require('@docusaurus/useBaseUrl').default('img/EditableGrid.png')} />

### Atributos específicos de rejilla editables {#editable-grid-specific-attributes}

| Atributo | Tipo    | Descripción                            | Valores                            |
| -------- | ------- | -------------------------------------- | ---------------------------------- |
| editable | Boolean | Permite editar las filas de cuadrícula | El valor predeterminado es `falso` |

## Multiopción de cuadrícula {#multioption-grid}

<img alt="Multiopción" src={require('@docusaurus/useBaseUrl').default('img/MultioptionGrid.png')} />

> **Nota:** La cuadrícula multiopción enviará una variable llamada `[GridId]-RowTyp` (donde `[GridId]` es el identificador de la cuadrícula) que contiene la acción realizada en cada fila (`INSERT`, `ACTUALIZACIÓN` o `DELETE` acciones)

### Atributos específicos de cuadrícula de múltiples opciones {#multioption-grid-specific-attributes}

| Atributo           | Tipo    | Descripción                                                                       | Valores                            |
| ------------------ | ------- | --------------------------------------------------------------------------------- | ---------------------------------- |
| enviar-operaciones | Boolean | Permite editar las filas de cuadrícula, pero enviando sólo las líneas modificadas | El valor predeterminado es `falso` |

## Botones de cuadrícula {#grid-buttons}


Puede añadir elementos de botón para realizar acciones sobre las cuadrículas. Por ejemplo, un par de botones para insertar y borrar filas.

<img alt="Botones de rejilla" src={require('@docusaurus/useBaseUrl').default('img/GridButtons.png')} />

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

Puede definir un menú de contexto dentro de la cuadrícula para ayudar al usuario a realizar acciones con las filas seleccionadas. Ver [menú contextual](context-menu.md).

<img alt="Menú contextual cuadriculado" src={require('@docusaurus/useBaseUrl').default('img/GridContextMenu.png')} />

## Variables

El componente grid envía algunas variables específicas al servidor, dependiendo de su estado y atributos. Aquí está la lista de variables que pueden ser enviadas por el componente de cuadrícula:

| Variable                        | Tipo         | Condición                              | Descripción                                                                                                                                              |
| ------------------------------- | ------------ | -------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| _[GridName]_                    | Matriz       | Siempre                                | Lista de identificadores de líneas seleccionadas                                                                                                         |
| _[GridName]_-id                 | Matriz       | Siempre                                | Lista de identificadores de fila para registros con operaciones                                                                                          |
| _[ColumnName]_                  | Matriz       | `enviar-all="falso"`                   | Lista de valores de columnas de líneas seleccionadas (para cada columna)                                                                                 |
| _[ColumnName]_                  | Matriz       | `enviar-all="verdadero"`               | Lista de todos los valores de columna (para cada columna)                                                                                                |
| _[ColumnName]_.selected         | Valor/Matriz | Siempre                                | Lista de valores de columna de líneas seleccionadas (para cada columna). Si sólo hay una fila seleccionada, sólo enviará el valor en lugar de una matriz |
| _[GridName]_.selectedRowAddress | JsonNode     | Cuando sólo hay una línea seleccionada | Nodo Json con la dirección `` de la fila seleccionada: vista, componente e id de fila                                                                    |
| sort                            | Matriz       | When sorting                           | Lista de JsonNodes con la información de ordenamiento de cuadrícula (`id`: id de columna, `dirección`: dirección de ordenación)                          |
| _[GridName]_.data               | JsonNode     | Siempre                                | Lista de información extra de la cuadrícula (página, máximos registros, información de ordenación, etc.)                                                 |

La cuadrícula multioperación envía las variables de una manera diferente a las otras rejillas:

| Variable                        | Tipo         | Condición                              | Descripción                                                                                                                                              |
| ------------------------------- | ------------ | -------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| _[GridName]_                    | Entero       | `multioperation="verdadero"`           | Número de operaciones enviadas                                                                                                                           |
| _[GridName]_.RowTyp             | Matriz       | `multioperation="verdadero"`           | Lista de operaciones requeridas para cada columna (sólo para las filas con operaciones)                                                                  |
| _[GridName]_-id                 | Matriz       | Siempre                                | Lista de identificadores de fila para registros con operaciones                                                                                          |
| _[ColumnName]_                  | Matriz       | `multioperation="verdadero"`           | Lista de valores de columna para filas con operaciones (para cada columna)                                                                               |
| _[ColumnName]_.selected         | Valor/Matriz | Siempre                                | Lista de valores de columna de líneas seleccionadas (para cada columna). Si sólo hay una fila seleccionada, sólo enviará el valor en lugar de una matriz |
| _[GridName]_.selectedRowAddress | JsonNode     | Cuando sólo hay una línea seleccionada | Nodo Json con la dirección `` de la fila seleccionada: vista, componente e id de fila                                                                    |
| sort                            | Matriz       | When sorting                           | Lista de JsonNodes con la información de ordenamiento de cuadrícula (`id`: id de columna, `dirección`: dirección de ordenación)                          |
| _[GridName]_.data               | JsonNode     | Siempre                                | Lista de información extra de la cuadrícula (página, máximos registros, información de ordenación, etc.)                                                 |

> **Nota:** Ver muestras de uso en [Definición de servicio](service-definition.md#load-beans-from-parameters)