---
id: tab-and-tabcontainer
title: Pestañas y contenedor de pestaña
---

Una **pestaña** es un componente muy útil para dividir los componentes de la pantalla en *pestañas*:

<img alt="Pestaña" src={require('@docusaurus/useBaseUrl').default('img/Tab.png')} />

## Esqueleto de XML {#xml-skeleton}

```xml 
<tab id="[tab-identifier]" initial-load="[initial-load]" target-action="[target-action]" maximize="[maximize-tab]">
  <tabcontainer id="[tabcontainer-identifier-1]">
  ...
  <tabcontainer id="[tabcontainer-identifier-n]">
  <dependency...></dependency>
  <context-menu...></context-menu>
</tab>
```

The tab list is filled with the `value` and `label` fields of the query/enum launched with `[target-action]`.

## Estructura de pestañas {#tab-structure}

```xml
<tab id="[tab-identifier]" initial-load="[initial-load]" target-action="[target-action]">
   ...
</tab>
```

| Elemento                                 |       Uso       | Varias instancias | Descripción                                                |
| ---------------------------------------- |:---------------:|:-----------------:| ---------------------------------------------------------- |
| [tab](#tab-attributes)                   | **Obligatorio** |        No         | Nodo global de pestaña. Define los atributos de la pestaña |
| [tabcontainer](#tabcontainer-attributes) | **Obligatorio** |        Si         | Lista de contenedores de pestañas a mostrar                |
| [dependency](dependencies.md)            |    Opcional     |        Si         | Lista de dependencias de la pestaña                        |
| [context-menu](context-menu.md)          |    Opcional     |        No         | Menú contextual de la pestaña                              |

## Atributos de pestaña {#tab-attributes}

| Atributo      |       Uso       |  Tipo   | Descripción                                                                                     | Valores                                                                                             |
| ------------- |:---------------:|:-------:| ----------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| id            | **Obligatorio** | String  | Identificador de pestañas. Tiene que coincidir con los valores definidos en el `target-action`. |                                                                                                     |
| carga-inicial | **Obligatorio** | String  | Llamada de acción al servidor para cargar los datos de criterio (se lanza al cargar la ventana) | `enum` (para [enumerado](enumerate-definition.md)) o `query` (para [consulta](query-definition.md)) |
| target-action | **Obligatorio** | String  | Identificador de consulta en el servidor                                                        |                                                                                                     |
| style         |    Opcional     | String  | Clases CSS de la pestaña                                                                        | **Nota:** Aquí puedes usar la clase `expand` para permitir que la ventana se expanda                |
| maximize      |    Opcional     | Boolean | Mostrar el icono maximizar o no                                                                 |                                                                                                     |
| orientation   |    Opcional     | String  | Wizard steps orientation                                                                        | Default value is `vertical`                                                                         |


## Estructura del contenedor de pestaña {#tabcontainer-structure}

A tabcontainer is a window opened when a tab is selected. Note that `[tabcontainer-identifier]` must match the value of the query/enumerated list set at `Tab` element.

```xml
<tabcontainer id="[tabcontainer-identifier]" type="[type]" label="[label]" style="[style]" expandible="[expandible]">
  ...
</tabcontainer>
```

## Atributos del contenedor de pestaña {#tabcontainer-attributes}

| Atributo   |       Uso       |  Tipo  | Descripción                                                                                        | Valores                                                                              |
| ---------- |:---------------:|:------:| -------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| id         | **Obligatorio** | String | Identificador del contenedor. Tiene que coincidir con los valores definidos en el `target-action`. |                                                                                      |
| label      |    Opcional     | String | Título del contenedor                                                                              | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md)                 |
| style      |    Opcional     | String | Clases CSS del contenedor                                                                          | **Nota:** Aquí puedes usar la clase `expand` para permitir que la ventana se expanda |
| type       |    Opcional     | String | Tipo de etiqueta HTML del contenedor                                                               | `div`, `span`, `p`, ...                                                              |
| expandible |    Opcional     | String | Cómo [expandir](layout.md) los elementos del contenedor                                            | `vertical`, `horizontal`                                                             |