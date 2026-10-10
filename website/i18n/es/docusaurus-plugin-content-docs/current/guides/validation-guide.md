---
id: validation
title: Validación
sidebar_label: Validación
---

La acción de validación comprueba las condiciones de cada criterio con un valor en el atributo validation. Esta acción está relacionada con un botón de tipo `validate` (consulta [acciones de botón](../api/button.md#button-actions)).

<img alt="ValidateImage" src={require('@docusaurus/useBaseUrl').default('img/ValidateImage.png')} />

## Esqueleto de XML {#xml-skeleton}

```xml 
<criteria [attributes] validation="[condition to validate]" />
```

## Tipos de validación {#validation-types}

Hay varios tipos de validación:

| Valor del atributo | Componente                            | Descripción                                                                             | Ejemplos                                                                                      |
|--------------------|---------------------------------------|-----------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| **required**       | **Todos** excepto `checkbox` y `radio` | Comprueba si el campo tiene valor                                                      | `validation="{required:true}"`                                                                |
| **text**           | `Text` / `Textarea` / `Password`      | Comprueba si el valor del criterio es un texto (no se permiten números ni espacios en blanco) | `validation="{text:true}"`                                                               |
| **textWithSpaces** | `Text` / `Textarea` / `Password`      | Comprueba si el valor del criterio es un texto (no se permiten números)                 | `validation="{textWithSpaces:true}"`                                                          |
| **number**         | `Text` / `Textarea` / `Password`      | Comprueba si el valor del criterio es un número                                         | `validation="number"`                                                                         |
| **integer**        | `Text` / `Textarea` / `Password`      | Comprueba si el valor es un entero bien formado                                         | `validation="integer"`                                                                        |
| **digits**         | `Text` / `Textarea` / `Password`      | Comprueba si el valor es un número con solo dígitos                                     | `validation="digits"`                                                                         |
| **date**           | `Text` / `Textarea` / `Password`      | Comprueba si el valor es una fecha válida                                               | `validation="date"`                                                                           |
| **time**           | `Text` / `Textarea` / `Password`      | Comprueba si el valor es una hora válida (hora, minutos y segundos)                     | `validation="time"`                                                                           |
| **email**          | `Text` / `Textarea` / `Password`      | Comprueba si el valor es una cadena de correo electrónico válida                        | `validation="email"`                                                                          |
| **gt**             | **Todos** excepto `checkbox` y `radio` | Comprueba si el valor del criterio es mayor que `value`                                | `validation="{gt:`[value](#validation-values)`}"`                                             |
| **ge**             | **Todos** excepto `checkbox` y `radio` | Comprueba si el valor del criterio es mayor o igual que `value`                        | `validation="{ge:`[value](#validation-values)`}"`                                             |
| **lt**             | **Todos** excepto `checkbox` y `radio` | Comprueba si el valor del criterio es menor que `value`                                | `validation="{lt:`[value](#validation-values)`}"`                                             |
| **le**             | **Todos** excepto `checkbox` y `radio` | Comprueba si el valor del criterio es menor o igual que `value`                        | `validation="{le:`[value](#validation-values)`}"`                                             |
| **eq**             | **Todos** excepto `checkbox` y `radio` | Comprueba si el valor del criterio es igual a `value`                                  | `validation="{eq:`[value](#validation-values)`}"`                                             |
| **ne**             | **Todos** excepto `checkbox` y `radio` | Comprueba si el valor del criterio es distinto de `value`                              | `validation="{ne:`[value](#validation-values)`}"`                                             |
| **mod**            | `Numeric` / `Date` / `Time`           | Comprueba si el valor del criterio es divisible por `value`                             | `validation="{mod:`[value](#validation-values)`}"`                                            |
| **range**          | `Numeric` / `Date` / `Time`           | Comprueba si el valor está dentro del rango                                             | `validation="{range:{from:`[value](#validation-values)`, to:`[value](#validation-values)`}}"` |
| **equallength**    | `Text` / `Textarea` / `Password`      | Comprueba si la longitud del texto es igual a `value`                                   | `validation="{equallength:`[value](#validation-values)`}"`                                     |
| **maxlength**      | `Text` / `Textarea` / `Password`      | Comprueba si la longitud del texto es menor que `value`                                 | `validation="{maxlength:`[value](#validation-values)`}"`                                       |
| **minlength**      | `Text` / `Textarea` / `Password`      | Comprueba si la longitud del texto es mayor que `value`                                 | `validation="{minlength:`[value](#validation-values)`}"`                                      |
| **checkAtLeast**   | `Checkbox` / `Button checkbox`        | Asegura que haya al menos `value` casillas marcadas en el grupo                         | `validation="{checkAtLeast:`[value](#validation-values)`}"`                                   |
| **maxRepeat**      | `Grid columns`                        | Comprueba si un elemento se repite `value` veces en su columna de la tabla            | `validation="{maxRepeat:`[value](#validation-values)`}"`                                      |
| **pattern**        | `Text` / `Textarea` / `Password`      | Comprueba si el texto es igual al parámetro                                             | `validation="{pattern:`[value](#validation-values)`}"`                                        |
 
### Valores de validación {#validation-values}

Puedes asignar un valor estático simple al comparador de validación:

```javascript 
{eq:3}
```

... o puedes tomar el valor de un ajuste (setting) o de un criterio:

```javascript 
{eq:{criterion:"CriterionId",type:"date"}}
```

Estos son los posibles atributos de un comparador de valor:

| Valor del atributo  | Descripción                                                 | Ejemplos                                    | 
| ------------------- | ----------------------------------------------------------- | ------------------------------------------- | 
| value               | Valor constante con el que comparar                         | `value:5`                                   | 
| criterion           | Obtiene el valor de un criterio                             | `criterion:"CriterionId"`                   |
| setting             | Obtiene el valor de un ajuste (setting)                     | `setting:"minlengthPassword"`               |
| message             | Locale del mensaje a obtener si no se supera la validación  | `message:"VALIDATION_MESSAGE_CUSTOM_ERROR"` |
| type                | Tipo de comparación                                         | `type:"`[type](#comparison-types)`"`        |

### Tipos de comparación {#comparison-types}

Puedes definir el tipo de la comparación, para comprobar los valores como un tipo definido:

| Tipo    | Descripción                    | 
| ------- | ------------------------------ | 
| date    | Compara como fechas            | 
| integer | Compara como enteros           | 
| float   | Compara como números decimales |  
| string  | Compara como cadenas (por defecto) |  
       
## Validación múltiple {#multiple-validation}

Es posible tener más de una acción de validación en un único atributo `validation`. Para ello, es necesario insertar las validaciones en formato json:

```javascript 
{eq:{criterion:"Field1"},ne:{criterion:"Field2"}}
```

Hay una excepción con las reglas de validación simples que no tienen parámetros, como `required`. En este caso, no es necesario poner la regla entre llaves `{}`:

```xml
validation="required"
```

Si quieres combinar reglas simples con reglas complejas, debes definirla también como una regla compleja: 

```javascript 
{required:true, maxlength:{value:6, type:"integer"}}
```

## Ejemplos {#examples}

### Criterios estándar con acción de validación {#standard-criteria-with-validation-action}

```xml
<criteria label="PARAMETER_TEXT" id="TxtReq" variable="TxtReq" component="text" style="col-xs-6 col-sm-3 col-lg-2" validation="{required:true, maxlength:4}"/>

```

### Comprobar que la contraseña es válida {#check-that-the-password-is-valid}

```xml
<criteria label="PARAMETER_PASSWD" id="Pas" component="text" 
validation="{required: true, pattern:{setting:'passwordPattern'}, ne:{criterion:'OldPas', message:'VALIDATOR_MESSAGE_REPEAT_OLD_PASSWORD'}, minlength:{setting:'minlengthPassword'}}" 
style="col2" />
```

### Comprobar si una fecha es mayor o igual que otra {#check-if-a-date-is-greater-or-equal-than-another}

```xml
<criteria label="PARAMETER_FILTERED_DATE" id="FilCalReq" variable="FilCalReq" component="filtered-calendar" initial-load="query" 
target-action="FilCalDat" style="col-xs-6 col-sm-3 col-lg-2" validation="{required:true,ge:{criterion:'FilCal',type:'date'}}" strict="false" />
```
