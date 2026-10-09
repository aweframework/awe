---
id: actions
title: Actions
---

To perform an action when we click a button, or a dependency is activated, we need to define a `button-action` or a `dependency-action`. El formato de estas acciones es el siguiente:

```xml
<button-action type="[action]" target="[action-target]" context="[action-context]" silent="[silent]" async="[async]"
               server-action="[server-action]" target-action="[server-target-action]"/>
```

o

```xml
<dependency-action type="[action]" target="[action-target]" context="[action-context]" silent="[silent]" async="[async]"
                   server-action="[server-action]" target-action="[server-target-action]"/>
```

## Atributos de acción {#action-attributes}

| Atributo        |      Uso     |   Tipo  | Descripción                                                                      | Valores                                                                       |
| --------------- | :----------: | :-----: | -------------------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| `type`          | **Required** |  String | Acción que se lanzará.                                           | See [client action list](#client-actions)                                     |
| `target`        |   Opcional   |  String | Objetivo de la acción                                                            | Opción, mensaje o identificador de componente, dependiendo del tipo de acción |
| `context`       |   Opcional   |  String | Contexto donde está el objetivo                                                  | Vista en la que se encuentra el componente                                    |
| `server-action` |   Opcional   |  String | Llamada de acción del servidor                                                   | See [server action list](#server-actions)                                     |
| `target-action` |   Opcional   |  String | Destino para llamar al servidor                                                  |                                                                               |
| `silent`        |   Opcional   | Boolean | Iniciar la acción sin mostrar la barra de carga                                  |                                                                               |
| `async`         |   Opcional   | Boolean | Launch the action in the [async stack](#async-stack)                             |                                                                               |
| `value`         |   Opcional   |  String | Valor a establecer para el criterio en caso de que la acción sea de tipo "value" |                                                                               |

## Acciones del cliente {#client-actions}

Las acciones de los clientes son procesos iniciados en el lado del cliente de la aplicación web (en el navegador). They are launched sequentially (unless defined as [async](#async-stack)). Depending on the action type, the `target` and `context` attributes may differ in meaning.

### Comunes {#general}

Acciones genéricas que se lanzan en el navegador. Estas acciones no necesitan atributos extra y se aplican a todos los componentes de la pantalla.

#### Acciones generales {#general-actions}

| Action                     | Descripción                                                                                                                                                                 |
| -------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `reset`                    | Vaciar todos los criterios en la pantalla                                                                                                                                   |
| `restore`                  | Restaurar los valores iniciales de todos los criterios en la pantalla                                                                                                       |
| `restore-target`           | Restaurar los valores de los criterios de la pantalla que estén dentro de una etiqueta con un identificador específico (definido en <code>target</code>) |
| `validate`                 | Iniciar una validación de todos los criterios de la pantalla                                                                                                                |
| `print`                    | Lanza una impresión de la pantalla. Es igual que la opción de imprimir del navegador                                                                        |
| `confirm-updated-data`     | Muestra un diálogo de confirmación para advertir al usuario que hay criterios con datos en la pantalla, y puede perderlos al navegar a otra pantalla                        |
| `confirm-not-updated-data` | Muestra un diálogo de confirmación para advertir al usuario que los criterios en la pantalla no han sido actualizados                                                       |
| `confirm-empty-data`       | Muestra un diálogo de confirmación para advertir al usuario que todos los criterios en la pantalla están vacíos                                                             |
| `resize`                   | Fuerza un chequeo de tamaño de pantalla (útil en caso de que no se actualicen bien los tamaños de los componentes)                                       |
| `toggle-menu`              | Mostrar / Ocultar el menú                                                                                                                                                   |
| `toggle-navbar`            | Mostrar / Ocultar la barra de navegación                                                                                                                                    |
| `disable-dependencies`     | Disable the [dependency](dependencies.md) system                                                                                                                            |
| `enable-dependencies`      | Enable the [dependency](dependencies.md) system                                                                                                                             |
| `cancel`                   | Limpia la pila de acciones actual                                                                                                                                           |
| `value`                    | Establece un valor a un criterio                                                                                                                                            |
| `wait`                     | Wait an amount of milliseconds defined on `target` attribute                                                                                                                |
| `close-window`             | Intenta cerrar la ventana actual del navegador (a veces pedirá permiso al usuario)                                                                       |

### Mensaje {#message}

Acciones que eventualmente pueden mostrar un mensaje al usuario.

#### Acciones de mensaje {#message-actions}

| Action    | Descripción                                                            |
| --------- | ---------------------------------------------------------------------- |
| `confirm` | Launch a confirm dialog with the message defined on `target` attribute |

#### Atributos del mensaje {#message-attributes}

| Atributo |      Uso     |  Tipo  | Descripción              | Valores |
| -------- | :----------: | :----: | ------------------------ | ------- |
| `target` | **Required** | String | Identificador de mensaje |         |

### Navegación {#navigation}

Las acciones de navegación se utilizan para pasar de una pantalla a otra.

#### Acciones de navegación {#navigation-actions}

| Action           | Descripción                                                                                                                                                              |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `back`           | Ir a la pantalla anterior. Igual que el botón atrás del navegador. Does **not** need `target` attribute.                 |
| `forward`        | Ir a la siguiente pantalla. Igual que el botón de avance del navegador. Does **not** need `target` attribute.            |
| `screen`         | Ir a una pantalla. **Needs** `target` attribute.                                                                                         |
| `reload`         | Recargar pantalla actual                                                                                                                                                 |
| `logout`         | Cerrar sesión y salir del menú privado.  Does **not** need `target` attribute.                                                           |
| `redirect`       | Redirects the current screen to a new URL defined in `target` attribute. If parameter `newWindow` is set to `true`, the URL will be open in a new window |
| `redirectScreen` | Launches a `redirect` action if the current screen matches the `screen` parameter                                                                                        |

#### Atributos de navegación {#navigation-attributes}

| Atributo    |         Uso         |   Tipo  | Descripción                                                                                                                         | Valores                                                               |
| ----------- | :-----------------: | :-----: | ----------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| `target`    | _Depends_ on action |  String | Identificador de opción                                                                                                             |                                                                       |
| `context`   |       Opcional      |  String | Contexto de la pantalla. Si no está definido, el contexto es el mismo que la pantalla del lanzador. | `screen/public` for public options, `screen/home` for private options |
| `newWindow` |       Opcional      | Boolean | Open the redirect url in a new window.                                                                              |                                                                       |
| `screen`    | _Depends_ on action |  String | Screen to check when launching the `redirectScreen` action.                                                         |                                                                       |

### Componente {#component}

Acciones que funcionan sobre componentes en la pantalla.

#### Acciones del componente {#component-actions}

| Action                           | Descripción                                                                                                  | Funciona en                 |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------ | --------------------------- |
| `add-class`                      | Add the css class/classes defined on `target-action` to the tag with the selector defined on `target`        | `tag`                       |
| `remove-class`                   | Remove the css class/classes defined on `target-action` to the tag with the selector defined on `target`     | `tag`                       |
| `toggle-class`                   | Toggle the css class/classes defined on `target-action` to the tag with the selector defined on `target`     | `tag`                       |
| `reset`                          | Vaciar un valor de criterio                                                                                  | `criteria`, `grid`, `chart` |
| `restore`                        | Restaurar a los valores iniciales un criterio                                                                | `criteria`, `grid`, `chart` |
| `start-load`                     | Sets a component as _loading_                                                                                | `criteria`, `grid`, `chart` |
| `copy-criterion-value-clipboard` | Copy a criterion value to the clipboard                                                                      | `criteria`                  |
| `validate`                       | Launch a validation on the criterion or criteria inside the `target` tag                                     | `tag`, `criteria`           |
| `dialog`                         | Abre un diálogo modal                                                                                        | `dialog`                    |
| `close`                          | Cierra un diálogo                                                                                            | `dialog`                    |
| `filter`                         | Recargar una matriz                                                                                          | `grid`                      |
| `add-row`                        | Añadir una fila vacía en la parte inferior de la matriz                                                      | `grid`                      |
| `add-row-top`                    | Añadir una fila vacía en la parte superior de la matriz                                                      | `grid`                      |
| `add-row-up`                     | Añadir una fila vacía sobre la fila seleccionada                                                             | `grid`                      |
| `add-row-down`                   | Añadir una fila vacía debajo de la fila seleccionada                                                         | `grid`                      |
| `copy-row`                       | Copia la fila seleccionada en la parte inferior de la matriz                                                 | `grid`                      |
| `copy-row-top`                   | Copia la fila seleccionada en la parte superior de la matriz                                                 | `grid`                      |
| `copy-row-up`                    | Copiar la fila seleccionada sobre la fila seleccionada                                                       | `grid`                      |
| `copy-row-down`                  | Copiar la fila seleccionada debajo de la fila seleccionada                                                   | `grid`                      |
| `delete-row`                     | Eliminar la fila seleccionada                                                                                | `grid`                      |
| `save-row`                       | Guardar la fila seleccionada                                                                                 | `grid`                      |
| `check-one-selected`             | Comprueba si hay una fila seleccionada                                                                       | `grid`                      |
| `check-some-selected`            | Comprueba si hay una o más filas seleccionadas                                                               | `grid`                      |
| `check-records-saved`            | Comprueba si todos los registros están almacenados (el usuario no está editando una fila) | `grid`                      |
| `check-records-generated`        | Comprueba si hay al menos una fila en la matriz                                                              | `grid`                      |
| `select-first-row`               | Selecciona la primera fila de la matriz                                                                      | `grid`                      |
| `select-last-row`                | Selecciona la última fila de la matriz                                                                       | `grid`                      |
| `select-all-rows`                | Seleccionar todas las filas de la matriz                                                                     | `grid`                      |
| `unselect-all-rows`              | Deseleccionar todas las filas de la tabla                                                                    | `grid`                      |
| `validate-selected-row`          | Iniciar una validación en la fila seleccionada de la matriz                                                  | `grid`                      |
| `copy-selected-rows-clipboard`   | Copy the selected rows on the grid to the clipboard                                                          | `grid`                      |
| `change-theme`                   | Changes the theme to the value defined on the `target` criterion                                             | `criteria`                  |
| `change-language`                | Changes the language to the value defined on the `target` criterion                                          | `criteria`                  |
| `reload-language`                | Recargar el idioma buscando cambios                                                                          | `criteria`                  |
| `next-step`                      | Mover al siguiente paso del asistente                                                                        | `wizard`                    |
| `prev-step`                      | Mover al paso anterior del asistente                                                                         | `wizard`                    |
| `first-step`                     | Mover al primer paso del asistente                                                                           | `wizard`                    |
| `last-step`                      | Mover al último paso del asistente                                                                           | `wizard`                    |
| `nth-step`                       | Mover al paso n del asistente                                                                                | `wizard`                    |

#### Atributos del componente {#component-attributes}

| Atributo  |      Uso     |  Tipo  | Descripción                                                                                                                   | Valores                                                                  |
| --------- | :----------: | :----: | ----------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| `target`  | **Required** | String | Identificador de componente o etiqueta                                                                                        |                                                                          |
| `context` |   Opcional   | String | Contexto del componente. Si no se define, el contexto es el mismo que el componente lanzador. | `base` for the menu container screen, `report@home` for the menu options |

### Llamada al servidor {#server-call}

#### Acciones de llamada al servidor {#server-call-actions}

| Action            | Descripción                                                                                                         |
| ----------------- | ------------------------------------------------------------------------------------------------------------------- |
| `server`          | Lanzar una llamada de servidor con los parámetros de la pantalla                                                    |
| `server-print`    | Iniciar una llamada al servidor con los parámetros de la pantalla e información adicional de impresión              |
| `server-download` | Lanzar una llamada de servidor con los parámetros de la pantalla para llamar a una acción que descargará un archivo |

#### Atributos de llamada del servidor {#server-call-attributes}

| Atributo        |      Uso     |  Tipo  | Descripción                                                                                                                                     | Valores                                                                                  |
| --------------- | :----------: | :----: | ----------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| `target`        |   Opcional   | String | Componente o identificador de mensaje para la respuesta                                                                                         |                                                                                          |
| `context`       |   Opcional   | String | Contexto del componente para la respuesta. Si no se define, el contexto es el mismo que el componente lanzador. | `base` for the menu container screen, `report@home` for the menu options                 |
| `server-action` | **Required** | String | Acción para lanzar en el servidor.                                                                                              | See [server actions](#server-actions)                                                    |
| `target-action` | **Required** | String | Objetivo para la acción del servidor                                                                                                            | [Query identifier](query-definition.md) or [Maintain identifier](maintain-definition.md) |

## Acciones del servidor {#server-actions}

| Action                    | Descripción                                                                                                                                                                    | Valores                                                                                                |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------ |
| `login`                   | Iniciar sesión en la aplicación                                                                                                                                                | Usuario y contraseña                                                                                   |
| `logout`                  |                                                                                                                                                                                |                                                                                                        |
| `screen`                  | Navegar a una pantalla                                                                                                                                                         | `target`: Menu option identifier                                                       |
| `data`                    | Obtener datos del servidor. Fills the `values` list of the criterion                                                                                           | `targetAction`: Query identifier                                                       |
| `value`                   | Obtener un valor del servidor. Fills the `selected` list of the criterion. _In suggests, this action also fills the `values` list_             | `targetAction`: Query identifier                                                       |
| `update-model`            | Obtener valores para algunos criterios. The query field alias **must** be the criterion id to match. Fills the `selected` list of the criteria | `targetAction`: Query identifier                                                       |
| `subscribe`               | Suscribirse a una recuperación de datos de consulta. La suscripción almacena la solicitud del componente y transmite datos a la dirección del componente       | `targetAction`: Query identifier                                                       |
| `maintain`                | Lanzar un proceso de mantenimiento                                                                                                                                             | `targetAction`: Maintain target identifier                                             |
| `maintain-silent`         | Lanzar un proceso de mantenimiento sin mensaje de respuesta                                                                                                                    | `targetAction`: Maintain target identifier                                             |
| `maintain-async`          | Lanzar un proceso de mantenimiento sin ninguna respuesta                                                                                                                       | `targetAction`: Maintain target identifier                                             |
| `validate`                | Ejecutar una consulta, y si devuelve una advertencia o error, invalida el componente lanzador con un mensaje                                                                   | `targetAction`: Query identifier                                                       |
| `unique`                  | Ejecutar una consulta, y si devuelve datos, no valida el lanzador                                                                                                              | `targetAction`: Query identifier                                                       |
| `control`                 | Lanzar una consulta, y si no termina bien, devuelve un mensaje                                                                                                                 | `targetAction`: Query identifier                                                       |
| `control-cancel`          | Lanzar una consulta, y si no termina bien, devuelve un mensaje y cancela la cola de acciones                                                                                   | `targetAction`: Query identifier                                                       |
| `control-confirm`         | Lanzar una consulta, y si no termina bien, devuelve un mensaje de confirmación                                                                                                 | `targetAction`: Query identifier, `target`: Confirm message identifier |
| `control-confirm-cancel`  | Lanzar una consulta, y si termina bien, cancela la cola y si termina con un error, devuelve un mensaje de confirmación                                                         | `targetAction`: Query identifier, `target`: Confirm message identifier |
| `control-confirm-message` | Lanzar una consulta, y si no termina bien, devuelve un mensaje de confirmación con la salida del mensaje de consulta                                                           | `targetAction`: Query identifier                                                       |
| `control-empty-cancel`    | Lanzar una consulta, y si no devuelve datos, muestra un mensaje y cancela la cola                                                                                              | `targetAction`: Query identifier, `target`: Message identifier         |
| `control-unique-cancel`   | Lanzar una consulta, y si devuelve datos, muestra un mensaje y cancela la cola                                                                                                 | `targetAction`: Query identifier, `target`: Message identifier         |
| `app-help`                | Muestra la ayuda de pantalla generada automáticamente basándose en los atributos de la pantalla                                                                                | `target`: Screen identifier (optional)                              |
| `help`                    | Obtener el libro de ayuda de la aplicación en el idioma actual                                                                                                                 |                                                                                                        |
| `get-file-maintain`       | Lanzar un proceso de mantenimiento y recuperar un archivo para descargar                                                                                                       |                                                                                                        |

## Acciones del cliente de servicios Java {#client-actions-from-java-services}

Este tipo de acciones se utilizan para ejecutar las acciones del cliente desde los servicios java en el servidor. Son útiles para realizar muchas acciones en el elemento de la ventana con un único servicio. Por ejemplo, rellene criterios y matrices a la vez, añada dinámicamente columnas a una matriz, agregue o reemplace series en las gráficas, etc.

### Acciones generales de servicios java {#general-actions-from-java-services}

| Action           | Descripción                                                  | Parámetros                                                                                                                           |
| ---------------- | ------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| `message`        | Enviar un mensaje al cliente (sin target) | `type` - Message type (`ok`, `info`, `warning`, `error`), `title` - Message title, `message`- Message description |
| `target-message` | Enviar un mensaje a un componente cliente                    | `type` - Message type (`ok`, `info`, `warning`, `error`), `title` - Message title, `message`- Message description |

**message**

```java
// Client action list
ServiceData serviceData = new ServiceData();

// Message action
ClientAction messageAction = new ClientAction("message");
messageAction.addParameter("type", "warning");
messageAction.addParameter("title", title);
messageAction.addParameter("message", message);
serviceData.addClientAction(messageAction);
```

**target-message**

```java
// Client action list
ServiceData serviceData = new ServiceData();

// Message action
ClientAction messageAction = new ClientAction("target-message");
messageAction.addParameter("type", "warning");
messageAction.addParameter("title", title);
messageAction.addParameter("message", message);

// Optional
ComponentAddress address = new ComponentAddress(applicationName, null, "report", null, "CriterionId", null, null);
messageAction.setAddress(address);

serviceData.addClientAction(messageAction);
```

### Acciones de criterios de servicios java {#criteria-actions-from-java-services}

| Action   | Descripción                                                                  | Parámetros                                                                                    |
| -------- | ---------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| `select` | Rellenar los valores seleccionados en un criterio                            | `values` - Datalist with criteria value                                                       |
| `fill`   | Rellenar la lista de los valores disponibles para seleccionar en un criterio | `rows` -  rows of datalist (at least must have `value` and `label` fields) |

**select**

```java
ServiceData setCriteriaValue() {
 // Variable initialization
 ServiceData serviceData = new ServiceData();

 // Create client action to fill criteria
 ClientAction selectCrtTransactionIdAction = new ClientAction("select");

 // Set target (Criteria ID to set value)
 selectCrtTransactionIdAction.setTarget("transactionId");

 // Add parameters to actions
 selectCrtTransactionIdAction.addParameter("values", Arrays.asList("TR001"))

 // Add actions to serviceData
 serviceData.addClientAction(selectCrtTransactionIdAction);

 return serviceData;
}
```

### Acciones de matrices desde servicios de java {#grid-actions-from-java-services}

| Action            | Descripción                                                                                                                                                                                    | Parámetros                                                                                                                                                                                   |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `fill`            | Rellena una tabla con una lista de datos                                                                                                                                                       | `rows` -  rows of datalist                                                                                                                                                                   |
|                   |                                                                                                                                                                                                | `total` - Number of total pages of datalist                                                                                                                                                  |
|                   |                                                                                                                                                                                                | `page` - Page of datalist                                                                                                                                                                    |
|                   |                                                                                                                                                                                                | `records` - Records number of datalist                                                                                                                                                       |
| `add-columns`     | Añadir columnas a la estructura de la matriz                                                                                                                                                   | `columns` - ArrayNode with column structure                                                                                                                                                  |
| `replace-columns` | Reemplazar las columnas de la matriz con otra estructura de columnas                                                                                                                           | `columns` - ArrayNode with column structure                                                                                                                                                  |
| `update-cell`     | Actualizar una celda en una matriz. Utilizado para actualizar una celda de una tabla con otro valor, estilo, icono, título ... | `values` - ObjectNode with cell attributes                                                                                                                                                   |
| `add-row`         | Añadir una fila en la parte inferior de la matriz                                                                                                                                              | `row` - row values (optional)                                                                                                                                             |
|                   |                                                                                                                                                                                                | `selectedRow` - Selected row identifier (optional)                                                                                                                        |
| `add-row-top`     | Añadir una fila en la parte superior de la matriz                                                                                                                                              | `row` - row values (optional)                                                                                                                                             |
|                   |                                                                                                                                                                                                | `selectedRow` - Selected row identifier (optional)                                                                                                                        |
| `add-row-up`      | Añadir una fila sobre la fila seleccionada                                                                                                                                                     | `row` - row values (optional)                                                                                                                                             |
|                   |                                                                                                                                                                                                | `selectedRow` - Selected row identifier (optional)                                                                                                                        |
| `add-row-down`    | Añadir una fila debajo de la fila seleccionada                                                                                                                                                 | `row` - row values (optional)                                                                                                                                             |
|                   |                                                                                                                                                                                                | `selectedRow` - Selected row identifier (optional)                                                                                                                        |
| `copy-row`        | Copia la fila seleccionada en la parte inferior de la matriz                                                                                                                                   | `selectedRow` - Selected row identifier (optional)                                                                                                                        |
| `copy-row-top`    | Copia la fila seleccionada en la parte superior de la matriz                                                                                                                                   | `selectedRow` - Selected row identifier (optional)                                                                                                                        |
| `copy-row-up`     | Copiar la fila seleccionada sobre la fila seleccionada                                                                                                                                         | `selectedRow` - Selected row identifier (optional)                                                                                                                        |
| `copy-row-down`   | Copiar la fila seleccionada debajo de la fila seleccionada                                                                                                                                     | `selectedRow` - Selected row identifier (optional)                                                                                                                        |
| `update-row`      | Actualizar los valores de fila seleccionados                                                                                                                                                   | `row` - row values, `rowId` - id of row to be updated (if none given, selected one will be selected), `style` - CSS class to add to the row (optional) |
| `delete-row`      | Eliminar la fila seleccionada                                                                                                                                                                  |                                                                                                                                                                                              |

**fill**

Using a list of beans as parameters:

```java
@Data
@Accessors(chain=true)
public class MyBean {
  private String id;
  private String label;
  private Integer type;
}
```

The _fill_ action should be something like:

```java
public ServiceData fillGrid(List<MyBean> beanList) {
 return new ServiceData()
   .addClientAction(new FillActionBuilder("GridToFill", DataListUtil.fromBeanList(beanList))
     .build()
   );
}
```

Using parameter list the _fill_ action should be like:

```java
public ServiceData fillGrid(List<String> idList, List<String> labelList, List<Integer> typeList) {
  DataList dataList = new DataList();
  DataListUtil.addColumn(dataList, "id", idList);
  DataListUtil.addColumn(dataList, "label", labelList);
  DataListUtil.addColumn(dataList, "type", typeList);
  
  return new ServiceData()
   .addClientAction(new FillActionBuilder("GridToFill", dataList)
     .build()
   );
}
```

**add-columns**

```java
ServiceData addColumnsToGrid() {
 // Variable initialization
 ServiceData serviceData = new ServiceData();

 // Create client action to fill many criteria
 ClientAction addColumnsAction = new ClientAction("add-columns");

 // Build dynamic column structure
List<Column> columnList = new ArrayList<Column>();
 Column columnName = new Column();
 columnName.setName("Name");
 columnName.setLabel("COLUMN_LABEL");
 columnName.setCharLength("15");
 columnName.setField("Name");
 columnName.setAlign("Center");

 // Add column to list
 columnList.add(columnName);

 // Build column structure
 ArrayNode columns = ColumnUtil.buildColumnListStructure(columnList);

 addColumnsAction.setTarget("GrdDivididendList");

 // Add parameters to action
 addColumnsAction.addParameter("columns", new CellData(columns));

 // Add action to list
 clientActionList.add(addColumnsAction);

 return serviceData;
}
```

**replace-columns**

```java
ServiceData replaceColumnsToGrid() {
 // Variable initialization
 ServiceData serviceData = new ServiceData();

 // Create client action to fill many criteria
 ClientAction replaceColumnsAction = new ClientAction("replace-columns");

 // Build dynamic column structure
 ArrayList<Column> columnList = new ArrayList<Column>();
 Column columnName = new Column();
 column.setName("Name");
 column.setLabel("COLUMN_LABEL");
 column.setCharLength("15");
 column.setField("Name");
 column.setAlign("Center");

 Column columnType = new Column();
 columnType.setName("Type");
 columnType.setLabel("COLUMN_TYPE");
 columnType.setCharLength("15");
 columnType.setField("Type");
 columnType.setAlign("Center");

 // Add column to list
 columnList.add(columnName );
columnList.add(columnType );

 // Build column structure
 ArrayNode columns = ColumnUtil.buildColumnListStructure(columnList);

 replaceColumnsAction.setTarget("GrdDivididendList");

 // Add parameters to action
 replaceColumnsAction.addParameter("columns", new CellData(columns));

 // Add action to list
 clientActionList.add(replaceColumnsAction);

 return serviceData;
}
```

**update-cell**

```java
...
 // Variable initialization
 ServiceData serviceData = new ServiceData();

 ClientAction updateCell = new ClientAction("update-cell");

 // Set values
 ObjectNode values = JsonNodeFactory.instance.objectNode();
 values.put("value", value);
 values.put("label", label);
 values.put("style", "updated");

 // Build address of cell
 ComponentAddress address = new ComponentAddress(applicationName, null, "report", null, "GrdSwapLst", idRow, idColumn);
 updateCell.setAddress(address);
 updateCell.setAsync("true");
 updateCell.setSilent("true");
 updateCell.addParameter("data", new CellData(values));
 
 // Add action to list
 clientActionList.add(updateCell);

 return serviceData;
 
```

> **Note:** If you want to change only a value, only passing the value to the `data` parameter inside a `CellData` is necessary. Si desea cambiar algo más, entonces será necesario generar un objeto JSON con el modelo que desea actualizar. In this model you must include the `value` and `label` attributes.

### Acciones de gráficas desde servicios java {#chart-actions-from-java-services}

| Action                 | Descripción                       | Parámetros                             |
| ---------------------- | --------------------------------- | -------------------------------------- |
| `replace-chart-series` | Reemplazar la serie de un gráfico | `series` - ArrayNode with chart series |
| `add-chart-series`     | Añadir una serie al gráfico       | `series` - ArrayNode with chart serie  |
| `remove-chart-series`  | Eliminar la serie de un gráfico   | `series` - ArrayNode with chart series |

**replace-chart-series**

```java
/**
   * Test method to replace the series of chart
   *
   * @param userList (Dummy list for build name of series)
   * @return
   * @throws AWException
   */
  public ServiceData replaceSeriesChart(List<String> userList) throws AWException {

    // Init variables
    ServiceData serviceData = new ServiceData();
    List<ClientAction> clientActionList = serviceData.getClientActionList();
    JsonNodeFactory factory = JsonNodeFactory.instance;
    ArrayNode serieList = factory.arrayNode();

    // Get month list for xAxis
    List<String> months = builDummyMonthList();

    // Add json data of series
    for (String user : userList) {
      // New serie
      ChartSerie serie = new ChartSerie();
      serie.setId(user);
      serie.setName(user);

      // Add data to serie
      for (String month : months) {
        ChartSeriePoint point = new ChartSeriePoint(factory.textNode(month), factory.numberNode(new Random().nextInt((10 - 0) + 1) + 0));
        // Add point to serie [x,y]
        serie.getData().add(point);
      }
      // Add serie
      serieList.add(serie.toJson());
    }

    // Create action replace series of chart
    ClientAction replaceSeriesAction = new ClientAction("replace-chart-series");
    replaceSeriesAction.setTarget("ChrLinTst");
    ArrayList<Parameter> parameterList = replaceSeriesAction.getParameterList();
    parameterList.add(new Parameter("series", new CellData(serieList)));
    // Add action to list
    clientActionList.add(replaceSeriesAction);

    return serviceData;
  }
```

**add-chart-series**

```java
/**
   * Test method to add the series of chart
   *
   * @param userList (Dummy list for build name of series)
   * @return
   * @throws AWException
   */
  public ServiceData addSeriesChart(List<String> userList) throws AWException {

    // Init variables
    ServiceData serviceData = new ServiceData();
    List<ClientAction> clientActionList = serviceData.getClientActionList();
    JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
    // Get month list for xAxis
    List<String> months = builDummyMonthList();

    // Create arrayNode of chart series
    ArrayNode serieList = jsonFactory.arrayNode();

    // Add json data of series
    for (String user : userList) {
      // New serie
      ChartSerie serie = new ChartSerie();
      serie.setId(user);
      serie.setName(user);

      // Add data to serie
      for (String month : months) {
        ChartSeriePoint point = new ChartSeriePoint(jsonFactory.textNode(month), jsonFactory.numberNode(new Random().nextInt((10 - 0) + 1) + 0));
        // Add point to serie [x,y]
        serie.getData().add(point);
      }
      // Add serie
      serieList.add(serie.toJson());
    }

    // Create action add series of chart
    ClientAction addSeriesAction = new ClientAction("add-chart-series");
    addSeriesAction.setTarget("ChrLinTst");
    ArrayList<Parameter> parameterList = addSeriesAction.getParameterList();
    parameterList.add(new Parameter("series", new CellData(serieList)));
    // Add action to list
    clientActionList.add(addSeriesAction);

    return serviceData;
  }
```

**remove-chart-series**

```java
/**
   * Test method to remove the series of chart
   *
   * @param userList (Dummy list for build name of series)
   * @return
   * @throws AWException
   */
  public ServiceData removeSeriesChart(List<String> userList) throws AWException {

    // Init variables
    ServiceData serviceData = new ServiceData();
    List<ClientAction> clientActionList = serviceData.getClientActionList();
    JsonNodeFactory jsonFactory = JsonNodeFactory.instance;

    // Create arrayNode of chart series
    ArrayNode series = jsonFactory.arrayNode();

    // Add json data of series
    for (String user : userList) {
      ChartSerie serie = new ChartSerie();
      serie.setId(user);
      // Add serie
      series.add(serie.toJson());
    }

    // Create action remove series of chart
    ClientAction removeSeriesAction = new ClientAction("remove-chart-series");
    removeSeriesAction.setTarget("ChrLinTst");
    ArrayList<Parameter> parameterList = removeSeriesAction.getParameterList();
    parameterList.add(new Parameter("series", new CellData(series)));
    // Add action to list
    clientActionList.add(removeSeriesAction);

    return serviceData;
  }
```

## Pila de acciones {#actions-stack}

Las acciones se lanzan secuencialmente. Se almacenan en una pila de acciones, y cuando la acción actual ha terminado, se lanza la siguiente.

There are some actions that can _clean_ the actions stack, like the `cancel` client action. Other actions freeze the current stack and creates another stack in an upper level, like the `dialog` client action.

<img alt="sync-stack" src={require('@docusaurus/useBaseUrl').default('img/sync-stack.png')}/>

### Pila asíncrona {#async-stack}

When you launch an action with the `async` flag activated, it is launched on the **async stack**. Se ejecuta de forma asincrónica, y no espera ninguna otra acción.

<img alt="async-stack" src={require('@docusaurus/useBaseUrl').default('img/async-stack.png')}/>

### Mostrar la pila {#show-the-stack}

If you want to see what the stacks are doing, you can add a delay to these actions and make them visible by typing the next keys **within the application focus**:

```
ALT + SHIFT + [number]
```

Where `[number]` is the number of seconds you want to delay every action (for example, 1);

To disable the action stack, just do the same thing delaying **0** seconds:

```
ALT + SHIFT + 0
```

## Ejemplos {#examples}

### Validación {#validation}

Validar todos los criterios, lanzar un mensaje de confirmación y después lanzar un target de mantenimiento para almacenar los datos de la pantalla:

```xml
<button-action type="validate" />
<button-action type="confirm" target="[target]" />
<button-action type="server" server-action="maintain" target-action="[maintain-target]" />
```

### Navegación {#navigation-1}

Ir a la pantalla anterior, advirtiendo al usuario que los datos de la pantalla pueden perderse

```xml
<button-action type="confirm-updated-data" />
<button-action type="back" />
```

### Descargar un archivo {#download-a-file-stored-somewhere}

<img alt="TextView" src={require('@docusaurus/useBaseUrl').default('img/TextView.png')}/>

**Screen**

```xml
<criteria label="PARAMETER_TEXT" id="TxtViw" variable="ButVal" component="text-view" style="col-xs-6 col-sm-3 col-lg-2" icon="download">
  <dependency>
    <dependency-element id="TxtViw"/>
    <dependency-element id="TxtViw" event="click"/>
    <dependency-action type="server-download" server-action="get-file-maintain" target-action="downloadFile"/>
  </dependency>
  ...
</criteria>
```

**Maintain.xml**

```xml
<target name="downloadFile">
  <serve service="downloadFile">
    <variable id="fileName" type="STRING" name="TxtViw" />
  </serve>
</target>
```

**Service.xml**

```xml
<service id="downloadFile">
  <java classname="com.almis.awe.test.File" method="downloadFile">
    <service-parameter type="STRING" name="fileName" />
  </java>
</service>
```

**Java service**

```java
/**
 * Given a file identifier, download a file
 *
 * @param fileIdentifier
 * @return ServiceData
 * @throws AWException
 */
public ServiceData downloadFile(String fileIdentifier) throws AWException {
  ServiceData serviceData = new ServiceData();
  FileController fileController = new FileController();
  FileData fileData = fileController.getFileData(fileIdentifier);

  String path = fileData.getFilePath();
  FileInputStream file = FileUtil.getFileStream(path);

  // Set variables
  fileData.setFileStream(file);
  serviceData.setData(fileData);
  return serviceData;
}
```