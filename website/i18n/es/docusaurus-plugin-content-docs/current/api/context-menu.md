---
id: context-menu
title: Menú contextual
---

El motor de Awe permite definir un menú contextual dentro de un *grid*, *tree grid*, gráfica, botón, criterio, pestaña o *wizard*. Con estos menús, se puede añadir funcionalidad a la ventana.

## Apertura del menú {#opening-the-menu}

El menú se abre con un clic derecho sobre el componente que lo declara: *grids*, *tree grids*, gráficas, botones, criterios, pestañas y *wizards*.

- Cuando ninguna de las opciones es visible (por ejemplo, por sus dependencias), el menú no se abre y se muestra el menú del navegador. Los *grids* y *tree grids* son la excepción: las opciones de un *grid* suelen depender de la fila seleccionada, y esas dependencias se resuelven después de que el clic seleccione la fila, así que un *grid* abre su menú siempre que declare opciones.
- Cuando un componente está dentro de otro que también declara un menú contextual (un criterio dentro de una pestaña), prevalece el menú del componente interior sobre el de su contenedor.

### Abrir el menú desde un botón {#opening-the-menu-from-a-button}

Un botón abre un menú contextual con la acción de cliente `show-context-menu`. Es la forma de dar a cada fila de un *grid* un botón que abre con clic izquierdo el menú contextual del *grid*:

```xml
<column name="Actions" label="" component="button" icon="ellipsis-v" charlength="12">
  <button-action type="show-context-menu"/>
</column>
```

- El botón de una fila de *grid* selecciona su fila (solo ella, como hace todo [botón de una fila](grids.md#button-column-component)) y abre el menú contextual del *grid* justo debajo del botón.
- Cualquier otro botón abre su propio menú contextual (los elementos `context-button` declarados dentro del botón), justo debajo de sí mismo.
- Cuando no hay nada que abrir la acción solo termina y las siguientes acciones continúan. Para un botón fuera de un *grid* significa que no declara opciones o que ninguna es visible. Un *grid* abre su menú siempre que declare opciones, aunque ninguna sea visible en ese momento, porque sus dependencias se resuelven después de seleccionar la fila; solo un *grid* que no declara opciones no tiene nada que abrir (y entonces tampoco se selecciona la fila).
- La acción funciona en los dos motores de cliente (AngularJS y React) y se permite en cualquier botón. No indiques `target`: la acción siempre actúa sobre el botón que la lanza.

<img alt="Menú contextual de grid" src={require('@docusaurus/useBaseUrl').default('img/GridContextMenu.png')} />

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

| Elemento                                                       | Uso             | Varias instancias | Descripción                                                                     |
| -------------------------------------------------------------- | --------------- | ----------------- | ------------------------------------------------------------------------------- |
| [context-button](#context-button-attributes)             | **Obligatorio** | Si                | Elemento del botón del menú de contexto. Define un elemento del menú contextual |
| [button-action](#button-action-attributes)                | **Obligatorio** | Si                | Acción del botón contextual. Se puede definir una lista de acciones de botón    |
| [context-separator](#context-separator-attributes) | Opcional        | Si                | Línea separadora de la lista de botones de contexto                             |
| [dependency](dependencies.md)                                  | Opcional        | Si                | Lista de dependencias definidas en el botón                                     |

### Atributos del botón de contexto {#context-button-attributes}

| Nombre  | Tipo   | Uso             | Descripción                           | Valores                                                              |
| ------- | ------ | --------------- | ------------------------------------- | -------------------------------------------------------------------- |
| `id`    | String | **Obligatorio** | Identificador del botón de contexto   |                                                                      |
| `label` | String | **Obligatorio** | Etiqueta del botón contextual.        | **Nota:** Puedes usar literales [i18n](i18n-internationalization.md) |
| `icon`  | String | Opcional        | Nombre del icono del botón contextual | **Nota:** Puedes consultar todos los conjuntos de iconos en la pantalla [icons](icons.md)     |

> **Nota:** El botón contextual tiene los mismos atributos que el elemento de botón. Puedes ver más información [aquí](button.md#button-attributes)

### Atributos de acción del botón {#button-action-attributes}

> **Nota:** Puede ver todos los atributos de `button-action` [aquí](button.md#button-actions)

### Atributos de los separadores de contexto {#context-separator-attributes}

| Nombre | Tipo   | Uso      | Descripción                             | Valores |
| ------ | ------ | -------- | --------------------------------------- | ------- |
| `name` | String | Opcional | Identificador del separador de contexto |         |

## Ejemplos {#examples}

- Menú contextual dentro de un grid

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
