---
id: tab-and-tabcontainer
title: Pestañas y contenedor de pestaña
---

A **tab** is a very useful screen component to separate the screen components in _tabs_:

<img alt="Tab" src={require('@docusaurus/useBaseUrl').default('img/Tab.png')} />

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

| Elemento                                 |      Uso     | Varias instancias | Descripción                                                                |
| ---------------------------------------- | :----------: | :---------------: | -------------------------------------------------------------------------- |
| [tab](#tab-attributes)                   | **Required** |         No        | Nodo global de pestaña. Define los atributos de la pestaña |
| [tabcontainer](#tabcontainer-attributes) | **Required** |         Si        | Lista de contenedores de pestañas a mostrar                                |
| [dependency](dependencies.md)            |   Opcional   |         Si        | Lista de dependencias de la pestaña                                        |
| [context-menu](context-menu.md)          |   Opcional   |         No        | Menú contextual de la pestaña                                              |

## Atributos de pestaña {#tab-attributes}

| Atributo      |      Uso     |   Tipo  | Descripción                                                                                                                                | Valores                                                                                                                                     |
| ------------- | :----------: | :-----: | ------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------- |
| id            | **Required** |  String | Identificador de pestañas. Tiene que coincidir con los valores definidos en el <code>target-action</code>. |                                                                                                                                             |
| carga-inicial | **Required** |  String | Llamada de acción al servidor para cargar los datos de criterio (se lanza al cargar la ventana)                         | `enum` (for [enumerated](enumerate-definition.md)) or `query` (for [query call](query-definition.md)) |
| target-action | **Required** |  String | Destino para llamar al servidor                                                                                                            |                                                                                                                                             |
| style         |   Opcional   |  String | Clases CSS de la pestaña                                                                                                                   | **Note:** Here you can use `expand` class to set the tab as expandible                                                      |
| maximize      |   Opcional   | Boolean | Mostrar el icono maximizar o no                                                                                                            |                                                                                                                                             |

## Estructura del contenedor de pestaña {#tabcontainer-structure}

A tabcontainer is a window opened when a tab is selected. Note that `[tabcontainer-identifier]` must match the value
of the query/enumerated list set at `Tab` element.

```xml
<tabcontainer id="[tabcontainer-identifier]" type="[type]" label="[label]" style="[style]" expandible="[expandible]">
  ...
</tabcontainer>
```

## Atributos del contenedor de pestaña {#tabcontainer-attributes}

| Atributo   |      Uso     |  Tipo  | Descripción                                                                                                                                   | Valores                                                                                                       |
| ---------- | :----------: | :----: | --------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| id         | **Required** | String | Identificador del contenedor. Tiene que coincidir con los valores definidos en el <code>target-action</code>. |                                                                                                               |
| label      |   Opcional   | String | Título del contenedor                                                                                                                         | **Note:** You can use [i18n](i18n-internationalization.md) files (locales) |
| style      |   Opcional   | String | Clases CSS del contenedor                                                                                                                     | **Note:** Here you can use `expand` class to set the window as expandible                     |
| type       |   Opcional   | String | Tipo de etiqueta HTML del contenedor                                                                                                          | `div`, `span`, `p`, ...                                       |
| expandible |   Opcional   | String | How to [expand](layout.md) the tabcontainer children                                                                                          | `vertical`, `horizontal`                                                                                      |