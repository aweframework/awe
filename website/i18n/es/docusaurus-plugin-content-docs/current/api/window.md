---
id: window
title: Ventana
---

Una ventana es un contenedor con una barra de título. También puede maximizarse o restaurarse, y es muy útil para ordenar grupos de componentes en la pantalla.

<img alt="Ventana" src={require('@docusaurus/useBaseUrl').default('img/Window.png')} />

## Esqueleto XML {#xml-skeleton}

```xml 
<window id="[window-identifier]" label="[window-label]" style="[window-style]"
        icon="[window-icon]" expandible="[expand-orientation]" maximize="[maximize-window]">
  <tag type="div" style="panel-body">...</tag>
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
| [grid](grids.md)             | Opcional        | No                | Una [rejilla](grids.md) dentro de la ventana                                                                  |
| [chart](chart.md)            | Opcional        | No                | Un [gráfico](chart.md) dentro de la ventana                                                                   |

> **Nota** Hay dos estilos especiales que puedes usar como estilos de tag en las ventanas:
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
| maximize   | Opcional | Boolean | Si se muestra o no el icono de maximizar             |                                                                                               |
| help       | Opcional | String  | Texto de ayuda del criterio                          | **Nota:** Puedes usar archivos [i18n](i18n-internationalization.md) (locales)                 |
| help-image | Opcional | String  | Imagen de ayuda del criterio                         | Esto **debe** ser una ruta de imagen                                                          |

## Ejemplos {#examples}

### Ventana expandible con rejilla (maximizable) {#expandible-window-with-grid-maximizable}

```xml 
<window style="expand" maximize="true" label="SCREEN_TEXT_DATA" icon="list">
  <grid ...>...</grid>
</window>
```

<img alt="Ventana expandible con rejilla" src={require('@docusaurus/useBaseUrl').default('img/expandible window with grid.png')} />

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
