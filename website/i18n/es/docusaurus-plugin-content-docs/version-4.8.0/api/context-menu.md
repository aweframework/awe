---
id: context-menu
title: Menú contextual
---

El motor de Awe permite definir un menú contextual dentro de una matriz, criterios o gráficas. Con estos menús, se puede añadir funcionalidad a la ventana.

<img alt="GridContextMenu" src={require('@docusaurus/useBaseUrl').default('img/GridContextMenu.png')} />

## Estructura Xml {#xml-structure}

La estructura xml de los menús contextuales es la siguiente:

```xml
<context-button id="[Id]" label="[label]" icon=[icon]">
  <context-button id="[Id]"  label="[label]" icon=[icon]>
    <button-action type="[action-type]" target="[target]" silent="[silent]" />
  </context-button>
    <context-button id="[Id]"  label="[label]" icon=[icon]>
      <button-action type="[action-type]" target="[target]" silent="[silent]" />
    </context-button>
    <context-separator/>
    <context-button id="[Id]"  label="[label]" icon=[icon]>
      <button-action type="[action-type]" target="[target]" silent="[silent]" />
      ... more button actions
    </context-button>
    <dependencies/>
    .. more context-button
    .. more context-separator
  </context-button>
  .. more context-button
  .. more context-separator
</context-button>
```

## Elementos del menú contextual {#context-menu-elements}

| Elemento                                           | Uso          | Varias instancias | Descripción                                                                                     |
| -------------------------------------------------- | ------------ | ----------------- | ----------------------------------------------------------------------------------------------- |
| [context-button](#context-button-attributes)       | **Required** | Si                | Elemento del botón del menú de contexto. Define un elemento del menú contextual |
| [button-action](#button-action-attributes)         | **Required** | Si                | Acción del botón contextual. Se puede definir una lista de acciones de botón    |
| [context-separator](#context-separator-attributes) | Opcional     | Si                | Línea separadora de la lista de botones de contexto                                             |
| [dependency](dependencies.md)                      | Opcional     | Si                | Lista de dependencias definidas en el botón                                                     |

### Atributos del botón de contexto {#context-button-attributes}

| Name    | Tipo   | Uso          | Descripción                                    | Valores                                                                                                       |
| ------- | ------ | ------------ | ---------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| `id`    | String | **Required** | Identificador del botón de contexto            |                                                                                                               |
| `label` | String | **Required** | Etiqueta del botón contextual. | **Note:** You can use [i18n](i18n-internationalization.md) files (locales) |
| `icon`  | String | Opcional     | Nombre del icono del botón contextual          | **Note:** You can check all iconsets at [icons](icons.md) screen                              |

> **Note:** The context button has the same attributes as button element. You can see more info [here](button.md#button-attributes)

### Atributos de acción del botón {#button-action-attributes}

> **Note:** You can see all attributes of `button-action ` [here](button.md#button-actions)

### Atributos de los separadores de contexto {#context-separator-attributes}

| Name   | Tipo   | Uso      | Descripción                             | Valores |
| ------ | ------ | -------- | --------------------------------------- | ------- |
| `name` | String | Opcional | Identificador del separador de contexto |         |

## Ejemplos {#examples}

- Menú contextual dentro de una matriz

```xml
<grid id="GrdSta" style="expand" initial-load="query" server-action="data" target-action="QryUniTst" max="30">
  <column label="PARAMETER_TEXT" name="Als" sort-field="Als" align="left" charlength="20" style="separator" />
  <group-header name="GrpHeaCol" label="PARAMETER_TEXT">
    <column label="PARAMETER_TEXT" name="Des" sort-field="Des" align="left" charlength="40" />
    <column label="PARAMETER_TEXT" name="Prg" sort-field="Prg" align="center" charlength="40" 
            component="progress" value="50" server-action="data" target-action="QryChkPrg" />
  </group-header>
  <context-button id="CtxGrdStaAdd" label="BUTTON_NEW" icon="plus-circle" >
    <button-action type="screen" target="matrix_test" />
    <dependency target-type="show" initial="true">
      <dependency-element id="GrdSta" column="Als" attribute="selectedRowValue" condition="!=" value="awemadora02" />
      <dependency-element id="GrdSta" event="select-row" />
    </dependency>
  </context-button>
  <context-button id="CtxGrdStaDel" label="BUTTON_DELETE" icon="trash">
    <button-action type="screen" target="matrix_test" />
    <dependency target-type="show" initial="true">
      <dependency-element id="GrdSta" column="Als" attribute="selectedRowValue" condition="==" value="awemadora02" />
      <dependency-element id="GrdSta" event="select-row" />
    </dependency>
  </context-button>
</grid>
```
