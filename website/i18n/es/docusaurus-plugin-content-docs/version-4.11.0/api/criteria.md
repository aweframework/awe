---
id: criteria
title: Criteria
---

Los elementos de criterio son componentes de ventana que pueden obtener la entrada del usuario y enviarla al servidor de aplicaciones (lógica de negocio).

<img alt="Criteria" src={require('@docusaurus/useBaseUrl').default('img/Criteria.png')} />

## Cómo funcionan {#how-does-it-work}

Internamente, un criterio tiene 2 atributos básicos para administrar la información que se envía al servidor y la información que se muestra al usuario:

- **selected:** The selected values of the criterion. These values **will** be sent to the server.
- **values:** The available values of the criterion. These values **will _not_** be sent to the server. It's a list of _values_ and _labels_ that will be shown to the user to choose from.

### Prioridades {#priorities}

The **selected** values can be fulfilled in several ways. Esta es una lista de prioridades con estas maneras:

1. **variable** - These are the values sent from another screen
2. **screen target** - The values returned by the screen target are the most priority of the list
3. **`initial-load="value"`** - The query-retrieved values follow the screen target values
4. **session** - Session-stored values
5. **property** - Property-stored values
6. **value** - Static values defined on value attribute

## Esqueleto de XML {#xml-skeleton}

```xml
<criteria id="[identifier]" component="[component]" label="[label]" placeholder="[placeholder]" style="[classes]"
          initial-load="[initial-load]" server-action="[server-action]" target-action="[target-action]" 
          variable="[variable]" value="[value]" session="[session]" property="[property]"
          validation="[validation]" readonly="[read-only]" size="[size]" unit="[unit]" icon="[icon]"  
          printable="[printable]" help="[help]" help-image="[help-image]" 
          optional="[optional]" area-rows="[area-rows]" number-format="[number-format]" capitalize="[capitalize]"
          strict="[strict]" checked="[checked]" group="[group]" show-slider="[slider]" destination="[destination]">
  <dependency... />
</criteria>
```

## Estructura de criterios {#criteria-structure}

