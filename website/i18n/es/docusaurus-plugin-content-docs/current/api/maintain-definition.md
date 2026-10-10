---
id: maintain
title: Definición de maintain
sidebar_label: Definición de maintain
---

El motor de maintain se usa para realizar operaciones que no devuelven datos. Estas operaciones sirven para actualizar datos en el servidor. **Solo devuelven** el resultado del proceso: si la operación ha ido **bien** o si ha habido un **problema** al realizarla.

:::tip
Cada elemento y atributo de los targets de maintain está listado en la [referencia XSD](/reference/maintain) generada.
:::

Hay varios tipos de operaciones maintain: insert, update, delete y operaciones múltiples. Además, una operación maintain puede llamar a un servicio para hacer el trabajo (enviar un email, imprimir un informe, ...).

Puedes poner tantas operaciones como quieras dentro de un target. Por ejemplo, una operación insert seguida de dos operaciones update.

<img alt="Motor de maintain" src={require('@docusaurus/useBaseUrl').default('img/Maintain_engine.png')}/>

:::info
**Nota:** Todos los maintains se definen en el archivo `Maintain.xml` de la **carpeta global**. Consulta la [estructura de proyecto](../guides/project-structure.md#global-folder) para más información.
:::

## Maintain {#maintain}

Esta sección describe cómo se realizan las operaciones con el motor de maintain de AWE.

### Estructura XML del maintain {#xml-maintain-structure}

La estructura del maintain simple es la siguiente:

```xml
<target name="[target_name]" public="[public]">
  <[OPERATION] multiple="[multiple]" audit="[audit_table]">
    <table id="[table_name]" schema="[schema_name]"/>
    <field id="[field_name]" table="[table_name]" variable="[field_variable_name]" sequence="[sequence_name]"/>
    <field id="[field_name]" table="[table_name]" variable="[field_variable_name]" auto-incremental="[true/false]"/>
    ... (more fields)
    <where>
      <and>
        <filter left-field="[Field id]" condition="[Condition]" right-variable="[Variable]"/>
        ... (more filters)
        <filter left-field="[Field id]" condition="[Condition]" right-variable="[Variable]"/>
      </and>
    </where>
    <variable id="[variable_name]" type="[variable_type]" name="[parameter_name]" list="[variable_list]"/>
    <variable id="[variable_name]" type="[variable_type]" value="[static_value]" />
    <variable id="[variable_name]" type="[variable_type]" session="[session_variable]"/>
    ... (more variables)

  </[OPERATION]>
</target>

```

> *Nota:* [OPERATION] puede ser cualquiera de los siguientes valores: `insert`, `delete`, `update`, `multiple`, `commit`, `send-email`, `queue`, `serve` o `retrieve-data`.

### Elemento maintain {#maintain-element}

Para facilitar el desarrollo de maintain, no todos los elementos son obligatorios.

| Elemento                        | Uso           | Varias instancias   | Descripción                                                                                 |
|---------------------------------|---------------|---------------------|---------------------------------------------------------------------------------------------|
| [target](#target-element)       | **Obligatorio** | No                | Describe el nombre de la operación                                                          |
| [insert](#insert)               | Opcional      | Si                  | Se usa para hacer operaciones de inserción en base de datos                                 |
| [update](#update)               | Opcional      | Si                  | Se usa para hacer operaciones de actualización en base de datos                             |
| [delete](#delete)               | Opcional      | Si                  | Se usa para hacer operaciones de borrado en base de datos                                   |
| [multiple](#multiple)           | Opcional      | Si                  | Se usa para hacer múltiples operaciones (insert, delete o update) en base de datos de **una sola vez** |
| [commit](#commit)               | Opcional      | Si                  | Se usa para hacer commit en base de datos y almacenar los cambios                           |
| [serve](#service-maintain)      | Opcional      | Si                  | Se usa para hacer operaciones con servicios (servicios Java o Web)                          |
| [queue](#queue-maintain)                 | Opcional      | Si                  | Se usa para hacer operaciones con colas                                             |
| [send-email](#email-maintain)       | Opcional      | Si                  | Envía emails                                                                                |
| [retrieve-data](#retrieve-data) | Opcional      | Si                  | Recupera datos de SQL, servicios, enumerados o colas                                        |
| [table](#table-element)         | Opcional      | No                  | Describe la tabla sobre la que se hacen los cambios                                         |
| [where](#where-element)         | Opcional      | No                  | Define las condiciones que deben cumplirse para realizar la operación. Es la cláusula `where` de sql |
| [field](#field-element)         | Opcional      | Si                  | Describe las **columnas** de la tabla sobre la que se opera                                 |
| [constant](#constant-element)   | Opcional      | Si                  | Campo constante para actualizar la tabla                                                    |
| [operation](#operation-element) | Opcional      | Si                  | Campo de operación para actualizar la tabla                                                 |
| [variable](#variable-element)   | Opcional      | Si                  | Son los parámetros pasados a los maintains                                                  |

#### Elemento target {#target-element}

El elemento target tiene los siguientes atributos:

| Atributo  | Uso          | Tipo    | Descripción                                                        | Valores                                                                                                             |
|-----------|--------------|---------|--------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| name      | **Obligatorio** | String  | Identificador del maintain                                      | **Nota:**  El nombre debe ser único                                                                                 |
| public    | Opcional     | Boolean | Para establecer que una query se pueda lanzar fuera de sesión (sin haber iniciado sesión) | Por defecto es `false`                                                        |                 
| label     | Opcional     | String  | Se usa para establecer el mensaje de salida tras ejecutar el mantenimiento | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales)                               | 
| exclusive | Opcional     | Boolean | Para restringir la ejecución concurrente del target                | Establece este atributo a `true` si no quieres que el target se ejecute de forma concurrente (varios usuarios a la vez) |

#### Elemento table {#table-element}

El elemento table tiene los siguientes atributos:

| Atributo  | Uso          | Tipo    | Descripción                    | Valores                                                   |
|-----------|--------------|---------|--------------------------------|-----------------------------------------------------------|
| id        | **Obligatorio** | String  | Nombre de la tabla          | **Nota:** Es el nombre real de la tabla en la base de datos |
| schema    | Opcional     | String  | Se usa para establecer el usuario propietario de la tabla | **Nota:** Es el esquema (usuario) real de la tabla en la base de datos |

#### Elemento where {#where-element}

El elemento where tiene la siguiente estructura:

```xml
<target name="maintain">
...
  <where>
  <and>
    <filter left-field="[Filter field]" left-table="[Filter table]" condition="[Condition]" right-variable="[Variable]" optional="[Filter Optional]"/>
    <filter left-field="[Field 1]" left-table="[Filter table]" condition="[Condition]" 
      right-field="[Field 2]" right-table="[Table 2]" ignorecase="[Ignorecase]" trim="[Trim]"/>
    <filter left-field="[Field]" left-table="[Filter table]" condition="[Condition]" right-query="[Subquery]"/>
      <or>
       ... (more filters or filter groups)
      </or>
      ... (more filters or filter groups)
  </and>
  <or>
    ... (more filters or filter groups)
  </or>
 </where>
 ...
</target>
```

> **Nota:** Hay dos operadores disponibles para enlazar filtros. `<and>` y `<or>`

#### Elemento filter {#filter-element}

El comportamiento del elemento filter es el mismo que el del [elemento filter de query](./query-definition.md#filter-element).

#### Elemento field {#field-element}

El elemento field en los maintains tiene los siguientes atributos:

| Atributo         | Uso          | Tipo    | Descripción                                                                                                                                  | Valores                                                                                                                                             |
|------------------|--------------|---------|----------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| id               | **Obligatorio** | String  | Nombre del campo                                                                                                                          | **Nota:** Es el nombre real de la columna de la tabla en la base de datos                                                                           |
| table            | Opcional     | String  | Nombre de la tabla del campo                                                                                                                 |                                                                                                                                                     |
| alias            | Opcional     | String  | Alias del campo. Se usa para describir el campo                                                                                              |                                                                                                                                                     |
| sequence         | Opcional     | String  | Nombre de la secuencia a insertar de la `tabla AweKey`                                                                                       | **Nota:** Solo se aplica en maintains `insert`.  Es obligatorio definir una nueva variable sin nombre para usarla y asignarla al atributo variable  |
| auto-incremental | Opcional     | Boolean | Si el valor del campo se recupera de una columna de tipo identity/autoincremental                                                           | **Nota:** Solo se aplica en maintains `insert`.  Es obligatorio definir una nueva variable sin nombre para usarla y asignarla al atributo variable  |
| key              | Opcional     | Boolean | Si el campo es una clave de la tabla, este valor debe establecerse a `true`                                                                  | **Nota:** Solo se aplica en maintains `multiple`                                                                                                    |
| audit            | Opcional     | Boolean | Registra el campo **SOLO** en la tabla de auditoría. **Nota:** Si este atributo se establece a `true` este campo **NO** se registrará en la tabla | El valor por defecto es `false`                                                                                                               |
| optional         | Opcional     | Boolean | Deja este campo fuera de la operación cuando su variable no tiene valor, en lugar de escribir `null`                                         | El valor por defecto es `false`. Ver [campos opcionales](#optional-fields)                                                                          |
| variable         | Opcional     | String  | Se usa para establecer el campo de entrada con el valor de una variable                                                                      |                                                                                                                                                     |
| query            | Opcional     | String  | Es el identificador de la query para hacer una subquery                                                                                      | **Nota:** El id de la query debe existir                                                                                                            |
| function         | Opcional     | String  | Para aplicar una función sql al campo                                                                                                        | Los valores posibles se definen en [funciones de campo](query-definition.md#field-functions)                                                        |
| cast             | Opcional     | String  | Cambia el formato del campo                                                                                                                  | Los valores posibles son `STRING`, `INTEGER`, `LONG`, `FLOAT` y `DOUBLE`                                                                            |

#### Campos opcionales {#optional-fields}

Por defecto, cada `field` declarado llega a la sentencia generada, por lo que una variable sin valor
escribe `null` en su columna. Con `optional="true"` el campo se deja fuera de la sentencia
por completo cuando su variable no tiene valor:

- en un **insert**, la columna toma su valor por defecto de la base de datos en lugar de `null`;
- en un **update**, la columna conserva su valor almacenado en lugar de sobrescribirse con `null`.

```xml
<update audit="HISAweThm">
  <table id="AweThm"/>
  <field id="IdeThm" table="AweThm" variable="themeId"/>
  <field id="Nam" table="AweThm" variable="themeName" optional="true"/>
  <where>
    <and>
      <filter left-field="IdeThm" condition="eq" right-variable="themeId"/>
    </and>
  </where>
  <variable id="themeId" type="INTEGER" name="themeId"/>
  <variable id="themeName" type="STRING" name="themeName"/>
</update>
```

Enviar `themeId` sin `themeName` actualiza solo `IdeThm`; el `Nam` almacenado no se toca.

> *Nota:* "Sin valor" significa **nulo o vacío**, la misma regla que `optional` ya sigue en los
> [filtros de query](query-definition.md#filter-element), por lo que el atributo se comporta de forma coherente en
> ambos sitios.

> *Nota:* El atributo se aplica a `insert` y `update`. Un `delete` no construye lista de campos, por lo que
> no le afecta.

> *Nota:* Las tablas de auditoría **no** se ven afectadas: registran la operación que se solicitó y
> sus columnas admiten nulos, por lo que el histórico sigue conservando una fila para la columna omitida.

> ⚠️ **Limitación:** Un campo enlazado a una variable de tipo **lista** (un maintain por lotes) nunca se omite.
> Todas las filas de un lote comparten una única lista de columnas, por lo que dejar fuera una columna solo para una fila
> no es representable en la sentencia.

#### Elemento constant {#constant-element}

El elemento *constant* tiene los siguientes atributos:

| Atributo  | Uso          | Tipo   | Descripción                              | Valores                                                                                   |
|-----------|--------------|--------|------------------------------------------|-------------------------------------------------------------------------------------------|
| id        | **Obligatorio** | String | Nombre del campo                      | **Nota:** Es el nombre real de la columna de la tabla en la base de datos                 |
| table     | Opcional     | String | Nombre de la tabla del campo             |                                                                                           |
| function  | Opcional     | String | Para aplicar una función sql al campo    | Los valores posibles se definen en [funciones de campo](query-definition.md#field-functions) |
| cast      | Opcional     | String | Cambia el formato del campo              | Los valores posibles son `STRING`, `INTEGER`, `LONG`, `FLOAT` y `DOUBLE`                  |
| value     | Obligatorio  | String | Un valor estático a usar como valor del campo |                                                                                      |
| type      | Opcional     | String | Tipo del valor                           | Los valores posibles están disponibles [aquí](query-definition.md#variable-types)         |

#### Elemento operation {#operation-element}

El elemento *operation* permite definir operaciones entre campos y se resolverá como cláusulas SQL:

```xml
<operation operator="[operator]" alias="[alias]">
  <constant value="[constant value]" />
  <field id="[field name]" table="[field table]" />
  ...
</operation>
```

| Atributo  | Uso          | Tipo    | Descripción                    | Valores                                                                                   |
|-----------|--------------|---------|--------------------------------|-------------------------------------------------------------------------------------------|
| id        | **Obligatorio** | String  | Nombre del campo            | **Nota:** Es el nombre real de la columna de la tabla en la base de datos                 |
| table     | Opcional     | String  | Nombre de la tabla del campo   |                                                                                           |
| operator  | Obligatorio  | String  | Operador de la operación       | Ver [atributo operator](query-definition.md#operator-attribute)                           |
| function  | Opcional     | String  | Para aplicar una función sql al campo | Los valores posibles se definen en [funciones de campo](query-definition.md#field-functions) |
| cast      | Opcional     | String  | Cambia el formato del campo    | Los valores posibles son `STRING`, `INTEGER`, `LONG`, `FLOAT` y `DOUBLE`                  |

#### Elemento variable {#variable-element}

El elemento variable en los maintains tiene los siguientes atributos:

| Atributo  | Uso          | Tipo     | Descripción                                                                                       | Valores                                                       |
|-----------|--------------|----------|---------------------------------------------------------------------------------------------------|---------------------------------------------------------------|
| id        | **Obligatorio** | String   | Es el nombre identificador de la variable                                                      | **Nota**: El id debe ser único                                |
| type      | **Obligatorio** | String   | Describe el tipo de la variable                                                                | Puede ser __STRING__, __INTEGER__, etc.                       |
| name      | Opcional     | String   | Nombre de una variable                                                                            | La variable es el nombre de un criterio definido en la pantalla |
| optional  | Opcional     | Boolean  | Establecer este atributo a `true` evitará un error en caso de que no se pueda recuperar el valor de la variable | El valor por defecto es `false`                          |
| value     | Opcional     | String   | Valor                                                                                             |                                                               |
| property  | Opcional     | String   | Nombre de la propiedad                                                                            |                                                               |
| session   | Opcional     | String   | Nombre de la variable de sesión                                                                   |                                                               |

## **Maintain SQL** {#sql-maintain}

### **Insert** {#insert}

El elemento insert en los maintains tiene los siguientes atributos:

| Atributo   | Uso       | Tipo    | Descripción                                                                                                                                                                              | Valores                                                                        |
|------------|-----------|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| multiple   | Opcional  | Boolean | Si es `true` esta operación se lanzará tantas veces como valores tenga la variable de lista definida; si es `audit` esta operación se auditará tantas veces como valores tenga la variable de lista definida | `true`, `false` o `audit`                                              |
| label      | Opcional  | String  | Se usa para establecer el mensaje de salida tras ejecutar el mantenimiento                                                                                                               | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales)  | 
| audit      | Opcional  | String  | El nombre de la tabla de auditoría donde se van a almacenar los valores de auditoría                                                                                                     | **Nota:** La tabla de auditoría debe existir                                   |
| query      | Opcional  | String  | Nombre de una query para recuperar datos de una sentencia [INSERT INTO SELECT](#insert-into-select)                                                                                      | **Nota:** Si también se define `audit`, AWE reutiliza el mismo conjunto de filas de origen para el insert base y las filas de auditoría |
| batch      | Opcional  | Boolean | Realiza la operación en lotes                                                                                                                                                            | El valor por defecto es `false`                                                |
| batch-size | Opcional  | Integer | Tamaño del lote                                                                                                                                                                          | El valor por defecto es el definido en las propiedades                         |


>**Nota:** Puedes ver la información del elemento `field` [aquí](#field-element)

La estructura del maintain insert es la siguiente:

```xml
<target name="[target_name]" public="[public]">
  <insert multiple="[multiple]" audit="[audit_table]">
    <table id="[table_name]"/>
    <field id="[field_name]" table="[table_name]" variable="[field_variable_name]" sequence="[sequence_name]"/>
    ... (more fields)
    <variable id="[variable_name]" type="[variable_type]" name="[parameter_name]" list="[variable_list]"/>
    <variable id="[variable_name]" type="[variable_type]" value="[static_value]" />
    <variable id="[variable_name]" type="[variable_type]" session="[session_variable]"/>
    ... (more variables)
  </insert>
</target>

```

> **Importante:** el nombre del atributo `sequence` debe existir en la tabla AweKey

### **Ejemplos de `INSERT`** {#insert-examples}

```xml
<!-- Insert into AweQue table -->
<target name="QueNew">
   <insert audit="HISAweQue">
      <table id="AweQue"/>
      <field id="IdeAweQue" sequence="JmsKey" variable="IdeAweQue"/>
      <field id="Als" variable="Als"/>
      <field id="Des" variable="Des"/>
      <field id="QueTyp" variable="QueTyp"/>
      <field id="Act" variable="Act"/>
      <variable id="IdeAweQue" type="INTEGER" name="IdeAweQue" />
      <variable id="Als" type="STRING" name="Als" />
      <variable id="Des" type="STRING" name="Des" />
      <variable id="QueTyp" type="STRING" name="QueTyp" />    
      <variable id="Act" type="INTEGER" name="Act" value="1"/>
    </insert>
    <commit/>
    <serve service="RelJmsCon"/>
  </target>
```

```xml
<!-- Insert with historic data -->
<target name="SitNew">
  <insert audit="HISAweSit">
      <table id="AweSit"/>
      <field id="IdeSit" sequence="SitKey" variable="IdeSit"/>
      <field id="Nam" variable="Nam"/>
      <field id="Ord" variable="Ord"/>
      <field id="Act" variable="Act"/>
      <variable id="Nam" type="STRING" name="Nam" />
      <variable id="Ord" type="INTEGER" name="Ord" />
      <variable id="Act" type="INTEGER" name="Act" />
      <variable id="IdeSit" type="INTEGER" name="IdeSit" />
    </insert>
</target>
```

### Inserts con **columnas autoincrementales** (IDENTITY/AUTO INCREMENT) {#inserts-with-autoincrement-columns-identityauto-increment}

Cuando la base de datos genera la clave primaria automáticamente (IDENTITY/AUTO INCREMENT), en las operaciones `insert` debes:

- Marcar la columna autoincremental con `auto-incremental="true"` en el elemento `field` para que AWE recupere el id generado tras el insert.
- Declarar una `variable` para ese campo que almacene el valor generado. No necesitas pasarla como entrada; AWE la rellenará con el id generado y puedes reutilizarla dentro del mismo `target` (por ejemplo, en operaciones posteriores) o devolverla al cliente.
- No establecer `sequence` en columnas autoincrementales (las secuencias solo se aplican a las secuencias de AWE almacenadas en la tabla `AweKey`).

Ejemplo usando la tabla `TestAutoIncrement` (con `id` como IDENTITY):

```xml
<!-- Insert into a table with an auto‑increment (IDENTITY) primary key -->
<target name="TstAutoIncNew">
  <insert>
    <table id="TestAutoIncrement"/>
    <!-- Mark the identity column as key and declare the variable to capture the generated id -->
    <field id="id" auto-incremental="true" variable="id"/>
    <field id="name" variable="name"/>
    <field id="email" variable="email"/>

    <!-- Input variables -->
    <variable id="name" type="STRING" name="name"/>
    <variable id="email" type="STRING" name="email"/>

    <!-- Output variable that will hold the generated id after the insert -->
    <variable id="id" type="INTEGER"/>
  </insert>
</target>
```

> Nota: En los maintains `multiple`, también es obligatorio tener un `field` con `auto-incremental="true"` para indicar la clave de la tabla. La misma convención se aplica cuando la clave la genera la base de datos.

Ejemplo reutilizando el id generado en un insert posterior (el padre `tst_order` y sus filas `tst_order_item`):

```xml
<target name="insertWithKeySingle">
  <!-- First insert: capture the generated OrderId from tst_order.id -->
  <insert>
    <table id="tst_order"/>
    <!-- Mark the identity column as auto-incremental="true" and declare the variable to capture the generated id -->
    <field id="id" auto-incremental="true" variable="OrderId"/>
    <field id="order_date" variable="OrderDate"/>
    <constant id="order_comments" value="Test Order"/>

    <variable id="OrderDate" type="SYSTEM_DATE" name="OrderDate"/>
    <!-- Output variable filled by AWE with the generated id -->
    <variable id="OrderId" type="INTEGER" name="OrderId"/>
  </insert>

  <!-- Second insert (multiple): reuse OrderId to insert order items -->
  <insert multiple="true">
    <table id="tst_order_item"/>
    <field id="order_id" variable="OrderId"/>
    <field id="order_product" variable="ProductName"/>
    <field id="order_price" variable="ProductPrice"/>
    <field id="order_quantity" variable="ProductQuantity"/>

    <variable id="OrderId" type="INTEGER" name="OrderId"/>
    <variable id="ProductName" type="STRING" name="order_product"/>
    <variable id="ProductPrice" type="FLOAT" name="order_price"/>
    <variable id="ProductQuantity" type="INTEGER" name="order_quantity"/>
  </insert>
</target>
```

### **Insert into select** {#insert-into-select}

La sentencia `INSERT INTO SELECT` es otra forma de hacer una sentencia insert, pero recuperando los datos de una subquery. La estructura de una sentencia `INSERT INTO SELECT` es la siguiente:

```xml
<target name="[target_name]" public="[public]">
  <insert query="[query_to_retrieve_data_from]">
    <table id="[table_name]"/>
    <field id="[field_name]"/>
    ... (more fields)
  </insert>
</target>
```

>**Nota:** El número de campos definidos en la sentencia `INSERT INTO SELECT` y en la subquery debe coincidir.

>**Nota:** Cuando `audit` se define junto con `query`, las filas de auditoría se generan a partir de las mismas filas de origen que usa el insert base. Las queries de origen vacías insertan cero filas base y cero filas de auditoría.

### **Ejemplos de `INSERT INTO SELECT`** {#insert-into-select-examples}

```xml
<!-- Insert into select (maintain.xml) -->
<target name="test">
  <insert query="testData">
    <table id="test"/>
    <field id="Nam"/>
    <field id="Ord"/>
  </insert>
</target>

<!-- Query for data retrieving (queries.xml) -->
<query id="testData" distinct="true">
  <table id="AweKey" />
  <field id="KeyNam"/>
  <field id="Act"/>
</query>
```

Esto generará el siguiente código SQL:

```sql
INSERT INTO test (Nam, Ord) SELECT DISTINCT KeyNam, Act FROM AweKey
```

### **Update** {#update}

El elemento update en los maintains tiene los siguientes atributos:

| Atributo   | Uso       | Tipo     | Descripción                                                                                                                                                                              | Valores                                                                    |
|------------|-----------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| multiple   | Opcional  | Boolean  | Si es `true` esta operación se lanzará tantas veces como valores tenga la variable de lista definida; si es `audit` esta operación se auditará tantas veces como valores tenga la variable de lista definida | `true`, `false` o `audit`                                          |
| label      | Opcional  | String   | Se usa para establecer el mensaje de salida tras ejecutar el mantenimiento                                                                                                               | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales) | 
| audit      | Opcional  | String   | El nombre de la tabla de auditoría donde se van a almacenar los valores de auditoría                                                                                                     | **Nota:** La tabla de auditoría debe existir                               |
| batch      | Opcional  | Boolean  | Realiza la operación en lotes                                                                                                                                                            | El valor por defecto es `false`                                            |
| batch-size | Opcional  | Integer  | Tamaño del lote                                                                                                                                                                          | El valor por defecto es el definido en las propiedades                     |

>**Nota:** Puedes ver la información del elemento `where` [aquí](#where-element)

La estructura del maintain update es la siguiente:

```xml
<target name="[target_name]" public="[public]">
  <update multiple="[multiple]" audit="[audit_table]">
    <table id="[table_name]"/>
    <field id="[field_name]" table="[table_name]" variable="[field_variable_name]" audit="[audit]"/>
    ... (more fields)
    <where>
      <and>
        <filter left-field="[filter_field]" left-table="[filter_field_table]" condition="[filter_condition]" 
         right-variable="[filter_variable]" ignorecase="[filter_ignore_case]" trim="[filter_trim]"/>
        ... (more filters)
      </and>
    </where>
    <variable id="[variable_name]" type="[variable_type]" name="[parameter_name]" list="[variable_list]"/>
    <variable id="[variable_name]" type="[variable_type]" value="[static_value]" />
    <variable id="[variable_name]" type="[variable_type]" session="[session_variable]"/>
    ... (more variables)
  </update>
</target>

```

### **Ejemplos de update** {#update-examples}

```xml
<!-- Manage User as logged out -->
<target name="MgrUsrLogOut" public="true">
  <!-- Mark User as logged out -->
  <update>
    <table id="ope"/>
    <field id="l1_con" variable="Con"/>
    <field id="l1_dat" variable="SysDat"/>
    <where>
      <and>
        <filter left-field="l1_nom" condition="eq" right-variable="Usr" ignorecase="true"/>
      </and>
    </where>
    <variable id="Con" type="INTEGER" value="0"/>
    <variable id="SysDat" type="SYSTEM_DATE" />
    <variable id="Usr" type="STRING" session="user" optional="false"/>
  </update>
</target>
```

```xml
<target name="UsrUpd">
  <update>
    <table id="ope"/>
    <field id="l1_act" variable="Sta"/>
    <field id="WebPrn" variable="WebPrn"/>
    <field id="PcPrn" variable="PcPrn"/>
    <field id="EmlAdr" variable="Eml"/>
    <field id="EmlSrv" variable="EmlSrv"/>
    <field id="l1_lan" variable="Lan"/>
    <field id="OpeNam" variable="Nam"/>
    <field id="IdePro" variable="Pro"/>
    <field id="IdeThm" variable="Thm"/>
    <field id="ScrIni" variable="ScrIni"/>
    <field id="Res" variable="Res"/>
    <field id="PwdLck" variable="PwdLck"/>
    <where>
      <and>
        <filter left-field="IdeOpe" condition="eq" right-variable="IdeOpe"/>
      </and>
    </where>
    <variable id="IdeOpe" type="INTEGER" name="IdeOpe" />
    <variable id="Sta" type="INTEGER" name="Sta" />
    <variable id="WebPrn" type="STRINGN" name="WebPrn" />
    <variable id="PcPrn" type="STRINGN" name="PcPrn" />
    <variable id="Eml" type="STRINGN" name="Eml" />
    <variable id="EmlSrv" type="STRINGN" name="EmlSrv" />
    <variable id="Lan" type="STRINGN" name="Lan" />
    <variable id="Nam" type="STRINGN" name="Nam" />
    <variable id="Pro" type="INTEGER" name="Pro" />
    <variable id="Thm" type="INTEGER" name="Thm" />
    <variable id="ScrIni" type="STRING" name="ScrIni" />
    <variable id="Res" type="STRING" name="Res" />
    <variable id="PwdLck" type="INTEGER" name="PwdLck" />
  </update>
</target>
```

### **Delete** {#delete}

El elemento delete en los maintains tiene los siguientes atributos:

| Atributo   | Uso       | Tipo    | Descripción                                                                                                                                                                              | Valores                                                                    |
|------------|-----------|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| multiple   | Opcional  | Boolean | Si es `true` esta operación se lanzará tantas veces como valores tenga la variable de lista definida; si es `audit` esta operación se auditará tantas veces como valores tenga la variable de lista definida | `true`, `false` o `audit`                                          |
| label      | Opcional  | String  | Se usa para establecer el mensaje de salida tras ejecutar el mantenimiento                                                                                                               | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales) | 
| audit      | Opcional  | String  | El nombre de la tabla de auditoría donde se van a almacenar los valores de auditoría                                                                                                     | **Nota:** La tabla de auditoría debe existir                               |
| batch      | Opcional  | Boolean | Realiza la operación en lotes                                                                                                                                                            | El valor por defecto es `false`                                            |
| batch-size | Opcional  | Integer | Tamaño del lote                                                                                                                                                                          | El valor por defecto es el definido en las propiedades                     |

>**Nota:** Puedes ver la información del elemento `where` [aquí](#where-element)

La estructura del maintain delete es la siguiente:

```xml
<target name="[target_name]" public="[public]">
  <delete multiple="[multiple]" audit="[audit_table]">
    <table id="[table_name]"/> 
    <field id="[field_name]" table="[table_name]" variable="[field_variable_name]" audit="[audit]"/>
    <where>
      <and>
        <filter left-field="[filter_field]" left-table="[filter_field_table]" condition="[filter_condition]" 
        right-variable="[filter_variable]" ignorecase="[filter_ignore_case]" trim="[filter_trim]"/>
        ... (more filters)
      </and>
    </where>
    <variable id="[variable_name]" type="[variable_type]" name="[parameter_name]" list="[variable_list]"/>
    <variable id="[variable_name]" type="[variable_type]" value="[static_value]" />
    <variable id="[variable_name]" type="[variable_type]" session="[session_variable]"/>
    ... (more variables)
  </delete>
</target>

```

#### **Ejemplos de delete** {#delete-examples}

```xml
<!-- Delete maintain with historic operation -->
<target name="ProDel">
  <delete audit="HISAweModPro">
    <table id="AweModPro"/>
    <field id="IdePro" variable="IdePro" audit="true"/>
    <where>
      <and>
        <filter left-field="IdePro" condition="eq" right-variable="IdePro" />
      </and>
    </where>
    <variable id="IdePro" type="INTEGER" name="IdePro"/>
  </delete>
</target>
```

```xml
<!-- Delete maintain with multiple atributte -->
<target name="UsrDel">
  <delete multiple="true">
    <table id="ope"/>
    <where>
      <and>
        <filter left-field="IdeOpe" condition="eq" right-variable="IdeOpe"/>
      </and>
    </where>
    <variable id="IdeOpe" type="INTEGER" name="IdeOpe"/>
  </delete>
</target>
```

### **Multiple** {#multiple}

Los maintains `multiple` se usan en el widget `Grid` para realizar operaciones sql.

El elemento multiple en los maintains tiene los siguientes atributos:

| Atributo  | Uso          | Tipo    | Descripción                                                           | Valores                                                                    |
|-----------|--------------|---------|-----------------------------------------------------------------------|----------------------------------------------------------------------------|
| grid      | **Obligatorio** | String  | Es el nombre del grid donde se realizan las operaciones            |                                                                            |
| label     | Opcional     | String  | Se usa para establecer el mensaje de salida tras ejecutar el mantenimiento | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales) | 
| audit     | Opcional     | String  | El nombre de la tabla de auditoría donde se van a almacenar los valores de auditoría | **Nota:** La tabla de auditoría debe existir                     |

> **IMPORTANTE:** Debe existir un campo con el atributo `key="true"` para indicar qué campo es la clave de la tabla

#### **Ejemplos de multiple** {#multiple-examples}

```xml
<!-- Screen Access Multiple -->
<target name="ScrAccUpd">
  <multiple audit="HISAweScrRes" grid="GrdScrAccLst">
    <table id="AweScrRes"/>
    <field id="IdeAweScrRes" variable="IdeAweScrRes" sequence="ScrResKey" key="true"/>
    <field id="IdePro" variable="IdePro"/>
    <field id="IdeOpe" variable="IdeOpe"/>
    <field id="IdeMod" variable="IdeMod"/>
    <field id="Opt" variable="Opt"/>
    <field id="AccMod" variable="AccMod"/>
    <field id="Act" variable="Act"/>
    <variable id="IdeAweScrRes" type="INTEGER" name="IdeAweScrRes"/>
    <variable id="IdeOpe" type="INTEGER" name="IdeOpe" optional="true"/>
    <variable id="IdePro" type="INTEGER" name="IdePro" optional="true"/>
    <variable id="IdeMod" type="INTEGER" name="IdeMod" optional="true"/>
    <variable id="Opt"  type="STRING" name="Opt" />
    <variable id="AccMod" type="STRING" name="AccMod"/>
    <variable id="Act" type="INTEGER" name="Act" />
  </multiple>
</target>
```

```xml
<!-- AweKey Update Multiple -->
<target name="AweKeyUpd">
  <multiple grid="GrdKeyLst">
    <table id="AweKey"/>
    <field id="KeyNam" variable="KeyNam" key="true"/>
    <field id="KeyVal" variable="KeyVal"/>
    <field id="Act" variable="Act"/>
    <variable id="KeyNam" type="STRING" name="KeyNam"/>
    <variable id="KeyVal" type="INTEGER" name="KeyVal"/>
    <variable id="Act" type="INTEGER" name="Act"/>
  </multiple>
</target>
```

### **Auditorías** {#audits}

Las auditorías las gestiona AWE automáticamente, pero las tablas de auditoría deben crearse en tiempo de desarrollo y necesitan tener al menos tres campos clave:

*  **Usuario de auditoría** - Usuario que realiza la operación
*  **Marca de tiempo de auditoría** - Fecha y hora de la operación de auditoría
*  **Tipo de auditoría** - Operación de inserción `(I)`, actualización `(U)` o borrado `(D)`


### **Commit** {#commit}

La etiqueta `<commit/>` se usa dentro de los targets de maintain para forzar que los **datos se guarden** en la base de datos. Normalmente se usa entre distintas operaciones de mantenimiento para asegurar que los datos son correctos antes de pasar al siguiente paso.

#### **Ejemplo de commit** {#commit-example}

```xml
<!-- Do commit before execute service operation -->
<target name="DbsDel">
  <delete multiple="true" audit="HISAweSitModDbs">
    <table id="AweSitModDbs"/>
    <field id="IdeDbs" variable="IdeDbs" audit="true"/>
    <where>
      <and>
        <filter left-field="IdeDbs" condition="eq" right-variable="IdeDbs" />
      </and>
    </where>
    <variable id="IdeDbs" type="INTEGER" name="IdeDbs"/>
  </delete>
  <delete multiple="true" audit="HISAweDbs">
    <table id="AweDbs"/>
    <field id="IdeDbs" variable="IdeDbs" audit="true" />
    <field id="Als" variable="Als" audit="true" />
    <field id="Des" variable="Des" audit="true" />
    <field id="Dct" variable="Dct" audit="true" />
    <field id="Typ" variable="Typ" audit="true" />
    <field id="Dbt" variable="Dbt" audit="true" />
    <field id="Dbc" variable="Dbc" audit="true" />
    <where>
      <and>
        <filter left-field="IdeDbs" condition="eq" right-variable="IdeDbs" />
      </and>
    </where>
    <variable id="IdeDbs" type="INTEGER" name="IdeDbs" />
    <variable id="Als" type="STRING" name="Als" />
    <variable id="Des" type="STRINGN" name="Des" />
    <variable id="Dct" type="STRING" name="Dct" />
    <variable id="Dbt" type="STRING" name="Dbt" />
    <variable id="Typ" type="STRING" name="Typ" />
    <variable id="Dbc" type="STRING" name="Dbc" />
  </delete>
  <commit/>
  <serve service="RelDbsCon"/>
</target>
```

## Maintain de servicio {#service-maintain}

Se usan para llamar a servicios (servicios Java o Web) desde una operación maintain.

El maintain de servicio tiene la siguiente estructura xml:

```xml
<target name="[target_name]" exclusive="[exclusive]" public="[public]">
  <serve service="[service_name]">
    <variable id="[variable_name]" type="[variable_type]" name="[parameter_name]"/>
    <variable id="[variable_name]" type="[variable_type]" value="[static_value]"/>
    <variable id="[variable_name]" type="[variable_type]" session="[session_variable]"/>
    ... (more variables)
  </serve>
</target>
```

>**IMPORTANTE**: El maintain de servicio debe tener el elemento `<serve>`. El nombre del servicio debe existir en el archivo `Services.xml`. Puedes ver [servicios](service-definition.md) para más información.

El elemento serve en los maintains tiene los siguientes atributos:

| Atributo  | Uso          | Tipo    | Descripción                                                   | Valores                                                                     |
|-----------|--------------|---------|---------------------------------------------------------------|-----------------------------------------------------------------------------|
| service   | **Obligatorio** | String  | Identificador del servicio                                 | **Nota:** El id del servicio debe existir en el archivo `Services.xml`      |
| label     | Opcional     | String  | Se usa para establecer el mensaje de salida tras ejecutar el mantenimiento | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales)  | 

#### **Ejemplos de maintain de servicio** {#service-maintain-examples}

```xml
<!-- Serve example -->
<target name="[target_name]">
  <serve service="RelAllScrCfg" />
  <serve service="LoaScrCfgByDbs">
    <variable id="database" type="STRING" session="database"/>
  </serve>
</target>
```

```xml
<!-- Clear profile Cache -->
<target name="ClrCchPrf">
  <serve service="ClrCch">
    <variable id="cache" type="STRING" value="PROFILE"/>
  </serve>
</target>
```

## **Maintain de cola** {#queue-maintain}

Se usan para enviar mensajes a una cola Jms desde una operación maintain.

El maintain de cola tiene la siguiente estructura xml:

```xml
<target name="[target_name]" >
  <queue name="[Id_queue]">
    <variable id="[variable_name]" type="[variable_type]" name="[parameter_name]" [[list="list"] 
     [session="session_variable"] [value="static_value"] [property="property_value"]]/>
    ... (more variables)
  </queue>
</target>
```

>**IMPORTANTE**: El maintain de cola debe tener el elemento `<queue>`. El nombre de la cola debe existir en el archivo `Queues.xml`. Puedes ver [colas](jms-queues-definition.md) para más información.

El elemento queue en los maintains tiene los siguientes atributos:

| Atributo  | Uso          | Tipo     | Descripción                                                   | Valores                                                                     |
|-----------|--------------|----------|---------------------------------------------------------------|-----------------------------------------------------------------------------|
| name      | **Obligatorio** | String   | Identificador de la cola                                   | **Nota:** El id de la cola debe existir en el archivo `Queues.xml`          |
| label     | Opcional     | String   | Se usa para establecer el mensaje de salida tras ejecutar el mantenimiento | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales)  | 

#### **Ejemplos de maintain de cola** {#queue-maintain-examples}

```xml
<!-- Test queues -->
<target name="TstQueSndSyn">
  <queue name="TstQueSndSyn">
    <variable id="Usr" type="STRING" name="CrtNam"/>
  </queue>
</target>
```

## **Maintain de email** {#email-maintain}

Se usan para enviar emails desde una operación maintain.

El maintain de email tiene la siguiente estructura xml:

```xml
<target name="[target_name]" >
  <send-email name="[Email Id]">
    <variable id="[variable_name]" type="[variable_type]" name="[parameter_name]" [[list="list"] 
     [session="session_variable"] [value="static_value"] [property="property_value"]]/>
    ... (more variables)
  </send-email>
</target>
```

>**IMPORTANTE**: El maintain de email debe tener el elemento `<send-email>`. El identificador del email debe existir en el archivo `Email.xml`. Puedes ver [definición de email](email-definition.md) para más información.

El elemento email en los maintains tiene los siguientes atributos:

| Atributo  | Uso          | Tipo    | Descripción                                                   | Valores                                                                     |
|-----------|--------------|---------|---------------------------------------------------------------|-----------------------------------------------------------------------------|
| id        | **Obligatorio** | String  | Identificador del email                                    | **Nota:** El id del email debe existir en el archivo `Email.xml`            |
| label     | Opcional     | String  | Se usa para establecer el mensaje de salida tras ejecutar el mantenimiento | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales)  | 

#### **Ejemplos de maintain de email** {#email-maintain-examples}

```xml
<!-- Send reports by e-mail -->
<target name="SndRep">
  <send-email id="SndRep"/>
</target>
```

## **Include target** {#include-target}

Puedes añadir una etiqueta `include-target` para reutilizar targets ya definidos en lugar de reescribirlos.

Include target tiene la siguiente estructura xml:

```xml
<target name="[Target name]" >
  <include-target name="[target-to-include]"/>
</target>
```

>**IMPORTANTE**: Include target debe tener el elemento `<include-target>`. El `[target-to-include]` debe existir en alguno de los archivos `Maintain.xml`.

Include target tiene los siguientes atributos:

| Atributo  | Uso           | Tipo     | Descripción        | Valores                                                       |
|-----------|---------------|----------|--------------------|---------------------------------------------------------------|
| name      | **Obligatorio** | String   | Identificador del target | **Nota:** El nombre del target debe existir en el archivo `Maintain.xml` |

#### **Ejemplos de include target** {#include-target-examples}

```xml
<!-- Test insert -->
<target name="testInsert">
  <insert>
    <table id="HISAweMod" />
    <field id="HISope" variable="User" />
    <field id="HISdat" variable="Date" />
    <field id="HISact" variable="Action" />
    <field id="Nam" variable="Nam" />
    <field id="IdeThm" variable="Thm" />
    <field id="ScrIni" variable="Scr" />
    <field id="Act" variable="Act" />
    <variable id="Nam" type="STRING" value="testIncludeTarget" />
    <variable id="Thm" type="INTEGER" value="1" />
    <variable id="Scr" type="STRING" value="testIncludeTarget" />
    <variable id="Act" type="INTEGER" value="1" />
    <variable id="User" type="STRING" value="testIncludeTarget" />
    <variable id="Date" type="SYSTEM_DATE"/>
    <variable id="Action" type="STRING" value="T" />
  </insert>
</target>

<!-- Test update -->
<target name="testUpdate">
  <update>
    <table id="HISAweMod" />
    <field id="Nam" variable="Nam" />
    <field id="IdeThm" variable="Thm" />
    <field id="ScrIni" variable="Scr" />
    <field id="Act" variable="Act" />
    <where>
      <and>
        <filter left-field="HISope" condition="eq" right-variable="User"/>
        <filter left-field="HISact" condition="eq" right-variable="Action"/>
      </and>
    </where>
    <variable id="Nam" type="STRING" value="testIncludeTargetUpd" />
    <variable id="Thm" type="INTEGER" value="2" />
    <variable id="Scr" type="STRING" value="testIncludeTargetUpd" />
    <variable id="Act" type="INTEGER" value="2" />
    <variable id="User" type="STRING" value="testIncludeTarget" />
    <variable id="Action" type="STRING" value="T" />
  </update>
</target>

<!-- Test delete -->
<target name="testDelete">
  <delete>
    <table id="HISAweMod" />
    <where>
      <and>
        <filter left-field="HISope" condition="eq" right-variable="User"/>
        <filter left-field="HISact" condition="eq" right-variable="Action"/>
      </and>
    </where>
    <variable id="User" type="STRING" value="testIncludeTarget" />
    <variable id="Action" type="STRING" value="T" />
  </delete>
</target>

<!-- Test include -->
<target name="testInclude">
  <include-target name="testInsert"/>
  <include-target name="testUpdate"/>
  <include-target name="testDelete"/>
</target>
```

## **Retrieve data** {#retrieve-data}

El target retrieve data es útil para recuperar datos de una query (SQL, servicio, enumerado, etc.) 
y enviarlos a un proceso maintain.

Retrieve data tiene la siguiente estructura xml:

```xml
<target name="[target_name]">
  <retrieve-data service="[service_id]" enumerated="[enumerated_id]" queue="[queue_id]">
    <table .../>
    <field alias="[field_alias]"
    <variable id="[variable_name]" type="[variable_type]" name="[parameter_name]" [[list="list"] 
     [session="session_variable"] [value="static_value"] [property="property_value"]]/>
    ... (more variables)
  </retrieve-data>
</target>
```

La etiqueta `retrieve-data` funciona como una query definida en queries.xml. Cada columna recuperada se 
almacenará como un parámetro con el nombre del alias del campo.

> **NOTA:** Si estás usando secuencias de AWE, deberías considerar priorizar esta forma de 
recuperar datos para insertarlos en lugar de las sentencias `INSERT INTO SELECT`.

#### **Ejemplos de retrieve data** {#retrieve-data-examples}

```xml
<target name="testRetrieveDataAndInsertAfter">
  <retrieve-data>
    <table id="Table1"/>
    <field id="Field1" alias="name"/>
    <field id="Field2" alias="screen"/>
    <field id="Field3" alias="user"/>
    <field id="Field4" alias="action"/>
  </retrieve-data>
  <insert multiple="true">
    <table id="Table2"/>
    <field id="User" variable="User"/>
    <field id="Date" variable="Date"/>
    <field id="Action" variable="Action"/>
    <field id="Name" variable="Nam"/>
    <field id="Active" variable="Active"/>
    <variable id="User" type="STRING" name="user"/>
    <variable id="Date" type="SYSTEM_DATE"/>
    <variable id="Action" type="STRING" name="action"/>
    <variable id="Name" type="STRING" name="name"/>
    <variable id="Active" type="INTEGER" value="1"/>
  </insert>
</target>
```

Este ejemplo recuperará una lista de variables llamadas `name`, `screen`, `user` y `action` 
y las usará como parámetros de entrada de la sentencia insert a `Table2`.
