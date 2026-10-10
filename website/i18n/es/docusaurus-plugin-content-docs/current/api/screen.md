---
id: screen
title: Elemento pantalla
---

La etiqueta `screen` es la etiqueta del contenedor para todos los componentes de pantalla. En esta etiqueta tienes que definir la plantilla `template` de la pantalla se va a renderizar, el título de la pantalla (`label`), y algunas otras características como el `target`, que es una lista de consultas que se lanzará al pintar la ventana y que rellenará los valores de los criterios.

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

| Elemento                     | Uso             | Varias instancias | Descripción            |
| ---------------------------- | --------------- | ----------------- | ---------------------- |
| [screen](#screen-attributes) | **Obligatorio** | No                | Contenedor de pantalla |
| [tag](tags.md)               | Opcional        | Si                | Descriptores de origen |

## Atributos de pantalla {#screen-attributes}

El elemento de pantalla tiene los siguientes atributos:

| Atributo        | Uso             | Tipo    | Descripción                                                                                                                                                                          | Valores                                                                                                                             |
| --------------- | --------------- | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------- |
| template        | **Obligatorio** | String  | Plantilla que va a procesar el archivo XML                                                                                                                                           | `full`, `window`                                                                                                                    |
| label           | Opcional        | String  | Título de la pantalla                                                                                                                                                                | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                                                                |
| keep-criteria   | Opcional        | Boolean | Guarda los datos de la pantalla para mostrarlos cuando el usuario regresa a la pantalla (sólo los datos de entradas, no las matrices o los datos de gráficos)                        | Por defecto es `false`                                                                                                              |
| target          | Opcional        | String  | Consultas iniciales que inicializan todos los valores de los criterios en la pantalla. Las columnas de consultas deben coincidir con los [identificadores de criterios](criteria.md) | [Identificadores de consulta](query-definition.md) separados por comas `,`                                                          |
| onload          | Opcional        | String  | Mantenimiento que se lanza al cargar la pantalla                                                                                                                                     | Identificador del mantenimiento                                                                                                     |
| onunload        | Opcional        | String  | Mantenimiento que se lanza al salir de la pantalla                                                                                                                                   | Identificador del mantenimiento                                                                                                     |
| schema-location | **Obligatorio** | String  | Ubicación de los archivos XSD                                                                                                                                                        | Ejemplo para pantallas de awe: `https://aweframework.gitlab.io/awe/docs/schemas/screen.xsd` (ver [herramientas XSD](../guides/xsd-tooling.md)) |