| Elemento                        | Uso          | Varias instancias | Descripción                                                                     |
| ------------------------------- | ------------ | ----------------- | ------------------------------------------------------------------------------- |
| [criteria](#general-attributes) | **Required** | No                | Nodo global de los criterios. Define los atributos del criterio |
| [dependency](dependencies.md)   | Opcional     | Si                | Lista de dependencias adjuntas al criterio                                      |

### Atributos generales {#general-attributes}

| Atributo             | Uso          | Tipo    | Descripción                                                                                                        | Valores                                                                                                                                                                                                                                                                                                                                               |
| -------------------- | ------------ | ------- | ------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| id                   | **Required** | String  | Identificador de criterio. Con fines de referencia                                                 |                                                                                                                                                                                                                                                                                                                                                       |
| component            | **Required** | String  | Tipo de criterio                                                                                                   | See [components](#components)                                                                                                                                                                                                                                                                                                                         |
| label                | Opcional     | String  | Texto del criterio (fuera del criterio)                                                         | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                                                                                                                                                                                                                                         |
| marcador de posición | Opcional     | String  | Texto del criterio (dentro del criterio)                                                        | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                                                                                                                                                                                                                                         |
| style                | Opcional     | String  | Clases CSS Criterion                                                                                               | See [Bootstrap positioning](http://getbootstrap.com/css/#grid-example-basic) for criteria sizing                                                                                                                                                                                                                                                      |
| carga-inicial        | Opcional     | String  | Llamada de acción al servidor para cargar los datos de criterio (se lanza al cargar la ventana) | `enum` (for [enumerated](enumerate-definition.md)), `query` (for [query call](query-definition.md) loading the **[values](#how-does-it-work)** part of the criterion) or `value` (for [query call](query-definition.md) loading the **[selected](#how-does-it-work)** part of the criterion) |
| server-action        | Opcional     | String  | Llamada de acción del servidor                                                                                     | See [server action list](actions.md#server-actions)                                                                                                                                                                                                                                                                                                   |
| target-action        | Opcional     | String  | Destino para llamar al servidor                                                                                    |                                                                                                                                                                                                                                                                                                                                                       |
| max                  | Opcional     | Entero  | Número máximo de valores                                                                                           | **Note:** Default value is set in property `var.def.rpp` in file base.properties                                                                                                                                                                                                                                      |
| carga automática     | Opcional     | Boolean | Iniciar acción de destino cuando la pantalla ha sido inicializada                                                  | **Note:** Default value is `false`                                                                                                                                                                                                                                                                                                    |
| autorefrescar        | Opcional     | Entero  | Ejecuta la acción del objetivo cada X segundos                                                                     | **Note:** The value is in seconds                                                                                                                                                                                                                                                                                                     |
| variable             | Opcional     | String  | Parámetro para rellenar el valor del criterio                                                                      | **Identifier** of a criterion in the **previous screen**                                                                                                                                                                                                                                                                                              |
| value                | Opcional     | String  | Valor por defecto del criterio                                                                                     |                                                                                                                                                                                                                                                                                                                                                       |
| session              | Opcional     | String  | Session variable to load the criterion                                                                             | Session variable identifier                                                                                                                                                                                                                                                                                                                           |
| property             | Opcional     | String  | Property variable to load the criterion                                                                            | Property variable identifier                                                                                                                                                                                                                                                                                                                          |
| validation           | Opcional     | String  | Reglas de validación                                                                                               | See [validation](../guides/validation-guide.md)                                                                                                                                                                                                                                                                                                       |
| sólo lectura         | Opcional     | Boolean | Establecer criterio como sólo lectura                                                                              | Default value is `false`                                                                                                                                                                                                                                                                                                                              |
| size                 | Opcional     | String  | Tamaño del criterio                                                                                                | `sm` (default), `md` or `lg`.                                                                                                                                                                                                                                                                                      |
| unit                 | Opcional     | String  | Texto de unidad de criterio                                                                                        | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                                                                                                                                                                                                                                         |
| icon                 | Opcional     | String  | Identificador de icono                                                                                             | **Note:** You can check all iconsets at [icons](icons.md) screen                                                                                                                                                                                                                                                                      |
| imprimible           | Opcional     | String  | Comprobar si el criterio es imprimible                                                                             | `true`, `false`, `excel`, `all`, `tab`                                                                                                                                                                                                                                                                                                                |
| ayuda                | Opcional     | String  | Help text for the criterion                                                                                        | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                                                                                                                                                                                                                                         |
| imagen de ayuda      | Opcional     | String  | Help image for the criterion                                                                                       | This **must** be a image path                                                                                                                                                                                                                                                                                                                         |
| etiqueta izquierda   | Opcional     | Entero  | Put the label on the left **and** give it a size in chars                                                          | El valor por defecto está vacío (la etiqueta está arriba en lugar de izquierda). Si está definido, el valor debe ser un número de caracteres para la etiqueta                                                                                                                                                      |

### Atributos específicos {#specific-attributes}

| Atributo                | Criterio                                                          | Tipo    | Descripción                                                                          | Valores                                                                                                                                                                              |
| ----------------------- | ----------------------------------------------------------------- | ------- | ------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| optional                | [Select](#select-criterion)                                       | Boolean | Permite seleccionar un valor vacío                                                   | Default value is `false`                                                                                                                                                             |
| filas de área           | [Textarea](#textarea-criterion)                                   | Entero  | Número de filas del área de texto                                                    | Default value is `3`                                                                                                                                                                 |
| formato-numero          | [Numeric](#numeric-criterion)                                     | String  | Formato del número                                                                   | See [autonumeric plugin](http://www.decorplanit.com/plugin/)                                                                                                                         |
| capitalizar             | [Select](#select-criterion), [Suggest](#suggest-criterion)        | Boolean | Poner la primera letra de las opciones en mayúsculas y las otras en minúsculas       | Default value is `false`                                                                                                                                                             |
| estricto                | [Suggest](#suggest-criterion)                                     | Boolean | Permitir al usuario sólo seleccionar los valores de la lista                         | Default value is `true`                                                                                                                                                              |
| check-target            | [Suggest](#suggest-criterion)                                     | String  | Acción de destino a lanzar si la etiqueta no está definida al sugerir inicialización | Default value is the defined at `target-action`                                                                                                                                      |
| comprobado              | [Checkbox](#checkbox-criterion), [Radio](#radio-button-criterion) | Boolean | Marcar el criterio como comprobado inicialmente                                      | Default value is `false`                                                                                                                                                             |
| group                   | [Checkbox](#checkbox-criterion), [Radio](#radio-button-criterion) | String  | Grupo del criterio (para fines de validación y gestión)           |                                                                                                                                                                                      |
| mostrar-deslizador      | [Numeric](#numeric-criterion)                                     | Boolean | Se utiliza para mostrar el deslizador gráfico del componente                         | **Note:** Only apply in numeric criteria                                                                                                                             |
| destination             | [Uploader](#uploader-criterion)                                   | String  | Carpeta relativa de destino para subir el archivo                                    |                                                                                                                                                                                      |
| show-weekends           | [Date](#date-criterion)                                           | Boolean | Para activar o desactivar los días de fin de semana                                  | Default value is `true`                                                                                                                                                              |
| fechas de futuro        | [Date](#date-criterion)                                           | Boolean | Para activar o desactivar los días futuros después del valor de fecha seleccionado   | Default value is `true`                                                                                                                                                              |
| date-format             | [Date](#date-criterion)                                           | String  | Para establecer el formato de la fecha que desea mostrar                             | Default value is `dd/MM/yyyy`. **Note:** See the format in the following [link](https://bootstrap-datepicker.readthedocs.org/en/latest/options.html) |
| fecha-mostrar-hoy-botón | [Date](#date-criterion)                                           | Boolean | Para mostrar o no el botón para seleccionar la fecha de hoy                          | Default value is `true`                                                                                                                                                              |
| fecha-vista-modo        | [Date](#date-criterion)                                           | String  | Selecciona 'días', 'meses' o 'años' para establecer la magnitud mínima a mostrar     | Default value is `days`                                                                                                                                                              |

## Components

### Criterios de texto {#text-criterion}

Entrada de texto básica.

<img alt="textCriterion" src={require('@docusaurus/useBaseUrl').default('img/textCriterion.png')} />

### Criterio de contraseña {#password-criterion}

Entrada de texto básica, pero los caracteres escritos no se muestran al usuario.

<img alt="Password" src={require('@docusaurus/useBaseUrl').default('img/Password.png')} />

### Textarea criterion

Criterio que permite al usuario insertar un texto grande y nuevas líneas.

<img alt="Textarea" src={require('@docusaurus/useBaseUrl').default('img/Textarea.png')} />

### Criterio oculto {#hidden-criterion}

Criterios ocultos. Es útil enviar valores estáticos a una consulta o como elemento de fórmula.

### Criterio numérico {#numeric-criterion}

Criterio que permite insertar números formateados.

<img alt="numericCriterion" src={require('@docusaurus/useBaseUrl').default('img/numericCriterion.png')} />

Criterio numérico con deslizador activado

<img alt="Numeric_slider" src={require('@docusaurus/useBaseUrl').default('img/Numeric_slider.png')} />

#### Atributo de formato numérico {#number-format-attribute}

Este atributo se utiliza para formatear el criterio numérico y el deslizador. Se especifica en el formato de objeto json.

| Atributo                                 | Uso      | Tipo    | Descripción                                                                                                                                                                                                                  | Valores                                                                                                       |
| ---------------------------------------- | -------- | ------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| min                                      | Opcional | Number  | El valor mínimo posible                                                                                                                                                                                                      | Ex: `{min: 5}`                                                                                |
| max                                      | Opcional | Number  | El valor máximo posible                                                                                                                                                                                                      | Ex: `{max: 5}`                                                                                |
| aSign                                    | Opcional | String  | Símbolo de moneda deseado                                                                                                                                                                                                    | Ex: `{aSign: ' €'}`                                                                           |
| pSign                                    | Opcional | String  | Controla la colocación del símbolo de moneda                                                                                                                                                                                 | pSign: 'p' to prefix or pSign: 's' to suffix (**default**) |
| aPad                                     | Opcional | Boolean | Controla el relleno de los decimales                                                                                                                                                                                         | **true** always pads the decimal with zeros or **false**  (**default**) to no padding      |
| precisión                                | Opcional | Number  | Number of decimals                                                                                                                                                                                                           | **Note:** The thousand & decimal separators can not be the same           |
| paso                                     | Opcional | Number  | Paso de incremento deslizante                                                                                                                                                                                                | Ex: `{step: 5}`                                                                               |
| ticks                                    | Opcional | Matriz  | Utilizado para definir los valores de ticks en el deslizador. Las marcas de marca son indicadores que indican valores especiales en el rango. Esta opción sobrescribe las opciones min y max | Ex: `{ticks: [-1000, -500, 0, 500, 1000]}`                                                    |
| etiquetas_etiquetas | Opcional | Matriz  | Define las etiquetas debajo de las marcas de tick. Acepta entrada HTML                                                                                                                                       | Ex: `{ticks_labels: ['-$1000', '-$500', '$0', '$500', '$1000']}`                              |

> **Note:** You can view all autonumeric attributes [here](http://www.decorplanit.com/plugin/) and the slider attributes [here](https://github.com/seiyria/bootstrap-slider#options)

#### Ejemplos de formato numérico {#number-format-examples}

```xml
number-format="{min: 0, max: 100, paso: 0. 1, precisión: 2, asign:' £', pSign:'s', aPad:true}"
---
number-format="{min: -1000, max: 1000, step: 10, precision: 2, Prensa:' $', pSign:'s', aPad:true,  
ticks: [-1000, -500, 0, 500, 1000], ticks_labels: ['-$1000', '-$500', '$0', '$500', '$500', '$1000']}"
```

### Criterio de fecha {#date-criterion}

Permite seleccionar una fecha con un calendario.

<img alt="dateCriteron" src={require('@docusaurus/useBaseUrl').default('img/dateCriteron.png')} />

### Criterio de hora {#time-criterion}

Permite seleccionar una hora con un selector de tiempo.

<img alt="TimeCriterion" src={require('@docusaurus/useBaseUrl').default('img/TimeCriterion.png')} />

### Criterio de fecha filtrado {#filtered-date-criterion}

Permite seleccionar una fecha con un calendario de una lista de fechas filtradas.

<img alt="FilteredDate.ong" src={require('@docusaurus/useBaseUrl').default('img/FilteredDate.ong.png')} />

### Seleccionar criterio {#select-criterion}

Muestra una lista y permite al usuario seleccionar un elemento.

<img alt="Select" src={require('@docusaurus/useBaseUrl').default('img/Select.png')} />

### Criterios sugeridos {#suggest-criterion}

Permite al usuario buscar un valor escribiendo algunos caracteres del valor buscar.

<img alt="Suggest" src={require('@docusaurus/useBaseUrl').default('img/Suggest.png')} />

> **Note:** The _typed_ text is sent to the server as `suggest` parameter, but only when the query is used to fill in the options. De lo contrario, el id de la sugerencia debería utilizarse como de costumbre.
>
> **Important:** `initial-load="query"` or `initial-load="enum"` attributes must not be used with this
> component, as it search for the values when typing. If you want to set an initial value on the
> suggest component, you should use the `target` attribute on screen or the `variable` attribute on
> the criterion.
>
> The `check-target` attribute will make to launch a query when criterion is initialized **only if**
> the criterion data has `value` field but no `label` field (i.e. if suggest component has been loaded)
> with a `target` query on screen load but only with a field instead of a compound field with `value`
> and `label` fields).

### Seleccionar sugerencia de VS {#select-vs-suggest}

| Seleccionar                                                                                              | Sugerencia                                                                                  |
| -------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| La lista de valores es "fijada"                                                                          | La lista puede cambiar cada vez que interactúa con el componente                            |
| La lista se carga cuando entra en la ventana                                                             | La lista se carga cuando interactúa con el componente                                       |
| Filtrado en el cliente                                                                                   | Se puede filtrar en cliente y servidor                                                      |
| Tienes que usarlo cuando la lista de valores es pequeña y con un número fijo de valores. | Se puede utilizar con listas grandes y pequeñas                                             |
| Usar atributo "max"=0                                                                                    | Utilice el atributo "max" para limitar los datos devueltos por el servidor. |
|                                                                                                          | Debe utilizar la variable "sugerir" para filtrar.                           |
| No se utiliza cuando hay interrelaciones entre criterios                                                 | Uso forzoso cuando hay interrelaciones con otros criterios.                 |

### Múltiples criterios de selección {#multiple-select-criterion}

Muestra una lista y permite al usuario seleccionar algunos elementos.

<img alt="Multiselect" src={require('@docusaurus/useBaseUrl').default('img/Multiselect.png')} />

### Múltiples criterios de sugerencia {#multiple-suggest-criterion}

Permite al usuario buscar algunos valores escribiendo algunos caracteres de los valores buscar.

<img alt="Multisuggest" src={require('@docusaurus/useBaseUrl').default('img/Multisuggest.png')} />

> **Note:** The _typed_ text is sent to the server as `suggest` parameter.

### Picklist criterion

It works in a similar way of a multiselect criterion, but is more intuitive

<img alt="Picklist" src={require('@docusaurus/useBaseUrl').default('img/PickList.png')} />

To fill the picklist the component uses the following columns in a query:

| Atributo    | Uso          | Tipo   | Descripción         | Valores                                                                                                                                              |
| ----------- | ------------ | ------ | ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| value       | **Required** | String | Element identifier  | Element value (value sent to server)                                                                                              |
| label       | **Required** | String | Element title       | Element title  **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                         |
| description | Opcional     | String | Element description | Element description  **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                   |
| unit        | Opcional     | String | Element unit text   | Text shown on the right of the element **Note:** You can use [i18n](i18n-internationalization.md) files (locales) |
| icon        | Opcional     | String | Element icon        | Icon shown on the left of the description **Note:** You can check all iconsets at [icons](icons.md) screen                           |
| imagen      | Opcional     | String | Element image       | URL to image shown on the left of the element                                                                                                        |

> **Note**: Picklist is only available in the new `AWE React` engine

### Criterios de checkbox {#checkbox-criterion}

Muestra una casilla de verificación. It sends a `1` (or the value defined in the **[value](#general-attributes)** attribute) if it's checked or `0` if it is unchecked.

<img alt="Checkboxes" src={require('@docusaurus/useBaseUrl').default('img/Checkboxes.png')} />

### Criterio del botón de radio {#radio-button-criterion}

Muestra un botón de radio. Es una monoselección. It sends the **[value](#general-attributes)** attribute of the selected element between all the radio buttons with the same **[group](#specific-attributes)** attribute.

<img alt="Radios" src={require('@docusaurus/useBaseUrl').default('img/Radios.png')} />

> **Note:** If we want a radio button group to be required, all the radio buttons that are inside the group must have validation="required"

### Criterio de la casilla de verificación de botón (línea) {#button-checkbox-criterion-line}

Similar to a [checkbox](#checkbox-criterion) but it has a button appearance. It sends a `1` (or the value defined in the **[value](#general-attributes)** attribute) if it's checked or `0` if it is unchecked.

<img alt="ButtonCheckbox" src={require('@docusaurus/useBaseUrl').default('img/ButtonCheckbox.png')} />

### Criterio del botón de radio (línea) {#radio-button-criterion-line}

Similar to a [radio button](#radio-button-criterion) but it has a button appearance. Es una monoselección. It sends the **[value](#general-attributes)** attribute of the selected element between all the radio buttons with the same **[group](#specific-attributes)** attribute.

<img alt="ButtonRadio" src={require('@docusaurus/useBaseUrl').default('img/ButtonRadio.png')} />

### Criterios de color {#color-criterion}

Criterios de color. Es útil para obtener el valor hexadecimal de color con un selector de color widget.

<img alt="ColorPicker" src={require('@docusaurus/useBaseUrl').default('img/ColorPicker.png')} />

### Criterio del cargador {#uploader-criterion}

Criterio útil para enviar archivos al servidor. Una vez subidos, los archivos pueden ser gestionados por los procesos del servidor

<img alt="UploaderCriterion" src={require('@docusaurus/useBaseUrl').default('img/UploaderCriterion.png')} />

### Criterio de vista de texto {#text-view-criterion}

Este criterio simplemente muestra un texto, que puede ser recuperado de una variable, un parámetro, una consulta o incluso cargado de dependencias.

<img alt="TextView" src={require('@docusaurus/useBaseUrl').default('img/TextView.png')} />

## Ejemplos {#examples}

### Múltiples sugerencias con icono {#multiple-suggest-with-icon}

```xml
<criteria label="PARAMETER_USER" component="suggest-multiple" id="..." icon="user" initial-load="query" target-action="..."    
          style="col-xs-12 col-sm-6 col-lg-3"/>
```

<img alt="SuggestWithIcon" src={require('@docusaurus/useBaseUrl').default('img/SuggestWithIcon.png')} />

### Grupo de botones de radio {#radio-button-group}

```xml
<criteria component="radio" label="PARAMETER_RADIO_1" id="RadBox1" group="RadBox" variable="RadBox" 
          value="Radio1" style="col-xs-6 col-sm-2 col-lg-1 no-label" validation="required" checked="true"/>
<criteria component="radio" label="PARAMETER_RADIO_2" id="RadBox2" group="RadBox" variable="RadBox" 
          value="Radio2" style="col-xs-6 col-sm-2 col-lg-1 no-label" readonly="true"/>
<criteria component="radio" label="PARAMETER_RADIO_3" id="RadBox3" group="RadBox" variable="RadBox" 
          value="Radio3" style="col-xs-6 col-sm-2 col-lg-1 no-label"/>
```

<img alt="Radios" src={require('@docusaurus/useBaseUrl').default('img/Radios.png')} />

### Texto requerido sin etiqueta, con marcador de posición y con icono {#required-text-without-label-with-placeholder-and-with-icon}

```xml
<criteria placeholder="SCREEN_TEXT_USER" component="text" icon="user" id="user" validation="required" 
          style="col-xs-6 col-sm-4 col-md-2"/>
```

<img alt="UserName" src={require('@docusaurus/useBaseUrl').default('img/UserName.png')} />

### Seleccionar múltiple con dos valores preseleccionados {#select-multiple-with-two-preselected-values}

```xml
<criteria component="select-multiple" id="user" label="PARAMETER_USER" icon="user" initial-load="query" 
          target-action="..." style="col-xs-12 col-sm-6 col-lg-3" value="1, 2" validation="required"/>
```

<img alt="SelectMultiplePreselected" src={require('@docusaurus/useBaseUrl').default('img/SelectMultiplePreselected.png')} />

### Ejemplo de criterio de color {#color-criterion-sample}

```xml
<criteria label="PARAMETER_COLOR" id="Col" variable="Col" component="color" 
          style="col-xs-6 col-sm-3 col-lg-2" value="#d5db89" />

```

<img alt="ColorPicker2" src={require('@docusaurus/useBaseUrl').default('img/ColorPicker2.png')} />

### Ejemplo de criterio de carga {#uploader-criterion-sample}

```xml
<criteria label="PARAMETER_UPLOADER" id="Upl" component="uploader" validation="required" 
          style="col-xs-12 col-sm-6 col-lg-4" destination="testModule">
```

<img alt="UploaderCriterion2" src={require('@docusaurus/useBaseUrl').default('img/UploaderCriterion2.png')} />