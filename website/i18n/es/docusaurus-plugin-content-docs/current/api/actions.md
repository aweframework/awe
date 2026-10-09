---
id: actions
title: Actions
---

Para realizar una acción cuando hacemos clic en un botón o una dependencia está activada necesitamos definir una `button-action` o una `dependency-action`. El formato de estas acciones es el siguiente:

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

| Atributo        |       Uso       |  Tipo   | Descripción                                                                      | Valores                                                                       |
| --------------- |:---------------:|:-------:| -------------------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| `type`          | **Obligatorio** | String  | Acción que se lanzará.                                                           | Ver lista de [acciones del cliente](#client-actions)                     |
| `target`        |    Opcional     | String  | Objetivo de la acción                                                            | Opción, mensaje o identificador de componente, dependiendo del tipo de acción |
| `context`       |    Opcional     | String  | Contexto donde está el objetivo                                                  | Vista en la que se encuentra el componente                                    |
| `server-action` |    Opcional     | String  | Llamada de acción del servidor                                                   | Ver lista de [acciones del servidor](#server-actions)                  |
| `target-action` |    Opcional     | String  | Identificador del objetivo en el servidor                                        |                                                                               |
| `silent`        |    Opcional     | Boolean | Iniciar la acción sin mostrar la barra de carga                                  |                                                                               |
| `async`         |    Opcional     | Boolean | Ejecuta la acción en la pila [asíncrona](#async-stack)                           |                                                                               |
| `value`         |    Opcional     | String  | Valor a establecer para el criterio en caso de que la acción sea de tipo "value" |                                                                               |

## Acciones del cliente {#client-actions}

Las acciones de los clientes son procesos iniciados en el lado del cliente de la aplicación web (en el navegador). Se ejecutan secuencialmente (a menos que se definan como [asíncronas](#async-stack)). Dependiendo del tipo de acción, los atributos `target` y `context` pueden diferir en su significado.

### Comunes {#general}

Acciones genéricas que se lanzan en el navegador. Estas acciones no necesitan atributos extra y se aplican a todos los componentes de la pantalla.

#### Acciones generales {#general-actions}

| Action                     | Descripción                                                                                                                                          |
| -------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| `reset`                    | Vaciar todos los criterios en la pantalla                                                                                                            |
| `restore`                  | Restaurar los valores iniciales de todos los criterios en la pantalla                                                                                |
| `restore-target`           | Restaurar los valores de los criterios de la pantalla que estén dentro de una etiqueta con un identificador específico (definido en `target`)        |
| `validate`                 | Iniciar una validación de todos los criterios de la pantalla                                                                                         |
| `print`                    | Lanza una impresión de la pantalla. Es igual que la opción de imprimir del navegador                                                                 |
| `confirm-updated-data`     | Muestra un diálogo de confirmación para advertir al usuario que hay criterios con datos en la pantalla, y puede perderlos al navegar a otra pantalla |
| `confirm-not-updated-data` | Muestra un diálogo de confirmación para advertir al usuario que los criterios en la pantalla no han sido actualizados                                |
| `confirm-empty-data`       | Muestra un diálogo de confirmación para advertir al usuario que todos los criterios en la pantalla están vacíos                                      |
| `resize`                   | Fuerza un chequeo de tamaño de pantalla (útil en caso de que no se actualicen bien los tamaños de los componentes)                                   |
| `toggle-menu`              | Mostrar / Ocultar el menú                                                                                                                            |
| `toggle-navbar`            | Mostrar / Ocultar la barra de navegación                                                                                                             |
| `disable-dependencies`     | Desactivar el sistema de [dependencias](dependencies.md)                                                                                             |
| `enable-dependencies`      | Activar el sistema de [dependencias](dependencies.md)                                                                                                |
| `cancel`                   | Limpia la pila de acciones actual                                                                                                                    |
| `value`                    | Establece un valor a un criterio                                                                                                                     |
| `wait`                     | Espera una cantidad de milisegundos definidos en el atributo `target`                                                                                |
| `close-window`             | Intenta cerrar la ventana actual del navegador (a veces pedirá permiso al usuario)                                                                   |
| `update-theme`             | Reload the theme stylesheet to retrieve theme changes without reloading the screen                                                                   |
| `get-file`                 | Download a file from the server. The file to retrieve is identified by the `filename` parameter                                                      |
| `change-menu`              | Replace the current menu options with the options received on the `options` parameter                                                                |

### Mensaje {#message}

Acciones que eventualmente pueden mostrar un mensaje al usuario.

#### Acciones de mensaje {#message-actions}

| Action    | Descripción                                                                        |
| --------- | ---------------------------------------------------------------------------------- |
| `confirm` | Muestra un diálogo de confirmación con el mensaje definido en el atributo `target` |


#### Atributos del mensaje {#message-attributes}

| Atributo |       Uso       |  Tipo  | Descripción              | Valores |
| -------- |:---------------:|:------:| ------------------------ | ------- |
| `target` | **Obligatorio** | String | Identificador de mensaje |         |

### Navegación {#navigation}

Las acciones de navegación se utilizan para pasar de una pantalla a otra.

#### Acciones de navegación {#navigation-actions}

| Action            | Descripción                                                                                                                                                                                                                                                                                                                               |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `back`            | Ir a la pantalla anterior. Igual que el botón atrás del navegador. **No** necesita el atributo `target`.                                                                                                                                                                                                                                  |
| `screen`          | Ir a una pantalla. **Necesita** el atributo `target`.                                                                                                                                                                                                                                                                                     |
| `reload`          | Recargar pantalla actual                                                                                                                                                                                                                                                                                                                  |
| `reload-page`     | Fully reload the browser page, like a manual refresh. Unlike `reload` (which re-runs the current screen inside the SPA), it re-fetches everything from the server, so it also picks up structural changes. Used by the development [hot reload](../dev-tools) to refresh the browser automatically. **No** necesita el atributo `target`. |
| `logout`          | Cerrar sesión y salir del menú privado.  **No** necesita el atributo `target`.                                                                                                                                                                                                                                                            |
| `redirect`        | Redirects the current screen to a new URL defined in `target` attribute. If parameter `newWindow` is set to `true`, the URL will be open in a new window                                                                                                                                                                                  |
| `redirect-screen` | Launches a `redirect` action if the current screen matches the `screen` parameter                                                                                                                                                                                                                                                         |

#### Atributos de navegación {#navigation-attributes}

| Atributo    |          Uso           |  Tipo   | Descripción                                                                                         | Valores                                                                      |
| ----------- |:----------------------:|:-------:| --------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| `target`    | *Depende* de la acción | String  | Identificador de opción                                                                             |                                                                              |
| `context`   |        Opcional        | String  | Contexto de la pantalla. Si no está definido, el contexto es el mismo que la pantalla del lanzador. | `screen/public` para opciones públicas, `screen/home` para opciones privadas |
| `newWindow` |        Opcional        | Boolean | Open the redirect url in a new window.                                                              |                                                                              |
| `screen`    | *Depende* de la acción | String  | Screen to check when launching the `redirect-screen` action.                                        |                                                                              |

### Componente {#component}

Acciones que funcionan sobre componentes en la pantalla.

#### Acciones del componente {#component-actions}

| Action                           | Descripción                                                                                              | Funciona en                 |
| -------------------------------- | -------------------------------------------------------------------------------------------------------- | --------------------------- |
| `add-class`                      | Agrega la clase css definida en `target-action` a la etiqueta con el selector definido en `target`       | `tag`                       |
| `remove-class`                   | Elimina las clases definidas en `target-action` a la etiqueta con el selector definido en `target`       | `tag`                       |
| `toggle-class`                   | Toggle the css class/classes defined on `target-action` to the tag with the selector defined on `target` | `tag`                       |
| `reset`                          | Vaciar un valor de criterio                                                                              | `criteria`, `grid`, `chart` |
| `restore`                        | Restaurar a los valores iniciales un criterio                                                            | `criteria`, `grid`, `chart` |
| `start-load`                     | Establece un componente como *cargando*                                                                  | `criteria`, `grid`, `chart` |
| `copy-criterion-value-clipboard` | Copy a criterion value to the clipboard                                                                  | `criteria`                  |
| `validate`                       | Inicia una validación sobre el criterio o criterios dentro de la etiqueta `target`                       | `tag`, `criteria`           |
| `set-valid`                      | Set a criterion as valid, clearing its previous validation error                                         | `criteria`                  |
| `set-invalid`                    | Set a criterion as invalid, showing the message defined on the `message` parameter                       | `criteria`                  |
| `fill-suggest`                   | Fill the available and selected values of a suggest criterion with the `values` parameter                | `criteria`                  |
| `update-controller`              | Update the controller attribute defined on the `attribute` parameter with the value received             | Any component               |
| `dialog`                         | Abre un diálogo modal                                                                                    | `dialog`                    |
| `close`                          | Cierra un diálogo                                                                                        | `dialog`                    |
| `close-cancel`                   | Closes a dialog cancelling the actions stack which opened it                                             | `dialog`                    |
| `filter`                         | Recargar una matriz                                                                                      | `grid`                      |
| `add-row`                        | Añadir una fila vacía en la parte inferior de la matriz                                                  | `grid`                      |
| `add-row-top`                    | Añadir una fila vacía en la parte superior de la matriz                                                  | `grid`                      |
| `add-row-up`                     | Añadir una fila vacía sobre la fila seleccionada                                                         | `grid`                      |
| `add-row-down`                   | Añadir una fila vacía debajo de la fila seleccionada                                                     | `grid`                      |
| `copy-row`                       | Copia la fila seleccionada en la parte inferior de la matriz                                             | `grid`                      |
| `copy-row-top`                   | Copia la fila seleccionada en la parte superior de la matriz                                             | `grid`                      |
| `copy-row-up`                    | Copiar la fila seleccionada sobre la fila seleccionada                                                   | `grid`                      |
| `copy-row-down`                  | Copiar la fila seleccionada debajo de la fila seleccionada                                               | `grid`                      |
| `delete-row`                     | Eliminar la fila seleccionada                                                                            | `grid`                      |
| `save-row`                       | Guardar la fila seleccionada                                                                             | `grid`                      |
| `cancelar fila`                  | Cancel the edition of the selected row                                                                   | `grid` (editable)           |
| `check-one-selected`             | Comprueba si hay una fila seleccionada                                                                   | `grid`                      |
| `check-some-selected`            | Comprueba si hay una o más filas seleccionadas                                                           | `grid`                      |
| `check-records-saved`            | Comprueba si todos los registros están almacenados (el usuario no está editando una fila)                | `grid`                      |
| `check-records-generated`        | Comprueba si hay al menos una fila en la matriz                                                          | `grid`                      |
| `select-first-row`               | Selecciona la primera fila de la matriz                                                                  | `grid`                      |
| `select-last-row`                | Selecciona la última fila de la matriz                                                                   | `grid`                      |
| `select-all-rows`                | Seleccionar todas las filas de la matriz                                                                 | `grid`                      |
| `unselect-all-rows`              | Deseleccionar todas las filas de la tabla                                                                | `grid`                      |
| `validate-selected-row`          | Iniciar una validación en la fila seleccionada de la matriz                                              | `grid`                      |
| `copy-selected-rows-clipboard`   | Copy the selected rows on the grid to the clipboard                                                      | `grid`                      |
| `show-columns`                   | Show the grid columns defined on the `columns` parameter                                                 | `grid`                      |
| `hide-columns`                   | Hide the grid columns defined on the `columns` parameter                                                 | `grid`                      |
| `tree-branch`                    | Add the rows received on the `datalist` parameter as children of the expanding branch                    | `grid` (tree)               |
| `change-theme`                   | Cambia el tema al valor definido en el criterio definido en `target`                                     | `criteria`                  |
| `change-language`                | Cambia el idioma al valor definido en el criterio definido en `target`                                   | `criteria`                  |
| `reload-language`                | Recargar el idioma buscando cambios                                                                      | `criteria`                  |
| `next-step`                      | Mover al siguiente paso del asistente                                                                    | `wizard`                    |
| `prev-step`                      | Mover al paso anterior del asistente                                                                     | `wizard`                    |
| `first-step`                     | Mover al primer paso del asistente                                                                       | `wizard`                    |
| `last-step`                      | Mover al último paso del asistente                                                                       | `wizard`                    |
| `nth-step`                       | Mover al paso n del asistente                                                                            | `wizard`                    |
| `add-points`                     | Add the points received on the `data` parameter to the chart series                                      | `chart`                     |
| `set-pivot-sorters`              | Set the field sort order of a pivot table with the `sorters` parameter                                   | `pivot`                     |
| `set-pivot-group-rows`           | Set the fields used as row groups, defined as a comma-separated list on the `rows` parameter             | `pivot`                     |
| `set-pivot-group-cols`           | Set the fields used as column groups, defined as a comma-separated list on the `cols` parameter          | `pivot`                     |
| `clear-file`                     | Clear the uploaded file, resetting the uploader component                                                | `cargador`                  |
| `taglist-data`                   | Load the content of a taglist with the HTML and component data received from the server                  | `taglist`                   |
| `polyline`                       | Draw a polyline on a map with the points received on the `rows` parameter (`Lat` and `Lon` fields)       | `map`                       |


#### Atributos del componente {#component-attributes}

| Atributo  |       Uso       |  Tipo  | Descripción                                                                                   | Valores                                                                                 |
| --------- |:---------------:|:------:| --------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| `target`  | **Obligatorio** | String | Identificador de componente o etiqueta                                                        |                                                                                         |
| `context` |    Opcional     | String | Contexto del componente. Si no se define, el contexto es el mismo que el componente lanzador. | `base` para la pantalla del contenedor de menú, `report@home` para las opciones de menú |

### Llamada al servidor {#server-call}

#### Acciones de llamada al servidor {#server-call-actions}

| Action            | Descripción                                                                                                         |
| ----------------- | ------------------------------------------------------------------------------------------------------------------- |
| `server`          | Lanzar una llamada de servidor con los parámetros de la pantalla                                                    |
| `server-print`    | Iniciar una llamada al servidor con los parámetros de la pantalla e información adicional de impresión              |
| `server-download` | Lanzar una llamada de servidor con los parámetros de la pantalla para llamar a una acción que descargará un archivo |

#### Atributos de llamada del servidor {#server-call-attributes}

| Atributo        |       Uso       |  Tipo  | Descripción                                                                                                     | Valores                                                                                                     |
| --------------- |:---------------:|:------:| --------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| `target`        |    Opcional     | String | Componente o identificador de mensaje para la respuesta                                                         |                                                                                                             |
| `context`       |    Opcional     | String | Contexto del componente para la respuesta. Si no se define, el contexto es el mismo que el componente lanzador. | `base` para la pantalla del contenedor de menú, `report@home` para las opciones de menú                     |
| `server-action` | **Obligatorio** | String | Acción para lanzar en el servidor.                                                                              | Ver lista de [acciones del servidor](#server-actions)                                                |
| `target-action` | **Obligatorio** | String | Objetivo para la acción del servidor                                                                            | [Identificador de consulta](query-definition.md) o [Identificador de mantenimiento](maintain-definition.md) |

### Internal actions

These actions are dispatched by the framework itself as part of the screen and component lifecycle. They are documented here for reference, and are not normally used in application XML.

#### Internal action list

| Action                 | Descripción                                                                                   |
| ---------------------- | --------------------------------------------------------------------------------------------- |
| `screen-data`          | Store the screen configuration received from the server and launch the actions attached to it |
| `end-load`             | Notify a component that its data load has finished                                            |
| `end-dependency`       | Notify the dependency controller that a dependency has finished                               |
| `file-status`          | Update the upload progress of an uploader component                                           |
| `file-uploaded`        | Store the uploaded file data in the uploader model when an upload finishes                    |
| `file-downloaded`      | Notify a downloader component that the file download has finished                             |
| `locals-retrieved`     | Store the translations retrieved from the server for a language                               |
| `log-delta`            | Append new log lines to a log viewer component and scroll it down                             |
| `pos-añadir-fila`      | Launched after a row has been added to a grid                                                 |
| `pos-eliminar-fila`    | Launched after a row has been deleted from a grid                                             |
| `después-guardar-fila` | Launched after a row has been saved on an editable grid                                       |
| `pos-cancelar-fila`    | Launched after the edition of a row has been cancelled on an editable grid                    |

## Acciones del servidor {#server-actions}

| Action                    | Descripción                                                                                                                                                                  | Valores                                                                |
| ------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| `login`                   | Iniciar sesión en la aplicación                                                                                                                                              | Usuario y contraseña                                                   |
| `logout`                  |                                                                                                                                                                              |                                                                        |
| `screen`                  | Navegar a una pantalla                                                                                                                                                       | `target`: Identificador de opción de menú                              |
| `data`                    | Obtener datos del servidor. Rellena la lista de `values` del criterio                                                                                                        | `targetAction`: Identificador de consulta                              |
| `value`                   | Obtener un valor del servidor. Rellena la lista de `selected` del criterio. *En los suggest, esta acción también rellena la lista de `values`*                               | `targetAction`: Identificador de consulta                              |
| `update-model`            | Obtener valores para algunos criterios. El alias del campo de consulta **debe** ser coincidir con el identificador del criterio. Rellena la lista de `selected` del criterio | `targetAction`: Identificador de consulta                              |
| `subscribe`               | Suscribirse a una recuperación de datos de consulta. La suscripción almacena la solicitud del componente y transmite datos a la dirección del componente                     | `targetAction`: Identificador de consulta                              |
| `maintain`                | Lanzar un proceso de mantenimiento                                                                                                                                           | `targetAction`: Maintain target identifier                             |
| `maintain-silent`         | Lanzar un proceso de mantenimiento sin mensaje de respuesta                                                                                                                  | `targetAction`: Maintain target identifier                             |
| `maintain-async`          | Lanzar un proceso de mantenimiento sin ninguna respuesta                                                                                                                     | `targetAction`: Maintain target identifier                             |
| `validate`                | Ejecutar una consulta, y si devuelve una advertencia o error, invalida el componente lanzador con un mensaje                                                                 | `targetAction`: Identificador de consulta                              |
| `unique`                  | Ejecutar una consulta, y si devuelve datos, no valida el lanzador                                                                                                            | `targetAction`: Identificador de consulta                              |
| `control`                 | Lanzar una consulta, y si no termina bien, devuelve un mensaje                                                                                                               | `targetAction`: Identificador de consulta                              |
| `control-cancel`          | Lanzar una consulta, y si no termina bien, devuelve un mensaje y cancela la cola de acciones                                                                                 | `targetAction`: Identificador de consulta                              |
| `control-confirm`         | Lanzar una consulta, y si no termina bien, devuelve un mensaje de confirmación                                                                                               | `targetAction`: Query identifier, `target`: Confirm message identifier |
| `control-confirm-cancel`  | Lanzar una consulta, y si termina bien, cancela la cola y si termina con un error, devuelve un mensaje de confirmación                                                       | `targetAction`: Query identifier, `target`: Confirm message identifier |
| `control-confirm-message` | Lanzar una consulta, y si no termina bien, devuelve un mensaje de confirmación con la salida del mensaje de consulta                                                         | `targetAction`: Identificador de consulta                              |
| `control-empty-cancel`    | Lanzar una consulta, y si no devuelve datos, muestra un mensaje y cancela la cola                                                                                            | `targetAction`: Query identifier, `target`: Message identifier         |
| `control-unique-cancel`   | Lanzar una consulta, y si devuelve datos, muestra un mensaje y cancela la cola                                                                                               | `targetAction`: Query identifier, `target`: Message identifier         |
| `app-help`                | Muestra la ayuda de pantalla generada automáticamente basándose en los atributos de la pantalla                                                                              | `target`: Identificador de pantalla (opcional)                         |
| `ayuda`                   | Obtener el libro de ayuda de la aplicación en el idioma actual                                                                                                               |                                                                        |
| `get-file-maintain`       | Lanzar un proceso de mantenimiento y recuperar un archivo para descargar                                                                                                     |                                                                        |

## Acciones del cliente de servicios Java {#client-actions-from-java-services}

Este tipo de acciones se utilizan para ejecutar las acciones del cliente desde los servicios java en el servidor. Son útiles para realizar muchas acciones en el elemento de la ventana con un único servicio. Por ejemplo, rellene criterios y matrices a la vez, añada dinámicamente columnas a una matriz, agregue o reemplace series en las gráficas, etc.

### Acciones generales de servicios java {#general-actions-from-java-services}

| Acción            | Descripción                                                                               | Parámetros                                                                                                                    |
| ----------------- | ----------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| `message`         | Enviar un mensaje al cliente (sin target)                                                 | `type` - Tipo de mensaje (`ok`, `info`, `warning`, `error`), `title` - Título del mensaje, `message`- Descripción del mensaje |
| `target-message`  | Enviar un mensaje a un componente cliente                                                 | `type` - Tipo de mensaje (`ok`, `info`, `warning`, `error`), `title` - Título del mensaje, `message`- Descripción del mensaje |
| `screen`          | Navigate to a screen option. The option is defined as the action `target`                 | `reload` - Force the screen reload (optional)                                                                                 |
| `redirect`        | Redirect the browser to the URL defined as the action `target`                            | `newWindow` - Open the URL in a new window (optional)                                                                         |
| `redirect-screen` | Launch a `redirect` action only if the current screen matches the `screen` parameter      | `screen` - Screen name to check against the current screen                                                                    |
| `dialog`          | Open a modal dialog. The dialog identifier is defined as the action `target`              |                                                                                                                               |
| `confirm`         | Launch a confirm dialog                                                                   | `title` - Confirm title, `message` - Confirm description                                                                      |
| `get-file`        | Download a file from the server                                                           | `filename` - Serialized file data of the file to download                                                                     |
| `add-class`       | Add css classes to the elements matching the selector defined as the action `target`      | `targetAction` - Space-separated css class list                                                                               |
| `remove-class`    | Remove css classes from the elements matching the selector defined as the action `target` | `targetAction` - Space-separated css class list                                                                               |
| `toggle-class`    | Toggle css classes on the elements matching the selector defined as the action `target`   | `targetAction` - Space-separated css class list                                                                               |

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

**screen**

```java
public ServiceData goToScreen() {
  return new ServiceData()
    .addClientAction(new ScreenActionBuilder("MyOption").build());
}
```

**redirect**

```java
public ServiceData redirectToUrl() {
  // Open the URL in a new window
  return new ServiceData()
    .addClientAction(new RedirectActionBuilder("https://www.example.com", true).build());
}
```

**redirect-screen**

```java
public ServiceData redirectFromScreen() {
  // Redirect only if the current screen is "currentScreen"
  return new ServiceData()
    .addClientAction(new RedirectScreenActionBuilder("currentScreen", "https://www.example.com").build());
}
```

**dialog**

```java
public ServiceData openDialog() {
  return new ServiceData()
    .addClientAction(new DialogActionBuilder("MyDialog").build());
}
```

**confirm**

```java
public ServiceData confirmChanges() {
  return new ServiceData()
    .addClientAction(new ConfirmActionBuilder("CONFIRM_TITLE", "CONFIRM_MESSAGE").build());
}
```

**get-file**

```java
public ServiceData downloadFile(FileData fileData) {
  return new ServiceData()
    .addClientAction(new DownloadActionBuilder(fileData).build());
}
```

**add-class**

```java
public ServiceData highlightPanel() {
  return new ServiceData()
    .addClientAction(new AddCssClassActionBuilder(".panel-header", "highlight").build());
}
```

**remove-class**

```java
public ServiceData removeHighlight() {
  return new ServiceData()
    .addClientAction(new RemoveCssClassActionBuilder(".panel-header", "highlight").build());
}
```

**toggle-class**

```java
public ServiceData togglePanel() {
  return new ServiceData()
    .addClientAction(new ToggleCssClassActionBuilder(".panel-header", "expanded").build());
}
```

### Acciones de criterios de servicios java {#criteria-actions-from-java-services}

| Acción              | Descripción                                                                  | Parámetros                                                                                                                            |
| ------------------- | ---------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| `seleccionar`       | Rellenar los valores seleccionados en un criterio                            | `values` - Lista de datos con los valores de los criterios                                                                            |
| `fill`              | Rellenar la lista de los valores disponibles para seleccionar en un criterio | `row` - filas de la lista (al menos deben tener los campos `value` y `label`)                                                         |
| `fill-suggest`      | Fill the available and selected values of a suggest criterion                | `values` - List of suggest values (with `value` and `label` fields)                                                                   |
| `update-controller` | Update an attribute of a component controller                                | `attribute` - Controller attribute to update, `value` - Value to set (or `datalist` - DataList whose first row `value` field is used) |

**seleccionar**

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

**fill-suggest**

```java
public ServiceData fillSuggestValues() {
  return new ServiceData()
    .addClientAction(new FillSuggestActionBuilder("SuggestCriterion",
      new SuggestValue("TR001", "Transaction 001"),
      new SuggestValue("TR002", "Transaction 002"))
      .build());
}
```

**update-controller**

```java
public ServiceData updateButtonLabel() {
  // Update the "label" attribute of the "ButPrn" component controller
  return new ServiceData()
    .addClientAction(new UpdateControllerActionBuilder("ButPrn", "label", "BUTTON_PRINT_ALL").build());
}
```

### Acciones de matrices desde servicios de java {#grid-actions-from-java-services}

| Acción              | Descripción                                                                                                                    | Parámetros                                                                                                                                                                |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `fill`              | Rellena una tabla con una lista de datos                                                                                       | `rows` - filas de la lista de datos                                                                                                                                       |
|                     |                                                                                                                                | `total` - Número total de páginas de la lista de datos                                                                                                                    |
|                     |                                                                                                                                | `page` - Página de la lista de datos                                                                                                                                      |
|                     |                                                                                                                                | `records` - Número de registros de la lista de datos                                                                                                                      |
| `add-columns`       | Añadir columnas a la estructura de la matriz                                                                                   | `columns` - ArrayNode con estructura de columna                                                                                                                           |
| `replace-columns`   | Reemplazar las columnas de la matriz con otra estructura de columnas                                                           | `columns` - ArrayNode con estructura de columna                                                                                                                           |
| `update-cell`       | Actualizar una celda en una matriz. Utilizado para actualizar una celda de una tabla con otro valor, estilo, icono, título ... | `values` - ObjectNode con atributos de celda                                                                                                                              |
| `add-row`           | Añadir una fila en la parte inferior de la matriz                                                                              | `row` - valores de la fila (opcional)                                                                                                                                     |
|                     |                                                                                                                                | `selectedRow` - Identificador de fila seleccionada (opcional)                                                                                                             |
| `add-row-top`       | Añadir una fila en la parte superior de la matriz                                                                              | `row` - valores de fila (opcional)                                                                                                                                        |
|                     |                                                                                                                                | `selectedRow` - Identificador de fila seleccionada (opcional)                                                                                                             |
| `add-row-up`        | Añadir una fila sobre la fila seleccionada                                                                                     | `row` - valores de fila (opcional)                                                                                                                                        |
|                     |                                                                                                                                | `selectedRow` - Identificador de fila seleccionada (opcional)                                                                                                             |
| `add-row-down`      | Añadir una fila debajo de la fila seleccionada                                                                                 | `row` - valores de la fila (opcional)                                                                                                                                     |
|                     |                                                                                                                                | `selectedRow` - Identificador de fila seleccionada (opcional)                                                                                                             |
| `copy-row`          | Copia la fila seleccionada en la parte inferior de la tabla                                                                    | `selectedRow` - Identificador de fila seleccionada (opcional)                                                                                                             |
| `copy-row-top`      | Copia la fila seleccionada en la parte superior de la tabla                                                                    | `selectedRow` - Identificador de fila seleccionada (opcional)                                                                                                             |
| `copy-row-up`       | Copiar la fila seleccionada en la fila anterior                                                                                | `selectedRow` - Identificador de fila seleccionada (opcional)                                                                                                             |
| `copy-row-down`     | Copiar la fila seleccionada debajo de la fila seleccionada                                                                     | `selectedRow` - Identificador de fila seleccionada (opcional)                                                                                                             |
| `update-row`        | Actualizar los valores de fila seleccionados                                                                                   | `row` - valores de la fila, `rowId` - id de fila a actualizar (si no se da ninguna, se cogerá la fila seleccionada), `style` - clase CSS para añadir a la fila (opcional) |
| `delete-row`        | Elimina la fila seleccionada                                                                                                   |                                                                                                                                                                           |
| `filter`            | Reload the grid data                                                                                                           |                                                                                                                                                                           |
| `show-columns`      | Show grid columns                                                                                                              | `columns` - List of column identifiers to show                                                                                                                            |
| `hide-columns`      | Hide grid columns                                                                                                              | `columns` - List of column identifiers to hide                                                                                                                            |
| `select-all-rows`   | Seleccionar todas las filas de la matriz                                                                                       |                                                                                                                                                                           |
| `unselect-all-rows` | Deseleccionar todas las filas de la tabla                                                                                      |                                                                                                                                                                           |

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

> **Nota:** Si desea cambiar solo un valor, solo es necesario pasar el valor al parámetro `data` dentro de un `CellData`. Si desea cambiar algo más, entonces será necesario generar un objeto JSON con el modelo que desea actualizar. En este modelo debe incluir los atributos `value` y `label`.

**filter**

```java
public ServiceData reloadGrid() {
  return new ServiceData()
    .addClientAction(new FilterActionBuilder("GrdLst").build());
}
```

**show-columns**

```java
public ServiceData showColumns() {
  return new ServiceData()
    .addClientAction(new ShowColumnsActionBuilder("GrdLst", "name", "type").build());
}
```

**hide-columns**

```java
public ServiceData hideColumns() {
  return new ServiceData()
    .addClientAction(new HideColumnsActionBuilder("GrdLst", "name", "type").build());
}
```

**select-all-rows**

```java
public ServiceData selectAllRows() {
  return new ServiceData()
    .addClientAction(new SelectAllRowsActionBuilder("GrdLst").build());
}
```

**unselect-all-rows**

```java
public ServiceData unselectAllRows() {
  return new ServiceData()
    .addClientAction(new UnselectAllRowsActionBuilder("GrdLst").build());
}
```

### Acciones de gráficas desde servicios java {#chart-actions-from-java-services}

| Action                 | Descripción                       | Parámetros                                             |
| ---------------------- | --------------------------------- | ------------------------------------------------------ |
| `replace-chart-series` | Reemplazar la serie de un gráfico | `series` - ArrayNode con series de gráficos            |
| `add-chart-series`     | Añadir una serie al gráfico       | `series` - ArrayNode con serie de gráficos             |
| `remove-chart-series`  | Eliminar la serie de un gráfico   | `series` - ArrayNode con series de gráficos            |
| `add-points`           | Add points to the chart series    | `data` - DataList with the points to add to each serie |

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

**add-points**

```java
public ServiceData addPointsToChart() {
  // Build the points to add: each row holds the value for each serie
  DataList dataList = new DataList();
  DataListUtil.addColumn(dataList, "date", Arrays.asList("2020-01"));
  DataListUtil.addColumn(dataList, "serie1", Arrays.asList(4));

  return new ServiceData()
    .addClientAction(new AddPointsActionBuilder("ChrLin", dataList).build());
}
```

## Pila de acciones {#actions-stack}

Las acciones se lanzan secuencialmente. Se almacenan en una pila de acciones, y cuando la acción actual ha terminado, se lanza la siguiente.

Hay algunas acciones que pueden *limpiar* la pila de acciones, como la acción de cliente `cancel`. Otras acciones congelan la pila actual y crean otra pila en un nivel superior, como la acción del cliente `dialog`.

<img alt="Pila de acciones síncrona" src={require('@docusaurus/useBaseUrl').default('img/sync-stack.png')} />

### Pila asíncrona {#async-stack}

Cuando se lanza una acción con la opción `async` activada, se lanza en la pila **asíncrona**. Se ejecuta de forma asincrónica, y no espera ninguna otra acción.

<img alt="Pila de acciones asíncrona" src={require('@docusaurus/useBaseUrl').default('img/async-stack.png')} />

### Mostrar la pila {#show-the-stack}

Si quieres ver lo que están haciendo las pilas, puedes añadir un retraso a estas acciones y hacerlas visibles tecleando **dentro del foco de la aplicación**:

```
ALT + SHIFT + [number]
```

Donde `[number]` es el número de segundos que desea retrasar cada acción (por ejemplo, 1);

Para desactivar la pila de acción, simplemente teclee lo mismo utilizando **0** segundos:

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

<img alt="TextView" src={require('@docusaurus/useBaseUrl').default('img/TextView.png')} />

**Pantalla**
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

**Servicio de Java**

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