---
id: criteria
title: Criterios
---

Los elementos de criterio son componentes de ventana que pueden obtener la entrada del usuario y enviarla al servidor de aplicaciones (lógica de negocio).

<img alt="Criteria" src={require('@docusaurus/useBaseUrl').default('img/Criteria.png')} />

## Cómo funcionan {#how-does-it-work}

Internamente, un criterio tiene 2 atributos básicos para administrar la información que se envía al servidor y la información que se muestra al usuario:

* **seleccionado:** Los valores seleccionados del criterio. Estos valores **serán** enviados al servidor.
* **valores:** Los valores disponibles del criterio. Estos valores **_no_** serán enviados al servidor. Es una lista de *valores* y *etiquetas* que se mostrarán al usuario para elegir.

### Prioridades {#priorities}

Los valores **seleccionados** pueden ser completados de varias maneras. Esta es una lista de prioridades con estas maneras:

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
| label                | Opcional        | String  | Texto del criterio (fuera del criterio)                                                         | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                                                                                                                                                                                                                                              |
| placeholder | Opcional        | String  | Texto del criterio (dentro del criterio)                                                        | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                                                                                                                                                                                                                                              |
| style                | Opcional        | String  | Clases CSS del criterio                                                                            | Ver [posicionamiento de Bootstrap](http://getbootstrap.com/css/#grid-example-basic) para el tamaño del criterio                                                                                                                                                                                                         |
| initial-load        | Opcional        | String  | Llamada de acción al servidor para cargar los datos de criterio (se lanza al cargar la ventana) | `enum` (para [enumerado](enumerate-definition.md)), `query` (para llamada de consulta [query call](query-definition.md) cargando los valores **[values](#how-does-it-work)** parte del criterio) o `value` (para llamada de consulta [query call](query-definition.md) cargando el **[seleccionado](#how-does-it-work)** parte del criterio) |
| server-action        | Opcional        | String  | Llamada de acción del servidor                                                                  | Ver [lista de acciones del servidor](actions.md#server-actions)                                                                                                                                                                                                                                                         |
| target-action        | Opcional        | String  | Destino para llamar al servidor                                                                 |                                                                                                                                                                                                                                                                                                                         |
| max                  | Opcional        | Entero  | Número máximo de valores                                                                        | **Nota:** El valor predeterminado se establece en la propiedad `var.def.rpp` en el archivo base.properties                                                                                                                                                                                                              |
| autoload     | Opcional        | Boolean | Iniciar acción de destino cuando la pantalla ha sido inicializada                               | **Nota:** El valor predeterminado es `false`                                                                                                                                                                                                                                                                            |
| autorefresh        | Opcional        | Entero  | Ejecuta la acción del objetivo cada X segundos                                                  | **Nota:** El valor está en segundos                                                                                                                                                                                                                                                                                     |
| variable             | Opcional        | String  | Parámetro para rellenar el valor del criterio                                                   | **Identificador** de un criterio en la **pantalla anterior**                                                                                                                                                                                                                                                            |
| value                | Opcional        | String  | Valor por defecto del criterio                                                                  |                                                                                                                                                                                                                                                                                                                         |
| session              | Opcional        | String  | Variable de sesión para cargar el criterio                                                          | Identificador de la variable de sesión                                                                                                                                                                                                                                                                                             |
| property             | Opcional        | String  | Variable de propiedad para cargar el criterio                                                         | Identificador de la variable de propiedad                                                                                                                                                                                                                                                                                            |
| validation           | Opcional        | String  | Reglas de validación                                                                            | Ver [validación](../guides/validation-guide.md)                                                                                                                                                                                                                                                                         |
| readonly         | Opcional        | Boolean | Establecer criterio como sólo lectura                                                           | El valor predeterminado es `false`                                                                                                                                                                                                                                                                                      |
| size                 | Opcional        | String  | Tamaño del criterio                                                                             | `sm` (por defecto), `md` o `lg`.                                                                                                                                                                                                                                                                                           |
| unit                 | Opcional        | String  | Texto de unidad de criterio                                                                     | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                                                                                                                                                                                                                                              |
| icon                 | Opcional        | String  | Identificador de icono                                                                          | **Nota:** Puedes consultar todos los conjuntos de iconos en la pantalla [icons](icons.md)                                                                                                                                                                                                                                                        |
| printable           | Opcional        | String  | Comprobar si el criterio es imprimible                                                          | `true` (por defecto) o `false`. **Nota:** `excel` solo tiene efecto en las [columnas de grid](grids.md#printing-columns)                                                                                                                                                                                                             |
| help                | Opcional        | String  | Texto de ayuda para el criterio                                                                     | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                                                                                                                                                                                                                                              |
| help-image      | Opcional        | String  | Imagen de ayuda para el criterio                                                                    | Esta **debe** ser una ruta de imagen                                                                                                                                                                                                                                                                                    |
| left-label   | Opcional        | Entero  | Pon la etiqueta a la izquierda **y** dale un tamaño en caracteres                               | El valor por defecto está vacío (la etiqueta está arriba en lugar de izquierda). Si está definido, el valor debe ser un número de caracteres para la etiqueta. La etiqueta se muestra a la izquierda desde el punto de corte `sm` (576 px) y arriba por debajo de él. Su ancho es el número de caracteres por los píxeles por carácter del tamaño del criterio (7 px en `sm`, 8 px en `md` y 10 px en `lg`) en ambos motores                                                                                                                                                           |

Los atributos `help` y `help-image` muestran un tooltip cuando el usuario se detiene sobre la etiqueta del criterio (junto
a ella aparece un icono de ayuda). Ambos motores lo muestran en todos los criterios, incluidos los checkbox, los radio,
los checkbox de botones y los radio de botones, y también en los [botones](button.md) y en las cabeceras de las
[columnas del grid](grids.md#column-attributes).

### Atributos específicos {#specific-attributes}

| Atributo                | Criterio                                                                      | Tipo    | Descripción                                                                          | Valores                                                                                                                                                           |
| ----------------------- | ----------------------------------------------------------------------------- | ------- | ------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| optional                | [Seleccionar](#select-criterion)                                              | Boolean | Permite seleccionar un valor vacío                                                   | El valor predeterminado es `false`                                                                                                                                |
| area-rows           | [Textarea](#textarea-criterion)                                               | Entero  | Número de filas del área de texto                                                    | El valor predeterminado es `3`                                                                                                                                    |
| number-format          | [Numeric](#numeric-criterion)                                                 | String  | Formato del número                                                                   | Ver [plugin autonumeric](http://www.decorplanit.com/plugin/)                                                                                                           |
| capitalize             | [Seleccionar](#select-criterion), [Sugerencia](#suggest-criterion)            | Boolean | Poner la primera letra de las opciones en mayúsculas y las otras en minúsculas       | El valor predeterminado es `false`                                                                                                                                |
| strict                | [Sugerencia](#suggest-criterion)                                              | Boolean | Permitir al usuario sólo seleccionar los valores de la lista                         | El valor predeterminado es `true`                                                                                                                            |
| check-target            | [Sugerencia](#suggest-criterion)                                              | String  | Acción de destino a lanzar si la etiqueta no está definida al sugerir inicialización | El valor predeterminado es el definido en `target-action`                                                                                                         |
| checked              | [Caja de verificación](#checkbox-criterion), [Radio](#radio-button-criterion) | Boolean | Marcar el criterio como comprobado inicialmente                                      | El valor predeterminado es `false`                                                                                                                                |
| group                   | [Caja de verificación](#checkbox-criterion), [Radio](#radio-button-criterion) | String  | Grupo del criterio (para fines de validación y gestión)                              |                                                                                                                                                                   |
| show-slider      | [Numeric](#numeric-criterion)                                                 | Boolean | Se utiliza para mostrar el deslizador gráfico del componente                         | **Nota:** Sólo aplicar en criterios numéricos                                                                                                                     |
| destination             | [Cargador](#uploader-criterion)                                               | String  | Carpeta relativa de destino para subir el archivo                                    |                                                                                                                                                                   |
| drop-zone               | [Cargador](#uploader-criterion) | Boolean | Muestra un área para arrastrar y soltar el archivo, y una vista previa de las imágenes | El valor predeterminado es `false` |
| capture                 | [Cargador](#uploader-criterion) | String  | Añade un botón para hacer una foto con la cámara del dispositivo | `user` (cámara frontal) o `environment` (cámara trasera) |
| show-weekends           | [Date](#date-criterion)                                                       | Boolean | Para activar o desactivar los días de fin de semana                                  | El valor predeterminado es `true`                                                                                                                            |
| show-future-dates        | [Date](#date-criterion)                                                       | Boolean | Para activar o desactivar los días futuros después del valor de fecha seleccionado   | El valor predeterminado es `true`                                                                                                                            |
| date-format             | [Date](#date-criterion)                                                       | String  | Para establecer el formato de la fecha que desea mostrar                             | El valor predeterminado es `dd/MM/yyyy`. **Nota:** Ver el formato en el siguiente enlace [link](https://bootstrap-datepicker.readthedocs.org/en/latest/options.html) |
| date-show-today-button | [Date](#date-criterion)                                                       | Boolean | Para mostrar o no el botón para seleccionar la fecha de hoy                          | El valor predeterminado es `true`                                                                                                                            |
| date-view-mode        | [Date](#date-criterion)                                                       | String  | Selecciona 'days', 'months' o 'years' para establecer la magnitud mínima a mostrar     | El valor predeterminado es `days`                                                                                                                                 |
| presets                 | [Rango de fechas](#date-range-criterion) | String  | Preajustes rápidos que se muestran bajo el rango, separados por comas o espacios | `TODAY`, `YTD`, `ALL` o un periodo hacia atrás desde hoy (`7D`, `2W`, `1M`, `3M`, `12M`, `3Y`...). Por defecto no se muestra ningún preajuste |

## Componentes {#components}

### Criterios de texto {#text-criterion}

Entrada de texto básica.

<img alt="textCriterion" src={require('@docusaurus/useBaseUrl').default('img/textCriterion.png')} />

### Criterio de contraseña {#password-criterion}

Entrada de texto básica, pero los caracteres escritos no se muestran al usuario.

<img alt="Password" src={require('@docusaurus/useBaseUrl').default('img/Password.png')} />

Mientras las mayúsculas están bloqueadas, ambos motores marcan el campo con un color de aviso y un icono de flecha. El
estado sale de la última tecla pulsada en la página, así que el aviso aparece aunque el usuario activara el bloqueo antes
de llegar al campo.

En ambos motores el campo tiene un botón para mostrar u ocultar la contraseña en su extremo derecho (un icono de ojo,
"Mostrar contraseña" / "Ocultar contraseña"), accesible con el teclado y desactivado mientras el criterio es de solo
lectura. Solo cambia cómo se dibuja el campo: el valor, la validación y el parámetro enviado no cambian. Un campo
demasiado estrecho para alojar el botón sin tapar el texto (menos de 6rem, como una contraseña con una etiqueta a la
izquierda larga en una columna estrecha) lo oculta. La página de inicio de sesión mantiene su propio botón, y una columna
de contraseña de un grid no tiene botón.

### Criterio de área de texto {#textarea-criterion}

Criterio que permite al usuario insertar un texto grande y nuevas líneas.

<img alt="Textarea" src={require('@docusaurus/useBaseUrl').default('img/Textarea.png')} />

### Criterio oculto {#hidden-criterion}

Criterio oculto. Es útil enviar valores estáticos a una consulta o como elemento de fórmula.

### Criterio numérico {#numeric-criterion}

Criterio que permite insertar números formateados.

<img alt="numericCriterion" src={require('@docusaurus/useBaseUrl').default('img/numericCriterion.png')} />

Criterio numérico con deslizador activado

<img alt="Deslizador_numérico" src={require('@docusaurus/useBaseUrl').default('img/Numeric_slider.png')} />

#### Atributo de formato numérico {#number-format-attribute}

Este atributo se utiliza para formatear el criterio numérico y el deslizador. Se especifica en el formato de objeto json.

| Atributo            | Uso      | Tipo    | Descripción                                                                                                                                                                                  | Valores                                                                                   |
| ------------------- | -------- | ------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| min                 | Opcional | Number  | El valor mínimo posible                                                                                                                                                                      | Ej: `{min: 5}`                                                                            |
| max                 | Opcional | Number  | El valor máximo posible                                                                                                                                                                      | Ej: `{max: 5}`                                                                            |
| aSign               | Opcional | String  | Símbolo de moneda deseado                                                                                                                                                                    | Ej: `{aSign: ' €'}`                                                                       |
| pSign               | Opcional | String  | Controla la colocación del símbolo de moneda                                                                                                                                                 | pSign: 'p' para prefijo o pSign: 's' para sufijo (**predeterminado**)                     |
| aPad                | Opcional | Boolean | Controla el relleno de los decimales                                                                                                                                                         | **true** rellena siempre los decimales con ceros; **false** (**por defecto**) no aplica relleno |
| precision           | Opcional | Number  | Número de decimales                                                                                                                                                                           | **Nota:** Los mil & separadores decimales no pueden ser los mismos                        |
| step                | Opcional | Number  | Paso de incremento deslizante                                                                                                                                                                | Ej: `{step: 5}`                                                                           |
| ticks               | Opcional | Matriz  | Utilizado para definir los valores de ticks en el deslizador. Las marcas de marca son indicadores que indican valores especiales en el rango. Esta opción sobrescribe las opciones min y max | Ej: `{ticks: [-1000, -500, 0, 500, 1000]}`                                                |
| ticks_labels | Opcional | Matriz  | Define las etiquetas debajo de las marcas de tick. Acepta entrada HTML                                                                                                                       | Ej: `{ticks_labels: ['-$1000', '-$500', '$0', '$500', '$1000']}`                          |

> **Nota:** Puedes ver todos los atributos de autonumeric [aquí](http://www.decorplanit.com/plugin/) y los atributos deslizantes [aquí](https://github.com/seiyria/bootstrap-slider#options)

#### Ejemplos de formato numérico {#number-format-examples}

```xml
number-format="{min: 0, max: 100, step: 0.01, precision: 2, aSign:' £', pSign:'s', aPad:true}"
---
number-format="{min: -1000, max: 1000, step: 10, precision: 2, aSign:' $', pSign:'s', aPad:true,  
ticks: [-1000, -500, 0, 500, 1000], ticks_labels: ['-$1000', '-$500', '$0', '$500', '$1000']}"
```

### Criterio de fecha {#date-criterion}

Permite seleccionar una fecha con un calendario.

<img alt="dateCriterio" src={require('@docusaurus/useBaseUrl').default('img/dateCriteron.png')} />

Los motores AngularJS y React aplican las mismas opciones al criterio de fecha y a las columnas de fecha de un *grid*:

* `date-format` define el formato de la fecha que se muestra **y** del valor que el cliente envía al servidor y espera
  recibir de él. Por defecto es `dd/mm/yyyy`. Los símbolos son los del plugin bootstrap-datepicker (`d`, `dd`, `m`, `mm`,
  `M`, `MM`, `yy`, `yyyy`...), así que `yyyy-mm-dd` envía `2024-03-15`.
* `date-view-mode` con el valor `months` o `years` hace que el calendario seleccione un mes o un año. Úsalo con un
  formato sin día (`mm/yyyy` o `yyyy`).
* `show-weekends="false"` deshabilita los sábados y los domingos.
* `show-future-dates="false"` deshabilita las fechas posteriores a la fecha que tiene el criterio cuando arranca la
  pantalla.
* `date-show-today-button="false"` oculta el botón que selecciona el día de hoy.

### Criterio de hora {#time-criterion}

Permite seleccionar una hora con un selector de tiempo.

<img alt="TimeCriterion" src={require('@docusaurus/useBaseUrl').default('img/TimeCriterion.png')} />

### Criterio de rango de fechas {#date-range-criterion}

Permite seleccionar un periodo (una fecha de inicio y una fecha de fin) en un solo criterio. La fecha de fin no puede ser
anterior a la de inicio (se permite un fin abierto salvo que el criterio sea `required`, que exige ambas fechas). El
usuario puede escribir las fechas, elegirlas en el calendario o hacer clic en uno de los preajustes rápidos.

```xml
<criteria id="Period" component="date-range" label="PERIOD" presets="1M,3M,YTD,12M,3Y,ALL"/>
```

El criterio envía **dos parámetros** al servidor, con el nombre del identificador del criterio y los sufijos `.from`
y `.to`. Ambos son fechas en formato `dd/MM/yyyy` (un `null` cuando la fecha está vacía), de modo que un criterio con
identificador `Period` envía `Period.from` y `Period.to`, y una query los lee como cualquier otro parámetro:

```xml
<query id="OrdersByPeriod">
  <table id="Orders"/>
  <field id="OrderDate"/>
  <where>
    <and>
      <filter left-field="OrderDate" condition="ge" right-variable="PeriodFrom"/>
      <filter left-field="OrderDate" condition="le" right-variable="PeriodTo"/>
    </and>
  </where>
  <variable id="PeriodFrom" type="DATE" name="Period.from"/>
  <variable id="PeriodTo" type="DATE" name="Period.to"/>
</query>
```

El atributo **[value](#general-attributes)** es una lista de las dos fechas por defecto separadas por una coma
(`value="01/01/2026,31/01/2026"`); una primera fecha vacía (`value=",31/01/2026"`) deja abierto el inicio.

El atributo **[presets](#specific-attributes)** define los preajustes rápidos. Cada código se muestra como un botón que
rellena ambas fechas a partir de la fecha actual, y el preajuste activo se desactiva en cuanto se edita una de las fechas:

| Preajuste              | Rango                                            |
|------------------------|--------------------------------------------------|
| `TODAY`                | Hoy                                              |
| `YTD`                  | Desde el 1 de enero del año actual hasta hoy     |
| `ALL`                  | Borra ambas fechas (sin filtro de periodo)       |
| `<n>D`, `<n>W`         | Los últimos `n` días o semanas hasta hoy         |
| `<n>M`, `<n>Y`         | Los últimos `n` meses o años hasta hoy (`1M`, `3M`, `12M`, `3Y`...) |

La etiqueta de un preajuste es el [locale](i18n-internationalization.md) `DATE_RANGE_PRESET_` seguido de su código
(`DATE_RANGE_PRESET_YTD`), que AWE proporciona para `TODAY`, `1W`, `1M`, `3M`, `6M`, `YTD`, `12M`, `3Y` y `ALL`; cualquier
otro código muestra el propio código salvo que definas su locale. Los locales `DATE_RANGE_FROM` y `DATE_RANGE_TO` son
los marcadores de posición de los dos campos, y `VALIDATOR_MESSAGE_DATE_RANGE` el mensaje de validación.

> **Nota**: Las fechas son siempre `dd/MM/yyyy`: los atributos `date-format`, `show-weekends`, `show-future-dates` y
> `date-view-mode` del [criterio de fecha](#date-criterion) no se aplican al rango de fechas.

### Criterio de fecha filtrado {#filtered-date-criterion}

Permite seleccionar una fecha con un calendario de una lista de fechas filtradas.

<img alt="Filtrado Date.ong" src={require('@docusaurus/useBaseUrl').default('img/FilteredDate.ong.png')} />

### Seleccionar criterio {#select-criterion}

Muestra una lista y permite al usuario seleccionar un elemento.

<img alt="Seleccionar" src={require('@docusaurus/useBaseUrl').default('img/Select.png')} />

### Criterios sugeridos {#suggest-criterion}

Permite al usuario buscar un valor escribiendo algunos caracteres del valor buscar.

<img alt="Sugerencia" src={require('@docusaurus/useBaseUrl').default('img/Suggest.png')} />

> **Nota:** El texto *escrito* se envía al servidor como `suggest` parámetro, pero sólo cuando la consulta se utiliza para rellenar las opciones. De lo contrario, el id de la sugerencia debería utilizarse como de costumbre.
> 
> **Importante:** los atributos `initial-load="query"` o `initial-load="enum"` no deben utilizarse con este componente, ya que busca los valores mientras se escribe. Si desea establecer un valor inicial en el componente de sugerencia, debe usar el atributo `target` de la pantalla o el atributo `variable` del criterio.
> 
> El atributo `check-target` hará que se lance una consulta cuando se inicialice el criterio **solo si** los datos del criterio tienen el campo `value` pero no el campo `label` (es decir, si el componente de sugerencia se ha cargado con una consulta `target` al cargar la pantalla, pero solo con un campo en lugar de un campo compuesto con los campos `value` y `label`).


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

> **Nota:** El texto *escrito* se envía al servidor como `suggest` parámetro.

### Criterio de lista de selección dual (picklist) {#picklist-criterion}

Funciona de forma similar a un criterio de selección múltiple, pero es más intuitivo.

<img alt="Picklist" src={require('@docusaurus/useBaseUrl').default('img/PickList.png')} />

Para rellenar la lista de selección dual, el componente utiliza las siguientes columnas en una consulta:

| Atributo    | Uso             | Tipo   | Descripción         | Valores                                                                                                           |
| ----------- | --------------- | ------ | ------------------- | ----------------------------------------------------------------------------------------------------------------- |
| value       | **Obligatorio** | String | Identificador del elemento  | Valor del elemento (valor enviado al servidor)                                                                              |
| label       | **Obligatorio** | String | Título del elemento       | Título del elemento  **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                         |
| description | Opcional        | String | Descripción del elemento | Descripción del elemento  **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                   |
| unit        | Opcional        | String | Texto de unidad del elemento   | Texto mostrado a la derecha del elemento **Nota:** Puedes usar literales [i18n](i18n-internationalization.md) |
| icon        | Opcional        | String | Icono del elemento        | Icono mostrado a la izquierda de la descripción **Nota:** Puedes consultar todos los conjuntos de iconos en la pantalla [icons](icons.md)        |
| image      | Opcional        | String | Imagen del elemento       | URL de la imagen mostrada a la izquierda del elemento                                                                     |

> **Nota**: la lista de selección dual (picklist) solo está disponible en el nuevo motor `AWE React`

### Criterios de checkbox {#checkbox-criterion}

Muestra una casilla de verificación. Envía un `1` (o el valor definido en el atributo **[value](#general-attributes)**) si está marcado o `0` si está desmarcado.

<img alt="Casillas" src={require('@docusaurus/useBaseUrl').default('img/Checkboxes.png')} />

Con `style="switch"` (se puede combinar con las clases de columna, por ejemplo `style="col-xs-12 col-sm-6 switch"`) los
dos motores dibujan la casilla como un interruptor: una pista con un tirador y la etiqueta a su lado. Envía los mismos
valores (`1`, o el atributo **value**, cuando está activado y `0` cuando no), la tecla Espacio lo cambia, se anuncia como
interruptor (`role="switch"`) con su estado y se deshabilita mientras el criterio es de solo lectura.

```xml
<criteria label="PARAMETER_ACTIVE" id="Sta" component="checkbox" value="1" style="col-xs-12 col-sm-6 switch" />
```

### Criterio del botón de radio {#radio-button-criterion}

Muestra un botón de radio. Es una monoselección. Envía el atributo **[value](#general-attributes)** del elemento seleccionado entre todos los botones de radio con el mismo atributo **[grupo](#specific-attributes)**.

<img alt="Radios" src={require('@docusaurus/useBaseUrl').default('img/Radios.png')} />

> **Nota:** Si queremos que un grupo de botones de radio sea requerido, todos los botones de radio que están dentro del grupo deben tener validación="requerido"

### Criterio de la casilla de verificación de botón (línea) {#button-checkbox-criterion-line}

Similar a una [casilla de verificación](#checkbox-criterion) pero tiene una apariencia de botón. Envía un `1` (o el valor definido en el atributo **[value](#general-attributes)**) si está marcado o `0` si está desmarcado.

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

El cargador tiene dos atributos opcionales, disponibles tanto en el cliente AngularJS como en el de React:

- `drop-zone="true"` muestra un área de arrastre con borde discontinuo y una pista. El usuario puede soltar un archivo
  sobre ella (el diálogo de archivos sigue funcionando, y el botón **Elegir** sigue siendo accesible con el teclado).
  Cuando el archivo es una imagen raster (PNG, JPEG, GIF, WebP, BMP o AVIF) se muestra una vista previa. Los demás
  archivos, incluidas las imágenes SVG, se suben sin vista previa.
- `capture="user|environment"` añade un botón **Hacer foto** que abre la cámara del dispositivo en los navegadores
  móviles (`user` para la cámara frontal, `environment` para la trasera) y muestra una vista previa de la foto. Los
  navegadores sin cámara recurren al diálogo de archivos habitual.

Ambas opciones envían el archivo con la misma petición de subida que el botón **Elegir**, de modo que el servidor sigue
validando el tipo y el tamaño del archivo (`uploadMaxSize`). La vista previa se genera en el navegador a partir del
archivo seleccionado y se libera cuando se borra o se sustituye el archivo, o se cierra la pantalla. Los atributos se
ignoran en un cargador de solo lectura.

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

### Cargador con zona de arrastre y captura de cámara {#uploader-with-drop-zone-and-camera-capture}

```xml
<criteria label="PARAMETER_UPLOADER" id="Photo" component="uploader" destination="photos"
          style="col-xs-12 col-sm-6 col-lg-4" drop-zone="true" capture="environment"/>
```
