---
id: criteria
title: Criterios
---

Los elementos de criterio son componentes de ventana que pueden obtener la entrada del usuario y enviarla al servidor de aplicaciones (lógica de negocio).

<img alt="Criteria" src={require('@docusaurus/useBaseUrl').default('img/Criteria.png')} />

## Cómo funcionan {#how-does-it-work}

Internamente, un criterio tiene 2 atributos básicos para administrar la información que se envía al servidor y la información que se muestra al usuario:

* **seleccionado:** Los valores seleccionados del criterio. Estos valores **serán** enviados al servidor.
* **valores:** Los valores disponibles del criterio. Estos valores **no __** serán enviados al servidor. Es una lista de *valores* y *etiquetas* que se mostrarán al usuario para elegir.

### Prioridades {#priorities}

Los valores de **** seleccionados pueden ser completados de varias maneras. Esta es una lista de prioridades con estas maneras:

1. **variable** - Estos son los valores enviados desde otra pantalla
2. **screen target** - Los valores devueltos por el objetivo de la pantalla son la prioridad más alta de la lista
3. **`initial-load="value"`** - Los valores recuperados de consulta siguen los valores objetivo de la pantalla
4. **sesión** - Valores almacenados en sesión
5. **propiedad** - Valores almacenados en la propiedad
6. **valor** - Valores estáticos definidos en el atributo de valor

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

