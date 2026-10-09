---
id: screen
title: Elemento pantalla
---

The `screen` is the container tag for all screen components. In this tag you have to define the JSP `template` the screen is going to render, the screen title (`label`), and some other features like the `target`, which is a list of queries to launch that will fill the criteria values.

## Esqueleto de XML {#xml-skeleton}

La estructura básica de una pantalla XML es la siguiente:

```xml
<screen template="[template]" label="[screen-title-literal]" keep-criteria="[keep-criteria]" 
 target="[query-targets]" onload="[manintain-onload]" onunload="[maintain-onunload]"
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 xsi:noNamespaceSchemaLocation="[schema-location]">
  <tag source="[source1]">...</tag>
  <tag source="[source2]">...</tag>
</screen>
```

## Estructura de pantalla {#screen-structure}

Para facilitar el desarrollo, no todas las etiquetas son necesarias.

| Elemento                     | Uso          | Varias instancias | Descripción            |
| ---------------------------- | ------------ | ----------------- | ---------------------- |
| [screen](#screen-attributes) | **Required** | No                | Contenedor de pantalla |
| [tag](tags.md)               | Opcional     | Si                | Descriptores de origen |

## Atributos de pantalla {#screen-attributes}

El elemento de pantalla tiene los siguientes atributos:

| Atributo        | Uso          | Tipo    | Descripción                                                                                                                                                                      | Valores                                                                                                                                                                |
| --------------- | ------------ | ------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| template        | **Required** | String  | Plantilla que va a procesar el archivo XML                                                                                                                                       | `full`, `window`                                                                                                                                                       |
| label           | Opcional     | String  | Título de la pantalla                                                                                                                                                            | **Note:** You can use [i18n](i18n-internationalization.md) files (locales)                                                          |
| keep-criteria   | Opcional     | Boolean | Guarda los datos de la pantalla para mostrarlos cuando el usuario regresa a la pantalla (sólo los datos de entradas, no las matrices o los datos de gráficos) | Default value is `false`                                                                                                                                               |
| target          | Opcional     | String  | Consultas iniciales que inicializan todos los valores de los criterios en la pantalla. Queries columns must match [criteria](criteria.md) identifiers            | [Query](query-definition.md) identifiers, separated by commas `,`                                                                                                      |
| onload          | Opcional     | String  | Mantenimiento que se lanza al cargar la pantalla                                                                                                                                 | Identificador del mantenimiento                                                                                                                                        |
| onunload        | Opcional     | String  | Mantenimiento que se lanza al salir de la pantalla                                                                                                                               | Identificador del mantenimiento                                                                                                                                        |
| schema-location | **Required** | String  | Ubicación de los archivos XSD                                                                                                                                                    | Ejemplo de pantallas AWE: "https://aweframework.gitlab.io/awe/docs/schemas/screen.xsd" |
