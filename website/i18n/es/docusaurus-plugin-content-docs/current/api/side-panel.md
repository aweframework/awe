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
