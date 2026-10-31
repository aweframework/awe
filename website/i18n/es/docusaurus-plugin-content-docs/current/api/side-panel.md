---
id: side-panel
title: Panel lateral
---

Un panel lateral es un [diálogo](dialog.md) anclado al borde izquierdo o derecho de la pantalla, con una cabecera, un
cuerpo con desplazamiento y un pie opcional. Úselo para contenido que el usuario consulta o edita junto a la pantalla
(filtros, detalles, ajustes) sin salir de ella. Se abre y se cierra exactamente como un diálogo: con una acción de botón
de tipo `dialog` (ver [acciones de botón](button.md#button-actions)) y con las acciones `close` y `close-cancel`. Lo
soportan los dos motores web (AngularJS y React).

<img alt="Panel lateral" src={require('@docusaurus/useBaseUrl').default('img/side-panel.png')} />

---

## Esqueleto de XML {#xml-skeleton}

```xml
<side-panel id="[panel-identifier]" label="[panel-label]" icon="[panel-icon]" style="[panel-style]"
            position="[left|right]" max-width="[css-length]" on-close="[on-close]" deferred="[deferred]">
  [define the content of the side panel]
  <tag type="div" style="modal-footer">
    [footer buttons]
  </tag>
</side-panel>
```

## Estructura del panel lateral {#side-panel-structure}

| Elemento                             | Uso             | Múltiples instancias | Descripción                                                          |
|--------------------------------------|-----------------|----------------------|----------------------------------------------------------------------|
| [side-panel](#side-panel-attributes) | **Obligatorio** | No                   | Nodo global del panel lateral. Define los atributos del panel lateral |
| [header-tag](#identity-header)       | Opcional        | Sí                   | Etiqueta de la cabecera de identidad (con `header-grid`)             |

## Atributos del panel lateral {#side-panel-attributes}

| Atributo  | Uso      | Tipo    | Descripción                                                                                                                         | Valores                                                                                                  |
|-----------|----------|---------|-------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| id        | Opcional | String  | Identificador del panel lateral. Es el destino de las acciones `dialog`, `close` y `close-cancel`                                  |                                                                                                          |
| label     | Opcional | String  | Título de la cabecera                                                                                                               | **Nota:** Puede usar ficheros de [i18n](i18n-internationalization.md) (locales)                          |
| icon      | Opcional | String  | Icono de la cabecera (motor AngularJS)                                                                                              | **Nota:** Puede consultar todos los juegos de iconos en la pantalla de [iconos](icons.md)                |
| style     | Opcional | String  | Estilo del panel lateral (clases css)                                                                                               | Clases CSS separadas por espacio (`' '`)                                                                 |
| position  | Opcional | String  | Borde de la pantalla al que se ancla el panel lateral                                                                               | `right` (por defecto), `left`                                                                            |
| max-width | Opcional | String  | Ancho máximo del panel lateral. Ocupa todo el ancho de la pantalla cuando la pantalla es más estrecha                               | Una longitud CSS, por ejemplo `30rem`, `480px` o `50vw`. Por defecto `40rem`                             |
| on-close  | Opcional | String  | Comportamiento de la pila al cerrar el panel lateral con su botón de cierre, con Esc o con un clic fuera                            | `accept` (por defecto), `reject`. Reject cancela la pila, accept continúa ejecutando sus acciones        |
| deferred  | Opcional | Boolean | Carga los datos de los componentes del panel lateral la primera vez que se abre, no con la pantalla. Ver [carga diferida](dialog.md#deferred-loading) | `true`, `false` (por defecto)                                                               |
| header-grid | Opcional | String | Grid de la pantalla cuya fila seleccionada muestra la cabecera. Ver [cabecera de identidad](#identity-header) | Un id de grid |
| header-title-field | Opcional | String | Campo de la fila seleccionada con el título de la cabecera de identidad | Un campo del grid |
| header-subtitle-field | Opcional | String | Campo de la fila seleccionada con el subtítulo (atenuado, bajo el título). No se muestra si está vacío | Un campo del grid |
| header-avatar-field | Opcional | String | Campo de la fila seleccionada del que se toman las iniciales del avatar (como el `avatar-field` de una [columna de grid](grids.md)). Sin avatar si está vacío | Un campo del grid |
| header-avatar-tone-field | Opcional | String | Campo de la fila seleccionada con el tono del avatar | `primary`, `info`, `success`, `warning`, `danger`, `neutral` (por defecto, cualquier otro valor) |
| header-swatch-field | Opcional | String | Campo de la fila seleccionada con un color que se dibuja como un círculo grande en el lugar del avatar (como el `swatch-field` de una [columna de grid](grids.md#swatch)). Sin círculo con cualquier otro valor | Un nombre de paleta de AWE 5 o un hexadecimal `#RGB` / `#RRGGBB` |

## Apertura y cierre {#opening-and-closing}

El panel lateral se abre con una acción `dialog` cuyo destino es el panel lateral, igual que un diálogo:

```xml
<button id="ButPnlRight" label="BUTTON_SIDE_PANEL_RIGHT" icon="arrow-left">
  <button-action type="dialog" target="PnlRight"/>
</button>
```

Se cierra:

- con una acción `close` (las acciones que siguen a la acción `dialog` continúan) o `close-cancel` (se cancelan),
  normalmente en los botones de su pie;
- con el botón de cierre de su cabecera, con Esc o con un clic fuera de él. Estos siguen el atributo `on-close`:
  `accept` (por defecto) se comporta como `close` y `reject` como `close-cancel`.

## Cabecera de identidad {#identity-header}

Con `header-grid` la cabecera del panel lateral muestra la fila seleccionada de ese grid cuando hay **exactamente una**
fila seleccionada (por ejemplo, el panel lateral que edita el usuario de la fila seleccionada): un avatar grande con las
iniciales de `header-avatar-field`, el título de `header-title-field`, el subtítulo de `header-subtitle-field` y un
`header-tag` por cada campo que se muestra como etiqueta, en el orden del XML. Sin filas o con varias filas seleccionadas
(por ejemplo, el mismo panel lateral abierto para crear un registro nuevo) la cabecera muestra su etiqueta y su icono. La
cabecera sigue a la fila seleccionada mientras el panel lateral está abierto.

| Atributo de `header-tag` | Uso             | Descripción                                                                                                |
|--------------------------|-----------------|------------------------------------------------------------------------------------------------------------|
| field                    | **Obligatorio** | Campo de la fila seleccionada con el texto de la etiqueta. Una etiqueta con el valor vacío no se muestra    |
| tone-field               | Opcional        | Campo con el tono de la etiqueta: `success`, `warning`, `danger`, `info` o `neutral` (por defecto)          |
| icon-field               | Opcional        | Campo con el icono de la etiqueta. Con él la etiqueta es plana: el icono ocupa el lugar del punto           |

El avatar y las etiquetas se ven como el avatar y las etiquetas de estado de las [celdas del grid](grids.md). El avatar es
decorativo; el título es el nombre accesible del panel lateral. Con `header-swatch-field` un círculo del color de la fila
seleccionada (un nombre de paleta de AWE 5 o un hexadecimal, como la [muestra de color de una columna de
grid](grids.md#swatch)) ocupa el lugar del avatar, como hace la pantalla de temas con el color primario del tema; con un
color vacío o desconocido la cabecera muestra el avatar de `header-avatar-field`, si lo hay. Todos los atributos `header-*` son opcionales; sin
`header-grid` la cabecera no cambia. Lo soportan los dos motores.

```xml
<side-panel id="PnlUsr" label="SCREEN_TITLE_USER_PANEL" icon="user"
            header-grid="GrdUsrAdm" header-title-field="FulNam" header-subtitle-field="UsrPro"
            header-avatar-field="Ini" header-avatar-tone-field="IniTon">
  <header-tag field="StaTxt" tone-field="StaTon"/>
  <header-tag field="2faHdr" tone-field="2faTon" icon-field="2faIcn"/>
  [define the content of the side panel]
</side-panel>
```

## Pie {#footer}

Ponga los botones del panel lateral en una etiqueta hija con el estilo `modal-footer`. La cabecera y el pie siempre están
a la vista y el cuerpo se desplaza entre ellos cuando su contenido es más largo que la pantalla.

## Foco {#focus}

Mientras el panel lateral está abierto, el foco del teclado se queda dentro: Tab desde su último control vuelve al
primero (el botón de cierre de la cabecera) y Shift+Tab desde el primero va al último. Al cerrarse el panel lateral, el
foco vuelve al control que lo abrió.

## Ejemplo {#example}

Un panel lateral en el borde derecho, de 30rem de ancho, con pie, y un panel lateral diferido en el borde izquierdo, de
20rem de ancho, que rechaza la pila cuando se descarta:

```xml
<tag source="buttons">
  <button id="ButPnlRight" label="BUTTON_SIDE_PANEL_RIGHT" icon="arrow-left">
    <button-action type="dialog" target="PnlRight"/>
  </button>
  <button id="ButPnlLeft" label="BUTTON_SIDE_PANEL_LEFT" icon="arrow-right">
    <button-action type="dialog" target="PnlLeft"/>
  </button>
</tag>
<tag source="center">
  <window style="expand" label="SCREEN_TITLE_SIDE_PANEL" expandible="vertical">
    <tag type="div" style="panel-body">
      <criteria id="CrtPnlResult" label="PARAMETER_SIDE_PANEL_RESULT" component="text" readonly="true"
                style="col-xs-12 col-sm-6"/>
    </tag>
  </window>

  <side-panel id="PnlRight" label="SIDE_PANEL_TITLE_RIGHT" max-width="30rem">
    <tag type="div" style="panel-body">
      <criteria id="CrtPnlName" label="PARAMETER_NAME" component="text" style="col-xs-12"/>
      <criteria id="CrtPnlNotes" label="PARAMETER_DESCRIPTION" component="textarea" style="col-xs-12"/>
    </tag>
    <tag type="div" style="modal-footer">
      <button id="ButPnlRightCancel" label="BUTTON_CANCEL" icon="times">
        <button-action type="close-cancel" target="PnlRight"/>
      </button>
      <button id="ButPnlRightAccept" label="BUTTON_ACCEPT" icon="check">
        <button-action type="close" target="PnlRight"/>
        <button-action type="value" target="CrtPnlResult" value="accepted"/>
      </button>
    </tag>
  </side-panel>

  <side-panel id="PnlLeft" label="SIDE_PANEL_TITLE_LEFT" position="left" max-width="20rem" on-close="reject"
              deferred="true">
    <tag type="div" style="panel-body">
      <criteria id="CrtPnlLeftName" label="PARAMETER_NAME" component="text" style="col-xs-12"/>
    </tag>
    <tag type="div" style="modal-footer">
      <button id="ButPnlLeftClose" label="BUTTON_CLOSE" icon="times">
        <button-action type="close" target="PnlLeft"/>
      </button>
    </tag>
  </side-panel>
</tag>
```

Con el [módulo builder](../builder-module.md), `SidePanelBuilder` construye un `<side-panel>`:

```java
new SidePanelBuilder()
  .setPosition(SidePanelPosition.LEFT)
  .setMaxWidth("20rem")
  .setOnClose(OnClose.REJECT)
  .setDeferred(true)
  .setId("PnlLeft")
  .setLabel("SIDE_PANEL_TITLE_LEFT");
```
