---
id: window
title: Ventana
---

Una ventana es un contenedor con una barra de título. También puede maximizarse o restaurarse, y es muy útil para ordenar grupos de componentes en la pantalla.

<img alt="Ventana" src={require('@docusaurus/useBaseUrl').default('img/Window.png')} />

## Esqueleto XML {#xml-skeleton}

```xml 
<window id="[window-identifier]" label="[window-label]" style="[window-style]"
        icon="[window-icon]" expandible="[expand-orientation]" maximize="[maximize-window]"
        advanced-mode="[advanced-mode]">
  <tag type="div" style="panel-body">
    ...
    <advanced>...</advanced>
  </tag>
  <tag type="div" style="panel-footer">...</tag>
  ...
  <grid>...</grid>
  ...
  <chart>...</chart>
  ...
</window>
```

## Estructura de la ventana {#window-structure}

| Elemento                     | Uso             | Varias instancias | Descripción                                                                                                   |
|------------------------------|-----------------|-------------------|---------------------------------------------------------------------------------------------------------------|
| [window](#window-attributes) | **Obligatorio** | No                | Nodo global de la ventana. Describe los atributos de la ventana                                               |
| [tag](tags.md)               | Opcional        | Sí                | Una lista de [tags](tags.md) dentro de la ventana, normalmente con estilos como `panel-body` y `panel-footer` |
| [grid](grids.md)             | Opcional        | No                | Un [grid](grids.md) dentro de la ventana                                                                  |
| [chart](chart.md)            | Opcional        | No                | Un [gráfico](chart.md) dentro de la ventana                                                                   |
| [advanced](#advanced-search) | Opcional        | No                | El bloque avanzado de una ventana de búsqueda: sus criterios avanzados, que se muestran a demanda. Ver [búsqueda avanzada](#advanced-search) |

> **Nota** Hay tres estilos especiales que puedes usar como estilos de tag en las ventanas:
> * `panel-body`: Un estilo especial para definir el contenido de una ventana. Añade márgenes al contenido.
> * `panel-footer`: Un estilo especial para definir la parte inferior de una ventana. Se recomienda colocar botones dentro.
> * `expand-maximize`: Un estilo especial, normalmente combinado con `panel-body`, que expande el contenido de la ventana cuando esta se maximiza.

## Atributos de la ventana {#window-attributes}

| Atributo   | Uso      | Tipo    | Descripción                                          | Valores                                                                                       |
|------------|----------|---------|------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| id         | Opcional | String  | Identificador de la ventana. Con fines de referencia |                                                                                               |
| label      | Opcional | String  | Título de la ventana                                 | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales)                 |
| style      | Opcional | String  | Clases CSS de la ventana                             | **Nota:** Aquí puedes usar la clase `expand` para hacer la ventana expandible                 |
| icon       | Opcional | String  | Identificador del icono                              | **Nota:** Puedes consultar todos los conjuntos de iconos en la pantalla de [iconos](icons.md) |
| expandible | Opcional | String  | Cómo [expandir](layout.md) los hijos de la ventana   | `vertical`, `horizontal`                                                                      |
| maximize   | Opcional | Boolean | Si se muestra o no el icono de maximizar             | **Nota:** Solo se puede maximizar una ventana con `label`                                     |
| advanced-mode | Opcional | String | Cómo muestra la ventana su [bloque avanzado](#advanced-search) | `inline` (por defecto), `side-panel`. **Nota:** Solo se usa si la ventana tiene un bloque `advanced` |
| help       | Opcional | String  | Texto de ayuda del criterio                          | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales)                 |
| help-image | Opcional | String  | Imagen de ayuda del criterio                         | Esto **debe** ser una ruta de imagen                                                          |

## Ejemplos {#examples}

### Ventana expandible con grid (maximizable) {#expandible-window-with-grid-maximizable}

```xml 
<window style="expand" maximize="true" label="SCREEN_TEXT_DATA" icon="list">
  <grid ...>...</grid>
</window>
```

<img alt="Ventana expandible con grid" src={require('@docusaurus/useBaseUrl').default('img/expandible window with grid.png')} />

### Ventana estática con contenido y zona de botones {#static-window-with-content-and-buttons-zone}

```xml 
<window label="SCREEN_TEXT_CRITERIA" icon="filter">
  <tag type="div" style="panel-body">...</tag>
  <tag type="div" style="panel-footer">
    <tag type="div" style="pull-right">...</tag>
  </tag>
</window>
```
<img alt="Ventana estática con botones y panel" src={require('@docusaurus/useBaseUrl').default('img/Static_window_with_buttons_and_panel.png')} />

## Búsqueda avanzada {#advanced-search}

Una ventana de búsqueda puede mostrar solo los criterios que se usan a diario y dejar el resto a mano. Coloca primero
los criterios imprescindibles y, justo después, un bloque `<advanced>` con los menos frecuentes (y los tags de
maquetación que necesiten). Elige con `advanced-mode` cómo muestra la ventana el bloque:

* `inline` (por defecto): un botón **Filtros avanzados** muestra el bloque debajo de él, dentro de la ventana, y
  **Ocultar filtros avanzados** lo cierra. El botón no se mueve al abrirse el bloque.

<img alt="Filtros avanzados plegados, con un filtro oculto aplicado" src={require('@docusaurus/useBaseUrl').default('img/advanced-search-inline-collapsed.png')} />

<img alt="Filtros avanzados desplegados debajo del botón" src={require('@docusaurus/useBaseUrl').default('img/advanced-search-inline-expanded.png')} />

* `side-panel`: el mismo botón **Filtros avanzados** abre el bloque en un panel lateral con el título de la ventana.
  **Restablecer valores por defecto** devuelve los criterios avanzados a su valor por defecto, **Cancelar** (o Escape)
  deshace los cambios hechos en el panel y **Ver resultados** lo cierra y lanza la búsqueda.

<img alt="Filtros avanzados en un panel lateral" src={require('@docusaurus/useBaseUrl').default('img/advanced-search-side-panel.png')} />

El botón es el primer elemento de una fila que la ventana dibuja donde está el bloque `<advanced>`, así que los
criterios imprescindibles quedan encima y los avanzados debajo (o en el panel). El botón indica cuántos criterios
avanzados tienen un valor distinto del valor por defecto. Tras un separador, mientras haya filtros aplicados, la fila
muestra como chips **Filtrado por** cada criterio de la ventana cuyo valor difiere del valor por defecto, también los
avanzados que están ocultos (marcados con un icono). Cada chip devuelve su criterio al valor por defecto y **Quitar
todos**, al final de la fila, los devuelve todos. Si un criterio avanzado oculto no supera una validación, la ventana
lo muestra.

**Ver resultados** e Intro en cualquier criterio de la ventana lanzan el botón de envío de la ventana
(`button-type="submit"`). La barra de título y el pie de la ventana quedan tal como los escribes, así que los botones de
buscar y restablecer son botones normales con sus propios identificadores.

Los criterios avanzados siempre forman parte de la pantalla, así que las dependencias, la validación, los valores por
defecto, las cargas iniciales, los valores de sesión y de variable, la impresión y la exportación funcionan igual cuando
están ocultos. Declara como mucho un bloque `<advanced>` por ventana; solo tiene efecto dentro de una ventana. Una
ventana sin él no cambia.

```xml
<window id="WinUsrCrt" label="SCREEN_TEXT_CRITERIA" icon="filter" advanced-mode="side-panel">
  <tag type="div" style="panel-body">
    <criteria id="CrtUsr" component="suggest" label="PARAMETER_USER" .../>
    <criteria id="CrtPro" component="suggest" label="PARAMETER_PROFILE" .../>
    <advanced>
      <criteria id="CrtThm" component="suggest" label="PARAMETER_THEME" .../>
      <criteria id="CrtBlk" component="select" label="PARAMETER_BLOCKED" .../>
    </advanced>
  </tag>
  <tag type="div" style="panel-footer">
    <tag type="div" style="pull-right">
      <button id="ButRst" button-type="reset" label="BUTTON_RESET" icon="eraser"/>
      <button id="ButSch" button-type="submit" label="BUTTON_SEARCH" icon="search">
        <button-action type="filter" target="GrdUsrLst"/>
      </button>
    </tag>
  </tag>
</window>
```

## Motores AngularJS y React {#angularjs-and-react-engines}

Ambos motores tratan `expandible` y `maximize` de la misma forma:

* `expandible` establece la dirección en la que la ventana reparte el espacio entre sus hijos (`vertical` por defecto).
* `maximize="true"` muestra un botón en la barra de título que maximiza la ventana sobre el contenido de la pantalla y la
  restaura. La ventana ocupa el área de contenido, manteniendo visibles el menú y la barra superior, y las ventanas
  hermanas se ocultan mientras está maximizada. El botón Restaurar la devuelve a su tamaño; no hay atajo con la tecla
  Escape. El contenido con el estilo `expand-maximize` ocupa el espacio libre mientras la ventana está maximizada.

Las diferencias son solo visuales: el botón de maximizar muestra los iconos Lucide `expand` y `shrink` en el motor
AngularJS y `maximize` y `minimize` en el motor React, y el motor React muestra el `icon` de la ventana a la derecha de la barra de título.