| Elemento                        | Uso             | Varias instancias | Descripción                                                     |
| ------------------------------- | --------------- | ----------------- | --------------------------------------------------------------- |
| [criteria](#general-attributes) | **Obligatorio** | No                | Nodo global de los criterios. Define los atributos del criterio |
| [dependency](dependencies.md)   | Opcional        | Si                | Lista de dependencias adjuntas al criterio                      |

### Atributos generales {#general-attributes}

| Atributo             | Uso             | Tipo    | Descripción                                                                                     | Valores                                                                                                                                                                                                                                                                                                                 |
| -------------------- | --------------- | ------- | ----------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| id                   | **Obligatorio** | String  | Identificador de criterio. Con fines de referencia                                              |                                                                                                                                                                                                                                                                                                                         |
| component            | **Obligatorio** | String  | Tipo de criterio                                                                                | Ver [componentes](#components)                                                                                                                                                                                                                                                                                          |
| label                | Opcional        | String  | Texto del criterio (fuera del criterio)                                                         | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                                                                                                                                                                                                                                              |
| marcador de posición | Opcional        | String  | Texto del criterio (dentro del criterio)                                                        | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                                                                                                                                                                                                                                              |
| style                | Opcional        | String  | Clases CSS Criterion                                                                            | Ver [posicionamiento de Bootstrap](http://getbootstrap.com/css/#grid-example-basic) para el tamaño del criterio                                                                                                                                                                                                         |
| carga-inicial        | Opcional        | String  | Llamada de acción al servidor para cargar los datos de criterio (se lanza al cargar la ventana) | `enum` (para [enumerado](enumerate-definition.md)), `consulta` (para llamada de consulta [](query-definition.md) cargando los valores **[](#how-does-it-work)** parte del criterio) o `valores` (para llamada de consulta [](query-definition.md) cargando el **[seleccionado](#how-does-it-work)** parte del criterio) |
| server-action        | Opcional        | String  | Llamada de acción del servidor                                                                  | Ver [lista de acciones del servidor](actions.md#server-actions)                                                                                                                                                                                                                                                         |
| target-action        | Opcional        | String  | Destino para llamar al servidor                                                                 |                                                                                                                                                                                                                                                                                                                         |
| max                  | Opcional        | Entero  | Número máximo de valores                                                                        | **Nota:** El valor predeterminado se establece en la propiedad `var.def.rpp` en el archivo base.properties                                                                                                                                                                                                              |
| carga automática     | Opcional        | Boolean | Iniciar acción de destino cuando la pantalla ha sido inicializada                               | **Nota:** El valor predeterminado es `falso`                                                                                                                                                                                                                                                                            |
| autorefrescar        | Opcional        | Entero  | Ejecuta la acción del objetivo cada X segundos                                                  | **Nota:** El valor está en segundos                                                                                                                                                                                                                                                                                     |
| variable             | Opcional        | String  | Parámetro para rellenar el valor del criterio                                                   | **Identificador** de un criterio en la **pantalla anterior**                                                                                                                                                                                                                                                            |
| value                | Opcional        | String  | Valor por defecto del criterio                                                                  |                                                                                                                                                                                                                                                                                                                         |
| session              | Opcional        | String  | Session variable to load the criterion                                                          | Session variable identifier                                                                                                                                                                                                                                                                                             |
| property             | Opcional        | String  | Property variable to load the criterion                                                         | Property variable identifier                                                                                                                                                                                                                                                                                            |
| validation           | Opcional        | String  | Reglas de validación                                                                            | Ver [validación](../guides/validation-guide.md)                                                                                                                                                                                                                                                                         |
| sólo lectura         | Opcional        | Boolean | Establecer criterio como sólo lectura                                                           | El valor predeterminado es `falso`                                                                                                                                                                                                                                                                                      |
| size                 | Opcional        | String  | Tamaño del criterio                                                                             | `sm` (default), `md` or `lg`.                                                                                                                                                                                                                                                                                           |
| unit                 | Opcional        | String  | Texto de unidad de criterio                                                                     | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                                                                                                                                                                                                                                              |
| icon                 | Opcional        | String  | Identificador de icono                                                                          | **Note:** You can check all iconsets at [icons](icons.md) screen                                                                                                                                                                                                                                                        |
| imprimible           | Opcional        | String  | Comprobar si el criterio es imprimible                                                          | `true` (default) or `false`. **Note:** `excel` only has effect on [grid columns](grids.md#printing-columns)                                                                                                                                                                                                             |
| ayuda                | Opcional        | String  | Help text for the criterion                                                                     | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                                                                                                                                                                                                                                              |
| imagen de ayuda      | Opcional        | String  | Help image for the criterion                                                                    | Esta **debe** ser una ruta de imagen                                                                                                                                                                                                                                                                                    |
| etiqueta izquierda   | Opcional        | Entero  | Pon la etiqueta a la izquierda **y** dale un tamaño en caracteres                               | El valor por defecto está vacío (la etiqueta está arriba en lugar de izquierda). Si está definido, el valor debe ser un número de caracteres para la etiqueta                                                                                                                                                           |

### Atributos específicos {#specific-attributes}

| Atributo                | Criterio                                                                      | Tipo    | Descripción                                                                          | Valores                                                                                                                                                           |
| ----------------------- | ----------------------------------------------------------------------------- | ------- | ------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| optional                | [Seleccionar](#select-criterion)                                              | Boolean | Permite seleccionar un valor vacío                                                   | El valor predeterminado es `falso`                                                                                                                                |
| filas de área           | [Textarea](#textarea-criterion)                                               | Entero  | Número de filas del área de texto                                                    | El valor predeterminado es `3`                                                                                                                                    |
| formato-numero          | [Numeric](#numeric-criterion)                                                 | String  | Formato del número                                                                   | Ver [plugin umeric](http://www.decorplanit.com/plugin/)                                                                                                           |
| capitalizar             | [Seleccionar](#select-criterion), [Sugerencia](#suggest-criterion)            | Boolean | Poner la primera letra de las opciones en mayúsculas y las otras en minúsculas       | El valor predeterminado es `falso`                                                                                                                                |
| estricto                | [Sugerencia](#suggest-criterion)                                              | Boolean | Permitir al usuario sólo seleccionar los valores de la lista                         | El valor predeterminado es `verdadero`                                                                                                                            |
| check-target            | [Sugerencia](#suggest-criterion)                                              | String  | Acción de destino a lanzar si la etiqueta no está definida al sugerir inicialización | El valor predeterminado es el definido en `target-action`                                                                                                         |
| comprobado              | [Caja de verificación](#checkbox-criterion), [Radio](#radio-button-criterion) | Boolean | Marcar el criterio como comprobado inicialmente                                      | El valor predeterminado es `falso`                                                                                                                                |
| group                   | [Caja de verificación](#checkbox-criterion), [Radio](#radio-button-criterion) | String  | Grupo del criterio (para fines de validación y gestión)                              |                                                                                                                                                                   |
| mostrar-deslizador      | [Numeric](#numeric-criterion)                                                 | Boolean | Se utiliza para mostrar el deslizador gráfico del componente                         | **Nota:** Sólo aplicar en criterios numéricos                                                                                                                     |
| destination             | [Cargador](#uploader-criterion)                                               | String  | Carpeta relativa de destino para subir el archivo                                    |                                                                                                                                                                   |
| show-weekends           | [Date](#date-criterion)                                                       | Boolean | Para activar o desactivar los días de fin de semana                                  | El valor predeterminado es `verdadero`                                                                                                                            |
| fechas de futuro        | [Date](#date-criterion)                                                       | Boolean | Para activar o desactivar los días futuros después del valor de fecha seleccionado   | El valor predeterminado es `verdadero`                                                                                                                            |
| date-format             | [Date](#date-criterion)                                                       | String  | Para establecer el formato de la fecha que desea mostrar                             | El valor predeterminado es `dd/MM/aaaaa`. **Nota:** Ver el formato en el siguiente enlace [](https://bootstrap-datepicker.readthedocs.org/en/latest/options.html) |
| fecha-mostrar-hoy-botón | [Date](#date-criterion)                                                       | Boolean | Para mostrar o no el botón para seleccionar la fecha de hoy                          | El valor predeterminado es `verdadero`                                                                                                                            |
| fecha-vista-modo        | [Date](#date-criterion)                                                       | String  | Selecciona 'días', 'meses' o 'años' para establecer la magnitud mínima a mostrar     | El valor predeterminado es `días`                                                                                                                                 |

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

<img alt="Deslizador_numérico" src={require('@docusaurus/useBaseUrl').default('img/Numeric_slider.png')} />

#### Atributo de formato numérico {#number-format-attribute}

Este atributo se utiliza para formatear el criterio numérico y el deslizador. Se especifica en el formato de objeto json.

| Atributo            | Uso      | Tipo    | Descripción                                                                                                                                                                                  | Valores                                                                                   |
| ------------------- | -------- | ------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| min                 | Opcional | Number  | El valor mínimo posible                                                                                                                                                                      | Ex: `{min: 5}`                                                                            |
| max                 | Opcional | Number  | El valor máximo posible                                                                                                                                                                      | Ex: `{max: 5}`                                                                            |
| aSign               | Opcional | String  | Símbolo de moneda deseado                                                                                                                                                                    | Ex: `{aSign: ' €'}`                                                                       |
| pSign               | Opcional | String  | Controla la colocación del símbolo de moneda                                                                                                                                                 | pSign: 'p' para prefijo o pSign: 's' para sufijo (**predeterminado**)                     |
| aPad                | Opcional | Boolean | Controla el relleno de los decimales                                                                                                                                                         | **verdadero** siempre pon el decimal con ceros o **falso**  (**por defecto**) sin relleno |
| precisión           | Opcional | Number  | Number of decimals                                                                                                                                                                           | **Nota:** Los mil & separadores decimales no pueden ser los mismos                        |
| paso                | Opcional | Number  | Paso de incremento deslizante                                                                                                                                                                | Ex: `{step: 5}`                                                                           |
| ticks               | Opcional | Matriz  | Utilizado para definir los valores de ticks en el deslizador. Las marcas de marca son indicadores que indican valores especiales en el rango. Esta opción sobrescribe las opciones min y max | Ej: `{ticks: [-1000, -500, 0, 500, 1000]}`                                                |
| etiquetas_etiquetas | Opcional | Matriz  | Define las etiquetas debajo de las marcas de tick. Acepta entrada HTML                                                                                                                       | Ej: `{ticks_labels: ['-$1000', '-$500', '$0', '$500', '$1000']}`                          |

> **Nota:** Puedes ver todos los atributos uméricos [aquí](http://www.decorplanit.com/plugin/) y los atributos deslizantes [aquí](https://github.com/seiyria/bootstrap-slider#options)

#### Ejemplos de formato numérico {#number-format-examples}

```xml
number-format="{min: 0, max: 100, paso: 0. 1, precisión: 2, asign:' £', pSign:'s', aPad:true}"
---
number-format="{min: -1000, max: 1000, step: 10, precision: 2, Prensa:' $', pSign:'s', aPad:true,  
ticks: [-1000, -500, 0, 500, 1000], ticks_labels: ['-$1000', '-$500', '$0', '$500', '$500', '$1000']}"
```

### Criterio de fecha {#date-criterion}

Permite seleccionar una fecha con un calendario.

<img alt="dateCriterio" src={require('@docusaurus/useBaseUrl').default('img/dateCriteron.png')} />

### Criterio de hora {#time-criterion}

Permite seleccionar una hora con un selector de tiempo.

<img alt="TimeCriterion" src={require('@docusaurus/useBaseUrl').default('img/TimeCriterion.png')} />

### Criterio de fecha filtrado {#filtered-date-criterion}

Permite seleccionar una fecha con un calendario de una lista de fechas filtradas.

<img alt="Filtrado Date.ong" src={require('@docusaurus/useBaseUrl').default('img/FilteredDate.ong.png')} />

### Seleccionar criterio {#select-criterion}

Muestra una lista y permite al usuario seleccionar un elemento.

<img alt="Seleccionar" src={require('@docusaurus/useBaseUrl').default('img/Select.png')} />

### Criterios sugeridos {#suggest-criterion}

Permite al usuario buscar un valor escribiendo algunos caracteres del valor buscar.

<img alt="Sugerencia" src={require('@docusaurus/useBaseUrl').default('img/Suggest.png')} />

> **Nota:** El texto *escrito* se envía al servidor como `sugerir` parámetro, pero sólo cuando la consulta se utiliza para rellenar las opciones. De lo contrario, el id de la sugerencia debería utilizarse como de costumbre.
> 
> **Important:** `initial-load="query"` or `initial-load="enum"` attributes must not be used with this component, as it search for the values when typing. If you want to set an initial value on the suggest component, you should use the `target` attribute on screen or the `variable` attribute on the criterion.
> 
> The `check-target` attribute will make to launch a query when criterion is initialized **only if** the criterion data has `value` field but no `label` field (i.e. if suggest component has been loaded) with a `target` query on screen load but only with a field instead of a compound field with `value` and `label` fields).


### Seleccionar sugerencia de VS {#select-vs-suggest}

| Seleccionar                                                                              | Sugerencia                                                                  |
| ---------------------------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| La lista de valores es "fijada"                                                          | La lista puede cambiar cada vez que interactúa con el componente            |
| La lista se carga cuando entra en la ventana                                             | La lista se carga cuando interactúa con el componente                       |
| Filtrado en el cliente                                                                   | Se puede filtrar en cliente y servidor                                      |
| Tienes que usarlo cuando la lista de valores es pequeña y con un número fijo de valores. | Se puede utilizar con listas grandes y pequeñas                             |
| Usar atributo "max"=0                                                                    | Utilice el atributo "max" para limitar los datos devueltos por el servidor. |
|                                                                                          | Debe utilizar la variable "sugerir" para filtrar.                           |
| No se utiliza cuando hay interrelaciones entre criterios                                 | Uso forzoso cuando hay interrelaciones con otros criterios.                 |

### Múltiples criterios de selección {#multiple-select-criterion}

Muestra una lista y permite al usuario seleccionar algunos elementos.

<img alt="Multiselección" src={require('@docusaurus/useBaseUrl').default('img/Multiselect.png')} />

### Múltiples criterios de sugerencia {#multiple-suggest-criterion}

Permite al usuario buscar algunos valores escribiendo algunos caracteres de los valores buscar.

<img alt="Multisugerir" src={require('@docusaurus/useBaseUrl').default('img/Multisuggest.png')} />

> **Nota:** El texto *escrito* se envía al servidor como `sugerir` parámetro.

### Picklist criterion

It works in a similar way of a multiselect criterion, but is more intuitive

<img alt="Picklist" src={require('@docusaurus/useBaseUrl').default('img/PickList.png')} />

To fill the picklist the component uses the following columns in a query:

| Atributo    | Uso             | Tipo   | Descripción         | Valores                                                                                                           |
| ----------- | --------------- | ------ | ------------------- | ----------------------------------------------------------------------------------------------------------------- |
| value       | **Obligatorio** | String | Element identifier  | Element value (value sent to server)                                                                              |
| label       | **Obligatorio** | String | Element title       | Element title  **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                         |
| description | Opcional        | String | Element description | Element description  **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                   |
| unit        | Opcional        | String | Element unit text   | Text shown on the right of the element **Note:** You can use [i18n](i18n-internationalization.md) files (locales) |
| icon        | Opcional        | String | Element icon        | Icon shown on the left of the description **Note:** You can check all iconsets at [icons](icons.md) screen        |
| imagen      | Opcional        | String | Element image       | URL to image shown on the left of the element                                                                     |

> **Note**: Picklist is only available in the new `AWE React` engine

### Criterios de checkbox {#checkbox-criterion}

Muestra una casilla de verificación. Envía un `1` (o el valor definido en el **[valor](#general-attributes)** atributo) si está marcado o `0` si está desmarcado.

<img alt="Casillas" src={require('@docusaurus/useBaseUrl').default('img/Checkboxes.png')} />

### Criterio del botón de radio {#radio-button-criterion}

Muestra un botón de radio. Es una monoselección. Envía el atributo **[value](#general-attributes)** del elemento seleccionado entre todos los botones de radio con el mismo atributo **[grupo](#specific-attributes)**.

<img alt="Radios" src={require('@docusaurus/useBaseUrl').default('img/Radios.png')} />

> **Nota:** Si queremos que un grupo de botones de radio sea requerido, todos los botones de radio que están dentro del grupo deben tener validación="requerido"

### Criterio de la casilla de verificación de botón (línea) {#button-checkbox-criterion-line}

Similar a una casilla de verificación [](#checkbox-criterion) pero tiene una apariencia de botón. Envía un `1` (o el valor definido en el **[valor](#general-attributes)** atributo) si está marcado o `0` si está desmarcado.

<img alt="ButtonCheckbox" src={require('@docusaurus/useBaseUrl').default('img/ButtonCheckbox.png')} />

### Criterio del botón de radio (línea) {#radio-button-criterion-line}

Similar a un [botón de radio](#radio-button-criterion) pero tiene una apariencia de botón. Es una monoselección. Envía el atributo **[value](#general-attributes)** del elemento seleccionado entre todos los botones de radio con el mismo atributo **[grupo](#specific-attributes)**.

<img alt="Radio Botón" src={require('@docusaurus/useBaseUrl').default('img/ButtonRadio.png')} />

### Criterios de color {#color-criterion}

Criterios de color. Es útil para obtener el valor hexadecimal de color con un selector de color widget.

<img alt="Selector de color" src={require('@docusaurus/useBaseUrl').default('img/ColorPicker.png')} />

### Criterio del cargador {#uploader-criterion}

Criterio útil para enviar archivos al servidor. Una vez subidos, los archivos pueden ser gestionados por los procesos del servidor

<img alt="Criterio de subida" src={require('@docusaurus/useBaseUrl').default('img/UploaderCriterion.png')} />

### Criterio de vista de texto {#text-view-criterion}

Este criterio simplemente muestra un texto, que puede ser recuperado de una variable, un parámetro, una consulta o incluso cargado de dependencias.

<img alt="TextView" src={require('@docusaurus/useBaseUrl').default('img/TextView.png')} />

## Ejemplos {#examples}

### Múltiples sugerencias con icono {#multiple-suggest-with-icon}

```xml
<criteria label="PARAMETER_USER" component="suggest-multiple" id="..." icon="user" initial-load="query" target-action="..."    
          style="col-xs-12 col-sm-6 col-lg-3"/>
```

<img alt="Sugerir icono" src={require('@docusaurus/useBaseUrl').default('img/SuggestWithIcon.png')} />

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

<img alt="Usuario" src={require('@docusaurus/useBaseUrl').default('img/UserName.png')} />

### Seleccionar múltiple con dos valores preseleccionados {#select-multiple-with-two-preselected-values}

```xml
<criteria component="select-multiple" id="user" label="PARAMETER_USER" icon="user" initial-load="query" 
          target-action="..." style="col-xs-12 col-sm-6 col-lg-3" value="1, 2" validation="required"/>
```

<img alt="Seleccionar múltiples preseleccionados" src={require('@docusaurus/useBaseUrl').default('img/SelectMultiplePreselected.png')} />

### Ejemplo de criterio de color {#color-criterion-sample}

```xml
<criteria label="PARAMETER_COLOR" id="Col" variable="Col" component="color" 
          style="col-xs-6 col-sm-3 col-lg-2" value="#d5db89" />

```

<img alt="Color2" src={require('@docusaurus/useBaseUrl').default('img/ColorPicker2.png')} />

### Ejemplo de criterio de carga {#uploader-criterion-sample}

```xml
<criteria label="PARAMETER_UPLOADER" id="Upl" component="uploader" validation="required" 
          style="col-xs-12 col-sm-6 col-lg-4" destination="testModule">
```

<img alt="Criterio del cargador2" src={require('@docusaurus/useBaseUrl').default('img/UploaderCriterion2.png')} />