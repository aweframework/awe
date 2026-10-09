---
id: dependencies
title: Dependencias
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

| Elemento                                  | Uso             | Varias instancias | Descripción                                                                        |
| ----------------------------------------- | --------------- | ----------------- | ---------------------------------------------------------------------------------- |
| [dependency](#dependency-attributes)      | **Obligatorio** | No                | Nodo global de la dependencia. Define los atributos de dependencia                 |
| [dependency-element](#dependency-element) | **Obligatorio** | Si                | Condición para lanzar la dependencia                                               |
| [dependency-action](#dependency-action)   | **Opcional**    | Si                | [Acción](actions.md) que se iniciará si las condiciones de dependencia son válidas |

### Atributos de dependencias {#dependency-attributes}

| Atributo                    | Uso      | Tipo    | Descripción                                                                                                           | Valores                                                                       |
| --------------------------- | -------- | ------- | --------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| initial                     | Opcional | Boolean | Dependencia se lanzará al cargar la página                                                                            | El valor predeterminado es `falso`                                            |
| [source-type](#source-type) | Opcional | String  | Fuente de dependencias (donde se tomarán los datos)                                                                   | Ver [tipos de fuente](#source-type). El valor predeterminado no es `ninguno`  |
| [target-type](#target-type) | Opcional | String  | Destino de dependencias (donde se aplicará la dependencia)                                                            | Ver [tipos de destino](#target-type). El valor predeterminado no es `ninguno` |
| server-action               | Opcional | String  | Llamada de acción del servidor (cuando el tipo de fuente es `consulta`)                                               | Ver [lista de acciones del servidor](actions.md#server-actions)               |
| target-action               | Opcional | String  | Objetivo para llamar al servidor / Nombre del criterio, para obtener su valor, cuando el tipo de fuente es `launcher` |                                                                               |
| label                       | Opcional | String  | Etiqueta a aplicar al objetivo                                                                                        | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)    |
| value                       | Opcional | String  | Valor a aplicar al objetivo                                                                                           |                                                                               |
| formule                     | Opcional | String  | Formular para obtener el valor que se aplicará al objetivo                                                            |                                                                               |
| type                        | Opcional | String  | Cómo evaluar las condiciones [](#dependency-element)                                                                  | Los valores posibles son `y` (por defecto) y `o`.                             |
| invert                      | Opcional | Boolean | Invierte la evaluación de la condición [](#dependency-element)                                                        | El valor predeterminado es `falso`                                            |
| async                       | Opcional | Boolean | Ejecutar la acción del servidor de forma asincrónica                                                                  | El valor predeterminado es `falso`                                            |
| silent                      | Opcional | Boolean | Ejecutar la acción del servidor sin mostrar la barra de carga                                                         | El valor predeterminado es `falso`                                            |

### Tipo de fuente {#source-type}

El atributo `tipo fuente` se refiere al *lugar* de que estamos recuperando los datos para aplicar al componente.

| Value    | Descripción                                                                                                                   |
| -------- | ----------------------------------------------------------------------------------------------------------------------------- |
| none     | No hacer nada (no recuperar valores)                                                                                          |
| query    | Ejecutar una consulta para recuperar valores                                                                                  |
| launcher | Obtener los valores del alias almacenado definido en `target-action`                                                          |
| value    | Obtener los valores del atributo `value` definido en la dependencia                                                           |
| label    | Obtener los valores del atributo `label` definido en la dependencia (puede ser un [i18n](i18n-internationalization.md) local) |
| formule  | Obtener los valores de un formulario evaluado en el atributo `formule` definido en la dependencia                             |
| reset    | Borrar el valor de destino                                                                                                    |

> **Nota:** `source-type="action"` no es más necesario. Simplemente añada algunas acciones de dependencia a su dependencia y se lanzarán cuando las condiciones sean válidas.

### Target type

El atributo `target-type` se refiere al *lugar* donde estamos aplicando los datos recuperados del `tipo fuente` *o* lo que queramos hacer con el componente donde está la dependencia.

| Acción                 | Necesita datos | Acción inversa                  | Descripción                                                                                                                                |
| ---------------------- | -------------- | ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| none                   | No             | Ninguna                         | Se aplica a nada (valor predeterminado)                                                                                                    |
| label                  | **Si**         | Ninguna                         | Se aplica el valor recuperado del tipo de fuente `` a la etiqueta del componente                                                           |
| unit                   | **Si**         | none                            | Se aplica el valor recuperado del tipo de fuente `` a la etiqueta de unidad de componente                                                  |
| icon                   | **Si**         | none                            | Se aplica el valor recuperado del tipo de fuente `` al icono del componente                                                                |
| input                  | **Si**         | none                            | Se aplica el valor recuperado del tipo de fuente `` al valor del componente                                                                |
| format-number          | **Si**         | **restaurar formato de número** | Se aplica el valor de `tipo fuente` al formato de número de criterio (para [criterios](criteria.md#numeric-criterion) numéricos solamente) |
| validate               | **Si**         | none                            | Se aplica el valor recuperado del tipo de fuente `` al criterio de validación                                                              |
| atributo               | **Si**         | none                            | Establece un atributo definido en `target-action` con el valor recuperado `del tipo de fuente`                                             |
| ver                    | No             | **ocultar**                     | Mostrar el componente                                                                                                                      |
| ocultar                | No             | **ver**                         | Ocultar el componente                                                                                                                      |
| mostrar columna        | No             | **ocultar-columna**             | Mostrar la columna (sólo en columnas)                                                                                                      |
| ocultar-columna        | No             | **mostrar columna**             | Ocultar la columna (aplicable sólo en columnas)                                                                                            |
| habilitar              | No             | **desactivar**                  | Activar el componente                                                                                                                      |
| desactivar             | No             | **habilitar**                   | Desactivar el componente                                                                                                                   |
| set-requerido          | No             | **establecer-opcional**         | Establecer el criterio como requerido                                                                                                      |
| establecer-opcional    | No             | **set-requerido**               | Eliminar el criterio de validación requerido                                                                                               |
| set-solo lectura       | No             | **set-editable**                | Establecer el criterio como sólo lectura                                                                                                   |
| set-editable           | No             | **set-solo lectura**            | Permite editar el criterio                                                                                                                 |
| configurar-visible     | No             | **set-invisible**               | Establecer el componente como visible                                                                                                      |
| set-invisible          | No             | **configurar-visible**          | Establece el componente como invisible (similar a ocultar pero manteniendo el criterio oculto)                                             |
| activar-autorefrescar  | **Si**         | **desactivar-autorefresh**      | Habilita la autorefresh y establece el temporizador automático con el valor recuperado del tipo de fuente ``                               |
| desactivar-autorefresh | **Si**         | **activar-autorefrescar**       | Deshabilita la autorefresca y establece el temporizador automático con el valor recuperado del tipo de fuente ``                           |

> **Note:** Some `target-type` actions have an *inverse* action, which is executed when dependency conditions are not accomplished. En este caso, **NO** es necesario añadir otra dependencia con la acción inversa.

## Elemento de dependencias {#dependency-element}

Los elementos de dependencia son la definición de las *condiciones* necesarias para lanzar una dependencia.

## Esqueleto del elemento de dependencia {#dependency-element-skeleton}

```xml 
<dependency-element id="[id]" column="[column]" attribute="[attribute]" condition="[condition]" row="[row]"
                    id2="[id2]" column2="[column2]" attribute2="[attribute2]" event="[event]" value="[value]" 
                    optional="[optional]" cancel="[cancel]" view="[view]" alias="[alias]"/>
```

### Atributos del elemento de dependencias {#dependency-element-attributes}

| Atributo        | Uso             | Tipo    | Descripción                                                                                                                       | Valores                                                                                                                        |
| --------------- | --------------- | ------- | --------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| id              | **Obligatorio** | String  | Component identifier                                                                                                              |                                                                                                                                |
| columna         | Opcional        | String  | Identificador de columna del componente (aplicable en las rejillas)                                                               |                                                                                                                                |
| atributo        | Opcional        | String  | Atributo a comprobar.                                                                                                             | Ver [atributos](#attributes) a continuación. El valor predeterminado es `seleccionado`                                         |
| condition       | Opcional        | String  | Condición de comparación                                                                                                          | Vea las condiciones del elemento de dependencia [](#dependency-element-conditions). El valor predeterminado es `no está vacío` |
| fila            | Opcional        | String  | Número de fila de la cuadrícula para comprobar el atributo                                                                        |                                                                                                                                |
| id2             | Opcional        | String  | Identificador de componente a comparar                                                                                            |                                                                                                                                |
| columna2        | Opcional        | String  | Identificador de columna del componente (aplicable en las rejillas)                                                               |                                                                                                                                |
| atributo2       | Opcional        | String  | Atributo a comprobar.                                                                                                             |                                                                                                                                |
| evento          | Opcional        | String  | Evento a comprobar                                                                                                                | Ver [eventos](#events) a continuación.                                                                                         |
| value           | Opcional        | String  | Valor a comprobar                                                                                                                 |                                                                                                                                |
| optional        | Opcional        | Boolean | Marcar el elemento como opcional. Es útil usar sus valores incluso si están vacíos                                                | El valor predeterminado es `falso`                                                                                             |
| cancel          | Opcional        | Boolean | Cancelar la dependencia si este elemento no ha sido comprobado (útil para eventos obligatorios)                                   | El valor predeterminado es `falso`                                                                                             |
| view            | Opcional        | String  | Name of view where element is placed. Used when you need specify in what context the element on which you want to act is located. | Ex.: `view = "base"`                                                                                                           |
| alias           | Opcional        | String  | Nombre a aplicar al elemento de dependencia. Útil para `objetivos de dependencia` del launcher                                    |                                                                                                                                |
| comprobar-valor | Opcional        | Boolean | Marca los valores de este elemento (inicia la dependencia si este elemento cambia)                                                | El valor predeterminado es `verdadero`                                                                                         |

#### Condiciones del elemento de dependencias {#dependency-element-conditions}

- `eq` Valores son iguales
- `ne` Valores no son iguales
- `contiene` El valor 1 contiene el valor 2
- `no contiene` El valor 1 no contiene el valor 2
- `ge` Value1 es mayor o igual que value2
- `le` Value1 es menor o igual que el valor 2
- `gt` El valor 1 es mayor que el valor 2
- `lt` El valor 1 es menor que el valor 2
- `en` Value1 está en la lista de valor2
- `no en` Value1 no está en la lista de valor2
- `está vacío` Value1 está vacío
- `no está vacío` Value1 no está vacío

### Attributes

Los atributos son todos los datos que se pueden obtener de un componente AWE. Dependiendo del componente, puede recuperar información distinta de cada uno:

| Action                 | Aplicar a                                                                | Descripción                                                                                                                           |
| ---------------------- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------- |
| visible                | Todos los componentes                                                    | `verdadero` si el componente es visible                                                                                               |
| value                  | [Criteria](criteria.md)                                                  | Obtiene el valor del criterio. Este es el atributo predeterminado ****                                                                |
| texto                  | [Criteria](criteria.md)                                                  | Obtiene el componente *valor* visible                                                                                                 |
| label                  | [Criteria](criteria.md)                                                  | Obtiene etiqueta de componente                                                                                                        |
| unit                   | [Criteria](criteria.md)                                                  | Obtiene etiqueta de edición de componente                                                                                             |
| editable               | [Criteria](criteria.md)                                                  | `verdadero` si el componente es editable                                                                                              |
| requerido              | [Criteria](criteria.md)                                                  | `verdadero` si el componente es requerido                                                                                             |
| selectedValues         | [Seleccionar y Sugerir Múltiples](criteria.md#multiple-select-criterion) | Obtiene el número de los valores seleccionados del criterio de selección                                                              |
| totalValores           | [Seleccionar](criteria.md#select-criterion)                              | Obtiene el número total de valores del criterio de selección                                                                          |
| Fila actual            | [Tabla](grids.md)                                                        | Obtiene el identificador de fila actual                                                                                               |
| valor actual           | [Tabla](grids.md)                                                        | Obtiene `valor de columna` de la fila actual (no la seleccionada). Esto es útil para actualizar los valores de fila sin una selección |
| preventar Fila actual  | [Tabla](grids.md)                                                        | Obtiene la fila anterior a la fila actual                                                                                             |
| prevCurrentRowValue    | [Tabla](grids.md)                                                        | Obtiene `valor de columna` de la fila anterior a la fila actual                                                                       |
| siguiente Fila actual  | [Tabla](grids.md)                                                        | Obtiene la siguiente fila a la fila actual                                                                                            |
| siguiente Valor actual | [Tabla](grids.md)                                                        | Obtiene `valor de columna` de la siguiente fila a la fila actual                                                                      |
| selectedRow            | [Tabla](grids.md)                                                        | Obtiene el identificador de fila seleccionado                                                                                         |
| selectedRowValue       | [Tabla](grids.md)                                                        | Obtiene `valor de columna` de la fila seleccionada.                                                                                   |
| anteFila               | [Tabla](grids.md)                                                        | Obtiene la fila anterior al identificador seleccionado                                                                                |
| prevRowValue           | [Tabla](grids.md)                                                        | Obtiene `valor de columna` de la fila anterior a la seleccionada                                                                      |
| nextFila               | [Tabla](grids.md)                                                        | Obtiene la siguiente fila al identificador seleccionado                                                                               |
| nextRowValue           | [Tabla](grids.md)                                                        | Obtiene `valor de columna` de la siguiente fila a la seleccionada                                                                     |
| selected               | [Tabla](grids.md)                                                        | Número de filas seleccionadas de la cuadrícula                                                                                        |
| filas seleccionadas    | [Tabla](grids.md)                                                        | Número de filas seleccionadas de la cuadrícula                                                                                        |
| modificadas Rows       | [Tabla](grids.md)                                                        | Número de filas modificadas de la cuadrícula (en cuadrícula multioperativa)                                                           |
| totalRows              | [Tabla](grids.md)                                                        | Número total de filas de la cuadrícula (registros)                                                                                    |
| hasDataColumn          | [Tabla](grids.md)                                                        | `verdadero` si la columna `` definida tiene algún dato                                                                                |
| emptyDataColumn        | [Tabla](grids.md)                                                        | `verdadero` si la columna `` definida está vacía                                                                                      |
| fullDataColumn         | [Tabla](grids.md)                                                        | `verdadero` si la columna `` definida está llena de datos                                                                             |

> **Nota:** Si desea recuperar un `selectedRowValue` de una columna cuadrícula que no es editable, debe añadir un [lanzador de eventos](#events) para asegurarse de que la dependencia está siendo lanzada al seleccionar una fila (i. . `event="select-row"`)

### Eventos {#events}

Algunos componentes lanzan eventos en algunos casos. Puede capturar esos eventos con un elemento de dependencia para hacer que lancen la dependencia:

| Evento                  | Aplicar a                                                                        | Descripción                                                 |
| ----------------------- | -------------------------------------------------------------------------------- | ----------------------------------------------------------- |
| click                   | [Botón](button.md), [Opciones de contexto](context-menu.md) y [Cartas](chart.md) | Lanzado al pulsar el elemento                               |
| seleccionar-fila        | [Tabla](grids.md)                                                                | Se lanzó cuando una fila ha sido seleccionada               |
| antes guardar-fila      | [Tabla](grids.md)                                                                | Lanzado antes de guardar una fila                           |
| save-row                | [Tabla](grids.md)                                                                | Iniciado al guardar una fila                                |
| después-guardar-fila    | [Tabla](grids.md)                                                                | Iniciado después de guardar una fila                        |
| antes de cancelar fila  | [Tabla](grids.md)                                                                | Ejecutado antes de cancelar guardar una fila                |
| cancelar fila           | [Tabla](grids.md)                                                                | Se lanzó al cancelar guardando una fila                     |
| pos-cancelar-fila       | [Tabla](grids.md)                                                                | Ejecutado después de cancelar el guardado de una fila       |
| delete-row              | [Tabla](grids.md)                                                                | Iniciado al eliminar una fila                               |
| pos-eliminar-fila       | [Tabla](grids.md)                                                                | Iniciado después de eliminar una fila                       |
| add-row                 | [Tabla](grids.md)                                                                | Lanzado al agregar una fila                                 |
| pos-añadir-fila         | [Tabla](grids.md)                                                                | Iniciado después de añadir una fila                         |
| check-records-saved     | [Tabla](grids.md)                                                                | Iniciado al comprobar si los registros han sido guardados   |
| check-records-generated | [Tabla](grids.md)                                                                | Iniciado al comprobar si se han generado registros          |
| check-one-selected      | [Tabla](grids.md)                                                                | Se lanzó al comprobar si hay una fila seleccionada          |
| check-some-selected     | [Tabla](grids.md)                                                                | Iniciado al comprobar si hay al menos una fila seleccionada |
| doble clic              | [Charts](chart.md)                                                               | Se lanzó al hacer clic dos veces en un punto de gráfico     |
| context-menu            | [Charts](chart.md)                                                               | Se lanzó cuando se hace clic derecho en un punto de gráfico |
| zoom                    | [Charts](chart.md)                                                               | Se lanzó al seleccionar un rango (zooming) en un gráfico    |

## Acción de dependencias {#dependency-action}

Las acciones de dependencias son acciones lanzadas cuando una dependencia ha sido activada y la dependencia `tipo fuente` es `acción`. Vea [acciones](actions.md) para más información.

## Ejemplos {#examples}

**Cuando se hace clic en un botón, actualiza un criterio con un formulario**
```xml 
<dependency initial="true" source-type="formule" target-type="input" 
            formule="'Valor' + (parseInt('[ButSetVa1]'.substr(5,1),10) + parseInt('[ButSetVa2]'.substr(5,1),10))">
  <dependency-element id="ButSetVa3" event="click" />
  <dependency-element id="ButSetVa2" />
  <dependency-element id="ButSetVa1" />
</dependency>
```


**Validar si un valor de columna ya está en la base de datos**
```xml 
<dependency source-type="query" server-action="unique" target-action="AweKeyUni">
  <dependency-element id="GrdKeyLst" column="KeyNam" attribute="currentRowValue" />
</dependency>
```


**Activar un botón cuando hay una fila seleccionada en una cuadrícula**
```xml
<dependency target-type="enable" initial="true">
  <dependency-element id="GrdKeyLst" attribute="selectedRows" condition="eq" value="1" />
</dependency>
```


**Cambia el formato numérico cuando un valor de criterio es `4decimales`**
```xml
<dependency source-type="formule" target-type="format-number" initial="true" 
            formule="{vMin: '-999999', mDec: [NumHid], aSign:' JPY', pSign:'s', aPad:true}">
  <dependency-element id="Txt" condition="eq" value="4decimales" />
</dependency>
```


**Cambiar la validación de un criterio de tiempo**
```xml
<dependency source-type="value" target-type="validate" value="{textLE:['TimReq']}">
  <dependency-element id="CalReq" condition="eq" name="Cal" />
  <dependency-element id="Cal" condition="eq" name="CalReq" />
</dependency>
```


**Iniciar dos acciones cuando un valor de criterio cambia**
```xml
<dependency initial="true">
  <dependency-element id="CalReq" />
  <dependency-action type="server" server-action="data" target-action="ProAllLst" target="SelReq" />
  <dependency-action type="server" server-action="data" target-action="ProAllLst" target="SelMulReq" />
</dependency>
```


**Hacer que un criterio no sea visible (pero manteniendo el lugar) cuando un valor de criterio es `oculta`**
```xml
<dependency target-type="set-invisible" initial="true">
  <dependency-element id="Txt" condition="eq" value="oculta" />
</dependency>         
```


**Hacer un criterio editable (no solo lectura) cuando un valor de criterio es `edita`**
```xml
<dependency target-type="set-editable" initial="true">
  <dependency-element id="Txt" condition="eq" value="edita" />
</dependency>
```


**Rellena una etiqueta unitaria de criterio con el resultado de una consulta cuando se comprueba una casilla de verificación**
```xml
<dependency source-type="query" target-type="unit" server-action="data" target-action="ModSel">
  <dependency-element id="ChkBoxVa2" condition="eq" value="1" />
</dependency>
```


**Mostrar un componente cuando el valor de la columna de la fila seleccionada de una cuadrícula no es igual a `awemadora02`**
```xml
<dependency target-type="show" initial="true">
  <dependency-element id="GrdSta" column="Als" attribute="selectedRowValue" condition="ne" value="awemadora02"/>
  <dependency-element id="GrdSta" event="select-row"/>
</dependency>
```

**Usar una dependencia sólo en la fila seleccionada**
```xml
<dependency initial="true" target-type="enable">
  <dependency-element id="GridName" attribute="selectedRow" condition="eq" id2="GridName" attribute2="currentRow"/>
</dependency>
```

> **Nota:** Si desea recuperar un *valor de fila seleccionado* de una columna de cuadrícula que no es editable, debe añadir un [lanzador de eventos](#events) para asegurarse de que la dependencia está siendo lanzada al seleccionar una fila (i. . `event="select-row"`)


**Validar un criterio utilizando un método Java**

*Dependencia*
```xml
<criteria label="PARAMETER_TEXT" id="TxtVal" variable="TxtVal" component="text">
  <dependency source-type="query" server-action="validate" target-action="ValidateBankAccount">
    <dependency-element id="TxtVal"/>
  </dependency>
</criteria>
```

*Query*
```xml
<query id="ValidateBankAccount" service="ValidateBankAccount">
  <variable id="BankAccount" type="STRING" name="TxtVal" />
</query>
```

*Service*
```xml
<service id="ValidateBankAccount">
  <java classname="com.almis.awe.test.Validation" method="validateBankAccount">
    <service-parameter type="STRING" name="BankAccount" />
  </java>
</service>
```

*Método Java*
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

<img alt="Animación de Campanas" src={require('@docusaurus/useBaseUrl').default('img/Bell_Animation.gif')} />

A veces necesitamos mostrar un consejo en la pantalla para notar a los usuarios que algo ha sucedido en el back-end como una nueva entrada en la base de datos o similar. Vamos a explicar cómo agregar dos nombres de clase ("animado" y "faa-slow" para ajustar la velocidad animada) a un elemento HTML para animar un icono de campana y mostrar un número sobre el icono que representa una serie de errores en la aplicación. Al pulsar este botón, la campana detiene la animación y muestra un diálogo con una cuadrícula que muestra los detalles del error.


**Definición de los elementos de la pantalla**

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

La segunda dependencia es un tipo de "acción". Esta acción se activará cuando el valor del "elemento de dependencia" (el elemento con un identificador de "NotificationNumber") no sea igual (ne) a cero. Esto se representa con los siguientes atributos: attribute="value" condition="ne" value="0". La acción para esta dependencia es un tipo "add-class", así que cuando se ejecuta esta acción, se añadirá un nombre de clase a un elemento. Esta acción apunta al elemento con el nombre de la clase "fa-bell" dentro del elemento con el id "Advertencias" (atributo "target" en la etiqueta de dependencia-acción), por lo que este elemento recibirá el nombre de la clase descrito en el atributo "target-action" ("animado faa-slow").

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

*Service*

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