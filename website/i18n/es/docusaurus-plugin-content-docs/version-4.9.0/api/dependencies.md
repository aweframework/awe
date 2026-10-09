---
id: dependencies
title: Dependencies
---

Cada componente (criterio, grilla, gráfico, etc) puede tener algunas dependencias dentro de su comportamiento y contenido. Una dependencia puede ser lanzada dependiendo de las condiciones o eventos de los componentes.

:::warning
The element must have the component attribute to make it work. Si el elemento no tiene el atributo del componente, el sistema de dependencias ganó ́t funcionar correctamente.
:::

## Esqueleto de XML {#xml-skeleton}

```xml
<dependency initial="[initial]" source-type="[source-type]" target-type="[target-type]" 
            server-action="[server-action]" target-action="[target-action]" 
            label="[label-to-update]" value="[value]" formule="[formule]" 
            type="[type]" invert="[invert]" async="[async]" silent="[silent]">
  <dependency-element ... />
  ...
  <dependency-action .../>
  ...
</dependency>
```

## Estructura de dependencias {#dependency-structure}

| Elemento                                  | Uso          | Varias instancias | Descripción                                                                        |
| ----------------------------------------- | ------------ | ----------------- | ---------------------------------------------------------------------------------- |
| [dependency](#dependency-attributes)      | **Required** | No                | Nodo global de la dependencia. Define los atributos de dependencia |
| [dependency-element](#dependency-element) | **Required** | Si                | Condición para lanzar la dependencia                                               |
| [dependency-action](#dependency-action)   | **Optional** | Si                | [Action](actions.md) which will be launched if dependency conditions are valid     |

### Atributos de dependencias {#dependency-attributes}

| Atributo                    | Uso      | Tipo    | Descripción                                                                                            | Valores                                                                                                       |
| --------------------------- | -------- | ------- | ------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------- |
| initial                     | Opcional | Boolean | Dependencia se lanzará al cargar la página                                                             | Default value is `false`                                                                                      |
| [source-type](#source-type) | Opcional | String  | Fuente de dependencias (donde se tomarán los datos)                                 | See [source types](#source-type). Default value is `none`                                     |
| [target-type](#target-type) | Opcional | String  | Destino de dependencias (donde se aplicará la dependencia)                          | See [target types](#target-type). Default value is `none`                                     |
| server-action               | Opcional | String  | Server action call (when source-type is `query`)                                    | See [server action list](actions.md#server-actions)                                                           |
| target-action               | Opcional | String  | Target to call on the server / Name of the criterion, to get its value, when source-type is `launcher` |                                                                                                               |
| label                       | Opcional | String  | Etiqueta a aplicar al objetivo                                                                         | **Note:** You can use [i18n](i18n-internationalization.md) files (locales) |
| value                       | Opcional | String  | Valor a aplicar al objetivo                                                                            |                                                                                                               |
| formule                     | Opcional | String  | Formular para obtener el valor que se aplicará al objetivo                                             |                                                                                                               |
| type                        | Opcional | String  | How to evaluate the [conditions](#dependency-element)                                                  | Possible values are `and` (default) and `or`.                              |
| invert                      | Opcional | Boolean | Invert the [condition](#dependency-element) evaluation                                                 | Default value is `false`                                                                                      |
| async                       | Opcional | Boolean | Ejecutar la acción del servidor de forma asincrónica                                                   | Default value is `false`                                                                                      |
| silent                      | Opcional | Boolean | Ejecutar la acción del servidor sin mostrar la barra de carga                                          | Default value is `false`                                                                                      |

### Tipo de fuente {#source-type}

The `source-type` attribute refers to the _place_ from we are retrieving the data to apply to the component.

| Value    | Descripción                                                                                                                                  |
| -------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| none     | No hacer nada (no recuperar valores)                                                                                      |
| query    | Ejecutar una consulta para recuperar valores                                                                                                 |
| launcher | Get the values from the stored alias defined in `target-action`                                                                              |
| value    | Get the values from the `value` attribute defined on the dependency                                                                          |
| label    | Get the values from the `label` attribute defined on the dependency (can be a [i18n](i18n-internationalization.md) local) |
| formule  | Get the values from a evaluated formule on the `formule` attribute defined on the dependency                                                 |
| reset    | Borrar el valor de destino                                                                                                                   |

> **Note:** `source-type="action"` is no more needed. Simplemente añada algunas acciones de dependencia a su dependencia y se lanzarán cuando las condiciones sean válidas.

### Target type

The `target-type` attribute refers to the _place_ where we are applying the data retrieved from the `source-type` _or_ whatever we want to do with the component where the dependency is.

| Action                 | Necesita datos | Acción inversa            | Descripción                                                                                                                                              |
| ---------------------- | -------------- | ------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| none                   | No             | none                      | Se aplica a nada (valor predeterminado)                                                                                               |
| label                  | **Yes**        | none                      | Applies the `source-type` retrieved value to the component label                                                                                         |
| unit                   | **Yes**        | none                      | Applies the `source-type` retrieved value to the component unit label                                                                                    |
| icon                   | **Yes**        | none                      | Applies the `source-type` retrieved value to the component icon                                                                                          |
| input                  | **Yes**        | none                      | Applies the `source-type` retrieved value to the component value                                                                                         |
| format-number          | **Yes**        | **restore number format** | Applies the `source-type` retrieved value to the criterion number format (for [numeric](criteria.md#numeric-criterion) criteria only) |
| validate               | **Yes**        | none                      | Applies the `source-type` retrieved value to the criterion validation                                                                                    |
| atributo               | **Yes**        | none                      | Set an attribute defined on `target-action` with the `source-type` retrieved value                                                                       |
| ver                    | No             | **hide**                  | Mostrar el componente                                                                                                                                    |
| ocultar                | No             | **show**                  | Ocultar el componente                                                                                                                                    |
| mostrar columna        | No             | **hide-column**           | Mostrar la columna (sólo en columnas)                                                                                                 |
| ocultar-columna        | No             | **show-column**           | Ocultar la columna (aplicable sólo en columnas)                                                                                       |
| habilitar              | No             | **disable**               | Activar el componente                                                                                                                                    |
| desactivar             | No             | **enable**                | Desactivar el componente                                                                                                                                 |
| set-requerido          | No             | **set-optional**          | Establecer el criterio como requerido                                                                                                                    |
| establecer-opcional    | No             | **set-required**          | Eliminar el criterio de validación requerido                                                                                                             |
| set-solo lectura       | No             | **set-editable**          | Establecer el criterio como sólo lectura                                                                                                                 |
| set-editable           | No             | **set-readonly**          | Permite editar el criterio                                                                                                                               |
| configurar-visible     | No             | **set-invisible**         | Establecer el componente como visible                                                                                                                    |
| set-invisible          | No             | **set-visible**           | Establece el componente como invisible (similar a ocultar pero manteniendo el criterio oculto)                                        |
| activar-autorefrescar  | **Yes**        | **disable-autorefresh**   | Enable autorefresh and set the autorefresh timer with the `source-type` retrieved value                                                                  |
| desactivar-autorefresh | **Yes**        | **enable-autorefresh**    | Disable autorefresh and set the autorefresh timer with the `source-type` retrieved value                                                                 |

> **Note:** Some `target-type` actions have an _inverse_ action, which is executed when dependency conditions are not accomplished. In this case, it's **NOT** necessary to add another dependency with the inverse action.

## Elemento de dependencias {#dependency-element}

The dependency elements are the definition of the _conditions_ required to launch a dependency.

## Esqueleto del elemento de dependencia {#dependency-element-skeleton}

```xml
<dependency-element id="[id]" column="[column]" attribute="[attribute]" condition="[condition]" row="[row]"
                    id2="[id2]" column2="[column2]" attribute2="[attribute2]" event="[event]" value="[value]" 
                    optional="[optional]" cancel="[cancel]" view="[view]" alias="[alias]"/>
```

### Atributos del elemento de dependencias {#dependency-element-attributes}

| Atributo        | Uso          | Tipo    | Descripción                                                                                                                                                       | Valores                                                                                                              |
| --------------- | ------------ | ------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| id              | **Required** | String  | Component identifier                                                                                                                                              |                                                                                                                      |
| columna         | Opcional     | String  | Identificador de columna del componente (aplicable en las rejillas)                                                                            |                                                                                                                      |
| atributo        | Opcional     | String  | Atributo a comprobar.                                                                                                                             | See [attributes](#attributes) below. Default value is `selected`                                     |
| condition       | Opcional     | String  | Condición de comparación                                                                                                                                          | See [dependency element conditions](#dependency-element-conditions). Default value is `is not empty` |
| fila            | Opcional     | String  | Número de fila de la cuadrícula para comprobar el atributo                                                                                                        |                                                                                                                      |
| id2             | Opcional     | String  | Identificador de componente a comparar                                                                                                                            |                                                                                                                      |
| columna2        | Opcional     | String  | Identificador de columna del componente (aplicable en las rejillas)                                                                            |                                                                                                                      |
| atributo2       | Opcional     | String  | Atributo a comprobar.                                                                                                                             |                                                                                                                      |
| evento          | Opcional     | String  | Evento a comprobar                                                                                                                                                | See [events](#events) below.                                                                         |
| value           | Opcional     | String  | Valor a comprobar                                                                                                                                                 |                                                                                                                      |
| optional        | Opcional     | Boolean | Marcar el elemento como opcional. Es útil usar sus valores incluso si están vacíos                                                                | Default value is `false`                                                                                             |
| cancel          | Opcional     | Boolean | Cancelar la dependencia si este elemento no ha sido comprobado (útil para eventos obligatorios)                                                | Default value is `false`                                                                                             |
| view            | Opcional     | String  | Name of view where element is placed. Used when you need specify in what context the element on which you want to act is located. | Ex.: `view = "base"`                                                                 |
| alias           | Opcional     | String  | Nombre a aplicar al elemento de dependencia. Useful for `launcher` dependency targets                                                             |                                                                                                                      |
| comprobar-valor | Opcional     | Boolean | Marca los valores de este elemento (inicia la dependencia si este elemento cambia)                                                             | Default value is `true`                                                                                              |

#### Condiciones del elemento de dependencias {#dependency-element-conditions}

- `eq` Values are equal
- `ne` Values are not equal
- `contains` Value1 contains value2
- `not contains` Value1 doesn't contain value2
- `ge` Value1 is greater than or equal than value2
- `le` Value1 is less than or equal than value2
- `gt` Value1 is greater than value2
- `lt` Value1 is less than value2
- `in` Value1 is in value2 list
- `not in` Value1 is not in value2 list
- `is empty` Value1 is empty
- `is not empty` Value1 is not empty

### Attributes

Los atributos son todos los datos que se pueden obtener de un componente AWE. Dependiendo del componente, puede recuperar información distinta de cada uno:

| Action                 | Aplicar a                                                            | Descripción                                                                                                                                                          |
| ---------------------- | -------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| visible                | Todos los componentes                                                | `true` if component is visible                                                                                                                                       |
| value                  | [Criteria](criteria.md)                                              | Obtiene el valor del criterio. This is the **default attribute**                                                                                     |
| texto                  | [Criteria](criteria.md)                                              | Gets component _visible_ value                                                                                                                                       |
| label                  | [Criteria](criteria.md)                                              | Obtiene etiqueta de componente                                                                                                                                       |
| unit                   | [Criteria](criteria.md)                                              | Obtiene etiqueta de edición de componente                                                                                                                            |
| editable               | [Criteria](criteria.md)                                              | `true` if component is editable                                                                                                                                      |
| requerido              | [Criteria](criteria.md)                                              | `true` if component is required                                                                                                                                      |
| selectedValues         | [Select and Suggest Multiple](criteria.md#multiple-select-criterion) | Obtiene el número de los valores seleccionados del criterio de selección                                                                                             |
| totalValores           | [Select](criteria.md#select-criterion)                               | Obtiene el número total de valores del criterio de selección                                                                                                         |
| Fila actual            | [Grid](grids.md)                                                     | Obtiene el identificador de fila actual                                                                                                                              |
| valor actual           | [Grid](grids.md)                                                     | Gets `column` value of the current row (not the selected one). Esto es útil para actualizar los valores de fila sin una selección |
| preventar Fila actual  | [Grid](grids.md)                                                     | Obtiene la fila anterior a la fila actual                                                                                                                            |
| prevCurrentRowValue    | [Grid](grids.md)                                                     | Gets `column` value of the previous row to the current row                                                                                                           |
| siguiente Fila actual  | [Grid](grids.md)                                                     | Obtiene la siguiente fila a la fila actual                                                                                                                           |
| siguiente Valor actual | [Grid](grids.md)                                                     | Gets `column` value of the next row to the current row                                                                                                               |
| selectedRow            | [Grid](grids.md)                                                     | Obtiene el identificador de fila seleccionado                                                                                                                        |
| selectedRowValue       | [Grid](grids.md)                                                     | Gets `column` value of the selected row.                                                                                                             |
| anteFila               | [Grid](grids.md)                                                     | Obtiene la fila anterior al identificador seleccionado                                                                                                               |
| prevRowValue           | [Grid](grids.md)                                                     | Gets `column` value of the previous row to the selected one                                                                                                          |
| nextFila               | [Grid](grids.md)                                                     | Obtiene la siguiente fila al identificador seleccionado                                                                                                              |
| nextRowValue           | [Grid](grids.md)                                                     | Gets `column` value of the next row to the selected one                                                                                                              |
| selected               | [Grid](grids.md)                                                     | Número de filas seleccionadas de la cuadrícula                                                                                                                       |
| filas seleccionadas    | [Grid](grids.md)                                                     | Número de filas seleccionadas de la cuadrícula                                                                                                                       |
| modificadas Rows       | [Grid](grids.md)                                                     | Número de filas modificadas de la cuadrícula (en cuadrícula multioperativa)                                                                       |
| totalRows              | [Grid](grids.md)                                                     | Número total de filas de la cuadrícula (registros)                                                                                                |
| hasDataColumn          | [Grid](grids.md)                                                     | `true` if the `column` defined has any data                                                                                                                          |
| emptyDataColumn        | [Grid](grids.md)                                                     | `true` if the `column` defined is empty                                                                                                                              |
| fullDataColumn         | [Grid](grids.md)                                                     | `true` if the `column` defined is full of data                                                                                                                       |

> **Note:** If you want to retrieve a `selectedRowValue` from a grid column which isn't editable, you must add an [event launcher](#events) to make sure the dependency is being launched when selecting a row (i.e. `event="select-row"`)

### Eventos {#events}

Algunos componentes lanzan eventos en algunos casos. Puede capturar esos eventos con un elemento de dependencia para hacer que lancen la dependencia:

| Evento                  | Aplicar a                                                                     | Descripción                                                                 |
| ----------------------- | ----------------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| click                   | [Button](button.md), [Context option](context-menu.md) and [Charts](chart.md) | Lanzado al pulsar el elemento                                               |
| seleccionar-fila        | [Grid](grids.md)                                                              | Se lanzó cuando una fila ha sido seleccionada                               |
| antes guardar-fila      | [Grid](grids.md)                                                              | Lanzado antes de guardar una fila                                           |
| save-row                | [Grid](grids.md)                                                              | Iniciado al guardar una fila                                                |
| después-guardar-fila    | [Grid](grids.md)                                                              | Iniciado después de guardar una fila                                        |
| antes de cancelar fila  | [Grid](grids.md)                                                              | Ejecutado antes de cancelar guardar una fila                                |
| cancelar fila           | [Grid](grids.md)                                                              | Se lanzó al cancelar guardando una fila                                     |
| pos-cancelar-fila       | [Grid](grids.md)                                                              | Ejecutado después de cancelar el guardado de una fila                       |
| delete-row              | [Grid](grids.md)                                                              | Iniciado al eliminar una fila                                               |
| pos-eliminar-fila       | [Grid](grids.md)                                                              | Iniciado después de eliminar una fila                                       |
| add-row                 | [Grid](grids.md)                                                              | Lanzado al agregar una fila                                                 |
| pos-añadir-fila         | [Grid](grids.md)                                                              | Iniciado después de añadir una fila                                         |
| check-records-saved     | [Grid](grids.md)                                                              | Iniciado al comprobar si los registros han sido guardados                   |
| check-records-generated | [Grid](grids.md)                                                              | Iniciado al comprobar si se han generado registros                          |
| check-one-selected      | [Grid](grids.md)                                                              | Se lanzó al comprobar si hay una fila seleccionada                          |
| check-some-selected     | [Grid](grids.md)                                                              | Iniciado al comprobar si hay al menos una fila seleccionada                 |
| doble clic              | [Charts](chart.md)                                                            | Se lanzó al hacer clic dos veces en un punto de gráfico                     |
| context-menu            | [Charts](chart.md)                                                            | Se lanzó cuando se hace clic derecho en un punto de gráfico                 |
| zoom                    | [Charts](chart.md)                                                            | Se lanzó al seleccionar un rango (zooming) en un gráfico |

## Acción de dependencias {#dependency-action}

Dependency actions are actions launched when a dependency has been activated and the dependency `source-type` is `action`. See [actions](actions.md) for more information.

## Ejemplos {#examples}

**When a button is clicked, update a criterion with a formule**

```xml
<dependency initial="true" source-type="formule" target-type="input" 
            formule="'Valor' + (parseInt('[ButSetVa1]'.substr(5,1),10) + parseInt('[ButSetVa2]'.substr(5,1),10))">
  <dependency-element id="ButSetVa3" event="click" />
  <dependency-element id="ButSetVa2" />
  <dependency-element id="ButSetVa1" />
</dependency>
```

**Validate if a column value is already at the database**

```xml
<dependency source-type="query" server-action="unique" target-action="AweKeyUni">
  <dependency-element id="GrdKeyLst" column="KeyNam" attribute="currentRowValue" />
</dependency>
```

**Enable a button when there is one row selected on a grid**

```xml
<dependency target-type="enable" initial="true">
  <dependency-element id="GrdKeyLst" attribute="selectedRows" condition="eq" value="1" />
</dependency>
```

**Change the numeric format when a criterion value is `4decimales`**

```xml
<dependency source-type="formule" target-type="format-number" initial="true" 
            formule="{vMin: '-999999', mDec: [NumHid], aSign:' JPY', pSign:'s', aPad:true}">
  <dependency-element id="Txt" condition="eq" value="4decimales" />
</dependency>
```

**Change the validation of a time criterion**

```xml
<dependency source-type="value" target-type="validate" value="{textLE:['TimReq']}">
  <dependency-element id="CalReq" condition="eq" name="Cal" />
  <dependency-element id="Cal" condition="eq" name="CalReq" />
</dependency>
```

**Launch two actions when a criterion value changes**

```xml
<dependency initial="true">
  <dependency-element id="CalReq" />
  <dependency-action type="server" server-action="data" target-action="ProAllLst" target="SelReq" />
  <dependency-action type="server" server-action="data" target-action="ProAllLst" target="SelMulReq" />
</dependency>
```

**Make a criterion not visible (but keeping the place) when a criterion value is `oculta`**

```xml
<dependency target-type="set-invisible" initial="true">
  <dependency-element id="Txt" condition="eq" value="oculta" />
</dependency>		  
```

**Make a criterion editable (not readonly) when a criterion value is `edita`**

```xml
<dependency target-type="set-editable" initial="true">
  <dependency-element id="Txt" condition="eq" value="edita" />
</dependency>
```

**Fill a criterion unit label with the result of a query when a checkbox gets checked**

```xml
<dependency source-type="query" target-type="unit" server-action="data" target-action="ModSel">
  <dependency-element id="ChkBoxVa2" condition="eq" value="1" />
</dependency>
```

**Show a component when the column value of the selected row of a grid is not equal to `awemadora02`**

```xml
<dependency target-type="show" initial="true">
  <dependency-element id="GrdSta" column="Als" attribute="selectedRowValue" condition="ne" value="awemadora02"/>
  <dependency-element id="GrdSta" event="select-row"/>
</dependency>
```

**Use a dependency just in the selected row**

```xml
<dependency initial="true" target-type="enable">
  <dependency-element id="GridName" attribute="selectedRow" condition="eq" id2="GridName" attribute2="currentRow"/>
</dependency>
```

> **Note:** If you want to retrieve a _selected row value_ from a grid column which isn't editable, you must add an [event launcher](#events) to make sure the dependency is being launched when selecting a row (i.e. `event="select-row"`)

**Validate a criterion using a Java method**

_Dependency_

```xml
<criteria label="PARAMETER_TEXT" id="TxtVal" variable="TxtVal" component="text">
  <dependency source-type="query" server-action="validate" target-action="ValidateBankAccount">
    <dependency-element id="TxtVal"/>
  </dependency>
</criteria>
```

_Query_

```xml
<query id="ValidateBankAccount" service="ValidateBankAccount">
  <variable id="BankAccount" type="STRING" name="TxtVal" />
</query>
```

_Service_

```xml
<service id="ValidateBankAccount">
  <java classname="com.almis.awe.test.Validation" method="validateBankAccount">
    <service-parameter type="STRING" name="BankAccount" />
  </java>
</service>
```

_Java method_

```java
public class Validation {

  // PATTERNS
  private static final String BANK_ACOUNT = "^[0-9]{4}\\-[0-9]{4}\\-[0-9]{2}\\-[0-9]{10}$";
  private static final Pattern BANK_ACCOUNT_PATTERN = Pattern.compile(BANK_ACOUNT, 0);

  public ServiceData validateBankAccount(String bankAccount) throws AWException {
    ServiceData check = new ServiceData();
    Matcher matchAccount = BANK_ACCOUNT_PATTERN.matcher(bankAccount);
    if (!matchAccount.matches()) {
      check.setType(AnswerType.WARNING);
      check.setMessage(LocalSingleton.getInstance().getLocal("ERROR_MESSAGE_WRONG_BANK_ACCOUNT", bankAccount));
    }
    return check;
  }
}
```

### Añadir una clase a un elemento HTML cuando ocurre un evento {#add-a-class-to-a-html-element-when-an-event-occurs}

<img alt="Bell Animation" src={require('@docusaurus/useBaseUrl').default('img/Bell_Animation.gif')} />

A veces necesitamos mostrar un consejo en la pantalla para notar a los usuarios que algo ha sucedido en el back-end como una nueva entrada en la base de datos o similar. Vamos a explicar cómo agregar dos nombres de clase ("animado" y "faa-slow" para ajustar la velocidad animada) a un elemento HTML para animar un icono de campana y mostrar un número sobre el icono que representa una serie de errores en la aplicación. Al pulsar este botón, la campana detiene la animación y muestra un diálogo con una cuadrícula que muestra los detalles del error.

**Definition of the screen elements**

Primero debemos definir el elemento HTML en la pantalla xml, en este caso un elemento "info-button" con un atributo "icon" con un valor "bello" y un atributo "unit". Este último atributo representará el número de errores en la aplicación.

```xml
  <info-button style="nav-icon-btn-danger" title="BUTTON_NOTIFICATIONS" icon="bell" id="Warnings" unit="">
    ...
  </info-button>
```

El siguiente paso es agregar la acción para detener la animación de la campana (quitar el nombre de la clase) y mostrar el diálogo.

```xml
  <info-button style="nav-icon-btn-danger" title="BUTTON_NOTIFICATIONS" icon="bell" id="Warnings" unit="">
    <button-action type="remove-class" target-action="animated faa-slow" target="#Warnings .fa-bell" />
    <button-action type="dialog" target="MessageWarnings" />
  </info-button>
```

La primera acción del botón elimina los nombres de las clases "animadas" y "faa-slow" del elemento HTML apuntado en el atributo "target", en este caso el elemento con el nombre de clase "fa-bell" dentro del elemento con el id "Advertencias".

La segunda acción del botón abre el diálogo con el id "MessageWarnings".

Estas acciones se activan cuando el usuario hace clic en el botón.

Ahora debemos agregar las dependencias requeridas para mostrar los números de errores y agregar los nombres de clase "animados" y "faa-slow". Estas acciones deben ejecutarse cuando los usuarios entren en la pantalla o inicien sesión en la aplicación para que podamos añadir una acción aditiva.

```xml
<info-button style="nav-icon-btn-danger" title="BUTTON_NOTIFICATIONS" icon="bell" id="Warnings" unit="">
  <button-action type="remove-class" target-action="animated faa-slow" target="#Warnings .fa-bell" />
  <button-action type="dialog" target="MessageWarnings" />
  <dependency source-type="launcher" target-type="unit" target-action="notification" initial="true">
    <dependency-element id="NotificationNumber" alias="notification"/>
  </dependency>
  <dependency>
   <dependency-element id="NotificationNumber" attribute="value" condition="ne" value="0" />
   <dependency-action type="add-class" target-action="animated faa-slow" target="#Warnings .fa-bell" />
  </dependency>
</info-button>
```

La primera dependencia es un tipo de "lanzador" que se ejecuta cuando el usuario entra en la pantalla donde este elemento se define gracias al atributo "inicial" establecido en "verdadero". La dependencia, apunta al atributo "unit" del botón info-button y depende de un elemento con un id "NotificationNumber" representado en la etiqueta "dependency-element". Este nuevo elemento se creará en los siguientes pasos.

La segunda dependencia es un tipo de "acción". Esta acción se activará cuando el valor del "elemento de dependencia" (el elemento con un identificador de "NotificationNumber") no sea igual (ne) a cero. Esto se representa con los siguientes atributos: attribute="value" condition="ne" value="0".
La acción para esta dependencia es un tipo "add-class", así que cuando se ejecuta esta acción, se añadirá un nombre de clase a un elemento. Esta acción apunta al elemento con el nombre de la clase "fa-bell" dentro del elemento con el id "Advertencias" (atributo "target" en la etiqueta de dependencia-acción), por lo que este elemento recibirá el nombre de la clase descrito en el atributo "target-action" ("animado faa-slow").

Ahora vamos a definir el elemento de dependencia "NotificationNumber". Este elemento será un criterio oculto y tiene un atributo de "carga inicial" definido a "valor" y una "acción objetiva" que apunta a una consulta que debe definirse en "Consultas. ml" en AWE.

```xml
<criteria id="NotificationNumber" component="hidden" initial-load="value" target-action="getNotificationErrors" />
```

Y por último, pero no menos importante, la definición del diálogo.

```xml
<dialog id="MessageWarnings" modal="true" style="modal-lg" label="SCREEN_TEXT_WARNINGS" icon="exclamation-triangle" help="HELP_SCREEN_TEXT_PRINT_EMAIL">
  <tag type="div" style="modal-body row">
    <window label="BUTTON_NOTIFICATIONS" icon="bell" expandible="vertical">
      <grid id="GridMessageWarning" style="height-lg" max="50" server-action="data-silent" target-action="getErrorMessageInfo" autoload="true" autorefresh="30">
        ...
        columns definition
        ...
      </grid>
    </window>
  </tag>
</dialog>
```

En este punto hemos creado todos los elementos necesarios en la pantalla. El siguiente paso es definir las consultas que extraen los datos de la base de datos.

**Queries**

En el archivo "Queries.xml" debemos definir las siguientes etiquetas:

```xml
<query id="getErrorMessageInfo" service="getErrorMessageInfo"/>
```

```xml
<query id="getNotificationErrors" service="getErrorMessageInfo">
  <field id="errors" alias="value" />
  <field id="errors" alias="label" />
</query>
```

La primera consulta apunta a un servicio llamado "getErrorMessageInfo" definido en el archivo "Services.xml" en AWE. Esta consulta será lanzada por la cuadrícula creada en el diálogo.

La segunda consulta apunta al mismo servicio y espera dos valores uno para un campo "valor" y otro para un campo "etiqueta". Esta consulta será lanzada por los criterios ocultos.

_Service_

Una vez que las consultas están definidas y apuntan a un servicio, debemos crear este servicio. En el archivo "Services.xml" debemos definir las siguientes etiquetas:

```xml
<service id="getErrorMessageInfo">
  <java classname="com.isban.smgmi.web.controller.ErrorMessageController" method="getErrorMessageInfo">
  </java>
</service>
```

Este servicio ejecuta el método "getErrorMessageInfo" en la clase "com.isban.smgmi.web.controller.ErrorMessageController".

**Java**

En el Controlador debemos crear el método "getErrorMessageInfo" y devolver un objeto ServiceData. Este controlador debe llamar a una clase Manager.

```java
public class ErrorMessageController extends AWEController {
  public ServiceData getErrorMessageInfo() throws AWException {

    // Get event manager
    ErrorMessageManager errorMessageManager = new ErrorMessageManager();

    return errorMessageManager.getErrorMessageInfo();
  }
}
```

En la definición de Gestor debemos crear una acción de llenado del cliente para cargar datos en la cuadrícula de diálogo y una acción de cliente seleccionada para cargar datos en criterios ocultos.

```java
public class ErrorMessageManager extends AWEManager {

  /**
   * Get error info
   * 
   * @return
   * @throws AWException
   */
  public ServiceData getErrorMessageInfo() throws AWException {
    ...
          ClientAction fillErrorGrdAction = new ClientAction("fill");
          fillErrorGrdAction.setSilent("true");
          fillErrorGrdAction.setTarget("GridMessageWarning");

          DataList errorGrdParametersDatalist = new DataList();
          Integer rowIndex = 0;
          for (ErrorBean error : errorList) {
            HashMap<String, CellData> parametersRow = new HashMap<String, CellData>();
            parametersRow.put("id", new CellData(rowIndex));

            ...
            // add columns data
            ...

            errorGrdParametersDatalist.getRows().add(parametersRow);
            rowIndex++;
          }
          // Set records
          errorGrdParametersDatalist.setRecords(rowIndex);

          fillErrorGrdAction.addParameter("datalist", errorGrdParametersDatalist);

          // Notification Number
          ClientAction selectCrtNotificationNumberAction = new ClientAction("select");
          selectCrtNotificationNumberAction.setTarget("NotificationNumber");
          selectCrtNotificationNumberAction.setSilent("true");

          errorListSize = errorList.size();
          ArrayNode notifNumberArrayNode = JsonNodeFactory.instance.arrayNode();
          notifNumberArrayNode.add(String.valueOf(errorListSize));
          selectCrtNotificationNumberAction.addParameter("values", new CellData(notifNumberArrayNode));

          // Adds clients actions
          serviceData.addClientAction(selectCrtNotificationNumberAction);
          serviceData.addClientAction(fillErrorGrdAction);
    ...
  }

```