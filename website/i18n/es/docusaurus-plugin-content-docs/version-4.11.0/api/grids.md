---
id: grids
title: Tablas
---

Las tablas son el componente principal de AWE para mostrar las listas de datos. It is defined by _grid_ and _columns_:

- The **grid** is the container of the data list. Tiene la consulta a cargar y un conjunto de atributos para definir el comportamiento de la tabla.
- Each **column** contains the source of the data from the data list, the label of the column and other attributes to manage each column cell.

There is also a **group-header** tag which is used to show a second level header over a group of column headers.

<img alt="Grid" src={require('@docusaurus/useBaseUrl').default('img/Grid.png')} />

## Esqueleto de XML {#xml-skeleton}

To define a **grid** or a **treegrid** in AWE you must follow the next structure:

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

| Atributo               | Uso          | Tipo    | Descripción                                                                                                                                                                     | Valores                                                                                                                                               |
| ---------------------- | ------------ | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| id                     | **Required** | String  | Grid identifier. Con fines de referencia                                                                                                                        |                                                                                                                                                       |
| label                  | Opcional     | String  | Título de cuadrícula (sólo para imprimir)                                                                                                                    | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                                         |
| style                  | Opcional     | String  | Clases CSS de Grid                                                                                                                                                              |                                                                                                                                                       |
| carga-inicial          | Opcional     | String  | Llamada de acción del servidor para cargar los datos de la cuadrícula. It only supports `query` value                                                           |                                                                                                                                                       |
| server-action          | Opcional     | String  | Llamada de acción del servidor                                                                                                                                                  | See [server action list](actions.md#server-actions)                                                                                                   |
| target-action          | Opcional     | String  | Destino para llamar al servidor                                                                                                                                                 |                                                                                                                                                       |
| max                    | Opcional     | Entero  | Maximum number of elements to retrieve **per page**                                                                                                                             |                                                                                                                                                       |
| paginación-desactivada | Opcional     | Boolean | Deshabilitado el elemento de paginación de la cuadrícula                                                                                                                        | **Note:** Default value `false`                                                                                                       |
| valores-paginador      | Opcional     | String  | Activar paginador de cuadrícula. Establecer la lista de valores con el número de filas por página                                                               | **Example:** `pager-value="5,25,50,100"`                                                                                              |
| cargar-todo            | Opcional     | Boolean | Cargar todos los valores de la cuadrícula y ordenar y paginar localmente                                                                                                        | **Note:** Default value is `false`                                                                                                    |
| enviar todo            | Opcional     | Boolean | Enviar todos los valores de la cuadrícula al servidor en lugar de a los seleccionados. (Selected ones will be sent on `.selected` parameter) | **Note:** Default value is `false`                                                                                                    |
| ayuda                  | Opcional     | String  | Texto de ayuda para la cuadrícula                                                                                                                                               | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                                         |
| imagen de ayuda        | Opcional     | String  | Imagen de ayuda para la cuadrícula                                                                                                                                              | This **must** be a image path                                                                                                                         |
| carga de íconos        | Opcional     | String  | Establecer el icono de carga                                                                                                                                                    | `spinner` (default), `square`, `circles`, `carpet`, `dots`, `folding`, `squarebar`, `circlebar`, `cubes`, `icon`, `custom`, `none` |
| números de fila        | Opcional     | Boolean | Whether to show row numbers or not                                                                                                                                              | Default value is `true`                                                                                                                               |
| row-height             | Opcional     | Entero  | Set the row height in pixels                                                                                                                                                    |                                                                                                                                                       |
| validate-on-save       | Opcional     | Boolean | Set `true` to validate the selected row when clicking the `save` button                                                                                                         | Default value is `false`                                                                                                                              |

## Estructura de columna {#column-structure}

```xml
<column label="[column-label]" name="[column-name]" sort-field="[sort-field]" align="[text-align]" 
        charlength="[column-width-chars]" component="[column-component]" >
  <dependency ...> ... </dependency>
  ...
</column>
```

### Atributos de columna {#column-attributes}

| Atributo   | Uso          | Tipo    | Descripción                                                                                                                                                                   | Valores                                                                                                                                           |
| ---------- | ------------ | ------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| name       | **Required** | String  | Identificador de columna. Con fines de referencia                                                                                                             |                                                                                                                                                   |
| sort-field | Opcional     | String  | Ordenar por campo (si no treegrid)                                                                                                                         |                                                                                                                                                   |
| type       | Opcional     | String  | Tipo de campo (para fines de impresión)                                                                                                                    | `string`, `integer`, `float` or `date`                                                                                                            |
| hidden     | Opcional     | Boolean | Column is **not** visible                                                                                                                                                     | Default value is `false`                                                                                                                          |
| align      | Opcional     | String  | Column is **not** visible                                                                                                                                                     | `left`, `center` or `right`                                                                                                                       |
| width      | Opcional     | Entero  | Column width in pixels or percent.                                                                                                                            | Ex.: `width = "10"` or `width = "20%"` **Note:** You can use '\*' value for auto. |
| sortable   | Opcional     | Boolean | El campo es ordenable (si no es treegrid)                                                                                                                  | Default value is `true`                                                                                                                           |
| movable    | Opcional     | Boolean | Permite mover la posición de la columna en la cuadrícula                                                                                                                      | Default value is `true`                                                                                                                           |
| sendable   | Opcional     | Boolean | Los datos de columna deben ser enviados al servidor                                                                                                                           | Default value is `true`                                                                                                                           |
| charlength | Opcional     | Entero  | Ancho de columna en caracteres                                                                                                                                                |                                                                                                                                                   |
| label      | Opcional     | String  | Etiqueta de columna                                                                                                                                                           | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                                     |
| style      | Opcional     | String  | Columna clase css                                                                                                                                                             | Clase bruta a aplicar a la columna                                                                                                                |
| component  | Opcional     | String  | Tipo de columna (si editable)                                                                                                                              | See [components](criteria.md#components)                                                                                                          |
| max        | Opcional     | Entero  | Número máximo de registros a recuperar cuando la columna se inicializa con acción de destino                                                                                  | Default value is `30`                                                                                                                             |
| visibility | Opcional     | Boolean | Visibilidad inicial del componente de columna                                                                                                                                 | Default value is `true`                                                                                                                           |
| frozen     | Opcional     | Boolean | Mantenga la columna fijada fuera del desplazamiento horizontal. **WARNING**: Don't use this attribute if header has more than one line height | Default value is `false`                                                                                                                          |

> **Note:** When a column is editable (it has a _component_) all attributes of criteria can be used in the column. See **[criteria attributes](criteria.md#criteria-structure)** for more references.

### Componentes de columna {#column-components}

| Componente           | Descripción                                                                                                                                                      |
| -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| texto                | Componente de columna de texto. See [text criterion](criteria.md#text-criterion)                                                                 |
| contraseña           | Componente de columna de contraseña. See [password criterion](criteria.md#password-criterion)                                                    |
| textarea             | Textarea column component. See [textarea criterion](criteria.md#textarea-criterion)                                                              |
| numeric              | Componente numérico de columna. See [numeric criterion](criteria.md#numeric-criterion)                                                           |
| fecha                | Componente de columna de fecha. See [date criterion](criteria.md#date-criterion)                                                                 |
| tiempo               | Componente de columna de tiempo. See [time criterion](criteria.md#time-criterion)                                                                |
| fecha filtrada       | Componente de columna de fecha filtrado. See [filtered date criterion](criteria.md#filtered-date-criterion)                                      |
| seleccionar          | Seleccionar componente de columna. See [select criterion](criteria.md#select-criterion)                                                          |
| sugerir              | Sugerir componente de columna. See [suggest criterion](criteria.md#suggest-criterion)                                                            |
| seleccionar-múltiple | Seleccione un componente de columna múltiple. See [select multiple criterion](criteria.md#multiple-select-criterion)                             |
| sugerir-múltiple     | Sugerir el componente de múltiples columnas. See [suggest multiple criterion](criteria.md#multiple-suggest-criterion)                            |
| casilla              | Componente de columna Checkbox. See [checkbox criterion](criteria.md#checkbox-criterion)                                                         |
| color                | Componente de columna de color. See [color criterion](criteria.md#color-criterion)                                                               |
| cargador             | Componente de columna del cargador. See [uploader criterion](criteria.md#uploader-criterion)                                                     |
| vista de texto       | Componente de columna de vista de texto. See [text view criterion](criteria.md#text-view-criterion)                                              |
| texto-formateado     | Componente de columna de texto HTML. See [formatted-text component](#formatted-text-column-component)                                            |
| icon                 | Componente de columna de icono. Úselo para mostrar iconos impresionantes de fuentes en una columna.                              |
| imagen               | Componente de columna de imagen. Utilícelo para mostrar imágenes en una columna.                                                 |
| button               | Component which allows the user to click a button which launches a set of `button-action`                                                                        |
| progreso             | Componente de columna de progreso. Útil para mostrar una barra de progreso en una columna. (Trabajo en curso) |
| sparkline            | Sparkline column component. Útil para mostrar un gráfico de sparkline en una columna. (Trabajo en curso)      |
| link                 | HTML Link component. Opens an URL in a new page                                                                                                  |

### Specific row style

When you need set specific style to all cells of one row (ex.: change background color), you can add a new field called `_style_` with CSS style name.

```xml
<query id="MyQuery" >
    <field id="_style_" alias="_style_" variable="styleClass"/>
    <variable id="styleClass" type="STRING" value="red"/>
</query>
```

### Componentes de columna específicos {#specific-column-components}

Existen varios componentes desarrollados específicamente para ser utilizados en células de la red.
Estos componentes necesitan una estructura especial en la consulta que los llene:

```xml
<compound alias="specialCell">
  <computed alias="value" format="xxx"/>  
  <computed alias="label" format="xxx" translate="EnumTranslate"/>
</compound>
```

Estas son las células que utilizan la estructura especial:

#### Celda estándar sin componentes {#standard-cell-without-components}

When you send a **compound structure** to a standard cell, you can use the following attributes:

| Atributo        | Descripción                                                                                                                                               |
| --------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| value           | Valor que se enviará al servidor                                                                                                                          |
| label           | Texto que se mostrará. You can use [i18n](i18n-internationalization.md) files (locales)                                |
| title           | Texto a mostrar cuando mueve el ratón sobre el icono. You can use [i18n](i18n-internationalization.md) files (locales) |
| estilo de celda | Clase CSS para formatear el contenido                                                                                                                     |

#### Componente de columna de vista de texto {#text-view-column-component}

Este componente es muy útil para mostrar un texto con estilo dentro de una cuadrícula con un icono. The **compound attributes** to fill this component are the following:

| Atributo | Descripción                                                                                                                                                                      |
| -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| value    | Valor que se enviará al servidor                                                                                                                                                 |
| label    | Texto que se mostrará. You can use [i18n](i18n-internationalization.md) files (locales)                                                       |
| title    | Texto a mostrar cuando mueve el ratón sobre el icono. You can use [i18n](i18n-internationalization.md) files (locales)                        |
| icon     | Clase de icono. You can check all iconset at [icons](icons.md) screen                                                                                            |
| unit     | Etiqueta unitaria (se muestra en la derecha de la celda). You can use [i18n](i18n-internationalization.md) files (locales) |
| style    | Clase CSS para formatear el contenido                                                                                                                                            |

#### Componente de columna de icono {#icon-column-component}

<img alt="IconColumn" src={require('@docusaurus/useBaseUrl').default('img/IconColumn.png')} />

Este componente es muy útil para mostrar un icono dentro de una cuadrícula. The **compound attributes** to fill this component are the following:

| Atributo | Descripción                                                                                                                                               |
| -------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| value    | Valor que se enviará al servidor                                                                                                                          |
| label    | Texto a mostrar cuando mueve el ratón sobre el icono. You can use [i18n](i18n-internationalization.md) files (locales) |
| title    | Texto a mostrar cuando mueve el ratón sobre el icono. You can use [i18n](i18n-internationalization.md) files (locales) |
| icon     | Clase de icono. You can check all iconset at [icons](icons.md) screen                                                                     |
| style    | Clase CSS para formatear el contenido                                                                                                                     |

#### Componente de columna de texto con formato {#formatted-text-column-component}

Este componente se utiliza para mostrar código HTML dentro de una cuadrícula. Se permiten dos tipos de etiquetas HTML:

- `<format style="[text class]">`: Formatted text.
- `<br>`: New line

#### Componente de columna de imagen {#image-column-component}

<img alt="ImageColumn" src={require('@docusaurus/useBaseUrl').default('img/ImageColumn.png')} />

Este componente se utiliza para mostrar una imagen dentro de una cuadrícula. The **compound attributes** to fill this component are the following:

| Atributo | Descripción                                                                                                                                               |
| -------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| value    | Valor que se enviará al servidor                                                                                                                          |
| label    | Texto alternativo. You can use [i18n](i18n-internationalization.md) files (locales)                                    |
| title    | Texto a mostrar cuando mueve el ratón sobre el icono. You can use [i18n](i18n-internationalization.md) files (locales) |
| imagen   | Ruta de imagen                                                                                                                                            |
| style    | Clase CSS para formatear el contenido                                                                                                                     |

#### Componente de columna del botón {#button-column-component}

<img alt="Button Column" src={require('@docusaurus/useBaseUrl').default('img/DialogColumn.png')} />

A button column is a component which allows to launch a set of `button-action` defined on column.

| Atributo | Descripción                                                                                                                                               |
| -------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| value    | Valor que se enviará al servidor                                                                                                                          |
| label    | Texto a mostrar en el botón. You can use [i18n](i18n-internationalization.md) files (locales)                          |
| title    | Texto a mostrar cuando mueve el ratón sobre el botón. You can use [i18n](i18n-internationalization.md) files (locales) |
| icon     | Clase de icono para mostrar en el botón. You can check all iconsets at [icons](icons.md) screen                                           |
| style    | Clase CSS para formatear el contenido                                                                                                                     |

#### Componente de columna de progreso {#progress-column-component}

<img alt="Progress" src={require('@docusaurus/useBaseUrl').default('img/Progress.png')} />

El componente de progreso es muy útil para mostrar el estado de un trabajo, o una tarea.

| Atributo | Descripción                                                                                                                                               |
| -------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| value    | Valor (en porcentaje (0-100) de la barra de progreso                                                                |
| label    | Texto a mostrar en la barra de progreso (es decir, porcentaje)                                                                         |
| title    | Texto a mostrar cuando mueve el ratón sobre el botón. You can use [i18n](i18n-internationalization.md) files (locales) |
| style    | Clase CSS para formatear el contenido                                                                                                                     |

#### Sparkline column component

(Trabajo en curso)

## Estructura de cabecera de grupo {#group-header-structure}

```xml
<group-header label="[group-header-label]" name="[group-header-name]">
  <column...> ... </column>
  ...
</group-header>
```

<img alt="GroupHeader" src={require('@docusaurus/useBaseUrl').default('img/GroupHeader.png')} />

### Atributos de cabecera de grupo {#group-header-attributes}

| Atributo | Uso          | Tipo   | Descripción                                                                 | Valores                                                                                                       |
| -------- | ------------ | ------ | --------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| name     | **Required** | String | Identificador de cabecera de grupo. Con fines de referencia |                                                                                                               |
| label    | Opcional     | String | Etiqueta de cabecera de grupo                                               | **Note:** You can use [i18n](i18n-internationalization.md) files (locales) |

## Cuadrícula básica {#basic-grid}

La cuadrícula básica es la cuadrícula estándar sin una estructura de árbol. Se puede definir como cuadrícula de una sola selección, cuadrícula multiselectiva, cuadrícula editable o cuadrícula multiopcional y combinaciones entre ellas.

<img alt="BasicGrid" src={require('@docusaurus/useBaseUrl').default('img/BasicGrid.png')} />

### Atributos básicos específicos de la cuadrícula {#basic-grid-specific-attributes}

| Atributo        | Tipo    | Descripción                         | Valores                  |
| --------------- | ------- | ----------------------------------- | ------------------------ |
| mostrar totales | Boolean | Mostrar una línea con totalizadores | Default value is `false` |
| números de fila | Boolean | Mostrar los números de fila         | Default value is `false` |

## Multiselección de cuadrícula {#multiselect-grid}

<img alt="MultiselectGrid" src={require('@docusaurus/useBaseUrl').default('img/MultiselectGrid.png')} />

### Atributos específicos de cuadrícula de selección múltiple {#multiselect-grid-specific-attributes}

| Atributo                | Tipo    | Descripción                                                                      | Valores                  |
| ----------------------- | ------- | -------------------------------------------------------------------------------- | ------------------------ |
| multiselección          | Boolean | Permitir seleccionar más de una línea                                            | Default value is `false` |
| checkbox-multiselección | Boolean | Allow selecting more than one line, but clicking **only** on the side checkboxes | Default value is `false` |

## Rejilla de árbol {#tree-grid}

La rejilla de árbol es una rejilla que se puede expandir como árbol. It can be defined as _standard tree_, _loading tree_, _editable tree_ or _multi-option tree_, and combinations between them.

The data structure **needs** an **identifier per row**, and also a **parent identifier**, with no value if the row is a root row (with no parent). El campo identificador debe definirse en el atributo árbol-id de cuadrícula y el padre debe definirse en el atributo árbol-padre.

<img alt="TreeGrid" src={require('@docusaurus/useBaseUrl').default('img/TreeGrid.png')} />

### Atributos específicos de la cuadrícula del árbol {#tree-grid-specific-attributes}

| Atributo            | Tipo    | Descripción                         | Valores                                                                          |
| ------------------- | ------- | ----------------------------------- | -------------------------------------------------------------------------------- |
| zambullir           | Boolean | Establecer cuadrícula como treegrid | Default value is `false`                                                         |
| arbol-id            | String  | Definir campo identificador         | Default value is `id`                                                            |
| árbol-padre         | String  | Definir campo padre                 | Default value is `parent`                                                        |
| hoja de árbol       | String  | Definir el campo 'is leaf'          | Default value is `isLeaf`                                                        |
| ampliar-columna     | String  | Nombre de la columna a expandir     | Debe ser un identificador de columna                                             |
| nivel inicial       | Entero  | Nivel inicial para expandir         | Default value is `1`                                                             |
| expansión de iconos | String  | Icono de una rama sin expandir      | **Note:** You can check all iconsets at [icons](icons.md) screen |
| icono contraído     | String  | Icono de una rama expandida         | **Note:** You can check all iconsets at [icons](icons.md) screen |
| hoja de icono       | String  | Icono de una rama de hoja           | **Note:** You can check all iconsets at [icons](icons.md) screen |

## Rejilla editable {#editable-grid}

<img alt="EditableGrid" src={require('@docusaurus/useBaseUrl').default('img/EditableGrid.png')} />

### Atributos específicos de rejilla editables {#editable-grid-specific-attributes}

| Atributo | Tipo    | Descripción                            | Valores                  |
| -------- | ------- | -------------------------------------- | ------------------------ |
| editable | Boolean | Permite editar las filas de cuadrícula | Default value is `false` |

## Multiopción de cuadrícula {#multioption-grid}

<img alt="MultioptionGrid" src={require('@docusaurus/useBaseUrl').default('img/MultioptionGrid.png')} />

> **Note:** The multioption grid will send a variable called `[GridId]-RowTyp` (where `[GridId]` is the identifier of the grid) containing the action done in each row (`INSERT`, `UPDATE` or `DELETE` actions)

### Atributos específicos de cuadrícula de múltiples opciones {#multioption-grid-specific-attributes}

| Atributo           | Tipo    | Descripción                                                                       | Valores                  |
| ------------------ | ------- | --------------------------------------------------------------------------------- | ------------------------ |
| enviar-operaciones | Boolean | Permite editar las filas de cuadrícula, pero enviando sólo las líneas modificadas | Default value is `false` |

## Botones de cuadrícula {#grid-buttons}

Puede añadir elementos de botón para realizar acciones sobre las cuadrículas. Por ejemplo, un par de botones para insertar y borrar filas.

<img alt="GridButtons" src={require('@docusaurus/useBaseUrl').default('img/GridButtons.png')} />

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

Puede definir un menú de contexto dentro de la cuadrícula para ayudar al usuario a realizar acciones con las filas seleccionadas. See [context menu](context-menu.md).

<img alt="GridContextMenu" src={require('@docusaurus/useBaseUrl').default('img/GridContextMenu.png')} />

## Variables

El componente grid envía algunas variables específicas al servidor, dependiendo de su estado y atributos.
Aquí está la lista de variables que pueden ser enviadas por el componente de cuadrícula:

| Variable                                                                                    | Tipo         | Condición                              | Descripción                                                                                                                                                                                 |
| ------------------------------------------------------------------------------------------- | ------------ | -------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| _[GridName]_                            | Matriz       | Siempre                                | Lista de identificadores de líneas seleccionadas                                                                                                                                            |
| _[ColumnName]_                          | Matriz       | `send-all="false"`                     | Lista de valores de columnas de líneas seleccionadas (para cada columna)                                                                                                 |
| _[ColumnName]_                          | Matriz       | `send-all="true"`                      | Lista de todos los valores de columna (para cada columna)                                                                                                                |
| _[ColumnName]_.selected | Valor/Matriz | Siempre                                | Lista de valores de columna de líneas seleccionadas (para cada columna). Si sólo hay una fila seleccionada, sólo enviará el valor en lugar de una matriz |
| selectedRowAddress                                                                          | JsonNode     | Cuando sólo hay una línea seleccionada | Json node with the `address` of the selected row: view, component and row id                                                                                                |
| sort                                                                                        | Matriz       | Siempre                                | List of JsonNodes with the grid sort information (`id`: column id, `direction`: sort direction)                                          |
| _[GridName]_.data       | JsonNode     | Siempre                                | Lista de información extra de la cuadrícula (página, máximos registros, información de ordenación, etc.)                                                 |

La cuadrícula multioperación envía las variables de una manera diferente a las otras rejillas:

| Variable                                                                                    | Tipo         | Condición                              | Descripción                                                                                                                                                                                 |
| ------------------------------------------------------------------------------------------- | ------------ | -------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| _[GridName]_                            | Entero       | `multioperation="true"`                | Número de operaciones enviadas                                                                                                                                                              |
| _[GridName]_.RowTyp     | Matriz       | `multioperation="true"`                | Lista de operaciones requeridas para cada columna (sólo para las filas con operaciones)                                                                                  |
| _[GridName]_-id                         | Matriz       | `multioperation="true"`                | Lista de identificadores de fila para registros con operaciones                                                                                                                             |
| _[ColumnName]_                          | Matriz       | `multioperation="true"`                | Lista de valores de columna para filas con operaciones (para cada columna)                                                                                               |
| _[ColumnName]_.selected | Valor/Matriz | Siempre                                | Lista de valores de columna de líneas seleccionadas (para cada columna). Si sólo hay una fila seleccionada, sólo enviará el valor en lugar de una matriz |
| selectedRowAddress                                                                          | JsonNode     | Cuando sólo hay una línea seleccionada | Json node with the `address` of the selected row: view, component and row id                                                                                                |
| sort                                                                                        | Matriz       | Siempre                                | List of JsonNodes with the grid sort information (`id`: column id, `direction`: sort direction)                                          |
| _[GridName]_.data       | JsonNode     | Siempre                                | Lista de información extra de la cuadrícula (página, máximos registros, información de ordenación, etc.)                                                 |

> **Note:** See samples of usages on [Service Definition](service-definition.md#load-beans-from-parameters)