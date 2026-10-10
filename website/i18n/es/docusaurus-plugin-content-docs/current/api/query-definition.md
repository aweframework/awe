---
id: query
title: Definición de consultas
sidebar_label: Definición de consultas
---

El motor de consultas de AWE se usa para consultar datos en sistemas externos. Funciona como una interfaz.

:::tip
Todos los elementos y atributos de las consultas están listados en la [referencia XSD](/reference/query) generada.
:::

<img alt="motor" src={require('@docusaurus/useBaseUrl').default('img/Data_engine.png')}/>

:::info
**Nota:** Todas las consultas se definen en el archivo `queries.xml` de la **carpeta global**. Consulta la [estructura del proyecto](../guides/project-structure.md#global-folder) para más información.
:::

## Consulta SQL {#sql-query}

Esta sección describe cómo se gestionan las consultas a base de datos con el motor de consultas de AWE.

### Estructura XML de sql {#xml-sql-structure}

La estructura completa de una consulta sql es la siguiente:

```xml
<!-- Example sql query -->

<query id="[Query Id]" cacheable="[Cacheable]" distinct="Distinct" managed-pagination="Pagination" post-process="Post processed">
  <table id="[Table id]" schema="[Schema name]" alias="[Table alias]" query="[Subquery]"/>
  <field id="[Field id]" table="[Table field]" alias="[Alias field]"/>
  ...
  <field id="[Field id]" table="[Table field]" alias="[Alias field]"/>
  <field variable="[Variable id]"/>
  <constant value="[Constant value]" type="INTEGER"/>
  <computed format="[Format]" alias="[Alias]" transform="[Transform]"/>
  ...
  <computed format="[Format]" alias="[Alias]" transform="[Transform]"/>
  <compound alias="[Compoun alias]">
    <computed format="[Format]" alias="[Alias]"/>
    ...
    <computed format="[Format]" alias="[Alias]"/>
  </compound>
  <join type="[Type join]">
   <table id="[Table id]" alias="[Table alias]"/>
     <and>
      <filter left-field="[Field]" left-table="[Table]" condition="[Condition]" right-field="[Counterfield]" 
      right-table="[Countertable]"/>
      ...
      <filter left-field="[Field]" left-table="[Table]" condition="[Condition]" right-variable="[Variable]"/>
    </and>
  </join>
  ...
  <union type="[union_type]" query="[Union subquery]"/>
  <where>
  <and>
    <filter left-field="[Filter field]" table="[Filter table]" condition="[Condition]" right-variable="[Variable]" optional="[Filter Optional]"/>
    <filter left-field="[Field 1]" table="[Filter table]" condition="[Condition]" 
      right-field="[Field 2]" right-table="[Table 2]" ignorecase="[Ignorecase]" trim="[Trim]"/>
    <filter left-field="[Field]" table="[Filter table]" condition="[Condition]" right-query="[Subquery]"/>
      <or>
       ... (more filters or filter groups)
      </or>
      ... (more filters or filter groups)
  </and>
  <or>
    ... (more filters or filter groups)
  </or>
 </where>
 <variable id="[Variable id]" type="[Variable Type]" name="[Variable name]" optional="[Optional]"/>
 ... (More <variable>)
 <group-by field="[Group field]" table="[Group table]" />
 ... (More <group-by>)
 <order-by field="[Order field]" table="[Order table]" type="[Order type]"/>
 ... (More <order-by >)
 <totalize function="[Totalize function]" label="[Label]" field="[Totalize field]" style="[Totalize style]">
   <totalize-by field="[Totalize by field]"/>
   ... (more totalize by fields)
   <totalize-field field="[Totalize field]"/>
   ... (more totalized fields)
  </totalize>
</query>
```

### Estructura global de las consultas Sql {#global-sql-query-structure}

Para simplificar el desarrollo de consultas, no todos los elementos son obligatorios.

| Elemento                      |      Uso       |  Varias instancias    | Descripción                                                                                  |
|-------------------------------|:--------------:|:---------------------:|----------------------------------------------------------------------------------------------|
| [query](#query-element)       | **Obligatorio**|          No           | Define la consulta. También describe el **tipo de consulta** (servicio, cola, etc.)          |
| [table](#table-element)       | **Obligatorio**|          Sí           | La tabla o lista de tablas sobre la que se realiza la consulta                               |
| [field](#field-element)       | **Obligatorio**|          Sí           | Describe la **columna** de la tabla                                                          |
| [computed](#computed-element) |    Opcional    |          Sí           | Los elementos computed se usan para obtener campos de la consulta a partir de otras columnas (field) |
| [compound](#compound-element) |    Opcional    |          Sí           | Los elementos compound son una lista de computed. Se usan para obtener estructuras complejas |
| [join](#join-element)         |    Opcional    |          Sí           | Se usa para hacer `joins` entre tablas                                                       |
| [union](#union-element)       |    Opcional    |          Sí           | Se usa para hacer `unions` entre tablas                                                      |
| [where](#where-element)       |    Opcional    |          No           | Cláusula `Where` de la consulta sql. Contiene la lista de condiciones de los campos          |
| [having](#having-element)     |    Opcional    |          No           | Cláusula `Having` de la consulta sql. Contiene la lista de condiciones de los campos para funciones |
| [group-by](#group-by-element) |    Opcional    |          Sí           | Cláusula `Group by` de la consulta sql                                                       |
| [order-by](#order-by-element) |    Opcional    |          Sí           | Cláusula `Order by` de la consulta sql                                                       |
| [totalize](#totalize-element) |    Opcional    |          Sí           | Se usa para totalizar el resultado de la consulta                                            |
| [variable](#variable-element) |    Opcional    |          Sí           | Parámetros pasados desde las pantallas a la consulta                                         |

### Elemento query {#query-element}

El elemento *query* tiene los siguientes atributos:

| Atributo           |      Uso       |   Tipo    | Descripción                                                                                                                                                                           | Valores                                                                                                                                                           |
|--------------------|:--------------:|:---------:|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| id                 |**Obligatorio** |  String   | Identificador de la consulta                                                                                                                                                          | **Nota:**  El nombre del id debe ser único                                                                                                                        |
| distinct           |    Opcional    |  Boolean  | Se usa para devolver solo valores distintos (diferentes)                                                                                                                              | Por defecto es `false`                                                                                                                                            |
| cacheable          |    Opcional    |  Boolean  | Se usa para establecer una consulta como cacheable (para guardar los datos en memoria y evitar ejecutar la consulta de nuevo)                                                         | Por defecto es `false`. **Nota:** Si estableces una **consulta como cacheable** y hay subconsultas, debes definir en ella las mismas variables que tienen todas las subconsultas. |
| managed-pagination |    Opcional    |  Boolean  | Para establecer una consulta como paginada (para cargar solo los datos de esa página concreta, no todos los registros de la consulta).  Se usa para lograr un alto rendimiento en consultas con un número muy alto de registros. | Por defecto es `false`. **Nota:** Usa este parámetro solo en consultas sin totalize.                                                                              |
| post-process       |    Opcional    |  Boolean  | Lanza o no el procesamiento de la lista de datos que realiza AWE, para permitir a los desarrolladores hacer su propio posprocesamiento                                                | Por defecto es `true`. **Nota:** Solo se aplica en consultas de servicio. Para más información, consulta la [consulta de servicio](#service-query)                |
| public             |    Opcional    |  Boolean  | Permite que la consulta se ejecute sin necesidad de haber iniciado sesión                                                                                                             | Por defecto es `false`                                                                                                                                            |
| enumerated         |    Opcional    |  String   | El nombre del enumerado con el que rellenar la consulta                                                                                                                               | **Nota:** Solo se aplica en consultas de enumerado. Para más información, consulta la [consulta de enumerado](#enumerated-query)                                  |
| service            |    Opcional    |  String   | El nombre del servicio con el que rellenar la consulta                                                                                                                                | **Nota:** Solo se aplica en consultas de servicio. Para más información, consulta la [consulta de servicio](#service-query)                                       |
| queue              |    Opcional    |  String   | El nombre de la cola con la que rellenar la consulta                                                                                                                                  | **Nota:** Solo se aplica en consultas de cola. Para más información, consulta la [consulta de cola](#queue-query)                                                 |

### Caché de consultas y trazas de log {#query-cache-and-trace-logging}

Si `cacheable="true"`, el resultado de la consulta se almacena en caché usando como clave de caché el id de la consulta más los parámetros resueltos.
Esto evita volver a ejecutar la misma consulta con las mismas entradas durante la vida de la caché.

Para ver los aciertos de caché en los logs, establece el logger `com.almis.awe.cache.LoggingCache` a `trace`.
Cuando está habilitado, verás entradas como:

```log
Cache hit [queryData] key=getFavourites|{"max":0,"page":1,"user":"test"}
```

Ejemplo de configuración en `application.properties`:

```properties
logging.level.com.almis.awe.cache.LoggingCache=trace
```

## Elementos SQL {#sql-elements}

Los siguientes elementos se traducen a una cláusula SQL. 
Forman parte de las instrucciones SQL estándar:

### Elemento table {#table-element}

El elemento *table* tiene los siguientes atributos:

| Atributo  | Uso             | Tipo    | Descripción                                                                      | Valores                                                     |
|-----------|-----------------|---------|----------------------------------------------------------------------------------|-------------------------------------------------------------|
| id        | **Obligatorio** | String  | Nombre de la tabla                                                               | **Nota:** Nombre real de la tabla en la base de datos       |
| schema    | Opcional        | String  | Esquema de la tabla. Se usa para establecer el usuario propietario de la tabla   | **Nota:** Es el nombre real del esquema (usuario) en la base de datos |
| alias     | Opcional        | String  | Alias de la tabla. Se usa para renombrar temporalmente una tabla o el encabezado de una columna |                                            |
| query     | Opcional        | String  | Id de una subconsulta que se usará como origen de datos                          |                                                             |

#### Ejemplo de subconsulta {#subquery-example}

```xml

  <query id="GetProcessedAccounting" distinct="true">
    <field id="Name" alias="nameCol" table="a" />
    <field id="Created_Date" alias="createdDateCol" table="a"/>
    <field id="Type" alias="typeCol" table="a"/>
    <field id="ddo.OperationsCol" />
    <table id="ACCOUNTING" alias="a" />
    <join type="LEFT">
      <!-- Use Subquery -->
      <table alias="ddo" query="GetProcessedAccountingDirectDebit"/>
      <and>
        <filter left-field="IdCol" left-table="ddo" condition="eq" right-field="Id" right-table="a" />
      </and>
    </join>
  </query>
  
  <!-- Define Subquery -->
  <query id="GetProcessedAccountingDirectDebit">
    <field id="Id" alias="idCol" table="a" />
    <field id="Id" alias="OperationsCol" function="COUNT" table="a"/>
    <table id="ACCOUNTING" alias="a" />
    <join type="LEFT">
        <table id="ADEUDO_OPERACION"  alias="o"/>
        <and>
          <filter left-field="Accounting_Id" left-table="o" condition="eq" right-field="Id" right-table="a" />
        </and>
    </join>
    <where>
      <and>
        <filter left-field="Accounting_Id" left-table="o" condition="is not null" />
      </and>
    </where>
    <group-by field="Id" table="a"/>
  </query>
```
### Elemento field {#field-element}

El elemento *field* tiene los siguientes atributos:

| Atributo  |    Uso     |   Tipo    | Descripción                                                                         | Valores                                                                                                    |
|-----------|:----------:|:---------:|-------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| id        |  Opcional  |  String   | Nombre del campo                                                                    | **Nota:** Nombre real de la columna de la tabla en la base de datos                                        |
| table     |  Opcional  |  String   | Tabla del campo                                                                     |                                                                                                            |
| alias     |  Opcional  |  String   | Alias del campo. Se usa para describir el campo                                     |                                                                                                            |
| noprint   |  Opcional  |  Boolean  | Se usa para establecer un campo como no imprimible. (El valor del campo no se cargará en el resultSet) |                                                                                         |
| transform |  Opcional  |  String   | Se usa para dar formato al valor del campo                                          | Consulta [esto](#transform-attribute) para más información sobre el atributo transform.                    |
| pattern   |  Opcional  |  String   | Se usa en un campo de tipo número, define el patrón con el que dar formato al número | Consulta [esta página](http://docs.oracle.com/javase/tutorial/i18n/format/decimalFormat.html) para más información |
| translate |  Opcional  |  String   | Traduce la salida con un identificador de grupo enumerado                           | **Nota:** Si el valor del campo es igual a un valor enumerado, devuelve la etiqueta del enumerado          |
| function  |  Opcional  |  String   | Para aplicar una función sql al campo                                               | Los valores posibles se definen en [funciones de campo](#field-functions)                                  |
| cast      |  Opcional  |  String   | Cambia el formato del campo                                                         | Los valores posibles son `STRING`, `INTEGER`, `LONG`, `FLOAT` y `DOUBLE`                                   |
| query     |  Opcional  |  String   | Identificador de consulta para hacer una subconsulta                                | **Nota:** El id de la consulta debe existir, y los atributos `table` e `id` se ignorarán                   |
| variable  |  Opcional  |  String   | Un identificador de variable que se usará como valor del campo                      | **Nota:** Si se define el atributo `variable`, los atributos `table` e `id` se ignorarán                   |

> **Nota:** El orden de lectura de los atributos de los campos es el siguiente:
> 1. `query`
> 2. `variable`
> 3. `id` (y `table` si está definido)
>
> Se requiere al menos uno de los atributos anteriores en un campo.

#### Funciones de campo {#field-functions}

- `ABS`: Valor absoluto
- `AVG`: Media de valores 
- `CNT`: Contar valores
- `CNT_DISTINCT`: Contar valores distintos
- `MAX`: Valor máximo
- `MIN`: Valor mínimo
- `SUM`: Sumar valores
- `ROW_NUMBER`: Número de fila
- `RANK`: Ranking de agregación
- `TRUNCDATE` (no estándar): Truncar fecha
- `YEAR`: Obtener el año de una fecha
- `MONTH`: Obtener el mes de una fecha
- `DAY`: Obtener el día de una fecha
- `HOUR`: Obtener las horas de una fecha
- `MINUTE`: Obtener los minutos de una fecha
- `SECOND`: Obtener los segundos de una fecha
- `TRIM`: Eliminar todos los espacios de ambos lados de la cadena
- `LENGTH`: Obtener la longitud del campo

#### Atributo transform {#transform-attribute}

Estos son los valores posibles del atributo `transform`:

* `DATE`: Transforma el campo de salida **(Date/String)** en un campo de fecha web (`dd/MM/yyyy`)
* `DATE_MS`: Transforma el campo de salida **(Date/String)** en una fecha java en milisegundos (para ejes de fecha y hora de gráficos)
* `TIME`: Transforma el campo de salida **(Date/String)** en un campo de hora web (`HH:mm:ss`)
* `TIMESTAMP`: Transforma el campo de salida **(Date/String)** en un campo de marca de tiempo web (`dd/MM/yyyy HH:mm:ss`)
* `TIMESTAMP_MS`: Transforma el campo de salida **(Date/String)** en un campo de marca de tiempo web con
  milisegundos (`dd/MM/yyyy HH:mm:ss.SSS`)
* `JS_DATE`: Transforma el campo de salida **(Date/String)** en un campo de fecha javascript (para ejes de gráficos) (`MM/dd/yyyy`)
* `JS_TIMESTAMP`: Transforma el campo de salida **(Date/String)** en un campo de marca de tiempo
  javascript (`MM/dd/yyyy HH:mm:ss`)
* `GENERIC_DATE`: Transforma el campo de salida **(String)** de un formato de fecha definido en `format-from` a un formato de fecha
  definido en `format-to`
* `DATE_RDB`: Transforma el campo de salida **(String)** de un formato RDF inglés (`dd-MMM-yyyy`) a un campo de fecha
  web (`dd/MM/yyyy`)
* `ELAPSED_TIME`: Transforma el campo de salida **(Long)** de un valor long en milisegundos a una cadena localizada que indica
  el tiempo transcurrido (`12h`)
* `DATE_SINCE`: Transforma el campo de salida **(Date)** en una cadena localizada con la diferencia de tiempo respecto a
  ahora (`5 min ago`)
* `NUMBER`: Transforma el campo de salida como un número con un patrón. **IMPORTANTE**:
* Al usar esta transformación, el patrón asociado debe tener separador de millares. Por ejemplo: ###,###.00
* **NUNCA** uses esta transformación si los datos recuperados son para un componente numérico
* Esta transformación se usa normalmente cuando queremos mostrar un valor numérico en una rejilla de visualización (columnas sin
  componente)
* `NUMBER_PLAIN`: Transforma el campo de salida como un número con un patrón en bruto (sin separador de millares). **
  IMPORTANTE**:
* Al usar esta transformación, el patrón asociado no debe tener separador de millares (p. ej.: ###.00)
* Puede usarse para componentes numéricos y elementos que no tienen componente.
* Esta transformación se usa normalmente cuando queremos imprimir componentes numéricos y especificar el número de decimales que queremos
  ver en el archivo pdf. Normalmente, el número de decimales del patrón coincidirá con la "precisión" definida en el
  atributo number-format del componente numérico.
* `BOOLEAN`: Transforma el campo de salida como un valor booleano (`true`/`false`):
* `TEXT_HTML`: Transforma el campo de salida en texto HTML (para mostrarse en una página HTML)
* `TEXT_PLAIN`: Transforma el campo de salida en texto plano (para mostrarse dentro de un documento)
* `TEXT_UNILINE`: Transforma el campo de salida en un texto plano sin saltos de línea
* `MARKDOWN_HTML`: Transforma el campo de salida de Markdown a texto HTML (para mostrarse en una página HTML)
* `DECRYPT`: Descifra el valor de una columna que está cifrado en la base de datos
* `ARRAY`: Divide un valor de cadena con la cadena indicada en el atributo `pattern`

### Elemento constant {#constant-element}

El elemento *constant* tiene los siguientes atributos:

| Atributo  |    Uso     |   Tipo    | Descripción                                                                     | Valores                                                                                               |
|-----------|:----------:|:---------:|---------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| alias     |  Opcional  |  String   | Alias del campo. Se usa para describir el campo                                 |                                                                                                       |
| noprint   |  Opcional  |  Boolean  | Se usa para establecer un campo como no imprimible. (El valor del campo no se cargará en el resultSet) |                                                                                        |
| transform |  Opcional  |  String   | Se usa para dar formato al valor del campo                                      | Lee [esto](#transform-attribute) para más información sobre el atributo transform.                    |
| pattern   |  Opcional  |  String   | Se usa en un campo de tipo número, define el patrón con el que dar formato al número | Lee [esta página](http://docs.oracle.com/javase/tutorial/i18n/format/decimalFormat.html) para más información |
| translate |  Opcional  |  String   | Traduce la salida con un identificador de grupo enumerado                       | **Nota:** Si el valor del campo es igual a un valor enumerado, devuelve la etiqueta del enumerado     |
| function  |  Opcional  |  String   | Para aplicar una función sql al campo                                           | Los valores posibles se definen en [funciones de campo](#field-functions)                             |
| cast      |  Opcional  |  String   | Cambia el formato del campo                                                     | Los valores posibles son `STRING`, `INTEGER`, `LONG`, `FLOAT` y `DOUBLE`                              |
| value     |Obligatorio |  String   | Un valor estático que se usará como valor del campo                             |                                                                                                       |
| type      |  Opcional  |  String   | Tipo del valor                                                                  | Los valores posibles están disponibles [aquí](#variable-types)                                        |

### Elemento operation {#operation-element}

El elemento *operation* permite definir operaciones entre campos y se resolverá como cláusulas SQL:

```xml
<operation operator="[operator]" alias="[alias]">
  <constant value="[constant value]" />
  <field id="[field name]" table="[field table]" />
  ...
</operation>
```

| Atributo  |    Uso     |    Tipo     | Descripción                                                              | Valores                                                                                              |
|-----------|:----------:|:-----------:|--------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| operator  |Obligatorio |   String    | Operador de la operación                                                 | Consulta el [atributo operator](#operator-attribute)                                                 |
| alias     |  Opcional  |   String    | Alias del campo. Se usa para describir el campo                          |                                                                                                      |
| noprint   |  Opcional  |   Boolean   | Se usa para establecer un campo como no imprimible. (El valor del campo no se carga en el resultset) |                                                                                  |
| transform |  Opcional  |   String    | Se usa para dar formato al valor del campo                               | Consulta [esto](#transform-attribute) para más información sobre el atributo transform.              |
| pattern   |  Opcional  |   String    | Se usa en un valor de tipo número, define el patrón con el que dar formato al número | Consulta [esta página](http://docs.oracle.com/javase/tutorial/i18n/format/decimalFormat.html) para más información |
| translate |  Opcional  |   String    | Traduce la salida con un identificador de grupo enumerado                | **Nota:** Si el valor del campo es igual a un valor enumerado, devuelve la etiqueta del enumerado    |
| function  |  Opcional  |   String    | Para aplicar una función sql al campo                                    | Los valores posibles se definen en [funciones de campo](#field-functions)                            |
| cast      |  Opcional  |   String    | Cambia el formato del campo                                              | Los valores posibles son `STRING`, `INTEGER`, `LONG`, `FLOAT` y `DOUBLE`                             |

#### Atributo function {#function-attribute}

El atributo `function` también puede aplicarse directamente a una `operation`. En ese caso, AWE primero resuelve la
operación y después aplica la función SQL seleccionada al valor resultante.

Ejemplo: calcular el valor absoluto del resultado de una suma.

```xml
<operation function="ABS" operator="ADD" alias="AbsIde">
  <field id="IdeSitModDbs" function="SUM"/>
</operation>
```

En términos de SQL, esto se comporta como aplicar `ABS(...)` al resultado de la operación generada.

#### Atributo operator {#operator-attribute}

Estos son los valores posibles del atributo `operator`:

* `CONCAT`: Concatena varios campos de cadena
* `REPLACE`: Reemplaza en el primer campo la cadena definida en segundo lugar por la cadena definida en tercer lugar
* `SUBSTRING`: Extrae una subcadena de un campo de cadena (consulta [SUBSTRING](#substring-operation))
* `NULLIF`: Establece null si es igual al segundo operando
* `COALESCE`: Dado un conjunto de campos, devuelve el primero que **NO ES NULL**
* `ADD`: Suma dos campos (`+`)
* `SUB`: Resta dos campos (`-`)
* `MULT`: Multiplica dos campos (`*`)
* `DIV`: Divide dos campos (`/`)
* `MOD`: Devuelve el resto o el resto con signo de una división, después de dividir un número entre otro (llamado
  el módulo de la operación). (`%`)
* `POWER`: El primer campo se eleva a la potencia del segundo campo (`^`)
* `ROUND`: Sustituye un número por un valor aproximado
* `ADD_SECONDS`: Añade segundos a un campo de fecha
* `ADD_MINUTES`: Añade minutos a un campo de fecha
* `ADD_HOURS`: Añade horas a un campo de fecha
* `ADD_DAYS`: Añade días a un campo de fecha
* `ADD_WEEKS`: Añade semanas a un campo de fecha
* `ADD_MONTHS`: Añade meses a un campo de fecha
* `ADD_YEARS`: Añade años a un campo de fecha
* `DIFF_SECONDS`: Calcula la diferencia en segundos entre dos fechas
* `DIFF_MINUTES`: Calcula la diferencia en minutos entre dos fechas
* `DIFF_HOURS`: Calcula la diferencia en horas entre dos fechas
* `DIFF_DAYS`: Calcula la diferencia en días entre dos fechas
* `DIFF_WEEKS`: Calcula la diferencia en semanas entre dos fechas
* `DIFF_MONTHS`: Calcula la diferencia en meses entre dos fechas
* `DIFF_YEARS`: Calcula la diferencia en años entre dos fechas
* `SUB_SECONDS`: Resta segundos a un campo de fecha
* `SUB_MINUTES`: Resta minutos a un campo de fecha
* `SUB_HOURS`: Resta horas a un campo de fecha
* `SUB_DAYS`: Resta días a un campo de fecha
* `SUB_WEEKS`: Resta semanas a un campo de fecha
* `SUB_MONTHS`: Resta meses a un campo de fecha
* `SUB_YEARS`: Resta años a un campo de fecha

#### Operación SUBSTRING {#substring-operation}

`SUBSTRING` extrae parte de una cadena. Acepta **2 o 3 operandos**:

| Forma | Operandos | Resultado |
|------|----------|--------|
| `SUBSTRING(source, beginIndex)` | source + beginIndex | Desde `beginIndex` hasta el final de la cadena |
| `SUBSTRING(source, beginIndex, endIndex)` | source + beginIndex + endIndex | Desde `beginIndex` (incluido) hasta `endIndex` (excluido) |

> **La semántica sigue a Java / QueryDSL**, no al `SUBSTRING(str, start, length)` de SQL:
> - Los índices son **base 0**.
> - `beginIndex` es **inclusivo**.
> - `endIndex` es **exclusivo** (es decir, el carácter en `endIndex` no se incluye).
>
> QueryDSL traduce automáticamente esta semántica al SQL correcto para cada dialecto de base de datos soportado.

**Ejemplo con 2 argumentos** — extraer desde el índice 6 hasta el final: `"Hello World" → "World"`

```xml
<operation operator="SUBSTRING" alias="result">
  <field id="description" />
  <constant value="6" type="INTEGER"/>
</operation>
```

**Ejemplo con 3 argumentos** — extraer `[6, 11)`: `"Hello World" → "World"`

```xml
<operation operator="SUBSTRING" alias="result">
  <field id="description" />
  <constant value="6" type="INTEGER"/>
  <constant value="11" type="INTEGER"/>
</operation>
```

Tanto `beginIndex` como `endIndex` pueden ser constantes de tipo entero, variables de tipo entero o columnas enteras de la base de datos.

#### Ejemplos de operation {#operation-examples}

Campo concatenado: `("Pro" + pro.Nam + "-Mod" + mod.Nam) as parent`

```xml
<operation operator="CONCAT" alias="parent">
  <constant value="Pro" />
  <field id="Nam" table="pro" />
  <constant value="-Mod" />
  <field id="Nam" table="mod" />
</operation>
```

Sumar 1 a un campo: `(pro.Nam + 1) as parent`

```xml
<operation operator="ADD" alias="parent">
  <field id="Nam" table="pro" />
  <constant value="1" type="INTEGER"/>
</operation>
```

Redondear un campo a 2 decimales: 'round(column, 2)'
```xml
<operation operator="ROUND" alias="roundField">
  <field id="Rate" table="User" />
  <constant value="2" type="INTEGER"/>
</operation>
``` 

### Elemento case {#case-element}

El elemento *case* permite generar una lista de cláusulas `when` dentro de un elemento `field`. Debe definirse una cláusula `else` al final de la cláusula `case`. 
Tiene los mismos atributos que un [elemento filter](#filter-element) **más** algunas características adicionales:

| Atributo  |    Uso     |   Tipo    | Descripción                                                                  | Valores                                                                                              |
|-----------|:----------:|:---------:|------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| alias     |  Opcional  |  String   | Alias del campo. Se usa para describir el campo                              |                                                                                                      |
| noprint   |  Opcional  |  Boolean  | Se usa para establecer un campo como no imprimible. (El valor del campo no se carga en el resultset) |                                                                                      |
| transform |  Opcional  |  String   | Se usa para dar formato al valor del campo                                   | Consulta [esto](#transform-attribute) para más información sobre el atributo transform.              |
| pattern   |  Opcional  |  String   | Se usa en un campo de tipo número, define el patrón con el que dar formato al número | Consulta [esta página](http://docs.oracle.com/javase/tutorial/i18n/format/decimalFormat.html) para más información |
| translate |  Opcional  |  String   | Traduce la salida con un identificador de grupo enumerado                    | **Nota:** Si el valor del campo es igual a un valor enumerado, devuelve la etiqueta del enumerado    |
| function  |  Opcional  |  String   | Para aplicar una función sql al campo                                        | Los valores posibles se definen en [funciones de campo](#field-functions)                            |
| cast      |  Opcional  |  String   | Cambia el formato del campo                                                  | Los valores posibles son `STRING`, `INTEGER`, `LONG`, `FLOAT` y `DOUBLE`                             |

> **¡NUEVO!**: Como se describe en el [elemento filter](#filter-element), `left-operand` y `right-operand` también deben contener
> un nodo de `field`, `constant`, `operation` o `case`. Lo mismo ocurre con los elementos `then` y `else`.

> **¡NUEVO!**: Puedes usar varios filtros dentro de la cláusula `when` usando la cláusula `and` como se describe [aquí](#where-element).

#### Ejemplos de case {#case-examples}

Campo case: 

```sql
CASE WHEN (Nam = "sunset") THEN 1 WHEN (Nam = "sunny") THEN 2 WHEN (Nam = "purple-hills") THEN 3 ELSE 0 END AS "value"
```

se generará como:

```xml
<query id="testCaseWhenElse">
  <table id="AweThm"/>
  <case alias="value">
    <when condition="eq">
      <left-operand>
        <field id="Nam"/>
      </left-operand>
      <right-operand>
        <field variable="sunset"/>
      </right-operand>
      <then>
        <constant value="1" type="INTEGER"/>
      </then>
    </when>
    <when left-field="Nam" condition="eq" right-variable="sunny">
      <then>
        <constant value="2" type="INTEGER"/>
      </then>
    </when>
    <when left-field="Nam" condition="eq" right-variable="purple-hills">
      <then>
        <constant value="3" type="INTEGER"/>
      </then>
    </when>
    <else>
      <constant value="0" type="INTEGER"/>
    </else>
  </case>
  <variable id="sunset" type="STRING" value="sunset"/>
  <variable id="sunny" type="STRING" value="sunny"/>
  <variable id="purple-hills" type="STRING" value="purple-hills"/>
</query>
```

Campo case:

```sql
CASE WHEN (Nam = "sunset" AND Thm = "summer") THEN 1 ELSE 0 END AS "value"
```

se generará como:

```xml
<query id="testCaseWhenMultipleFilters">
  <table id="AweThm"/>
  <case alias="Nam">
    <when>
      <and>
        <filter condition="eq">
          <left-operand>
            <field id="Nam"/>
          </left-operand>
          <right-operand>
            <field variable="sunset"/>
          </right-operand>
        </filter>
        <filter condition="eq">
          <left-operand>
            <field id="Thm"/>
          </left-operand>
          <right-operand>
            <field variable="summer"/>
          </right-operand>
        </filter>
      </and>
      <then>
        <constant value="1" type="INTEGER"/>
      </then>
    </when>
    <else>
      <constant value="0" type="INTEGER"/>
    </else>
  </case>
  <variable id="sunset" type="STRING" value="sunset"/>
  <variable id="summer" type="STRING" value="summer"/>
</query>
```

### Elemento over {#over-element}

El elemento *over* permite modelar **funciones de ventana SQL**. Este elemento contiene una cláusula de campo (`field`, `constant`, 
`operation` o `case`) y algunas cláusulas `partition-by` u `order-by`.

| Atributo  |   Uso    |   Tipo    | Descripción                                                                     | Valores                                                                                              |
|-----------|:--------:|:---------:|---------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| alias     | Opcional |  String   | Alias del campo. Se usa para describir el campo                                 |                                                                                                      |
| noprint   | Opcional |  Boolean  | Se usa para establecer un campo como no imprimible. (El valor del campo no se cargará en el resultSet) |                                                                                       |
| transform | Opcional |  String   | Se usa para dar formato al valor del campo                                      | Consulta [esto](#transform-attribute) para más información sobre el atributo transform.              |
| pattern   | Opcional |  String   | Se usa en un valor de tipo número, define el patrón con el que dar formato al número | Consulta [esta página](http://docs.oracle.com/javase/tutorial/i18n/format/decimalFormat.html) para más información |
| translate | Opcional |  String   | Traduce la salida con un identificador de grupo enumerado                       | **Nota:** Si el valor del campo es igual a un valor enumerado, devuelve la etiqueta del enumerado    |
| function  | Opcional |  String   | Para aplicar una función sql al campo                                           | Los valores posibles se definen en [funciones over](#over-functions)                                 |
| cast      | Opcional |  String   | Cambia el formato del campo                                                     | Los valores posibles son `STRING`, `INTEGER`, `LONG`, `FLOAT` y `DOUBLE`                             |

#### Funciones over {#over-functions}

- `AVG`: Media de valores 
- `CNT`: Cuenta valores
- `CNT_DISTINCT`: Cuenta valores distintos
- `MAX`: Valor máximo
- `MIN`: Valor mínimo
- `SUM`: Suma de valores
- `FIRST_VALUE`: Primer valor
- `LAST_VALUE`: Último valor
- `LAG`: Lag
- `ROW_NUMBER`: Número de fila
- `TRUNCDATE` (no estándar): Trunca la fecha
- `LENGTH`: Longitud del valor del campo

#### Ejemplos de over {#over-examples}

Campo over: 

```sql
SELECT MAX(date) OVER (PARTITION BY name ORDER BY position ASC) as `maxValue` FROM tableId
```

se generará como:

```xml
<query id="testOver">
  <table id="tableId"/>
  <over alias="maxValue">
    <field id="date" function="MAX"/>
    <partition-by field="name"/>
    <order-by field="position" type="ASC"/>
  </over>
</query>
```

#### Elemento partition-by {#partition-by-element}

El elemento *partition-by* define una partición de una función de ventana. Repítelo para particionar por
varias expresiones.

| Atributo  |   Uso    |  Tipo  | Descripción                                        | Valores                                                                          |
|-----------|:--------:|:------:|----------------------------------------------------|----------------------------------------------------------------------------------|
| field     | Opcional | String | Campo por el que particionar                       |                                                                                  |
| table     | Opcional | String | Tabla del campo por el que particionar             |                                                                                  |
| function  | Opcional | String | Aplica una función sql al campo de partición       | Los valores posibles se definen en [funciones over](#over-functions)              |

También acepta un elemento `case` anidado en lugar de un campo, de modo que una partición puede ser una expresión
compuesta. Esto refleja el [elemento group-by](#group-by-element): ambos se modelan igual,
por lo que una `function` o un `case` se comportan de forma idéntica en cualquiera de las dos cláusulas.

Particionar por una expresión compuesta:

```sql
SELECT COUNT(id) OVER (PARTITION BY YEAR(date)) as `yearCount` FROM tableId
```

se generará como:

```xml
<query id="testOverPartitionFunction">
  <table id="tableId"/>
  <over alias="yearCount">
    <field id="id" function="CNT"/>
    <partition-by field="date" function="YEAR"/>
  </over>
</query>
```

Particionar por una expresión case:

```xml
<query id="testOverPartitionCase">
  <table id="tableId"/>
  <over alias="groupCount">
    <field id="id" function="CNT"/>
    <partition-by>
      <case>
        <when left-field="amount" condition="gt" right-variable="limit">
          <then>
            <constant value="HIGH"/>
          </then>
        </when>
        <else>
          <constant value="LOW"/>
        </else>
      </case>
    </partition-by>
  </over>
  <variable id="limit" type="INTEGER" value="1000"/>
</query>
```

### Elemento join {#join-element}

La estructura de join es la siguiente:

```xml
<query id="query">
...
 <join type="[Type]">
  <table id="[Table id]" alias="[Table alias]"/>
  <and>
   <filter left-field="[Join field 1]" left-table="[Table join 1]" condition="eq" 
    right-field="[Join field 2]" right-table="[Table join 2]" ignorecase="[Ignorecase]" trim="[Trim]"/>
  </and>
 </join>
 ... (more joins)
...
</query>
```

El elemento *join* tiene los siguientes atributos:

| Atributo  |    Uso     |  Tipo  | Descripción    | Valores                                                                                                                                                                                    |
|-----------|:----------:|:------:|----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| type      |  Opcional  | String | Tipo de join SQL | Los valores posibles son: `FULL`, `LEFT`, `INNER` o `RIGHT` **Nota:**  El valor por defecto es `INNER`. Para ver más información sobre los joins lee [esta página](http://www.w3schools.com/sql/sql_join.asp) |


### Elemento union {#union-element}

La estructura de union es la siguiente:

```xml
...
<query id="query">
 <union type="[Type]" query="[Union subquery]"/>
  ... (more unions)
</query>
...
```

El elemento *union* tiene los siguientes atributos:

| Atributo  |      Uso      |    Tipo     | Descripción                                          | Valores                                  |
|-----------|:-------------:|:-----------:|------------------------------------------------------|------------------------------------------|
| query     | **Obligatorio**|   String    | Id de la consulta con la que combinar el resultado   | **Nota:**  La consulta de alias debe existir |
| type      |   Opcional    |   String    | Combina el resultado de dos o más sentencias SELECT  | Usa `ALL` para permitir valores duplicados |

### Elemento where {#where-element}

La estructura del elemento where es la siguiente:

```xml
  <query id="WhereTest">
    <table id="HISAweThm" />
    <field id="hisact" alias="typ" />
    <where>
      <and>
        <filter left-field="hisact" condition="eq" right-variable="top" />
        <filter left-field="hisope" condition="ne" right-variable="ope" />
      </and>
    </where>
    <variable id="top" type="INTEGER" value="1445" />
    <variable id="ope" type="STRING" value="mgr" />
  </query>
```

### Elemento having {#having-element}

La estructura de having es la siguiente, igual que la del elemento where:

```xml
  <query id="HavTst" public="true">
    <table id="HISAweThm" />
    <field id="hisact" alias="typ" />
    <field id="sum(1)" alias="act"/>
    <group-by field="HisAct"/>
    <having>
      <and>
        <filter left-field="sum(1)" condition="gt" right-variable="top" />
      </and>
    </having>
    <variable id="top" type="INTEGER" value="1445" />
  </query>
```

### Elemento filter {#filter-element}

La estructura de filter es la siguiente:

```xml
<filter left-field="[Field 1]" left-table="[Field table 1]" left-variable="[Variable Id]" condition="[Condition]" type="[Type]"  
        right-field="[Field 2]" right-table="[Field table 2]" right-variable="[Variable Id]" query="[Query Id]" ignorecase="[Ignorecase]" trim="[Trim]" optional="[Optional]"/>
```

> **¡NUEVO!** Ahora puedes definir hijos `left-operand` y `right-operand` para definir los filtros. 
> Estos elementos deben contener elementos `field`, `constant`, `operation`, `case` u `over`:

 ```xml
<filter condition="[Condition]" ignorecase="[Ignorecase]" trim="[Trim]" optional="[Optional]">
  <left-operand>
    <field id="[field name]"/>
  </left-operand>
  <right-operand>
    <constant value="[static value]" type="[value type]"/>
  </right-operand> 
</filter>
 ```


El elemento *filter* tiene los siguientes atributos:

| Atributo       |     Uso      |   Tipo   | Descripción                                                                                          | Valores                                                        |
|----------------|:------------:|:--------:|------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| left-field     |   Opcional   |  String  | El nombre de una columna                                                                             |                                                                |
| left-table     |   Opcional   |  String  | El nombre de la tabla a la que pertenece el *field*                                                  |                                                                |
| left-variable  |   Opcional   |  String  | El id de una variable                                                                                |                                                                |
| type           |   Opcional   |  String  | El tipo de los valores almacenados en las columnas que se comparan                                   | `NUMBER`, `DECIMAL_NUMBER`, `DATE`, `TIME`,	`STRING` (por defecto) |
| condition      |**Obligatorio**|  String  | La condición de la comparación                                                                       | Consulta las [condiciones de comparación](#comparison-conditions) |
| right-field    |   Opcional   |  String  | El nombre de una columna                                                                             |                                                                |
| right-table    |   Opcional   |  String  | El nombre de la tabla a la que pertenece el *right-field*                                            |                                                                |
| right-variable |   Opcional   |  String  | El id de una variable                                                                                |                                                                |
| query          |   Opcional   |  String  | El id de una consulta con la que comparar (lado derecho)                                             |                                                                |
| ignorecase     |   Opcional   | Boolean  | Si la comparación debe ignorar mayúsculas y minúsculas                                               | `true`, `false` (por defecto)                                  |
| trim           |   Opcional   | Boolean  | Si los valores deben recortarse antes de la comparación                                              | `true`, `false` (por defecto)                                  |
| optional       |   Opcional   | Boolean  | Si este filtro comprueba contra una variable y el valor de la variable es null, se elimina este filtro | `true`, `false` (por defecto)                                |

#### Condiciones de comparación {#comparison-conditions}

- `eq`: Igual
- `ne`: Distinto
- `ge`: Mayor o igual
- `le`: Menor o igual
- `gt`: Mayor que
- `lt`: Menor que
- `in`: El primer operando está en una lista definida por el segundo operando (subconsulta o lista de variables)
- `not in`: El primer operando **no** está en una lista definida por el segundo operando (subconsulta o lista de variables)
- `is null`: El primer operando es null
- `is not null`: El primer operando no es null
- `like`: El primer operando contiene parte del texto del segundo operando
- `not like`: El primer operando no contiene parte del texto del segundo operando
- `exists`: Solo para consultas, la subconsulta contiene valores
- `not exists`: Solo para consultas, la subconsulta no contiene valores

### Elemento group by {#group-by-element}

El elemento *group by* tiene los siguientes atributos:

| Atributo  |      Uso      |   Tipo    | Descripción                      | Valores                                                                |
|-----------|:-------------:|:---------:|----------------------------------|------------------------------------------------------------------------|
| field     |**Obligatorio**|  String   | Alias del campo por el que agrupar los resultados |                                                       |
| table     |   Opcional    |  String   | Alias de la tabla por la que agrupar los resultados |                                                     |
| function  |   Opcional    |  String   | Función a aplicar al campo       | Los valores posibles se definen en [funciones de campo](#field-functions) |

Además, puedes usar la operación `CASE` dentro de `group-by`.

### Elemento order by {#order-by-element}

El elemento *order by* tiene los siguientes atributos:

| Atributo  |      Uso      |   Tipo   | Descripción                      | Valores                                                                        |
|-----------|:-------------:|:--------:|----------------------------------|--------------------------------------------------------------------------------|
| field     |**Obligatorio**|  String  | Alias del campo por el que ordenar los resultados |                                                               |
| table     |   Opcional    |  String  | Alias de la tabla por la que ordenar el resultado |                                                               |
| function  |   Opcional    |  String  | Función a aplicar al campo       | Los valores posibles se definen en [funciones de campo](#field-functions)      |
| type      |   Opcional    |  String  | Dirección de ordenación          | Los valores posibles son `DESC` o `ASC`. Por defecto es `ASC`                  |
| nulls     |   Opcional    |  String  | Si se ordenan los campos nulos   | Los valores posibles son `FIRST` o `LAST`. Por defecto depende del tipo de base de datos |

## Elementos de posprocesamiento {#post-process-elements}

Los siguientes elementos son muy potentes para generar resultados, pero se evalúan 
**después** de la cláusula SQL generada, y pueden introducir cierta lentitud en la obtención de la consulta.
Tenlo en cuenta al usarlos cuando diseñes la obtención de datos en consultas grandes.

### Elemento computed {#computed-element}

El elemento *computed* tiene los siguientes atributos:

| Atributo  |     Uso      |   Tipo   | Descripción                                                                                                    | Valores                                                                                                                |
|-----------|:------------:|:--------:|----------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| alias     |**Obligatorio**|  String  | Nombre de salida del campo computed                                                                            |                                                                                                                        |
| format    |**Obligatorio**|  String  | Se usa para insertar el alias de otro campo como variables. Tiene la misma **sintaxis** que el elemento eval de **javascript** | (Ej. [code] - [description] tomará el campo code y lo concatenará con el campo description con la cadena " - " |
| eval      |   Opcional   | Boolean  | Evalúa el formato del computed como una expresión                                                              | Por defecto es `false`                                                                                                 |
| nullValue |   Opcional   |  String  | Establece un valor para los valores nulos en los campos computed                                               | Ej: `nullValue="ZERO"` establece "ZERO" en los valores nulos                                                           |
| transform |   Opcional   |  String  | Se usa para dar formato al valor del computed                                                                  | Consulta [esto](#transform-attribute) para más información sobre el atributo transform.                                |
| pattern   |   Opcional   |  String  | Se usa en un computed con valor numérico, define el patrón con el que dar formato al número                    | Consulta [esta página](http://docs.oracle.com/javase/tutorial/i18n/format/decimalFormat.html) para más información     |
| translate |   Opcional   |  String  | Traduce la salida con un identificador de grupo enumerado                                                      | **Nota:** Si el valor del campo es igual a un valor enumerado, devuelve la etiqueta del enumerado                      |
| label     |   Opcional   |  String  | Para usar una etiqueta internacional i18n en el computed                                                       | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales)                                          |

#### Ejemplos de computed {#computed-examples}

```xml
<!-- Computed for add string "Prueba" to field Nam -->
<query id="QryEdiSug" cacheable="true">
 <table id="AweThm"/>
 <field id="IdeThm" alias="value" />
 <field id="Nam" alias="name" />
 <computed alias="label" format="Prueba - [name]"/>
 <where>
  <or>
   <filter left-field="Nam" condition="like" right-variable="Nam" ignorecase="true"/>
  </or>
 </where>
 <variable id="Nam" type="STRINGB" name="suggest" />
</query>
```

```xml
<!-- Using computed to get value of field "Value" as a label field -->
<query id="ProNamLst" service="ProFilLst" cacheable="true">
  <field id="value" />
  <computed format="[value]" alias="label" />
</query>
```

### Elemento compound {#compound-element}

La estructura de *compound* es la siguiente:

```xml
<compound alias="[Compound alias]">
  <computed format="[Format]" alias="[Alias]"/>
  <computed format="[Format]" alias="[Alias]"/>
  ...
</compound>
```

El elemento *compound* tiene los siguientes atributos:

| Atributo  |      Uso       |    Tipo     | Descripción                 | Valores                                                |
|-----------|:--------------:|:-----------:|-----------------------------|--------------------------------------------------------|
| alias     |**Obligatorio** |   String    | Es el identificador del compound | **Nota:**  El nombre del alias debe ser único en la consulta |

#### Ejemplos de compound {#compound-examples}

> Usa el elemento compound para obtener estructuras de salida complejas.

```xml
<!-- This compound get label and icon from many computeds-->

<query id="DbsLst" cacheable="true">
 <table id="AweDbs"/>
 <field id="IdeDbs" alias="IdeDbs" />
 <field id="Als" alias="Als" />
 <field id="Des" alias="Des" />
 <field id="Act" alias="Act" />
 <field id="Act" alias="ActTxt" translate="Es1Es0"/>
 <compound alias="ActIco">
  <computed format="GENERAL_STATUS_FA_[Act]" alias="icon"/>
  <computed format="[ActTxt]" alias="label"/>
 </compound>
</query>
```
Uso del compound de icono:

* El `alias` del compound debe coincidir con el `id` del campo de icono en la rejilla.
* El elemento computed con el alias `icon` contiene el icono que se asignará al campo de icono.
  En este caso, hay un enumerado con los identificadores de los iconos.
* El elemento computed con el alias `label` contiene la cadena que se mostrará al pasar el ratón por encima.

```xml
<!-- This compound get label and value as [Nam] field from many computeds fields -->

<query id="ScrCnfLst" cacheable="true">
 <table id="AweScrCnf" alias="scrCnf"/>
 <field id="IdeAweScrCnf" table="scrCnf" alias="IdeAweScrCnf" />
 <field id="IdeOpe" table="scrCnf" alias="IdeOpe" />
 <field id="IdePro" table="scrCnf" alias="IdePro" />
 <field id="Nam" table="scrCnf" alias="NamVal" />
 <compound alias="Nam">
  <computed format="[NamVal]" alias="value"/>
  <computed format="[NamVal]" alias="label"/>
 </compound>
 <where>
  <and> 
   <filter left-field="Act" left-table="scrCnf" condition="eq" right-variable="Act" optional="true"/>
  </and>
 </where>
 <variable id="Act" type="INTEGER" name="CrtAct" />
</query>
```

### Elemento totalize {#totalize-element}

La estructura de totalize es la siguiente:

```xml
<query id="query">
...
<totalize function="[Function]" label="[Label]" field="[Field]" style="[Style]">
  <totalize-by field="[Totalize field]"/>
  ... (more totalize by fields)
  <totalize-field field="[Totalize field]"/>
  ... (more totalized fields)
</totalize>
... (more totalize)
...
</query>
```

El elemento *totalize* tiene los siguientes atributos:

| Atributo  |     Uso      |  Tipo   | Descripción                                                         | Valores                                                                                         |
|-----------|:------------:|:-------:|---------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| function  |**Obligatorio**| String  | Función usada para generar el total                                 | Los valores posibles se definen en [funciones de agregación](#aggregation-functions-for-totalize) |
| label     |**Obligatorio**| String  | Etiqueta del texto que aparecerá en `totalizer-field` en las filas totalizadas |                                                                      |
| field     |**Obligatorio**| String  | Campo donde se mostrará la etiqueta del totalizador                 |                                                                                                 |
| style     |**Obligatorio**| String  | Es el estilo css que se establece en el widget de la rejilla        | Los valores posibles son `TOTAL` o `SUBTOTAL`                                                   |

El elemento totalize tiene los siguientes elementos:

| Elemento       |      Uso      | Varias instancias   | Descripción                                                                                                                                                                         |
|----------------|:-------------:|:-------------------:|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| totalize-field |**Obligatorio**|         Sí          | Tiene el atributo `field` para establecer el alias del campo al que aplicar la totalización                                                                                         |
| totalize-by    |**Obligatorio**|         Sí          | Tiene el atributo `field` para establecer el alias del campo por el que agrupar en la totalización y el atributo `function` para definir una [función de agregación](#aggregation-functions-for-totalize) específica |

#### Funciones de agregación para totalize {#aggregation-functions-for-totalize}

- `AVG`: Media de valores
- `CNT`: Cuenta valores
- `CNT_DISTINCT`: Cuenta valores distintos
- `MAX`: Valor máximo
- `MIN`: Valor mínimo
- `SUM`: Suma de valores
- `FIRST_VALUE`: Primer valor
- `LAST_VALUE`: Último valor

#### Ejemplos de totalize {#totalize-examples}

```xml
<!-- Test matrix with totalizer -->
  <query id="QrySitModDbsOrdTot" cacheable="true">
    <table query="QrySitModDbsOrd" alias="TotLst"/>
    <field id="IdeSitModDbs" table="TotLst" alias="IdeSitModDbs"/>
    <field id="IdeSit" table="TotLst" alias="IdeSit"/>
    <field id="NamSit" table="TotLst" alias="NamSit"/>
    <field id="IdeMod" table="TotLst" alias="IdeMod"/>
    <field id="NamMod" table="TotLst" alias="NamMod"/>
    <field id="IdeDbs" table="TotLst" alias="IdeDbs"/>
    <field id="Als" table="TotLst" alias="Als"/>
    <field id="Ord" table="TotLst" alias="Ord" transform="NUMBER"/>
    <totalize function="SUM" label="Subtotal" field="NamMod" style="SUBTOTAL">
      <totalize-field field="Ord"/>
      <totalize-by field="IdeMod"/>      
    </totalize>
    <totalize function="SUM" label="Total" field="Als" style="TOTAL">
      <totalize-field field="Ord"/>
    </totalize>
  </query>
```

### Elemento variable {#variable-element}

El elemento *variable* tiene los siguientes atributos:

| Atributo  |     Uso      |  Tipo   | Descripción                                                                                                                                                                                                                                                | Valores                                                                                                                                                                                   |
|-----------|:------------:|:-------:|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| id        |**Obligatorio**| String  | Identificador de la variable                                                                                                                                                                                                                               | **Nota:**  El id debe ser único                                                                                                                                                           |
| type      |**Obligatorio**| String  | Tipo de variable                                                                                                                                                                                                                                           | Los valores posibles están disponibles [aquí](#variable-types)                                                                                                                            |
| name      |   Opcional   | String  | Nombre de la variable. Es el nombre del componente con el que interactuamos en la pantalla                                                                                                                                                                 | **Nota:** En algunos casos puede ser útil conocer el nombre del criterio con el que interactuamos. Si definimos la variable como name="component", enviará el id del criterio            |
| value     |   Opcional   | String  | Para definir un valor estático de la variable                                                                                                                                                                                                              |                                                                                                                                                                                           |
| session   |   Opcional   | String  | La variable se establece con un valor de sesión                                                                                                                                                                                                            |                                                                                                                                                                                           |
| property  |   Opcional   | String  | La variable se establece con el valor de una propiedad                                                                                                                                                                                                     |                                                                                                                                                                                           |
| optional  |   Opcional   | Boolean | Indicador de si la variable es opcional. Si el criterio configurado en la variable puede existir o no. Si es opcional y no existe, la consulta no se ejecutará. Si no es opcional y la variable no existe se mostrará un error. | **Nota:** No se recomienda configurar como opcionales los criterios de tipo suggest, porque puede provocar un mal comportamiento                                                          |

#### Tipos de variable {#variable-types}

Estos son los tipos de variable posibles:

* **STRINGL**: una cadena con % en el lado izquierdo (para el operador `LIKE`)
* **STRINGR**: una cadena con % en el lado derecho (para el operador `LIKE`)
* **STRINGB**: una cadena con % en ambos lados (para el operador `LIKE`)
* **STRINGN**: una cadena que tiene `NULL` cuando está vacía
* **STRING**: una cadena que tiene "" cuando está vacía
* **STRING_HASH**: aplica la función Sha 256 a la variable de cadena
* **STRING_ENCRYPT**: aplica la función encryptRipEmd160 a la variable de cadena
* **INTEGER**: número entero
* **FLOAT**: número float (32 bits)
* **DOUBLE**: número double (64 bits)
* **DATE**: fecha web (`dd/MM/aaaa`)
* **TIME**: hora web (`HH:mm:ss`)
* **TIMESTAMP**: marca de tiempo web (`dd/MM/aaaa HH:mm:ss`)
* **SYSTEM_DATE**: Fecha del servidor (almacenada como fecha) (`dd/MM/aaaa`)
* **SYSTEM_TIME**: Hora del servidor (almacenada como cadena) (`HH:mm:ss`)
* **SYSTEM_TIMESTAMP**: Fecha del servidor (almacenada como marca de tiempo con milisegundos) (`dd/MM/aaaa HH:mm:ss.SSS`)
* **NULL**: Para pasar un valor `null`
* **OBJECT**: Para definir una variable como un objeto java
* **CLOB**: Para definir una variable como un archivo de texto grande
* **LIST_TO_STRING**: Recupera una lista de valores y los gestiona como valores separados por comas en una cadena
* **STRING_TO_LIST**: Recupera una cadena separada por comas y la transforma en una lista de valores
		
### Variables especiales {#special-variables}

Hay un conjunto de variables especiales que **no necesitan declararse como variable en la consulta** y
afectan al comportamiento de los resultados:

- **lang**: Hace que la traducción (con el atributo `translate="XxxXxx"`) use este idioma en lugar del de la
sesión

## Consulta de enumerado {#enumerated-query}

Una consulta de enumerado es una llamada a un grupo enumerado del archivo **Enumerated.xml**.

No recibirá ninguna variable de entrada, y devolverá una lista con dos campos: **value** y **label**.

Puedes consultar la estructura xml de los enumerados en [esta página](enumerate-definition.md)

### Estructura xml de enumerado {#xml-enumerated-structure}

```xml
<!-- Example enumerated query -->
<query id="[Query id]" enumerated="[Id enumerated]" cacheable="[cacheable]">
  <field id="[Field label]"/>
  <field id="[Field value]"/>
</query>
```

> # **Nota:** Todos los enumerados se definen en el archivo `Enumerated.xml` de la **carpeta global**. Consulta la [estructura del proyecto](../guides/project-structure.md#global-folder) para más información.

### Ejemplos de enumerado {#enumerated-examples}

```xml
<!-- Enumerated YES:1|NO:0 -->
<query id="Es1Es0" enumerated="Es1Es0">
  <field id="value"/>
  <field id="label"/>
</query>
```

```xml
<!-- Enumerated YES:Y|NO:N -->
<query id="EsyEnn" enumerated="EsyEnn">
  <field id="value"/>
  <field id="label"/>
</query>
```

## Consulta de servicio {#service-query}

Tipo especial de consultas usadas para llamar a servicios, ya sean **servicios java** o **servicios web**. Una consulta de servicio se compone de variables de entrada y campos de salida.

Puedes ver la estructura xml de los servicios en [esta página](service-definition.md)

> **Nota:** Todos los servicios se definen en el archivo `Services.xml` de la **carpeta global**. Consulta la [estructura del proyecto](../guides/project-structure.md#global-folder) para más información.

### Estructura xml de servicio {#xml-service-structure}

```xml
<query id="[Query ID]" service="[Service ID]" public="[Public]">
  <field id="[Field Id 1]"/>
  ...
  <field id="[Field Id n]"/>
  <computed alias="[Alias]" format="[Format]"/>
  ...
  <computed alias="[Alias]" format="[Format]"/>
  <variable id="[Variable ID1]" type="[Variable type]" name="[Variable name 1]" />
  ...
  <variable id="[Variable IDn]" type="[Variable type]" name="[Variable name N]" />
  <order-by field="[Order field]" table="[Order table]" type="[Order type]" nulls="[Nulls first or last]"/>
</query>
```

> **IMPORTANTE:** El **orden** de los campos en query.xml debe ser el mismo que el definido en el servicio en el archivo Services.xml

### Ejemplos de servicio {#service-examples}

* **Ejemplo de servicio java:**

`Código del xml de consulta`

```xml
<!-- Encrypt text (Service encryptText) -->
<query id="GetEncTxt" service="SerEncTxt" cacheable="true">
  <field id="value" />
  <field id="label" />
  <variable id="text" type="STRING" name="CrtTxt"/>
  <variable id="phraseKey" type="STRING" name="CrtPhr"/>
</query>
```

`Código del xml de servicio`

```xml
<service id="SerEncTxt">
  <java classname="com.almis.awe.core.services.controller.AccessController" method="encryptText">
    <service-parameter type="STRING" name="text" />
    <service-parameter type="STRING" name="phraseKey" />
  </java>
</service>
```

* **Ejemplo de servicio web:**

`Código del xml de consulta`

```xml
<query id="BoCptMomLiqTyp" service="FmbBoCptMomLiqTyp">
  <field id="value"/>
  <field id="orp_des"/>
  <field id="lab"/>
  <field id="orp_ext"/>
  <computed alias="label" format="[orp_des] - [lab] - [orp_ext]"/>
  <variable id="FldIde" type="STRING" value="LiqTyp" optional="false"/>
  <variable id="LiqTyp" type="STRING" name="LiqTyp" optional="false"/>
</query>
```

`Código del xml de servicio`

```xml
<service id="FmbBoCptMomLiqTyp">
  <web name="FmbBoCptMomLiqTyp" type="DATA">
    <service-parameter type="STRING" name="FldIde" list="false"/>
    <service-parameter type="STRING" name="LiqTyp" list="false"/>
  </web>
</service>
```

## Consulta de cola {#queue-query}

Tipo especial de consultas usadas para comunicarse con **colas de mensajes**. Una consulta de cola se compone de variables de entrada y campos de salida.

Puedes ver la estructura xml de las colas en [esta página](jms-queues-definition.md)

> **Nota:** Todas las colas se definen en el archivo `Queues.xml` de la **carpeta global**. Consulta la [estructura del proyecto](../guides/project-structure.md#global-folder) para más información.

### Estructura xml de cola {#xml-queue-structure}

```xml
<query id="[Query ID]" queue="[Queue ID]" public="[Public]">
  <!-- Output parameters -->
  <field id="[Field Id 1]"/>
  ...
  <field id="[Field Id n]"/>
  <computed alias="[Alias]" format="[Format]"/>
  ...
  <computed alias="[Alias]" format="[Format]"/>
  <!-- Input parameters -->
  <variable id="[Variable ID1]" type="[Variable type]" name="[Variable name 1]" />
  ...
  <variable id="[Variable IDn]" type="[Variable type]" name="[Variable name N]" />
</query>
```

### Ejemplos de cola {#queue-examples}

* **Ejemplo de consulta de cola:**

`Código del xml de consulta`

```xml
<!-- Queues: Fill a criterion with a wrapper values -->
<query id="TstSynQueWrpTxt" queue="SynQueWrpTxt">
  <!-- Input parameters -->
  <variable id="CrtVen" type="INTEGER" value="4"/>
  <variable id="CrtPue" type="INTEGER" value="2"/>
  <!-- Output parameters -->
  <field id="OutFld1" alias="value" />
  <field id="OutFld2" alias="label" />
</query>
```

`Código del xml de colas`

```xml
<!-- Queue retreive sync test with wrappers -->
<queue id="SynQueWrpTxt">
  <request-message destination="AweReq" type="TEXT" selector="wrapper">
    <message-wrapper type="XML" classname="com.almis.awe.core.wrappers.test.Casa"/>
  </request-message>
  <response-message destination="AweRes" type="TEXT">
    <message-wrapper type="XML" classname="com.almis.awe.core.wrappers.test.Casa"/>
  </response-message>
</queue>
```
